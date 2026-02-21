package com.zeiterfassung.audit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.model.entity.AuditLogEntity
import com.zeiterfassung.repository.AuditLogRepository
import com.zeiterfassung.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)

    @Async
    fun logLogin(
        userId: UUID,
        ipAddress: String?,
        userAgent: String?,
    ) {
        saveLog(userId, "LOGIN", null, null, null, null, ipAddress, userAgent)
    }

    @Async
    fun logLogout(
        userId: UUID,
        ipAddress: String?,
        userAgent: String?,
    ) {
        saveLog(userId, "LOGOUT", null, null, null, null, ipAddress, userAgent)
    }

    @Async
    fun logDataChange(
        actorId: UUID,
        action: String,
        entityType: String,
        entityId: UUID,
        oldValue: Any?,
        newValue: Any?,
    ) {
        val oldJson = oldValue?.let { objectMapper.valueToTree<JsonNode>(it) }
        val newJson = newValue?.let { objectMapper.valueToTree<JsonNode>(it) }
        saveLog(actorId, action, entityType, entityId, oldJson, newJson, null, null)
    }

    @Async
    fun logPermissionChange(
        actorId: UUID,
        entityType: String,
        entityId: UUID,
        oldValue: Any?,
        newValue: Any?,
    ) {
        logDataChange(actorId, "PERMISSION_CHANGE", entityType, entityId, oldValue, newValue)
    }

    private fun saveLog(
        userId: UUID,
        action: String,
        entityType: String?,
        entityId: UUID?,
        oldValue: JsonNode?,
        newValue: JsonNode?,
        ipAddress: String?,
        userAgent: String?,
    ) {
        try {
            val user = userRepository.findById(userId).orElse(null)
            val log =
                AuditLogEntity(
                    user = user,
                    action = action,
                    entityType = entityType,
                    entityId = entityId,
                    oldValue = oldValue,
                    newValue = newValue,
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                )
            auditLogRepository.save(log)
        } catch (e: Exception) {
            logger.error("Failed to save audit log: {}", e.message)
        }
    }
}
