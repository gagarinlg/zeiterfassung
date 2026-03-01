package com.zeiterfassung.service

import com.zeiterfassung.model.entity.BusinessTripEntity
import com.zeiterfassung.model.entity.SickLeaveEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.entity.VacationRequestEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.MessageSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {
    @Mock
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var messageSource: MessageSource

    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationService = NotificationService(emailService, messageSource)
    }

    @Test
    fun `notifyVacationRequestCreated should send emails to all managers including substitute`() {
        val employee =
            UserEntity(
                email = "employee@test.com",
                passwordHash = "hash",
                firstName = "Max",
                lastName = "Mustermann",
            )
        val manager =
            UserEntity(
                email = "manager@test.com",
                passwordHash = "hash",
                firstName = "Jane",
                lastName = "Boss",
            )
        val substitute =
            UserEntity(
                email = "substitute@test.com",
                passwordHash = "hash",
                firstName = "John",
                lastName = "Deputy",
            )
        val request =
            VacationRequestEntity(
                user = employee,
                startDate = LocalDate.of(2026, 6, 1),
                endDate = LocalDate.of(2026, 6, 5),
                totalDays = BigDecimal("5"),
            )

        `when`(
            messageSource.getMessage(
                anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(Locale::class.java) ?: Locale.GERMAN,
            ),
        ).thenReturn("Test message")

        notificationService.notifyVacationRequestCreated(request, listOf(manager, substitute))

        // Should send to both manager and substitute
        verify(emailService, times(2)).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `notifyVacationRequestCreated should skip managers with blank email`() {
        val employee =
            UserEntity(
                email = "employee@test.com",
                passwordHash = "hash",
                firstName = "Max",
                lastName = "Mustermann",
            )
        val manager =
            UserEntity(
                email = "",
                passwordHash = "hash",
                firstName = "No",
                lastName = "Email",
            )
        val request =
            VacationRequestEntity(
                user = employee,
                startDate = LocalDate.of(2026, 6, 1),
                endDate = LocalDate.of(2026, 6, 5),
                totalDays = BigDecimal("5"),
            )

        `when`(
            messageSource.getMessage(
                anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(Locale::class.java) ?: Locale.GERMAN,
            ),
        ).thenReturn("Test message")

        notificationService.notifyVacationRequestCreated(request, listOf(manager))

        verify(emailService, times(0)).sendAsync(anyString(), anyString(), anyString())
    }

    private fun createEmployee(email: String = "employee@test.com"): UserEntity =
        UserEntity(
            email = email,
            passwordHash = "hash",
            firstName = "Max",
            lastName = "Mustermann",
        )

    private fun createManager(email: String = "manager@test.com"): UserEntity =
        UserEntity(
            email = email,
            passwordHash = "hash",
            firstName = "Jane",
            lastName = "Boss",
        )

    private fun createVacationRequest(employee: UserEntity): VacationRequestEntity =
        VacationRequestEntity(
            user = employee,
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 5),
            totalDays = BigDecimal("5"),
        )

    private fun stubMessages() {
        org.mockito.Mockito.lenient().`when`(
            messageSource.getMessage(
                anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(Locale::class.java) ?: Locale.GERMAN,
            ),
        ).thenReturn("Test message")
    }

    @Test
    fun `notifyVacationApproved should send email to employee`() {
        stubMessages()
        val employee = createEmployee()
        val request = createVacationRequest(employee)

        notificationService.notifyVacationApproved(request, "Jane Boss")

        verify(emailService, times(1)).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `notifyVacationApproved should skip when employee email is blank`() {
        val employee = createEmployee(email = "")
        val request = createVacationRequest(employee)

        notificationService.notifyVacationApproved(request, "Jane Boss")

        verify(emailService, never()).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `notifyVacationRejected should send email to employee`() {
        stubMessages()
        val employee = createEmployee()
        val request = createVacationRequest(employee)

        notificationService.notifyVacationRejected(request, "Overlapping vacation period")

        verify(emailService, times(1)).sendAsync(
            anyString(),
            anyString(),
            anyString(),
        )
    }

    @Test
    fun `notifyVacationRejected should skip when employee email is blank`() {
        val employee = createEmployee(email = "")
        val request = createVacationRequest(employee)

        notificationService.notifyVacationRejected(request, "reason")

        verify(emailService, never()).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `notifyVacationCancelledByEmployee should send email to notify user`() {
        stubMessages()
        val employee = createEmployee()
        val request = createVacationRequest(employee)
        val manager = createManager()

        notificationService.notifyVacationCancelledByEmployee(request, manager)

        verify(emailService, times(1)).sendAsync(
            anyString(),
            anyString(),
            anyString(),
        )
    }

    @Test
    fun `notifyVacationCancelledByEmployee should skip when notify user email is blank`() {
        val employee = createEmployee()
        val request = createVacationRequest(employee)
        val manager = createManager(email = "")

        notificationService.notifyVacationCancelledByEmployee(request, manager)

        verify(emailService, never()).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `notifySickLeaveReported should send email to manager`() {
        stubMessages()
        val employee = createEmployee()
        val manager = createManager()
        val sickLeave =
            SickLeaveEntity(
                user = employee,
                startDate = LocalDate.of(2026, 7, 1),
                endDate = LocalDate.of(2026, 7, 3),
                notes = "Flu",
            )

        notificationService.notifySickLeaveReported(sickLeave, manager)

        verify(emailService, times(1)).sendAsync(
            anyString(),
            anyString(),
            anyString(),
        )
    }

    @Test
    fun `notifySickLeaveReported should use noNotes fallback when notes are null`() {
        stubMessages()
        val employee = createEmployee()
        val manager = createManager()
        val sickLeave =
            SickLeaveEntity(
                user = employee,
                startDate = LocalDate.of(2026, 7, 1),
                endDate = LocalDate.of(2026, 7, 3),
                notes = null,
            )

        notificationService.notifySickLeaveReported(sickLeave, manager)

        verify(emailService, times(1)).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `notifyBusinessTripRequested should send email to manager`() {
        stubMessages()
        val employee = createEmployee()
        val manager = createManager()
        val trip =
            BusinessTripEntity(
                user = employee,
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 3),
                destination = "Berlin",
                purpose = "Client meeting",
            )

        notificationService.notifyBusinessTripRequested(trip, manager)

        verify(emailService, times(1)).sendAsync(
            anyString(),
            anyString(),
            anyString(),
        )
    }

    @Test
    fun `notifyBusinessTripApproved should send email to employee`() {
        stubMessages()
        val employee = createEmployee()
        val trip =
            BusinessTripEntity(
                user = employee,
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 3),
                destination = "Berlin",
                purpose = "Client meeting",
            )

        notificationService.notifyBusinessTripApproved(trip, "Jane Boss")

        verify(emailService, times(1)).sendAsync(
            anyString(),
            anyString(),
            anyString(),
        )
    }

    @Test
    fun `notifyBusinessTripApproved should skip when employee email is blank`() {
        val employee = createEmployee(email = "")
        val trip =
            BusinessTripEntity(
                user = employee,
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 3),
                destination = "Berlin",
                purpose = "Client meeting",
            )

        notificationService.notifyBusinessTripApproved(trip, "Jane Boss")

        verify(emailService, never()).sendAsync(anyString(), anyString(), anyString())
    }

    @Test
    fun `notifyBusinessTripRejected should send email to employee`() {
        stubMessages()
        val employee = createEmployee()
        val trip =
            BusinessTripEntity(
                user = employee,
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 3),
                destination = "Berlin",
                purpose = "Client meeting",
            )

        notificationService.notifyBusinessTripRejected(trip, "Budget constraints")

        verify(emailService, times(1)).sendAsync(
            anyString(),
            anyString(),
            anyString(),
        )
    }

    @Test
    fun `notifyBusinessTripRejected should skip when employee email is blank`() {
        val employee = createEmployee(email = "")
        val trip =
            BusinessTripEntity(
                user = employee,
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 3),
                destination = "Berlin",
                purpose = "Client meeting",
            )

        notificationService.notifyBusinessTripRejected(trip, "Budget constraints")

        verify(emailService, never()).sendAsync(anyString(), anyString(), anyString())
    }
}
