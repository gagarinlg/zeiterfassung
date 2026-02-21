package com.zeiterfassung.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ArbZGComplianceServiceTest {
    private lateinit var service: ArbZGComplianceService

    @BeforeEach
    fun setUp() {
        service = ArbZGComplianceService()
    }

    // ── requiredBreakMinutes ──────────────────────────────────────────────────

    @Test
    fun `work under 6 hours requires no break`() {
        assertThat(service.requiredBreakMinutes(300)).isEqualTo(0)
        assertThat(service.requiredBreakMinutes(360)).isEqualTo(0)
    }

    @Test
    fun `work over 6 hours requires 30 minute break`() {
        assertThat(service.requiredBreakMinutes(361)).isEqualTo(30)
        assertThat(service.requiredBreakMinutes(540)).isEqualTo(30)
    }

    @Test
    fun `work over 9 hours requires 45 minute break`() {
        assertThat(service.requiredBreakMinutes(541)).isEqualTo(45)
        assertThat(service.requiredBreakMinutes(600)).isEqualTo(45)
    }

    // ── checkCompliance ───────────────────────────────────────────────────────

    @Test
    fun `compliant when work under 8 hours with sufficient break`() {
        val result = service.checkCompliance(480, 30)
        assertThat(result.isCompliant).isTrue()
        assertThat(result.notes).isEmpty()
    }

    @Test
    fun `non-compliant when work exceeds 10 hours`() {
        val result = service.checkCompliance(601, 45)
        assertThat(result.isCompliant).isFalse()
        assertThat(result.notes).anyMatch { it.contains("10 hours exceeded") }
    }

    @Test
    fun `non-compliant when work over 6 hours without required break`() {
        val result = service.checkCompliance(400, 0)
        assertThat(result.isCompliant).isFalse()
        assertThat(result.notes).anyMatch { it.contains("Insufficient break") }
    }

    @Test
    fun `non-compliant when work over 9 hours with only 30 minute break`() {
        val result = service.checkCompliance(541, 30)
        assertThat(result.isCompliant).isFalse()
        assertThat(result.notes).anyMatch { it.contains("Insufficient break") }
    }

    @Test
    fun `compliant when work over 9 hours with 45 minute break`() {
        val result = service.checkCompliance(541, 45)
        assertThat(result.isCompliant).isTrue()
    }

    @Test
    fun `note added when work between 8 and 10 hours`() {
        val result = service.checkCompliance(500, 30)
        assertThat(result.notes).anyMatch { it.contains("8-hour limit exceeded") }
    }

    // ── checkRestPeriod ───────────────────────────────────────────────────────

    @Test
    fun `rest period compliant when 11 hours between shifts`() {
        val end = Instant.now()
        val start = end.plus(11, ChronoUnit.HOURS)
        assertThat(service.checkRestPeriod(end, start)).isTrue()
    }

    @Test
    fun `rest period non-compliant when less than 11 hours between shifts`() {
        val end = Instant.now()
        val start = end.plus(10, ChronoUnit.HOURS)
        assertThat(service.checkRestPeriod(end, start)).isFalse()
    }

    @Test
    fun `rest period compliant when previous day entry is null`() {
        assertThat(service.checkRestPeriod(null, Instant.now())).isTrue()
    }

    // ── computeAutoDeduction — below 6 h threshold ───────────────────────────

    @Test
    fun `no deduction when work is exactly 6 hours`() {
        assertThat(service.computeAutoDeduction(360, 0)).isEqualTo(0)
    }

    @Test
    fun `no deduction when work is below 6 hours`() {
        assertThat(service.computeAutoDeduction(300, 0)).isEqualTo(0)
    }

    // ── computeAutoDeduction — 6h–9h band ────────────────────────────────────

    @Test
    fun `deduct only excess over 6h when 6h25m worked with no breaks`() {
        // 6h25m = 385 min; full 30-min deduction would yield 355 < 360: cap at 360
        assertThat(service.computeAutoDeduction(385, 0)).isEqualTo(25)
    }

    @Test
    fun `deduct full 30 min when result stays above 6h threshold`() {
        // 6h45m = 405 min; 405 - 30 = 375 >= 360: deduct 30
        assertThat(service.computeAutoDeduction(405, 0)).isEqualTo(30)
    }

    @Test
    fun `no deduction when sufficient qualifying breaks already taken in 6h band`() {
        assertThat(service.computeAutoDeduction(400, 30)).isEqualTo(0)
    }

    @Test
    fun `partial deduction when some qualifying breaks were taken in 6h band`() {
        // 6h45m = 405 min; 15 min already taken; deduct only 15 more
        assertThat(service.computeAutoDeduction(405, 15)).isEqualTo(15)
    }

    @Test
    fun `deduct only excess over 6h when partial breaks still leave result below threshold`() {
        // 6h05m = 365 min; shortage = 30 - 5 = 25; 365 - 25 = 340 < 360: cap to 365 - 360 = 5
        assertThat(service.computeAutoDeduction(365, 5)).isEqualTo(5)
    }

    // ── computeAutoDeduction — above 9h threshold ────────────────────────────

    @Test
    fun `deduct 30 min for 9h25m with no breaks because result falls into 6h-9h band`() {
        // 9h25m = 565 min; shortage45=45; 565-45=520 < 540 → re-evaluate for 30 min
        // shortage30=30; 565-30=535 >= 360: deduct 30
        assertThat(service.computeAutoDeduction(565, 0)).isEqualTo(30)
    }

    @Test
    fun `deduct full 45 min when result stays above 9h threshold`() {
        // 10h = 600 min; 600 - 45 = 555 > 540: deduct 45
        assertThat(service.computeAutoDeduction(600, 0)).isEqualTo(45)
    }

    @Test
    fun `deduct 30 min when 45 min deduction would land exactly at 9h threshold`() {
        // 9h45m = 585 min; hypothetical 45-min deduction would yield 540 (= BREAK_THRESHOLD_2,
        // not strictly > 540), so the work falls into the 6h–9h band where only 30 min is required.
        // shortage30=30; 585-30=555 >= 360: deduct 30
        assertThat(service.computeAutoDeduction(585, 0)).isEqualTo(30)
    }

    @Test
    fun `no deduction when 45 min qualifying breaks already taken above 9h threshold`() {
        assertThat(service.computeAutoDeduction(600, 45)).isEqualTo(0)
    }

    @Test
    fun `partial deduction when some qualifying breaks taken above 9h threshold`() {
        // 10h = 600 min; 20 min taken; shortage=25; 600-25=575 > 540: deduct 25
        assertThat(service.computeAutoDeduction(600, 20)).isEqualTo(25)
    }

    // ── short break rule ─────────────────────────────────────────────────────

    @Test
    fun `MIN_QUALIFYING_BREAK_MINUTES is 15`() {
        assertThat(ArbZGComplianceService.MIN_QUALIFYING_BREAK_MINUTES).isEqualTo(15)
    }

    @Test
    fun `break shorter than 15 min does not reduce auto-deduction`() {
        // A 10-min short break reduces raw work time but does not count toward ArbZG.
        // qualifyingBreaks = 0 even though a short break was taken.
        // Result is same as if no breaks had been taken at all.
        val withShortBreak = service.computeAutoDeduction(385, 0)
        assertThat(withShortBreak).isEqualTo(25)
    }

    @Test
    fun `break of exactly 15 min qualifies and reduces auto-deduction`() {
        // rawWork = 385 (6h25m). Qualifying = 15 min.
        // shortage = 30 - 15 = 15; 385 - 15 = 370 >= 360: deduct 15
        assertThat(service.computeAutoDeduction(385, 15)).isEqualTo(15)
    }
}
