package com.example.driprate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.ui.auth.AuthScreen
import com.example.driprate.ui.create.CreatePostScreen
import com.example.driprate.ui.feed.FeedScreen
import com.example.driprate.ui.theme.DripRateTheme

import com.example.driprate.ui.profile.ProfileScreen
import com.example.driprate.ui.profile.CollectionDetailsScreen
import com.example.driprate.ui.search.SearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(this)
        
        enableEdgeToEdge()
        setContent {
            DripRateTheme {
                // Стан для навігації, тепер спостерігаємо за TokenManager
                val authToken by TokenManager.tokenFlow.collectAsState()
                var currentScreen by remember { mutableStateOf("feed") }
                var selectedUserId by remember { mutableStateOf<String?>(null) }
                var selectedCollectionId by remember { mutableStateOf<String?>(null) }
                var selectedCollectionName by remember { mutableStateOf("") }

                LaunchedEffect(authToken) {
                    if (authToken == null) {
                        currentScreen = "auth"
                    } else if (currentScreen == "auth") {
                        currentScreen = "feed"
                    }
                }

                val effectiveScreen = if (authToken == null) "auth" else currentScreen

                when (effectiveScreen) {
                    "auth" -> {
                        if (authToken == null) {
                            AuthScreen(onAuthSuccess = { token ->
                                TokenManager.token = token
                            })
                        } else {
                            // If we have a token but still on auth screen, go to feed
                            currentScreen = "feed"
                        }
                    }
                    "feed" -> {
                        if (authToken == null) {
                            currentScreen = "auth"
                        } else {
                            FeedScreen(
                                onCreatePostClick = { currentScreen = "create_post" },
                                onProfileClick = { 
                                    selectedUserId = null
                                    currentScreen = "profile" 
                                },
                                onSearchClick = { currentScreen = "search" },
                                onUserClick = { userId ->
                                    selectedUserId = userId
                                    currentScreen = "profile"
                                }
                            )
                        }
                    }
                    "search" -> {
                        SearchScreen(
                            onUserClick = { userId ->
                                selectedUserId = userId
                                currentScreen = "profile"
                            },
                            onBackClick = { currentScreen = "feed" }
                        )
                    }
                    "create_post" -> {
                        CreatePostScreen(
                            onBackClick = { currentScreen = "feed" },
                            onPostCreated = { currentScreen = "feed" }
                        )
                    }
                    "profile" -> {
                        ProfileScreen(
                            userId = selectedUserId,
                            onBackClick = { currentScreen = "feed" },
                            onLogout = {
                                TokenManager.clear()
                            },
                            onCollectionClick = { id, name ->
                                selectedCollectionId = id
                                selectedCollectionName = name
                                currentScreen = "collection_details"
                            }
                        )
                    }
                    "collection_details" -> {
                        selectedCollectionId?.let { id ->
                            CollectionDetailsScreen(
                                collectionId = id,
                                collectionName = selectedCollectionName,
                                onBackClick = { currentScreen = "profile" }
                            )
                        } ?: run { currentScreen = "profile" }
                    }
                    else -> {
                        currentScreen = "feed"
                    }
                }
            }
        }
    }
}
