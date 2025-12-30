package com.example.shotacon.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.favoriteDataStore by preferencesDataStore(name = "favorites")

object FavoritePrefs {
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites_links")

    fun getFavorites(context: Context): Flow<Set<String>> =
        context.favoriteDataStore.data.map { prefs ->
            prefs[FAVORITES_KEY] ?: emptySet()
        }

    suspend fun addFavorite(context: Context, link: String) {
        context.favoriteDataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            current.add(link)
            prefs[FAVORITES_KEY] = current
        }
    }

    suspend fun removeFavorite(context: Context, link: String) {
        context.favoriteDataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            current.remove(link)
            prefs[FAVORITES_KEY] = current
        }
    }
}
