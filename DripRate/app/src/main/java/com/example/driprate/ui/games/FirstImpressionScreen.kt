@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.driprate.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.driprate.data.model.PublicationDTO
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.draw.rotate

@Composable
fun FirstImpressionScreen(
    onBackClick: () -> Unit,
    viewModel: FirstImpressionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val ratedCount by viewModel.ratedCount.collectAsState()
    val likedCount by viewModel.likedCount.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("First Impression", fontWeight = FontWeight.Bold)
                        if (ratedCount < 10 && state !is FirstImpressionState.Finished) {
                            Text(
                                text = "Post ${ratedCount + 1} of 10",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is FirstImpressionState.Loading -> CircularProgressIndicator()
                is FirstImpressionState.Finished -> {
                    // Summary Screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Star, 
                            contentDescription = null, 
                            modifier = Modifier.size(80.dp), 
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Session Complete!", 
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You liked $likedCount out of 10 looks.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.resetGame() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Play Again")
                        }
                        TextButton(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Feed")
                        }
                    }
                }
                is FirstImpressionState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetGame() }) { Text("Retry") }
                    }
                }
                is FirstImpressionState.Success -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Background progress indicator
                        LinearProgressIndicator(
                            progress = { ratedCount / 10f },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        SwipeableCard(
                            post = s.currentPost,
                            isTopCard = true,
                            onSwiped = { isLiked ->
                                viewModel.submitRating(s.currentPost.realId, isLiked)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeableCard(
    post: PublicationDTO,
    isTopCard: Boolean,
    onSwiped: (Boolean) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    val rotation = (offsetX.value / 20f).coerceIn(-15f, 15f)
    val alpha = (abs(offsetX.value) / 200f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.75f)
            .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
            .graphicsLayer {
                rotationZ = rotation
                // Slightly scale down cards behind the top one for a stack effect
                val scale = if (isTopCard) 1f else 0.95f
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .pointerInput(isTopCard) {
                if (!isTopCard) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX.value > 400) {
                            scope.launch {
                                offsetX.animateTo(1000f, tween(300))
                                onSwiped(true)
                            }
                        } else if (offsetX.value < -400) {
                            scope.launch {
                                offsetX.animateTo(-1000f, tween(300))
                                onSwiped(false)
                            }
                        } else {
                            scope.launch {
                                launch { offsetX.animateTo(0f, tween(300)) }
                                launch { offsetY.animateTo(0f, tween(300)) }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
    ) {
        // Image with placeholder
        AsyncImage(
            model = post.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = rememberVectorPainter(Icons.Default.Image),
            error = rememberVectorPainter(Icons.Default.Image)
        )

        // Overlay Text: Horizontal but rotated vertical (upwards)
        if (offsetX.value > 50) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                    }
            ) {
                Text(
                    "LIKE",
                    color = Color.Green,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .rotate(-90f)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else if (offsetX.value < -50) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                    }
            ) {
                Text(
                    "PASS", // Dislike feels heavy, PASS is more "game-like"
                    color = Color.Red,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .rotate(90f)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Author Info at bottom with gradient for readability
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(24.dp)
        ) {
            val displayName = post.realAuthorName
            
            Column {
                Text(
                    text = "@$displayName",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                if (!post.description.isNullOrBlank()) {
                    Text(
                        text = post.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
