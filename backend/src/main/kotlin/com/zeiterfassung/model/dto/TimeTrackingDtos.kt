package com.zeiterfassung.model.dto

import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ClockInRequest(
    val notes: String? = null,
    val source: TimeEntrySource = TimeEntrySource.WEB,
    val terminalId: String? = null,
)

data class ClockOutRequest(
    val notes: String? = null,
    val source: TimeEntrySource = TimeEntrySource.WEB,
    val terminalId: String? = null,
)

data class BreakStartRequest(
    val notes: String? = null,
    val source: TimeEntrySource = TimeEntrySource.WEB,
)

data class BreakEndRequest(
    val notes: String? = null,
    val source: TimeEntrySource = TimeEntrySource.WEB,
)

data class ManualTimeEntryRequest(
    @field:NotBlank val reason: String,
    val entryType: TimeEntryType,
    val timestamp: Instant,
    val notes: String? = null,
    val source: TimeEntrySource = TimeEntrySource.WEB,
)

data class EditTimeEntryRequest(
    @field:NotBlank val reason: String,
    val timestamp: Instant? = null,
    val notes: String? = null,
)

data class TimeEntryResponse(
    val id: UUID,
    val userId: UUID,
    val entryType: TimeEntryType,
    val timestamp: Instant,
    val source: TimeEntrySource,
    val terminalId: String?,
    val notes: String?,
    val isModified: Boolean,
    val modifiedById: UUID?,
    val createdAt: Instant,
)

data class DailySummaryResponse(
    val id: UUID,
    val userId: UUID,
    val date: LocalDate,
    val totalWorkMinutes: Int,
    val totalBreakMinutes: Int,
    val overtimeMinutes: Int,
    val isCompliant: Boolean,
    val complianceNotes: String?,
)

data class TimeSheetResponse(
    val userId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dailySummaries: List<DailySummaryResponse>,
    val totalWorkMinutes: Int,
    val totalBreakMinutes: Int,
    val totalOvertimeMinutes: Int,
    val entries: List<TimeEntryResponse>,
)

enum class TrackingStatus { CLOCKED_OUT, CLOCKED_IN, ON_BREAK }

data class TrackingStatusResponse(
    val status: TrackingStatus,
    val clockedInSince: Instant?,
    val breakStartedAt: Instant?,
    val elapsedWorkMinutes: Long,
    val elapsedBreakMinutes: Long,
    val todayWorkMinutes: Int,
    val todayBreakMinutes: Int,
)

data class EmployeeConfigRequest(
    val weeklyWorkHours: BigDecimal? = null,
    val dailyWorkHours: BigDecimal? = null,
    val workDays: List<Int>? = null,
    val vacationDaysPerYear: Int? = null,
    val vacationCarryOverMax: Int? = null,
    val contractStartDate: LocalDate? = null,
    val contractEndDate: LocalDate? = null,
    val isHomeOfficeEligible: Boolean? = null,
)

data class EmployeeConfigResponse(
    val id: UUID,
    val userId: UUID,
    val weeklyWorkHours: BigDecimal,
    val dailyWorkHours: BigDecimal,
    val workDays: List<Int>,
    val vacationDaysPerYear: Int,
    val vacationCarryOverMax: Int,
    val contractStartDate: LocalDate?,
    val contractEndDate: LocalDate?,
    val isHomeOfficeEligible: Boolean,
)

data class TerminalScanRequest(
    val rfidTagId: String,
    val terminalId: String,
)

data class TerminalScanResponse(
    val employee: EmployeeInfo,
    val entryType: String,
    val timestamp: Instant,
    val todayWorkMinutes: Int,
    val todayBreakMinutes: Int,
    val overtimeMinutes: Int,
    val remainingVacationDays: Float,
)

data class EmployeeInfo(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
)
