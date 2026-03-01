package com.zeiterfassung.model.dto

import com.zeiterfassung.model.enums.BusinessTripStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateBusinessTripRequest(
    @field:NotNull
    val startDate: LocalDate,
    @field:NotNull
    val endDate: LocalDate,
    @field:NotBlank
    val destination: String,
    @field:NotBlank
    val purpose: String,
    val notes: String? = null,
    val estimatedCost: BigDecimal? = null,
    val costCenter: String? = null,
)

data class UpdateBusinessTripRequest(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val destination: String? = null,
    val purpose: String? = null,
    val notes: String? = null,
    val estimatedCost: BigDecimal? = null,
    val costCenter: String? = null,
    val actualCost: BigDecimal? = null,
)

data class ApproveBusinessTripRequest(
    val notes: String? = null,
)

data class RejectBusinessTripRequest(
    @field:NotBlank
    val rejectionReason: String,
)

data class BusinessTripResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val destination: String,
    val purpose: String,
    val status: BusinessTripStatus,
    val approvedById: UUID?,
    val approvedByName: String?,
    val rejectionReason: String?,
    val notes: String?,
    val estimatedCost: BigDecimal?,
    val actualCost: BigDecimal?,
    val costCenter: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
