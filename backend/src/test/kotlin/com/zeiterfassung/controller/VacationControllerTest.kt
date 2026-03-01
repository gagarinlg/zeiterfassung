package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.ApproveVacationRequest
import com.zeiterfassung.model.dto.CreateVacationRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.PublicHolidayResponse
import com.zeiterfassung.model.dto.RejectVacationRequest
import com.zeiterfassung.model.dto.SetVacationBalanceRequest
import com.zeiterfassung.model.dto.UpdateVacationRequest
import com.zeiterfassung.model.dto.VacationBalanceResponse
import com.zeiterfassung.model.dto.VacationCalendarResponse
import com.zeiterfassung.model.dto.VacationRequestResponse
import com.zeiterfassung.model.enums.VacationStatus
import com.zeiterfassung.service.VacationService
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class VacationControllerTest {
    @Mock
    private lateinit var vacationService: VacationService

    private lateinit var controller: VacationController

    private val userId = UUID.randomUUID()
    private val actorId = userId.toString()

    private fun sampleVacationResponse(id: UUID = UUID.randomUUID()): VacationRequestResponse =
        VacationRequestResponse(
            id = id,
            userId = userId,
            userName = "Max Mustermann",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 5),
            isHalfDayStart = false,
            isHalfDayEnd = false,
            totalDays = BigDecimal("5"),
            status = VacationStatus.PENDING,
            approvedById = null,
            approvedByName = null,
            rejectionReason = null,
            notes = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = VacationController(vacationService)
    }

    @Test
    fun `createRequest should return 201 with vacation response`() {
        val dto = CreateVacationRequest(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), false, false, null)
        val expected = sampleVacationResponse()
        `when`(vacationService.createRequest(userId, dto)).thenReturn(expected)

        val response = controller.createRequest(dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isEqualTo(expected)
    }

    @Test
    fun `getMyRequests should return paginated vacation requests`() {
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
        val page = PageImpl(listOf(sampleVacationResponse()), pageable, 1)
        `when`(vacationService.getUserRequests(userId, null, null, pageable)).thenReturn(page)

        val response = controller.getMyRequests(actorId, null, null, 0, 20)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
        assertThat(response.body!!.totalElements).isEqualTo(1L)
    }

    @Test
    fun `getRequest should return vacation request by id`() {
        val requestId = UUID.randomUUID()
        val expected = sampleVacationResponse(requestId)
        `when`(vacationService.getRequest(requestId, userId)).thenReturn(expected)

        val response = controller.getRequest(requestId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo(requestId)
    }

    @Test
    fun `updateRequest should return updated vacation request`() {
        val requestId = UUID.randomUUID()
        val dto = UpdateVacationRequest(endDate = LocalDate.of(2026, 6, 10))
        val expected = sampleVacationResponse(requestId)
        `when`(vacationService.updateRequest(requestId, userId, dto)).thenReturn(expected)

        val response = controller.updateRequest(requestId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `cancelRequest should return no content`() {
        val requestId = UUID.randomUUID()

        val response = controller.cancelRequest(requestId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(vacationService).cancelRequest(requestId, userId)
    }

    @Test
    fun `approveRequest should return approved vacation request`() {
        val requestId = UUID.randomUUID()
        val dto = ApproveVacationRequest(notes = "Approved")
        val expected = sampleVacationResponse(requestId)
        `when`(vacationService.approveRequest(requestId, userId, dto)).thenReturn(expected)

        val response = controller.approveRequest(requestId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `approveRequest should use default ApproveVacationRequest when dto is null`() {
        val requestId = UUID.randomUUID()
        val defaultDto = ApproveVacationRequest()
        val expected = sampleVacationResponse(requestId)
        `when`(vacationService.approveRequest(requestId, userId, defaultDto)).thenReturn(expected)

        val response = controller.approveRequest(requestId, null, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `rejectRequest should return rejected vacation request`() {
        val requestId = UUID.randomUUID()
        val dto = RejectVacationRequest("Schedule conflict")
        val expected = sampleVacationResponse(requestId)
        `when`(vacationService.rejectRequest(requestId, userId, dto)).thenReturn(expected)

        val response = controller.rejectRequest(requestId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getPendingRequests should return paginated pending requests`() {
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt").ascending())
        val page = PageImpl(listOf(sampleVacationResponse()), pageable, 1)
        `when`(vacationService.getPendingRequests(userId, pageable)).thenReturn(page)

        val response = controller.getPendingRequests(actorId, 0, 20, false)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }

    @Test
    fun `getPendingRequests should return all pending when allRequests is true`() {
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt").ascending())
        val page = PageImpl(listOf(sampleVacationResponse()), pageable, 1)
        `when`(vacationService.getAllPendingRequests(pageable)).thenReturn(page)

        val response = controller.getPendingRequests(actorId, 0, 20, true)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getMyBalance should return vacation balance`() {
        val balance =
            VacationBalanceResponse(
                id = UUID.randomUUID(),
                userId = userId,
                year = 2026,
                totalDays = BigDecimal("30"),
                usedDays = BigDecimal("5"),
                carriedOverDays = BigDecimal("3"),
                remainingDays = BigDecimal("25"),
                pendingDays = BigDecimal("5"),
            )
        `when`(vacationService.getBalance(userId, 2026)).thenReturn(balance)

        val response = controller.getMyBalance(actorId, 2026)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.remainingDays).isEqualTo(BigDecimal("25"))
    }

    @Test
    fun `getUserBalance should return balance for specified user`() {
        val targetUserId = UUID.randomUUID()
        val balance =
            VacationBalanceResponse(
                id = UUID.randomUUID(),
                userId = targetUserId,
                year = 2026,
                totalDays = BigDecimal("30"),
                usedDays = BigDecimal("10"),
                carriedOverDays = BigDecimal.ZERO,
                remainingDays = BigDecimal("20"),
                pendingDays = BigDecimal.ZERO,
            )
        `when`(vacationService.getBalanceForManager(userId, targetUserId, 2026)).thenReturn(balance)

        val response = controller.getUserBalance(targetUserId, 2026, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getPublicHolidays should return holidays list`() {
        val holidays =
            listOf(
                PublicHolidayResponse(UUID.randomUUID(), LocalDate.of(2026, 1, 1), "Neujahr", null, true),
            )
        `when`(vacationService.getPublicHolidays(2026, null)).thenReturn(holidays)

        val response = controller.getPublicHolidays(2026, null)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }

    @Test
    fun `getTeamCalendar should return vacation calendar`() {
        val calendar =
            VacationCalendarResponse(
                year = 2026,
                month = 6,
                ownRequests = emptyList(),
                teamRequests = emptyList(),
                publicHolidays = emptyList(),
            )
        `when`(vacationService.getTeamCalendar(userId, 2026, 6)).thenReturn(calendar)

        val response = controller.getTeamCalendar(actorId, 2026, 6)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.month).isEqualTo(6)
    }

    @Test
    fun `setBalance should update and return vacation balance`() {
        val targetUserId = UUID.randomUUID()
        val dto = SetVacationBalanceRequest(totalDays = BigDecimal("25"), usedDays = null, carriedOverDays = null)
        val balance =
            VacationBalanceResponse(
                id = UUID.randomUUID(),
                userId = targetUserId,
                year = 2026,
                totalDays = BigDecimal("25"),
                usedDays = BigDecimal.ZERO,
                carriedOverDays = BigDecimal.ZERO,
                remainingDays = BigDecimal("25"),
                pendingDays = BigDecimal.ZERO,
            )
        `when`(vacationService.setBalance(targetUserId, 2026, dto.totalDays, dto.usedDays, dto.carriedOverDays, userId))
            .thenReturn(balance)

        val response = controller.setBalance(targetUserId, 2026, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `triggerCarryOver should return updated balance`() {
        val targetUserId = UUID.randomUUID()
        val balance =
            VacationBalanceResponse(
                id = UUID.randomUUID(),
                userId = targetUserId,
                year = 2026,
                totalDays = BigDecimal("30"),
                usedDays = BigDecimal.ZERO,
                carriedOverDays = BigDecimal("5"),
                remainingDays = BigDecimal("35"),
                pendingDays = BigDecimal.ZERO,
            )
        `when`(vacationService.triggerCarryOver(targetUserId, 2026, userId)).thenReturn(balance)

        val response = controller.triggerCarryOver(targetUserId, 2026, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.carriedOverDays).isEqualTo(BigDecimal("5"))
    }
}
