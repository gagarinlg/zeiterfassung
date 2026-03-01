package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.TrackingStatus
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
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TimeTrackingServiceEdgeCasesTest {
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
    fun `clockIn throws ConflictException when on break`() {
        val breakEntry = makeEntry(TimeEntryType.BREAK_START)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(breakEntry)

        val ex =
            assertThrows<ConflictException> {
                service.clockIn(userId, TimeEntrySource.WEB)
            }
        assertThat(ex.message).contains("break")
    }

    @Test
    fun `clockOut throws ConflictException when on break`() {
        val breakEntry = makeEntry(TimeEntryType.BREAK_START)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(breakEntry)

        val ex =
            assertThrows<ConflictException> {
                service.clockOut(userId, TimeEntrySource.WEB)
            }
        assertThat(ex.message).contains("break")
    }

    @Test
    fun `startBreak throws ConflictException when not clocked in - clocked out`() {
        val clockOutEntry = makeEntry(TimeEntryType.CLOCK_OUT)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(clockOutEntry)

        assertThrows<ConflictException> {
            service.startBreak(userId, TimeEntrySource.WEB)
        }
    }

    @Test
    fun `startBreak throws ConflictException when already on break`() {
        val breakEntry = makeEntry(TimeEntryType.BREAK_START)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(breakEntry)

        assertThrows<ConflictException> {
            service.startBreak(userId, TimeEntrySource.WEB)
        }
    }

    @Test
    fun `endBreak throws ConflictException when clocked in - not on break`() {
        val clockInEntry = makeEntry(TimeEntryType.CLOCK_IN)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(clockInEntry)

        assertThrows<ConflictException> {
            service.endBreak(userId, TimeEntrySource.WEB)
        }
    }

    @Test
    fun `endBreak throws ConflictException when clocked out`() {
        val clockOutEntry = makeEntry(TimeEntryType.CLOCK_OUT)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(clockOutEntry)

        assertThrows<ConflictException> {
            service.endBreak(userId, TimeEntrySource.WEB)
        }
    }

    @Test
    fun `clockIn with notes saves notes correctly`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(null)
        `when`(
            timeEntryRepository.save(any(TimeEntryEntity::class.java) ?: makeEntry(TimeEntryType.CLOCK_IN)),
        ).thenAnswer { it.arguments[0] as TimeEntryEntity }

        val result = service.clockIn(userId, TimeEntrySource.WEB, notes = "Working from home")
        assertThat(result.notes).isEqualTo("Working from home")
    }

    @Test
    fun `clockIn with terminal source and terminalId`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(null)
        `when`(
            timeEntryRepository.save(any(TimeEntryEntity::class.java) ?: makeEntry(TimeEntryType.CLOCK_IN)),
        ).thenAnswer { it.arguments[0] as TimeEntryEntity }

        val result = service.clockIn(userId, TimeEntrySource.TERMINAL, terminalId = "terminal-01")
        assertThat(result.source).isEqualTo(TimeEntrySource.TERMINAL)
        assertThat(result.terminalId).isEqualTo("terminal-01")
    }

    @Test
    fun `deleteTimeEntry throws when entry not found`() {
        val managerId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        val manager =
            UserEntity(
                id = managerId,
                email = "manager@test.com",
                passwordHash = "hash",
                firstName = "Manager",
                lastName = "User",
            )
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.deleteTimeEntry(managerId, entryId, "Correction")
        }
    }

    @Test
    fun `deleteTimeEntry throws when manager not found`() {
        val managerId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        `when`(userRepository.findById(managerId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.deleteTimeEntry(managerId, entryId, "Correction")
        }
    }

    @Test
    fun `getTimeSheet throws when user not found`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getTimeSheet(userId, LocalDate.now().minusDays(7), LocalDate.now())
        }
    }

    @Test
    fun `getCurrentStatus returns CLOCKED_OUT when no entries exist`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findTopByUserIdOrderByTimestampDesc(userId)).thenReturn(null)
        `when`(
            timeEntryRepository.findByUserIdAndDateRange(
                any(UUID::class.java) ?: userId,
                any(Instant::class.java) ?: Instant.now(),
                any(Instant::class.java) ?: Instant.now(),
            ),
        ).thenReturn(emptyList())

        val result = service.getCurrentStatus(userId)
        assertThat(result.status).isEqualTo(TrackingStatus.CLOCKED_OUT)
        assertThat(result.clockedInSince).isNull()
        assertThat(result.breakStartedAt).isNull()
        assertThat(result.todayWorkMinutes).isEqualTo(0)
    }

    private fun makeEntry(type: TimeEntryType) =
        TimeEntryEntity(
            user = user,
            entryType = type,
            timestamp = Instant.now(),
        )
}
