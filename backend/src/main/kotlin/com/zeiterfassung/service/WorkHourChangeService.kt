package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateWorkHourChangeRequest
import com.zeiterfassung.model.dto.RejectWorkHourChangeRequest
import com.zeiterfassung.model.dto.WorkHourChangeResponse
import com.zeiterfassung.model.entity.WorkHourChangeRequestEntity
import com.zeiterfassung.model.enums.WorkHourChangeStatus
import com.zeiterfassung.repository.EmployeeConfigRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.repository.WorkHourChangeRequestRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class WorkHourChangeService(
    private val workHourChangeRequestRepository: WorkHourChangeRequestRepository,
    private val employeeConfigRepository: EmployeeConfigRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
) {
    @Transactional
    fun createRequest(
        userId: UUID,
        dto: CreateWorkHourChangeRequest,
    ): WorkHourChangeResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val pending = workHourChangeRequestRepository.findByUserIdAndStatus(userId, WorkHourChangeStatus.PENDING)
        if (pending.isNotEmpty()) {
            throw ConflictException("A pending work hour change request already exists")
        }

        val config =
            employeeConfigRepository.findByUserId(userId)
                ?: throw ResourceNotFoundException("Employee config not found for user: $userId")

        val entity =
            WorkHourChangeRequestEntity(
                user = user,
                currentWeeklyHours = config.weeklyWorkHours,
                requestedWeeklyHours = dto.requestedWeeklyHours,
                currentDailyHours = config.dailyWorkHours,
                requestedDailyHours = dto.requestedDailyHours,
                effectiveDate = dto.effectiveDate,
                reason = dto.reason,
            )
        val saved = workHourChangeRequestRepository.save(entity)
        auditService.logDataChange(
            userId,
            "WORK_HOUR_CHANGE_CREATED",
            "WorkHourChangeRequest",
            saved.id,
            null,
            saved.toResponse(),
        )
        return saved.toResponse()
    }

    fun getMyRequests(
        userId: UUID,
        pageable: Pageable,
    ): Page<WorkHourChangeResponse> = workHourChangeRequestRepository.findByUserId(userId, pageable).map { it.toResponse() }

    fun getPendingRequests(pageable: Pageable): Page<WorkHourChangeResponse> =
        workHourChangeRequestRepository
            .findByStatus(WorkHourChangeStatus.PENDING, pageable)
            .map { it.toResponse() }

    @Transactional
    fun approveRequest(
        requestId: UUID,
        approverId: UUID,
    ): WorkHourChangeResponse {
        val entity = findRequestOrThrow(requestId)
        val approver =
            userRepository
                .findById(approverId)
                .orElseThrow { ResourceNotFoundException("User not found: $approverId") }

        if (entity.user.id == approverId) {
            throw ForbiddenException("Cannot approve own work hour change request")
        }
        if (entity.status != WorkHourChangeStatus.PENDING) {
            throw BadRequestException("Only pending requests can be approved")
        }

        val oldResponse = entity.toResponse()
        entity.status = WorkHourChangeStatus.APPROVED
        entity.approvedBy = approver

        // Update employee config with new work hours
        val config =
            employeeConfigRepository.findByUserId(entity.user.id)
                ?: throw ResourceNotFoundException("Employee config not found for user: ${entity.user.id}")
        config.weeklyWorkHours = entity.requestedWeeklyHours
        entity.requestedDailyHours?.let { config.dailyWorkHours = it }
        employeeConfigRepository.save(config)

        val saved = workHourChangeRequestRepository.save(entity)
        auditService.logDataChange(
            approverId,
            "WORK_HOUR_CHANGE_APPROVED",
            "WorkHourChangeRequest",
            requestId,
            oldResponse,
            saved.toResponse(),
        )
        return saved.toResponse()
    }

    @Transactional
    fun rejectRequest(
        requestId: UUID,
        approverId: UUID,
        dto: RejectWorkHourChangeRequest,
    ): WorkHourChangeResponse {
        val entity = findRequestOrThrow(requestId)

        if (entity.user.id == approverId) {
            throw ForbiddenException("Cannot reject own work hour change request")
        }
        if (entity.status != WorkHourChangeStatus.PENDING) {
            throw BadRequestException("Only pending requests can be rejected")
        }

        val oldResponse = entity.toResponse()
        entity.status = WorkHourChangeStatus.REJECTED
        entity.rejectionReason = dto.rejectionReason

        val saved = workHourChangeRequestRepository.save(entity)
        auditService.logDataChange(
            approverId,
            "WORK_HOUR_CHANGE_REJECTED",
            "WorkHourChangeRequest",
            requestId,
            oldResponse,
            saved.toResponse(),
        )
        return saved.toResponse()
    }

    @Transactional
    fun cancelRequest(
        requestId: UUID,
        userId: UUID,
    ): WorkHourChangeResponse {
        val entity = findRequestOrThrow(requestId)
        if (entity.user.id != userId) {
            throw ForbiddenException("Cannot cancel another user's request")
        }
        if (entity.status != WorkHourChangeStatus.PENDING) {
            throw BadRequestException("Only pending requests can be cancelled")
        }

        val oldResponse = entity.toResponse()
        entity.status = WorkHourChangeStatus.CANCELLED

        val saved = workHourChangeRequestRepository.save(entity)
        auditService.logDataChange(
            userId,
            "WORK_HOUR_CHANGE_CANCELLED",
            "WorkHourChangeRequest",
            requestId,
            oldResponse,
            saved.toResponse(),
        )
        return saved.toResponse()
    }

    private fun findRequestOrThrow(requestId: UUID): WorkHourChangeRequestEntity =
        workHourChangeRequestRepository
            .findById(requestId)
            .orElseThrow { ResourceNotFoundException("Work hour change request not found: $requestId") }
}
