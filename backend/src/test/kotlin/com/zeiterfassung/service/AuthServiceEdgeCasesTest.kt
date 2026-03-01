package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
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
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthServiceEdgeCasesTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var auditService: AuditService

    @Mock
    private lateinit var totpService: TotpService

    @Mock
    private lateinit var httpRequest: HttpServletRequest

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
                totpService,
                604800000L,
                900000L,
            )
    }

    @Test
    fun `login succeeds with correct password`() {
        val user = createUser()
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        `when`(jwtService.generateAccessToken(anyString(), anyString(), anyList(), anyList())).thenReturn("access-token")
        `when`(jwtService.generateRefreshToken()).thenReturn("refresh-token")
        `when`(jwtService.hashToken("refresh-token")).thenReturn("hashed-refresh")
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: createDummyToken(user)))
            .thenAnswer { it.arguments[0] }
        `when`(httpRequest.remoteAddr).thenReturn("127.0.0.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("TestAgent")

        val result = authService.login(LoginRequest(user.email, "correct-password"), httpRequest)
        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.user.email).isEqualTo(user.email)
    }

    @Test
    fun `login resets failed attempts after successful login`() {
        val user = createUser()
        user.failedLoginAttempts = 3
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        `when`(jwtService.generateAccessToken(anyString(), anyString(), anyList(), anyList())).thenReturn("token")
        `when`(jwtService.generateRefreshToken()).thenReturn("refresh")
        `when`(jwtService.hashToken("refresh")).thenReturn("hash")
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: createDummyToken(user)))
            .thenAnswer { it.arguments[0] }
        `when`(httpRequest.remoteAddr).thenReturn("127.0.0.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("Agent")

        authService.login(LoginRequest(user.email, "correct-password"), httpRequest)
        assertThat(user.failedLoginAttempts).isEqualTo(0)
        assertThat(user.lockedUntil).isNull()
    }

    @Test
    fun `login with TOTP enabled but no code throws`() {
        val user = createUser()
        user.totpEnabled = true
        user.totpSecret = "secret"
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)

        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest(user.email, "correct-password"), httpRequest)
        }
    }

    @Test
    fun `login with TOTP enabled and invalid code throws`() {
        val user = createUser()
        user.totpEnabled = true
        user.totpSecret = "secret"
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        `when`(totpService.verifyCode("secret", "000000")).thenReturn(false)

        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest(user.email, "correct-password", totpCode = "000000"), httpRequest)
        }
    }

    @Test
    fun `login with TOTP enabled and valid code succeeds`() {
        val user = createUser()
        user.totpEnabled = true
        user.totpSecret = "secret"
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)
        `when`(totpService.verifyCode("secret", "123456")).thenReturn(true)
        `when`(jwtService.generateAccessToken(anyString(), anyString(), anyList(), anyList())).thenReturn("token")
        `when`(jwtService.generateRefreshToken()).thenReturn("refresh")
        `when`(jwtService.hashToken("refresh")).thenReturn("hash")
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: createDummyToken(user)))
            .thenAnswer { it.arguments[0] }
        `when`(httpRequest.remoteAddr).thenReturn("127.0.0.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("Agent")

        val result = authService.login(LoginRequest(user.email, "correct-password", totpCode = "123456"), httpRequest)
        assertThat(result.accessToken).isEqualTo("token")
    }

    @Test
    fun `refreshToken throws for revoked token`() {
        val user = createUser()
        val storedToken = createStoredToken(user, isRevoked = true, expiresAt = Instant.now().plusSeconds(3600))
        `when`(jwtService.hashToken("old-refresh")).thenReturn("hashed")
        `when`(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(storedToken))

        val ex =
            assertThrows<UnauthorizedException> {
                authService.refreshToken(RefreshRequest("old-refresh"))
            }
        assertThat(ex.message).contains("reuse")
        verify(refreshTokenRepository).revokeAllByUserId(user.id)
    }

    @Test
    fun `refreshToken throws for expired token`() {
        val user = createUser()
        val storedToken = createStoredToken(user, isRevoked = false, expiresAt = Instant.now().minusSeconds(3600))
        `when`(jwtService.hashToken("expired-refresh")).thenReturn("hashed")
        `when`(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(storedToken))
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: storedToken)).thenAnswer { it.arguments[0] }

        val ex =
            assertThrows<UnauthorizedException> {
                authService.refreshToken(RefreshRequest("expired-refresh"))
            }
        assertThat(ex.message).contains("expired")
    }

    @Test
    fun `refreshToken with reuse detection revokes all sessions`() {
        val user = createUser()
        val storedToken = createStoredToken(user, isRevoked = true, expiresAt = Instant.now().plusSeconds(3600))
        `when`(jwtService.hashToken("reused-refresh")).thenReturn("hashed")
        `when`(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(storedToken))

        assertThrows<UnauthorizedException> {
            authService.refreshToken(RefreshRequest("reused-refresh"))
        }
        verify(refreshTokenRepository).revokeAllByUserId(user.id)
    }

    @Test
    fun `logout revokes specific refresh token`() {
        val userId = UUID.randomUUID()
        val user = createUser()
        val storedToken = createStoredToken(user, isRevoked = false, expiresAt = Instant.now().plusSeconds(3600))
        `when`(jwtService.hashToken("my-refresh")).thenReturn("hashed")
        `when`(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(storedToken))
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java) ?: storedToken)).thenAnswer { it.arguments[0] }
        `when`(httpRequest.remoteAddr).thenReturn("127.0.0.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("Agent")

        authService.logout(user.id, "my-refresh", httpRequest)
        assertThat(storedToken.isRevoked).isTrue()
    }

    @Test
    fun `logoutAll revokes all tokens for user`() {
        val userId = UUID.randomUUID()
        `when`(httpRequest.remoteAddr).thenReturn("127.0.0.1")
        `when`(httpRequest.getHeader("User-Agent")).thenReturn("Agent")

        authService.logoutAll(userId, httpRequest)

        verify(refreshTokenRepository).revokeAllByUserId(userId)
    }

    @Test
    fun `getCurrentUser returns user response`() {
        val user = createUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))

        val result = authService.getCurrentUser(user.id)
        assertThat(result.email).isEqualTo(user.email)
        assertThat(result.firstName).isEqualTo(user.firstName)
    }

    @Test
    fun `getCurrentUser throws for non-existent user`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            authService.getCurrentUser(id)
        }
    }

    private fun createUser(): UserEntity =
        UserEntity(
            id = UUID.randomUUID(),
            email = "test@test.com",
            passwordHash = passwordEncoder.encode("correct-password"),
            firstName = "Test",
            lastName = "User",
        )

    private fun createDummyToken(user: UserEntity) =
        RefreshTokenEntity(
            user = user,
            tokenHash = "dummy-hash",
            expiresAt = Instant.now().plusSeconds(3600),
        )

    private fun createStoredToken(
        user: UserEntity,
        isRevoked: Boolean,
        expiresAt: Instant,
    ): RefreshTokenEntity {
        val token =
            RefreshTokenEntity(
                user = user,
                tokenHash = "hashed",
                expiresAt = expiresAt,
            )
        token.isRevoked = isRevoked
        return token
    }
}
