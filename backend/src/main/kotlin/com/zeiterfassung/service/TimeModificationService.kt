package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateTimeModificationRequest
import com.zeiterfassung.model.dto.RejectTimeModificationRequest
import com.zeiterfassung.model.dto.TimeModificationResponse
import com.zeiterfassung.model.entity.TimeModificationRequestEntity
import com.zeiterfassung.model.enums.TimeModificationStatus
import com.zeiterfassung.repository.TimeEntryRepository
import com.zeiterfassung.repository.TimeModificationRequestRepository
import com.zeiterfassung.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.util.UUID

@Service
class TimeModificationService(
    private val timeModificationRequestRepository: TimeModificationRequestRepository,
    private val timeEntryRepository: TimeEntryRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val timeTrackingService: TimeTrackingService,
) {
    @Transactional
    fun createRequest(
        userId: UUID,
        dto: CreateTimeModificationRequest,
    ): TimeModificationResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val timeEntry =
            timeEntryRepository
                .findById(dto.timeEntryId)
                .orElseThrow { ResourceNotFoundException("Time entry not found: ${dto.timeEntryId}") }

        if (timeEntry.user.id != userId) {
            throw ForbiddenException("Cannot request modification for another user's time entry")
        }

        val pending = timeModificationRequestRepository.findByTimeEntryIdAndStatus(
            dto.timeEntryId,
            TimeModificationStatus.PENDING,
        )
        if (pending.isNotEmpty()) {
            throw ConflictException("A pending modification request already exists for this time entry")
        }

        val entity =
            TimeModificationRequestEntity(
                user = user,
                timeEntry = timeEntry,
                requestedTimestamp = dto.requestedTimestamp,
                requestedNotes = dto.requestedNotes,
                reason = dto.reason,
            )
        val saved = timeModificationRequestRepository.save(entity)
        auditService.logDataChange(
            userId,
            "TIME_MODIFICATION_CREATED",
            "TimeModificationRequest",
            saved.id,
            null,
            saved.toResponse(),
        )
        return saved.toResponse()
    }

    fun getMyRequests(
        userId: UUID,
        pageable: Pageable,
    ): Page<TimeModificationResponse> = timeModificationRequestRepository.findByUserId(userId, pageable).map { it.toResponse() }

    fun getPendingRequests(pageable: Pageable): Page<TimeModificationResponse> =
        timeModificationRequestRepository
            .findByStatus(TimeModificationStatus.PENDING, pageable)
            .map { it.toResponse() }

    @Transactional
    fun approveRequest(
        requestId: UUID,
        approverId: UUID,
    ): TimeModificationResponse {
        val entity = findRequestOrThrow(requestId)
        val approver =
            userRepository
                .findById(approverId)
                .orElseThrow { ResourceNotFoundException("User not found: $approverId") }

        if (entity.user.id == approverId) {
            throw ForbiddenException("Cannot approve own time modification request")
        }
        if (entity.status != TimeModificationStatus.PENDING) {
            throw BadRequestException("Only pending requests can be approved")
        }

        val oldResponse = entity.toResponse()
        entity.status = TimeModificationStatus.APPROVED
        entity.reviewedBy = approver

        // Apply the modification to the time entry
        val entry =
            timeEntryRepository
                .findById(entity.timeEntry.id)
                .orElseThrow { ResourceNotFoundException("Time entry not found: ${entity.timeEntry.id}") }

        val oldDate = entry.timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        entry.timestamp = entity.requestedTimestamp
        entity.requestedNotes?.let { entry.notes = it }
        entry.isModified = true
        entry.modifiedBy = approver
        timeEntryRepository.save(entry)

        // Recalculate daily summaries for both old and new dates
        val newDate = entity.requestedTimestamp.atZone(ZoneOffset.UTC).toLocalDate()
        timeTrackingService.recalculateDailySummary(entity.user.id, oldDate)
        if (oldDate != newDate) {
            timeTrackingService.recalculateDailySummary(entity.user.id, newDate)
        }

        val saved = timeModificationRequestRepository.save(entity)
        auditService.logDataChange(
            approverId,
            "TIME_MODIFICATION_APPROVED",
            "TimeModificationRequest",
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
        dto: RejectTimeModificationRequest,
    ): TimeModificationResponse {
        val entity = findRequestOrThrow(requestId)
        val reviewer =
            userRepository
                .findById(approverId)
                .orElseThrow { ResourceNotFoundException("User not found: $approverId") }

        if (entity.user.id == approverId) {
            throw ForbiddenException("Cannot reject own time modification request")
        }
        if (entity.status != TimeModificationStatus.PENDING) {
            throw BadRequestException("Only pending requests can be rejected")
        }

        val oldResponse = entity.toResponse()
        entity.status = TimeModificationStatus.REJECTED
        entity.reviewedBy = reviewer
        entity.rejectionReason = dto.rejectionReason

        val saved = timeModificationRequestRepository.save(entity)
        auditService.logDataChange(
            approverId,
            "TIME_MODIFICATION_REJECTED",
            "TimeModificationRequest",
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
    ): TimeModificationResponse {
        val entity = findRequestOrThrow(requestId)
        if (entity.user.id != userId) {
            throw ForbiddenException("Cannot cancel another user's request")
        }
        if (entity.status != TimeModificationStatus.PENDING) {
            throw BadRequestException("Only pending requests can be cancelled")
        }

        val oldResponse = entity.toResponse()
        entity.status = TimeModificationStatus.CANCELLED

        val saved = timeModificationRequestRepository.save(entity)
        auditService.logDataChange(
            userId,
            "TIME_MODIFICATION_CANCELLED",
            "TimeModificationRequest",
            requestId,
            oldResponse,
            saved.toResponse(),
        )
        return saved.toResponse()
    }

    private fun findRequestOrThrow(requestId: UUID): TimeModificationRequestEntity =
        timeModificationRequestRepository
            .findById(requestId)
            .orElseThrow { ResourceNotFoundException("Time modification request not found: $requestId") }
}
