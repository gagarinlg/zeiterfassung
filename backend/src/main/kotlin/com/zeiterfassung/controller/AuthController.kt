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
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val totpService: TotpService,
    private val passwordResetService: PasswordResetService,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<AuthResponse> {
        val response = authService.login(request, httpRequest)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userId: String,
        @RequestBody(required = false) body: Map<String, String>?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {
        authService.logout(UUID.fromString(userId), body?.get("refreshToken"), httpRequest)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/logout-all")
    fun logoutAll(
        @AuthenticationPrincipal userId: String,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {
        authService.logoutAll(UUID.fromString(userId), httpRequest)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<UserResponse> {
        val user = authService.getCurrentUser(UUID.fromString(userId))
        return ResponseEntity.ok(user)
    }

    @PostMapping("/totp/setup")
    fun setupTotp(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<TotpSetupResponse> = ResponseEntity.ok(totpService.generateSetup(UUID.fromString(userId)))

    @PostMapping("/totp/enable")
    fun enableTotp(
        @RequestBody request: TotpVerifyRequest,
        @AuthenticationPrincipal userId: String,
        @RequestParam secret: String,
    ): ResponseEntity<Void> {
        totpService.enableTotp(UUID.fromString(userId), secret, request.code)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/totp/disable")
    fun disableTotp(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        totpService.disableTotp(UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/reset-request")
    fun requestPasswordReset(
        @Valid @RequestBody request: PasswordResetLinkRequest,
    ): ResponseEntity<Void> {
        passwordResetService.requestPasswordReset(request.email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/password/reset-confirm")
    fun confirmPasswordReset(
        @Valid @RequestBody request: PasswordResetConfirmRequest,
    ): ResponseEntity<Void> {
        passwordResetService.confirmPasswordReset(request)
        return ResponseEntity.noContent().build()
    }
}
