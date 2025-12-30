package com.example.shotacon

import android.os.Bundle
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.shotacon.datastore.UserPrefs
import com.example.shotacon.ui.*
import com.example.shotacon.viewmodel.MangaViewModel
import com.example.shotacon.viewmodel.NavSharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity(), ImageLoaderFactory {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Настройка поддержки темной темы для системной панели навигации
        WindowCompat.setDecorFitsSystemWindows(window, true) // Включаем стандартное поведение

        // Инициализация Firebase
        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        setContent {
            MyApp(activity = this@MainActivity)
        }
    }

    // ⚡ Упрощенная конфигурация Coil для максимальной скорости
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.2) // 20% доступной памяти
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024) // 256MB кэш
                    .build()
            }
            .build()
    }
}

@Composable
fun MyApp(activity: Activity) {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    var isOffline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isOffline = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
    }

    if (isOffline) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Нет подключения к интернету") },
            text = { Text("Приложение не может работать без интернета.") },
            confirmButton = {
                TextButton(onClick = { isOffline = false }) {
                    Text("Продолжить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    activity.finish()
                }) {
                    Text("Выйти")
                }
            }
        )
    } else {
        AppEntryPoint(activity)
    }
}

@Composable
fun AppEntryPoint(context: Context) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var disclaimerAccepted by remember { mutableStateOf(false) }
    var disclaimerChecked by remember { mutableStateOf(false) }

    // Проверка принятия дисклеймера
    LaunchedEffect(Unit) {
        disclaimerAccepted = UserPrefs.isDisclaimerAccepted(context).first()
        disclaimerChecked = true
    }

    if (!disclaimerChecked) return

    if (!disclaimerAccepted) {
        DisclaimerScreen(context = context) {
            disclaimerAccepted = true
        }
        return
    }

    // Продолжение обычной логики
    var loggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var isDarkTheme by remember { mutableStateOf(false) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            val user = auth.currentUser
            user?.let {
                val doc = db.collection("users").document(it.uid).get().await()
                isDarkTheme = doc.getBoolean("darkTheme") ?: false
            }
        }
    }

    if (loggedIn) {
        MainAppScreen(isDarkTheme) { newTheme ->
            isDarkTheme = newTheme
            auth.currentUser?.let {
                scope.launch {
                    db.collection("users").document(it.uid)
                        .update("darkTheme", newTheme)
                }
            }
        }
    } else {
        LoginScreen(
            onSuccess = { loggedIn = true },
            context = context
        )
    }
}

@Composable
fun MainAppScreen(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navSharedViewModel: NavSharedViewModel = viewModel()
    // ✅ Создаём ОДИН ViewModel для всего NavHost
    val mangaViewModel: MangaViewModel = viewModel()

    // ✅ Определяем текущий экран
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showPageControlBar = currentRoute == "manga"

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        // Настройка системной панели навигации под тему
        val context = LocalContext.current
        LaunchedEffect(isDarkTheme) {
            val activity = context as? Activity
            if (activity != null) {
                val window = activity.window

                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightNavigationBars = !isDarkTheme
                    isAppearanceLightStatusBars = !isDarkTheme
                }

                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.statusBarColor = android.graphics.Color.TRANSPARENT

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    window.decorView.systemUiVisibility = if (isDarkTheme) {
                        window.decorView.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                    } else {
                        window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    }
                }
            }
        }
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                Column {

                    // ✅ Показываем панель только на экране манги
                    if (showPageControlBar) {
                        PageControlBar(viewModel = mangaViewModel)
                    }

                    // 🔽 ОСНОВНАЯ НАВИГАЦИЯ
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Манга") },
                            selected = currentDestination(navController) == "manga",
                            onClick = { navController.navigate("manga") { launchSingleTop = true } }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Star, null) },
                            label = { Text("Избранное") },
                            selected = currentDestination(navController) == "favorites",
                            onClick = { navController.navigate("favorites") { launchSingleTop = true } }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, null) },
                            label = { Text("Профиль") },
                            selected = currentDestination(navController) == "profile",
                            onClick = { navController.navigate("profile") { launchSingleTop = true } }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text("Настройки") },
                            selected = currentDestination(navController) == "settings",
                            onClick = { navController.navigate("settings") { launchSingleTop = true } }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "splash", // 👈 запускаем со сплэша
                modifier = Modifier.padding(innerPadding)
            ) {
                // --- Splash ---
                composable("splash") {
                    SplashScreen(navController)
                }

                // --- Главный экран ---
                composable("manga") {
                    MangaListScreen(
                        onOpenLink = { link ->
                            navSharedViewModel.setUrl(link)
                            navController.navigate("webview")
                        },
                        viewModel = mangaViewModel  // ✅ Передаём тот же ViewModel
                    )
                }

                // --- Избранное ---
                composable("favorites") {
                    FavoriteScreen(
                        onOpenLink = { link ->
                            navSharedViewModel.setUrl(link)
                            navController.navigate("webview")
                        }
                    )
                }

                // --- Профиль ---
                composable("profile") {
                    ProfileScreen(
                        onFavoritesClick = {
                            navController.navigate("favorites")
                        }
                    )
                }

                // --- Настройки ---
                composable("settings") {
                    SettingsScreen(isDarkTheme, onThemeChanged)
                }

                // --- WebView ---
                composable("webview") {
                    WebViewScreen(navSharedViewModel)
                }
            }
        }
    }
}

@Composable
fun currentDestination(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route?.split("/")?.firstOrNull()
}