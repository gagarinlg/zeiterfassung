package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.EditTimeEntryRequest
import com.zeiterfassung.model.dto.ManualTimeEntryRequest
import com.zeiterfassung.model.entity.DailySummaryEntity
import com.zeiterfassung.model.entity.EmployeeConfigEntity
import com.zeiterfassung.model.entity.PermissionEntity
import com.zeiterfassung.model.entity.RoleEntity
import com.zeiterfassung.model.entity.TimeEntryEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
import com.zeiterfassung.repository.DailySummaryRepository
import com.zeiterfassung.repository.EmployeeConfigRepository
import com.zeiterfassung.repository.TimeEntryRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.util.ArbZGComplianceService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TimeTrackingServiceEdgeCasesTest2 {
    @Mock private lateinit var timeEntryRepository: TimeEntryRepository
    @Mock private lateinit var dailySummaryRepository: DailySummaryRepository
    @Mock private lateinit var employeeConfigRepository: EmployeeConfigRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var auditService: AuditService

    private val complianceService = ArbZGComplianceService()
    private val objectMapper = ObjectMapper()
    private lateinit var service: TimeTrackingService

    private lateinit var user: UserEntity
    private lateinit var manager: UserEntity
    private val userId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = TimeTrackingService(
            timeEntryRepository, dailySummaryRepository, employeeConfigRepository,
            userRepository, complianceService, auditService, objectMapper,
        )
        user = UserEntity(id = userId, email = "user@test.com", passwordHash = "h", firstName = "Test", lastName = "User")
        manager = UserEntity(id = managerId, email = "mgr@test.com", passwordHash = "h", firstName = "Manager", lastName = "One")
        manager.subordinates.add(user)
    }

    // ---- getDailySummary ----

    @Test
    fun `getDailySummary returns existing summary`() {
        val date = LocalDate.of(2025, 1, 15)
        val summary = DailySummaryEntity(user = user, date = date, totalWorkMinutes = 480, totalBreakMinutes = 30)
        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(summary)

        val result = service.getDailySummary(userId, date)
        assertThat(result.totalWorkMinutes).isEqualTo(480)
        assertThat(result.totalBreakMinutes).isEqualTo(30)
    }

    @Test
    fun `getDailySummary recalculates when no existing summary`() {
        val date = LocalDate.of(2025, 1, 15)
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(null)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(dailySummaryRepository.save(any())).thenAnswer { it.arguments[0] as DailySummaryEntity }

        val result = service.getDailySummary(userId, date)
        assertThat(result.totalWorkMinutes).isEqualTo(0)
    }

    // ---- recalculateDailySummary ----

    @Test
    fun `recalculateDailySummary with entries creates summary`() {
        val date = LocalDate.of(2025, 1, 15)
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val clockIn = makeEntry(TimeEntryType.CLOCK_IN, startOfDay.plusSeconds(3600 * 8))
        val clockOut = makeEntry(TimeEntryType.CLOCK_OUT, startOfDay.plusSeconds(3600 * 16))

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay))
            .thenReturn(listOf(clockIn, clockOut))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(null)
        `when`(dailySummaryRepository.save(any())).thenAnswer { it.arguments[0] as DailySummaryEntity }

        val result = service.recalculateDailySummary(userId, date)
        assertThat(result.totalWorkMinutes).isGreaterThan(0)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `recalculateDailySummary updates existing summary`() {
        val date = LocalDate.of(2025, 1, 15)
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val existing = DailySummaryEntity(user = user, date = date, totalWorkMinutes = 100)

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(existing)
        `when`(dailySummaryRepository.save(any())).thenAnswer { it.arguments[0] as DailySummaryEntity }

        val result = service.recalculateDailySummary(userId, date)
        assertThat(result.totalWorkMinutes).isEqualTo(0)
    }

    @Test
    fun `recalculateDailySummary with custom daily hours`() {
        val date = LocalDate.of(2025, 1, 15)
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val config = EmployeeConfigEntity(user = user, dailyWorkHours = BigDecimal("6.00"))

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(config)
        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(null)
        `when`(dailySummaryRepository.save(any())).thenAnswer { it.arguments[0] as DailySummaryEntity }

        val result = service.recalculateDailySummary(userId, date)
        assertThat(result.overtimeMinutes).isEqualTo(0)
    }

    @Test
    fun `recalculateDailySummary throws when user not found`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.recalculateDailySummary(userId, LocalDate.now())
        }
    }

    // ---- getTimeSheet ----

    @Test
    fun `getTimeSheet returns summaries and entries`() {
        val startDate = LocalDate.of(2025, 1, 13)
        val endDate = LocalDate.of(2025, 1, 17)
        val summary = DailySummaryEntity(user = user, date = startDate, totalWorkMinutes = 480, totalBreakMinutes = 30, overtimeMinutes = 0)
        val entry = makeEntry(TimeEntryType.CLOCK_IN, Instant.now())

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(dailySummaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, endDate))
            .thenReturn(listOf(summary))
        `when`(timeEntryRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(
            any(UUID::class.java) ?: userId,
            any(Instant::class.java) ?: Instant.now(),
            any(Instant::class.java) ?: Instant.now(),
        )).thenReturn(listOf(entry))

        val result = service.getTimeSheet(userId, startDate, endDate)
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.dailySummaries).hasSize(1)
        assertThat(result.entries).hasSize(1)
        assertThat(result.totalWorkMinutes).isEqualTo(480)
    }

    @Test
    fun `getTimeSheet empty when no data`() {
        val startDate = LocalDate.of(2025, 1, 13)
        val endDate = LocalDate.of(2025, 1, 17)

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(dailySummaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, endDate))
            .thenReturn(emptyList())
        `when`(timeEntryRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(
            any(UUID::class.java) ?: userId,
            any(Instant::class.java) ?: Instant.now(),
            any(Instant::class.java) ?: Instant.now(),
        )).thenReturn(emptyList())

        val result = service.getTimeSheet(userId, startDate, endDate)
        assertThat(result.dailySummaries).isEmpty()
        assertThat(result.totalWorkMinutes).isEqualTo(0)
    }

    // ---- addManualEntry ----

    @Test
    fun `addManualEntry creates entry and recalculates`() {
        val timestamp = Instant.now().minusSeconds(3600)
        val request = ManualTimeEntryRequest(
            reason = "Forgot to clock in",
            entryType = TimeEntryType.CLOCK_IN,
            timestamp = timestamp,
        )
        val date = timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.save(any())).thenAnswer { it.arguments[0] as TimeEntryEntity }
        `when`(timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(null)
        `when`(dailySummaryRepository.save(any())).thenAnswer { it.arguments[0] as DailySummaryEntity }

        val result = service.addManualEntry(managerId, userId, request)
        assertThat(result.entryType).isEqualTo(TimeEntryType.CLOCK_IN)
        assertThat(result.isModified).isTrue()
        assertThat(result.modifiedById).isEqualTo(managerId)
    }

    @Test
    fun `addManualEntry throws when manager not found`() {
        val request = ManualTimeEntryRequest(
            reason = "Test", entryType = TimeEntryType.CLOCK_IN, timestamp = Instant.now(),
        )
        `when`(userRepository.findById(managerId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.addManualEntry(managerId, userId, request) }
    }

    @Test
    fun `addManualEntry throws when user not found`() {
        val request = ManualTimeEntryRequest(
            reason = "Test", entryType = TimeEntryType.CLOCK_IN, timestamp = Instant.now(),
        )
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.addManualEntry(managerId, userId, request) }
    }

    // ---- editTimeEntry ----

    @Test
    fun `editTimeEntry updates timestamp and notes`() {
        val entryId = UUID.randomUUID()
        val newTimestamp = Instant.now().minusSeconds(7200)
        val entry = makeEntry(TimeEntryType.CLOCK_IN, Instant.now().minusSeconds(3600))
        val request = EditTimeEntryRequest(reason = "Correction", timestamp = newTimestamp, notes = "Updated note")
        val date = newTimestamp.atZone(ZoneOffset.UTC).toLocalDate()
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry))
        `when`(timeEntryRepository.save(any())).thenAnswer { it.arguments[0] as TimeEntryEntity }
        `when`(timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(null)
        `when`(dailySummaryRepository.save(any())).thenAnswer { it.arguments[0] as DailySummaryEntity }

        val result = service.editTimeEntry(managerId, entryId, request)
        assertThat(result.timestamp).isEqualTo(newTimestamp)
        assertThat(result.notes).isEqualTo("Updated note")
        assertThat(result.isModified).isTrue()
    }

    @Test
    fun `editTimeEntry throws when entry not found`() {
        val entryId = UUID.randomUUID()
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.editTimeEntry(managerId, entryId, EditTimeEntryRequest(reason = "Fix"))
        }
    }

    @Test
    fun `editTimeEntry throws when manager not found`() {
        `when`(userRepository.findById(managerId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.editTimeEntry(managerId, UUID.randomUUID(), EditTimeEntryRequest(reason = "Fix"))
        }
    }

    // ---- deleteTimeEntry ----

    @Test
    fun `deleteTimeEntry deletes and recalculates`() {
        val entryId = UUID.randomUUID()
        val timestamp = Instant.now().minusSeconds(3600)
        val entry = makeEntry(TimeEntryType.CLOCK_IN, timestamp)
        val date = timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry))
        `when`(timeEntryRepository.findByUserIdAndDateRange(userId, startOfDay, endOfDay)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(dailySummaryRepository.findByUserIdAndDate(userId, date)).thenReturn(null)
        `when`(dailySummaryRepository.save(any())).thenAnswer { it.arguments[0] as DailySummaryEntity }

        service.deleteTimeEntry(managerId, entryId, "Duplicate entry")

        verify(timeEntryRepository).delete(entry)
    }

    // ---- getTeamCurrentStatus ----

    @Test
    fun `getTeamCurrentStatus returns status for all subordinates`() {
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findBySubstituteId(managerId)).thenReturn(emptyList())
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(null)
        `when`(timeEntryRepository.findByUserIdAndDateRange(
            any(UUID::class.java) ?: userId,
            any(Instant::class.java) ?: Instant.now(),
            any(Instant::class.java) ?: Instant.now(),
        )).thenReturn(emptyList())

        val result = service.getTeamCurrentStatus(managerId)
        assertThat(result).containsKey(userId)
    }

    @Test
    fun `getTeamCurrentStatus includes substitute subordinates`() {
        val otherEmpId = UUID.randomUUID()
        val otherEmp = UserEntity(id = otherEmpId, email = "emp2@t.com", passwordHash = "h", firstName = "E", lastName = "T")
        val otherMgr = UserEntity(id = UUID.randomUUID(), email = "mgr2@t.com", passwordHash = "h", firstName = "M", lastName = "T")
        otherMgr.subordinates.add(otherEmp)

        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findBySubstituteId(managerId)).thenReturn(listOf(otherMgr))
        // For userId (direct subordinate)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(null)
        // For otherEmpId (substitute subordinate)
        `when`(userRepository.findById(otherEmpId)).thenReturn(Optional.of(otherEmp))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(otherEmpId)).thenReturn(null)
        `when`(timeEntryRepository.findByUserIdAndDateRange(
            any(UUID::class.java) ?: userId,
            any(Instant::class.java) ?: Instant.now(),
            any(Instant::class.java) ?: Instant.now(),
        )).thenReturn(emptyList())

        val result = service.getTeamCurrentStatus(managerId)
        assertThat(result).containsKey(userId)
        assertThat(result).containsKey(otherEmpId)
    }

    @Test
    fun `getTeamCurrentStatus throws when manager not found`() {
        `when`(userRepository.findById(managerId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getTeamCurrentStatus(managerId) }
    }

    // ---- getEntriesForUser ----

    @Test
    fun `getEntriesForUser returns entries`() {
        val start = Instant.now().minusSeconds(86400)
        val end = Instant.now()
        val entry = makeEntry(TimeEntryType.CLOCK_IN, Instant.now().minusSeconds(3600))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, start, end))
            .thenReturn(listOf(entry))

        val result = service.getEntriesForUser(userId, start, end)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `getEntriesForUser throws when user not found`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getEntriesForUser(userId, Instant.now().minusSeconds(3600), Instant.now())
        }
    }

    // ---- getTeamMemberEntries ----

    @Test
    fun `getTeamMemberEntries succeeds for direct manager`() {
        val start = Instant.now().minusSeconds(86400)
        val end = Instant.now()
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, start, end))
            .thenReturn(emptyList())

        val result = service.getTeamMemberEntries(managerId, userId, start, end)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getTeamMemberEntries succeeds for admin`() {
        val adminPerm = PermissionEntity(name = "admin.users.manage")
        val adminRole = RoleEntity(name = "SUPER_ADMIN", permissions = mutableSetOf(adminPerm))
        val admin = UserEntity(id = UUID.randomUUID(), email = "admin@t.com", passwordHash = "h", firstName = "A", lastName = "D")
        admin.roles.add(adminRole)

        val start = Instant.now().minusSeconds(86400)
        val end = Instant.now()
        `when`(userRepository.findById(admin.id)).thenReturn(Optional.of(admin))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, start, end))
            .thenReturn(emptyList())

        val result = service.getTeamMemberEntries(admin.id, userId, start, end)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getTeamMemberEntries succeeds for substitute`() {
        val subId = UUID.randomUUID()
        val sub = UserEntity(id = subId, email = "sub@t.com", passwordHash = "h", firstName = "S", lastName = "U")
        val start = Instant.now().minusSeconds(86400)
        val end = Instant.now()

        `when`(userRepository.findById(subId)).thenReturn(Optional.of(sub))
        `when`(userRepository.findBySubstituteId(subId)).thenReturn(listOf(manager))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, start, end))
            .thenReturn(emptyList())

        val result = service.getTeamMemberEntries(subId, userId, start, end)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getTeamMemberEntries throws ForbiddenException for unrelated user`() {
        val otherId = UUID.randomUUID()
        val other = UserEntity(id = otherId, email = "other@t.com", passwordHash = "h", firstName = "O", lastName = "U")
        val start = Instant.now().minusSeconds(86400)
        val end = Instant.now()

        `when`(userRepository.findById(otherId)).thenReturn(Optional.of(other))
        `when`(userRepository.findBySubstituteId(otherId)).thenReturn(emptyList())

        assertThrows<ForbiddenException> {
            service.getTeamMemberEntries(otherId, userId, start, end)
        }
    }

    // ---- helpers ----

    private fun makeEntry(type: TimeEntryType, timestamp: Instant) =
        TimeEntryEntity(user = user, entryType = type, timestamp = timestamp)
}
