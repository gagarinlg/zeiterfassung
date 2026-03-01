package com.zeiterfassung.app.data.repository

import com.zeiterfassung.app.data.api.ZeiterfassungApi
import com.zeiterfassung.app.data.model.PageResponse
import com.zeiterfassung.app.data.model.VacationBalance
import com.zeiterfassung.app.data.model.VacationRequest
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VacationRepository @Inject constructor(
    private val api: ZeiterfassungApi,
) {
    suspend fun getBalance(userId: String): Result<VacationBalance> =
        runCatching {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            api.getVacationBalance(userId, year)
        }

    suspend fun getRequests(
        userId: String,
        page: Int = 0,
        size: Int = 20,
    ): Result<PageResponse<VacationRequest>> =
        runCatching { api.getVacationRequests(userId, page, size) }
}
