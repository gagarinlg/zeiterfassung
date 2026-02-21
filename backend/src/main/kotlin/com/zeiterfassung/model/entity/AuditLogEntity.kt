package com.zeiterfassung.model.entity

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserEntity? = null,
    @Column(nullable = false)
    val action: String,
    @Column(name = "entity_type")
    val entityType: String? = null,
    @Column(name = "entity_id")
    val entityId: UUID? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value")
    val oldValue: JsonNode? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value")
    val newValue: JsonNode? = null,
    @Column(name = "ip_address")
    val ipAddress: String? = null,
    @Column(name = "user_agent")
    val userAgent: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
