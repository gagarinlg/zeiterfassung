package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.AccountLockedException
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.LoginRequest
import com.zeiterfassung.model.dto.RefreshRequest
import com.zeiterfassung.model.entity.RefreshTokenEntity
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthServiceEdgeCasesTest2 {
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Mock private lateinit var jwtService: JwtService
    @Mock private lateinit var auditService: AuditService
    @Mock private lateinit var totpService: TotpService
    @Mock private lateinit var httpRequest: HttpServletRequest

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userRepository, refreshTokenRepository, jwtService, passwordEncoder,
            auditService, totpService, 604800000L, 900000L,
        )
    }

    // ---- Login success path ----

    @Test
    fun `login success returns access and refresh tokens`() {
        val user = createUser()
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        `when`(jwtService.generateAccessToken(anyString(), anyString(), anyList(), anyList())).thenReturn("access-jwt")
        `when`(jwtService.generateRefreshToken()).thenReturn("refresh-uuid")
        `when`(jwtService.hashToken("refresh-uuid")).thenReturn("refresh-hash")
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: createDummyToken(user)))
            .thenAnswer { it.arguments[0] }
        `when`(httpRequest.remoteAddr).thenReturn("192.168.1.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("JUnit/5")

        val result = authService.login(LoginRequest(user.email, "correct-password"), httpRequest)

        assertThat(result.accessToken).isEqualTo("access-jwt")
        assertThat(result.refreshToken).isEqualTo("refresh-uuid")
        assertThat(result.expiresIn).isEqualTo(900L)
        assertThat(result.user.email).isEqualTo(user.email)
        verify(auditService).logLogin(user.id, "192.168.1.1", "JUnit/5")
    }

    @Test
    fun `login clears lockout on success`() {
        val user = createUser()
        user.failedLoginAttempts = 4
        user.lockedUntil = Instant.now().minusSeconds(1) // Expired lock
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        `when`(jwtService.generateAccessToken(anyString(), anyString(), anyList(), anyList())).thenReturn("t")
        `when`(jwtService.generateRefreshToken()).thenReturn("r")
        `when`(jwtService.hashToken("r")).thenReturn("h")
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: createDummyToken(user)))
            .thenAnswer { it.arguments[0] }
        `when`(httpRequest.remoteAddr).thenReturn("127.0.0.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("Test")

        authService.login(LoginRequest(user.email, "correct-password"), httpRequest)

        assertThat(user.failedLoginAttempts).isEqualTo(0)
        assertThat(user.lockedUntil).isNull()
    }

    // ---- Account lockout ----

    @Test
    fun `login increments failed attempts on wrong password`() {
        val user = createUser()
        user.failedLoginAttempts = 0
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)

        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest(user.email, "wrong"), httpRequest)
        }
        assertThat(user.failedLoginAttempts).isEqualTo(1)
        assertThat(user.lockedUntil).isNull()
    }

    @Test
    fun `login locks account on 5th failed attempt`() {
        val user = createUser()
        user.failedLoginAttempts = 4
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)

        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest(user.email, "wrong"), httpRequest)
        }
        assertThat(user.failedLoginAttempts).isEqualTo(5)
        assertThat(user.lockedUntil).isNotNull()
        assertThat(user.lockedUntil).isAfter(Instant.now())
    }

    @Test
    fun `login rejects locked account`() {
        val user = createUser()
        user.lockedUntil = Instant.now().plusSeconds(600)
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))

        assertThrows<AccountLockedException> {
            authService.login(LoginRequest(user.email, "correct-password"), httpRequest)
        }
    }

    // ---- Refresh token flow ----

    @Test
    fun `refreshToken success rotates tokens`() {
        val user = createUser()
        val storedToken = createStoredToken(user, isRevoked = false, expiresAt = Instant.now().plusSeconds(3600))
        `when`(jwtService.hashToken("old-refresh")).thenReturn("old-hash")
        `when`(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(storedToken))
        `when`(jwtService.generateAccessToken(anyString(), anyString(), anyList(), anyList())).thenReturn("new-access")
        `when`(jwtService.generateRefreshToken()).thenReturn("new-refresh")
        `when`(jwtService.hashToken("new-refresh")).thenReturn("new-hash")
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: createDummyToken(user)))
            .thenAnswer { it.arguments[0] }

        val result = authService.refreshToken(RefreshRequest("old-refresh"))

        assertThat(result.accessToken).isEqualTo("new-access")
        assertThat(result.refreshToken).isEqualTo("new-refresh")
        assertThat(storedToken.isRevoked).isTrue()
    }

    @Test
    fun `refreshToken reuse detection revokes all`() {
        val user = createUser()
        val storedToken = createStoredToken(user, isRevoked = true, expiresAt = Instant.now().plusSeconds(3600))
        `when`(jwtService.hashToken("reused")).thenReturn("hash")
        `when`(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(storedToken))

        assertThrows<UnauthorizedException> { authService.refreshToken(RefreshRequest("reused")) }
        verify(refreshTokenRepository).revokeAllByUserId(user.id)
    }

    @Test
    fun `refreshToken expired throws`() {
        val user = createUser()
        val storedToken = createStoredToken(user, isRevoked = false, expiresAt = Instant.now().minusSeconds(60))
        `when`(jwtService.hashToken("expired")).thenReturn("hash")
        `when`(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(storedToken))
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: storedToken)).thenAnswer { it.arguments[0] }

        val ex = assertThrows<UnauthorizedException> { authService.refreshToken(RefreshRequest("expired")) }
        assertThat(ex.message).contains("expired")
    }

    @Test
    fun `refreshToken not found throws`() {
        `when`(jwtService.hashToken("unknown")).thenReturn("hash")
        `when`(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty())

        assertThrows<UnauthorizedException> { authService.refreshToken(RefreshRequest("unknown")) }
    }

    // ---- Logout ----

    @Test
    fun `logout with null refresh token still logs out`() {
        val userId = UUID.randomUUID()
        `when`(httpRequest.remoteAddr).thenReturn("10.0.0.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("Agent")

        authService.logout(userId, null, httpRequest)

        verify(auditService).logLogout(userId, "10.0.0.1", "Agent")
    }

    // ---- helpers ----

    private fun createUser(): UserEntity =
        UserEntity(
            id = UUID.randomUUID(),
            email = "auth@test.com",
            passwordHash = passwordEncoder.encode("correct-password"),
            firstName = "Auth",
            lastName = "User",
        )

    private fun createDummyToken(user: UserEntity) =
        RefreshTokenEntity(user = user, tokenHash = "dummy", expiresAt = Instant.now().plusSeconds(3600))

    private fun createStoredToken(user: UserEntity, isRevoked: Boolean, expiresAt: Instant): RefreshTokenEntity {
        val token = RefreshTokenEntity(user = user, tokenHash = "hash", expiresAt = expiresAt)
        token.isRevoked = isRevoked
        return token
    }
}
