package com.example.driprate.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit,
    viewModel: CreatePostViewModel = viewModel()
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val wardrobeItems by viewModel.wardrobeItems.collectAsState()
    val selectedWardrobeIds by viewModel.selectedWardrobeIds.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(state) {
        if (state is CreatePostState.Success) {
            onPostCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    Text("Select Image")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { isUrgent = !isUrgent }
            ) {
                Checkbox(
                    checked = isUrgent,
                    onCheckedChange = { isUrgent = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Urgent Rating")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Tag Garments from Wardrobe", style = MaterialTheme.typography.titleSmall)
            
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wardrobeItems) { item ->
                    val isSelected = selectedWardrobeIds.contains(item.id)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.toggleWardrobeSelection(item.id) }
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state is CreatePostState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        imageUri?.let {
                            viewModel.createPost(context, it, description, isUrgent)
                        }
                    },
                    enabled = imageUri != null && description.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Post")
                }
            }

            if (state is CreatePostState.Error) {
                Text(
                    text = (state as CreatePostState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
