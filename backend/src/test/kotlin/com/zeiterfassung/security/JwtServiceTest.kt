package com.zeiterfassung.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtServiceTest {
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setUp() {
        jwtService =
            JwtService(
                jwtSecret = "test-secret-key-that-is-long-enough-for-testing-purposes-256-bits",
                accessTokenExpMs = 900000L,
                refreshTokenExpMs = 604800000L,
            )
    }

    @Test
    fun `generateAccessToken should create valid token`() {
        val token = jwtService.generateAccessToken("user-id", "test@test.com", listOf("EMPLOYEE"), listOf("time.track.own"))
        assertThat(token).isNotBlank()
        assertThat(jwtService.validateToken(token)).isTrue()
    }

    @Test
    fun `extractUserId should return correct user id`() {
        val token = jwtService.generateAccessToken("user-123", "test@test.com", listOf("EMPLOYEE"), emptyList())
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123")
    }

    @Test
    fun `extractEmail should return correct email`() {
        val token = jwtService.generateAccessToken("user-123", "test@test.com", listOf("EMPLOYEE"), emptyList())
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@test.com")
    }

    @Test
    fun `extractRoles should return correct roles`() {
        val token = jwtService.generateAccessToken("user-123", "test@test.com", listOf("MANAGER", "EMPLOYEE"), emptyList())
        assertThat(jwtService.extractRoles(token)).containsExactlyInAnyOrder("MANAGER", "EMPLOYEE")
    }

    @Test
    fun `validateToken should return false for invalid token`() {
        assertThat(jwtService.validateToken("invalid-token")).isFalse()
    }

    @Test
    fun `validateToken should return false for empty token`() {
        assertThat(jwtService.validateToken("")).isFalse()
    }

    @Test
    fun `generateRefreshToken should return UUID string`() {
        val token = jwtService.generateRefreshToken()
        assertThat(token).isNotBlank()
    }

    @Test
    fun `hashToken should return consistent hash`() {
        val token = "test-token"
        val hash1 = jwtService.hashToken(token)
        val hash2 = jwtService.hashToken(token)
        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun `hashToken should return different hash for different tokens`() {
        val hash1 = jwtService.hashToken("token1")
        val hash2 = jwtService.hashToken("token2")
        assertThat(hash1).isNotEqualTo(hash2)
    }
}
