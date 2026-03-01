package com.zeiterfassung.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val employeeNumber: String?,
    val phone: String?,
    val photoUrl: String?,
    val managerId: String?,
    val isActive: Boolean,
    val roles: List<String>,
    val permissions: List<String>,
) {
    val fullName: String get() = "$firstName $lastName"
}

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "expiresIn") val expiresIn: Int,
    val user: User,
)

@JsonClass(generateAdapter = true)
data class TrackingStatusResponse(
    val status: String,
    val clockedInSince: String?,
    val breakStartedAt: String?,
    val elapsedWorkMinutes: Int,
    val elapsedBreakMinutes: Int,
    val todayWorkMinutes: Int,
    val todayBreakMinutes: Int,
)

@JsonClass(generateAdapter = true)
data class TimeEntry(
    val id: String,
    val userId: String,
    val entryType: String,
    val timestamp: String,
    val source: String,
    val notes: String?,
    val isModified: Boolean,
)

@JsonClass(generateAdapter = true)
data class DailySummary(
    val id: String,
    val userId: String,
    val date: String,
    val totalWorkMinutes: Int,
    val totalBreakMinutes: Int,
    val overtimeMinutes: Int,
    val isCompliant: Boolean,
    val complianceNotes: String?,
)

@JsonClass(generateAdapter = true)
data class VacationBalance(
    val id: String,
    val userId: String,
    val year: Int,
    val totalDays: Double,
    val usedDays: Double,
    val carriedOverDays: Double,
    val remainingDays: Double,
)

@JsonClass(generateAdapter = true)
data class VacationRequest(
    val id: String,
    val userId: String,
    val startDate: String,
    val endDate: String,
    val isHalfDayStart: Boolean,
    val isHalfDayEnd: Boolean,
    val totalDays: Double,
    val status: String,
    val approvedBy: String?,
    val rejectionReason: String?,
    val notes: String?,
    val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int,
)

@JsonClass(generateAdapter = true)
data class EmptyBody(
    val dummy: String? = null,
)
