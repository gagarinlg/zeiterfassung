package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.PasswordResetConfirmRequest
import com.zeiterfassung.model.entity.PasswordResetTokenEntity
import com.zeiterfassung.repository.PasswordResetTokenRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class PasswordResetService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService,
    @param:Value("\${app.frontend-url:http://localhost:5173}") private val frontendUrl: String,
) {
    private val logger = LoggerFactory.getLogger(PasswordResetService::class.java)

    @Transactional
    fun requestPasswordReset(email: String) {
        val user = userRepository.findByEmail(email).orElse(null)
        if (user == null) {
            logger.debug("Password reset requested for unknown email: {}", email)
            return
        }

        val rawToken = UUID.randomUUID().toString()
        val tokenHash = jwtService.hashToken(rawToken)
        val expiresAt = Instant.now().plusSeconds(3600)

        val resetToken =
            PasswordResetTokenEntity(
                user = user,
                tokenHash = tokenHash,
                expiresAt = expiresAt,
            )
        passwordResetTokenRepository.save(resetToken)

        val resetLink = "$frontendUrl/reset-password?token=$rawToken"
        emailService.sendAsync(
            to = user.email,
            subject = "Passwort zurücksetzen - Zeiterfassung",
            body =
                """
                Hallo ${user.firstName},

                Sie haben eine Passwort-Zurücksetzung angefordert.

                Bitte klicken Sie auf den folgenden Link:
                $resetLink

                Dieser Link ist 1 Stunde gültig.

                Falls Sie diese Anfrage nicht gestellt haben, ignorieren Sie diese E-Mail.

                Mit freundlichen Grüßen,
                Zeiterfassung System
                """.trimIndent(),
        )

        auditService.logDataChange(user.id, "PASSWORD_RESET_REQUESTED", "User", user.id, null, null)
    }

    @Transactional
    fun confirmPasswordReset(request: PasswordResetConfirmRequest) {
        if (request.newPassword != request.confirmPassword) {
            throw UnauthorizedException("Passwords do not match")
        }

        val tokenHash = jwtService.hashToken(request.token)
        val resetToken =
            passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow { UnauthorizedException("Invalid or expired reset token") }

        if (resetToken.used) {
            throw UnauthorizedException("Reset token already used")
        }
        if (resetToken.expiresAt.isBefore(Instant.now())) {
            throw UnauthorizedException("Reset token expired")
        }

        val user = resetToken.user
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)

        resetToken.used = true
        passwordResetTokenRepository.save(resetToken)

        auditService.logDataChange(user.id, "PASSWORD_RESET_COMPLETED", "User", user.id, null, null)

        emailService.sendAsync(
            to = user.email,
            subject = "Passwort geändert - Zeiterfassung",
            body =
                """
                Hallo ${user.firstName},

                Ihr Passwort wurde erfolgreich geändert.

                Falls Sie diese Änderung nicht vorgenommen haben, kontaktieren Sie bitte umgehend Ihren Administrator.

                Mit freundlichen Grüßen,
                Zeiterfassung System
                """.trimIndent(),
        )
    }
}
