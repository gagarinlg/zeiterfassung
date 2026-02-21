package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshTokenEntity>

    @Modifying
    @Query("UPDATE RefreshTokenEntity t SET t.isRevoked = true WHERE t.user.id = :userId")
    fun revokeAllByUserId(userId: UUID): Int

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant): Int
}
