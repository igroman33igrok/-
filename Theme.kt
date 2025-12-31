package com.example.shotacon.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ShotaconTheme(
    darkTheme: Boolean,
    winterTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        winterTheme && darkTheme -> darkColorScheme(
            primary = WinterPrimary,
            secondary = WinterAccent,
            background = WinterBackground,
            surface = WinterBackground,
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )

        winterTheme && !darkTheme -> lightColorScheme(
            primary = WinterPrimary,
            secondary = WinterAccent,
            background = Color(0xFFF4FBFF),
            surface = Color.White,
            onPrimary = Color.Black
        )

        darkTheme -> darkColorScheme(
            primary = BluePrimary,
            background = DarkBackground,
            surface = DarkBackground
        )

        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
