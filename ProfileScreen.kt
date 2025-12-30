package com.example.shotacon.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale

// Модель для хранения данных об аватаре
data class LocalAvatar(
    val fileName: String,
    val resId: Int,
    val name: String,
    val anime: String,
    val age: String
)

// Получение resId из drawable по имени файла
fun getAvatarResId(context: Context, fileName: String): Int {
    val nameWithoutExt = fileName.substringBeforeLast('.').lowercase()
    return context.resources.getIdentifier(nameWithoutExt, "drawable", context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onFavoritesClick: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    // Все аватарки с подписями
    val avatarOptions = listOf(
        LocalAvatar("Hoshino.jpg", getAvatarResId(context, "Hoshino.jpg"), "Таканаши Хошино", "Blue Archive", "17 лет"),
        LocalAvatar("Koro.jpg", getAvatarResId(context, "Koro.jpg"), "Коро-сэнсэй", "Ansatsu Kyoushitsu", "Бог смерти"),
        LocalAvatar("Rikka.jpg", getAvatarResId(context, "Rikka.jpg"), "Таканаши Рикка", "Love, Chunibyo & Other Delusions!", "16 лет"),
        LocalAvatar("Mahiro.png", getAvatarResId(context, "Mahiro.png"), "Ояма Махиро", "Onimai", "22 (было)"),
        LocalAvatar("Alya.png", getAvatarResId(context, "Alya.png"), "Алиса Кудзё", "Alya Sometimes Hides Her Feelings in Russian", "15 лет"),
        LocalAvatar("Ruby.png", getAvatarResId(context, "Ruby.png"), "Руби Хошино", "Oshi no Ko", "18 лет")
    )

    var displayName by remember { mutableStateOf<String?>(null) }
    var selectedAvatar by remember { mutableStateOf<LocalAvatar?>(null) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var loadingProfile by remember { mutableStateOf(true) }

    // Для редактирования ника
    var showNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Загрузка данных из Firebase
    LaunchedEffect(uid) {
        if (uid == null) {
            loadingProfile = false
            return@LaunchedEffect
        }
        loadingProfile = true
        try {
            val doc = db.collection("users").document(uid).get().await()
            displayName = doc.getString("displayName") ?: auth.currentUser?.displayName
            val savedAvatarName = doc.getString("avatarName")
            selectedAvatar = avatarOptions.find { it.fileName == savedAvatarName } ?: avatarOptions[0]
        } catch (e: Exception) {
            selectedAvatar = avatarOptions[0]
            displayName = auth.currentUser?.displayName
        } finally {
            loadingProfile = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Профиль") }) }) { padding ->
        if (loadingProfile) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Аватарка (один тап — выбор)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { showAvatarPicker = true }
            ) {
                selectedAvatar?.let {
                    Image(
                        painter = painterResource(id = it.resId),
                        contentDescription = "Аватар",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Ник (теперь кликабельный для изменения)
            Text(
                text = "Ник: ${displayName ?: "Не задан"}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    newName = displayName ?: ""
                    showNameDialog = true
                }
            )

            Spacer(Modifier.height(8.dp))
            Text(text = "E-mail: ${auth.currentUser?.email ?: "Не указан"}")

            Spacer(Modifier.height(24.dp))
            Button(onClick = onFavoritesClick, modifier = Modifier.fillMaxWidth()) {
                Text("Избранное")
            }

            Spacer(Modifier.weight(1f))
            Text(
                text = "Версия приложения 1.2",
                modifier = Modifier.align(Alignment.Start),
                color = Color.Gray
            )
        }
    }

    // Диалог с Pager для выбора аватарок
    if (showAvatarPicker) {
        Dialog(onDismissRequest = { showAvatarPicker = false }) {
            Surface(shape = MaterialTheme.shapes.medium) {
                val pagerState = rememberPagerState(pageCount = { avatarOptions.size })
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Выберите аватарку", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.height(220.dp)
                    ) { page ->
                        val avatar = avatarOptions[page]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                                    .clickable {
                                        scope.launch {
                                            if (uid != null) {
                                                db.collection("users").document(uid)
                                                    .set(mapOf("avatarName" to avatar.fileName), SetOptions.merge())
                                                    .await()
                                                selectedAvatar = avatar
                                            }
                                            showAvatarPicker = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = avatar.resId),
                                    contentDescription = avatar.fileName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(avatar.name)
                            Text(avatar.anime)
                            Text(avatar.age)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showAvatarPicker = false }) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }

    // Диалог изменения ника
    if (showNameDialog) {
        Dialog(onDismissRequest = { showNameDialog = false }) {
            Surface(shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Изменить ник", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Ник") }
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNameDialog = false }) {
                            Text("Отмена")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch {
                                if (uid != null) {
                                    db.collection("users").document(uid)
                                        .set(mapOf("displayName" to newName), SetOptions.merge())
                                        .await()
                                    displayName = newName
                                }
                                showNameDialog = false
                            }
                        }) {
                            Text("Сохранить")
                        }
                    }
                }
            }
        }
    }
}

// Копирование текста в буфер обмена
fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("UID", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
}
