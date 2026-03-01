package com.zeiterfassung.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @param:Value("\${app.mail.from}") private val fromAddress: String,
    @param:Value("\${app.mail.enabled:true}") private val mailEnabled: Boolean,
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    /**
     * Sends a plain-text email asynchronously. Silently swallows errors so
     * a mail failure never breaks the primary business operation.
     */
    @Async
    fun sendAsync(
        to: String,
        subject: String,
        body: String,
    ) {
        if (!mailEnabled) {
            logger.debug("Mail disabled – skipping email to {} subject: {}", to, subject)
            return
        }
        try {
            val message = SimpleMailMessage()
            message.from = fromAddress
            message.setTo(to)
            message.subject = subject
            message.text = body
            mailSender.send(message)
            logger.debug("Email sent to {} subject: {}", to, subject)
        } catch (e: Exception) {
            logger.error("Failed to send email to {}: {}", to, e.message)
        }
    }

    /**
     * Sends a test email synchronously and returns a result message.
     * Unlike sendAsync, this throws on failure so the caller can report the error.
     * Checks mailEnabled first to avoid connection attempts when mail is disabled.
     */
    fun sendTestMail(to: String) {
        if (!mailEnabled) {
            throw IllegalStateException(
                "Mail sending is disabled. Set MAIL_ENABLED=true and configure SMTP settings to enable email.",
            )
        }
        val message = SimpleMailMessage()
        message.from = fromAddress
        message.setTo(to)
        message.subject = "Zeiterfassung – E-Mail-Test"
        message.text =
            "Diese E-Mail bestätigt, dass die E-Mail-Konfiguration korrekt funktioniert.\n\n" +
            "This email confirms that the email configuration is working correctly.\n\n" +
            "— Zeiterfassung System"
        mailSender.send(message)
        logger.info("Test email sent successfully to {}", to)
    }
}
