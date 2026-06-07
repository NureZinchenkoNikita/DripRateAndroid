package com.example.driprate.ui.search

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
import com.example.driprate.data.model.CreateReportRequest
import com.example.driprate.data.model.PublicationDTO
import com.example.driprate.data.model.UserDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Loading : SearchResultState()
    data class Success(val publications: List<PublicationDTO>, val users: List<UserDTO>) : SearchResultState()
    data class Error(val message: String) : SearchResultState()
}

class SearchViewModel : ViewModel() {
    private val _searchState = MutableStateFlow<SearchResultState>(SearchResultState.Idle)
    val searchState: StateFlow<SearchResultState> = _searchState

    // States for interactive details inside search dialogs
    private val _comments = MutableStateFlow<List<CommentDTO>>(emptyList())
    val comments: StateFlow<List<CommentDTO>> = _comments

    private val _myCollections = MutableStateFlow<List<CollectionDTO>>(emptyList())
    val myCollections = _myCollections.asStateFlow()

    private val _userAvatars = MutableStateFlow<Map<String, String>>(emptyMap())
    val userAvatars: StateFlow<Map<String, String>> = _userAvatars.asStateFlow()

    private val _assessments = MutableStateFlow<List<AssessmentDTO>>(emptyList())
    val assessments: StateFlow<List<AssessmentDTO>> = _assessments.asStateFlow()

    private val _isAssessmentsLoading = MutableStateFlow(false)
    val isAssessmentsLoading: StateFlow<Boolean> = _isAssessmentsLoading.asStateFlow()

    private val _myUserId = MutableStateFlow<String?>(null)
    val myUserId: StateFlow<String?> = _myUserId.asStateFlow()

    init {
        loadMyCollections()
        loadMyProfile()
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchState.value = SearchResultState.Idle
            return
        }

        viewModelScope.launch {
            _searchState.value = SearchResultState.Loading
            try {
                val pubResponse = RetrofitClient.searchApi.searchPublications(query)
                val userResponse = RetrofitClient.searchApi.searchUsers(query)

                if (pubResponse.isSuccessful && userResponse.isSuccessful) {
                    val publications = pubResponse.body() ?: emptyList()
                    _searchState.value = SearchResultState.Success(
                        publications = publications,
                        users = userResponse.body() ?: emptyList()
                    )
                    // Prefetch avatars for loaded publications
                    fetchAvatarsForPosts(publications)
                } else {
                    if (pubResponse.code() == 401 || userResponse.code() == 401) {
                        TokenManager.clear()
                    }
                    _searchState.value = SearchResultState.Error("Search failed")
                }
            } catch (e: Exception) {
                _searchState.value = SearchResultState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.followUser(userId)
                if (response.isSuccessful) {
                    val currentState = _searchState.value
                    if (currentState is SearchResultState.Success) {
                        val updatedUsers = currentState.users.map {
                            if (it.id == userId) it.copy(isFollowing = true) else it
                        }
                        _searchState.value = currentState.copy(users = updatedUsers)
                    }
                } else if (response.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {}
        }
    }

    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.unfollowUser(userId)
                if (response.isSuccessful) {
                    val currentState = _searchState.value
                    if (currentState is SearchResultState.Success) {
                        val updatedUsers = currentState.users.map {
                            if (it.id == userId) it.copy(isFollowing = false) else it
                        }
                        _searchState.value = currentState.copy(users = updatedUsers)
                    }
                } else if (response.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {}
        }
    }

    // --- Interactive publication features mirrored from FeedViewModel ---

    private fun loadMyProfile() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.getMyProfile()
                if (response.isSuccessful) {
                    _myUserId.value = response.body()?.id
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error loading my profile", e)
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
                android.util.Log.e("SearchViewModel", "Error loading collections", e)
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
                android.util.Log.e("SearchViewModel", "Error creating collection", e)
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
                android.util.Log.e("SearchViewModel", "Error adding to collection", e)
            }
        }
    }

    private fun updatePublicationSaveStateLocally(publicationId: String, isSaved: Boolean) {
        val currentState = _searchState.value
        if (currentState is SearchResultState.Success) {
            val updated = currentState.publications.map { pub ->
                if (pub.id == publicationId) pub.copy(isSaved = isSaved) else pub
            }
            _searchState.value = currentState.copy(publications = updated)
        }
    }

    fun toggleSave(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _searchState.value
                if (currentState is SearchResultState.Success) {
                    val updated = currentState.publications.map { pub ->
                        if (pub.id == publicationId) pub.copy(isSaved = !pub.isSaved) else pub
                    }
                    _searchState.value = currentState.copy(publications = updated)
                }

                val response = RetrofitClient.publicationsApi.toggleSave(publicationId)
                if (!response.isSuccessful) {
                    android.util.Log.e("SearchViewModel", "Error toggling save: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error toggling save", e)
            }
        }
    }

    fun toggleLike(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _searchState.value
                if (currentState is SearchResultState.Success) {
                    val updated = currentState.publications.map { pub ->
                        if (pub.id == publicationId) {
                            val newIsLiked = !pub.isLiked
                            val newLikesCount = if (newIsLiked) pub.likesCount + 1 else pub.likesCount - 1
                            pub.copy(isLiked = newIsLiked, likesCount = newLikesCount)
                        } else {
                            pub
                        }
                    }
                    _searchState.value = currentState.copy(publications = updated)
                }

                val response = RetrofitClient.publicationsApi.toggleLike(publicationId)
                if (!response.isSuccessful) {
                    android.util.Log.e("SearchViewModel", "Error toggling like: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error toggling like", e)
            }
        }
    }

    fun setAssessment(publicationId: String, color: Int, fit: Int, orig: Int, style: Int) {
        viewModelScope.launch {
            try {
                val currentState = _searchState.value
                if (currentState is SearchResultState.Success) {
                    val updated = currentState.publications.map { pub ->
                        if (pub.id == publicationId) {
                            val newAvg = (color + fit + orig + style) / 4.0
                            pub.copy(averageAssessment = newAvg)
                        } else pub
                    }
                    _searchState.value = currentState.copy(publications = updated)
                }

                val request = AssessmentRequest(color, fit, orig, style)
                val response = RetrofitClient.publicationsApi.setAssessment(publicationId, request)
                if (!response.isSuccessful) {
                    android.util.Log.e("SearchViewModel", "Error setting assessment: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error setting assessment", e)
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
                android.util.Log.e("SearchViewModel", "Error loading root comments", e)
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
                android.util.Log.e("SearchViewModel", "Error loading replies", e)
            }
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
                    val currentState = _searchState.value
                    if (currentState is SearchResultState.Success) {
                        val updated = currentState.publications.map { pub ->
                            if (pub.id == publicationId) pub.copy(commentsCount = pub.commentsCount + 1) else pub
                        }
                        _searchState.value = currentState.copy(publications = updated)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error posting comment", e)
            }
        }
    }

    fun deleteComment(publicationId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.deleteComment(publicationId, commentId)
                if (response.isSuccessful) {
                    loadComments(publicationId)
                    val currentState = _searchState.value
                    if (currentState is SearchResultState.Success) {
                        val updated = currentState.publications.map { pub ->
                            if (pub.id == publicationId) pub.copy(commentsCount = (pub.commentsCount - 1).coerceAtLeast(0)) else pub
                        }
                        _searchState.value = currentState.copy(publications = updated)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error deleting comment", e)
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
                android.util.Log.e("SearchViewModel", "Error toggling comment like", e)
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
                android.util.Log.e("SearchViewModel", "Error loading assessments", e)
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
                } catch (e: Exception) {
                    android.util.Log.e("SearchViewModel", "Error fetching avatar for user $userId", e)
                }
            }
            _userAvatars.value = currentMap
        }
    }

    fun sendReport(targetId: String, targetType: String, reason: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.reportsApi.sendReport(CreateReportRequest(targetId, targetType, reason))
                if (response.isSuccessful) {
                    onComplete(null)
                } else {
                    if (response.code() == 409) {
                        onComplete("duplicate")
                    } else {
                        onComplete("error")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error sending report", e)
                onComplete("error")
            }
        }
    }
}
