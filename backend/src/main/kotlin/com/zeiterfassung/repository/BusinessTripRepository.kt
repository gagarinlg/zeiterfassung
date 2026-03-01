package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.BusinessTripEntity
import com.zeiterfassung.model.enums.BusinessTripStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface BusinessTripRepository : JpaRepository<BusinessTripEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<BusinessTripEntity>

    fun findByStatus(
        status: BusinessTripStatus,
        pageable: Pageable,
    ): Page<BusinessTripEntity>

    fun findByStatusAndUserIdIn(
        status: BusinessTripStatus,
        userIds: List<UUID>,
        pageable: Pageable,
    ): Page<BusinessTripEntity>

    @Query(
        """
        SELECT b FROM BusinessTripEntity b
        WHERE b.user.id = :userId
          AND b.status NOT IN ('CANCELLED', 'REJECTED')
          AND b.startDate <= :endDate
          AND b.endDate >= :startDate
          AND (:excludeId IS NULL OR b.id <> :excludeId)
        """,
    )
    fun findOverlapping(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("excludeId") excludeId: UUID?,
    ): List<BusinessTripEntity>
}
