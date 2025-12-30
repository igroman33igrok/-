package com.example.shotacon

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.shotacon.ui.*
import com.example.shotacon.viewmodel.MangaViewModel
import com.example.shotacon.viewmodel.NavSharedViewModel
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun Navigation(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navSharedViewModel: NavSharedViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // ---------- Splash ----------
        composable("splash") {
            SplashScreen(navController)
        }

        // ---------- Manga list ----------
        composable("mangaList") {
            val mangaViewModel: MangaViewModel = viewModel()

            MangaListScreen(
                onOpenLink = { link ->
                    val encoded = URLEncoder.encode(link, "UTF-8")
                    navController.navigate("reader/$encoded")
                },
                viewModel = mangaViewModel
            )
        }

        // ---------- Favorites ----------
        composable("favorites") {
            val mangaViewModel: MangaViewModel = viewModel()

            FavoriteScreen(
                onOpenLink = { link ->
                    val encoded = URLEncoder.encode(link, "UTF-8")
                    navController.navigate("reader/$encoded")
                }
            )
        }

        // ---------- Profile ----------
        composable("profile") {
            ProfileScreen(
                onFavoritesClick = {
                    navController.navigate("favorites")
                }
            )
        }

        // ---------- Settings ----------
        composable("settings") {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onThemeChanged = onThemeChanged
            )
        }

        // ---------- Reader ----------
        composable("reader/{link}") { backStack ->
            val encoded = backStack.arguments?.getString("link") ?: ""
            val decoded = URLDecoder.decode(encoded, "UTF-8")

            val mangaViewModel: MangaViewModel = viewModel()

            MangaReadScreen(
                link = decoded,
                viewModel = mangaViewModel
            )
        }
    }
}
