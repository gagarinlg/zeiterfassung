package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.VacationBalanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VacationBalanceRepository : JpaRepository<VacationBalanceEntity, UUID> {
    fun findByUserIdAndYear(
        userId: UUID,
        year: Int,
    ): VacationBalanceEntity?

    fun findByUserId(userId: UUID): List<VacationBalanceEntity>

    fun findByYear(year: Int): List<VacationBalanceEntity>
}
