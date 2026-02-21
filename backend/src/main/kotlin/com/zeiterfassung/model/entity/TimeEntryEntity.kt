package com.zeiterfassung.model.entity

import com.zeiterfassung.model.enums.TimeEntrySource
import com.zeiterfassung.model.enums.TimeEntryType
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
@Table(name = "time_entries")
class TimeEntryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    val entryType: TimeEntryType,
    @Column(nullable = false)
    var timestamp: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: TimeEntrySource = TimeEntrySource.WEB,
    @Column(name = "terminal_id")
    val terminalId: String? = null,
    var notes: String? = null,
    @Column(name = "is_modified", nullable = false)
    var isModified: Boolean = false,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    var modifiedBy: UserEntity? = null,
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
