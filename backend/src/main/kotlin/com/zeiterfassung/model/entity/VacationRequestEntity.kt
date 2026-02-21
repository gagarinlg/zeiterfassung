package com.zeiterfassung.model.entity

import com.zeiterfassung.model.enums.VacationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "vacation_requests")
class VacationRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,
    @Column(name = "is_half_day_start", nullable = false)
    var isHalfDayStart: Boolean = false,
    @Column(name = "is_half_day_end", nullable = false)
    var isHalfDayEnd: Boolean = false,
    @Column(name = "total_days", nullable = false)
    var totalDays: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VacationStatus = VacationStatus.PENDING,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    var approvedBy: UserEntity? = null,
    @Column(name = "rejection_reason")
    var rejectionReason: String? = null,
    var notes: String? = null,
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
