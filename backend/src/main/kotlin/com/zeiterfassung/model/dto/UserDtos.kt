package com.zeiterfassung.model.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 8)
    val password: String,
    @field:NotBlank
    val firstName: String,
    @field:NotBlank
    val lastName: String,
    val employeeNumber: String? = null,
    val phone: String? = null,
    val managerId: String? = null,
    val roles: List<String> = listOf("EMPLOYEE"),
)

data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val managerId: String? = null,
    val isActive: Boolean? = null,
    val dateFormat: String? = null,
    val timeFormat: String? = null,
)

data class ChangePasswordRequest(
    @field:NotBlank
    val currentPassword: String,
    @field:NotBlank
    @field:Size(min = 8)
    val newPassword: String,
)

data class ResetPasswordRequest(
    @field:NotBlank
    @field:Size(min = 8)
    val newPassword: String,
)

data class AssignRolesRequest(
    val roles: List<String>,
)

data class UpdateRfidRequest(
    val rfidTagId: String?,
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int,
)
