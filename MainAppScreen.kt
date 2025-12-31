package com.example.shotacon.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.shotacon.ui.theme.ShotaconTheme
import com.example.shotacon.viewmodel.MangaViewModel
import com.example.shotacon.viewmodel.NavSharedViewModel

@Composable
fun MainAppScreen(
    isDarkTheme: Boolean,
    winterTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onWinterThemeChanged: (Boolean) -> Unit
) {
    ShotaconTheme(
        darkTheme = isDarkTheme,
        winterTheme = winterTheme
    ) {
        val navController = rememberNavController()

        val navSharedViewModel: NavSharedViewModel = viewModel()
        val mangaViewModel: MangaViewModel = viewModel()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showPageControlBar = currentRoute == "manga"

        Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            Column {

                if (showPageControlBar) {
                    PageControlBar(viewModel = mangaViewModel)
                }

                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Манга") },
                        selected = currentDestination(navController) == "manga",
                        onClick = {
                            navController.navigate("manga") { launchSingleTop = true }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, null) },
                        label = { Text("Избранное") },
                        selected = currentDestination(navController) == "favorites",
                        onClick = {
                            navController.navigate("favorites") { launchSingleTop = true }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Профиль") },
                        selected = currentDestination(navController) == "profile",
                        onClick = {
                            navController.navigate("profile") { launchSingleTop = true }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Настройки") },
                        selected = currentDestination(navController) == "settings",
                        onClick = {
                            navController.navigate("settings") { launchSingleTop = true }
                        }
                    )
                }
            }
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(padding)
        ) {
            composable("splash") {
                SplashScreen(navController)
            }

            composable("manga") {
                MangaListScreen(
                    viewModel = mangaViewModel,
                    onOpenLink = { link ->
                        val encoded = java.net.URLEncoder.encode(link, "UTF-8")
                        navController.navigate("reader/$encoded")
                    }
                )
            }

            composable("favorites") {
                FavoriteScreen(
                    onOpenLink = { link ->
                        val encoded = java.net.URLEncoder.encode(link, "UTF-8")
                        navController.navigate("reader/$encoded")
                    }
                )
            }

            composable("profile") {
                ProfileScreen(
                    onFavoritesClick = {
                        navController.navigate("favorites")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChanged = onThemeChanged,
                    winterTheme = winterTheme,
                    onWinterThemeChanged = onWinterThemeChanged
                )
            }

            composable("reader/{url}") {
                val encoded = it.arguments?.getString("url") ?: ""
                val link = java.net.URLDecoder.decode(encoded, "UTF-8")
                MangaReadScreen(link)
            }
        }
    }
    }
}

@Composable
fun currentDestination(navController: NavController): String? {
    val entry by navController.currentBackStackEntryAsState()
    return entry?.destination?.route?.substringBefore("/")
}
