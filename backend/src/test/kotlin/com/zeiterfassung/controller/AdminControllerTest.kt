package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.AuditLogResponse
import com.zeiterfassung.model.dto.LdapConfigResponse
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.SystemSettingResponse
import com.zeiterfassung.model.dto.TestMailRequest
import com.zeiterfassung.model.dto.UpdateLdapConfigRequest
import com.zeiterfassung.model.dto.UpdateSystemSettingRequest
import com.zeiterfassung.service.AdminService
import com.zeiterfassung.service.EmailService
import com.zeiterfassung.service.LdapService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminControllerTest {
    @Mock
    private lateinit var adminService: AdminService

    @Mock
    private lateinit var ldapService: LdapService

    @Mock
    private lateinit var emailService: EmailService

    private lateinit var controller: AdminController

    private val actorId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        controller = AdminController(adminService, ldapService, emailService)
    }

    @Test
    fun `getAuditLog should return paginated audit log`() {
        val auditLogs =
            PageResponse(
                content =
                    listOf(
                        AuditLogResponse(
                            id = UUID.randomUUID(),
                            userId = UUID.randomUUID(),
                            userEmail = "user@test.com",
                            userFullName = "Test User",
                            action = "LOGIN",
                            entityType = null,
                            entityId = null,
                            ipAddress = "127.0.0.1",
                            userAgent = "TestAgent",
                            createdAt = Instant.now(),
                        ),
                    ),
                totalElements = 1L,
                totalPages = 1,
                pageNumber = 0,
                pageSize = 50,
            )
        val pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending())
        `when`(adminService.getAuditLog(pageable)).thenReturn(auditLogs)

        val response = controller.getAuditLog(0, 50)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
        assertThat(response.body!!.totalElements).isEqualTo(1L)
    }

    @Test
    fun `getAuditLogByUser should return filtered audit log`() {
        val userId = UUID.randomUUID()
        val auditLogs =
            PageResponse<AuditLogResponse>(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
                pageNumber = 0,
                pageSize = 50,
            )
        val pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending())
        `when`(adminService.getAuditLogByUser(userId, pageable)).thenReturn(auditLogs)

        val response = controller.getAuditLogByUser(userId, 0, 50)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).isEmpty()
    }

    @Test
    fun `getSystemSettings should return settings list`() {
        val settings =
            listOf(
                SystemSettingResponse("company.name", "Test GmbH", "Company name", Instant.now()),
                SystemSettingResponse("locale.default", "de", "Default locale", Instant.now()),
            )
        `when`(adminService.getSystemSettings()).thenReturn(settings)

        val response = controller.getSystemSettings()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(2)
        assertThat(response.body!![0].key).isEqualTo("company.name")
    }

    @Test
    fun `updateSystemSetting should update and return setting`() {
        val request = UpdateSystemSettingRequest(value = "New Company GmbH")
        val updated = SystemSettingResponse("company.name", "New Company GmbH", "Company name", Instant.now())
        `when`(adminService.updateSystemSetting("company.name", request, UUID.fromString(actorId))).thenReturn(updated)

        val response = controller.updateSystemSetting("company.name", request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.value).isEqualTo("New Company GmbH")
    }

    @Test
    fun `getLdapConfig should return LDAP configuration`() {
        val ldapConfig =
            LdapConfigResponse(
                enabled = true,
                url = "ldap://localhost:389",
                baseDn = "dc=example,dc=com",
                userSearchBase = "ou=users",
                userSearchFilter = "(uid={0})",
                groupSearchBase = "ou=groups",
                groupSearchFilter = "(member={0})",
                managerDn = "cn=admin,dc=example,dc=com",
                activeDirectoryMode = false,
                activeDirectoryDomain = "",
                roleMapping = "{}",
                emailAttribute = "mail",
                firstNameAttribute = "givenName",
                lastNameAttribute = "sn",
                employeeNumberAttribute = "employeeNumber",
            )
        `when`(ldapService.getLdapConfig()).thenReturn(ldapConfig)

        val response = controller.getLdapConfig()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.enabled).isTrue()
        assertThat(response.body!!.url).isEqualTo("ldap://localhost:389")
    }

    @Test
    fun `updateLdapConfig should update and return config`() {
        val request = UpdateLdapConfigRequest(enabled = false, url = "ldap://new:389")
        val updated =
            LdapConfigResponse(
                enabled = false,
                url = "ldap://new:389",
                baseDn = "dc=example,dc=com",
                userSearchBase = "ou=users",
                userSearchFilter = "(uid={0})",
                groupSearchBase = "ou=groups",
                groupSearchFilter = "(member={0})",
                managerDn = "cn=admin,dc=example,dc=com",
                activeDirectoryMode = false,
                activeDirectoryDomain = "",
                roleMapping = "{}",
                emailAttribute = "mail",
                firstNameAttribute = "givenName",
                lastNameAttribute = "sn",
                employeeNumberAttribute = "employeeNumber",
            )
        `when`(ldapService.updateLdapConfig(request)).thenReturn(updated)

        val response = controller.updateLdapConfig(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.enabled).isFalse()
    }

    @Test
    fun `sendTestMail should return ok on success`() {
        val request = TestMailRequest(recipientEmail = "test@example.com")
        doNothing().`when`(emailService).sendTestMail("test@example.com")

        val response = controller.sendTestMail(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["status"]).isEqualTo("ok")
        verify(emailService).sendTestMail("test@example.com")
    }

    @Test
    fun `sendTestMail should return bad request when mail not configured`() {
        val request = TestMailRequest(recipientEmail = "test@example.com")
        doThrow(IllegalStateException("Mail is not configured"))
            .`when`(emailService).sendTestMail("test@example.com")

        val response = controller.sendTestMail(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!["status"]).isEqualTo("error")
        assertThat(response.body!!["message"]).contains("Mail is not configured")
    }

    @Test
    fun `sendTestMail should return 500 on unexpected error`() {
        val request = TestMailRequest(recipientEmail = "test@example.com")
        doThrow(RuntimeException("SMTP connection failed"))
            .`when`(emailService).sendTestMail("test@example.com")

        val response = controller.sendTestMail(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body!!["status"]).isEqualTo("error")
        assertThat(response.body!!["message"]).contains("SMTP connection failed")
    }

    @Test
    fun `sendTestMail should handle exception with null message`() {
        val request = TestMailRequest(recipientEmail = "test@example.com")
        doThrow(RuntimeException())
            .`when`(emailService).sendTestMail("test@example.com")

        val response = controller.sendTestMail(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body!!["message"]).isEqualTo("Unknown error")
    }

    @Test
    fun `sendTestMail should handle IllegalStateException with null message`() {
        val request = TestMailRequest(recipientEmail = "test@example.com")
        doThrow(IllegalStateException())
            .`when`(emailService).sendTestMail("test@example.com")

        val response = controller.sendTestMail(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!["message"]).isEqualTo("Mail is not configured")
    }
}
