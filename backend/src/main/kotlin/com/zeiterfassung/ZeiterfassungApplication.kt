package com.zeiterfassung

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    exclude = [
        org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration::class,
    ],
)
@EnableAsync
@EnableScheduling
class ZeiterfassungApplication

fun main(args: Array<String>) {
    runApplication<ZeiterfassungApplication>(*args)
}
