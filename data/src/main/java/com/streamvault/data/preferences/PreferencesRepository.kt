package com.streamvault.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    companion object {
        private const val PIN_SALT_BYTES = 16
        private const val PIN_HASH_ITERATIONS = 120_000
        private const val PIN_HASH_KEY_BITS = 256
    }

    private object PreferencesKeys {
        val LAST_ACTIVE_PROVIDER_ID = longPreferencesKey("last_active_provider_id")
        val DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
        val PARENTAL_CONTROL_LEVEL = intPreferencesKey("parental_control_level")
        val LEGACY_PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val PARENTAL_PIN_SALT = stringPreferencesKey("parental_pin_salt")
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
        val salt = ByteArray(PIN_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        val saltBase64 = java.util.Base64.getEncoder().encodeToString(salt)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PARENTAL_PIN_HASH] = hash
            preferences[PreferencesKeys.PARENTAL_PIN_SALT] = saltBase64
            preferences.remove(PreferencesKeys.LEGACY_PARENTAL_PIN)
        }
    }

    suspend fun verifyParentalPin(pin: String): Boolean {
        val preferences = context.dataStore.data.first()

        val storedHash = preferences[PreferencesKeys.PARENTAL_PIN_HASH]
        val storedSaltBase64 = preferences[PreferencesKeys.PARENTAL_PIN_SALT]

        if (!storedHash.isNullOrBlank() && !storedSaltBase64.isNullOrBlank()) {
            val salt = runCatching { java.util.Base64.getDecoder().decode(storedSaltBase64) }.getOrNull()
                ?: return false
            return hashPin(pin, salt) == storedHash
        }

        val legacyPin = preferences[PreferencesKeys.LEGACY_PARENTAL_PIN]
        val valid = if (!legacyPin.isNullOrBlank()) {
            pin == legacyPin
        } else {
            pin == "0000"
        }

        if (valid) {
            // One-way migrate legacy/default behavior to hashed PIN storage.
            setParentalPin(pin)
        }

        return valid
    }

    suspend fun clearDefaultViewMode() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DEFAULT_VIEW_MODE)
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

    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PIN_HASH_ITERATIONS, PIN_HASH_KEY_BITS)
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return java.util.Base64.getEncoder().encodeToString(secret.encoded)
    }
}
