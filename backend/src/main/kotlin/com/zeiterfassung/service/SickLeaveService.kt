package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateSickLeaveRequest
import com.zeiterfassung.model.dto.SickLeaveResponse
import com.zeiterfassung.model.dto.UpdateSickLeaveRequest
import com.zeiterfassung.model.entity.SickLeaveEntity
import com.zeiterfassung.model.enums.SickLeaveStatus
import com.zeiterfassung.repository.SickLeaveRepository
import com.zeiterfassung.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class SickLeaveService(
    private val sickLeaveRepository: SickLeaveRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val notificationService: NotificationService,
) {
    @Transactional
    fun reportSickLeave(
        userId: UUID,
        dto: CreateSickLeaveRequest,
    ): SickLeaveResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        validateDates(dto.startDate, dto.endDate)
        checkOverlap(userId, dto.startDate, dto.endDate)

        val entity =
            SickLeaveEntity(
                user = user,
                startDate = dto.startDate,
                endDate = dto.endDate,
                notes = dto.notes,
            )
        val saved = sickLeaveRepository.save(entity)
        auditService.logDataChange(userId, "SICK_LEAVE_REPORTED", "SickLeave", saved.id, null, saved.toResponse())

        // Notify manager
        user.manager?.let { manager ->
            if (manager.email.isNotBlank()) {
                notificationService.notifySickLeaveReported(saved, manager)
            }
        }

        return saved.toResponse()
    }

    @Transactional
    fun reportSickLeaveByManager(
        managerId: UUID,
        userId: UUID,
        dto: CreateSickLeaveRequest,
    ): SickLeaveResponse {
        val manager =
            userRepository
                .findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        validateDates(dto.startDate, dto.endDate)
        checkOverlap(userId, dto.startDate, dto.endDate)

        val entity =
            SickLeaveEntity(
                user = user,
                startDate = dto.startDate,
                endDate = dto.endDate,
                notes = dto.notes,
                reportedBy = manager,
            )
        val saved = sickLeaveRepository.save(entity)
        auditService.logDataChange(managerId, "SICK_LEAVE_REPORTED_BY_MANAGER", "SickLeave", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun updateSickLeave(
        sickLeaveId: UUID,
        userId: UUID,
        dto: UpdateSickLeaveRequest,
    ): SickLeaveResponse {
        val entity = findOrThrow(sickLeaveId)
        if (entity.user.id != userId) {
            throw ForbiddenException("sick_leave.error.not_owner")
        }
        if (entity.status == SickLeaveStatus.CANCELLED) {
            throw BadRequestException("sick_leave.error.already_cancelled")
        }

        val oldResponse = entity.toResponse()

        dto.endDate?.let { newEnd ->
            if (entity.startDate.isAfter(newEnd)) {
                throw BadRequestException("sick_leave.error.start_after_end")
            }
            entity.endDate = newEnd
        }
        dto.hasCertificate?.let { entity.hasCertificate = it }
        dto.notes?.let { entity.notes = it }

        val saved = sickLeaveRepository.save(entity)
        auditService.logDataChange(userId, "SICK_LEAVE_UPDATED", "SickLeave", saved.id, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun cancelSickLeave(
        sickLeaveId: UUID,
        userId: UUID,
    ) {
        val entity = findOrThrow(sickLeaveId)
        if (entity.user.id != userId) {
            throw ForbiddenException("sick_leave.error.not_owner")
        }
        if (entity.status == SickLeaveStatus.CANCELLED) {
            throw BadRequestException("sick_leave.error.already_cancelled")
        }

        val oldResponse = entity.toResponse()
        entity.status = SickLeaveStatus.CANCELLED
        sickLeaveRepository.save(entity)
        auditService.logDataChange(userId, "SICK_LEAVE_CANCELLED", "SickLeave", sickLeaveId, oldResponse, entity.toResponse())
    }

    fun getSickLeave(sickLeaveId: UUID): SickLeaveResponse = findOrThrow(sickLeaveId).toResponse()

    fun getUserSickLeaves(
        userId: UUID,
        pageable: Pageable,
    ): Page<SickLeaveResponse> = sickLeaveRepository.findByUserId(userId, pageable).map { it.toResponse() }

    fun getSickLeavesForDateRange(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
    ): List<SickLeaveResponse> =
        sickLeaveRepository
            .findByUserIdAndStartDateBetween(userId, start, end)
            .map { it.toResponse() }

    @Transactional
    fun submitCertificate(
        sickLeaveId: UUID,
        userId: UUID,
    ): SickLeaveResponse {
        val entity = findOrThrow(sickLeaveId)
        if (entity.user.id != userId) {
            throw ForbiddenException("sick_leave.error.not_owner")
        }
        if (entity.status == SickLeaveStatus.CANCELLED) {
            throw BadRequestException("sick_leave.error.already_cancelled")
        }

        val oldResponse = entity.toResponse()
        entity.hasCertificate = true
        entity.certificateSubmittedAt = Instant.now()
        entity.status = SickLeaveStatus.CERTIFICATE_RECEIVED

        val saved = sickLeaveRepository.save(entity)
        auditService.logDataChange(userId, "SICK_LEAVE_CERTIFICATE_SUBMITTED", "SickLeave", saved.id, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    private fun validateDates(
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        if (startDate.isAfter(endDate)) {
            throw BadRequestException("sick_leave.error.start_after_end")
        }
    }

    private fun checkOverlap(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        val overlapping = sickLeaveRepository.findOverlapping(userId, startDate, endDate)
        if (overlapping.isNotEmpty()) {
            throw ConflictException("sick_leave.error.overlap")
        }
    }

    private fun findOrThrow(id: UUID): SickLeaveEntity =
        sickLeaveRepository
            .findById(id)
            .orElseThrow { ResourceNotFoundException("Sick leave not found: $id") }

    private fun SickLeaveEntity.toResponse() =
        SickLeaveResponse(
            id = id,
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            startDate = startDate,
            endDate = endDate,
            status = status,
            hasCertificate = hasCertificate,
            certificateSubmittedAt = certificateSubmittedAt,
            notes = notes,
            reportedById = reportedBy?.id,
            reportedByName = reportedBy?.let { "${it.firstName} ${it.lastName}" },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
