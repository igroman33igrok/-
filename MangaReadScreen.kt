package com.example.shotacon.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.shotacon.viewmodel.MangaViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MangaReadScreen(
    link: String,
    viewModel: MangaViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    // список загруженных URL изображения
    var images by remember { mutableStateOf<List<String>>(emptyList()) }

    // сначала загружаем первые
    var initialFetched by remember { mutableStateOf(false) }

    LaunchedEffect(link) {
        // сначала получим только первые 3 изображения
        val (first3, rest) = viewModel.parseMangaImagesByParts(link, initialCount = 3)
        images = first3
        initialFetched = true

        // асинхронно догружаем остальные
        scope.launch(Dispatchers.IO) {
            val all = first3.toMutableList()
            rest.forEach { url ->
                all.add(url)
                images = all.toList()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!initialFetched) {
            // показываем сразу индикатор до первых 3
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(images) { imgUrl ->
                AsyncImage(
                    model = imgUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                )
            }
            if (initialFetched && images.isEmpty()) {
                item {
                    Text("Не удалось загрузить мангу.", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
