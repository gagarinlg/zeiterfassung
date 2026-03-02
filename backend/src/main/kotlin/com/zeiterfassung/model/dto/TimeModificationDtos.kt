package com.zeiterfassung.model.dto

import com.zeiterfassung.model.enums.TimeModificationStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateTimeModificationRequest(
    @field:NotNull
    val timeEntryId: UUID,
    @field:NotNull
    val requestedTimestamp: Instant,
    val requestedNotes: String? = null,
    @field:NotBlank
    val reason: String,
)

data class RejectTimeModificationRequest(
    @field:NotBlank
    val rejectionReason: String,
)

data class TimeModificationResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val timeEntryId: UUID,
    val entryType: String,
    val originalTimestamp: Instant,
    val requestedTimestamp: Instant,
    val requestedNotes: String?,
    val reason: String,
    val status: TimeModificationStatus,
    val reviewedById: UUID?,
    val reviewedByName: String?,
    val rejectionReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
