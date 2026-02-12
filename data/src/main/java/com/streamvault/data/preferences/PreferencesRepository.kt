package com.streamvault.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferencesRepository @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val LAST_ACTIVE_PROVIDER_ID = longPreferencesKey("last_active_provider_id")
        val DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
    }

    val lastActiveProviderId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_ACTIVE_PROVIDER_ID]
    }

    val defaultViewMode: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_VIEW_MODE]
    }

    suspend fun setLastActiveProviderId(providerId: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ACTIVE_PROVIDER_ID] = providerId
        }
    }

    suspend fun setDefaultViewMode(viewMode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_VIEW_MODE] = viewMode
        }
    }

    suspend fun clearDefaultViewMode() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DEFAULT_VIEW_MODE)
        }
    }
}
