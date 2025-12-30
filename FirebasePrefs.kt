package com.example.shotacon.datastore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebasePrefs {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun userDoc() = auth.currentUser?.uid?.let {
        firestore.collection("users").document(it)
    }

    // Получение избранного
    suspend fun getFavorites(): Set<String> {
        val doc = userDoc()?.get()?.await()
        val list = doc?.get("favorites") as? List<String> ?: emptyList()
        return list.toSet()
    }

    // Сохранение избранного
    suspend fun setFavorites(favorites: Set<String>) {
        userDoc()?.set(mapOf("favorites" to favorites.toList()), com.google.firebase.firestore.SetOptions.merge())
    }

    // Получение тёмной темы
    suspend fun getDarkTheme(): Boolean {
        val doc = userDoc()?.get()?.await()
        return doc?.getBoolean("darkTheme") ?: false
    }

    // Сохранение тёмной темы
    suspend fun setDarkTheme(value: Boolean) {
        userDoc()?.set(mapOf("darkTheme" to value), com.google.firebase.firestore.SetOptions.merge())
    }
}
