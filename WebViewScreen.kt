package com.example.shotacon.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shotacon.viewmodel.NavSharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(navSharedViewModel: NavSharedViewModel = viewModel()) {
    val context = LocalContext.current
    val url by navSharedViewModel.url.collectAsState()
    val scope = rememberCoroutineScope()

    var loadError by remember { mutableStateOf(false) }
    var showFallbackDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    if (url.isBlank() || !url.startsWith("http")) {
        Text("Ошибка: некорректная ссылка")
        return
    }

    // Альтернативные домены
    val altDomains = listOf(
        "https://telegra.ph",
        "https://graph.org",
        "https://te.legra.ph"
    )

    val effectiveUrl = remember(url) {
        altDomains.firstOrNull { domain ->
            try {
                java.net.URL(domain).openConnection().connectTimeout = 1000
                true
            } catch (e: Exception) {
                false
            }
        }?.let { domain ->
            url.replace("https://telegra.ph", domain)
        } ?: url
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

                        // ✅ УБРАНЫ deprecated методы
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                        loadsImagesAutomatically = true
                        blockNetworkLoads = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportMultipleWindows(false)
                        setGeolocationEnabled(false)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            safeBrowsingEnabled = true
                        }
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                loadError = true
                                scope.launch {
                                    delay(2000)
                                    showFallbackDialog = true
                                }
                            }
                        }
                    }

                    loadUrl(effectiveUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (showFallbackDialog) {
            AlertDialog(
                onDismissRequest = { showFallbackDialog = false },
                title = { Text("Проблема с загрузкой") },
                text = { Text("Манга загружается медленно. Открыть в браузере?") },
                confirmButton = {
                    TextButton(onClick = {
                        showFallbackDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveUrl))
                        context.startActivity(intent)
                    }) {
                        Text("Открыть в браузере")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFallbackDialog = false }) {
                        Text("Продолжить")
                    }
                }
            )
        }
    }
}