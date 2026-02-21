package com.zeiterfassung.controller

import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.EmployeeInfo
import com.zeiterfassung.model.dto.TerminalScanRequest
import com.zeiterfassung.model.dto.TerminalScanResponse
import com.zeiterfassung.model.dto.TrackingStatus
import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.service.EmployeeConfigService
import com.zeiterfassung.service.TimeTrackingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/terminal")
class TerminalController(
    private val userRepository: UserRepository,
    private val timeTrackingService: TimeTrackingService,
    private val employeeConfigService: EmployeeConfigService,
) {
    @PostMapping("/scan")
    fun scan(
        @RequestBody request: TerminalScanRequest,
    ): ResponseEntity<TerminalScanResponse> {
        val user =
            userRepository.findByRfidTagId(request.rfidTagId)
                .orElseThrow { ResourceNotFoundException("No employee found for RFID tag: ${request.rfidTagId}") }

        val status = timeTrackingService.getCurrentStatus(user.id)
        val action: String
        val timestamp = Instant.now()

        if (status.status == TrackingStatus.CLOCKED_IN || status.status == TrackingStatus.ON_BREAK) {
            timeTrackingService.clockOut(user.id, TimeEntrySource.TERMINAL, terminalId = request.terminalId)
            action = TimeEntryType.CLOCK_OUT.name
        } else {
            timeTrackingService.clockIn(user.id, TimeEntrySource.TERMINAL, terminalId = request.terminalId)
            action = TimeEntryType.CLOCK_IN.name
        }

        val today = LocalDate.now(ZoneOffset.UTC)
        val summary = timeTrackingService.getDailySummary(user.id, today)
        val config = employeeConfigService.getConfig(user.id)

        return ResponseEntity.ok(
            TerminalScanResponse(
                employee =
                    EmployeeInfo(
                        id = user.id,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        photoUrl = user.photoUrl,
                    ),
                action = action,
                timestamp = timestamp,
                todayWorkMinutes = summary.totalWorkMinutes,
                todayBreakMinutes = summary.totalBreakMinutes,
                overtimeMinutes = summary.overtimeMinutes,
                // TODO: replace with actual remaining days once vacation module is implemented
                remainingVacationDays = config.vacationDaysPerYear.toFloat(),
            ),
        )
    }
}
