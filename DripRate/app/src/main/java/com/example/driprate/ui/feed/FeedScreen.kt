package com.example.driprate.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import com.example.driprate.data.model.AssessmentDTO
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.driprate.data.model.CommentDTO
import com.example.driprate.data.model.PublicationDTO
import java.util.Locale
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    onCreatePostClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onUserClick: (String) -> Unit = {}
) {
    val feedState by viewModel.feedState.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val myCollections by viewModel.myCollections.collectAsState()
    val userAvatars by viewModel.userAvatars.collectAsState()
    
    var selectedPublicationIdForComments by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var showCollectionPicker by remember { mutableStateOf<String?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showRateDialog by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var currentTitle by remember { mutableStateOf("Global Feed") }
    val tabs = listOf("Global")
    val myUserId by viewModel.myUserId.collectAsState()
    val assessmentsList by viewModel.assessments.collectAsState()
    var selectedPublicationIdForAssessments by remember { mutableStateOf<String?>(null) } // ДОДАЛИ
    var showAssessmentsSheet by remember { mutableStateOf(false) } // ДОДАЛИ

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            viewModel.loadGlobalFeed()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Feeds",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall
                )
                NavigationDrawerItem(
                    label = { Text("Subscriptions") },
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        currentTitle = "Subscriptions"
                        viewModel.loadSubscriptionsFeed() // Не забудь додати виклик завантаження
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Top Feed") },
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        currentTitle = "Top Feed"
                        viewModel.loadTopFeed()
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Urgent Feed") },
                    selected = selectedTabIndex == 3,
                    onClick = {
                        selectedTabIndex = 3
                        currentTitle = "Urgent Feed"
                        viewModel.loadUrgentFeed()
                        scope.launch { drawerState.close() }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("My Collections", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { showCreateCollectionDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Collection")
                    }
                }

                myCollections.forEach { collection ->
                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(collection.name)
                                val isDefault = collection.name.lowercase() == "liked" || collection.name.lowercase() == "saved"
                                if (!isDefault) {
                                    IconButton(onClick = { viewModel.deleteCollection(collection.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }
                            }
                        },
                        selected = false,
                        onClick = {
                            viewModel.loadCollectionItems(collection.id)
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Прибрали Column і TabRow, залишили тільки AppBar
                CenterAlignedTopAppBar(
                    title = { Text(currentTitle) }, // Динамічний заголовок
                    navigationIcon = {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    actions = {
                        IconButton(onClick = onProfileClick) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onCreatePostClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (val state = feedState) {
                    is FeedState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is FeedState.Success -> {
                        // ТРИГЕР ЗАВАНТАЖЕННЯ: як тільки прийшли пости, фоном запускаємо збір аватарок
                        LaunchedEffect(state.items) {
                            viewModel.fetchAvatarsForPosts(state.items)
                        }

                        LazyColumn {
                            items(state.items) { publication ->
                                // Отримуємо аватарку з нашого кешу за ID автора
                                val cachedAvatar = userAvatars[publication.authorId]

                                PublicationItem(
                                    publication = publication.copy(authorAvatarUrl = cachedAvatar),
                                    myUserId = myUserId, // ДОДАЄМО передачу твого ID
                                    onLikeClick = { viewModel.toggleLike(publication.id) },
                                    onSaveClick = { viewModel.toggleSave(publication.id) },
                                    onCollectionClick = { showCollectionPicker = publication.id },
                                    onRatingRowClick = {
                                        if (myUserId != null && publication.authorId == myUserId) {
                                            // Якщо це мій пост -> виїжджає список тих, хто оцінив
                                            selectedPublicationIdForAssessments = publication.id
                                            viewModel.loadAssessmentsList(publication.id)
                                            showAssessmentsSheet = true
                                        } else {
                                            // Якщо чужий -> відкриваємо діалог із 4 повзунками
                                            showRateDialog = publication.id
                                        }
                                    },
                                    onCommentsClick = {
                                        selectedPublicationIdForComments = publication.id
                                        viewModel.loadComments(publication.id)
                                        showBottomSheet = true
                                    },
                                    onUserClick = onUserClick
                                )
                            }
                        }
                    }
                    is FeedState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            if (showAssessmentsSheet && selectedPublicationIdForAssessments != null) {
                ModalBottomSheet(
                    onDismissRequest = { showAssessmentsSheet = false },
                    sheetState = sheetState
                ) {
                    AssessmentsSheetContent(
                        assessments = assessmentsList,
                        onUserClick = { userId ->
                            showAssessmentsSheet = false
                            onUserClick(userId) // Перехід у профіль користувача
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
                        onToggleLike = { commentId -> // ДОДАНО
                            viewModel.toggleCommentLike(selectedPublicationIdForComments!!, commentId)
                        }
                    )
                }
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
                            // Always show "Saved" as an option at the top if it exists, or just show all
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                // Фільтруємо, щоб не показувати системні колекції "Saved" і "Liked"
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


@Composable
fun PublicationItem(
    publication: PublicationDTO,
    myUserId: String?,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onRatingRowClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onUserClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable { publication.authorId?.let { onUserClick(it) } },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = publication.authorAvatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                    error = rememberVectorPainter(Icons.Default.AccountCircle)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = publication.authorName ?: "Anonymous", style = MaterialTheme.typography.titleMedium)
            }

            AsyncImage(
                model = publication.imageUrl,
                contentDescription = "Post image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.Image),
                error = rememberVectorPainter(Icons.Default.Image),
                onError = {
                    android.util.Log.e("Coil", "Image load failed for URL: ${publication.imageUrl}, Error: ${it.result.throwable}")
                }
            )

            publication.description?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector =
                            if (publication.isLiked)
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (publication.isLiked)
                            Color.Red
                        else
                            Color.Gray
                    )
                }
                Text(text = "${publication.likesCount}")
                
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onCommentsClick) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments")
                }
                Text(text = "${publication.commentsCount}")

                IconButton(onClick = onSaveClick) { // Швидке збереження
                    Icon(
                        imageVector = if (publication.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Quick Save",
                        tint = if (publication.isSaved) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                // НОВА КНОПКА для додавання в колекції
                IconButton(onClick = onCollectionClick) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen, // Або Icons.Default.AddCircleOutline
                        contentDescription = "Add to specific collection",
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Компактний UI для рейтингу
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRatingRowClick() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (publication.averageAssessment != null && publication.averageAssessment > 0)
                            String.format(Locale.getDefault(), "%.1f", publication.averageAssessment)
                        else "Rate",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun CommentsSheetContent(
    comments: List<CommentDTO>,
    myUserId: String?,
    onPostComment: (String, String?) -> Unit,
    onDeleteComment: (String) -> Unit,
    onLoadReplies: (String) -> Unit,
    onToggleLike: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentDTO?>(null) } // Стан для відповіді

    Column(modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp)) {
        Text("Comments", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            items(comments) { comment ->
                CommentNode(
                    comment = comment,
                    myUserId = myUserId,
                    onReplyClick = { replyingTo = it },
                    onDeleteClick = onDeleteComment,
                    onLoadRepliesClick = onLoadReplies,
                    onToggleLike = onToggleLike
                )
            }
        }

        // Плашка, яка показує, кому ми відповідаємо
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Replying to ${replyingTo?.authorName ?: "Anonymous"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { replyingTo = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add a comment...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onPostComment(commentText, replyingTo?.id)
                        commentText = ""
                        replyingTo = null // Скидаємо після відправки
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
@Composable
fun CommentNode(
    comment: CommentDTO,
    myUserId: String?, // ДОДАЛИ параметр
    onReplyClick: (CommentDTO) -> Unit,
    onDeleteClick: (String) -> Unit, // ДОДАЛИ параметр
    onLoadRepliesClick: (String) -> Unit = {},
    onToggleLike: (String) -> Unit,
    depth: Int = 0
) {
    var expanded by remember { mutableStateOf(comment.replies.size == 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 24).dp)
    ) {
        Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
            AsyncImage(
                model = comment.authorAvatarUrl,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                error = rememberVectorPainter(Icons.Default.AccountCircle)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(comment.authorName?.takeIf { it.isNotBlank() } ?: "Anonymous", style = MaterialTheme.typography.labelLarge)
                Text(comment.text, style = MaterialTheme.typography.bodyMedium)

                // ЗАМІНЮЄМО старий Text("Reply") на цей Row:
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onReplyClick(comment) }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Кнопка лайку
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like comment",
                        tint = if (comment.isLiked) Color.Red else Color.Gray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onToggleLike(comment.id) }
                    )

                    if (comment.likesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Показуємо іконку видалення ТІЛЬКИ якщо це мій коментар
            if (myUserId != null && comment.authorId == myUserId) {
                IconButton(
                    onClick = { onDeleteClick(comment.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete comment",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Використовуємо repliesCount з сервера для відображення кнопки
        if (comment.repliesCount > 0) {
            Text(
                text = if (expanded) "Сховати відповіді" else "Переглянути відповіді (${comment.repliesCount})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(start = 40.dp, bottom = 8.dp)
                    .clickable {
                        expanded = !expanded
                        // Якщо ми розгортаємо і список локально пустий - треба буде їх завантажити
                        if (expanded && comment.replies.isEmpty()) {
                            onLoadRepliesClick(comment.id)
                        }
                    }
            )

            // Малюємо відповіді, тільки якщо вони реально є у нас в списку
            if (expanded && comment.replies.isNotEmpty()) {
                comment.replies.forEach { reply ->
                    CommentNode(
                        comment = reply,
                        myUserId = myUserId,
                        onReplyClick = onReplyClick,
                        onDeleteClick = onDeleteClick,
                        onLoadRepliesClick = onLoadRepliesClick,
                        onToggleLike = onToggleLike, // ПРОКИДАЄМО ДАЛІ
                        depth = depth + 1
                    )
                }
            } else if (expanded && comment.replies.isEmpty()) {
                // Плейсхолдер поки йде завантаження (якщо потрібно)
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 40.dp).size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }

    }
}
@Composable
fun AssessmentsSheetContent(
    assessments: List<AssessmentDTO>,
    onUserClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight(0.6f).padding(16.dp)) {
        Text("Ratings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        if (assessments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(assessments) { assessment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick(assessment.userId) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = assessment.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                            error = rememberVectorPainter(Icons.Default.AccountCircle)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = assessment.userName?.takeIf { it.isNotBlank() } ?: "Anonymous",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        // Замінили 5 зірочок на одну велику зірку з цифрою середнього балу (з 10)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", assessment.averageScore),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).padding(start = 4.dp),
                            tint = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun RatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (color: Int, fit: Int, originality: Int, style: Int) -> Unit
) {
    var color by remember { mutableFloatStateOf(5f) }
    var fit by remember { mutableFloatStateOf(5f) }
    var originality by remember { mutableFloatStateOf(5f) }
    var style by remember { mutableFloatStateOf(5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this Drip \uD83D\uDD25", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                RatingSliderRow("Color Coordination", color) { color = it }
                RatingSliderRow("Fit & Proportions", fit) { fit = it }
                RatingSliderRow("Originality", originality) { originality = it }
                RatingSliderRow("Overall Style", style) { style = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(color.toInt(), fit.toInt(), originality.toInt(), style.toInt())
            }) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RatingSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${value.toInt()}/10", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..10f,
            steps = 8 // (10 - 1) - 1
        )
    }
}