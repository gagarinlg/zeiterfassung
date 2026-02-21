package com.zeiterfassung.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "vacation_balances")
class VacationBalanceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(nullable = false)
    var year: Int,
    @Column(name = "total_days", nullable = false)
    var totalDays: BigDecimal = BigDecimal.ZERO,
    @Column(name = "used_days", nullable = false)
    var usedDays: BigDecimal = BigDecimal.ZERO,
    @Column(name = "carried_over_days", nullable = false)
    var carriedOverDays: BigDecimal = BigDecimal.ZERO,
    // Generated column in PostgreSQL; insertable=false,updatable=false prevents Hibernate from writing it.
    @Column(name = "remaining_days", insertable = false, updatable = false)
    var remainingDays: BigDecimal = BigDecimal.ZERO,
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
