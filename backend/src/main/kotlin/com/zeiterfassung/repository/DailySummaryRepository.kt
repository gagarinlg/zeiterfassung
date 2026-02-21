package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.DailySummaryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface DailySummaryRepository : JpaRepository<DailySummaryEntity, UUID> {
    fun findByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): DailySummaryEntity?

    fun findByUserIdAndDateBetweenOrderByDateAsc(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailySummaryEntity>
}
