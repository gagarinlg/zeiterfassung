package com.zeiterfassung.model.entity

import com.zeiterfassung.model.dto.WorkHourChangeResponse
import com.zeiterfassung.model.enums.WorkHourChangeStatus
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
@Table(name = "work_hour_change_requests")
class WorkHourChangeRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(name = "current_weekly_hours", nullable = false)
    val currentWeeklyHours: BigDecimal,
    @Column(name = "requested_weekly_hours", nullable = false)
    val requestedWeeklyHours: BigDecimal,
    @Column(name = "current_daily_hours")
    val currentDailyHours: BigDecimal? = null,
    @Column(name = "requested_daily_hours")
    val requestedDailyHours: BigDecimal? = null,
    @Column(name = "effective_date", nullable = false)
    val effectiveDate: LocalDate,
    val reason: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WorkHourChangeStatus = WorkHourChangeStatus.PENDING,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    var approvedBy: UserEntity? = null,
    @Column(name = "rejection_reason")
    var rejectionReason: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    fun toResponse() =
        WorkHourChangeResponse(
            id = id,
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            currentWeeklyHours = currentWeeklyHours,
            requestedWeeklyHours = requestedWeeklyHours,
            currentDailyHours = currentDailyHours,
            requestedDailyHours = requestedDailyHours,
            effectiveDate = effectiveDate,
            reason = reason,
            status = status,
            approvedById = approvedBy?.id,
            approvedByName = approvedBy?.let { "${it.firstName} ${it.lastName}" },
            rejectionReason = rejectionReason,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
