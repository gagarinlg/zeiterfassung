package com.zeiterfassung.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverConfigDataStore by preferencesDataStore(name = "server_config")

@Singleton
class ServerConfigPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val serverUrlKey = stringPreferencesKey("server_url")

    val serverUrl: Flow<String?> = context.serverConfigDataStore.data.map { it[serverUrlKey] }

    suspend fun saveServerUrl(url: String) {
        context.serverConfigDataStore.edit { prefs ->
            prefs[serverUrlKey] = url
        }
    }

    suspend fun getServerUrl(): String? = serverUrl.first()

    suspend fun clearServerUrl() {
        context.serverConfigDataStore.edit { prefs ->
            prefs.remove(serverUrlKey)
        }
    }

    /**
     * Reads server URL from Android managed app configuration (MDM provisioning).
     * Returns null if no managed configuration is set.
     */
    fun getManagedServerUrl(): String? {
        val restrictions =
            context.getSystemService(Context.RESTRICTIONS_SERVICE) as? android.content.RestrictionsManager
        val appRestrictions = restrictions?.applicationRestrictions
        return appRestrictions?.getString("server_url")
    }

    /**
     * Returns the effective server URL: managed config > user setting > default.
     */
    suspend fun getEffectiveServerUrl(default: String): String {
        return getManagedServerUrl()
            ?: getServerUrl()
            ?: default
    }
}
