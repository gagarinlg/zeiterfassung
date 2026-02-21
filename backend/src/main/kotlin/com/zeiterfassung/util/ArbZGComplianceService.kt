package com.zeiterfassung.util

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

data class ComplianceResult(
    val isCompliant: Boolean,
    val notes: MutableList<String>,
)

@Service
class ArbZGComplianceService {
    companion object {
        const val MAX_WORK_MINUTES = 600   // §3 ArbZG: 10h absolute max
        const val WARN_WORK_MINUTES = 480  // §3 ArbZG: 8h regular max
        const val BREAK_THRESHOLD_1 = 360  // §4 ArbZG: >6h requires 30 min break
        const val BREAK_THRESHOLD_2 = 540  // §4 ArbZG: >9h requires 45 min break
        const val REQUIRED_BREAK_1 = 30
        const val REQUIRED_BREAK_2 = 45
        const val MIN_REST_MINUTES = 660   // §5 ArbZG: 11h rest between shifts
    }

    fun checkCompliance(workMinutes: Int, breakMinutes: Int): ComplianceResult {
        val notes = mutableListOf<String>()
        var compliant = true

        if (workMinutes > MAX_WORK_MINUTES) {
            compliant = false
            notes.add("§3 ArbZG: Maximum work time of 10 hours exceeded (worked $workMinutes min)")
        } else if (workMinutes > WARN_WORK_MINUTES) {
            notes.add("§3 ArbZG: Regular 8-hour limit exceeded (worked $workMinutes min); ensure 6-month average compliance")
        }

        val required = requiredBreakMinutes(workMinutes)
        if (required > 0 && breakMinutes < required) {
            compliant = false
            notes.add("§4 ArbZG: Insufficient break time ($breakMinutes min taken, $required min required for $workMinutes min of work)")
        }

        return ComplianceResult(compliant, notes)
    }

    fun requiredBreakMinutes(workMinutes: Int): Int =
        when {
            workMinutes > BREAK_THRESHOLD_2 -> REQUIRED_BREAK_2
            workMinutes > BREAK_THRESHOLD_1 -> REQUIRED_BREAK_1
            else -> 0
        }

    fun checkRestPeriod(previousDayLastEntry: Instant?, currentDayFirstEntry: Instant?): Boolean {
        if (previousDayLastEntry == null || currentDayFirstEntry == null) return true
        val minutesBetween = ChronoUnit.MINUTES.between(previousDayLastEntry, currentDayFirstEntry)
        return minutesBetween >= MIN_REST_MINUTES
    }
}
