package com.example.shotacon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shotacon.viewmodel.MangaViewModel

@Composable
fun PageControlBar(viewModel: MangaViewModel) {
    val currentPage by viewModel.currentPageFlow.collectAsState()
    val hasNextPage by viewModel.hasNextPageFlow.collectAsState()

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Страница: $currentPage",
                style = MaterialTheme.typography.titleMedium
            )

            Row {
                IconButton(
                    onClick = { viewModel.prevPage() },
                    enabled = currentPage > 1  // ✅ Исправлено: не даёт уйти в 0 страницу
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }

                IconButton(
                    onClick = { viewModel.nextPage() },
                    enabled = hasNextPage
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}