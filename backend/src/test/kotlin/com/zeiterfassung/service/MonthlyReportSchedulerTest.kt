package com.zeiterfassung.service

import com.zeiterfassung.model.entity.DailySummaryEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.entity.VacationBalanceEntity
import com.zeiterfassung.repository.DailySummaryRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.repository.VacationBalanceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.context.MessageSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonthlyReportSchedulerTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var dailySummaryRepository: DailySummaryRepository

    @Mock
    private lateinit var vacationBalanceRepository: VacationBalanceRepository

    @Mock
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var messageSource: MessageSource

    private lateinit var scheduler: MonthlyReportScheduler

    @BeforeEach
    fun setUp() {
        scheduler =
            MonthlyReportScheduler(
                userRepository,
                dailySummaryRepository,
                vacationBalanceRepository,
                emailService,
                messageSource,
            )
        `when`(
            messageSource.getMessage(
                anyString(),
                any() ?: emptyArray<Any>(),
                any(Locale::class.java) ?: Locale.GERMAN,
            ),
        ).thenReturn("stub-message")
    }

    @Test
    fun `sendMonthlyReports sends personal reports to active users`() {
        val activeUser = createUser("active@test.com", isActive = true, isDeleted = false)
        `when`(userRepository.findAll()).thenReturn(listOf(activeUser))
        stubSummariesAndBalance(activeUser)

        scheduler.sendMonthlyReports()

        verify(emailService).sendAsync(
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
        )
    }

    @Test
    fun `sendMonthlyReports skips inactive users`() {
        val inactiveUser = createUser("inactive@test.com", isActive = false, isDeleted = false)
        `when`(userRepository.findAll()).thenReturn(listOf(inactiveUser))

        scheduler.sendMonthlyReports()

        verifyNoInteractions(emailService)
    }

    @Test
    fun `sendMonthlyReports skips deleted users`() {
        val deletedUser = createUser("deleted@test.com", isActive = true, isDeleted = true)
        `when`(userRepository.findAll()).thenReturn(listOf(deletedUser))

        scheduler.sendMonthlyReports()

        verifyNoInteractions(emailService)
    }

    @Test
    fun `sendMonthlyReports sends team reports to managers with subordinates`() {
        val subordinate = createUser("sub@test.com", isActive = true, isDeleted = false)
        val manager = createUser("manager@test.com", isActive = true, isDeleted = false)
        manager.subordinates.add(subordinate)

        `when`(userRepository.findAll()).thenReturn(listOf(manager))
        stubSummariesAndBalance(manager)
        stubSummariesAndBalance(subordinate)

        scheduler.sendMonthlyReports()

        // Personal report + team report = 2 calls for the manager
        verify(emailService, times(2)).sendAsync(
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
        )
    }

    @Test
    fun `sendMonthlyReports handles email send failure gracefully`() {
        val user1 = createUser("fail@test.com", isActive = true, isDeleted = false)
        val user2 = createUser("ok@test.com", isActive = true, isDeleted = false)
        `when`(userRepository.findAll()).thenReturn(listOf(user1, user2))
        stubSummariesAndBalance(user1)
        stubSummariesAndBalance(user2)

        // First call throws, second should still proceed
        `when`(
            emailService.sendAsync(
                any(String::class.java) ?: "",
                any(String::class.java) ?: "",
                any(String::class.java) ?: "",
            ),
        ).thenThrow(RuntimeException("SMTP error"))
            .thenAnswer { }

        // Should not throw
        scheduler.sendMonthlyReports()

        verify(emailService, times(2)).sendAsync(
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
        )
    }

    @Test
    fun `sendPersonalReport skips users with blank email`() {
        val userNoEmail = createUser("", isActive = true, isDeleted = false)
        `when`(userRepository.findAll()).thenReturn(listOf(userNoEmail))

        scheduler.sendMonthlyReports()

        verifyNoInteractions(emailService)
    }

    @Test
    fun `formatHours formats correctly`() {
        val method = MonthlyReportScheduler::class.java.getDeclaredMethod("formatHours", Int::class.java)
        method.isAccessible = true

        assertThat(method.invoke(scheduler, 0)).isEqualTo("0:00")
        assertThat(method.invoke(scheduler, 60)).isEqualTo("1:00")
        assertThat(method.invoke(scheduler, 90)).isEqualTo("1:30")
        assertThat(method.invoke(scheduler, 125)).isEqualTo("2:05")
        assertThat(method.invoke(scheduler, 480)).isEqualTo("8:00")
    }

    private fun createUser(
        email: String,
        isActive: Boolean,
        isDeleted: Boolean,
    ): UserEntity {
        val u =
            UserEntity(
                id = UUID.randomUUID(),
                email = email,
                passwordHash = "hash",
                firstName = "Test",
                lastName = "User",
            )
        u.isActive = isActive
        u.isDeleted = isDeleted
        return u
    }

    private fun stubSummariesAndBalance(user: UserEntity) {
        val summary =
            DailySummaryEntity(
                user = user,
                date = LocalDate.now().minusMonths(1),
                totalWorkMinutes = 480,
                totalBreakMinutes = 30,
                overtimeMinutes = 0,
                isCompliant = true,
            )
        `when`(
            dailySummaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(
                any(UUID::class.java) ?: UUID.randomUUID(),
                any(LocalDate::class.java) ?: LocalDate.now(),
                any(LocalDate::class.java) ?: LocalDate.now(),
            ),
        ).thenReturn(listOf(summary))
        `when`(
            vacationBalanceRepository.findByUserIdAndYear(any(UUID::class.java) ?: UUID.randomUUID(), anyInt()),
        ).thenReturn(
            VacationBalanceEntity(
                user = user,
                year = LocalDate.now().year,
                totalDays = BigDecimal("30"),
                usedDays = BigDecimal("5"),
                carriedOverDays = BigDecimal.ZERO,
            ),
        )
    }
}
