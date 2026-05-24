package com.example.driprate.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.driprate.data.model.PublicationDTO
import com.example.driprate.data.model.UserDTO
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onCollectionClick: (String, String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.profileState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (val s = state) {
                            is ProfileState.Success -> s.user.displayName ?: s.user.userName
                            else -> "Profile"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (userId == null || (state as? ProfileState.Success)?.isOwnProfile == true) {
                        IconButton(onClick = { viewModel.logout(onLogout) }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val s = state) {
                is ProfileState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ProfileState.Success -> ProfileContent(
                    user = s.user,
                    posts = s.posts,
                    collections = s.collections,
                    isOwnProfile = s.isOwnProfile,
                    onDeletePost = { postId -> 
                        viewModel.deletePost(postId, userId)
                        scope.launch {
                            snackbarHostState.showSnackbar("Post deleted")
                        }
                    },
                    onUpdateProfile = { username, displayName, bio ->
                        viewModel.updateProfile(username, displayName, bio) { success ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "Profile updated successfully" else "Failed to update profile"
                                )
                            }
                        }
                    },
                    onChangePassword = { old, new ->
                        viewModel.changePassword(old, new) { success ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "Password changed successfully" else "Failed to change password"
                                )
                            }
                        }
                    },
                    onUpdateAvatar = { part ->
                        viewModel.updateAvatar(part) { success ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "Avatar updated successfully" else "Failed to update avatar"
                                )
                            }
                        }
                    },
                    onCreateCollection = { name ->
                        viewModel.createCollection(name)
                    },
                    onDeleteCollection = { id ->
                        viewModel.deleteCollection(id)
                    },
                    onCollectionClick = onCollectionClick,
                    onFollowClick = { 
                        if (userId != null) {
                            viewModel.followUser(userId)
                        }
                    },
                    onUnfollowClick = {
                        if (userId != null) {
                            viewModel.unfollowUser(userId)
                        }
                    }
                )
                is ProfileState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: UserDTO,
    posts: List<PublicationDTO>,
    collections: List<com.example.driprate.data.api.CollectionDTO>,
    isOwnProfile: Boolean,
    onDeletePost: (String) -> Unit,
    onUpdateProfile: (String?, String?, String?) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onUpdateAvatar: (MultipartBody.Part) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onCollectionClick: (String, String) -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = File(context.cacheDir, "temp_avatar.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
            onUpdateAvatar(body)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable(enabled = isOwnProfile) { launcher.launch("image/*") },
                contentScale = ContentScale.Crop
            )
            if (isOwnProfile) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = user.displayName ?: user.userName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "@${user.userName}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        
        user.bio?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Posts", user.publicationsCount)
            StatItem("Followers", user.followersCount)
            StatItem("Following", user.followingCount)
        }

        if (isOwnProfile) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showEditDialog = true }) {
                    Text("Edit Profile")
                }
                OutlinedButton(onClick = { showPasswordDialog = true }) {
                    Text("Password")
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (user.isFollowing) onUnfollowClick() else onFollowClick()
                },
                modifier = Modifier.fillMaxWidth(0.5f),
                colors = if (user.isFollowing) 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    else ButtonDefaults.buttonColors()
            ) {
                Text(if (user.isFollowing) "Unfollow" else "Follow")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Posts", modifier = Modifier.padding(8.dp))
            }
            if (isOwnProfile) {
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Collections", modifier = Modifier.padding(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(posts) { post ->
                    Box {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (isOwnProfile) {
                            IconButton(
                                onClick = { onDeletePost(post.id) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1 && isOwnProfile) {
            Column(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { showCreateCollectionDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text("Create Collection")
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(collections) { collection ->
                        Card(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.5f),
                            onClick = { onCollectionClick(collection.id, collection.name) }
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                Column(modifier = Modifier.align(Alignment.Center)) {
                                    Text(collection.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${collection.itemsCount} items", style = MaterialTheme.typography.bodySmall)
                                }
                                if (isOwnProfile) {
                                    IconButton(
                                        onClick = { onDeleteCollection(collection.id) },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Collection",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCollectionDialog) {
        var collectionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = { Text("New Collection") },
            text = {
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = { collectionName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (collectionName.isNotBlank()) {
                        onCreateCollection(collectionName)
                        showCreateCollectionDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onConfirm = { username, displayName, bio ->
                onUpdateProfile(username, displayName, bio)
                showEditDialog = false
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { old, new ->
                onChangePassword(old, new)
                showPasswordDialog = false
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    user: UserDTO,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, String?) -> Unit
) {
    var username by remember { mutableStateOf(user.userName) }
    var displayName by remember { mutableStateOf(user.displayName ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = username, 
                    onValueChange = { 
                        username = it
                        error = null
                    }, 
                    label = { Text("Username") },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName, 
                    onValueChange = { displayName = it }, 
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio, 
                    onValueChange = { bio = it }, 
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (username.isBlank()) {
                        error = "Username cannot be empty"
                    } else {
                        onConfirm(username, displayName, bio)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { 
                        oldPassword = it
                        error = null
                    },
                    label = { Text("Old Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it
                        error = null
                    },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (oldPassword.isBlank() || newPassword.isBlank()) {
                        error = "All fields are required"
                    } else if (newPassword.length < 6) {
                        error = "New password must be at least 6 characters"
                    } else {
                        onConfirm(oldPassword, newPassword)
                    }
                }
            ) { Text("Change") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
