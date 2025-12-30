package com.example.shotacon.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shotacon.R
import com.example.shotacon.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel = viewModel() // ✅ Используем ViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ Правильная подписка на StateFlow
    val imageCachingEnabled by viewModel.getImageCachingEnabled(context).collectAsState()
    val boostMode by viewModel.getBoostModeEnabled(context).collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(title = { Text("Настройки") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔷 Картинка Telegram
            Image(
                painter = painterResource(id = R.drawable.telegram_icon),
                contentDescription = "Telegram Icon",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/shotacon228"))
                        context.startActivity(intent)
                    }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 🌙 Темная тема переключатель
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Темная тема")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onThemeChanged(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🖼️ Кэширование изображений
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Кэширование обложек")
                    Text(
                        text = if (imageCachingEnabled)
                            "Обложки сохраняются и загружаются быстро"
                        else
                            "Обложки отключены для максимальной скорости",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = imageCachingEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            viewModel.setImageCachingEnabled(context, enabled)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ РЕЖИМ БЫСТРОЙ ЗАГРУЗКИ (теперь работает корректно)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Режим быстрой загрузки")
                    Text(
                        text = if (boostMode)
                            "Используются альтернативные сервера для telegra.ph"
                        else
                            "Стандартная загрузка (может быть медленной без VPN)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = boostMode,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            viewModel.setBoostModeEnabled(context, enabled)
                        }
                    }
                )
            }
        }
    }
}