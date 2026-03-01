package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.BreakEndRequest
import com.zeiterfassung.model.dto.BreakStartRequest
import com.zeiterfassung.model.dto.ClockInRequest
import com.zeiterfassung.model.dto.ClockOutRequest
import com.zeiterfassung.model.dto.DailySummaryResponse
import com.zeiterfassung.model.dto.EditTimeEntryRequest
import com.zeiterfassung.model.dto.ManualTimeEntryRequest
import com.zeiterfassung.model.dto.TimeEntryResponse
import com.zeiterfassung.model.dto.TimeSheetResponse
import com.zeiterfassung.model.dto.TrackingStatus
import com.zeiterfassung.model.dto.TrackingStatusResponse
import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
import com.zeiterfassung.service.TimeTrackingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TimeTrackingControllerTest {
    @Mock
    private lateinit var timeTrackingService: TimeTrackingService

    private lateinit var controller: TimeTrackingController

    private val userId = UUID.randomUUID()
    private val actorId = userId.toString()

    private fun sampleTimeEntry(type: TimeEntryType = TimeEntryType.CLOCK_IN): TimeEntryResponse =
        TimeEntryResponse(
            id = UUID.randomUUID(),
            userId = userId,
            entryType = type,
            timestamp = Instant.now(),
            source = TimeEntrySource.WEB,
            terminalId = null,
            notes = null,
            isModified = false,
            modifiedById = null,
            createdAt = Instant.now(),
        )

    private fun sampleDailySummary(date: LocalDate = LocalDate.now()): DailySummaryResponse =
        DailySummaryResponse(
            id = UUID.randomUUID(),
            userId = userId,
            date = date,
            totalWorkMinutes = 480,
            totalBreakMinutes = 30,
            overtimeMinutes = 0,
            isCompliant = true,
            complianceNotes = null,
        )

    @BeforeEach
    fun setUp() {
        controller = TimeTrackingController(timeTrackingService)
    }

    @Test
    fun `clockIn should return 201 with time entry`() {
        val request = ClockInRequest(notes = null, source = TimeEntrySource.WEB, terminalId = null)
        val expected = sampleTimeEntry(TimeEntryType.CLOCK_IN)
        `when`(timeTrackingService.clockIn(userId, TimeEntrySource.WEB, null, null)).thenReturn(expected)

        val response = controller.clockIn(request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.entryType).isEqualTo(TimeEntryType.CLOCK_IN)
    }

    @Test
    fun `clockOut should return 201 with time entry`() {
        val request = ClockOutRequest(notes = "Done for today", source = TimeEntrySource.WEB, terminalId = null)
        val expected = sampleTimeEntry(TimeEntryType.CLOCK_OUT)
        `when`(timeTrackingService.clockOut(userId, TimeEntrySource.WEB, "Done for today", null)).thenReturn(expected)

        val response = controller.clockOut(request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `startBreak should return 201 with time entry`() {
        val request = BreakStartRequest(notes = null, source = TimeEntrySource.WEB)
        val expected = sampleTimeEntry(TimeEntryType.BREAK_START)
        `when`(timeTrackingService.startBreak(userId, TimeEntrySource.WEB, null)).thenReturn(expected)

        val response = controller.startBreak(request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `endBreak should return 201 with time entry`() {
        val request = BreakEndRequest(notes = null, source = TimeEntrySource.WEB)
        val expected = sampleTimeEntry(TimeEntryType.BREAK_END)
        `when`(timeTrackingService.endBreak(userId, TimeEntrySource.WEB, null)).thenReturn(expected)

        val response = controller.endBreak(request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `getStatus should return current tracking status`() {
        val status =
            TrackingStatusResponse(
                status = TrackingStatus.CLOCKED_IN,
                clockedInSince = Instant.now(),
                breakStartedAt = null,
                elapsedWorkMinutes = 120,
                elapsedBreakMinutes = 0,
                todayWorkMinutes = 120,
                todayBreakMinutes = 0,
            )
        `when`(timeTrackingService.getCurrentStatus(userId)).thenReturn(status)

        val response = controller.getStatus(actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo(TrackingStatus.CLOCKED_IN)
    }

    @Test
    fun `getDailySummary should return summary for date`() {
        val date = LocalDate.of(2026, 6, 1)
        val summary = sampleDailySummary(date)
        `when`(timeTrackingService.getDailySummary(userId, date)).thenReturn(summary)

        val response = controller.getDailySummary(date, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.date).isEqualTo(date)
    }

    @Test
    fun `getEntries should return entries for date range`() {
        val start = Instant.parse("2026-06-01T00:00:00Z")
        val end = Instant.parse("2026-06-07T23:59:59Z")
        val entries = listOf(sampleTimeEntry())
        `when`(timeTrackingService.getEntriesForUser(userId, start, end)).thenReturn(entries)

        val response = controller.getEntries(start, end, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }

    @Test
    fun `getWeeklySummary should return timesheet for week`() {
        val weekStart = LocalDate.of(2026, 6, 1)
        val weekEnd = weekStart.plusDays(6)
        val sheet =
            TimeSheetResponse(
                userId = userId,
                startDate = weekStart,
                endDate = weekEnd,
                dailySummaries = emptyList(),
                totalWorkMinutes = 2400,
                totalBreakMinutes = 150,
                totalOvertimeMinutes = 0,
                entries = emptyList(),
            )
        `when`(timeTrackingService.getTimeSheet(userId, weekStart, weekEnd)).thenReturn(sheet)

        val response = controller.getWeeklySummary(weekStart, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getMonthlySummary should return timesheet for month`() {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 30)
        val sheet =
            TimeSheetResponse(
                userId = userId,
                startDate = start,
                endDate = end,
                dailySummaries = emptyList(),
                totalWorkMinutes = 9600,
                totalBreakMinutes = 600,
                totalOvertimeMinutes = 0,
                entries = emptyList(),
            )
        `when`(timeTrackingService.getTimeSheet(userId, start, end)).thenReturn(sheet)

        val response = controller.getMonthlySummary(2026, 6, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getTimeSheet should return timesheet for custom range`() {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 15)
        val sheet =
            TimeSheetResponse(
                userId = userId,
                startDate = start,
                endDate = end,
                dailySummaries = emptyList(),
                totalWorkMinutes = 4800,
                totalBreakMinutes = 300,
                totalOvertimeMinutes = 0,
                entries = emptyList(),
            )
        `when`(timeTrackingService.getTimeSheet(userId, start, end)).thenReturn(sheet)

        val response = controller.getTimeSheet(start, end, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getTeamEntries should return entries for team member`() {
        val teamMemberId = UUID.randomUUID()
        val start = Instant.parse("2026-06-01T00:00:00Z")
        val end = Instant.parse("2026-06-07T23:59:59Z")
        val entries = listOf(sampleTimeEntry())
        `when`(timeTrackingService.getTeamMemberEntries(userId, teamMemberId, start, end)).thenReturn(entries)

        val response = controller.getTeamEntries(teamMemberId, start, end, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }

    @Test
    fun `addManualEntry should return 201 with time entry`() {
        val targetUserId = UUID.randomUUID()
        val request =
            ManualTimeEntryRequest(
                reason = "Forgotten clock-in",
                entryType = TimeEntryType.CLOCK_IN,
                timestamp = Instant.now(),
                notes = null,
                source = TimeEntrySource.WEB,
            )
        val expected = sampleTimeEntry()
        `when`(timeTrackingService.addManualEntry(userId, targetUserId, request)).thenReturn(expected)

        val response = controller.addManualEntry(request, targetUserId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `editTimeEntry should return updated entry`() {
        val entryId = UUID.randomUUID()
        val request = EditTimeEntryRequest(reason = "Time correction", timestamp = Instant.now(), notes = null)
        val expected = sampleTimeEntry()
        `when`(timeTrackingService.editTimeEntry(userId, entryId, request)).thenReturn(expected)

        val response = controller.editTimeEntry(entryId, request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `deleteTimeEntry should return no content`() {
        val entryId = UUID.randomUUID()

        val response = controller.deleteTimeEntry(entryId, "Entry was duplicate", actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(timeTrackingService).deleteTimeEntry(userId, entryId, "Entry was duplicate")
    }

    @Test
    fun `getTeamStatus should return team status map`() {
        val teamMemberId = UUID.randomUUID()
        val statusMap =
            mapOf(
                teamMemberId to
                    TrackingStatusResponse(
                        status = TrackingStatus.CLOCKED_IN,
                        clockedInSince = Instant.now(),
                        breakStartedAt = null,
                        elapsedWorkMinutes = 60,
                        elapsedBreakMinutes = 0,
                        todayWorkMinutes = 60,
                        todayBreakMinutes = 0,
                    ),
            )
        `when`(timeTrackingService.getTeamCurrentStatus(userId)).thenReturn(statusMap)

        val response = controller.getTeamStatus(actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).containsKey(teamMemberId)
    }

    @Test
    fun `exportCsv should write CSV with header and summaries`() {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 2)
        val sheet =
            TimeSheetResponse(
                userId = userId,
                startDate = start,
                endDate = end,
                dailySummaries =
                    listOf(
                        sampleDailySummary(start),
                        DailySummaryResponse(
                            id = UUID.randomUUID(),
                            userId = userId,
                            date = end,
                            totalWorkMinutes = 500,
                            totalBreakMinutes = 45,
                            overtimeMinutes = 20,
                            isCompliant = true,
                            complianceNotes = "All good",
                        ),
                    ),
                totalWorkMinutes = 980,
                totalBreakMinutes = 75,
                totalOvertimeMinutes = 20,
                entries = emptyList(),
            )
        `when`(timeTrackingService.getTimeSheet(userId, start, end)).thenReturn(sheet)

        val httpResponse = MockHttpServletResponse()
        controller.exportCsv(start, end, actorId, httpResponse)

        val csv = httpResponse.contentAsString
        assertThat(csv).contains("Date,Work Minutes,Break Minutes,Overtime Minutes,Compliant,Notes")
        assertThat(csv).contains("2026-06-01")
        assertThat(csv).contains("2026-06-02")
        assertThat(httpResponse.getHeader("Content-Disposition")).contains("timesheet_")
    }

    @Test
    fun `exportCsv should sanitise CSV injection prefixes in notes`() {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 1)
        val sheet =
            TimeSheetResponse(
                userId = userId,
                startDate = start,
                endDate = end,
                dailySummaries =
                    listOf(
                        DailySummaryResponse(
                            id = UUID.randomUUID(),
                            userId = userId,
                            date = start,
                            totalWorkMinutes = 480,
                            totalBreakMinutes = 30,
                            overtimeMinutes = 0,
                            isCompliant = false,
                            complianceNotes = "=cmd('malicious')",
                        ),
                    ),
                totalWorkMinutes = 480,
                totalBreakMinutes = 30,
                totalOvertimeMinutes = 0,
                entries = emptyList(),
            )
        `when`(timeTrackingService.getTimeSheet(userId, start, end)).thenReturn(sheet)

        val httpResponse = MockHttpServletResponse()
        controller.exportCsv(start, end, actorId, httpResponse)

        val csv = httpResponse.contentAsString
        // The formula prefix '=' should be neutralised with a leading space
        assertThat(csv).contains(" =cmd")
        assertThat(csv).doesNotContain(",\"=cmd")
    }

    @Test
    fun `exportCsv should handle null compliance notes`() {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 1)
        val sheet =
            TimeSheetResponse(
                userId = userId,
                startDate = start,
                endDate = end,
                dailySummaries = listOf(sampleDailySummary(start)),
                totalWorkMinutes = 480,
                totalBreakMinutes = 30,
                totalOvertimeMinutes = 0,
                entries = emptyList(),
            )
        `when`(timeTrackingService.getTimeSheet(userId, start, end)).thenReturn(sheet)

        val httpResponse = MockHttpServletResponse()
        controller.exportCsv(start, end, actorId, httpResponse)

        val csv = httpResponse.contentAsString
        assertThat(csv).contains("\"\"")
    }
}
