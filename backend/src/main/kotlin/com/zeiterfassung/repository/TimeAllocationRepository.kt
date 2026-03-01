package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.TimeAllocationEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface TimeAllocationRepository : JpaRepository<TimeAllocationEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<TimeAllocationEntity>

    fun findByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): List<TimeAllocationEntity>

    fun findByUserIdAndDateBetween(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
    ): List<TimeAllocationEntity>

    fun findByProjectId(
        projectId: UUID,
        pageable: Pageable,
    ): Page<TimeAllocationEntity>

    @Query(
        """
        SELECT COALESCE(SUM(t.minutes), 0) FROM TimeAllocationEntity t
        WHERE t.user.id = :userId AND t.date = :date
        """,
    )
    fun sumMinutesByUserIdAndDate(
        @Param("userId") userId: UUID,
        @Param("date") date: LocalDate,
    ): Int
}
