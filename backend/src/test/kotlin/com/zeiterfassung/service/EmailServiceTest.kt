package com.zeiterfassung.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

@ExtendWith(MockitoExtension::class)
class EmailServiceTest {
    @Mock private lateinit var mailSender: JavaMailSender

    private val fromAddress = "noreply@zeiterfassung.test"

    private lateinit var enabledService: EmailService
    private lateinit var disabledService: EmailService

    @BeforeEach
    fun setUp() {
        enabledService = EmailService(mailSender, fromAddress, true)
        disabledService = EmailService(mailSender, fromAddress, false)
    }

    @Test
    fun `sendTestMail should throw IllegalStateException when mail is disabled`() {
        val ex =
            assertThrows<IllegalStateException> {
                disabledService.sendTestMail("user@example.com")
            }
        assertThat(ex.message).contains("disabled")
    }

    @Test
    fun `sendTestMail should send email when mail is enabled`() {
        enabledService.sendTestMail("user@example.com")
        verify(mailSender).send(any(SimpleMailMessage::class.java))
    }

    @Test
    fun `sendAsync should not call mailSender when mail is disabled`() {
        disabledService.sendAsync("user@example.com", "Subject", "Body")
        verify(mailSender, never()).send(any(SimpleMailMessage::class.java))
    }

    @Test
    fun `sendAsync should send email when mail is enabled`() {
        enabledService.sendAsync("user@example.com", "Subject", "Body")
        verify(mailSender).send(any(SimpleMailMessage::class.java))
    }

    @Test
    fun `sendAsync should not throw when mail is enabled but send fails`() {
        org.mockito.Mockito.doThrow(RuntimeException("SMTP error"))
            .`when`(mailSender).send(any(SimpleMailMessage::class.java))

        // Should not throw – error is swallowed
        enabledService.sendAsync("user@example.com", "Subject", "Body")
    }
}
