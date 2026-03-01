package com.zeiterfassung.model.entity

import com.zeiterfassung.model.enums.BusinessTripStatus
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
@Table(name = "business_trips")
class BusinessTripEntity(
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
    @Column(nullable = false)
    var destination: String,
    @Column(nullable = false)
    var purpose: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BusinessTripStatus = BusinessTripStatus.REQUESTED,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    var approvedBy: UserEntity? = null,
    @Column(name = "rejection_reason")
    var rejectionReason: String? = null,
    var notes: String? = null,
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    var estimatedCost: BigDecimal? = null,
    @Column(name = "actual_cost", precision = 10, scale = 2)
    var actualCost: BigDecimal? = null,
    @Column(name = "cost_center")
    var costCenter: String? = null,
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
