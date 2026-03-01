package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.SickLeaveEntity
import com.zeiterfassung.model.enums.SickLeaveStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface SickLeaveRepository : JpaRepository<SickLeaveEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<SickLeaveEntity>

    fun findByUserIdAndStartDateBetween(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
    ): List<SickLeaveEntity>

    fun findByStatus(
        status: SickLeaveStatus,
        pageable: Pageable,
    ): Page<SickLeaveEntity>

    @Query(
        """
        SELECT s FROM SickLeaveEntity s
        WHERE s.user.id = :userId
          AND s.status != 'CANCELLED'
          AND s.startDate <= :endDate
          AND s.endDate >= :startDate
        """,
    )
    fun findOverlapping(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<SickLeaveEntity>
}
