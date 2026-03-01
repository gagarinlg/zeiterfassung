package com.zeiterfassung.model.dto

import java.time.Instant
import java.util.UUID

data class AuditLogResponse(
    val id: UUID,
    val userId: UUID?,
    val userEmail: String?,
    val userFullName: String?,
    val action: String,
    val entityType: String?,
    val entityId: UUID?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: Instant,
)

data class SystemSettingResponse(
    val key: String,
    val value: String,
    val description: String?,
    val updatedAt: Instant?,
)

data class UpdateSystemSettingRequest(
    val value: String,
)

data class TestMailRequest(
    val recipientEmail: String,
)
