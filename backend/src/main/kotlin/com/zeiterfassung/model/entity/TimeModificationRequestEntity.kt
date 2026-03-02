package com.zeiterfassung.model.entity

import com.zeiterfassung.model.dto.TimeModificationResponse
import com.zeiterfassung.model.enums.TimeModificationStatus
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
import java.util.UUID

@Entity
@Table(name = "time_modification_requests")
class TimeModificationRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_entry_id", nullable = false)
    val timeEntry: TimeEntryEntity,
    @Column(name = "requested_timestamp", nullable = false)
    val requestedTimestamp: Instant,
    @Column(name = "requested_notes")
    val requestedNotes: String? = null,
    @Column(nullable = false)
    val reason: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TimeModificationStatus = TimeModificationStatus.PENDING,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    var reviewedBy: UserEntity? = null,
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
        TimeModificationResponse(
            id = id,
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            timeEntryId = timeEntry.id,
            entryType = timeEntry.entryType.name,
            originalTimestamp = timeEntry.timestamp,
            requestedTimestamp = requestedTimestamp,
            requestedNotes = requestedNotes,
            reason = reason,
            status = status,
            reviewedById = reviewedBy?.id,
            reviewedByName = reviewedBy?.let { "${it.firstName} ${it.lastName}" },
            rejectionReason = rejectionReason,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
