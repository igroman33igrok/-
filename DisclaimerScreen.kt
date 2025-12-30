package com.example.shotacon.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shotacon.datastore.UserPrefs
import kotlinx.coroutines.launch

@Composable
fun DisclaimerScreen(
    context: Context,
    onAccepted: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ Предупреждение 18+",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Это приложение содержит материалы, предназначенные только для совершеннолетних, включая сцены насилия и крови. Продолжая, вы подтверждаете, что вам 18 лет или больше. Все персонажи выдуманны, их возраст больше 21 года.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                scope.launch {
                    UserPrefs.setDisclaimerAccepted(context, true)
                    onAccepted()
                }
            }) {
                Text("Мне есть 18 лет, продолжить")
            }
        }
    }
}
