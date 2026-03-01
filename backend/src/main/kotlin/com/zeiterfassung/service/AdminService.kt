package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.AuditLogResponse
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.SystemSettingResponse
import com.zeiterfassung.model.dto.UpdateSystemSettingRequest
import com.zeiterfassung.model.entity.AuditLogEntity
import com.zeiterfassung.repository.AuditLogRepository
import com.zeiterfassung.repository.SystemSettingRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AdminService(
    private val auditLogRepository: AuditLogRepository,
    private val systemSettingRepository: SystemSettingRepository,
    private val auditService: AuditService,
) {
    fun getAuditLog(pageable: Pageable): PageResponse<AuditLogResponse> {
        val page = auditLogRepository.findAll(pageable)
        return PageResponse(
            content = page.content.map { it.toResponse() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            pageNumber = page.number,
            pageSize = page.size,
        )
    }

    fun getAuditLogByUser(
        userId: UUID,
        pageable: Pageable,
    ): PageResponse<AuditLogResponse> {
        val page = auditLogRepository.findByUserId(userId, pageable)
        return PageResponse(
            content = page.content.map { it.toResponse() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            pageNumber = page.number,
            pageSize = page.size,
        )
    }

    @Cacheable("systemSettings")
    fun getSystemSettings(): List<SystemSettingResponse> = systemSettingRepository.findAll().map { it.toResponse() }

    @CacheEvict(value = ["systemSettings"], allEntries = true)
    @Transactional
    fun updateSystemSetting(
        key: String,
        request: UpdateSystemSettingRequest,
        actorId: UUID,
    ): SystemSettingResponse {
        val setting =
            systemSettingRepository
                .findByKey(key)
                .orElseThrow { ResourceNotFoundException("Setting not found: $key") }
        val oldValue = setting.value
        setting.value = request.value
        val saved = systemSettingRepository.save(setting)
        auditService.logDataChange(actorId, "SETTING_UPDATED", "SystemSetting", saved.id, oldValue, request.value)
        return saved.toResponse()
    }

    private fun AuditLogEntity.toResponse() =
        AuditLogResponse(
            id = this.id,
            userId = this.user?.id,
            userEmail = this.user?.email,
            userFullName = this.user?.let { "${it.firstName} ${it.lastName}" },
            action = this.action,
            entityType = this.entityType,
            entityId = this.entityId,
            ipAddress = this.ipAddress,
            userAgent = this.userAgent,
            createdAt = this.createdAt,
        )

    private fun com.zeiterfassung.model.entity.SystemSettingEntity.toResponse() =
        SystemSettingResponse(
            key = this.key,
            value = this.value ?: "",
            description = this.description,
            updatedAt = this.updatedAt,
        )
}
