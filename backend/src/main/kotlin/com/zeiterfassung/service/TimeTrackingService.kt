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
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)
        if (lastEntry != null && lastEntry.entryType == TimeEntryType.CLOCK_IN) {
            throw ConflictException("Already clocked in. Please clock out first.")
        }
        if (lastEntry != null && lastEntry.entryType == TimeEntryType.BREAK_START) {
            throw ConflictException("Currently on break. Please end break first.")
        }

        val entry =
            TimeEntryEntity(
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
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)
        if (lastEntry == null || lastEntry.entryType == TimeEntryType.CLOCK_OUT) {
            throw ConflictException("Not clocked in. Please clock in first.")
        }
        if (lastEntry.entryType == TimeEntryType.BREAK_START) {
            throw ConflictException("Currently on break. Please end break before clocking out.")
        }

        val entry =
            TimeEntryEntity(
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
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)
        if (lastEntry == null || lastEntry.entryType != TimeEntryType.CLOCK_IN) {
            throw ConflictException("Must be clocked in to start a break.")
        }

        val entry =
            TimeEntryEntity(
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
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)
        if (lastEntry == null || lastEntry.entryType != TimeEntryType.BREAK_START) {
            throw ConflictException("Not on break.")
        }

        val entry =
            TimeEntryEntity(
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
        userRepository
            .findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val lastEntry = timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)
        val today = LocalDate.now(ZoneOffset.UTC)
        val todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant()
        val todayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val todayEntries = timeEntryRepository.findByUserIdAndDateRange(userId, todayStart, todayEnd)

        val calc = calculateWorkTime(todayEntries, Instant.now())
        val workMin = calc.effectiveWorkMinutes
        val breakMin = calc.effectiveBreakMinutes

        val status =
            when (lastEntry?.entryType) {
                TimeEntryType.CLOCK_IN -> TrackingStatus.CLOCKED_IN
                TimeEntryType.BREAK_START -> TrackingStatus.ON_BREAK
                else -> TrackingStatus.CLOCKED_OUT
            }

        val clockedInSince =
            if (status == TrackingStatus.CLOCKED_IN || status == TrackingStatus.ON_BREAK) {
                todayEntries.lastOrNull { it.entryType == TimeEntryType.CLOCK_IN }?.timestamp
            } else {
                null
            }

        val breakStartedAt = if (status == TrackingStatus.ON_BREAK) lastEntry?.timestamp else null

        val elapsedWork =
            if (status == TrackingStatus.CLOCKED_IN && clockedInSince != null) {
                ChronoUnit.MINUTES.between(clockedInSince, Instant.now())
            } else {
                0L
            }

        val elapsedBreak =
            if (status == TrackingStatus.ON_BREAK && breakStartedAt != null) {
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
    fun recalculateDailySummary(
        userId: UUID,
        date: LocalDate,
    ): DailySummaryResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val entries = timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay)

        val calc = calculateWorkTime(entries, null)

        val config = employeeConfigRepository.findByUserId(userId)
        val targetMinutes = config?.dailyWorkHours?.multiply(BigDecimal(60))?.toInt() ?: 480
        val overtimeMin = maxOf(0, calc.effectiveWorkMinutes - targetMinutes)

        val compliance = complianceService.checkCompliance(calc.effectiveWorkMinutes, calc.effectiveQualifyingBreakMinutes)

        val summary =
            dailySummaryRepository.findByUserIdAndDate(userId, date)
                ?: DailySummaryEntity(user = user, date = date)

        summary.totalWorkMinutes = calc.effectiveWorkMinutes
        summary.totalBreakMinutes = calc.effectiveBreakMinutes
        summary.overtimeMinutes = overtimeMin
        summary.isCompliant = compliance.isCompliant
        summary.complianceNotes = if (compliance.notes.isEmpty()) null else compliance.notes.joinToString("; ")

        val saved = dailySummaryRepository.save(summary)
        return saved.toResponse()
    }

    fun getDailySummary(
        userId: UUID,
        date: LocalDate,
    ): DailySummaryResponse {
        val summary =
            dailySummaryRepository.findByUserIdAndDate(userId, date)
                ?: return recalculateDailySummary(userId, date)
        return summary.toResponse()
    }

    fun getTimeSheet(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): TimeSheetResponse {
        userRepository
            .findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val summaries = dailySummaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, endDate)
        val startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val entries = timeEntryRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, startInstant, endInstant)

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

    fun getEntriesForUser(
        userId: UUID,
        start: Instant,
        end: Instant,
    ): List<TimeEntryResponse> {
        userRepository
            .findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }
        return timeEntryRepository
            .findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, start, end)
            .map { it.toResponse() }
    }

    /**
     * DSGVO: Managers may only access time data of their direct subordinates.
     * Admins (hasAuthority admin.users.manage) bypass this check at the controller level.
     */
    fun getTeamMemberEntries(
        managerId: UUID,
        userId: UUID,
        start: Instant,
        end: Instant,
    ): List<TimeEntryResponse> {
        val manager =
            userRepository
                .findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val isAdmin = manager.roles.flatMap { it.permissions }.any { it.name == "admin.users.manage" }
        if (!isAdmin && manager.subordinates.none { it.id == userId }) {
            // Check if user is a substitute for a manager who has this subordinate
            val isSubstitute =
                userRepository.findBySubstituteId(managerId).any { m ->
                    m.subordinates.any { it.id == userId }
                }
            if (!isSubstitute) {
                throw com.zeiterfassung.exception.ForbiddenException("Access denied: user $userId is not your subordinate")
            }
        }
        return getEntriesForUser(userId, start, end)
    }

    @Transactional
    fun addManualEntry(
        managerId: UUID,
        userId: UUID,
        request: ManualTimeEntryRequest,
    ): TimeEntryResponse {
        val manager =
            userRepository
                .findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val entry =
            TimeEntryEntity(
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
        val manager =
            userRepository
                .findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val entry =
            timeEntryRepository
                .findById(entryId)
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
        userRepository
            .findById(managerId)
            .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val entry =
            timeEntryRepository
                .findById(entryId)
                .orElseThrow { ResourceNotFoundException("TimeEntry not found: $entryId") }

        val oldResponse = entry.toResponse()
        val date = entry.timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        val userId = entry.user.id
        timeEntryRepository.delete(entry)
        recalculateDailySummary(userId, date)
        auditService.logDataChange(managerId, "TIME_ENTRY_DELETED", "TimeEntry", entryId, oldResponse, null)
    }

    @Transactional(readOnly = true)
    fun getTeamCurrentStatus(managerId: UUID): Map<UUID, TrackingStatusResponse> {
        val manager =
            userRepository
                .findById(managerId)
                .orElseThrow { ResourceNotFoundException("Manager not found: $managerId") }
        val allSubordinateIds = mutableSetOf<UUID>()
        manager.subordinates.forEach { allSubordinateIds.add(it.id) }
        // Include subordinates from managers who designated this user as substitute
        userRepository.findBySubstituteId(managerId).forEach { m ->
            m.subordinates.forEach { allSubordinateIds.add(it.id) }
        }
        return allSubordinateIds.associateWith { getCurrentStatus(it) }
    }

    /**
     * Breaks shorter than this threshold don't count toward ArbZG mandatory break requirements,
     * but still reduce work time (they are neither work nor qualifying break).
     */
    private val minQualifyingBreak = ArbZGComplianceService.MIN_QUALIFYING_BREAK_MINUTES

    /**
     * Breakdown of a day's work and break time as derived from raw time entries.
     *
     * @property rawWorkMinutes              Actual work periods (excluding ALL break time).
     * @property qualifyingBreakMinutes      Sum of breaks >= [minQualifyingBreak] min — count toward ArbZG.
     * @property shortBreakMinutes           Sum of breaks < [minQualifyingBreak] min — reduce work time but
     *                                       do NOT satisfy mandatory break requirements.
     * @property autoDeductedMinutes         Minutes auto-deducted because mandatory breaks were not taken.
     * @property effectiveWorkMinutes        Final work time (rawWork – autoDeducted).
     * @property effectiveBreakMinutes       Total time not counted as work (all breaks + auto-deducted).
     * @property effectiveQualifyingBreakMinutes Qualifying breaks + auto-deducted, used for compliance check.
     */
    private data class WorkTimeCalculation(
        val rawWorkMinutes: Int,
        val qualifyingBreakMinutes: Int,
        val shortBreakMinutes: Int,
        val autoDeductedMinutes: Int,
    ) {
        val effectiveWorkMinutes: Int get() = rawWorkMinutes - autoDeductedMinutes
        val effectiveBreakMinutes: Int get() = qualifyingBreakMinutes + shortBreakMinutes + autoDeductedMinutes

        /**
         * Qualifying breaks actually taken plus auto-deducted minutes.
         * Auto-deducted time is treated as if it were a qualifying break for ArbZG
         * compliance purposes: the system enforces the mandatory rest by subtracting it
         * from work time, so it counts toward the required break total.
         */
        val effectiveQualifyingBreakMinutes: Int get() = qualifyingBreakMinutes + autoDeductedMinutes
    }

    /**
     * Calculates work and break minutes from a list of time entries ordered ascending by timestamp.
     *
     * Break classification (§4 ArbZG):
     * - Breaks >= [minQualifyingBreak] min count toward the mandatory rest requirement.
     * - Breaks < [minQualifyingBreak] min reduce logged time but do NOT satisfy the requirement.
     *
     * If the employee did not take sufficient qualifying breaks, the missing amount is
     * automatically deducted from raw work time. The deduction is capped so that
     * effective work never falls below the ArbZG threshold that triggered the requirement
     * (e.g. 6:25h worked with no breaks → 6:00h effective work, not 5:55h).
     *
     * @param openEndTime If not null, use this as the end time for open clock-in periods (live status).
     */
    private fun calculateWorkTime(
        entries: List<TimeEntryEntity>,
        openEndTime: Instant?,
    ): WorkTimeCalculation {
        var rawWorkMinutes = 0L
        var qualifyingBreakMinutes = 0L
        var shortBreakMinutes = 0L
        var clockInTime: Instant? = null
        var breakStartTime: Instant? = null

        for (entry in entries) {
            when (entry.entryType) {
                TimeEntryType.CLOCK_IN -> clockInTime = entry.timestamp
                TimeEntryType.CLOCK_OUT -> {
                    if (clockInTime != null) {
                        rawWorkMinutes += ChronoUnit.MINUTES.between(clockInTime, entry.timestamp)
                        clockInTime = null
                    }
                }
                TimeEntryType.BREAK_START -> breakStartTime = entry.timestamp
                TimeEntryType.BREAK_END -> {
                    if (breakStartTime != null) {
                        val breakLen = ChronoUnit.MINUTES.between(breakStartTime, entry.timestamp)
                        if (breakLen >= minQualifyingBreak) {
                            qualifyingBreakMinutes += breakLen
                        } else {
                            shortBreakMinutes += breakLen
                        }
                        breakStartTime = null
                    }
                }
            }
        }

        // Handle open clock-in for live status. If currently on break, work only counts
        // up to when the break started (the in-progress break is not yet classified).
        if (clockInTime != null && openEndTime != null) {
            val workEnd = breakStartTime ?: openEndTime
            rawWorkMinutes += ChronoUnit.MINUTES.between(clockInTime, workEnd)
        }

        val autoDeducted =
            complianceService.computeAutoDeduction(rawWorkMinutes.toInt(), qualifyingBreakMinutes.toInt())

        return WorkTimeCalculation(
            rawWorkMinutes = rawWorkMinutes.toInt(),
            qualifyingBreakMinutes = qualifyingBreakMinutes.toInt(),
            shortBreakMinutes = shortBreakMinutes.toInt(),
            autoDeductedMinutes = autoDeducted,
        )
    }

    private fun TimeEntryEntity.toResponse() =
        TimeEntryResponse(
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

    private fun DailySummaryEntity.toResponse() =
        DailySummaryResponse(
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
