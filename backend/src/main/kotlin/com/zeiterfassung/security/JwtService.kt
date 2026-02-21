package com.zeiterfassung.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.jwt.access-token-expiration-ms}") private val accessTokenExpMs: Long,
    @Value("\${app.jwt.refresh-token-expiration-ms}") private val refreshTokenExpMs: Long,
) {
    private val logger = LoggerFactory.getLogger(JwtService::class.java)

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateAccessToken(
        userId: String,
        email: String,
        roles: List<String>,
        permissions: List<String>,
    ): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpMs)
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("roles", roles)
            .claim("permissions", permissions)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    fun generateRefreshToken(): String = UUID.randomUUID().toString()

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: JwtException) {
            logger.debug("Invalid JWT token: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            logger.debug("JWT token is empty: {}", e.message)
            false
        }
    }

    fun extractUserId(token: String): String = getClaims(token).subject

    fun extractEmail(token: String): String = getClaims(token)["email"] as String

    @Suppress("UNCHECKED_CAST")
    fun extractRoles(token: String): List<String> = getClaims(token)["roles"] as List<String>

    @Suppress("UNCHECKED_CAST")
    fun extractPermissions(token: String): List<String> = getClaims(token)["permissions"] as List<String>

    fun getRefreshTokenExpirationMs(): Long = refreshTokenExpMs

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
