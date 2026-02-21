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
}
