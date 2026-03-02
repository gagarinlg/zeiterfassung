package com.zeiterfassung.model.dto

import com.zeiterfassung.model.enums.WorkHourChangeStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateWorkHourChangeRequest(
    @field:NotNull
    val requestedWeeklyHours: BigDecimal,
    val requestedDailyHours: BigDecimal? = null,
    @field:NotNull
    val effectiveDate: LocalDate,
    val reason: String? = null,
)

data class RejectWorkHourChangeRequest(
    @field:NotBlank
    val rejectionReason: String,
)

data class WorkHourChangeResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val currentWeeklyHours: BigDecimal,
    val requestedWeeklyHours: BigDecimal,
    val currentDailyHours: BigDecimal?,
    val requestedDailyHours: BigDecimal?,
    val effectiveDate: LocalDate,
    val reason: String?,
    val status: WorkHourChangeStatus,
    val approvedById: UUID?,
    val approvedByName: String?,
    val rejectionReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
