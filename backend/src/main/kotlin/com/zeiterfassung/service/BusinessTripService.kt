package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.ApproveBusinessTripRequest
import com.zeiterfassung.model.dto.BusinessTripResponse
import com.zeiterfassung.model.dto.CreateBusinessTripRequest
import com.zeiterfassung.model.dto.RejectBusinessTripRequest
import com.zeiterfassung.model.dto.UpdateBusinessTripRequest
import com.zeiterfassung.model.entity.BusinessTripEntity
import com.zeiterfassung.model.enums.BusinessTripStatus
import com.zeiterfassung.repository.BusinessTripRepository
import com.zeiterfassung.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BusinessTripService(
    private val businessTripRepository: BusinessTripRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val notificationService: NotificationService,
) {
    @Transactional
    fun createTrip(
        userId: UUID,
        dto: CreateBusinessTripRequest,
    ): BusinessTripResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        if (dto.startDate.isAfter(dto.endDate)) {
            throw BadRequestException("business_trip.error.start_after_end")
        }

        val overlapping = businessTripRepository.findOverlapping(userId, dto.startDate, dto.endDate, null)
        if (overlapping.isNotEmpty()) {
            throw ConflictException("business_trip.error.overlap")
        }

        val entity =
            BusinessTripEntity(
                user = user,
                startDate = dto.startDate,
                endDate = dto.endDate,
                destination = dto.destination,
                purpose = dto.purpose,
                notes = dto.notes,
                estimatedCost = dto.estimatedCost,
                costCenter = dto.costCenter,
            )
        val saved = businessTripRepository.save(entity)
        auditService.logDataChange(userId, "BUSINESS_TRIP_CREATED", "BusinessTrip", saved.id, null, saved.toResponse())

        // Notify manager
        user.manager?.let { manager ->
            if (manager.email.isNotBlank()) {
                notificationService.notifyBusinessTripRequested(saved, manager)
            }
        }

        return saved.toResponse()
    }

    @Transactional
    fun updateTrip(
        tripId: UUID,
        userId: UUID,
        dto: UpdateBusinessTripRequest,
    ): BusinessTripResponse {
        val entity = findOrThrow(tripId)
        if (entity.user.id != userId) {
            throw ForbiddenException("business_trip.error.not_owner")
        }
        if (entity.status != BusinessTripStatus.REQUESTED) {
            throw BadRequestException("business_trip.error.not_requested")
        }

        val newStart = dto.startDate ?: entity.startDate
        val newEnd = dto.endDate ?: entity.endDate
        if (newStart.isAfter(newEnd)) {
            throw BadRequestException("business_trip.error.start_after_end")
        }

        val overlapping = businessTripRepository.findOverlapping(userId, newStart, newEnd, tripId)
        if (overlapping.isNotEmpty()) {
            throw ConflictException("business_trip.error.overlap")
        }

        val oldResponse = entity.toResponse()
        entity.startDate = newStart
        entity.endDate = newEnd
        dto.destination?.let { entity.destination = it }
        dto.purpose?.let { entity.purpose = it }
        dto.notes?.let { entity.notes = it }
        dto.estimatedCost?.let { entity.estimatedCost = it }
        dto.costCenter?.let { entity.costCenter = it }
        dto.actualCost?.let { entity.actualCost = it }

        val saved = businessTripRepository.save(entity)
        auditService.logDataChange(userId, "BUSINESS_TRIP_UPDATED", "BusinessTrip", saved.id, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun cancelTrip(
        tripId: UUID,
        userId: UUID,
    ) {
        val entity = findOrThrow(tripId)
        if (entity.user.id != userId) {
            throw ForbiddenException("business_trip.error.not_owner")
        }
        if (entity.status == BusinessTripStatus.CANCELLED) {
            throw BadRequestException("business_trip.error.already_cancelled")
        }
        if (entity.status == BusinessTripStatus.COMPLETED) {
            throw BadRequestException("business_trip.error.already_completed")
        }

        val oldResponse = entity.toResponse()
        entity.status = BusinessTripStatus.CANCELLED
        businessTripRepository.save(entity)
        auditService.logDataChange(userId, "BUSINESS_TRIP_CANCELLED", "BusinessTrip", tripId, oldResponse, entity.toResponse())
    }

    @Transactional
    fun approveTrip(
        tripId: UUID,
        approverId: UUID,
        dto: ApproveBusinessTripRequest,
    ): BusinessTripResponse {
        val entity = findOrThrow(tripId)
        val approver =
            userRepository
                .findById(approverId)
                .orElseThrow { ResourceNotFoundException("User not found: $approverId") }

        if (entity.user.id == approverId) {
            throw ForbiddenException("business_trip.error.cannot_approve_own")
        }
        if (entity.status != BusinessTripStatus.REQUESTED) {
            throw BadRequestException("business_trip.error.not_requested")
        }

        val oldResponse = entity.toResponse()
        entity.status = BusinessTripStatus.APPROVED
        entity.approvedBy = approver
        dto.notes?.let { entity.notes = it }

        val saved = businessTripRepository.save(entity)
        auditService.logDataChange(approverId, "BUSINESS_TRIP_APPROVED", "BusinessTrip", tripId, oldResponse, saved.toResponse())
        notificationService.notifyBusinessTripApproved(saved, "${approver.firstName} ${approver.lastName}")
        return saved.toResponse()
    }

    @Transactional
    fun rejectTrip(
        tripId: UUID,
        approverId: UUID,
        dto: RejectBusinessTripRequest,
    ): BusinessTripResponse {
        val entity = findOrThrow(tripId)

        if (entity.user.id == approverId) {
            throw ForbiddenException("business_trip.error.cannot_approve_own")
        }
        if (entity.status != BusinessTripStatus.REQUESTED) {
            throw BadRequestException("business_trip.error.not_requested")
        }

        val oldResponse = entity.toResponse()
        entity.status = BusinessTripStatus.REJECTED
        entity.rejectionReason = dto.rejectionReason

        val saved = businessTripRepository.save(entity)
        auditService.logDataChange(approverId, "BUSINESS_TRIP_REJECTED", "BusinessTrip", tripId, oldResponse, saved.toResponse())
        notificationService.notifyBusinessTripRejected(saved, dto.rejectionReason)
        return saved.toResponse()
    }

    @Transactional
    fun completeTrip(
        tripId: UUID,
        userId: UUID,
        actualCost: java.math.BigDecimal?,
    ): BusinessTripResponse {
        val entity = findOrThrow(tripId)
        if (entity.user.id != userId) {
            throw ForbiddenException("business_trip.error.not_owner")
        }
        if (entity.status != BusinessTripStatus.APPROVED) {
            throw BadRequestException("business_trip.error.not_approved")
        }

        val oldResponse = entity.toResponse()
        entity.status = BusinessTripStatus.COMPLETED
        actualCost?.let { entity.actualCost = it }

        val saved = businessTripRepository.save(entity)
        auditService.logDataChange(userId, "BUSINESS_TRIP_COMPLETED", "BusinessTrip", tripId, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    fun getTrip(tripId: UUID): BusinessTripResponse = findOrThrow(tripId).toResponse()

    fun getUserTrips(
        userId: UUID,
        pageable: Pageable,
    ): Page<BusinessTripResponse> = businessTripRepository.findByUserId(userId, pageable).map { it.toResponse() }

    fun getPendingTrips(
        managerId: UUID,
        pageable: Pageable,
    ): Page<BusinessTripResponse> {
        val manager =
            userRepository
                .findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val subordinateIds = manager.subordinates.map { it.id }.toMutableList()
        userRepository.findBySubstituteId(managerId).forEach { m ->
            subordinateIds.addAll(m.subordinates.map { it.id })
        }
        if (subordinateIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable)
        }
        return businessTripRepository
            .findByStatusAndUserIdIn(BusinessTripStatus.REQUESTED, subordinateIds, pageable)
            .map { it.toResponse() }
    }

    private fun findOrThrow(id: UUID): BusinessTripEntity =
        businessTripRepository
            .findById(id)
            .orElseThrow { ResourceNotFoundException("Business trip not found: $id") }

    private fun BusinessTripEntity.toResponse() =
        BusinessTripResponse(
            id = id,
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            startDate = startDate,
            endDate = endDate,
            destination = destination,
            purpose = purpose,
            status = status,
            approvedById = approvedBy?.id,
            approvedByName = approvedBy?.let { "${it.firstName} ${it.lastName}" },
            rejectionReason = rejectionReason,
            notes = notes,
            estimatedCost = estimatedCost,
            actualCost = actualCost,
            costCenter = costCenter,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
