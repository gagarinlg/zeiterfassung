package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.UpdateSystemSettingRequest
import com.zeiterfassung.model.entity.AuditLogEntity
import com.zeiterfassung.model.entity.SystemSettingEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.AuditLogRepository
import com.zeiterfassung.repository.SystemSettingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {
    @Mock private lateinit var auditLogRepository: AuditLogRepository

    @Mock private lateinit var systemSettingRepository: SystemSettingRepository

    @Mock private lateinit var auditService: AuditService

    private lateinit var adminService: AdminService
    private val actorId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        adminService = AdminService(auditLogRepository, systemSettingRepository, auditService)
    }

    private fun makeUser(id: UUID = UUID.randomUUID()) =
        UserEntity(
            id = id,
            email = "user@test.com",
            passwordHash = "hash",
            firstName = "Test",
            lastName = "User",
        )

    private fun makeAuditLog(user: UserEntity? = null) =
        AuditLogEntity(
            id = UUID.randomUUID(),
            user = user,
            action = "LOGIN",
            entityType = "User",
            entityId = user?.id,
            ipAddress = "127.0.0.1",
            createdAt = Instant.now(),
        )

    private fun makeSetting(
        key: String,
        value: String,
    ) = SystemSettingEntity(
        id = UUID.randomUUID(),
        key = key,
        value = value,
        description = "Description for $key",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    // --- getAuditLog ---

    @Test
    fun `getAuditLog should return paginated audit log`() {
        val user = makeUser()
        val entry = makeAuditLog(user)
        val pageable = PageRequest.of(0, 10)
        `when`(auditLogRepository.findAll(pageable)).thenReturn(PageImpl(listOf(entry), pageable, 1))

        val result = adminService.getAuditLog(pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].action).isEqualTo("LOGIN")
        assertThat(result.content[0].userEmail).isEqualTo("user@test.com")
        assertThat(result.content[0].userFullName).isEqualTo("Test User")
    }

    @Test
    fun `getAuditLog should handle entries without a user`() {
        val entry = makeAuditLog(user = null)
        val pageable = PageRequest.of(0, 10)
        `when`(auditLogRepository.findAll(pageable)).thenReturn(PageImpl(listOf(entry), pageable, 1))

        val result = adminService.getAuditLog(pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].userId).isNull()
        assertThat(result.content[0].userEmail).isNull()
        assertThat(result.content[0].userFullName).isNull()
    }

    @Test
    fun `getAuditLog should return empty page when no entries exist`() {
        val pageable = PageRequest.of(0, 10)
        `when`(auditLogRepository.findAll(pageable)).thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = adminService.getAuditLog(pageable)

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
        assertThat(result.totalPages).isEqualTo(0)
    }

    // --- getAuditLogByUser ---

    @Test
    fun `getAuditLogByUser should filter by userId`() {
        val userId = UUID.randomUUID()
        val user = makeUser(userId)
        val entry = makeAuditLog(user)
        val pageable = PageRequest.of(0, 10)
        `when`(auditLogRepository.findByUserId(userId, pageable)).thenReturn(PageImpl(listOf(entry), pageable, 1))

        val result = adminService.getAuditLogByUser(userId, pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].userId).isEqualTo(userId)
    }

    // --- getSystemSettings ---

    @Test
    fun `getSystemSettings should return all settings`() {
        val settings =
            listOf(
                makeSetting("company.name", "Test GmbH"),
                makeSetting("working.hours.default", "8"),
            )
        `when`(systemSettingRepository.findAll()).thenReturn(settings)

        val result = adminService.getSystemSettings()

        assertThat(result).hasSize(2)
        assertThat(result[0].key).isEqualTo("company.name")
        assertThat(result[0].value).isEqualTo("Test GmbH")
        assertThat(result[1].key).isEqualTo("working.hours.default")
    }

    @Test
    fun `getSystemSettings should return empty list when no settings`() {
        `when`(systemSettingRepository.findAll()).thenReturn(emptyList())

        val result = adminService.getSystemSettings()

        assertThat(result).isEmpty()
    }

    // --- updateSystemSetting ---

    @Test
    fun `updateSystemSetting should update and return setting`() {
        val settingId = UUID.randomUUID()
        val setting = SystemSettingEntity(id = settingId, key = "company.name", value = "Old GmbH", description = "Company name")
        val updatedSetting = SystemSettingEntity(id = settingId, key = "company.name", value = "New GmbH", description = "Company name")
        `when`(systemSettingRepository.findByKey("company.name")).thenReturn(Optional.of(setting))
        `when`(systemSettingRepository.save(any())).thenReturn(updatedSetting)

        val result = adminService.updateSystemSetting("company.name", UpdateSystemSettingRequest("New GmbH"), actorId)

        assertThat(result.value).isEqualTo("New GmbH")
        verify(auditService).logDataChange(
            actorId,
            "SETTING_UPDATED",
            "SystemSetting",
            settingId,
            "Old GmbH",
            "New GmbH",
        )
    }

    @Test
    fun `updateSystemSetting should throw ResourceNotFoundException for unknown key`() {
        `when`(systemSettingRepository.findByKey("nonexistent")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            adminService.updateSystemSetting("nonexistent", UpdateSystemSettingRequest("value"), actorId)
        }
    }

    @Test
    fun `updateSystemSetting should handle null value in existing setting`() {
        val setting = makeSetting("email.enabled", "true").apply { value = null }
        `when`(systemSettingRepository.findByKey("email.enabled")).thenReturn(Optional.of(setting))
        `when`(systemSettingRepository.save(any())).thenReturn(setting.apply { value = "false" })

        val result = adminService.updateSystemSetting("email.enabled", UpdateSystemSettingRequest("false"), actorId)

        assertThat(result.value).isEqualTo("false")
    }
}
