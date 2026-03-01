package com.zeiterfassung.model.dto

import java.time.Instant

data class GdprDataExportResponse(
    val exportedAt: Instant,
    val personalInfo: GdprPersonalInfo,
    val timeEntries: List<GdprTimeEntry>,
    val vacationRequests: List<GdprVacationRequest>,
    val sickLeaves: List<GdprSickLeave>,
    val businessTrips: List<GdprBusinessTrip>,
    val auditLog: List<GdprAuditEntry>,
)

data class GdprPersonalInfo(
    val email: String,
    val firstName: String,
    val lastName: String,
    val employeeNumber: String?,
    val phone: String?,
    val dateFormat: String?,
    val timeFormat: String?,
    val createdAt: Instant,
)

data class GdprTimeEntry(
    val entryType: String,
    val timestamp: Instant,
    val source: String,
    val notes: String?,
)

data class GdprVacationRequest(
    val startDate: String,
    val endDate: String,
    val totalDays: String,
    val status: String,
    val notes: String?,
    val createdAt: Instant,
)

data class GdprSickLeave(
    val startDate: String,
    val endDate: String,
    val status: String,
    val hasCertificate: Boolean,
    val notes: String?,
    val createdAt: Instant,
)

data class GdprBusinessTrip(
    val startDate: String,
    val endDate: String,
    val destination: String,
    val purpose: String,
    val status: String,
    val notes: String?,
    val createdAt: Instant,
)

data class GdprAuditEntry(
    val action: String,
    val timestamp: Instant,
    val entityType: String?,
)

data class GdprDeletionResponse(
    val status: String,
    val message: String,
    val deletedAt: Instant,
)
