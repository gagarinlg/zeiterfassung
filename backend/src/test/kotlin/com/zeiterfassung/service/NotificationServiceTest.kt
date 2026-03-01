package com.zeiterfassung.service

import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.entity.VacationRequestEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
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
}
