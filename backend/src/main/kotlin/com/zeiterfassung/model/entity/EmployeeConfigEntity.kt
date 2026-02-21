package com.zeiterfassung.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "employee_configs")
class EmployeeConfigEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: UserEntity,
    @Column(name = "weekly_work_hours", nullable = false)
    var weeklyWorkHours: BigDecimal = BigDecimal("40.00"),
    @Column(name = "daily_work_hours", nullable = false)
    var dailyWorkHours: BigDecimal = BigDecimal("8.00"),
    // stored as JSON array string; use "text" for H2 test compatibility
    @Column(name = "work_days", nullable = false, columnDefinition = "text")
    var workDays: String = "[1,2,3,4,5]",
    @Column(name = "vacation_days_per_year", nullable = false)
    var vacationDaysPerYear: Int = 30,
    @Column(name = "vacation_carry_over_max", nullable = false)
    var vacationCarryOverMax: Int = 10,
    @Column(name = "contract_start_date")
    var contractStartDate: LocalDate? = null,
    @Column(name = "contract_end_date")
    var contractEndDate: LocalDate? = null,
    @Column(name = "is_home_office_eligible", nullable = false)
    var isHomeOfficeEligible: Boolean = false,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
