package com.zeiterfassung.app.data.repository

import com.zeiterfassung.app.data.api.TimesheetResponse
import com.zeiterfassung.app.data.api.ZeiterfassungApi
import com.zeiterfassung.app.data.model.TimeEntry
import com.zeiterfassung.app.data.model.TrackingStatusResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeTrackingRepository @Inject constructor(
    private val api: ZeiterfassungApi,
) {
    suspend fun getStatus(userId: String): Result<TrackingStatusResponse> =
        runCatching { api.getTrackingStatus(userId) }

    suspend fun clockIn(): Result<TimeEntry> = runCatching { api.clockIn() }

    suspend fun clockOut(): Result<TimeEntry> = runCatching { api.clockOut() }

    suspend fun startBreak(): Result<TimeEntry> = runCatching { api.startBreak() }

    suspend fun endBreak(): Result<TimeEntry> = runCatching { api.endBreak() }

    suspend fun getTimesheet(
        userId: String,
        startDate: String,
        endDate: String,
    ): Result<TimesheetResponse> = runCatching { api.getTimesheet(userId, startDate, endDate) }
}
