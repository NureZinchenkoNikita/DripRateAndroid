package com.example.driprate.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.driprate.data.model.UserDTO
import com.example.driprate.ui.feed.PublicationItem
import com.example.driprate.ui.feed.CommentsSheetContent
import com.example.driprate.ui.feed.AssessmentsSheetContent
import com.example.driprate.ui.feed.RatePostDialog
import androidx.compose.material.icons.filled.Flag
import com.example.driprate.ui.components.ReportDialog
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String? = null,
    viewModel: SearchViewModel = viewModel(),
    onUserClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var query by remember { mutableStateOf(initialQuery ?: "") }
    val searchState by viewModel.searchState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Publications")

    // Interactive details states
    var selectedPublicationForDetail by remember { mutableStateOf<String?>(null) }
    
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedPublicationIdForComments by remember { mutableStateOf<String?>(null) }
    val comments by viewModel.comments.collectAsState()
    
    var showAssessmentsSheet by remember { mutableStateOf(false) }
    var selectedPublicationIdForAssessments by remember { mutableStateOf<String?>(null) }
    val assessmentsList by viewModel.assessments.collectAsState()
    
    var showRateDialog by remember { mutableStateOf<String?>(null) }
    
    var showCollectionPicker by remember { mutableStateOf<String?>(null) }
    val myCollections by viewModel.myCollections.collectAsState()
    val myUserId by viewModel.myUserId.collectAsState()
    val userAvatars by viewModel.userAvatars.collectAsState()
    var showReportDialogForTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            query = initialQuery
            viewModel.search(initialQuery)
            if (initialQuery.startsWith("#")) {
                selectedTab = 1
            }
        }
    }

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
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = searchState) {
                is SearchResultState.Idle -> {
                    Text("Start typing to search", modifier = Modifier.align(Alignment.Center))
                }
                is SearchResultState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SearchResultState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                        
                        when (selectedTab) {
                            0 -> { // Users
                                if (state.users.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No users found")
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(state.users) { user ->
                                            UserSearchItem(
                                                user = user,
                                                onUserClick = onUserClick,
                                                onFollowClick = { viewModel.followUser(user.id) },
                                                onUnfollowClick = { viewModel.unfollowUser(user.id) }
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> { // Publications
                                if (state.publications.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No publications found")
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(1.dp),
                                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        items(state.publications) { pub ->
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clickable { selectedPublicationForDetail = pub.id }
                                            ) {
                                                AsyncImage(
                                                    model = pub.imageUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                    placeholder = rememberVectorPainter(Icons.Default.Image),
                                                    error = rememberVectorPainter(Icons.Default.Image)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is SearchResultState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }

            // Publication Detail Dialog
            if (selectedPublicationForDetail != null) {
                val successState = searchState as? SearchResultState.Success
                val currentPub = successState?.publications?.find { it.id == selectedPublicationForDetail }
                if (currentPub != null) {
                    Dialog(
                        onDismissRequest = { selectedPublicationForDetail = null }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.9f),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    val cachedAvatar = userAvatars[currentPub.authorId]
                                    PublicationItem(
                                        publication = currentPub.copy(authorAvatarUrl = cachedAvatar),
                                        myUserId = myUserId,
                                        onLikeClick = { viewModel.toggleLike(currentPub.id) },
                                        onSaveClick = { viewModel.toggleSave(currentPub.id) },
                                        onCollectionClick = { 
                                            showCollectionPicker = currentPub.id 
                                            viewModel.loadMyCollections()
                                        },
                                        onRatingRowClick = {
                                            if (myUserId != null && currentPub.authorId == myUserId) {
                                                selectedPublicationIdForAssessments = currentPub.id
                                                viewModel.loadAssessmentsList(currentPub.id)
                                                showAssessmentsSheet = true
                                            } else {
                                                showRateDialog = currentPub.id
                                            }
                                        },
                                        onCommentsClick = {
                                            selectedPublicationIdForComments = currentPub.id
                                            viewModel.loadComments(currentPub.id)
                                            showBottomSheet = true
                                        },
                                        onUserClick = { userId ->
                                            selectedPublicationForDetail = null
                                            onUserClick(userId)
                                        },
                                        onTagClick = { tag ->
                                            selectedPublicationForDetail = null
                                            query = "#$tag"
                                            viewModel.search("#$tag")
                                            selectedTab = 1
                                        },
                                        onReportClick = { showReportDialogForTarget = Pair(currentPub.id, "Publication") }
                                    )
                                }
                                
                                IconButton(
                                    onClick = { selectedPublicationForDetail = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Close", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Sheets and Sub-Dialogs
            if (showAssessmentsSheet && selectedPublicationIdForAssessments != null) {
                ModalBottomSheet(
                    onDismissRequest = { showAssessmentsSheet = false },
                    sheetState = sheetState
                ) {
                    AssessmentsSheetContent(
                        assessments = assessmentsList,
                        onUserClick = { userId ->
                            showAssessmentsSheet = false
                            selectedPublicationForDetail = null
                            onUserClick(userId)
                        }
                    )
                }
            }

            if (showBottomSheet && selectedPublicationIdForComments != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
                    CommentsSheetContent(
                        comments = comments,
                        myUserId = myUserId,
                        onPostComment = { text, parentId ->
                            viewModel.postComment(selectedPublicationIdForComments!!, text, parentId)
                        },
                        onDeleteComment = { commentId ->
                            viewModel.deleteComment(selectedPublicationIdForComments!!, commentId)
                        },
                        onLoadReplies = { parentId ->
                            viewModel.loadReplies(selectedPublicationIdForComments!!, parentId)
                        },
                        onToggleLike = { commentId ->
                            viewModel.toggleCommentLike(selectedPublicationIdForComments!!, commentId)
                        },
                        onReportCommentClick = { commentId ->
                            showReportDialogForTarget = Pair(commentId, "Comment")
                        }
                    )
                }
            }

            if (showRateDialog != null) {
                RatePostDialog(
                    onDismiss = { showRateDialog = null },
                    onSubmit = { color, fit, orig, style ->
                        viewModel.setAssessment(showRateDialog!!, color, fit, orig, style)
                        showRateDialog = null
                    }
                )
            }

            if (showCollectionPicker != null) {
                AlertDialog(
                    onDismissRequest = { showCollectionPicker = null },
                    title = { Text("Add to Collection") },
                    text = {
                        Column {
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                item {
                                    ListItem(
                                        headlineContent = { Text("+ Create New Collection", color = MaterialTheme.colorScheme.primary) },
                                        modifier = Modifier.clickable {
                                            showCreateCollectionDialog = true
                                        }
                                    )
                                }
                                val customCollections = myCollections.filter {
                                    it.name.lowercase() != "likes" &&
                                            it.name.lowercase() != "saved" &&
                                            it.name.lowercase() != "збережене"
                                }
                                items(customCollections) { collection ->
                                    ListItem(
                                        headlineContent = { Text(collection.name) },
                                        modifier = Modifier.clickable {
                                            viewModel.addToCollection(collection.id, showCollectionPicker!!)
                                            showCollectionPicker = null
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCollectionPicker = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showReportDialogForTarget != null) {
                val (targetId, targetType) = showReportDialogForTarget!!
                val context = LocalContext.current
                ReportDialog(
                    onDismiss = { showReportDialogForTarget = null },
                    onSubmit = { reason ->
                        viewModel.sendReport(targetId, targetType, reason) { status ->
                            showReportDialogForTarget = null
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

            if (showCreateCollectionDialog) {
                var newCollectionName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateCollectionDialog = false },
                    title = { Text("New Collection") },
                    text = {
                        TextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            placeholder = { Text("Name") }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newCollectionName.isNotBlank()) {
                                viewModel.createCollection(newCollectionName)
                                showCreateCollectionDialog = false
                            }
                        }) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateCollectionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: UserDTO,
    onUserClick: (String) -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onUserClick(user.id) },
        headlineContent = { Text(user.displayName ?: "") },
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
