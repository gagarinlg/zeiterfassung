package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.TimeEntryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface TimeEntryRepository : JpaRepository<TimeEntryEntity, UUID> {
    fun findByUser_IdAndTimestampBetweenOrderByTimestampAsc(
        userId: UUID,
        start: Instant,
        end: Instant,
    ): List<TimeEntryEntity>

    fun findTopByUser_IdOrderByTimestampDesc(userId: UUID): TimeEntryEntity?

    @Query(
        """
        SELECT e FROM TimeEntryEntity e
        WHERE e.user.id = :userId
          AND e.timestamp >= :startOfDay
          AND e.timestamp < :endOfDay
        ORDER BY e.timestamp ASC
        """,
    )
    fun findByUser_IdAndDateRange(
        @Param("userId") userId: UUID,
        @Param("startOfDay") startOfDay: Instant,
        @Param("endOfDay") endOfDay: Instant,
    ): List<TimeEntryEntity>
}
