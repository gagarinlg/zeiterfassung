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
import com.zeiterfassung.model.dto.TrackingStatusResponse
import com.zeiterfassung.service.TimeTrackingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

@RestController
@RequestMapping("/time")
@Tag(name = "Time Tracking")
@SecurityRequirement(name = "bearerAuth")
class TimeTrackingController(
    private val timeTrackingService: TimeTrackingService,
) {
    @PostMapping("/clock-in")
    @PreAuthorize("hasAuthority('time.track.own')")
    @Operation(summary = "Clock in", description = "Records a clock-in event for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Clocked in successfully")
    @ApiResponse(responseCode = "409", description = "Already clocked in")
    fun clockIn(
        @RequestBody request: ClockInRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.clockIn(UUID.fromString(actorId), request.source, request.notes, request.terminalId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAuthority('time.track.own')")
    @Operation(summary = "Clock out", description = "Records a clock-out event for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Clocked out successfully")
    @ApiResponse(responseCode = "409", description = "Not currently clocked in")
    fun clockOut(
        @RequestBody request: ClockOutRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.clockOut(UUID.fromString(actorId), request.source, request.notes, request.terminalId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/break/start")
    @PreAuthorize("hasAuthority('time.track.own')")
    @Operation(summary = "Start break", description = "Records the start of a break for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Break started")
    fun startBreak(
        @RequestBody request: BreakStartRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.startBreak(UUID.fromString(actorId), request.source, request.notes)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/break/end")
    @PreAuthorize("hasAuthority('time.track.own')")
    @Operation(summary = "End break", description = "Records the end of a break for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Break ended")
    fun endBreak(
        @RequestBody request: BreakEndRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.endBreak(UUID.fromString(actorId), request.source, request.notes)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(
        summary = "Get current tracking status",
        description = "Returns the current clock-in/break status of the authenticated user.",
    )
    @ApiResponse(responseCode = "200", description = "Current status returned")
    fun getStatus(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TrackingStatusResponse> = ResponseEntity.ok(timeTrackingService.getCurrentStatus(UUID.fromString(actorId)))

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(summary = "Get today's summary", description = "Returns a summary of today's work hours, breaks, and compliance.")
    @ApiResponse(responseCode = "200", description = "Today's summary returned")
    fun getToday(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<DailySummaryResponse> =
        ResponseEntity.ok(timeTrackingService.getDailySummary(UUID.fromString(actorId), LocalDate.now(ZoneOffset.UTC)))

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(
        summary = "Get time entries",
        description = "Returns time entries for the authenticated user within the specified date range.",
    )
    @ApiResponse(responseCode = "200", description = "Time entries returned")
    fun getEntries(
        @RequestParam start: Instant,
        @RequestParam end: Instant,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<List<TimeEntryResponse>> =
        ResponseEntity.ok(timeTrackingService.getEntriesForUser(UUID.fromString(actorId), start, end))

    @GetMapping("/summary/daily/{date}")
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(summary = "Get daily summary", description = "Returns a work summary for a specific date.")
    @ApiResponse(responseCode = "200", description = "Daily summary returned")
    fun getDailySummary(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<DailySummaryResponse> = ResponseEntity.ok(timeTrackingService.getDailySummary(UUID.fromString(actorId), date))

    @GetMapping("/summary/weekly")
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(summary = "Get weekly summary", description = "Returns a timesheet for the week starting on the specified date.")
    @ApiResponse(responseCode = "200", description = "Weekly summary returned")
    fun getWeeklySummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) weekStart: LocalDate,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeSheetResponse> {
        val userId = UUID.fromString(actorId)
        return ResponseEntity.ok(timeTrackingService.getTimeSheet(userId, weekStart, weekStart.plusDays(6)))
    }

    @GetMapping("/summary/monthly")
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(summary = "Get monthly summary", description = "Returns a timesheet for the specified month.")
    @ApiResponse(responseCode = "200", description = "Monthly summary returned")
    fun getMonthlySummary(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeSheetResponse> {
        val userId = UUID.fromString(actorId)
        val yearMonth = YearMonth.of(year, month)
        return ResponseEntity.ok(
            timeTrackingService.getTimeSheet(userId, yearMonth.atDay(1), yearMonth.atEndOfMonth()),
        )
    }

    @GetMapping("/timesheet")
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(summary = "Get timesheet", description = "Returns a timesheet for a custom date range.")
    @ApiResponse(responseCode = "200", description = "Timesheet returned")
    fun getTimeSheet(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeSheetResponse> = ResponseEntity.ok(timeTrackingService.getTimeSheet(UUID.fromString(actorId), start, end))

    @GetMapping("/manage/team/entries")
    @PreAuthorize("hasAuthority('time.view.team')")
    @Operation(
        summary = "Get team member entries",
        description = "Returns time entries for a specific team member. Requires team view permission.",
    )
    @ApiResponse(responseCode = "200", description = "Team member entries returned")
    @ApiResponse(responseCode = "403", description = "Not authorized to view this team member")
    fun getTeamEntries(
        @RequestParam userId: UUID,
        @RequestParam start: Instant,
        @RequestParam end: Instant,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<List<TimeEntryResponse>> =
        ResponseEntity.ok(timeTrackingService.getTeamMemberEntries(UUID.fromString(actorId), userId, start, end))

    @PostMapping("/manage/entry")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(
        summary = "Add manual time entry",
        description = "Creates a manual time entry for a team member. Requires team edit permission.",
    )
    @ApiResponse(responseCode = "201", description = "Manual entry created")
    fun addManualEntry(
        @Valid @RequestBody request: ManualTimeEntryRequest,
        @RequestParam userId: UUID,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.addManualEntry(UUID.fromString(actorId), userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/manage/entry/{id}")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(summary = "Edit time entry", description = "Edits an existing time entry for a team member.")
    @ApiResponse(responseCode = "200", description = "Time entry updated")
    @ApiResponse(responseCode = "404", description = "Time entry not found")
    fun editTimeEntry(
        @PathVariable id: UUID,
        @Valid @RequestBody request: EditTimeEntryRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> = ResponseEntity.ok(timeTrackingService.editTimeEntry(UUID.fromString(actorId), id, request))

    @DeleteMapping("/manage/entry/{id}")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(summary = "Delete time entry", description = "Soft-deletes a time entry with a mandatory reason.")
    @ApiResponse(responseCode = "204", description = "Time entry deleted")
    @ApiResponse(responseCode = "404", description = "Time entry not found")
    fun deleteTimeEntry(
        @PathVariable id: UUID,
        @RequestParam reason: String,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Void> {
        timeTrackingService.deleteTimeEntry(UUID.fromString(actorId), id, reason)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/manage/team/status")
    @PreAuthorize("hasAuthority('time.view.team')")
    @Operation(summary = "Get team status", description = "Returns the current clock-in/break status of all team members.")
    @ApiResponse(responseCode = "200", description = "Team status returned")
    fun getTeamStatus(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Map<UUID, TrackingStatusResponse>> =
        ResponseEntity.ok(timeTrackingService.getTeamCurrentStatus(UUID.fromString(actorId)))

    @GetMapping("/export/csv", produces = ["text/csv"])
    @PreAuthorize("hasAuthority('time.view.own')")
    @Operation(summary = "Export timesheet as CSV", description = "Exports the timesheet for a date range as a CSV file download.")
    @ApiResponse(responseCode = "200", description = "CSV file returned")
    fun exportCsv(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @AuthenticationPrincipal actorId: String,
        response: jakarta.servlet.http.HttpServletResponse,
    ) {
        val userId = UUID.fromString(actorId)
        val sheet = timeTrackingService.getTimeSheet(userId, start, end)
        response.setHeader("Content-Disposition", "attachment; filename=\"timesheet_$start-$end.csv\"")
        val writer = response.writer
        writer.println("Date,Work Minutes,Break Minutes,Overtime Minutes,Compliant,Notes")
        sheet.dailySummaries.forEach { summary ->
            // Sanitise notes to prevent CSV injection: escape quotes (RFC 4180)
            // and neutralise formula-injection prefixes (=, +, -, @, TAB, CR/LF).
            val rawNotes = summary.complianceNotes ?: ""
            val escapedNotes = rawNotes.replace("\"", "\"\"")
            val safeNotes =
                if (escapedNotes.isNotEmpty() && escapedNotes[0] in setOf('=', '+', '-', '@', '\t', '\r', '\n')) {
                    " $escapedNotes"
                } else {
                    escapedNotes
                }
            writer.println(
                "${summary.date},${summary.totalWorkMinutes},${summary.totalBreakMinutes}," +
                    "${summary.overtimeMinutes},${summary.isCompliant},\"$safeNotes\"",
            )
        }
        writer.flush()
    }
}
