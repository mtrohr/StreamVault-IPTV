package com.streamvault.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val LAST_ACTIVE_PROVIDER_ID = longPreferencesKey("last_active_provider_id")
        val DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
        val PARENTAL_CONTROL_LEVEL = intPreferencesKey("parental_control_level")
        val PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val DEFAULT_CATEGORY_ID = longPreferencesKey("default_category_id")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val lastActiveProviderId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_ACTIVE_PROVIDER_ID]
    }

    val defaultViewMode: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_VIEW_MODE]
    }

    val parentalControlLevel: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PARENTAL_CONTROL_LEVEL] ?: 1 // Default to 1 = LOCKED
        }

    val parentalPin: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PARENTAL_PIN] ?: "0000"
        }

    suspend fun setLastActiveProviderId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ACTIVE_PROVIDER_ID] = id
        }
    }

    suspend fun setDefaultViewMode(viewMode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_VIEW_MODE] = viewMode
        }
    }

    suspend fun setParentalControlLevel(level: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PARENTAL_CONTROL_LEVEL] = level
        }
    }

    suspend fun setParentalPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PARENTAL_PIN] = pin
        }
    }

    suspend fun clearDefaultViewMode() {
        context.dataStore.edit { preferences ->
        }
    }

    val defaultCategoryId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_CATEGORY_ID]
    }

    suspend fun setDefaultCategory(categoryId: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CATEGORY_ID] = categoryId
        }
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "system"
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language
        }
    }

    fun getAspectRatioForChannel(channelId: Long): Flow<String?> {
        val key = stringPreferencesKey("aspect_ratio_$channelId")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun setAspectRatioForChannel(channelId: Long, ratio: String) {
        val key = stringPreferencesKey("aspect_ratio_$channelId")
        context.dataStore.edit { preferences ->
            preferences[key] = ratio
        }
    }
}
