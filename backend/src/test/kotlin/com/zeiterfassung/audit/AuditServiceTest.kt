package com.zeiterfassung.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zeiterfassung.model.entity.AuditLogEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.AuditLogRepository
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuditServiceTest {
    @Mock
    private lateinit var auditLogRepository: AuditLogRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var service: AuditService

    @Captor
    private lateinit var auditLogCaptor: ArgumentCaptor<AuditLogEntity>

    private lateinit var user: UserEntity
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            AuditService(
                auditLogRepository,
                userRepository,
                objectMapper,
            )
        user =
            UserEntity(
                id = userId,
                email = "test@test.com",
                passwordHash = "hash",
                firstName = "Test",
                lastName = "User",
            )
    }

    @Test
    fun `logLogin saves audit entry with correct action`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(auditLogRepository.save(any(AuditLogEntity::class.java) ?: createDummyLog())).thenAnswer { it.arguments[0] }

        service.logLogin(userId, "192.168.1.1", "Mozilla/5.0")

        verify(auditLogRepository).save(auditLogCaptor.capture())
        val saved = auditLogCaptor.value
        assertThat(saved.action).isEqualTo("LOGIN")
        assertThat(saved.user).isEqualTo(user)
        assertThat(saved.ipAddress).isEqualTo("192.168.1.1")
        assertThat(saved.userAgent).isEqualTo("Mozilla/5.0")
        assertThat(saved.entityType).isNull()
        assertThat(saved.entityId).isNull()
    }

    @Test
    fun `logLogout saves audit entry with correct action`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(auditLogRepository.save(any(AuditLogEntity::class.java) ?: createDummyLog())).thenAnswer { it.arguments[0] }

        service.logLogout(userId, "10.0.0.1", "Chrome")

        verify(auditLogRepository).save(auditLogCaptor.capture())
        val saved = auditLogCaptor.value
        assertThat(saved.action).isEqualTo("LOGOUT")
        assertThat(saved.ipAddress).isEqualTo("10.0.0.1")
    }

    @Test
    fun `logDataChange saves entry with old and new values`() {
        val entityId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(auditLogRepository.save(any(AuditLogEntity::class.java) ?: createDummyLog())).thenAnswer { it.arguments[0] }

        service.logDataChange(userId, "USER_UPDATED", "User", entityId, mapOf("name" to "old"), mapOf("name" to "new"))

        verify(auditLogRepository).save(auditLogCaptor.capture())
        val saved = auditLogCaptor.value
        assertThat(saved.action).isEqualTo("USER_UPDATED")
        assertThat(saved.entityType).isEqualTo("User")
        assertThat(saved.entityId).isEqualTo(entityId)
        assertThat(saved.oldValue).isNotNull
        assertThat(saved.newValue).isNotNull
        assertThat(saved.oldValue!!.get("name").asText()).isEqualTo("old")
        assertThat(saved.newValue!!.get("name").asText()).isEqualTo("new")
    }

    @Test
    fun `logPermissionChange delegates to logDataChange with correct action`() {
        val entityId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(auditLogRepository.save(any(AuditLogEntity::class.java) ?: createDummyLog())).thenAnswer { it.arguments[0] }

        service.logPermissionChange(userId, "User", entityId, listOf("EMPLOYEE"), listOf("ADMIN", "EMPLOYEE"))

        verify(auditLogRepository).save(auditLogCaptor.capture())
        val saved = auditLogCaptor.value
        assertThat(saved.action).isEqualTo("PERMISSION_CHANGE")
        assertThat(saved.entityType).isEqualTo("User")
    }

    @Test
    fun `saveLog handles exception gracefully without throwing`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(auditLogRepository.save(any(AuditLogEntity::class.java) ?: createDummyLog()))
            .thenThrow(RuntimeException("DB error"))

        // Should not throw
        service.logLogin(userId, "192.168.1.1", "Mozilla/5.0")

        verify(auditLogRepository).save(any(AuditLogEntity::class.java) ?: createDummyLog())
    }

    @Test
    fun `saveLog handles non-existent user`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())
        `when`(auditLogRepository.save(any(AuditLogEntity::class.java) ?: createDummyLog())).thenAnswer { it.arguments[0] }

        // Should not throw; user will be null in the audit entry
        service.logLogin(userId, null, null)

        verify(auditLogRepository).save(auditLogCaptor.capture())
        val saved = auditLogCaptor.value
        assertThat(saved.user).isNull()
        assertThat(saved.action).isEqualTo("LOGIN")
    }

    private fun createDummyLog() = AuditLogEntity(action = "DUMMY")
}
