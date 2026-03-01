package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.AuthResponse
import com.zeiterfassung.model.dto.LoginRequest
import com.zeiterfassung.model.dto.PasswordResetConfirmRequest
import com.zeiterfassung.model.dto.PasswordResetLinkRequest
import com.zeiterfassung.model.dto.RefreshRequest
import com.zeiterfassung.model.dto.TotpSetupResponse
import com.zeiterfassung.model.dto.TotpVerifyRequest
import com.zeiterfassung.model.dto.UserResponse
import com.zeiterfassung.service.AuthService
import com.zeiterfassung.service.PasswordResetService
import com.zeiterfassung.service.TotpService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
class AuthController(
    private val authService: AuthService,
    private val totpService: TotpService,
    private val passwordResetService: PasswordResetService,
) {
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Login with email and password. Returns JWT tokens.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials or TOTP required")
    @ApiResponse(responseCode = "423", description = "Account locked after too many failed attempts")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<AuthResponse> {
        val response = authService.login(request, httpRequest)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for new JWT tokens.")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current session", description = "Invalidate the current refresh token.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    fun logout(
        @AuthenticationPrincipal userId: String,
        @RequestBody(required = false) body: Map<String, String>?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {
        authService.logout(UUID.fromString(userId), body?.get("refreshToken"), httpRequest)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all sessions", description = "Invalidate all refresh tokens for the current user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "All sessions terminated")
    fun logoutAll(
        @AuthenticationPrincipal userId: String,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {
        authService.logoutAll(UUID.fromString(userId), httpRequest)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "User profile returned")
    fun getCurrentUser(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<UserResponse> {
        val user = authService.getCurrentUser(UUID.fromString(userId))
        return ResponseEntity.ok(user)
    }

    @PostMapping("/totp/setup")
    @Operation(summary = "Set up TOTP 2FA", description = "Generate a new TOTP secret and provisioning URI for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "TOTP setup data returned")
    fun setupTotp(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<TotpSetupResponse> = ResponseEntity.ok(totpService.generateSetup(UUID.fromString(userId)))

    @PostMapping("/totp/enable")
    @Operation(summary = "Enable TOTP 2FA", description = "Verify and enable TOTP two-factor authentication.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "TOTP enabled successfully")
    @ApiResponse(responseCode = "400", description = "Invalid TOTP code")
    fun enableTotp(
        @Valid @RequestBody request: TotpVerifyRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        totpService.enableTotp(UUID.fromString(userId), request.secret, request.code)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/totp/disable")
    @Operation(summary = "Disable TOTP 2FA", description = "Disable two-factor authentication for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "TOTP disabled successfully")
    fun disableTotp(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        totpService.disableTotp(UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "Request password reset", description = "Send a password reset link to the specified email address.")
    @ApiResponse(responseCode = "200", description = "Reset email sent if the account exists")
    fun requestPasswordReset(
        @Valid @RequestBody request: PasswordResetLinkRequest,
    ): ResponseEntity<Void> {
        passwordResetService.requestPasswordReset(request.email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/password/reset-confirm")
    @Operation(summary = "Confirm password reset", description = "Reset the password using a valid token.")
    @ApiResponse(responseCode = "204", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    fun confirmPasswordReset(
        @Valid @RequestBody request: PasswordResetConfirmRequest,
    ): ResponseEntity<Void> {
        passwordResetService.confirmPasswordReset(request)
        return ResponseEntity.noContent().build()
    }
}
