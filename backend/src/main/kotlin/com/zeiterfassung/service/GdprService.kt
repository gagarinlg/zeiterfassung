package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.GdprAuditEntry
import com.zeiterfassung.model.dto.GdprBusinessTrip
import com.zeiterfassung.model.dto.GdprDataExportResponse
import com.zeiterfassung.model.dto.GdprDeletionResponse
import com.zeiterfassung.model.dto.GdprPersonalInfo
import com.zeiterfassung.model.dto.GdprSickLeave
import com.zeiterfassung.model.dto.GdprTimeEntry
import com.zeiterfassung.model.dto.GdprVacationRequest
import com.zeiterfassung.repository.AuditLogRepository
import com.zeiterfassung.repository.BusinessTripRepository
import com.zeiterfassung.repository.RefreshTokenRepository
import com.zeiterfassung.repository.SickLeaveRepository
import com.zeiterfassung.repository.TimeEntryRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.repository.VacationRequestRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class GdprService(
    private val userRepository: UserRepository,
    private val timeEntryRepository: TimeEntryRepository,
    private val vacationRequestRepository: VacationRequestRepository,
    private val sickLeaveRepository: SickLeaveRepository,
    private val businessTripRepository: BusinessTripRepository,
    private val auditLogRepository: AuditLogRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val auditService: AuditService,
) {
    fun exportUserData(userId: UUID): GdprDataExportResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val personalInfo =
            GdprPersonalInfo(
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                employeeNumber = user.employeeNumber,
                phone = user.phone,
                dateFormat = user.dateFormat,
                timeFormat = user.timeFormat,
                createdAt = user.createdAt,
            )

        val timeEntries =
            timeEntryRepository.findByUserIdOrderByTimestampAsc(userId).map { entry ->
                GdprTimeEntry(
                    entryType = entry.entryType.name,
                    timestamp = entry.timestamp,
                    source = entry.source.name,
                    notes = entry.notes,
                )
            }

        val vacationRequests =
            vacationRequestRepository.findByUserId(userId, Pageable.unpaged()).content.map { vr ->
                GdprVacationRequest(
                    startDate = vr.startDate.toString(),
                    endDate = vr.endDate.toString(),
                    totalDays = vr.totalDays.toPlainString(),
                    status = vr.status.name,
                    notes = vr.notes,
                    createdAt = vr.createdAt,
                )
            }

        val sickLeaves =
            sickLeaveRepository.findByUserId(userId, Pageable.unpaged()).content.map { sl ->
                GdprSickLeave(
                    startDate = sl.startDate.toString(),
                    endDate = sl.endDate.toString(),
                    status = sl.status.name,
                    hasCertificate = sl.hasCertificate,
                    notes = sl.notes,
                    createdAt = sl.createdAt,
                )
            }

        val businessTrips =
            businessTripRepository.findByUserId(userId, Pageable.unpaged()).content.map { bt ->
                GdprBusinessTrip(
                    startDate = bt.startDate.toString(),
                    endDate = bt.endDate.toString(),
                    destination = bt.destination,
                    purpose = bt.purpose,
                    status = bt.status.name,
                    notes = bt.notes,
                    createdAt = bt.createdAt,
                )
            }

        val auditLog =
            auditLogRepository.findByUserIdOrderByCreatedAtAsc(userId).map { entry ->
                GdprAuditEntry(
                    action = entry.action,
                    timestamp = entry.createdAt,
                    entityType = entry.entityType,
                )
            }

        return GdprDataExportResponse(
            exportedAt = Instant.now(),
            personalInfo = personalInfo,
            timeEntries = timeEntries,
            vacationRequests = vacationRequests,
            sickLeaves = sickLeaves,
            businessTrips = businessTrips,
            auditLog = auditLog,
        )
    }

    @Transactional
    fun requestDeletion(
        userId: UUID,
        actorId: UUID,
    ): GdprDeletionResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val anonymizedEmail = "deleted-${UUID.randomUUID()}@anonymized.invalid"

        user.isDeleted = true
        user.isActive = false
        user.firstName = "Deleted"
        user.lastName = "User"
        user.email = anonymizedEmail
        user.phone = null
        user.rfidTagId = null
        user.photoUrl = null
        user.employeeNumber = null
        user.totpSecret = null
        user.totpEnabled = false

        userRepository.save(user)
        refreshTokenRepository.revokeAllByUserId(userId)

        auditService.logDataChange(actorId, "GDPR_USER_DELETED", "User", userId, null, null)

        return GdprDeletionResponse(
            status = "COMPLETED",
            message = "User data has been anonymized and account deactivated",
            deletedAt = Instant.now(),
        )
    }
}
