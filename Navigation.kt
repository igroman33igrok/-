package com.example.shotacon

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shotacon.ui.*
import com.example.shotacon.viewmodel.NavSharedViewModel

@Composable
fun Navigation(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navSharedViewModel: NavSharedViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController)
        }

        composable("favorites") {
            FavoriteScreen(
                onOpenLink = { url ->
                    navSharedViewModel.setUrl(url)
                    navController.navigate("webview")
                }
            )
        }
        composable("settings") {
            SettingsScreen(isDarkTheme, onThemeChanged)
        }
        composable("webview") {
            WebViewScreen(navSharedViewModel)
        }
        composable("profile") {
            ProfileScreen(onFavoritesClick = { navController.navigate("favorites") })
        }
    }
}
