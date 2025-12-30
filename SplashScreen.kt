package com.example.shotacon.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shotacon.R
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable

@Composable
fun SplashScreen(navController: NavController) {
    // 🎨 Цвет фона зависит от темы (тёмная / светлая)
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    // 🔹 Анимация "пульсации" чиби-девочки с плавным эффектом
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    // 🔹 Анимация появления текста
    val textAlphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    // 🔹 Через 3 секунды переходим на экран манги
    LaunchedEffect(Unit) {
        delay(3000)
        navController.navigate("manga") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // 🔹 Surface скрывает нижнюю панель и подстраивается под тему
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable {
                navController.navigate("manga") {
                    popUpTo("splash") { inclusive = true }
                }
            },
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // 🧸 Анимированная чиби-девочка
                Image(
                    painter = painterResource(id = R.drawable.chibi_loader),
                    contentDescription = "Загрузка",
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scaleAnim),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Загружаем мангу...",
                    color = textColor.copy(alpha = textAlphaAnim),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Нажмите, чтобы пропустить",
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
