package com.zeiterfassung.model.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateProjectRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val code: String,
    val description: String? = null,
    val costCenter: String? = null,
)

data class UpdateProjectRequest(
    val name: String? = null,
    val code: String? = null,
    val description: String? = null,
    val costCenter: String? = null,
    val isActive: Boolean? = null,
)

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val code: String,
    val description: String?,
    val costCenter: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateTimeAllocationRequest(
    @field:NotNull
    val projectId: UUID,
    @field:NotNull
    val date: LocalDate,
    @field:NotNull
    @field:Min(1)
    val minutes: Int,
    val notes: String? = null,
)

data class UpdateTimeAllocationRequest(
    val projectId: UUID? = null,
    val date: LocalDate? = null,
    @field:Min(1)
    val minutes: Int? = null,
    val notes: String? = null,
)

data class TimeAllocationResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val projectId: UUID,
    val projectName: String,
    val projectCode: String,
    val date: LocalDate,
    val minutes: Int,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
