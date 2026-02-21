package com.zeiterfassung

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class ZeiterfassungApplication

fun main(args: Array<String>) {
    runApplication<ZeiterfassungApplication>(*args)
}
