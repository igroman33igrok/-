package com.example.shotacon.viewmodel

import android.util.Log
import android.util.LruCache
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shotacon.model.Manga
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MangaViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // =====================================================
    // FIRESTORE
    // =====================================================
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        try {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (e: Exception) {
            Log.w("MangaVM", "Firestore persistence error")
        }
    }

    private val pageSize = 10

    private val _mangaList = MutableStateFlow<List<Manga>>(emptyList())
    val mangaList: StateFlow<List<Manga>> = _mangaList

    val currentPageFlow = MutableStateFlow(1)
    val hasNextPageFlow = MutableStateFlow(true)
    val errorFlow = MutableStateFlow<String?>(null)

    private var currentCategory: String
        get() = savedStateHandle["category"] ?: ""
        set(value) { savedStateHandle["category"] = value }

    private var currentPage: Int
        get() = savedStateHandle["page"] ?: 0
        set(value) { savedStateHandle["page"] = value }

    private val pageSnapshots = mutableListOf<DocumentSnapshot?>()

    // =====================================================
    // ЗАГРУЗКА СПИСКА МАНГ
    // =====================================================
    fun loadMangaPage(category: String, search: String, page: Int) {
        viewModelScope.launch {
            try {
                if (category != currentCategory) {
                    currentCategory = category
                    currentPage = 0
                    pageSnapshots.clear()
                    _mangaList.value = emptyList()
                } else {
                    currentPage = page
                }

                var query = db.collection("manga")
                    .whereEqualTo("category", currentCategory)
                    .orderBy("title")
                    .limit(pageSize.toLong())

                if (currentPage > 0 && pageSnapshots.size >= currentPage) {
                    pageSnapshots[currentPage - 1]?.let {
                        query = query.startAfter(it)
                    }
                }

                val result = query.get().await()
                val docs = result.documents

                hasNextPageFlow.value = docs.size >= pageSize

                if (docs.isNotEmpty()) {
                    if (pageSnapshots.size == currentPage) {
                        pageSnapshots.add(docs.last())
                    }

                    _mangaList.value = docs.mapNotNull {
                        val title = it.getString("title") ?: return@mapNotNull null
                        val link = it.getString("link") ?: return@mapNotNull null
                        val category = it.getString("category") ?: ""
                        val imageUrl = it.getString("imageUrl") ?: ""

                        Manga(
                            title = title,
                            link = link,
                            category = category,
                            imageUrl = imageUrl
                        )
                    }

                    currentPageFlow.value = currentPage + 1
                }

            } catch (e: Exception) {
                errorFlow.value = "Ошибка загрузки"
                Log.e("MangaVM", "loadMangaPage", e)
            }
        }
    }

    // =====================================================
    // НАВИГАЦИЯ ПО СТРАНИЦАМ
    // =====================================================
    fun prevPage() {
        if (currentPage > 0) {
            currentPage--
            currentPageFlow.value = currentPage + 1 // Для отображения (начиная с 1)
            loadMangaPage(currentCategory, "", currentPage)
        }
    }

    fun nextPage() {
        if (hasNextPageFlow.value) {
            currentPage++
            currentPageFlow.value = currentPage + 1 // Для отображения (начиная с 1)
            loadMangaPage(currentCategory, "", currentPage)
        }
    }

    // =====================================================
    // ЗАГРУЗКА ОБЛОЖЕК
    // =====================================================
    fun loadCoverForManga(manga: Manga, onCoverLoaded: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (manga.imageUrl.isNotBlank()) {
                    onCoverLoaded(manga.imageUrl)
                    return@launch
                }

                // Парсим страницу для получения обложки
                val doc = Jsoup.connect(manga.link)
                    .timeout(8000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()

                // Ищем первую подходящую картинку
                val coverUrl = doc.select("img[src]")
                    .firstOrNull { img ->
                        val src = img.absUrl("src")
                        // Фильтруем по размеру и типу
                        src.contains("jpg", ignoreCase = true) ||
                        src.contains("png", ignoreCase = true) ||
                        src.contains("jpeg", ignoreCase = true)
                    }
                    ?.absUrl("src")
                    ?: ""

                if (coverUrl.isNotBlank()) {
                    onCoverLoaded(coverUrl)
                    // Можно сохранить в Firestore для будущих загрузок
                    try {
                        db.collection("manga")
                            .whereEqualTo("link", manga.link)
                            .get()
                            .addOnSuccessListener { docs ->
                                docs.documents.firstOrNull()?.reference?.update("imageUrl", coverUrl)
                            }
                    } catch (e: Exception) {
                        Log.w("MangaVM", "Failed to save cover to Firestore", e)
                    }
                }
            } catch (e: Exception) {
                Log.w("MangaVM", "Failed to load cover for ${manga.title}", e)
            }
        }
    }

    // =====================================================
    // 🔥 ГЛАВНОЕ: ЗАГРУЗКА МАНГИ ПО ЧАСТЯМ
    // =====================================================
    suspend fun parseMangaImagesByParts(
        link: String,
        initialCount: Int = 3
    ): Pair<List<String>, List<String>> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(link)
                .timeout(8000)
                .userAgent("Mozilla/5.0")
                .get()

            val images = doc.select("img[src]")
                .mapNotNull { it.absUrl("src") }
                .filter { it.isNotBlank() }

            val first = images.take(initialCount)
            val rest = images.drop(initialCount)

            Pair(first, rest)
        } catch (e: Exception) {
            Log.e("MangaVM", "parse error", e)
            Pair(emptyList(), emptyList())
        }
    }
}
