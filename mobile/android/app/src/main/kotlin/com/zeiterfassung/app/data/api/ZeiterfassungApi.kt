package com.zeiterfassung.app.data.api

import com.zeiterfassung.app.data.model.DailySummary
import com.zeiterfassung.app.data.model.EmptyBody
import com.zeiterfassung.app.data.model.LoginRequest
import com.zeiterfassung.app.data.model.LoginResponse
import com.zeiterfassung.app.data.model.PageResponse
import com.zeiterfassung.app.data.model.TimeEntry
import com.zeiterfassung.app.data.model.TrackingStatusResponse
import com.zeiterfassung.app.data.model.User
import com.zeiterfassung.app.data.model.VacationBalance
import com.zeiterfassung.app.data.model.VacationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ZeiterfassungApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: EmptyBody = EmptyBody()): Unit

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): User

    @GET("time-entries/status/{userId}")
    suspend fun getTrackingStatus(@Path("userId") userId: String): TrackingStatusResponse

    @POST("time-entries/clock-in")
    suspend fun clockIn(@Body body: EmptyBody = EmptyBody()): TimeEntry

    @POST("time-entries/clock-out")
    suspend fun clockOut(@Body body: EmptyBody = EmptyBody()): TimeEntry

    @POST("time-entries/break/start")
    suspend fun startBreak(@Body body: EmptyBody = EmptyBody()): TimeEntry

    @POST("time-entries/break/end")
    suspend fun endBreak(@Body body: EmptyBody = EmptyBody()): TimeEntry

    @GET("time-entries/timesheet/{userId}")
    suspend fun getTimesheet(
        @Path("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): TimesheetResponse

    @GET("vacation/balance/{userId}/{year}")
    suspend fun getVacationBalance(
        @Path("userId") userId: String,
        @Path("year") year: Int,
    ): VacationBalance

    @GET("vacation/requests/{userId}")
    suspend fun getVacationRequests(
        @Path("userId") userId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<VacationRequest>
}

data class TimesheetResponse(
    val userId: String,
    val startDate: String,
    val endDate: String,
    val dailySummaries: List<DailySummary>,
    val totalWorkMinutes: Int,
    val totalBreakMinutes: Int,
    val totalOvertimeMinutes: Int,
    val entries: List<TimeEntry>,
)
