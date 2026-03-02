package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.EmployeeInfo
import com.zeiterfassung.model.dto.TerminalScanRequest
import com.zeiterfassung.model.dto.TerminalScanResponse
import com.zeiterfassung.service.TerminalService
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
class TerminalControllerTest {
    @Mock
    private lateinit var terminalService: TerminalService

    private lateinit var controller: TerminalController

    @BeforeEach
    fun setUp() {
        controller = TerminalController(terminalService)
    }

    @Test
    fun `scan should return scan response on success`() {
        val scanResponse =
            TerminalScanResponse(
                employee =
                    EmployeeInfo(
                        id = UUID.randomUUID(),
                        firstName = "Max",
                        lastName = "Mustermann",
                        photoUrl = null,
                    ),
                entryType = "CLOCK_IN",
                timestamp = Instant.now(),
                todayWorkMinutes = 0,
                todayBreakMinutes = 0,
                overtimeMinutes = 120,
                remainingVacationDays = 15.0f,
            )
        `when`(terminalService.scan("RFID123", "TERMINAL-01")).thenReturn(scanResponse)

        val request = TerminalScanRequest(rfidTagId = "RFID123", terminalId = "TERMINAL-01")
        val response = controller.scan(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.entryType).isEqualTo("CLOCK_IN")
        assertThat(response.body!!.employee.firstName).isEqualTo("Max")
    }

    @Test
    fun `scan should handle clock out response`() {
        val scanResponse =
            TerminalScanResponse(
                employee =
                    EmployeeInfo(
                        id = UUID.randomUUID(),
                        firstName = "Max",
                        lastName = "Mustermann",
                        photoUrl = "https://photo.url/max.jpg",
                    ),
                entryType = "CLOCK_OUT",
                timestamp = Instant.now(),
                todayWorkMinutes = 480,
                todayBreakMinutes = 30,
                overtimeMinutes = 0,
                remainingVacationDays = 10.5f,
            )
        `when`(terminalService.scan("RFID456", "TERMINAL-02")).thenReturn(scanResponse)

        val request = TerminalScanRequest(rfidTagId = "RFID456", terminalId = "TERMINAL-02")
        val response = controller.scan(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.entryType).isEqualTo("CLOCK_OUT")
        assertThat(response.body!!.todayWorkMinutes).isEqualTo(480)
    }

    @Test
    fun `heartbeat should return ok status`() {
        val response = controller.heartbeat()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["status"]).isEqualTo("ok")
    }
}
