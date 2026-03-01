package com.zeiterfassung.service

import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.EmployeeInfo
import com.zeiterfassung.model.dto.TerminalScanResponse
import com.zeiterfassung.model.dto.TrackingStatus
import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
import com.zeiterfassung.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
class TerminalService(
    private val userRepository: UserRepository,
    private val timeTrackingService: TimeTrackingService,
    private val vacationService: VacationService,
) {
    /**
     * Processes an RFID scan from a terminal device.
     *
     * Looks up the employee by RFID tag, toggles their clock-in/out state,
     * and returns a response with today's summary and vacation balance.
     *
     * @throws ResourceNotFoundException if no employee is registered for the given RFID tag
     */
    fun scan(
        rfidTagId: String,
        terminalId: String,
    ): TerminalScanResponse {
        val user =
            userRepository
                .findByRfidTagId(rfidTagId)
                .orElseThrow { ResourceNotFoundException("No employee found for RFID tag: $rfidTagId") }

        val entryType = performClockToggle(user.id, terminalId)
        val summary = timeTrackingService.getDailySummary(user.id, LocalDate.now(ZoneOffset.UTC))
        val remainingVacationDays = getRemainingVacationDays(user.id)

        return TerminalScanResponse(
            employee =
                EmployeeInfo(
                    id = user.id,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    photoUrl = user.photoUrl,
                ),
            entryType = entryType,
            timestamp = Instant.now(),
            todayWorkMinutes = summary.totalWorkMinutes,
            todayBreakMinutes = summary.totalBreakMinutes,
            overtimeMinutes = summary.overtimeMinutes,
            remainingVacationDays = remainingVacationDays,
        )
    }

    private fun performClockToggle(
        userId: UUID,
        terminalId: String,
    ): String {
        val status = timeTrackingService.getCurrentStatus(userId)
        return if (status.status == TrackingStatus.CLOCKED_IN || status.status == TrackingStatus.ON_BREAK) {
            timeTrackingService.clockOut(userId, TimeEntrySource.TERMINAL, terminalId = terminalId)
            TimeEntryType.CLOCK_OUT.name
        } else {
            timeTrackingService.clockIn(userId, TimeEntrySource.TERMINAL, terminalId = terminalId)
            TimeEntryType.CLOCK_IN.name
        }
    }

    private fun getRemainingVacationDays(userId: UUID): Float {
        val year = LocalDate.now(ZoneOffset.UTC).year
        return try {
            vacationService.getBalance(userId, year).remainingDays.toFloat()
        } catch (_: Exception) {
            0f
        }
    }
}
