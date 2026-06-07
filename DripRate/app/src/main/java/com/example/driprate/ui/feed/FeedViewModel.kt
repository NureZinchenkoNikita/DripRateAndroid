package com.example.driprate.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.data.api.CollectionDTO
import com.example.driprate.data.api.CreateCollectionRequest
import com.example.driprate.data.model.AssessmentDTO
import com.example.driprate.data.model.AssessmentRequest
import com.example.driprate.data.model.CommentDTO
import com.example.driprate.data.model.CreateCommentRequest
import com.example.driprate.data.model.GlobalFeedResponse
import com.example.driprate.data.model.PublicationDTO
import com.example.driprate.data.model.CreateReportRequest
import com.example.driprate.data.model.FeedItem
import com.example.driprate.data.model.AdvertisementDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedState {
    object Loading : FeedState()
    data class Success(val items: List<FeedItem>) : FeedState()
    data class Error(val message: String) : FeedState()
}

class FeedViewModel : ViewModel() {
    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState

    private val _comments = MutableStateFlow<List<CommentDTO>>(emptyList())
    val comments: StateFlow<List<CommentDTO>> = _comments

    private val _myCollections = MutableStateFlow<List<CollectionDTO>>(emptyList())
    val myCollections = _myCollections.asStateFlow()

    private val _userAvatars = MutableStateFlow<Map<String, String>>(emptyMap())
    val userAvatars: StateFlow<Map<String, String>> = _userAvatars.asStateFlow()

    private var currentTab = 0
    private var selectedCollectionId: String? = null

    private val registeredAdIds = mutableSetOf<String>()

    init {
        loadMyCollections()
        loadMyProfile()
    }

    fun registerAdView(adId: String) {
        if (registeredAdIds.contains(adId)) return
        registeredAdIds.add(adId)
        viewModelScope.launch {
            try {
                RetrofitClient.advertisementsApi.registerView(adId)
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error registering ad view", e)
            }
        }
    }

    fun loadMyCollections() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.getMyCollections()
                if (response.isSuccessful) {
                    _myCollections.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error loading collections", e)
            }
        }
    }

    fun createCollection(name: String, isPublic: Boolean = false) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.createCollection(CreateCollectionRequest(name, isPublic))
                if (response.isSuccessful) {
                    loadMyCollections()
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error creating collection", e)
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
                } catch (e: Exception) {
                    android.util.Log.e("FeedViewModel", "Error fetching avatar for user $userId", e)
                }
            }
            _userAvatars.value = currentMap
        }
    }

    private val _assessments = MutableStateFlow<List<AssessmentDTO>>(emptyList())
    val assessments: StateFlow<List<AssessmentDTO>> = _assessments.asStateFlow()

    private val _isAssessmentsLoading = MutableStateFlow(false)
    val isAssessmentsLoading: StateFlow<Boolean> = _isAssessmentsLoading.asStateFlow()

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.deleteCollection(collectionId)
                if (response.isSuccessful) {
                    loadMyCollections()
                    if (currentTab == 4 && selectedCollectionId == collectionId) {
                        refreshCurrentFeed()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error deleting collection", e)
            }
        }
    }

    fun addToCollection(collectionId: String, publicationId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.addItemToCollection(collectionId, publicationId)
                if (response.isSuccessful) {
                    updatePublicationSaveStateLocally(publicationId, true)
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error adding to collection", e)
            }
        }
    }

    fun removeFromCollection(collectionId: String, publicationId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.removeItemFromCollection(collectionId, publicationId)
                if (response.isSuccessful) {
                    updatePublicationSaveStateLocally(publicationId, false)
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error removing from collection", e)
            }
        }
    }

    private fun updatePublicationSaveStateLocally(publicationId: String, isSaved: Boolean) {
        val currentState = _feedState.value
        if (currentState is FeedState.Success) {
            val updatedItems = currentState.items.map { item ->
                if (item is FeedItem.Publication && item.data.id == publicationId) {
                    FeedItem.Publication(item.data.copy(isSaved = isSaved))
                } else item
            }
            _feedState.value = FeedState.Success(updatedItems)
        }
    }

    fun toggleSave(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _feedState.value
                if (currentState is FeedState.Success) {
                    val updatedItems = currentState.items.map { item ->
                        if (item is FeedItem.Publication && item.data.id == publicationId) {
                            FeedItem.Publication(item.data.copy(isSaved = !item.data.isSaved))
                        } else item
                    }
                    _feedState.value = FeedState.Success(updatedItems)
                }

                val response = RetrofitClient.publicationsApi.toggleSave(publicationId)
                if (!response.isSuccessful) {
                    android.util.Log.e("FeedViewModel", "Error toggling save: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error toggling save", e)
            }
        }
    }

    fun toggleLike(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _feedState.value
                if (currentState is FeedState.Success) {
                    val updatedItems = currentState.items.map { item ->
                        if (item is FeedItem.Publication && item.data.id == publicationId) {
                            val newIsLiked = !item.data.isLiked
                            val newLikesCount = if (newIsLiked) item.data.likesCount + 1 else item.data.likesCount - 1
                            FeedItem.Publication(item.data.copy(isLiked = newIsLiked, likesCount = newLikesCount))
                        } else item
                    }
                    _feedState.value = FeedState.Success(updatedItems)
                }

                val response = RetrofitClient.publicationsApi.toggleLike(publicationId)
                if (!response.isSuccessful) {
                    android.util.Log.e("FeedViewModel", "Error toggling like: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error toggling like", e)
            }
        }
    }

    fun setAssessment(publicationId: String, color: Int, fit: Int, orig: Int, style: Int) {
        viewModelScope.launch {
            try {
                val currentState = _feedState.value
                if (currentState is FeedState.Success) {
                    val updatedItems = currentState.items.map { item ->
                        if (item is FeedItem.Publication && item.data.id == publicationId) {
                            val newAvg = (color + fit + orig + style) / 4.0
                            FeedItem.Publication(item.data.copy(averageAssessment = newAvg))
                        } else item
                    }
                    _feedState.value = FeedState.Success(updatedItems)
                }

                val request = AssessmentRequest(color, fit, orig, style)
                val response = RetrofitClient.publicationsApi.setAssessment(publicationId, request)
                if (!response.isSuccessful) {
                    android.util.Log.e("FeedViewModel", "Error setting assessment: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error setting assessment", e)
            }
        }
    }

    fun loadComments(publicationId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.getComments(publicationId, null)
                if (response.isSuccessful) {
                    _comments.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error loading root comments", e)
            }
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
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error loading replies", e)
            }
        }
    }

    private fun insertRepliesIntoTree(comments: List<CommentDTO>, targetParentId: String, newReplies: List<CommentDTO>): List<CommentDTO> {
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
                val response = RetrofitClient.publicationsApi.postComment(publicationId, CreateCommentRequest(content, parentCommentId))
                if (response.isSuccessful) {
                    loadComments(publicationId)
                    refreshCurrentFeed()
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error posting comment", e)
            }
        }
    }

    fun loadCollectionItems(collectionId: String) {
        currentTab = 4
        selectedCollectionId = collectionId
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            try {
                val response = RetrofitClient.collectionsApi.getCollectionItems(collectionId)
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    _feedState.value = FeedState.Success(items.map { FeedItem.Publication(it) })
                } else {
                    _feedState.value = FeedState.Error("Failed to load collection items")
                }
            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadGlobalFeed() {
        currentTab = 0
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            try {
                val response = RetrofitClient.feedApi.getGlobalFeed(0, 20)
                if (response.isSuccessful) {
                    val feedResponse = response.body()
                    _feedState.value = FeedState.Success(feedResponse?.toFeedItems() ?: emptyList())
                } else {
                    if (response.code() == 401) TokenManager.clear()
                    _feedState.value = FeedState.Error("Failed to load global feed: ${response.code()}")
                }
            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadSubscriptionsFeed() {
        currentTab = 1
        loadFeed { RetrofitClient.feedApi.getSubscriptionsFeed(0, 20) }
    }

    fun loadTopFeed() {
        currentTab = 2
        loadFeed { RetrofitClient.feedApi.getTopFeed(0, 20) }
    }

    fun loadUrgentFeed() {
        currentTab = 5
        loadFeed { RetrofitClient.feedApi.getUrgentFeed(0, 20) }
    }

    private fun refreshCurrentFeed() {
        when (currentTab) {
            0 -> loadGlobalFeed()
            1 -> loadSubscriptionsFeed()
            2 -> loadTopFeed()
            3 -> loadSavedFeed()
            4 -> selectedCollectionId?.let { loadCollectionItems(it) }
            5 -> loadUrgentFeed()
        }
    }

    fun loadSavedFeed() {
        currentTab = 3
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            try {
                val response = RetrofitClient.collectionsApi.getMyCollections()
                if (response.isSuccessful) {
                    val collections = response.body() ?: emptyList()
                    val savedCollection = collections.find { it.name.lowercase() == "saved" || it.name.lowercase() == "збережене" }
                    if (savedCollection != null) {
                        val itemsResponse = RetrofitClient.collectionsApi.getCollectionItems(savedCollection.id)
                        if (itemsResponse.isSuccessful) {
                            val items = itemsResponse.body() ?: emptyList()
                            _feedState.value = FeedState.Success(items.map { FeedItem.Publication(it) })
                        } else {
                            _feedState.value = FeedState.Error("Failed to load saved items")
                        }
                    } else {
                        _feedState.value = FeedState.Success(emptyList())
                    }
                } else {
                    _feedState.value = FeedState.Error("Failed to load collections")
                }
            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadFeed(apiCall: suspend () -> retrofit2.Response<List<PublicationDTO>>) {
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val publications = response.body() ?: emptyList()
                    _feedState.value = FeedState.Success(publications.map { FeedItem.Publication(it) })
                } else {
                    if (response.code() == 401) TokenManager.clear()
                    _feedState.value = FeedState.Error("Failed to load feed: ${response.code()}")
                }
            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private val _myUserId = MutableStateFlow<String?>(null)
    val myUserId: StateFlow<String?> = _myUserId.asStateFlow()

    private fun loadMyProfile() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.getMyProfile()
                if (response.isSuccessful) {
                    _myUserId.value = response.body()?.id
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error loading my profile", e)
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
                android.util.Log.e("FeedViewModel", "Error loading assessments", e)
            } finally {
                _isAssessmentsLoading.value = false
            }
        }
    }

    fun deleteComment(publicationId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.deleteComment(publicationId, commentId)
                if (response.isSuccessful) {
                    loadComments(publicationId)
                    refreshCurrentFeed()
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error deleting comment", e)
            }
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
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error toggling comment like", e)
            }
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
                android.util.Log.e("FeedViewModel", "Error sending report", e)
                onComplete("error")
            }
        }
    }
}
