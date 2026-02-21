package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.ApproveVacationRequest
import com.zeiterfassung.model.dto.CreateVacationRequest
import com.zeiterfassung.model.dto.PublicHolidayResponse
import com.zeiterfassung.model.dto.RejectVacationRequest
import com.zeiterfassung.model.dto.UpdateVacationRequest
import com.zeiterfassung.model.dto.VacationBalanceResponse
import com.zeiterfassung.model.dto.VacationCalendarResponse
import com.zeiterfassung.model.dto.VacationRequestResponse
import com.zeiterfassung.model.entity.PublicHolidayEntity
import com.zeiterfassung.model.entity.VacationBalanceEntity
import com.zeiterfassung.model.entity.VacationRequestEntity
import com.zeiterfassung.model.enums.VacationStatus
import com.zeiterfassung.repository.EmployeeConfigRepository
import com.zeiterfassung.repository.PublicHolidayRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.repository.VacationBalanceRepository
import com.zeiterfassung.repository.VacationRequestRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class VacationService(
    private val vacationRequestRepository: VacationRequestRepository,
    private val vacationBalanceRepository: VacationBalanceRepository,
    private val publicHolidayRepository: PublicHolidayRepository,
    private val employeeConfigRepository: EmployeeConfigRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun createRequest(
        userId: UUID,
        dto: CreateVacationRequest,
    ): VacationRequestResponse {
        val user =
            userRepository.findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        if (dto.startDate.isAfter(dto.endDate)) {
            throw BadRequestException("vacation.error.start_after_end")
        }
        if (dto.startDate.isBefore(LocalDate.now())) {
            throw BadRequestException("vacation.error.past_date")
        }

        val overlapping = vacationRequestRepository.findOverlapping(userId, dto.startDate, dto.endDate, null)
        if (overlapping.isNotEmpty()) {
            throw ConflictException("vacation.error.overlap")
        }

        val workDays = getWorkDays(userId)
        val holidays = getHolidaysForRange(dto.startDate, dto.endDate, null)
        val totalDays =
            calculateWorkingDays(
                dto.startDate,
                dto.endDate,
                dto.isHalfDayStart,
                dto.isHalfDayEnd,
                workDays,
                holidays,
            )

        val balance = getOrCreateBalance(userId, dto.startDate.year)
        val effectiveRemaining = balance.totalDays + balance.carriedOverDays - balance.usedDays
        if (totalDays > effectiveRemaining) {
            throw BadRequestException("vacation.error.insufficient_balance")
        }

        val entity =
            VacationRequestEntity(
                user = user,
                startDate = dto.startDate,
                endDate = dto.endDate,
                isHalfDayStart = dto.isHalfDayStart,
                isHalfDayEnd = dto.isHalfDayEnd,
                totalDays = totalDays,
                notes = dto.notes,
            )
        val saved = vacationRequestRepository.save(entity)
        auditService.logDataChange(userId, "VACATION_REQUEST_CREATED", "VacationRequest", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun updateRequest(
        requestId: UUID,
        userId: UUID,
        dto: UpdateVacationRequest,
    ): VacationRequestResponse {
        val entity = findRequestOrThrow(requestId)
        if (entity.user.id != userId) {
            throw ForbiddenException("vacation.error.not_owner")
        }
        if (entity.status != VacationStatus.PENDING) {
            throw BadRequestException("vacation.error.not_pending")
        }

        val newStart = dto.startDate ?: entity.startDate
        val newEnd = dto.endDate ?: entity.endDate

        if (newStart.isAfter(newEnd)) {
            throw BadRequestException("vacation.error.start_after_end")
        }
        if (newStart.isBefore(LocalDate.now())) {
            throw BadRequestException("vacation.error.past_date")
        }

        val overlapping = vacationRequestRepository.findOverlapping(userId, newStart, newEnd, requestId)
        if (overlapping.isNotEmpty()) {
            throw ConflictException("vacation.error.overlap")
        }

        val newHalfDayStart = dto.isHalfDayStart ?: entity.isHalfDayStart
        val newHalfDayEnd = dto.isHalfDayEnd ?: entity.isHalfDayEnd

        val workDays = getWorkDays(userId)
        val holidays = getHolidaysForRange(newStart, newEnd, null)
        val totalDays = calculateWorkingDays(newStart, newEnd, newHalfDayStart, newHalfDayEnd, workDays, holidays)

        val balance = getOrCreateBalance(userId, newStart.year)
        val effectiveRemaining = balance.totalDays + balance.carriedOverDays - balance.usedDays
        if (totalDays > effectiveRemaining) {
            throw BadRequestException("vacation.error.insufficient_balance")
        }

        val oldResponse = entity.toResponse()
        entity.startDate = newStart
        entity.endDate = newEnd
        entity.isHalfDayStart = newHalfDayStart
        entity.isHalfDayEnd = newHalfDayEnd
        entity.totalDays = totalDays
        dto.notes?.let { entity.notes = it }

        val saved = vacationRequestRepository.save(entity)
        auditService.logDataChange(userId, "VACATION_REQUEST_UPDATED", "VacationRequest", saved.id, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun cancelRequest(
        requestId: UUID,
        userId: UUID,
    ) {
        val entity = findRequestOrThrow(requestId)
        if (entity.user.id != userId) {
            throw ForbiddenException("vacation.error.not_owner")
        }
        if (entity.status == VacationStatus.CANCELLED) {
            throw BadRequestException("vacation.error.already_cancelled")
        }
        if (entity.status == VacationStatus.REJECTED) {
            throw BadRequestException("vacation.error.not_pending")
        }

        val wasApproved = entity.status == VacationStatus.APPROVED
        val oldResponse = entity.toResponse()
        entity.status = VacationStatus.CANCELLED

        vacationRequestRepository.save(entity)

        if (wasApproved) {
            val balance = vacationBalanceRepository.findByUserIdAndYear(userId, entity.startDate.year)
            if (balance != null) {
                balance.usedDays = (balance.usedDays - entity.totalDays).max(BigDecimal.ZERO)
                vacationBalanceRepository.save(balance)
            }
        }

        auditService.logDataChange(userId, "VACATION_REQUEST_CANCELLED", "VacationRequest", requestId, oldResponse, entity.toResponse())
    }

    @Transactional
    fun approveRequest(
        requestId: UUID,
        approverId: UUID,
        dto: ApproveVacationRequest,
    ): VacationRequestResponse {
        val entity = findRequestOrThrow(requestId)
        val approver =
            userRepository.findById(approverId)
                .orElseThrow { ResourceNotFoundException("User not found: $approverId") }

        if (entity.user.id == approverId) {
            throw ForbiddenException("vacation.error.cannot_approve_own")
        }
        if (entity.status != VacationStatus.PENDING) {
            throw BadRequestException("vacation.error.not_pending")
        }

        val oldResponse = entity.toResponse()
        entity.status = VacationStatus.APPROVED
        entity.approvedBy = approver

        val balance = getOrCreateBalance(entity.user.id, entity.startDate.year)
        balance.usedDays = balance.usedDays + entity.totalDays
        vacationBalanceRepository.save(balance)

        val saved = vacationRequestRepository.save(entity)
        auditService.logDataChange(approverId, "VACATION_REQUEST_APPROVED", "VacationRequest", requestId, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun rejectRequest(
        requestId: UUID,
        approverId: UUID,
        dto: RejectVacationRequest,
    ): VacationRequestResponse {
        val entity = findRequestOrThrow(requestId)

        if (entity.user.id == approverId) {
            throw ForbiddenException("vacation.error.cannot_approve_own")
        }
        if (entity.status != VacationStatus.PENDING) {
            throw BadRequestException("vacation.error.not_pending")
        }

        val oldResponse = entity.toResponse()
        entity.status = VacationStatus.REJECTED
        entity.rejectionReason = dto.rejectionReason

        val saved = vacationRequestRepository.save(entity)
        auditService.logDataChange(approverId, "VACATION_REQUEST_REJECTED", "VacationRequest", requestId, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    fun getRequest(
        requestId: UUID,
        requestingUserId: UUID,
    ): VacationRequestResponse {
        val entity = findRequestOrThrow(requestId)
        val requestingUser =
            userRepository.findById(requestingUserId)
                .orElseThrow { ResourceNotFoundException("User not found: $requestingUserId") }
        val isOwner = entity.user.id == requestingUserId
        val isManagerOfOwner = requestingUser.subordinates.any { it.id == entity.user.id }
        val hasAdminPermission =
            requestingUser.roles.flatMap { it.permissions }.any { it.name == "admin.users.manage" }
        if (!isOwner && !isManagerOfOwner && !hasAdminPermission) {
            throw ForbiddenException("vacation.error.not_owner")
        }
        return entity.toResponse()
    }

    fun getUserRequests(
        userId: UUID,
        year: Int?,
        status: VacationStatus?,
        pageable: Pageable,
    ): Page<VacationRequestResponse> {
        val page =
            if (year != null) {
                val start = LocalDate.of(year, 1, 1)
                val end = LocalDate.of(year, 12, 31)
                val all = vacationRequestRepository.findByUserIdAndStartDateBetween(userId, start, end)
                val filtered = if (status != null) all.filter { it.status == status } else all
                // Wrap into page manually
                org.springframework.data.domain.PageImpl(
                    filtered.sortedByDescending { it.createdAt }
                        .drop(pageable.pageNumber * pageable.pageSize)
                        .take(pageable.pageSize),
                    pageable,
                    filtered.size.toLong(),
                )
            } else if (status != null) {
                val all = vacationRequestRepository.findByUserIdAndStatus(userId, status)
                org.springframework.data.domain.PageImpl(
                    all.sortedByDescending { it.createdAt }
                        .drop(pageable.pageNumber * pageable.pageSize)
                        .take(pageable.pageSize),
                    pageable,
                    all.size.toLong(),
                )
            } else {
                vacationRequestRepository.findByUserId(userId, pageable)
            }
        return page.map { it.toResponse() }
    }

    fun getPendingRequests(
        managerId: UUID,
        pageable: Pageable,
    ): Page<VacationRequestResponse> {
        val manager =
            userRepository.findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val subordinateIds = manager.subordinates.map { it.id }
        if (subordinateIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable)
        }
        return vacationRequestRepository
            .findByStatusAndUserIdIn(VacationStatus.PENDING, subordinateIds, pageable)
            .map { it.toResponse() }
    }

    fun getAllPendingRequests(pageable: Pageable): Page<VacationRequestResponse> =
        vacationRequestRepository.findByStatus(VacationStatus.PENDING, pageable).map { it.toResponse() }

    @Transactional
    fun getBalance(
        userId: UUID,
        year: Int,
    ): VacationBalanceResponse {
        val balance = getOrCreateBalance(userId, year)
        val pendingDays = getPendingDays(userId, year)
        return balance.toResponse(pendingDays)
    }

    fun getPublicHolidays(
        year: Int,
        stateCode: String?,
    ): List<PublicHolidayResponse> {
        val all = publicHolidayRepository.findApplicableForYear(year)
        return all
            .filter { stateCode == null || it.stateCode == null || it.stateCode == stateCode }
            .map { holiday ->
                val date =
                    if (holiday.isRecurring) {
                        holiday.date.withYear(year)
                    } else {
                        holiday.date
                    }
                PublicHolidayResponse(
                    id = holiday.id,
                    date = date,
                    name = holiday.name,
                    stateCode = holiday.stateCode,
                    isRecurring = holiday.isRecurring,
                )
            }
            .sortedBy { it.date }
    }

    fun getTeamCalendar(
        managerId: UUID,
        year: Int,
        month: Int,
    ): VacationCalendarResponse {
        val manager =
            userRepository.findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

        val subordinateIds = manager.subordinates.map { it.id }
        val teamRequests =
            if (subordinateIds.isEmpty()) {
                emptyList()
            } else {
                vacationRequestRepository.findApprovedByUserIdsAndDateRange(subordinateIds, startDate, endDate)
                    .map { it.toResponse() }
            }

        val ownRequests =
            vacationRequestRepository.findByUserIdAndStartDateBetween(managerId, startDate, endDate)
                .map { it.toResponse() }

        val holidays = getPublicHolidays(year, null)
            .filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }

        return VacationCalendarResponse(
            year = year,
            month = month,
            ownRequests = ownRequests,
            teamRequests = teamRequests,
            publicHolidays = holidays,
        )
    }

    @Transactional
    fun initializeYearBalance(
        userId: UUID,
        year: Int,
    ): VacationBalanceEntity {
        val existing = vacationBalanceRepository.findByUserIdAndYear(userId, year)
        if (existing != null) return existing

        val user =
            userRepository.findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }
        val config = employeeConfigRepository.findByUserId(userId)
        val totalDays = BigDecimal(config?.vacationDaysPerYear ?: 30)

        val carriedOverDays = calculateCarryOver(userId, year, config?.vacationCarryOverMax ?: 10)

        val balance =
            VacationBalanceEntity(
                user = user,
                year = year,
                totalDays = totalDays,
                carriedOverDays = carriedOverDays,
            )
        return vacationBalanceRepository.save(balance)
    }

    fun calculateWorkingDays(
        startDate: LocalDate,
        endDate: LocalDate,
        isHalfDayStart: Boolean,
        isHalfDayEnd: Boolean,
        workDays: List<Int>,
        holidays: List<LocalDate>,
    ): BigDecimal {
        if (startDate.isAfter(endDate)) return BigDecimal.ZERO

        var total = BigDecimal.ZERO
        var current = startDate

        while (!current.isAfter(endDate)) {
            val dayOfWeek = current.dayOfWeek.value // 1=Mon .. 7=Sun
            if (dayOfWeek in workDays && current !in holidays) {
                val isFirst = current == startDate
                val isLast = current == endDate
                val isHalfDay = (isFirst && isHalfDayStart) || (isLast && isHalfDayEnd && startDate != endDate)
                // If single day and both halves: 0.5 (half) or 1.0 (full)
                val isSingleDay = startDate == endDate
                val dayValue =
                    when {
                        isSingleDay && isHalfDayStart -> BigDecimal("0.5")
                        isHalfDay -> BigDecimal("0.5")
                        else -> BigDecimal.ONE
                    }
                total = total.add(dayValue)
            }
            current = current.plusDays(1)
        }

        return total
    }

    private fun getOrCreateBalance(
        userId: UUID,
        year: Int,
    ): VacationBalanceEntity =
        vacationBalanceRepository.findByUserIdAndYear(userId, year)
            ?: initializeYearBalance(userId, year)

    private fun calculateCarryOver(
        userId: UUID,
        year: Int,
        maxCarryOver: Int,
    ): BigDecimal {
        val prevYear = year - 1
        val prevBalance = vacationBalanceRepository.findByUserIdAndYear(userId, prevYear) ?: return BigDecimal.ZERO
        val prevRemaining = prevBalance.totalDays + prevBalance.carriedOverDays - prevBalance.usedDays
        if (prevRemaining <= BigDecimal.ZERO) return BigDecimal.ZERO
        return prevRemaining.min(BigDecimal(maxCarryOver))
    }

    private fun getPendingDays(
        userId: UUID,
        year: Int,
    ): BigDecimal {
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        return vacationRequestRepository.findByUserIdAndStartDateBetween(userId, start, end)
            .filter { it.status == VacationStatus.PENDING }
            .fold(BigDecimal.ZERO) { acc, req -> acc + req.totalDays }
    }

    private fun getWorkDays(userId: UUID): List<Int> {
        val config = employeeConfigRepository.findByUserId(userId) ?: return listOf(1, 2, 3, 4, 5)
        return try {
            objectMapper.readValue(config.workDays)
        } catch (e: Exception) {
            listOf(1, 2, 3, 4, 5)
        }
    }

    private fun getHolidaysForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        stateCode: String?,
    ): List<LocalDate> =
        getPublicHolidays(startDate.year, stateCode)
            .filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
            .map { it.date }

    private fun findRequestOrThrow(requestId: UUID): VacationRequestEntity =
        vacationRequestRepository.findById(requestId)
            .orElseThrow { ResourceNotFoundException("Vacation request not found: $requestId") }

    private fun VacationRequestEntity.toResponse() =
        VacationRequestResponse(
            id = id,
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            startDate = startDate,
            endDate = endDate,
            isHalfDayStart = isHalfDayStart,
            isHalfDayEnd = isHalfDayEnd,
            totalDays = totalDays,
            status = status,
            approvedById = approvedBy?.id,
            approvedByName = approvedBy?.let { "${it.firstName} ${it.lastName}" },
            rejectionReason = rejectionReason,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun VacationBalanceEntity.toResponse(pendingDays: BigDecimal = BigDecimal.ZERO) =
        VacationBalanceResponse(
            id = id,
            userId = user.id,
            year = year,
            totalDays = totalDays,
            usedDays = usedDays,
            carriedOverDays = carriedOverDays,
            remainingDays = (totalDays + carriedOverDays - usedDays).max(BigDecimal.ZERO),
            pendingDays = pendingDays,
        )

    private fun PublicHolidayEntity.toResponse() =
        PublicHolidayResponse(
            id = id,
            date = date,
            name = name,
            stateCode = stateCode,
            isRecurring = isRecurring,
        )
}
