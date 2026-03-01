package com.zeiterfassung.app.data.repository

import com.zeiterfassung.app.data.api.ZeiterfassungApi
import com.zeiterfassung.app.data.model.LoginResponse
import com.zeiterfassung.app.data.model.LoginRequest
import com.zeiterfassung.app.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ZeiterfassungApi,
    private val prefs: AuthPreferences,
) {
    suspend fun login(email: String, password: String): Result<LoginResponse> =
        runCatching {
            val response = api.login(LoginRequest(email, password))
            prefs.saveTokens(response.accessToken, response.refreshToken, response.user.id)
            response
        }

    suspend fun logout() {
        runCatching { api.logout() }
        prefs.clearTokens()
    }

    suspend fun getCurrentUserId(): String? = prefs.getUserId()

    suspend fun isLoggedIn(): Boolean = prefs.getAccessToken() != null

    suspend fun getUser(userId: String): Result<User> = runCatching { api.getUser(userId) }
}
