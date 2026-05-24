package com.example.driprate.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.driprate.data.model.UserDTO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onUserClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { 
                            query = it
                            viewModel.search(it)
                        },
                        placeholder = { Text("Search users or posts...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { 
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            )
        }
    ) {
innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = searchState) {
                is SearchResultState.Idle -> {
                    Text("Start typing to search", modifier = Modifier.align(Alignment.Center))
                }
                is SearchResultState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SearchResultState.Success -> {
                    LazyColumn {
                        if (state.users.isNotEmpty()) {
                            item {
                                Text("Users", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp))
                            }
                            items(state.users) { user ->
                                UserSearchItem(
                                    user = user, 
                                    onFollowClick = { viewModel.followUser(user.id) },
                                    onUnfollowClick = { viewModel.unfollowUser(user.id) }
                                )
                            }
                        }
                        
                        // We could also show publications here, but usually search shows users first or in tabs
                    }
                }
                is SearchResultState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(user: UserDTO, onFollowClick: () -> Unit, onUnfollowClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(user.userName) },
        supportingContent = { user.displayName?.let { Text(it) } },
        leadingContent = {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Button(
                onClick = { if (user.isFollowing) onUnfollowClick() else onFollowClick() },
                colors = if (user.isFollowing) 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    else ButtonDefaults.buttonColors()
            ) {
                Text(if (user.isFollowing) "Unfollow" else "Follow")
            }
        }
    )
}
