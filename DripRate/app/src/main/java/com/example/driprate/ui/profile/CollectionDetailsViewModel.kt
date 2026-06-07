package com.example.driprate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.data.model.PublicationDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.driprate.data.model.CommentDTO
import com.example.driprate.data.model.AssessmentDTO
import com.example.driprate.data.api.CollectionDTO
import com.example.driprate.data.api.CreateCollectionRequest
import com.example.driprate.data.model.AssessmentRequest
import com.example.driprate.data.model.CreateCommentRequest
import com.example.driprate.data.model.CreateReportRequest

sealed class CollectionDetailsState {
    object Loading : CollectionDetailsState()
    data class Success(
        val name: String,
        val items: List<PublicationDTO>,
        val isOwnCollection: Boolean
    ) : CollectionDetailsState()
    data class Error(val message: String) : CollectionDetailsState()
}

class CollectionDetailsViewModel : ViewModel() {
    private val _state = MutableStateFlow<CollectionDetailsState>(CollectionDetailsState.Loading)
    val state: StateFlow<CollectionDetailsState> = _state

    private val _comments = MutableStateFlow<List<CommentDTO>>(emptyList())
    val comments: StateFlow<List<CommentDTO>> = _comments.asStateFlow()

    private val _assessments = MutableStateFlow<List<AssessmentDTO>>(emptyList())
    val assessments: StateFlow<List<AssessmentDTO>> = _assessments.asStateFlow()

    private val _isAssessmentsLoading = MutableStateFlow(false)
    val isAssessmentsLoading: StateFlow<Boolean> = _isAssessmentsLoading.asStateFlow()

    private val _userAvatars = MutableStateFlow<Map<String, String>>(emptyMap())
    val userAvatars: StateFlow<Map<String, String>> = _userAvatars.asStateFlow()

    private val _myUserId = MutableStateFlow<String?>(null)
    val myUserId: StateFlow<String?> = _myUserId.asStateFlow()

    private val _myCollections = MutableStateFlow<List<CollectionDTO>>(emptyList())
    val myCollections: StateFlow<List<CollectionDTO>> = _myCollections.asStateFlow()

    init {
        loadMyProfile()
        loadMyCollections()
    }

    private fun loadMyProfile() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.getMyProfile()
                if (response.isSuccessful) {
                    _myUserId.value = response.body()?.id
                }
            } catch (e: Exception) {}
        }
    }

    fun loadCollection(collectionId: String, collectionName: String) {
        viewModelScope.launch {
            _state.value = CollectionDetailsState.Loading
            try {
                val response = RetrofitClient.collectionsApi.getCollectionItems(collectionId)
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    
                    // Check ownership
                    var isOwn = false
                    try {
                        val myCollectionsResponse = RetrofitClient.collectionsApi.getMyCollections()
                        if (myCollectionsResponse.isSuccessful) {
                            isOwn = myCollectionsResponse.body()?.any { it.id == collectionId } == true
                        }
                    } catch (e: Exception) {}

                    _state.value = CollectionDetailsState.Success(collectionName, items, isOwn)
                    fetchAvatarsForPosts(items)
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    }
                    _state.value = CollectionDetailsState.Error("Failed to load collection items: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = CollectionDetailsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun removeFromCollection(collectionId: String, publicationId: String, collectionName: String) {
        viewModelScope.launch {
            try {
                // Optimistically remove
                val currentState = _state.value
                if (currentState is CollectionDetailsState.Success) {
                    _state.value = currentState.copy(
                        items = currentState.items.filter { it.id != publicationId }
                    )
                }
                val response = RetrofitClient.collectionsApi.removeItemFromCollection(collectionId, publicationId)
                if (response.isSuccessful) {
                    loadCollection(collectionId, collectionName)
                } else {
                    loadCollection(collectionId, collectionName)
                }
            } catch (e: Exception) {
                loadCollection(collectionId, collectionName)
            }
        }
    }

    // --- Interactive publication features mirrored from ProfileViewModel ---

    fun loadMyCollections() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.getMyCollections()
                if (response.isSuccessful) {
                    _myCollections.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    fun addToCollection(collectionId: String, publicationId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.addItemToCollection(collectionId, publicationId)
                if (response.isSuccessful) {
                    updatePublicationSaveStateLocally(publicationId, true)
                }
            } catch (e: Exception) {}
        }
    }

    private fun updatePublicationSaveStateLocally(publicationId: String, isSaved: Boolean) {
        val currentState = _state.value
        if (currentState is CollectionDetailsState.Success) {
            val updated = currentState.items.map { pub ->
                if (pub.id == publicationId) pub.copy(isSaved = isSaved) else pub
            }
            _state.value = currentState.copy(items = updated)
        }
    }

    fun toggleSave(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                if (currentState is CollectionDetailsState.Success) {
                    val updated = currentState.items.map { pub ->
                        if (pub.id == publicationId) pub.copy(isSaved = !pub.isSaved) else pub
                    }
                    _state.value = currentState.copy(items = updated)
                }
                RetrofitClient.publicationsApi.toggleSave(publicationId)
            } catch (e: Exception) {}
        }
    }

    fun toggleLike(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                if (currentState is CollectionDetailsState.Success) {
                    val updated = currentState.items.map { pub ->
                        if (pub.id == publicationId) {
                            val newIsLiked = !pub.isLiked
                            val newLikesCount = if (newIsLiked) pub.likesCount + 1 else pub.likesCount - 1
                            pub.copy(isLiked = newIsLiked, likesCount = newLikesCount)
                        } else pub
                    }
                    _state.value = currentState.copy(items = updated)
                }
                RetrofitClient.publicationsApi.toggleLike(publicationId)
            } catch (e: Exception) {}
        }
    }

    fun setAssessment(publicationId: String, color: Int, fit: Int, orig: Int, style: Int) {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                if (currentState is CollectionDetailsState.Success) {
                    val updated = currentState.items.map { pub ->
                        if (pub.id == publicationId) {
                            val newAvg = (color + fit + orig + style) / 4.0
                            pub.copy(averageAssessment = newAvg)
                        } else pub
                    }
                    _state.value = currentState.copy(items = updated)
                }
                val request = AssessmentRequest(color, fit, orig, style)
                RetrofitClient.publicationsApi.setAssessment(publicationId, request)
            } catch (e: Exception) {}
        }
    }

    fun loadComments(publicationId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.getComments(publicationId, null)
                if (response.isSuccessful) {
                    _comments.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    fun loadReplies(publicationId: String, parentCommentId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.getComments(publicationId, parentCommentId)
                if (response.isSuccessful) {
                    val newReplies = response.body() ?: emptyList()
                    val currentTree = _comments.value
                    _comments.value = insertRepliesIntoTree(currentTree, parentCommentId, newReplies)
                }
            } catch (e: Exception) {}
        }
    }

    private fun insertRepliesIntoTree(
        comments: List<CommentDTO>,
        targetParentId: String,
        newReplies: List<CommentDTO>
    ): List<CommentDTO> {
        return comments.map { comment ->
            if (comment.id == targetParentId) {
                comment.copy(replies = newReplies)
            } else if (comment.replies.isNotEmpty()) {
                comment.copy(replies = insertRepliesIntoTree(comment.replies, targetParentId, newReplies))
            } else {
                comment
            }
        }
    }

    fun postComment(publicationId: String, content: String, parentCommentId: String? = null) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.postComment(
                    publicationId,
                    CreateCommentRequest(content, parentCommentId)
                )
                if (response.isSuccessful) {
                    loadComments(publicationId)
                }
            } catch (e: Exception) {}
        }
    }

    fun deleteComment(publicationId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.deleteComment(publicationId, commentId)
                if (response.isSuccessful) {
                    loadComments(publicationId)
                }
            } catch (e: Exception) {}
        }
    }

    fun toggleCommentLike(publicationId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.toggleCommentLike(publicationId, commentId)
                if (response.isSuccessful) {
                    val currentTree = _comments.value
                    _comments.value = updateLikeInTree(currentTree, commentId)
                }
            } catch (e: Exception) {}
        }
    }

    private fun updateLikeInTree(comments: List<CommentDTO>, targetCommentId: String): List<CommentDTO> {
        return comments.map { comment ->
            if (comment.id == targetCommentId) {
                val newIsLiked = !comment.isLiked
                val newLikesCount = if (newIsLiked) comment.likesCount + 1 else comment.likesCount - 1
                comment.copy(isLiked = newIsLiked, likesCount = newLikesCount)
            } else if (comment.replies.isNotEmpty()) {
                comment.copy(replies = updateLikeInTree(comment.replies, targetCommentId))
            } else {
                comment
            }
        }
    }

    fun loadAssessmentsList(publicationId: String) {
        viewModelScope.launch {
            _isAssessmentsLoading.value = true
            _assessments.value = emptyList()
            try {
                val response = RetrofitClient.publicationsApi.getAssessmentsList(publicationId)
                if (response.isSuccessful) {
                    _assessments.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _isAssessmentsLoading.value = false
            }
        }
    }

    fun fetchAvatarsForPosts(posts: List<PublicationDTO>) {
        viewModelScope.launch {
            val currentMap = _userAvatars.value.toMutableMap()
            val uniqueUserIds = posts.mapNotNull { it.authorId }
                .distinct()
                .filter { !currentMap.containsKey(it) }

            uniqueUserIds.forEach { userId ->
                try {
                    val response = RetrofitClient.userApi.getUserProfile(userId)
                    if (response.isSuccessful) {
                        response.body()?.avatarUrl?.let { url ->
                            currentMap[userId] = url
                        }
                    }
                } catch (e: Exception) {}
            }
            _userAvatars.value = currentMap
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.collectionsApi.createCollection(CreateCollectionRequest(name))
                loadMyCollections()
            } catch (e: Exception) {}
        }
    }

    fun sendReport(targetId: String, targetType: String, reason: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.reportsApi.sendReport(CreateReportRequest(targetId, targetType, reason))
                if (response.isSuccessful) {
                    onComplete(null)
                } else {
                    if (response.code() == 409) onComplete("duplicate") else onComplete("error")
                }
            } catch (e: Exception) {
                onComplete("error")
            }
        }
    }
}
