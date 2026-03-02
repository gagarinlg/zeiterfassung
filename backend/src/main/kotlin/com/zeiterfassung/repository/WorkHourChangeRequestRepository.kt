package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.WorkHourChangeRequestEntity
import com.zeiterfassung.model.enums.WorkHourChangeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WorkHourChangeRequestRepository : JpaRepository<WorkHourChangeRequestEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<WorkHourChangeRequestEntity>

    fun findByStatus(
        status: WorkHourChangeStatus,
        pageable: Pageable,
    ): Page<WorkHourChangeRequestEntity>

    fun findByUserIdAndStatus(
        userId: UUID,
        status: WorkHourChangeStatus,
    ): List<WorkHourChangeRequestEntity>
}
