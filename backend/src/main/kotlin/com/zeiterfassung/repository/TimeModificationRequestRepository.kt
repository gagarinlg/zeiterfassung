package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.TimeModificationRequestEntity
import com.zeiterfassung.model.enums.TimeModificationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TimeModificationRequestRepository : JpaRepository<TimeModificationRequestEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<TimeModificationRequestEntity>

    fun findByStatus(
        status: TimeModificationStatus,
        pageable: Pageable,
    ): Page<TimeModificationRequestEntity>

    fun findByTimeEntryIdAndStatus(
        timeEntryId: UUID,
        status: TimeModificationStatus,
    ): List<TimeModificationRequestEntity>
}
