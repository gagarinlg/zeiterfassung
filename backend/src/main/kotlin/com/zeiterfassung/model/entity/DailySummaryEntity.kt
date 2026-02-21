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
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "daily_summaries")
class DailySummaryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(nullable = false)
    val date: LocalDate,
    @Column(name = "total_work_minutes", nullable = false)
    var totalWorkMinutes: Int = 0,
    @Column(name = "total_break_minutes", nullable = false)
    var totalBreakMinutes: Int = 0,
    @Column(name = "overtime_minutes", nullable = false)
    var overtimeMinutes: Int = 0,
    @Column(name = "is_compliant", nullable = false)
    var isCompliant: Boolean = true,
    @Column(name = "compliance_notes")
    var complianceNotes: String? = null,
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
