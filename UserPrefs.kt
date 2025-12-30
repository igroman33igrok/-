package com.example.shotacon.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPrefs {
    private val LOGGED_IN = booleanPreferencesKey("logged_in")
    private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    private val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
    private val IMAGE_CACHING_ENABLED = booleanPreferencesKey("image_caching_enabled")
    // ✅ НОВАЯ НАСТРОЙКА: Режим быстрой загрузки
    private val BOOST_MODE = booleanPreferencesKey("boost_mode")

    fun isLoggedIn(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[LOGGED_IN] ?: false }

    suspend fun setLoggedIn(context: Context, value: Boolean) {
        context.dataStore.edit { it[LOGGED_IN] = value }
    }

    fun getDarkTheme(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[IS_DARK_THEME] ?: false }

    suspend fun setDarkTheme(context: Context, value: Boolean) {
        context.dataStore.edit { it[IS_DARK_THEME] = value }
    }

    fun isDisclaimerAccepted(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[DISCLAIMER_ACCEPTED] ?: false }

    suspend fun setDisclaimerAccepted(context: Context, accepted: Boolean) {
        context.dataStore.edit { it[DISCLAIMER_ACCEPTED] = accepted }
    }

    fun isImageCachingEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[IMAGE_CACHING_ENABLED] ?: true }

    suspend fun setImageCachingEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IMAGE_CACHING_ENABLED] = enabled }
    }

    // ✅ НОВЫЕ МЕТОДЫ: Режим быстрой загрузки
    fun isBoostModeEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[BOOST_MODE] ?: false }

    suspend fun setBoostModeEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[BOOST_MODE] = enabled }
    }
}