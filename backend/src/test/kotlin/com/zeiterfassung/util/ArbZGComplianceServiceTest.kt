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

    @Test
    fun `note added when work between 8 and 10 hours`() {
        val result = service.checkCompliance(500, 30)
        assertThat(result.notes).anyMatch { it.contains("8-hour limit exceeded") }
    }
}
