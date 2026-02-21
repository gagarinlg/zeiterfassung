package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.VacationRequestEntity
import com.zeiterfassung.model.enums.VacationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface VacationRequestRepository : JpaRepository<VacationRequestEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<VacationRequestEntity>

    fun findByUserIdAndStatus(
        userId: UUID,
        status: VacationStatus,
    ): List<VacationRequestEntity>

    fun findByStatus(
        status: VacationStatus,
        pageable: Pageable,
    ): Page<VacationRequestEntity>

    fun findByStatusAndUserIdIn(
        status: VacationStatus,
        userIds: List<UUID>,
        pageable: Pageable,
    ): Page<VacationRequestEntity>

    @Query(
        """
        SELECT v FROM VacationRequestEntity v
        WHERE v.user.id = :userId
          AND v.status IN ('PENDING', 'APPROVED')
          AND v.startDate <= :endDate
          AND v.endDate >= :startDate
          AND (:excludeId IS NULL OR v.id <> :excludeId)
        """,
    )
    fun findOverlapping(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("excludeId") excludeId: UUID?,
    ): List<VacationRequestEntity>

    fun findByUserIdAndStartDateBetween(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<VacationRequestEntity>

    fun findByApprovedById(approverId: UUID): List<VacationRequestEntity>

    @Query(
        """
        SELECT v FROM VacationRequestEntity v
        WHERE v.startDate <= :endDate AND v.endDate >= :startDate
          AND v.status = 'APPROVED'
          AND v.user.id IN :userIds
        """,
    )
    fun findApprovedByUserIdsAndDateRange(
        @Param("userIds") userIds: List<UUID>,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<VacationRequestEntity>
}
