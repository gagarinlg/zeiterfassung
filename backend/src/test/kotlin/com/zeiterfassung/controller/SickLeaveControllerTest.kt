package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.CreateSickLeaveRequest
import com.zeiterfassung.model.dto.SickLeaveResponse
import com.zeiterfassung.model.dto.UpdateSickLeaveRequest
import com.zeiterfassung.model.enums.SickLeaveStatus
import com.zeiterfassung.service.SickLeaveService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SickLeaveControllerTest {
    @Mock
    private lateinit var sickLeaveService: SickLeaveService

    private lateinit var controller: SickLeaveController

    private val userId = UUID.randomUUID()
    private val actorId = userId.toString()

    private fun sampleSickLeave(id: UUID = UUID.randomUUID()): SickLeaveResponse =
        SickLeaveResponse(
            id = id,
            userId = userId,
            userName = "Max Mustermann",
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 7, 3),
            status = SickLeaveStatus.REPORTED,
            hasCertificate = false,
            certificateSubmittedAt = null,
            notes = "Flu",
            reportedById = null,
            reportedByName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = SickLeaveController(sickLeaveService)
    }

    @Test
    fun `reportSickLeave should return 201 with sick leave`() {
        val dto = CreateSickLeaveRequest(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "Flu")
        val expected = sampleSickLeave()
        `when`(sickLeaveService.reportSickLeave(userId, dto)).thenReturn(expected)

        val response = controller.reportSickLeave(dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `reportSickLeaveForUser should return 201 with sick leave`() {
        val targetUserId = UUID.randomUUID()
        val dto = CreateSickLeaveRequest(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), null)
        val expected = sampleSickLeave()
        `when`(sickLeaveService.reportSickLeaveByManager(userId, targetUserId, dto)).thenReturn(expected)

        val response = controller.reportSickLeaveForUser(targetUserId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `updateSickLeave should return updated sick leave`() {
        val sickLeaveId = UUID.randomUUID()
        val dto = UpdateSickLeaveRequest(endDate = LocalDate.of(2026, 7, 5))
        val expected = sampleSickLeave(sickLeaveId)
        `when`(sickLeaveService.updateSickLeave(sickLeaveId, userId, dto)).thenReturn(expected)

        val response = controller.updateSickLeave(sickLeaveId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `cancelSickLeave should return no content`() {
        val sickLeaveId = UUID.randomUUID()

        val response = controller.cancelSickLeave(sickLeaveId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(sickLeaveService).cancelSickLeave(sickLeaveId, userId)
    }

    @Test
    fun `getMySickLeaves should return paginated sick leaves`() {
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
        val page = PageImpl(listOf(sampleSickLeave()), pageable, 1)
        `when`(sickLeaveService.getUserSickLeaves(userId, pageable)).thenReturn(page)

        val response = controller.getMySickLeaves(actorId, 0, 20)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }

    @Test
    fun `getSickLeave should return sick leave by id`() {
        val sickLeaveId = UUID.randomUUID()
        val expected = sampleSickLeave(sickLeaveId)
        `when`(sickLeaveService.getSickLeave(sickLeaveId)).thenReturn(expected)

        val response = controller.getSickLeave(sickLeaveId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo(sickLeaveId)
    }

    @Test
    fun `submitCertificate should return updated sick leave`() {
        val sickLeaveId = UUID.randomUUID()
        val expected = sampleSickLeave(sickLeaveId)
        `when`(sickLeaveService.submitCertificate(sickLeaveId, userId)).thenReturn(expected)

        val response = controller.submitCertificate(sickLeaveId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}
