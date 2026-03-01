package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.PasswordResetConfirmRequest
import com.zeiterfassung.model.entity.PasswordResetTokenEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.PasswordResetTokenRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.security.JwtService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PasswordResetServiceTest {
    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Mock private lateinit var emailService: EmailService

    @Mock private lateinit var jwtService: JwtService

    @Mock private lateinit var passwordEncoder: PasswordEncoder

    @Mock private lateinit var auditService: AuditService

    private lateinit var passwordResetService: PasswordResetService

    private val frontendUrl = "http://localhost:5173"

    @BeforeEach
    fun setUp() {
        passwordResetService =
            PasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                emailService,
                jwtService,
                passwordEncoder,
                auditService,
                frontendUrl,
            )
    }

    private fun makeUser(id: UUID = UUID.randomUUID()) =
        UserEntity(
            id = id,
            email = "user@test.com",
            passwordHash = "hash",
            firstName = "Test",
            lastName = "User",
        )

    // --- requestPasswordReset ---

    @Test
    fun `requestPasswordReset should send email and save token for known email`() {
        val user = makeUser()
        `when`(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user))
        `when`(jwtService.hashToken(anyString())).thenReturn("hashed-token")
        `when`(passwordResetTokenRepository.save(any<PasswordResetTokenEntity>())).thenAnswer { it.arguments[0] }

        passwordResetService.requestPasswordReset("user@test.com")

        verify(passwordResetTokenRepository).save(any<PasswordResetTokenEntity>())
        verify(emailService).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `requestPasswordReset should silently return for unknown email`() {
        `when`(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty())

        passwordResetService.requestPasswordReset("unknown@test.com")

        verify(passwordResetTokenRepository, never()).save(any<PasswordResetTokenEntity>())
        verify(emailService, never()).sendAsync(anyString(), anyString(), anyString())
    }

    // --- confirmPasswordReset ---

    @Test
    fun `confirmPasswordReset should reset password for valid token`() {
        val user = makeUser()
        val resetToken =
            PasswordResetTokenEntity(
                user = user,
                tokenHash = "hashed-token",
                expiresAt = Instant.now().plusSeconds(3600),
            )
        val request = PasswordResetConfirmRequest(
            token = "raw-token",
            newPassword = "newPassword1",
            confirmPassword = "newPassword1",
        )
        `when`(jwtService.hashToken("raw-token")).thenReturn("hashed-token")
        `when`(passwordResetTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(resetToken))
        `when`(passwordEncoder.encode("newPassword1")).thenReturn("encoded-hash")
        `when`(userRepository.save(any<UserEntity>())).thenReturn(user)
        `when`(passwordResetTokenRepository.save(any<PasswordResetTokenEntity>())).thenAnswer { it.arguments[0] }

        passwordResetService.confirmPasswordReset(request)

        assertThat(user.passwordHash).isEqualTo("encoded-hash")
        assertThat(resetToken.used).isTrue()
        verify(userRepository).save(user)
        verify(passwordResetTokenRepository).save(resetToken)
    }

    @Test
    fun `confirmPasswordReset should throw UnauthorizedException when passwords do not match`() {
        val request = PasswordResetConfirmRequest(
            token = "raw-token",
            newPassword = "password1",
            confirmPassword = "password2",
        )

        assertThrows<UnauthorizedException> {
            passwordResetService.confirmPasswordReset(request)
        }
    }

    @Test
    fun `confirmPasswordReset should throw UnauthorizedException for invalid token`() {
        val request = PasswordResetConfirmRequest(
            token = "invalid-token",
            newPassword = "newPassword1",
            confirmPassword = "newPassword1",
        )
        `when`(jwtService.hashToken("invalid-token")).thenReturn("hashed-invalid")
        `when`(passwordResetTokenRepository.findByTokenHash("hashed-invalid")).thenReturn(Optional.empty())

        assertThrows<UnauthorizedException> {
            passwordResetService.confirmPasswordReset(request)
        }
    }

    @Test
    fun `confirmPasswordReset should throw UnauthorizedException for expired token`() {
        val user = makeUser()
        val resetToken =
            PasswordResetTokenEntity(
                user = user,
                tokenHash = "hashed-token",
                expiresAt = Instant.now().minusSeconds(60),
            )
        val request = PasswordResetConfirmRequest(
            token = "raw-token",
            newPassword = "newPassword1",
            confirmPassword = "newPassword1",
        )
        `when`(jwtService.hashToken("raw-token")).thenReturn("hashed-token")
        `when`(passwordResetTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(resetToken))

        assertThrows<UnauthorizedException> {
            passwordResetService.confirmPasswordReset(request)
        }
    }

    @Test
    fun `confirmPasswordReset should throw UnauthorizedException for already used token`() {
        val user = makeUser()
        val resetToken =
            PasswordResetTokenEntity(
                user = user,
                tokenHash = "hashed-token",
                expiresAt = Instant.now().plusSeconds(3600),
            ).apply { used = true }
        val request = PasswordResetConfirmRequest(
            token = "raw-token",
            newPassword = "newPassword1",
            confirmPassword = "newPassword1",
        )
        `when`(jwtService.hashToken("raw-token")).thenReturn("hashed-token")
        `when`(passwordResetTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(resetToken))

        assertThrows<UnauthorizedException> {
            passwordResetService.confirmPasswordReset(request)
        }
    }
}
