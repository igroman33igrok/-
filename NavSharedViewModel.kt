package com.example.shotacon.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NavSharedViewModel : ViewModel() {
    private val _url = MutableStateFlow("")
    val url: StateFlow<String> get() = _url

    fun setUrl(value: String) {
        _url.value = value.trim()
    }
}
