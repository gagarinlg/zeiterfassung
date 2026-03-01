package com.zeiterfassung.model.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
    val totpCode: String? = null,
)

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse,
)

data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val employeeNumber: String?,
    val phone: String?,
    val photoUrl: String?,
    val managerId: String?,
    val isActive: Boolean,
    val roles: List<String>,
    val permissions: List<String>,
    val dateFormat: String?,
    val timeFormat: String?,
    val totpEnabled: Boolean = false,
)
