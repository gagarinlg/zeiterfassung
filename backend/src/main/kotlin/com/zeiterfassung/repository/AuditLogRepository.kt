package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.AuditLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AuditLogEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<AuditLogEntity>

    fun findByEntityTypeAndEntityId(
        entityType: String,
        entityId: UUID,
    ): List<AuditLogEntity>
}
