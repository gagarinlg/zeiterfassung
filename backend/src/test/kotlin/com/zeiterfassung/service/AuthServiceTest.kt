package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.AccountLockedException
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.LoginRequest
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.RefreshTokenRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.security.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock private lateinit var jwtService: JwtService

    @Mock private lateinit var auditService: AuditService

    @Mock private lateinit var httpRequest: HttpServletRequest

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService =
            AuthService(
                userRepository,
                refreshTokenRepository,
                jwtService,
                passwordEncoder,
                auditService,
                604800000L,
                900000L,
            )
    }

    @Test
    fun `login should throw UnauthorizedException for non-existent user`() {
        `when`(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty())
        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest("unknown@test.com", "password"), httpRequest)
        }
    }

    @Test
    fun `login should throw AccountLockedException for locked account`() {
        val user = createUser()
        user.lockedUntil = Instant.now().plusSeconds(3600)
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        assertThrows<AccountLockedException> {
            authService.login(LoginRequest(user.email, "password"), httpRequest)
        }
    }

    @Test
    fun `login should throw UnauthorizedException for wrong password`() {
        val user = createUser()
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest(user.email, "wrong-password"), httpRequest)
        }
    }

    @Test
    fun `login should lock account after 5 failed attempts`() {
        val user = createUser()
        user.failedLoginAttempts = 4
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest(user.email, "wrong-password"), httpRequest)
        }
        assertThat(user.lockedUntil).isNotNull()
    }

    private fun createUser(): UserEntity =
        UserEntity(
            id = UUID.randomUUID(),
            email = "test@test.com",
            passwordHash = passwordEncoder.encode("correct-password"),
            firstName = "Test",
            lastName = "User",
        )
}
