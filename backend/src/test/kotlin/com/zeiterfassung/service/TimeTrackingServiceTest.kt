package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.model.entity.DailySummaryEntity
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
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TimeTrackingServiceTest {
    @Mock
    private lateinit var timeEntryRepository: TimeEntryRepository

    @Mock
    private lateinit var dailySummaryRepository: DailySummaryRepository

    @Mock
    private lateinit var employeeConfigRepository: EmployeeConfigRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var auditService: AuditService

    private val complianceService = ArbZGComplianceService()
    private val objectMapper = ObjectMapper()
    private lateinit var service: TimeTrackingService

    private lateinit var user: UserEntity
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            TimeTrackingService(
                timeEntryRepository,
                dailySummaryRepository,
                employeeConfigRepository,
                userRepository,
                complianceService,
                auditService,
                objectMapper,
            )
        user =
            UserEntity(
                id = userId,
                email = "test@test.com",
                passwordHash = "hash",
                firstName = "Test",
                lastName = "User",
            )
    }

    @Test
    fun `clockIn should succeed when not clocked in`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(null)
        `when`(
            timeEntryRepository.save(any(TimeEntryEntity::class.java) ?: makeClockIn()),
        ).thenAnswer { it.arguments[0] as TimeEntryEntity }

        val result = service.clockIn(userId, TimeEntrySource.WEB)
        assertThat(result.entryType).isEqualTo(TimeEntryType.CLOCK_IN)
    }

    @Test
    fun `clockIn should throw ConflictException when already clocked in`() {
        val clockInEntry = makeClockIn()
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(clockInEntry)

        assertThrows<ConflictException> {
            service.clockIn(userId, TimeEntrySource.WEB)
        }
    }

    @Test
    fun `clockOut should succeed when clocked in`() {
        val clockInEntry =
            TimeEntryEntity(
                user = user,
                entryType = TimeEntryType.CLOCK_IN,
                timestamp = Instant.now().minusSeconds(3600),
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(clockInEntry)
        `when`(
            timeEntryRepository.save(any(TimeEntryEntity::class.java) ?: clockInEntry),
        ).thenAnswer { it.arguments[0] as TimeEntryEntity }
        `when`(
            timeEntryRepository.findByUserIdAndDateRange(
                any(UUID::class.java) ?: userId,
                any(Instant::class.java) ?: Instant.now(),
                any(Instant::class.java) ?: Instant.now(),
            ),
        ).thenReturn(listOf(clockInEntry))
        `when`(
            dailySummaryRepository.findByUserIdAndDate(
                any(UUID::class.java) ?: userId,
                any(LocalDate::class.java) ?: LocalDate.now(),
            ),
        ).thenReturn(null)
        `when`(
            dailySummaryRepository.save(
                any(DailySummaryEntity::class.java) ?: DailySummaryEntity(user = user, date = LocalDate.now()),
            ),
        ).thenAnswer { it.arguments[0] }
        `when`(employeeConfigRepository.findByUserId(any(UUID::class.java) ?: userId)).thenReturn(null)

        val result = service.clockOut(userId, TimeEntrySource.WEB)
        assertThat(result.entryType).isEqualTo(TimeEntryType.CLOCK_OUT)
    }

    @Test
    fun `clockOut should throw ConflictException when not clocked in`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(null)

        assertThrows<ConflictException> {
            service.clockOut(userId, TimeEntrySource.WEB)
        }
    }

    @Test
    fun `startBreak should succeed when clocked in`() {
        val clockInEntry = makeClockIn()
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(clockInEntry)
        `when`(
            timeEntryRepository.save(any(TimeEntryEntity::class.java) ?: clockInEntry),
        ).thenAnswer { it.arguments[0] as TimeEntryEntity }

        val result = service.startBreak(userId, TimeEntrySource.WEB)
        assertThat(result.entryType).isEqualTo(TimeEntryType.BREAK_START)
    }

    @Test
    fun `endBreak should succeed when on break`() {
        val breakEntry =
            TimeEntryEntity(
                user = user,
                entryType = TimeEntryType.BREAK_START,
                timestamp = Instant.now().minusSeconds(1800),
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(breakEntry)
        `when`(
            timeEntryRepository.save(any(TimeEntryEntity::class.java) ?: breakEntry),
        ).thenAnswer { it.arguments[0] as TimeEntryEntity }

        val result = service.endBreak(userId, TimeEntrySource.WEB)
        assertThat(result.entryType).isEqualTo(TimeEntryType.BREAK_END)
    }

    @Test
    fun `getCurrentStatus should count gap between clock out and clock in as qualifying break`() {
        val now = Instant.now()
        val entries =
            listOf(
                TimeEntryEntity(user = user, entryType = TimeEntryType.CLOCK_IN, timestamp = now.minus(5, ChronoUnit.HOURS)),
                TimeEntryEntity(user = user, entryType = TimeEntryType.CLOCK_OUT, timestamp = now.minus(4, ChronoUnit.HOURS)),
                // 30-minute gap (qualifying break)
                TimeEntryEntity(
                    user = user,
                    entryType = TimeEntryType.CLOCK_IN,
                    timestamp = now.minus(3, ChronoUnit.HOURS).minus(30, ChronoUnit.MINUTES),
                ),
                TimeEntryEntity(user = user, entryType = TimeEntryType.CLOCK_OUT, timestamp = now.minus(1, ChronoUnit.HOURS)),
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(entries.last())
        `when`(
            timeEntryRepository.findByUserIdAndDateRange(
                any(UUID::class.java) ?: userId,
                any(Instant::class.java) ?: now,
                any(Instant::class.java) ?: now,
            ),
        ).thenReturn(entries)

        val result = service.getCurrentStatus(userId)

        // Total work = 60 + 150 = 210 min, gap = 30 min (qualifying break)
        assertThat(result.todayBreakMinutes).isGreaterThanOrEqualTo(30)
    }

    @Test
    fun `getCurrentStatus should count short gap between sessions as short break`() {
        val now = Instant.now()
        val entries =
            listOf(
                TimeEntryEntity(user = user, entryType = TimeEntryType.CLOCK_IN, timestamp = now.minus(3, ChronoUnit.HOURS)),
                TimeEntryEntity(user = user, entryType = TimeEntryType.CLOCK_OUT, timestamp = now.minus(2, ChronoUnit.HOURS)),
                // 10-minute gap (short break, < 15 min)
                TimeEntryEntity(
                    user = user,
                    entryType = TimeEntryType.CLOCK_IN,
                    timestamp = now.minus(2, ChronoUnit.HOURS).plus(10, ChronoUnit.MINUTES),
                ),
                TimeEntryEntity(user = user, entryType = TimeEntryType.CLOCK_OUT, timestamp = now.minus(1, ChronoUnit.HOURS)),
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(entries.last())
        `when`(
            timeEntryRepository.findByUserIdAndDateRange(
                any(UUID::class.java) ?: userId,
                any(Instant::class.java) ?: now,
                any(Instant::class.java) ?: now,
            ),
        ).thenReturn(entries)

        val result = service.getCurrentStatus(userId)

        // Total break includes the 10-minute short break gap
        assertThat(result.todayBreakMinutes).isGreaterThanOrEqualTo(10)
    }

    private fun makeClockIn() =
        TimeEntryEntity(
            user = user,
            entryType = TimeEntryType.CLOCK_IN,
            timestamp = Instant.now(),
        )
}
