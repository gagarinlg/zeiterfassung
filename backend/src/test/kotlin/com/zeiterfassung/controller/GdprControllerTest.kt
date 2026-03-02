package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.GdprDataExportResponse
import com.zeiterfassung.model.dto.GdprDeletionResponse
import com.zeiterfassung.service.GdprService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GdprControllerTest {
    @Mock
    private lateinit var gdprService: GdprService

    private lateinit var controller: GdprController

    private val userId = UUID.randomUUID()
    private val actorId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        controller = GdprController(gdprService)
    }

    @Test
    fun `exportOwnData should return data export for authenticated user`() {
        val export =
            GdprDataExportResponse(
                exportedAt = Instant.now(),
                personalInfo =
                    com.zeiterfassung.model.dto.GdprPersonalInfo(
                        email = "user@test.com",
                        firstName = "Max",
                        lastName = "Mustermann",
                        employeeNumber = "EMP001",
                        phone = null,
                        dateFormat = null,
                        timeFormat = null,
                        createdAt = Instant.now(),
                    ),
                timeEntries = emptyList(),
                vacationRequests = emptyList(),
                sickLeaves = emptyList(),
                businessTrips = emptyList(),
                auditLog = emptyList(),
            )
        `when`(gdprService.exportUserData(userId)).thenReturn(export)

        val response = controller.exportOwnData(userId.toString())

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.personalInfo.email).isEqualTo("user@test.com")
    }

    @Test
    fun `requestOwnDeletion should delete own data`() {
        val deletion =
            GdprDeletionResponse(
                status = "ok",
                message = "Account anonymized and deactivated",
                deletedAt = Instant.now(),
            )
        `when`(gdprService.requestDeletion(userId, userId)).thenReturn(deletion)

        val response = controller.requestOwnDeletion(userId.toString())

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo("ok")
    }

    @Test
    fun `exportUserData should return data export for admin query`() {
        val targetUserId = UUID.randomUUID()
        val export =
            GdprDataExportResponse(
                exportedAt = Instant.now(),
                personalInfo =
                    com.zeiterfassung.model.dto.GdprPersonalInfo(
                        email = "other@test.com",
                        firstName = "Other",
                        lastName = "User",
                        employeeNumber = null,
                        phone = null,
                        dateFormat = null,
                        timeFormat = null,
                        createdAt = Instant.now(),
                    ),
                timeEntries = emptyList(),
                vacationRequests = emptyList(),
                sickLeaves = emptyList(),
                businessTrips = emptyList(),
                auditLog = emptyList(),
            )
        `when`(gdprService.exportUserData(targetUserId)).thenReturn(export)

        val response = controller.exportUserData(targetUserId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.personalInfo.firstName).isEqualTo("Other")
    }

    @Test
    fun `deleteUserData should delete user data as admin`() {
        val targetUserId = UUID.randomUUID()
        val deletion =
            GdprDeletionResponse(
                status = "ok",
                message = "Account anonymized and deactivated",
                deletedAt = Instant.now(),
            )
        `when`(gdprService.requestDeletion(targetUserId, UUID.fromString(actorId))).thenReturn(deletion)

        val response = controller.deleteUserData(targetUserId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo("ok")
    }
}
