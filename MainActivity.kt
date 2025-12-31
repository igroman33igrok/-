package com.example.shotacon

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.shotacon.datastore.UserPrefs
import com.example.shotacon.ui.*
import com.example.shotacon.ui.theme.ShotaconTheme
import com.example.shotacon.viewmodel.MangaViewModel
import com.example.shotacon.viewmodel.NavSharedViewModel
import androidx.compose.material3.MaterialTheme
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.initialize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity(), ImageLoaderFactory {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        setContent {
            MyApp(activity = this)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.2)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}

@Composable
fun MyApp(activity: Activity) {
    val context = LocalContext.current
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var isOffline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        isOffline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
    }

    if (isOffline) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Нет интернета") },
            text = { Text("Приложение не может работать без подключения.") },
            confirmButton = {
                TextButton(onClick = { isOffline = false }) {
                    Text("Продолжить")
                }
            },
            dismissButton = {
                TextButton(onClick = { activity.finish() }) {
                    Text("Выйти")
                }
            }
        )
    } else {
        AppEntryPoint(activity)
    }
}

@Composable
fun AppEntryPoint(activity: Activity) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var disclaimerAccepted by remember { mutableStateOf(false) }
    var disclaimerChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        disclaimerAccepted = UserPrefs.isDisclaimerAccepted(activity).first()
        disclaimerChecked = true
    }

    if (!disclaimerChecked) return

    // Используем системную тему по умолчанию для экранов входа
    var isDarkTheme by rememberSaveable { mutableStateOf(false) }
    var winterTheme by rememberSaveable { mutableStateOf(false) }

    // Загружаем сохраненные настройки темы, если пользователь уже входил
    LaunchedEffect(Unit) {
        isDarkTheme = UserPrefs.getDarkTheme(activity).first()
        winterTheme = false // По умолчанию зимняя тема отключена
    }

    // Обновляем темы при входе пользователя
    var loggedIn by remember { mutableStateOf(auth.currentUser != null) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            auth.currentUser?.let {
                val doc = db.collection("users").document(it.uid).get().await()
                isDarkTheme = doc.getBoolean("darkTheme") ?: false
                winterTheme = doc.getBoolean("winterTheme") ?: false
            }
        }
    }

    ShotaconTheme(
        darkTheme = isDarkTheme,
        winterTheme = winterTheme
    ) {
        if (!disclaimerAccepted) {
            DisclaimerScreen(activity) {
                disclaimerAccepted = true
            }
            return@ShotaconTheme
        }

        if (loggedIn) {
            MainAppScreen(
                isDarkTheme = isDarkTheme,
                winterTheme = winterTheme,
                onThemeChanged = { value ->
                    isDarkTheme = value
                    auth.currentUser?.let {
                        scope.launch {
                            db.collection("users").document(it.uid)
                                .update("darkTheme", value)
                        }
                    }
                },
                onWinterThemeChanged = { value ->
                    winterTheme = value
                    auth.currentUser?.let {
                        scope.launch {
                            db.collection("users").document(it.uid)
                                .update("winterTheme", value)
                        }
                    }
                }
            )
        } else {
            LoginScreen(
                onSuccess = { loggedIn = true },
                context = activity
            )
        }
    }
}

@Composable
fun currentDestination(navController: NavController): String? {
    val entry by navController.currentBackStackEntryAsState()
    return entry?.destination?.route?.substringBefore("/")
}
