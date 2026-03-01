package com.zeiterfassung.model.dto

import com.zeiterfassung.model.enums.SickLeaveStatus
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateSickLeaveRequest(
    @field:NotNull
    val startDate: LocalDate,
    @field:NotNull
    val endDate: LocalDate,
    val notes: String? = null,
)

data class UpdateSickLeaveRequest(
    val endDate: LocalDate? = null,
    val hasCertificate: Boolean? = null,
    val notes: String? = null,
)

data class SickLeaveResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: SickLeaveStatus,
    val hasCertificate: Boolean,
    val certificateSubmittedAt: Instant?,
    val notes: String?,
    val reportedById: UUID?,
    val reportedByName: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
