package com.zeiterfassung

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ZeiterfassungApplication

fun main(args: Array<String>) {
    runApplication<ZeiterfassungApplication>(*args)
}
