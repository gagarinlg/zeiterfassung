package com.zeiterfassung.service

import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.DailySummaryResponse
import com.zeiterfassung.model.dto.TrackingStatus
import com.zeiterfassung.model.dto.TrackingStatusResponse
import com.zeiterfassung.model.dto.VacationBalanceResponse
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TerminalServiceTest {
    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var timeTrackingService: TimeTrackingService

    @Mock private lateinit var vacationService: VacationService

    private lateinit var service: TerminalService

    private lateinit var user: UserEntity
    private val userId = UUID.randomUUID()
    private val rfidTagId = "RFID-001"
    private val terminalId = "TERMINAL-01"

    @BeforeEach
    fun setUp() {
        service = TerminalService(userRepository, timeTrackingService, vacationService)
        user =
            UserEntity(
                id = userId,
                email = "employee@test.com",
                passwordHash = "hash",
                firstName = "Max",
                lastName = "Mustermann",
            )
    }

    @Test
    fun `scan should clock in when employee is clocked out`() {
        `when`(userRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.of(user))
        `when`(timeTrackingService.getCurrentStatus(userId)).thenReturn(clockedOutStatus())
        `when`(vacationService.getBalance(userId, LocalDate.now().year)).thenReturn(balanceResponse(10.0))
        `when`(timeTrackingService.getDailySummary(userId, LocalDate.now())).thenReturn(emptySummary())

        val result = service.scan(rfidTagId, terminalId)

        assertThat(result.entryType).isEqualTo(TimeEntryType.CLOCK_IN.name)
        assertThat(result.employee.firstName).isEqualTo("Max")
        assertThat(result.employee.lastName).isEqualTo("Mustermann")
        assertThat(result.remainingVacationDays).isEqualTo(10.0f)
        verify(timeTrackingService).clockIn(userId, TimeEntrySource.TERMINAL, terminalId = terminalId)
    }

    @Test
    fun `scan should clock out when employee is clocked in`() {
        `when`(userRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.of(user))
        `when`(timeTrackingService.getCurrentStatus(userId)).thenReturn(clockedInStatus())
        `when`(vacationService.getBalance(userId, LocalDate.now().year)).thenReturn(balanceResponse(5.5))
        `when`(timeTrackingService.getDailySummary(userId, LocalDate.now())).thenReturn(summaryWith(480, 30, 0))

        val result = service.scan(rfidTagId, terminalId)

        assertThat(result.entryType).isEqualTo(TimeEntryType.CLOCK_OUT.name)
        assertThat(result.todayWorkMinutes).isEqualTo(480)
        assertThat(result.todayBreakMinutes).isEqualTo(30)
        assertThat(result.remainingVacationDays).isEqualTo(5.5f)
        verify(timeTrackingService).clockOut(userId, TimeEntrySource.TERMINAL, terminalId = terminalId)
    }

    @Test
    fun `scan should clock out when employee is on break`() {
        `when`(userRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.of(user))
        `when`(timeTrackingService.getCurrentStatus(userId)).thenReturn(onBreakStatus())
        `when`(vacationService.getBalance(userId, LocalDate.now().year)).thenReturn(balanceResponse(0.0))
        `when`(timeTrackingService.getDailySummary(userId, LocalDate.now())).thenReturn(emptySummary())

        val result = service.scan(rfidTagId, terminalId)

        assertThat(result.entryType).isEqualTo(TimeEntryType.CLOCK_OUT.name)
        verify(timeTrackingService).clockOut(userId, TimeEntrySource.TERMINAL, terminalId = terminalId)
    }

    @Test
    fun `scan should throw ResourceNotFoundException for unknown RFID tag`() {
        `when`(userRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.scan(rfidTagId, terminalId)
        }
    }

    @Test
    fun `scan should include employee photo URL in response`() {
        user.photoUrl = "https://example.com/photo.jpg"
        `when`(userRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.of(user))
        `when`(timeTrackingService.getCurrentStatus(userId)).thenReturn(clockedOutStatus())
        `when`(vacationService.getBalance(userId, LocalDate.now().year)).thenReturn(balanceResponse(10.0))
        `when`(timeTrackingService.getDailySummary(userId, LocalDate.now())).thenReturn(emptySummary())

        val result = service.scan(rfidTagId, terminalId)

        assertThat(result.employee.photoUrl).isEqualTo("https://example.com/photo.jpg")
    }

    @Test
    fun `scan should return zero vacation days when vacation service fails`() {
        `when`(userRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.of(user))
        `when`(timeTrackingService.getCurrentStatus(userId)).thenReturn(clockedOutStatus())
        `when`(vacationService.getBalance(userId, LocalDate.now().year))
            .thenThrow(RuntimeException("Vacation service unavailable"))
        `when`(timeTrackingService.getDailySummary(userId, LocalDate.now())).thenReturn(emptySummary())

        val result = service.scan(rfidTagId, terminalId)

        assertThat(result.remainingVacationDays).isEqualTo(0f)
    }

    @Test
    fun `scan should include correct overtime in response`() {
        `when`(userRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.of(user))
        `when`(timeTrackingService.getCurrentStatus(userId)).thenReturn(clockedInStatus())
        `when`(vacationService.getBalance(userId, LocalDate.now().year)).thenReturn(balanceResponse(10.0))
        `when`(timeTrackingService.getDailySummary(userId, LocalDate.now())).thenReturn(summaryWith(600, 0, 120))

        val result = service.scan(rfidTagId, terminalId)

        assertThat(result.overtimeMinutes).isEqualTo(120)
    }

    // region helpers

    private fun clockedOutStatus() =
        TrackingStatusResponse(
            status = TrackingStatus.CLOCKED_OUT,
            clockedInSince = null,
            breakStartedAt = null,
            elapsedWorkMinutes = 0,
            elapsedBreakMinutes = 0,
            todayWorkMinutes = 0,
            todayBreakMinutes = 0,
        )

    private fun clockedInStatus() =
        TrackingStatusResponse(
            status = TrackingStatus.CLOCKED_IN,
            clockedInSince = Instant.now().minusSeconds(3600),
            breakStartedAt = null,
            elapsedWorkMinutes = 60,
            elapsedBreakMinutes = 0,
            todayWorkMinutes = 60,
            todayBreakMinutes = 0,
        )

    private fun onBreakStatus() =
        TrackingStatusResponse(
            status = TrackingStatus.ON_BREAK,
            clockedInSince = Instant.now().minusSeconds(3600),
            breakStartedAt = Instant.now().minusSeconds(900),
            elapsedWorkMinutes = 45,
            elapsedBreakMinutes = 15,
            todayWorkMinutes = 45,
            todayBreakMinutes = 15,
        )

    private fun emptySummary() = summaryWith(0, 0, 0)

    private fun summaryWith(
        workMinutes: Int,
        breakMinutes: Int,
        overtimeMinutes: Int,
    ) = DailySummaryResponse(
        id = UUID.randomUUID(),
        userId = userId,
        date = LocalDate.now(),
        totalWorkMinutes = workMinutes,
        totalBreakMinutes = breakMinutes,
        overtimeMinutes = overtimeMinutes,
        isCompliant = true,
        complianceNotes = null,
    )

    private fun balanceResponse(remaining: Double) =
        VacationBalanceResponse(
            id = UUID.randomUUID(),
            userId = userId,
            year = LocalDate.now().year,
            totalDays = BigDecimal("30"),
            usedDays = BigDecimal.ZERO,
            carriedOverDays = BigDecimal.ZERO,
            remainingDays = BigDecimal.valueOf(remaining),
            pendingDays = BigDecimal.ZERO,
        )

    // endregion
}
