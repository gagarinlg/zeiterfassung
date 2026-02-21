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
        const val MAX_WORK_MINUTES = 600 // §3 ArbZG: 10h absolute max
        const val WARN_WORK_MINUTES = 480 // §3 ArbZG: 8h regular max
        const val BREAK_THRESHOLD_1 = 360 // §4 ArbZG: >6h requires 30 min break
        const val BREAK_THRESHOLD_2 = 540 // §4 ArbZG: >9h requires 45 min break
        const val REQUIRED_BREAK_1 = 30
        const val REQUIRED_BREAK_2 = 45
        const val MIN_REST_MINUTES = 660 // §5 ArbZG: 11h rest between shifts

        /**
         * Minimum break duration that counts toward the ArbZG mandatory break requirement.
         * Breaks shorter than this are not counted as work time, but do NOT qualify as
         * mandatory rest periods under §4 ArbZG.
         */
        const val MIN_QUALIFYING_BREAK_MINUTES = 15
    }

    /**
     * Checks ArbZG compliance given effective work minutes and qualifying break minutes.
     *
     * @param workMinutes    Effective work time after any automatic break deductions.
     * @param breakMinutes   Total qualifying break time (breaks >= 15 min) actually taken
     *                       plus any automatically deducted minutes.
     */
    fun checkCompliance(
        workMinutes: Int,
        breakMinutes: Int,
    ): ComplianceResult {
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

    /**
     * Computes the minutes to automatically deduct from raw work time when the employee
     * did not take sufficient qualifying breaks (breaks >= [MIN_QUALIFYING_BREAK_MINUTES]).
     *
     * The deduction respects ArbZG threshold corner cases: deducting the full required
     * break must not push the effective work time below the threshold that triggered the
     * requirement in the first place. In those cases only the excess over the threshold
     * is deducted.
     *
     * Examples:
     *  - rawWork=385 (6h25m), qualifying=0  → deduct 25 min → effectiveWork=360 (6h00m)
     *  - rawWork=405 (6h45m), qualifying=0  → deduct 30 min → effectiveWork=375 (6h15m)
     *  - rawWork=565 (9h25m), qualifying=0  → deduct 30 min → effectiveWork=535 (8h55m)
     *  - rawWork=600 (10h),   qualifying=0  → deduct 45 min → effectiveWork=555 (9h15m)
     *
     * @param rawWorkMinutes       Actual work minutes (logged time minus ALL break time).
     * @param qualifyingBreakMinutes Break minutes that count toward the ArbZG requirement
     *                             (only breaks >= [MIN_QUALIFYING_BREAK_MINUTES]).
     * @return Minutes to auto-deduct from rawWorkMinutes as missing mandatory break time.
     */
    fun computeAutoDeduction(
        rawWorkMinutes: Int,
        qualifyingBreakMinutes: Int,
    ): Int =
        when {
            rawWorkMinutes > BREAK_THRESHOLD_2 -> {
                // Currently above the 9h threshold: 45 min required.
                val shortage = maxOf(0, REQUIRED_BREAK_2 - qualifyingBreakMinutes)
                if (shortage == 0) {
                    0
                } else {
                    val afterDeduct = rawWorkMinutes - shortage
                    when {
                        afterDeduct > BREAK_THRESHOLD_2 ->
                            // After full deduction still > 9h: consistent, deduct all.
                            shortage
                        afterDeduct >= BREAK_THRESHOLD_1 -> {
                            // Deduction drops work into the 6h–9h band.
                            // Re-evaluate: only 30 min required for that band.
                            val shortage30 = maxOf(0, REQUIRED_BREAK_1 - qualifyingBreakMinutes)
                            val afterDeduct30 = rawWorkMinutes - shortage30
                            when {
                                afterDeduct30 >= BREAK_THRESHOLD_1 -> shortage30
                                else -> maxOf(0, rawWorkMinutes - BREAK_THRESHOLD_1)
                            }
                        }
                        else ->
                            // Deduction would drop below 6h: only deduct to exactly 6h.
                            maxOf(0, rawWorkMinutes - BREAK_THRESHOLD_1)
                    }
                }
            }
            rawWorkMinutes > BREAK_THRESHOLD_1 -> {
                // In the 6h–9h band: 30 min required.
                val shortage = maxOf(0, REQUIRED_BREAK_1 - qualifyingBreakMinutes)
                if (shortage == 0) {
                    0
                } else {
                    val afterDeduct = rawWorkMinutes - shortage
                    when {
                        afterDeduct >= BREAK_THRESHOLD_1 -> shortage
                        else -> maxOf(0, rawWorkMinutes - BREAK_THRESHOLD_1)
                    }
                }
            }
            else -> 0 // <= 6h: no mandatory break required.
        }

    fun checkRestPeriod(
        previousDayLastEntry: Instant?,
        currentDayFirstEntry: Instant?,
    ): Boolean {
        if (previousDayLastEntry == null || currentDayFirstEntry == null) return true
        val minutesBetween = ChronoUnit.MINUTES.between(previousDayLastEntry, currentDayFirstEntry)
        return minutesBetween >= MIN_REST_MINUTES
    }
}
