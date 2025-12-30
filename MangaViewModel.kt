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

class MangaViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ✅ ДОБАВЛЕНО: Флаг кэширования изображений
    var imageCachingEnabled = true

    // ---------- КЭШ ИЗОБРАЖЕНИЙ ----------
    private val imageCache = LruCache<String, String>(100)

    private val altDomains = listOf(
        "https://telegra.ph",
        "https://graph.org",
        "https://te.legra.ph"
    )

    // ---------- FIRESTORE ----------
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        try {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (e: Exception) {
            Log.w("MangaViewModel", "Persistence error: ${e.message}")
        }
    }

    private val pageSize = 10

    // ---------- STATE ----------
    private val _mangaList = MutableStateFlow<List<Manga>>(emptyList())
    val mangaList: StateFlow<List<Manga>> = _mangaList

    private var currentCategory: String
        get() = savedStateHandle["category"] ?: ""
        set(value) { savedStateHandle["category"] = value }

    private var currentSearch: String
        get() = savedStateHandle["search"] ?: ""
        set(value) { savedStateHandle["search"] = value }

    private var currentPage: Int
        get() = savedStateHandle["page"] ?: 0
        set(value) { savedStateHandle["page"] = value }

    private val pageSnapshots = mutableListOf<DocumentSnapshot?>()

    val currentPageFlow = MutableStateFlow(currentPage + 1)
    val hasNextPageFlow = MutableStateFlow(true)
    val errorFlow = MutableStateFlow<String?>(null)
    val isLoadingFlow = MutableStateFlow(false)

    // =========================================================
    // 🔹 ЗАГРУЗКА СТРАНИЦЫ
    // =========================================================
    fun loadMangaPage(category: String, search: String, page: Int) {
        viewModelScope.launch {
            isLoadingFlow.value = true

            if (category != currentCategory || search != currentSearch) {
                currentCategory = category
                currentSearch = search
                currentPage = 0
                pageSnapshots.clear()
                _mangaList.value = emptyList()
                currentPageFlow.value = 1
                hasNextPageFlow.value = true
            } else {
                currentPage = page
            }

            try {
                var query = db.collection("manga")
                    .whereEqualTo("category", currentCategory)
                    .orderBy("title")
                    .limit(pageSize.toLong())

                if (currentSearch.isNotBlank()) {
                    query = query
                        .whereGreaterThanOrEqualTo("title", currentSearch)
                        .whereLessThanOrEqualTo("title", currentSearch + '\uf8ff')
                }

                if (currentPage > 0 && pageSnapshots.size >= currentPage) {
                    pageSnapshots[currentPage - 1]?.let {
                        query = query.startAfter(it)
                    }
                }

                val result = withTimeout(8000) { query.get().await() }
                val docs = result.documents

                hasNextPageFlow.value = docs.size >= pageSize

                if (docs.isNotEmpty()) {
                    if (pageSnapshots.size == currentPage) {
                        pageSnapshots.add(docs.last())
                    }

                    val base = docs.mapNotNull {
                        Manga(
                            it.getString("title") ?: return@mapNotNull null,
                            it.getString("link") ?: return@mapNotNull null,
                            it.getString("category") ?: "",
                            ""
                        )
                    }

                    _mangaList.value = base
                    currentPageFlow.value = currentPage + 1

                    if (hasNextPageFlow.value) {
                        preloadNextPage()
                    }

                    // ✅ Используем imageCachingEnabled
                    if (imageCachingEnabled) {
                        preloadAllImages(base)
                    }
                } else {
                    hasNextPageFlow.value = false
                }

            } catch (e: Exception) {
                errorFlow.value = "Ошибка загрузки: ${e.message}"
                Log.e("MangaViewModel", "loadMangaPage", e)
            } finally {
                isLoadingFlow.value = false
            }
        }
    }

    // =========================================================
    // 🔹 ПРЕДЗАГРУЗКА СЛЕДУЮЩЕЙ СТРАНИЦЫ
    // =========================================================
    private fun preloadNextPage() {
        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                var query = db.collection("manga")
                    .whereEqualTo("category", currentCategory)
                    .orderBy("title")
                    .limit(pageSize.toLong())

                if (currentSearch.isNotBlank()) {
                    query = query
                        .whereGreaterThanOrEqualTo("title", currentSearch)
                        .whereLessThanOrEqualTo("title", currentSearch + '\uf8ff')
                }

                pageSnapshots.getOrNull(nextPage - 1)?.let {
                    query = query.startAfter(it)
                }

                query.get().await()
            } catch (e: Exception) {
                Log.w("MangaViewModel", "Preload failed: ${e.message}")
            }
        }
    }

    // =========================================================
    // 🔹 ПЕРЕКЛЮЧЕНИЕ СТРАНИЦ
    // =========================================================
    fun nextPage() {
        if (!hasNextPageFlow.value || isLoadingFlow.value) return

        val page = currentPage + 1
        currentPage = page
        currentPageFlow.value = page + 1

        loadMangaPage(
            category = currentCategory,
            search = currentSearch,
            page = page
        )
    }

    fun prevPage() {
        if (currentPage <= 0 || isLoadingFlow.value) return

        val page = currentPage - 1
        currentPage = page
        currentPageFlow.value = page + 1

        loadMangaPage(
            category = currentCategory,
            search = currentSearch,
            page = page
        )
    }

    // =========================================================
    // 🔹 ЗАГРУЗКА ИЗОБРАЖЕНИЙ
    // =========================================================
    private fun preloadAllImages(list: List<Manga>) {
        viewModelScope.launch(Dispatchers.IO) {
            val withImages = list.map { manga ->
                async {
                    val img = fetchFirstImageWithFallback(manga.link) ?: ""
                    manga.copy(imageUrl = img)
                }
            }.awaitAll()

            _mangaList.value = withImages
            preloadNextImages(withImages)
        }
    }

    private suspend fun fetchFirstImageWithFallback(link: String): String? = withContext(Dispatchers.IO) {
        imageCache.get(link)?.let { return@withContext it }

        altDomains.forEach { domain ->
            try {
                val modifiedLink = link.replace("https://telegra.ph", domain)
                val connection = java.net.URL(modifiedLink).openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 5000

                val doc = org.jsoup.Jsoup.parse(connection.getInputStream(), "UTF-8", modifiedLink)
                val img = doc.selectFirst("img[src]")?.absUrl("src")
                img?.let {
                    imageCache.put(link, it)
                    return@withContext it
                }
            } catch (e: Exception) {
                Log.w("MangaViewModel", "Failed to load from $domain: ${e.message}")
            }
        }
        null
    }

    private fun preloadNextImages(list: List<Manga>) {
        val next = list.takeLast(5).map { it.link }
        viewModelScope.launch(Dispatchers.IO) {
            next.forEach { fetchFirstImageWithFallback(it) }
        }
    }
}