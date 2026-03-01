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
    val employeeNumber: String? = null,
)

data class ChangePasswordRequest(
    @field:NotBlank
    val currentPassword: String,
    @field:NotBlank
    @field:Size(min = 8)
    val newPassword: String,
    @field:NotBlank
    val confirmPassword: String,
)

data class ResetPasswordRequest(
    @field:NotBlank
    @field:Size(min = 8)
    val newPassword: String,
    @field:NotBlank
    val confirmPassword: String,
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

data class TotpSetupResponse(
    val secret: String,
    val qrCodeUri: String,
)

data class TotpVerifyRequest(
    @field:NotBlank
    val code: String,
)

data class PasswordResetLinkRequest(
    @field:NotBlank
    @field:Email
    val email: String,
)

data class PasswordResetConfirmRequest(
    @field:NotBlank
    val token: String,
    @field:NotBlank
    @field:Size(min = 8)
    val newPassword: String,
    @field:NotBlank
    val confirmPassword: String,
)

data class LdapConfigResponse(
    val enabled: Boolean,
    val url: String,
    val baseDn: String,
    val userSearchBase: String,
    val userSearchFilter: String,
    val groupSearchBase: String,
    val groupSearchFilter: String,
    val managerDn: String,
    val activeDirectoryMode: Boolean,
    val activeDirectoryDomain: String,
    val roleMapping: String,
    val emailAttribute: String,
    val firstNameAttribute: String,
    val lastNameAttribute: String,
    val employeeNumberAttribute: String,
)

data class UpdateLdapConfigRequest(
    val enabled: Boolean? = null,
    val url: String? = null,
    val baseDn: String? = null,
    val userSearchBase: String? = null,
    val userSearchFilter: String? = null,
    val groupSearchBase: String? = null,
    val groupSearchFilter: String? = null,
    val managerDn: String? = null,
    val managerPassword: String? = null,
    val activeDirectoryMode: Boolean? = null,
    val activeDirectoryDomain: String? = null,
    val roleMapping: String? = null,
    val emailAttribute: String? = null,
    val firstNameAttribute: String? = null,
    val lastNameAttribute: String? = null,
    val employeeNumberAttribute: String? = null,
)
