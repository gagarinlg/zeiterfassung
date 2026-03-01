package com.zeiterfassung.model.dto

import com.zeiterfassung.model.enums.VacationStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateVacationRequest(
    @field:NotNull
    val startDate: LocalDate,
    @field:NotNull
    val endDate: LocalDate,
    val isHalfDayStart: Boolean = false,
    val isHalfDayEnd: Boolean = false,
    val notes: String? = null,
)

data class UpdateVacationRequest(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isHalfDayStart: Boolean? = null,
    val isHalfDayEnd: Boolean? = null,
    val notes: String? = null,
)

data class ApproveVacationRequest(
    val notes: String? = null,
)

data class RejectVacationRequest(
    @field:NotBlank
    val rejectionReason: String,
)

data class VacationRequestResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isHalfDayStart: Boolean,
    val isHalfDayEnd: Boolean,
    val totalDays: BigDecimal,
    val status: VacationStatus,
    val approvedById: UUID?,
    val approvedByName: String?,
    val rejectionReason: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class VacationBalanceResponse(
    val id: UUID,
    val userId: UUID,
    val year: Int,
    val totalDays: BigDecimal,
    val usedDays: BigDecimal,
    val carriedOverDays: BigDecimal,
    val remainingDays: BigDecimal,
    val pendingDays: BigDecimal,
)

data class PublicHolidayResponse(
    val id: UUID,
    val date: LocalDate,
    val name: String,
    val stateCode: String?,
    val isRecurring: Boolean,
)

data class VacationCalendarResponse(
    val year: Int,
    val month: Int,
    val ownRequests: List<VacationRequestResponse>,
    val teamRequests: List<VacationRequestResponse>,
    val publicHolidays: List<PublicHolidayResponse>,
)

data class TeamVacationOverviewResponse(
    val pendingCount: Int,
    val pendingRequests: List<VacationRequestResponse>,
)

data class SetVacationBalanceRequest(
    @field:NotNull
    val totalDays: BigDecimal? = null,
    val usedDays: BigDecimal? = null,
    val carriedOverDays: BigDecimal? = null,
)
