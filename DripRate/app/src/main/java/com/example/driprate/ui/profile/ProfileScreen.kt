package com.example.driprate.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
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
import com.example.driprate.data.model.WardrobeItemDTO
import com.example.driprate.data.model.TagDTO
import androidx.compose.material.icons.filled.Flag
import com.example.driprate.ui.components.ReportDialog
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Link
import java.io.File
import java.io.FileOutputStream
import android.net.Uri


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onUserClick: (String) -> Unit = {},
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onCollectionClick: (String, String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.profileState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPreferencesDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    LaunchedEffect(showPreferencesDialog) {
        if (showPreferencesDialog) {
            viewModel.loadPreferences()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (val s = state) {
                            is ProfileState.Success -> s.user.displayName ?: "Profile"
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
                        IconButton(onClick = { showPreferencesDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Preferences")
                        }
                        IconButton(onClick = { viewModel.logout(onLogout) }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    } else {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.Flag, contentDescription = "Report User", tint = MaterialTheme.colorScheme.error)
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
                    wardrobe = s.wardrobe,
                    isOwnProfile = s.isOwnProfile,
                    onDeletePost = { postId -> 
                        viewModel.deletePost(postId, userId)
                        scope.launch {
                            snackbarHostState.showSnackbar("Post deleted")
                        }
                    },
                    onUpdateProfile = { displayName, bio ->
                        viewModel.updateProfile(displayName, bio) { success ->
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
                    },
                    onAddWardrobeItem = { name, brand, storeLink, price, photo, onComplete ->
                        viewModel.addWardrobeItem(name, brand, storeLink, price, photo, onComplete)
                    },
                    onUpdateWardrobeItem = { id, name, brand, storeLink, price, photo, onComplete ->
                        viewModel.updateWardrobeItem(id, name, brand, storeLink, price, photo, onComplete)
                    },
                    onDeleteWardrobeItem = { id, onComplete ->
                        viewModel.deleteWardrobeItem(id, onComplete)
                    },
                    onUserClick = onUserClick,
                    viewModel = viewModel
                )
                is ProfileState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            }

            if (showPreferencesDialog) {
                val allTags by viewModel.allTags.collectAsState()
                val preferredTags by viewModel.preferredTags.collectAsState()

                PreferencesDialog(
                    allTags = allTags,
                    preferredTags = preferredTags,
                    onDismiss = { showPreferencesDialog = false },
                    onSave = { selectedIds ->
                        viewModel.savePreferences(selectedIds) { success ->
                            if (success) {
                                showPreferencesDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Preferences saved successfully")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to save preferences")
                                }
                            }
                        }
                    }
                )
            }

            if (showReportDialog && userId != null) {
                val context = LocalContext.current
                ReportDialog(
                    onDismiss = { showReportDialog = false },
                    onSubmit = { reason ->
                        viewModel.sendReport(userId, "User", reason) { status ->
                            showReportDialog = false
                            val msg = when (status) {
                                null -> "Report submitted successfully"
                                "duplicate" -> "You have already reported this content"
                                else -> "Failed to submit report"
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: UserDTO,
    posts: List<PublicationDTO>,
    collections: List<com.example.driprate.data.api.CollectionDTO>,
    wardrobe: List<WardrobeItemDTO>,
    isOwnProfile: Boolean,
    onDeletePost: (String) -> Unit,
    onUpdateProfile: (String?, String?) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onUpdateAvatar: (MultipartBody.Part) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onCollectionClick: (String, String) -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onAddWardrobeItem: (String, String?, String?, Double?, MultipartBody.Part?, (String?) -> Unit) -> Unit,
    onUpdateWardrobeItem: (String, String, String?, String?, Double?, MultipartBody.Part?, (String?) -> Unit) -> Unit,
    onDeleteWardrobeItem: (String, (Boolean) -> Unit) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ProfileViewModel
) {
    val followersList by viewModel.followersList.collectAsState()
    val followingList by viewModel.followingList.collectAsState()
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddWardrobeDialog by remember { mutableStateOf(false) }
    var selectedWardrobeItemForDetail by remember { mutableStateOf<WardrobeItemDTO?>(null) }
    var showEditWardrobeDialog by remember { mutableStateOf<WardrobeItemDTO?>(null) }
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
            val body = MultipartBody.Part.createFormData("File", file.name, requestFile)
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
        
        Text(text = user.displayName ?: "", style = MaterialTheme.typography.headlineMedium)
        
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
            StatItem("Followers", user.followersCount, onClick = {
                viewModel.loadFollowers(user.id)
                showFollowersDialog = true
            })
            StatItem("Following", user.followingCount, onClick = {
                viewModel.loadFollowing(user.id)
                showFollowingDialog = true
            })
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
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Collections", modifier = Modifier.padding(8.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Wardrobe", modifier = Modifier.padding(8.dp))
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
        } else if (selectedTab == 1) {
            Column(modifier = Modifier.weight(1f)) {
                if (isOwnProfile) {
                    Button(
                        onClick = { showCreateCollectionDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text("Create Collection")
                    }
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
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
                                val isDefault = collection.name.lowercase() == "likes" || 
                                                collection.name.lowercase() == "liked" || 
                                                collection.name.lowercase() == "saved" || 
                                                collection.name.lowercase() == "збережене" ||
                                                collection.name.lowercase() == "подобається"
                                
                                if (isOwnProfile && !isDefault) {
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
        } else if (selectedTab == 2) {
            Column(modifier = Modifier.weight(1f)) {
                if (isOwnProfile) {
                    Button(
                        onClick = { showAddWardrobeDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Garment")
                    }
                }
                
                if (wardrobe.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Wardrobe is empty", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(wardrobe) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clickable { selectedWardrobeItemForDetail = item }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        placeholder = rememberVectorPainter(Icons.Default.Image),
                                        error = rememberVectorPainter(Icons.Default.Image)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        item.price?.let {
                                            Text(
                                                text = "$${it}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
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
    }

    if (showAddWardrobeDialog) {
        var name by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("") }
        var storeLink by remember { mutableStateOf("") }
        var priceStr by remember { mutableStateOf("") }
        var itemImageUri by remember { mutableStateOf<Uri?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        val photoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            itemImageUri = uri
        }

        AlertDialog(
            onDismissRequest = { showAddWardrobeDialog = false },
            title = { Text("Add New Garment") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (itemImageUri != null) {
                            AsyncImage(
                                model = itemImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Select Photo")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { 
                            brand = it
                            errorMessage = null
                        },
                        label = { Text("Brand (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = storeLink,
                        onValueChange = { 
                            storeLink = it
                            errorMessage = null
                        },
                        label = { Text("Store Link (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { 
                            priceStr = it
                            errorMessage = null
                        },
                        label = { Text("Estimated Price ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            isSubmitting = true
                            errorMessage = null
                            var photoPart: MultipartBody.Part? = null
                            itemImageUri?.let { uri ->
                                val file = File(context.cacheDir, "temp_cloth_${System.currentTimeMillis()}.jpg")
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(file).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                photoPart = MultipartBody.Part.createFormData("Photo", file.name, requestFile)
                            }
                            val priceValue = priceStr.toDoubleOrNull()
                            onAddWardrobeItem(name, brand.takeIf { it.isNotBlank() }, storeLink.takeIf { it.isNotBlank() }, priceValue, photoPart) { error ->
                                isSubmitting = false
                                if (error == null) {
                                    showAddWardrobeDialog = false
                                } else {
                                    errorMessage = error
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !isSubmitting
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWardrobeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedWardrobeItemForDetail != null) {
        val item = selectedWardrobeItemForDetail!!
        AlertDialog(
            onDismissRequest = { selectedWardrobeItemForDetail = null },
            title = { Text(item.name) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = rememberVectorPainter(Icons.Default.Image),
                        error = rememberVectorPainter(Icons.Default.Image)
                    )

                    if (!item.brand.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Brand: ${item.brand}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (item.price != null) {
                        Text("Price: $${item.price}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    if (!item.storeLink.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Shop Link",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.clickable {
                                    // Open link or just display
                                }
                            )
                        }
                        Text(item.storeLink, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                if (isOwnProfile) {
                    Button(onClick = {
                        showEditWardrobeDialog = item
                        selectedWardrobeItemForDetail = null
                    }) {
                        Text("Edit")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isOwnProfile) {
                        TextButton(
                            onClick = {
                                onDeleteWardrobeItem(item.id) { success ->
                                    if (success) {
                                        selectedWardrobeItemForDetail = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = { selectedWardrobeItemForDetail = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showEditWardrobeDialog != null) {
        val item = showEditWardrobeDialog!!
        var name by remember { mutableStateOf(item.name) }
        var brand by remember { mutableStateOf(item.brand ?: "") }
        var storeLink by remember { mutableStateOf(item.storeLink ?: "") }
        var priceStr by remember { mutableStateOf(item.price?.toString() ?: "") }
        var itemImageUri by remember { mutableStateOf<Uri?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        val photoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            itemImageUri = uri
        }

        AlertDialog(
            onDismissRequest = { showEditWardrobeDialog = null },
            title = { Text("Edit Garment") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (itemImageUri != null) {
                            AsyncImage(
                                model = itemImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = rememberVectorPainter(Icons.Default.Image),
                                error = rememberVectorPainter(Icons.Default.Image)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { 
                            brand = it
                            errorMessage = null
                        },
                        label = { Text("Brand (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = storeLink,
                        onValueChange = { 
                            storeLink = it
                            errorMessage = null
                        },
                        label = { Text("Store Link (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { 
                            priceStr = it
                            errorMessage = null
                        },
                        label = { Text("Estimated Price ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            isSubmitting = true
                            errorMessage = null
                            var photoPart: MultipartBody.Part? = null
                            itemImageUri?.let { uri ->
                                val file = File(context.cacheDir, "temp_cloth_${System.currentTimeMillis()}.jpg")
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(file).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                photoPart = MultipartBody.Part.createFormData("Photo", file.name, requestFile)
                            }
                            val priceValue = priceStr.toDoubleOrNull()
                            onUpdateWardrobeItem(item.id, name, brand.takeIf { it.isNotBlank() }, storeLink.takeIf { it.isNotBlank() }, priceValue, photoPart) { error ->
                                isSubmitting = false
                                if (error == null) {
                                    showEditWardrobeDialog = null
                                } else {
                                    errorMessage = error
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !isSubmitting
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWardrobeDialog = null }) {
                    Text("Cancel")
                }
            }
        )
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
            onConfirm = { displayName, bio ->
                onUpdateProfile(displayName, bio)
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

    if (showFollowersDialog) {
        UserListDialog(
            title = "Followers",
            users = followersList,
            onDismiss = { showFollowersDialog = false },
            onUserClick = onUserClick
        )
    }

    if (showFollowingDialog) {
        UserListDialog(
            title = "Following",
            users = followingList,
            onDismiss = { showFollowingDialog = false },
            onUserClick = onUserClick
        )
    }
}

@Composable
fun EditProfileDialog(
    user: UserDTO,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(user.displayName ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = displayName, 
                    onValueChange = { 
                        displayName = it
                        error = null
                    }, 
                    label = { Text("Display Name") },
                    isError = error != null,
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
                    if (displayName.isBlank()) {
                        error = "Display Name cannot be empty"
                    } else {
                        onConfirm(displayName, bio)
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
fun StatItem(label: String, count: Int, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun UserListDialog(
    title: String,
    users: List<UserDTO>,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No users found", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onUserClick(user.id)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholder = rememberVectorPainter(Icons.Default.Image),
                                error = rememberVectorPainter(Icons.Default.Image)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.displayName ?: "", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDialog(
    allTags: List<TagDTO>,
    preferredTags: List<TagDTO>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var selectedIds by remember(preferredTags) {
        mutableStateOf(preferredTags.map { it.id }.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Style Preferences") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select your preferred styles and tags to personalize your feed recommendations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                if (allTags.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val grouped = allTags.groupBy { it.category ?: "Style" }
                    for ((category, tags) in grouped) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (tag in tags) {
                                    val isSelected = selectedIds.contains(tag.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedIds = if (isSelected) {
                                                selectedIds - tag.id
                                            } else {
                                                selectedIds + tag.id
                                            }
                                        },
                                        label = { Text(tag.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedIds.toList()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}