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

import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.driprate.ui.feed.*
import com.example.driprate.data.model.PublicationDTO
import androidx.compose.material.icons.filled.Close

import androidx.compose.foundation.lazy.LazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailsScreen(
    collectionId: String,
    collectionName: String,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit = {},
    viewModel: CollectionDetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedPostForDetail by remember { mutableStateOf<PublicationDTO?>(null) }
    
    val myUserId by viewModel.myUserId.collectAsState()
    val myCollections by viewModel.myCollections.collectAsState()
    val userAvatars by viewModel.userAvatars.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val assessmentsList by viewModel.assessments.collectAsState()
    val isAssessmentsLoading by viewModel.isAssessmentsLoading.collectAsState()
    
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showAssessmentsSheet by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf<String?>(null) }
    var showCollectionPicker by remember { mutableStateOf<String?>(null) }
    var showReportDialogForTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

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
                                Box(modifier = Modifier.clickable { selectedPostForDetail = post }) {
                                    AsyncImage(
                                        model = post.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (s.isOwnCollection) {
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
                }
                is CollectionDetailsState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // Detailed Post Dialog
    if (selectedPostForDetail != null) {
        val currentItems = (state as? CollectionDetailsState.Success)?.items ?: emptyList()
        val currentPub = currentItems.find { it.id == selectedPostForDetail!!.id } ?: selectedPostForDetail!!
        
        Dialog(
            onDismissRequest = { selectedPostForDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Post") },
                            navigationIcon = {
                                IconButton(onClick = { selectedPostForDetail = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        LazyColumn {
                            item {
                                PublicationItem(
                                    publication = currentPub.copy(authorAvatarUrl = userAvatars[currentPub.authorId]),
                                    myUserId = myUserId,
                                    onLikeClick = { viewModel.toggleLike(currentPub.id) },
                                    onSaveClick = { viewModel.toggleSave(currentPub.id) },
                                    onCollectionClick = { 
                                        showCollectionPicker = currentPub.id 
                                        viewModel.loadMyCollections()
                                    },
                                    onRatingRowClick = {
                                        if (myUserId != null && currentPub.authorId == myUserId) {
                                            viewModel.loadAssessmentsList(currentPub.id)
                                            showAssessmentsSheet = true
                                        } else {
                                            showRateDialog = currentPub.id
                                        }
                                    },
                                    onCommentsClick = {
                                        viewModel.loadComments(currentPub.id)
                                        showCommentsSheet = true
                                    },
                                    onUserClick = { userId ->
                                        selectedPostForDetail = null
                                        onUserClick(userId)
                                    },
                                    onTagClick = { tag -> },
                                    onReportClick = { showReportDialogForTarget = Pair(currentPub.id, "Publication") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheets and Dialogs mirrored from FeedScreen
    if (showCommentsSheet && selectedPostForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = sheetState
        ) {
            CommentsSheetContent(
                comments = comments,
                myUserId = myUserId,
                onPostComment = { text, parentId -> viewModel.postComment(selectedPostForDetail!!.id, text, parentId) },
                onLoadReplies = { parentId -> viewModel.loadReplies(selectedPostForDetail!!.id, parentId) },
                onDeleteComment = { commentId -> viewModel.deleteComment(selectedPostForDetail!!.id, commentId) },
                onToggleLike = { commentId -> viewModel.toggleCommentLike(selectedPostForDetail!!.id, commentId) },
                onReportCommentClick = { commentId ->
                    showReportDialogForTarget = Pair(commentId, "Comment")
                }
            )
        }
    }

    if (showAssessmentsSheet && selectedPostForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { showAssessmentsSheet = false },
            sheetState = sheetState
        ) {
            AssessmentsSheetContent(
                assessments = assessmentsList,
                isLoading = isAssessmentsLoading,
                onUserClick = { userId ->
                    showAssessmentsSheet = false
                    selectedPostForDetail = null
                    onUserClick(userId)
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
        ModalBottomSheet(
            onDismissRequest = { showCollectionPicker = null },
            sheetState = sheetState
        ) {
            CollectionPickerSheet(
                collections = myCollections,
                onCollectionSelected = { collId ->
                    viewModel.addToCollection(collId, showCollectionPicker!!)
                    showCollectionPicker = null
                },
                onCreateNewCollection = { name ->
                    viewModel.createCollection(name)
                }
            )
        }
    }

    if (showReportDialogForTarget != null) {
        com.example.driprate.ui.components.ReportDialog(
            onDismiss = { showReportDialogForTarget = null },
            onSubmit = { reason ->
                viewModel.sendReport(showReportDialogForTarget!!.first, showReportDialogForTarget!!.second, reason) { status ->
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
}
