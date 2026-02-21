package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.PublicHolidayEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface PublicHolidayRepository : JpaRepository<PublicHolidayEntity, UUID> {
    fun findByDateBetween(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<PublicHolidayEntity>

    fun findByStateCodeIsNullOrStateCode(stateCode: String): List<PublicHolidayEntity>

    @Query(
        """
        SELECT p FROM PublicHolidayEntity p
        WHERE EXTRACT(YEAR FROM p.date) = :year
           OR p.isRecurring = true
        """,
    )
    fun findApplicableForYear(
        @Param("year") year: Int,
    ): List<PublicHolidayEntity>
}
