package com.zeiterfassung.model.entity

import com.zeiterfassung.model.enums.SickLeaveStatus
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
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "sick_leaves")
class SickLeaveEntity(
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SickLeaveStatus = SickLeaveStatus.REPORTED,
    @Column(name = "has_certificate", nullable = false)
    var hasCertificate: Boolean = false,
    @Column(name = "certificate_submitted_at")
    var certificateSubmittedAt: Instant? = null,
    var notes: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    var reportedBy: UserEntity? = null,
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
