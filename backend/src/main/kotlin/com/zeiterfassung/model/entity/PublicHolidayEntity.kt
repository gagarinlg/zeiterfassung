package com.zeiterfassung.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "public_holidays")
class PublicHolidayEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var date: LocalDate,
    @Column(nullable = false)
    var name: String,
    @Column(name = "state_code")
    var stateCode: String? = null,
    @Column(name = "is_recurring", nullable = false)
    var isRecurring: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
