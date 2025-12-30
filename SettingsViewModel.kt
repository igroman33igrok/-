package com.example.shotacon.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shotacon.datastore.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    // ✅ Правильная подписка на Flow с сохранением состояния
    fun getImageCachingEnabled(context: Context): StateFlow<Boolean> {
        return UserPrefs.isImageCachingEnabled(context)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )
    }

    fun getBoostModeEnabled(context: Context): StateFlow<Boolean> {
        return UserPrefs.isBoostModeEnabled(context)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }

    suspend fun setImageCachingEnabled(context: Context, enabled: Boolean) {
        UserPrefs.setImageCachingEnabled(context, enabled)
    }

    suspend fun setBoostModeEnabled(context: Context, enabled: Boolean) {
        UserPrefs.setBoostModeEnabled(context, enabled)
    }
}