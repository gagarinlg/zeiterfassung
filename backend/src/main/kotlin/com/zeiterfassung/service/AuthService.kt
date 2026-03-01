package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.AccountLockedException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.AuthResponse
import com.zeiterfassung.model.dto.LoginRequest
import com.zeiterfassung.model.dto.RefreshRequest
import com.zeiterfassung.model.dto.UserResponse
import com.zeiterfassung.model.entity.RefreshTokenEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.RefreshTokenRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.security.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService,
    private val totpService: TotpService,
    @param:Value("\${app.jwt.refresh-token-expiration-ms}") private val refreshTokenExpMs: Long,
    @param:Value("\${app.jwt.access-token-expiration-ms}") private val accessTokenExpMs: Long,
) {
    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MINUTES = 15L
    }

    @Transactional
    fun login(
        request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): AuthResponse {
        val user =
            userRepository
                .findByEmail(request.email)
                .orElseThrow { UnauthorizedException("Invalid credentials") }

        if (user.lockedUntil != null && user.lockedUntil!!.isAfter(Instant.now())) {
            throw AccountLockedException("Account is locked. Please try again later.")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            user.failedLoginAttempts++
            if (user.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                user.lockedUntil = Instant.now().plusSeconds(LOCKOUT_MINUTES * 60)
            }
            userRepository.save(user)
            throw UnauthorizedException("Invalid credentials")
        }

        user.failedLoginAttempts = 0
        user.lockedUntil = null
        userRepository.save(user)

        // TOTP verification
        if (user.totpEnabled) {
            if (request.totpCode.isNullOrBlank()) {
                throw UnauthorizedException("TOTP code required")
            }
            if (!totpService.verifyCode(user.totpSecret!!, request.totpCode)) {
                throw UnauthorizedException("Invalid TOTP code")
            }
        }

        val roles = user.roles.map { it.name }
        val permissions = user.roles.flatMap { it.permissions.map { p -> p.name } }.distinct()

        val accessToken = jwtService.generateAccessToken(user.id.toString(), user.email, roles, permissions)
        val rawRefreshToken = jwtService.generateRefreshToken()
        val tokenHash = jwtService.hashToken(rawRefreshToken)

        val refreshToken =
            RefreshTokenEntity(
                user = user,
                tokenHash = tokenHash,
                expiresAt = Instant.now().plusMillis(refreshTokenExpMs),
            )
        refreshTokenRepository.save(refreshToken)

        auditService.logLogin(user.id, httpRequest.remoteAddr, httpRequest.getHeader("User-Agent"))

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            expiresIn = accessTokenExpMs / 1000,
            user = user.toUserResponse(),
        )
    }

    @Transactional
    fun refreshToken(request: RefreshRequest): AuthResponse {
        val tokenHash = jwtService.hashToken(request.refreshToken)
        val storedToken =
            refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow { UnauthorizedException("Invalid refresh token") }

        if (storedToken.isRevoked) {
            refreshTokenRepository.revokeAllByUserId(storedToken.user.id)
            throw UnauthorizedException("Refresh token reuse detected. All sessions revoked.")
        }

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            storedToken.isRevoked = true
            refreshTokenRepository.save(storedToken)
            throw UnauthorizedException("Refresh token expired")
        }

        val user = storedToken.user
        storedToken.isRevoked = true

        val roles = user.roles.map { it.name }
        val permissions = user.roles.flatMap { it.permissions.map { p -> p.name } }.distinct()

        val accessToken = jwtService.generateAccessToken(user.id.toString(), user.email, roles, permissions)
        val rawRefreshToken = jwtService.generateRefreshToken()
        val newTokenHash = jwtService.hashToken(rawRefreshToken)

        val newRefreshToken =
            RefreshTokenEntity(
                user = user,
                tokenHash = newTokenHash,
                expiresAt = Instant.now().plusMillis(refreshTokenExpMs),
            )
        val savedNew = refreshTokenRepository.save(newRefreshToken)
        storedToken.replacedBy = savedNew
        refreshTokenRepository.save(storedToken)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            expiresIn = accessTokenExpMs / 1000,
            user = user.toUserResponse(),
        )
    }

    @Transactional
    fun logout(
        userId: UUID,
        refreshToken: String?,
        httpRequest: HttpServletRequest,
    ) {
        if (refreshToken != null) {
            val tokenHash = jwtService.hashToken(refreshToken)
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent { token ->
                token.isRevoked = true
                refreshTokenRepository.save(token)
            }
        }
        auditService.logLogout(userId, httpRequest.remoteAddr, httpRequest.getHeader("User-Agent"))
    }

    @Transactional
    fun logoutAll(
        userId: UUID,
        httpRequest: HttpServletRequest,
    ) {
        refreshTokenRepository.revokeAllByUserId(userId)
        auditService.logLogout(userId, httpRequest.remoteAddr, httpRequest.getHeader("User-Agent"))
    }

    fun getCurrentUser(userId: UUID): UserResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found") }
        return user.toUserResponse()
    }

    private fun UserEntity.toUserResponse(): UserResponse {
        val roles = this.roles.map { it.name }
        val permissions = this.roles.flatMap { it.permissions.map { p -> p.name } }.distinct()
        return UserResponse(
            id = this.id.toString(),
            email = this.email,
            firstName = this.firstName,
            lastName = this.lastName,
            employeeNumber = this.employeeNumber,
            phone = this.phone,
            photoUrl = this.photoUrl,
            managerId = this.manager?.id?.toString(),
            isActive = this.isActive,
            roles = roles,
            permissions = permissions,
            dateFormat = this.dateFormat,
            timeFormat = this.timeFormat,
            totpEnabled = this.totpEnabled,
        )
    }
}
