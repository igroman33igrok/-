package com.example.shotacon.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.*
import kotlin.random.Random
import coil.compose.AsyncImage
import com.example.shotacon.model.Manga
import com.example.shotacon.viewmodel.MangaViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 🎄 Новогодние украшения
@Composable
fun Snowflakes() {
    val snowflakes = remember { List(15) { SnowflakeData() } }

    snowflakes.forEach { snowflake ->
        SnowflakeItem(snowflake)
    }
}

data class SnowflakeData(
    val id: Int = Random.nextInt(),
    val x: Float = Random.nextFloat(),
    val speed: Float = Random.nextFloat() * 2 + 1, // скорость падения
    val size: Float = Random.nextFloat() * 8 + 4, // размер снежинки
    val opacity: Float = Random.nextFloat() * 0.5f + 0.3f // прозрачность
)

@Composable
fun SnowflakeItem(data: SnowflakeData) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween((5000 / data.speed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(
                x = (data.x * LocalContext.current.resources.displayMetrics.widthPixels).dp,
                y = yOffset.dp
            )
    ) {
        Text(
            text = "❄",
            fontSize = data.size.sp,
            color = Color.White.copy(alpha = data.opacity)
        )
    }
}

@Composable
fun ChristmasDecorations() {
    // Ёлочные украшения в углах
    Box(modifier = Modifier.fillMaxSize()) {
        // Левый верхний угол - звезда
        Text(
            text = "⭐",
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .zIndex(5f)
        )

        // Правый верхний угол - новогодний шар
        Text(
            text = "🎄",
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(5f)
        )

        // Левый нижний угол - подарок
        Text(
            text = "🎁",
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .zIndex(5f)
        )

        // Правый нижний угол - свеча
        Text(
            text = "🕯️",
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .zIndex(5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaListScreen(
    onOpenLink: (String) -> Unit,
    viewModel: MangaViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var favorites by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var showErrorDialog by remember { mutableStateOf(false) }
    val errorMessage by viewModel.errorFlow.collectAsState()
    // ✅ УБРАНО: isLoading больше не показывается

    // Кэш для обложек
    var coverCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val categories = listOf("Лол", "Шот", "Классика")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    val mangaList by viewModel.mangaList.collectAsState()
    val currentPageState by viewModel.currentPageFlow.collectAsState()
    val hasNextPage by viewModel.hasNextPageFlow.collectAsState()

    // ---------- Загрузка избранного ----------
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val snapshot = db.collection("users").document(userId).get().await()
                favorites = (snapshot.get("favorites") as? List<Map<String, String>>)
                    ?.mapNotNull {
                        val title = it["title"] ?: return@mapNotNull null
                        val link = it["link"] ?: return@mapNotNull null
                        val imageUrl = it["imageUrl"] ?: ""
                        Manga(title, link, "", imageUrl)
                    } ?: emptyList()
            } catch (e: Exception) {
                showErrorDialog = true
            }
        }
    }

    // ---------- Загрузка манги ----------
    LaunchedEffect(selectedCategory) {
        viewModel.loadMangaPage(selectedCategory, "", 0)
    }

    if (errorMessage != null || showErrorDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Ошибка") },
            text = { Text("Нет подключения к интернету") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("Ок")
                }
            }
        )
    }

    // ================= ОСНОВНОЙ КОНТЕЙНЕР =================
    Box(modifier = Modifier.fillMaxSize()) {
        // 🎄 Новогодние украшения
        Snowflakes()
        ChristmasDecorations()

        Column(modifier = Modifier.fillMaxSize()) {

            // ---------- Категории ----------
            TabRow(
                selectedTabIndex = categories.indexOf(selectedCategory)
            ) {
                categories.forEach { title ->
                    Tab(
                        selected = selectedCategory == title,
                        onClick = { selectedCategory = title },
                        text = { Text(title) }
                    )
                }
            }

            // ---------- Список манги ----------
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                items(mangaList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                if (item.link.isNotBlank()) {
                                    onOpenLink(item.link)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            val coverUrl = coverCache[item.link] ?: item.imageUrl

                            AsyncImage(
                                model = if (coverUrl.isNotBlank()) coverUrl else {
                                    // Если обложка не загружена, запускаем загрузку
                                    LaunchedEffect(item.link) {
                                        if (item.link.isNotBlank() && !coverCache.containsKey(item.link)) {
                                            viewModel.loadCoverForManga(item) { loadedCover ->
                                                coverCache = coverCache + (item.link to loadedCover)
                                            }
                                        }
                                    }
                                    // Пока загружается, показываем placeholder
                                    null
                                },
                                contentDescription = null,
                                modifier = Modifier.size(100.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.title,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2
                                    )

                                    IconButton(onClick = {
                                        userId?.let {
                                            scope.launch {
                                                try {
                                                    val docRef = db.collection("users").document(it)
                                                    val newFavs = favorites.toMutableList()
                                                    val exists = newFavs.any { fav -> fav.link == item.link }

                                                    if (exists) {
                                                        newFavs.removeAll { fav -> fav.link == item.link }
                                                    } else {
                                                        newFavs.add(item)
                                                    }

                                                    favorites = newFavs

                                                    docRef.update(
                                                        "favorites",
                                                        newFavs.map { fav ->
                                                            mapOf(
                                                                "title" to fav.title,
                                                                "link" to fav.link,
                                                                "imageUrl" to fav.imageUrl
                                                            )
                                                        }
                                                    ).await()
                                                } catch (e: Exception) {
                                                    showErrorDialog = true
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = if (favorites.any { it.link == item.link })
                                                Color(0xFFFFD700) // Золотой для избранных
                                            else
                                                Color.Gray // Серый для остальных
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= ПАНЕЛЬ СТРАНИЦ =================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .zIndex(10f),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Страница: $currentPageState",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {

                    IconButton(
                        onClick = {
                            viewModel.prevPage()
                        },
                        enabled = currentPageState > 1
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.nextPage()
                        },
                        enabled = hasNextPage
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Вперёд",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}