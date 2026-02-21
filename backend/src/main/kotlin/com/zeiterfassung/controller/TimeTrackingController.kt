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
class TimeTrackingController(
    private val timeTrackingService: TimeTrackingService,
) {
    @PostMapping("/clock-in")
    @PreAuthorize("hasAuthority('time.track.own')")
    fun clockIn(
        @RequestBody request: ClockInRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.clockIn(UUID.fromString(actorId), request.source, request.notes, request.terminalId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAuthority('time.track.own')")
    fun clockOut(
        @RequestBody request: ClockOutRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.clockOut(UUID.fromString(actorId), request.source, request.notes, request.terminalId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/break/start")
    @PreAuthorize("hasAuthority('time.track.own')")
    fun startBreak(
        @RequestBody request: BreakStartRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.startBreak(UUID.fromString(actorId), request.source, request.notes)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/break/end")
    @PreAuthorize("hasAuthority('time.track.own')")
    fun endBreak(
        @RequestBody request: BreakEndRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> {
        val response = timeTrackingService.endBreak(UUID.fromString(actorId), request.source, request.notes)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('time.view.own')")
    fun getStatus(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TrackingStatusResponse> = ResponseEntity.ok(timeTrackingService.getCurrentStatus(UUID.fromString(actorId)))

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('time.view.own')")
    fun getToday(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<DailySummaryResponse> =
        ResponseEntity.ok(timeTrackingService.getDailySummary(UUID.fromString(actorId), LocalDate.now(ZoneOffset.UTC)))

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('time.view.own')")
    fun getEntries(
        @RequestParam start: Instant,
        @RequestParam end: Instant,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<List<TimeEntryResponse>> =
        ResponseEntity.ok(timeTrackingService.getEntriesForUser(UUID.fromString(actorId), start, end))

    @GetMapping("/summary/daily/{date}")
    @PreAuthorize("hasAuthority('time.view.own')")
    fun getDailySummary(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<DailySummaryResponse> = ResponseEntity.ok(timeTrackingService.getDailySummary(UUID.fromString(actorId), date))

    @GetMapping("/summary/weekly")
    @PreAuthorize("hasAuthority('time.view.own')")
    fun getWeeklySummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) weekStart: LocalDate,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeSheetResponse> {
        val userId = UUID.fromString(actorId)
        return ResponseEntity.ok(timeTrackingService.getTimeSheet(userId, weekStart, weekStart.plusDays(6)))
    }

    @GetMapping("/summary/monthly")
    @PreAuthorize("hasAuthority('time.view.own')")
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
    fun getTimeSheet(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeSheetResponse> = ResponseEntity.ok(timeTrackingService.getTimeSheet(UUID.fromString(actorId), start, end))

    @GetMapping("/manage/team/entries")
    @PreAuthorize("hasAuthority('time.view.team')")
    fun getTeamEntries(
        @RequestParam userId: UUID,
        @RequestParam start: Instant,
        @RequestParam end: Instant,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<List<TimeEntryResponse>> =
        ResponseEntity.ok(timeTrackingService.getTeamMemberEntries(UUID.fromString(actorId), userId, start, end))

    @PostMapping("/manage/entry")
    @PreAuthorize("hasAuthority('time.edit.team')")
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
    fun editTimeEntry(
        @PathVariable id: UUID,
        @Valid @RequestBody request: EditTimeEntryRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<TimeEntryResponse> = ResponseEntity.ok(timeTrackingService.editTimeEntry(UUID.fromString(actorId), id, request))

    @DeleteMapping("/manage/entry/{id}")
    @PreAuthorize("hasAuthority('time.edit.team')")
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
    fun getTeamStatus(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Map<UUID, TrackingStatusResponse>> =
        ResponseEntity.ok(timeTrackingService.getTeamCurrentStatus(UUID.fromString(actorId)))
}
