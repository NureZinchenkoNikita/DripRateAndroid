package com.example.driprate.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailsScreen(
    collectionId: String,
    collectionName: String,
    onBackClick: () -> Unit,
    viewModel: CollectionDetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(collectionId) {
        viewModel.loadCollection(collectionId, collectionName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collectionName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val s = state) {
                is CollectionDetailsState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is CollectionDetailsState.Success -> {
                    if (s.items.isEmpty()) {
                        Text("No items in this collection", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(s.items) { post ->
                                Box {
                                    AsyncImage(
                                        model = post.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeFromCollection(collectionId, post.id, collectionName) },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove from collection",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is CollectionDetailsState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
