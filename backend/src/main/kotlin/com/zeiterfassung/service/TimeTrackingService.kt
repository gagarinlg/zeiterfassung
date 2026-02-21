package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.DailySummaryResponse
import com.zeiterfassung.model.dto.EditTimeEntryRequest
import com.zeiterfassung.model.dto.ManualTimeEntryRequest
import com.zeiterfassung.model.dto.TimeEntryResponse
import com.zeiterfassung.model.dto.TimeSheetResponse
import com.zeiterfassung.model.dto.TrackingStatus
import com.zeiterfassung.model.dto.TrackingStatusResponse
import com.zeiterfassung.model.entity.DailySummaryEntity
import com.zeiterfassung.model.entity.TimeEntryEntity
import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
import com.zeiterfassung.repository.DailySummaryRepository
import com.zeiterfassung.repository.EmployeeConfigRepository
import com.zeiterfassung.repository.TimeEntryRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.util.ArbZGComplianceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class TimeTrackingService(
    private val timeEntryRepository: TimeEntryRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val employeeConfigRepository: EmployeeConfigRepository,
    private val userRepository: UserRepository,
    private val complianceService: ArbZGComplianceService,
    private val auditService: AuditService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun clockIn(
        userId: UUID,
        source: TimeEntrySource,
        notes: String? = null,
        terminalId: String? = null,
    ): TimeEntryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUser_IdOrderByTimestampDesc(userId)
        if (lastEntry != null && lastEntry.entryType == TimeEntryType.CLOCK_IN) {
            throw ConflictException("Already clocked in. Please clock out first.")
        }
        if (lastEntry != null && lastEntry.entryType == TimeEntryType.BREAK_START) {
            throw ConflictException("Currently on break. Please end break first.")
        }

        val entry = TimeEntryEntity(
            user = user,
            entryType = TimeEntryType.CLOCK_IN,
            timestamp = Instant.now(),
            source = source,
            terminalId = terminalId,
            notes = notes,
        )
        val saved = timeEntryRepository.save(entry)
        auditService.logDataChange(userId, "CLOCK_IN", "TimeEntry", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun clockOut(
        userId: UUID,
        source: TimeEntrySource,
        notes: String? = null,
        terminalId: String? = null,
    ): TimeEntryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUser_IdOrderByTimestampDesc(userId)
        if (lastEntry == null || lastEntry.entryType == TimeEntryType.CLOCK_OUT) {
            throw ConflictException("Not clocked in. Please clock in first.")
        }
        if (lastEntry.entryType == TimeEntryType.BREAK_START) {
            throw ConflictException("Currently on break. Please end break before clocking out.")
        }

        val entry = TimeEntryEntity(
            user = user,
            entryType = TimeEntryType.CLOCK_OUT,
            timestamp = Instant.now(),
            source = source,
            terminalId = terminalId,
            notes = notes,
        )
        val saved = timeEntryRepository.save(entry)
        recalculateDailySummary(userId, LocalDate.now(ZoneOffset.UTC))
        auditService.logDataChange(userId, "CLOCK_OUT", "TimeEntry", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun startBreak(
        userId: UUID,
        source: TimeEntrySource,
        notes: String? = null,
    ): TimeEntryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUser_IdOrderByTimestampDesc(userId)
        if (lastEntry == null || lastEntry.entryType != TimeEntryType.CLOCK_IN) {
            throw ConflictException("Must be clocked in to start a break.")
        }

        val entry = TimeEntryEntity(
            user = user,
            entryType = TimeEntryType.BREAK_START,
            timestamp = Instant.now(),
            source = source,
            notes = notes,
        )
        val saved = timeEntryRepository.save(entry)
        auditService.logDataChange(userId, "BREAK_START", "TimeEntry", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun endBreak(
        userId: UUID,
        source: TimeEntrySource,
        notes: String? = null,
    ): TimeEntryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUser_IdOrderByTimestampDesc(userId)
        if (lastEntry == null || lastEntry.entryType != TimeEntryType.BREAK_START) {
            throw ConflictException("Not on break.")
        }

        val entry = TimeEntryEntity(
            user = user,
            entryType = TimeEntryType.BREAK_END,
            timestamp = Instant.now(),
            source = source,
            notes = notes,
        )
        val saved = timeEntryRepository.save(entry)
        auditService.logDataChange(userId, "BREAK_END", "TimeEntry", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    fun getCurrentStatus(userId: UUID): TrackingStatusResponse {
        userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUser_IdOrderByTimestampDesc(userId)
        val today = LocalDate.now(ZoneOffset.UTC)
        val todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant()
        val todayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val todayEntries = timeEntryRepository.findByUser_IdAndDateRange(userId, todayStart, todayEnd)

        val (workMin, breakMin) = calculateMinutes(todayEntries, Instant.now())

        val status = when (lastEntry?.entryType) {
            TimeEntryType.CLOCK_IN -> TrackingStatus.CLOCKED_IN
            TimeEntryType.BREAK_START -> TrackingStatus.ON_BREAK
            else -> TrackingStatus.CLOCKED_OUT
        }

        val clockedInSince = if (status == TrackingStatus.CLOCKED_IN || status == TrackingStatus.ON_BREAK) {
            todayEntries.lastOrNull { it.entryType == TimeEntryType.CLOCK_IN }?.timestamp
        } else {
            null
        }

        val breakStartedAt = if (status == TrackingStatus.ON_BREAK) lastEntry?.timestamp else null

        val elapsedWork = if (status == TrackingStatus.CLOCKED_IN && clockedInSince != null) {
            ChronoUnit.MINUTES.between(clockedInSince, Instant.now())
        } else {
            0L
        }

        val elapsedBreak = if (status == TrackingStatus.ON_BREAK && breakStartedAt != null) {
            ChronoUnit.MINUTES.between(breakStartedAt, Instant.now())
        } else {
            0L
        }

        return TrackingStatusResponse(
            status = status,
            clockedInSince = clockedInSince,
            breakStartedAt = breakStartedAt,
            elapsedWorkMinutes = elapsedWork,
            elapsedBreakMinutes = elapsedBreak,
            todayWorkMinutes = workMin,
            todayBreakMinutes = breakMin,
        )
    }

    @Transactional
    fun recalculateDailySummary(userId: UUID, date: LocalDate): DailySummaryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val entries = timeEntryRepository.findByUser_IdAndDateRange(userId, startOfDay, endOfDay)

        val (workMin, breakMin) = calculateMinutes(entries, null)

        val config = employeeConfigRepository.findByUser_Id(userId)
        val targetMinutes = config?.dailyWorkHours?.multiply(BigDecimal(60))?.toInt() ?: 480
        val overtimeMin = maxOf(0, workMin - targetMinutes)

        val compliance = complianceService.checkCompliance(workMin, breakMin)

        val summary = dailySummaryRepository.findByUser_IdAndDate(userId, date)
            ?: DailySummaryEntity(user = user, date = date)

        summary.totalWorkMinutes = workMin
        summary.totalBreakMinutes = breakMin
        summary.overtimeMinutes = overtimeMin
        summary.isCompliant = compliance.isCompliant
        summary.complianceNotes = if (compliance.notes.isEmpty()) null else compliance.notes.joinToString("; ")

        val saved = dailySummaryRepository.save(summary)
        return saved.toResponse()
    }

    fun getDailySummary(userId: UUID, date: LocalDate): DailySummaryResponse {
        val summary = dailySummaryRepository.findByUser_IdAndDate(userId, date)
            ?: return recalculateDailySummary(userId, date)
        return summary.toResponse()
    }

    fun getTimeSheet(userId: UUID, startDate: LocalDate, endDate: LocalDate): TimeSheetResponse {
        userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val summaries = dailySummaryRepository.findByUser_IdAndDateBetweenOrderByDateAsc(userId, startDate, endDate)
        val startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val entries = timeEntryRepository.findByUser_IdAndTimestampBetweenOrderByTimestampAsc(userId, startInstant, endInstant)

        return TimeSheetResponse(
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            dailySummaries = summaries.map { it.toResponse() },
            totalWorkMinutes = summaries.sumOf { it.totalWorkMinutes },
            totalBreakMinutes = summaries.sumOf { it.totalBreakMinutes },
            totalOvertimeMinutes = summaries.sumOf { it.overtimeMinutes },
            entries = entries.map { it.toResponse() },
        )
    }

    fun getEntriesForUser(userId: UUID, start: Instant, end: Instant): List<TimeEntryResponse> {
        userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }
        return timeEntryRepository.findByUser_IdAndTimestampBetweenOrderByTimestampAsc(userId, start, end)
            .map { it.toResponse() }
    }

    @Transactional
    fun addManualEntry(
        managerId: UUID,
        userId: UUID,
        request: ManualTimeEntryRequest,
    ): TimeEntryResponse {
        val manager = userRepository.findById(managerId)
            .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val entry = TimeEntryEntity(
            user = user,
            entryType = request.entryType,
            timestamp = request.timestamp,
            source = request.source,
            notes = request.notes,
            isModified = true,
            modifiedBy = manager,
        )
        val saved = timeEntryRepository.save(entry)
        val date = request.timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        recalculateDailySummary(userId, date)
        auditService.logDataChange(managerId, "MANUAL_TIME_ENTRY", "TimeEntry", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun editTimeEntry(
        managerId: UUID,
        entryId: UUID,
        request: EditTimeEntryRequest,
    ): TimeEntryResponse {
        val manager = userRepository.findById(managerId)
            .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val entry = timeEntryRepository.findById(entryId)
            .orElseThrow { ResourceNotFoundException("TimeEntry not found: $entryId") }

        val oldResponse = entry.toResponse()

        request.timestamp?.let { entry.timestamp = it }
        request.notes?.let { entry.notes = it }
        entry.isModified = true
        entry.modifiedBy = manager

        val saved = timeEntryRepository.save(entry)
        val date = saved.timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        recalculateDailySummary(saved.user.id, date)
        auditService.logDataChange(managerId, "TIME_ENTRY_EDITED", "TimeEntry", entryId, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun deleteTimeEntry(
        managerId: UUID,
        entryId: UUID,
        reason: String,
    ) {
        userRepository.findById(managerId)
            .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val entry = timeEntryRepository.findById(entryId)
            .orElseThrow { ResourceNotFoundException("TimeEntry not found: $entryId") }

        val oldResponse = entry.toResponse()
        val date = entry.timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        val userId = entry.user.id
        timeEntryRepository.delete(entry)
        recalculateDailySummary(userId, date)
        auditService.logDataChange(managerId, "TIME_ENTRY_DELETED", "TimeEntry", entryId, oldResponse, null)
    }

    fun getTeamCurrentStatus(managerId: UUID): Map<UUID, TrackingStatusResponse> {
        val manager = userRepository.findById(managerId)
            .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        return manager.subordinates.associate { it.id to getCurrentStatus(it.id) }
    }

    /**
     * Calculates work and break minutes from a list of time entries ordered by timestamp ascending.
     * @param openEndTime if not null, use this as the end time for open (unclosed) clock-in periods (for live status).
     */
    private fun calculateMinutes(
        entries: List<TimeEntryEntity>,
        openEndTime: Instant?,
    ): Pair<Int, Int> {
        var workMinutes = 0L
        var breakMinutes = 0L
        var clockInTime: Instant? = null
        var breakStartTime: Instant? = null

        for (entry in entries) {
            when (entry.entryType) {
                TimeEntryType.CLOCK_IN -> clockInTime = entry.timestamp
                TimeEntryType.CLOCK_OUT -> {
                    if (clockInTime != null) {
                        workMinutes += ChronoUnit.MINUTES.between(clockInTime, entry.timestamp)
                        clockInTime = null
                    }
                }
                TimeEntryType.BREAK_START -> breakStartTime = entry.timestamp
                TimeEntryType.BREAK_END -> {
                    if (breakStartTime != null) {
                        breakMinutes += ChronoUnit.MINUTES.between(breakStartTime, entry.timestamp)
                        breakStartTime = null
                    }
                }
            }
        }

        // Handle open clock-in (no clock-out yet) for live status
        if (clockInTime != null && openEndTime != null) {
            workMinutes += ChronoUnit.MINUTES.between(clockInTime, openEndTime)
        }

        return Pair(workMinutes.toInt(), breakMinutes.toInt())
    }

    private fun TimeEntryEntity.toResponse() = TimeEntryResponse(
        id = id,
        userId = user.id,
        entryType = entryType,
        timestamp = timestamp,
        source = source,
        terminalId = terminalId,
        notes = notes,
        isModified = isModified,
        modifiedById = modifiedBy?.id,
        createdAt = createdAt,
    )

    private fun DailySummaryEntity.toResponse() = DailySummaryResponse(
        id = id,
        userId = user.id,
        date = date,
        totalWorkMinutes = totalWorkMinutes,
        totalBreakMinutes = totalBreakMinutes,
        overtimeMinutes = overtimeMinutes,
        isCompliant = isCompliant,
        complianceNotes = complianceNotes,
    )
}
