package com.example.shotacon.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.shotacon.model.Manga
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(onOpenLink: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var favorites by remember { mutableStateOf<List<Manga>>(emptyList()) }

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val snapshot = db.collection("users").document(userId).get().await()
                favorites = (snapshot.get("favorites") as? List<Map<String, String>>)
                    ?.mapNotNull {
                        val title = it["title"] ?: return@mapNotNull null
                        val link = it["link"] ?: return@mapNotNull null
                        val imageUrl = it["imageUrl"] ?: ""
                        Manga(title, link, "", imageUrl)
                    } ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Избранное") })
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            if (favorites.isEmpty()) {
                item {
                    Text(
                        "Пока нет избранных манг",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(favorites) { manga ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                if (manga.link.isNotBlank()) {
                                    onOpenLink(manga.link)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = manga.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = manga.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .alignByBaseline()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
