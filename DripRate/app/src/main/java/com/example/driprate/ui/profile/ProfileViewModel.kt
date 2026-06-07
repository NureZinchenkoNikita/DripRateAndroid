package com.example.driprate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.data.model.PublicationDTO
import com.example.driprate.data.model.UserDTO
import com.example.driprate.data.model.WardrobeItemDTO
import com.example.driprate.data.model.TagDTO
import com.example.driprate.data.model.CreateReportRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import com.example.driprate.data.model.AssessmentRequest
import com.example.driprate.data.model.CommentDTO
import com.example.driprate.data.model.CreateCommentRequest
import com.example.driprate.data.api.CollectionDTO
import com.example.driprate.data.api.CreateCollectionRequest
import com.example.driprate.data.model.AssessmentDTO

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(
        val user: UserDTO,
        val posts: List<PublicationDTO>,
        val collections: List<com.example.driprate.data.api.CollectionDTO>,
        val wardrobe: List<WardrobeItemDTO>,
        val isOwnProfile: Boolean
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

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

    private val _myCollections = MutableStateFlow<List<com.example.driprate.data.api.CollectionDTO>>(emptyList())
    val myCollections: StateFlow<List<com.example.driprate.data.api.CollectionDTO>> = _myCollections.asStateFlow()

    private var currentUserId: String? = null

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
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading my profile", e)
            }
        }
    }

    fun loadProfile(userId: String? = null, silent: Boolean = false) {
        android.util.Log.d("ProfileViewModel", "loadProfile: userId=$userId, currentUserId=$currentUserId, silent=$silent")
        currentUserId = userId
        viewModelScope.launch {
            android.util.Log.d("ProfileViewModel", "loadProfile coroutine started")
            if (!silent) {
                _profileState.value = ProfileState.Loading
            }
            try {
                val userResponse = if (userId == null) {
                    RetrofitClient.userApi.getMyProfile()
                } else {
                    RetrofitClient.userApi.getUserProfile(userId)
                }

                if (userResponse.isSuccessful) {
                    val user = userResponse.body()!!
                    
                    var isOwn = userId == null
                    try {
                        val myProfileResponse = RetrofitClient.userApi.getMyProfile()
                        if (myProfileResponse.isSuccessful) {
                            val myId = myProfileResponse.body()?.id
                            isOwn = userId == null || (myId != null && user.id == myId)
                        } else if (myProfileResponse.code() == 401) {
                            TokenManager.clear()
                            return@launch
                        }
                    } catch (e: Exception) {
                        // If my profile check fails, fallback to userId == null logic
                    }

                    val postsResponse = RetrofitClient.feedApi.getUserFeed(user.id, 0, 50)
                    val posts = if (postsResponse.isSuccessful) {
                        // Повертаємо як було: просто body()
                        postsResponse.body() ?: emptyList()
                    } else {
                        if (postsResponse.code() == 401) TokenManager.clear()
                        emptyList()
                    }

                    var collections: List<com.example.driprate.data.api.CollectionDTO> = emptyList()
                    val collectionsResponse = if (isOwn) {
                        RetrofitClient.collectionsApi.getMyCollections()
                    } else {
                        RetrofitClient.collectionsApi.getUserCollections(user.id)
                    }

                    if (collectionsResponse.isSuccessful) {
                        collections = collectionsResponse.body() ?: emptyList()
                    } else if (collectionsResponse.code() == 401) {
                        TokenManager.clear()
                    }

                    val wardrobeResponse = RetrofitClient.wardrobeApi.getWardrobe(user.id)
                    val wardrobe = if (wardrobeResponse.isSuccessful) {
                        wardrobeResponse.body() ?: emptyList()
                    } else {
                        if (wardrobeResponse.code() == 401) TokenManager.clear()
                        emptyList()
                    }

                    _profileState.value = ProfileState.Success(user, posts, collections, wardrobe, isOwn)
                } else {
                    if (userResponse.code() == 401) {
                        TokenManager.clear()
                    }
                    if (!silent) {
                        _profileState.value = ProfileState.Error("Error: ${userResponse.code()}")
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
                } else {
                    android.util.Log.e("ProfileViewModel", "Silent refresh failed", e)
                }
            }
        }
    }

    fun deletePost(publicationId: String, userId: String?) {
        viewModelScope.launch {
            try {
                // Optimistically remove post
                val currentState = _profileState.value
                if (currentState is ProfileState.Success) {
                    _profileState.value = currentState.copy(
                        posts = currentState.posts.filter { it.id != publicationId }
                    )
                }
                val response = RetrofitClient.publicationsApi.deletePublication(publicationId)
                if (response.isSuccessful) {
                    refreshPostsOnly()
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    } else {
                        loadProfile(currentUserId, silent = true)
                    }
                }
            } catch (e: Exception) {
                loadProfile(currentUserId, silent = true)
            }
        }
    }

    fun updateProfile(displayName: String?, bio: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val request = com.example.driprate.data.model.UpdateProfileRequest(
                    displayName = displayName,
                    bio = bio
                )
                val response = RetrofitClient.userApi.updateProfile(request)
                if (response.isSuccessful) {
                    refreshUserOnly()
                    onComplete(true)
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    }
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun changePassword(old: String, new: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val request = com.example.driprate.data.model.ChangePasswordRequest(old, new)
                val response = RetrofitClient.userApi.changePassword(request)
                if (response.code() == 401) {
                    TokenManager.clear()
                }
                onComplete(response.isSuccessful)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun updateAvatar(avatarPart: okhttp3.MultipartBody.Part, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.updateAvatar(avatarPart)
                if (response.isSuccessful) {
                    refreshUserOnly()
                    onComplete(true)
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    }
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun createCollection(name: String) {
        android.util.Log.d("ProfileViewModel", "createCollection: name=$name, currentUserId=$currentUserId")
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.createCollection(
                    com.example.driprate.data.api.CreateCollectionRequest(name)
                )
                android.util.Log.d("ProfileViewModel", "createCollection response code: ${response.code()}")
                if (response.isSuccessful) {
                    val newId = response.body() ?: ""
                    val currentState = _profileState.value
                    if (currentState is ProfileState.Success && newId.isNotEmpty()) {
                        val newCollection = com.example.driprate.data.api.CollectionDTO(
                            id = newId,
                            name = name,
                            isPublic = false,
                            itemsCount = 0
                        )
                        _profileState.value = currentState.copy(
                            collections = currentState.collections + newCollection
                        )
                    }
                    refreshCollectionsOnly()
                } else if (response.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error creating collection", e)
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                // Optimistically remove collection
                val currentState = _profileState.value
                if (currentState is ProfileState.Success) {
                    _profileState.value = currentState.copy(
                        collections = currentState.collections.filter { it.id != collectionId }
                    )
                }
                val response = RetrofitClient.collectionsApi.deleteCollection(collectionId)
                if (response.isSuccessful) {
                    refreshCollectionsOnly()
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    } else {
                        loadProfile(currentUserId, silent = true)
                    }
                }
            } catch (e: Exception) {
                loadProfile(currentUserId, silent = true)
            }
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.followUser(userId)
                if (response.isSuccessful) {
                    refreshUserOnly()
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
                    refreshUserOnly()
                } else if (response.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {}
        }
    }

    fun logout(onLogout: () -> Unit) {
        TokenManager.clear()
        onLogout()
    }

    fun addWardrobeItem(
        name: String,
        brand: String?,
        storeLink: String?,
        price: Double?,
        photoPart: okhttp3.MultipartBody.Part?,
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val brandPart = brand?.toRequestBody("text/plain".toMediaTypeOrNull())
                val storeLinkPart = storeLink?.toRequestBody("text/plain".toMediaTypeOrNull())
                val pricePart = price?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.wardrobeApi.addWardrobeItem(
                    name = namePart,
                    brand = brandPart,
                    storeLink = storeLinkPart,
                    estimatedPrice = pricePart,
                    photo = photoPart
                )
                if (response.isSuccessful) {
                    refreshWardrobeOnly()
                    onComplete(null)
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                        onComplete("Unauthorized")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        onComplete(parseErrorMessage(errorBody))
                    }
                }
            } catch (e: Exception) {
                onComplete(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun updateWardrobeItem(
        id: String,
        name: String,
        brand: String?,
        storeLink: String?,
        price: Double?,
        photoPart: okhttp3.MultipartBody.Part?,
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val brandPart = brand?.toRequestBody("text/plain".toMediaTypeOrNull())
                val storeLinkPart = storeLink?.toRequestBody("text/plain".toMediaTypeOrNull())
                val pricePart = price?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.wardrobeApi.updateWardrobeItem(
                    id = id,
                    name = namePart,
                    brand = brandPart,
                    storeLink = storeLinkPart,
                    estimatedPrice = pricePart,
                    photo = photoPart
                )
                if (response.isSuccessful) {
                    refreshWardrobeOnly()
                    onComplete(null)
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                        onComplete("Unauthorized")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        onComplete(parseErrorMessage(errorBody))
                    }
                }
            } catch (e: Exception) {
                onComplete(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Server returned an error"
        // Find all occurrences of "message" or "title" fields inside the JSON string
        val messageRegex = """"(?:message|title)":\s*"([^"]+)"""".toRegex()
        val matches = messageRegex.findAll(errorBody)
        if (matches.any()) {
            val messages = matches.map { it.groupValues[1] }
                .filter { it != "One or more validation errors occurred." } // Filter out generic validation title
                .toList()
            if (messages.isNotEmpty()) {
                return messages.joinToString("\n")
            }
        }
        return "Failed to save wardrobe item. Please check your inputs (e.g., storefront link should be a valid absolute URL)."
    }

    fun deleteWardrobeItem(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Optimistically remove
                val currentState = _profileState.value
                if (currentState is ProfileState.Success) {
                    _profileState.value = currentState.copy(
                        wardrobe = currentState.wardrobe.filter { it.id != id }
                    )
                }
                val response = RetrofitClient.wardrobeApi.deleteWardrobeItem(id)
                if (response.isSuccessful) {
                    refreshWardrobeOnly()
                    onComplete(true)
                } else {
                    if (response.code() == 401) TokenManager.clear()
                    loadProfile(currentUserId, silent = true)
                    onComplete(false)
                }
            } catch (e: Exception) {
                loadProfile(currentUserId, silent = true)
                onComplete(false)
            }
        }
    }

    private fun refreshCollectionsOnly() {
        val currentState = _profileState.value
        if (currentState !is ProfileState.Success) return
        viewModelScope.launch {
            try {
                val collectionsResponse = if (currentState.isOwnProfile) {
                    RetrofitClient.collectionsApi.getMyCollections()
                } else {
                    RetrofitClient.collectionsApi.getUserCollections(currentState.user.id)
                }
                if (collectionsResponse.isSuccessful) {
                    val newCollections = collectionsResponse.body() ?: emptyList()
                    val latestState = _profileState.value
                    if (latestState is ProfileState.Success) {
                        _profileState.value = latestState.copy(collections = newCollections)
                    }
                } else if (collectionsResponse.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error refreshing collections", e)
            }
        }
    }

    private fun refreshPostsOnly() {
        val currentState = _profileState.value
        if (currentState !is ProfileState.Success) return
        viewModelScope.launch {
            try {
                val postsResponse = RetrofitClient.feedApi.getUserFeed(currentState.user.id, 0, 50)
                if (postsResponse.isSuccessful) {
                    val newPosts = postsResponse.body() ?: emptyList()
                    val latestState = _profileState.value
                    if (latestState is ProfileState.Success) {
                        _profileState.value = latestState.copy(posts = newPosts)
                    }
                } else if (postsResponse.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error refreshing posts", e)
            }
        }
    }

    private fun refreshWardrobeOnly() {
        val currentState = _profileState.value
        if (currentState !is ProfileState.Success) return
        viewModelScope.launch {
            try {
                val wardrobeResponse = RetrofitClient.wardrobeApi.getWardrobe(currentState.user.id)
                if (wardrobeResponse.isSuccessful) {
                    val newWardrobe = wardrobeResponse.body() ?: emptyList()
                    val latestState = _profileState.value
                    if (latestState is ProfileState.Success) {
                        _profileState.value = latestState.copy(wardrobe = newWardrobe)
                    }
                } else if (wardrobeResponse.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error refreshing wardrobe", e)
            }
        }
    }

    private fun refreshUserOnly() {
        val currentState = _profileState.value
        if (currentState !is ProfileState.Success) return
        viewModelScope.launch {
            try {
                val userResponse = if (currentUserId == null) {
                    RetrofitClient.userApi.getMyProfile()
                } else {
                    RetrofitClient.userApi.getUserProfile(currentUserId!!)
                }
                if (userResponse.isSuccessful) {
                    val newUser = userResponse.body()!!
                    val latestState = _profileState.value
                    if (latestState is ProfileState.Success) {
                        _profileState.value = latestState.copy(user = newUser)
                    }
                } else if (userResponse.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error refreshing user profile", e)
            }
        }
    }

    private val _followersList = MutableStateFlow<List<UserDTO>>(emptyList())
    val followersList: StateFlow<List<UserDTO>> = _followersList.asStateFlow()

    private val _followingList = MutableStateFlow<List<UserDTO>>(emptyList())
    val followingList: StateFlow<List<UserDTO>> = _followingList.asStateFlow()

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.getFollowers(userId)
                if (response.isSuccessful) {
                    _followersList.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading followers", e)
            }
        }
    }

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.getFollowing(userId)
                if (response.isSuccessful) {
                    _followingList.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading following", e)
            }
        }
    }

    private val _allTags = MutableStateFlow<List<TagDTO>>(emptyList())
    val allTags: StateFlow<List<TagDTO>> = _allTags.asStateFlow()

    private val _preferredTags = MutableStateFlow<List<TagDTO>>(emptyList())
    val preferredTags: StateFlow<List<TagDTO>> = _preferredTags.asStateFlow()

    fun loadPreferences() {
        viewModelScope.launch {
            try {
                val tagsResp = RetrofitClient.metaApi.getTags()
                val prefResp = RetrofitClient.userApi.getMyPreferences()
                if (tagsResp.isSuccessful && prefResp.isSuccessful) {
                    _allTags.value = tagsResp.body() ?: emptyList()
                    _preferredTags.value = prefResp.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading preferences", e)
            }
        }
    }

    fun savePreferences(tagIds: List<String>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.updatePreferences(tagIds)
                if (response.isSuccessful) {
                    loadPreferences()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
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
                    if (response.code() == 409) {
                        onComplete("duplicate")
                    } else {
                        onComplete("error")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error sending report", e)
                onComplete("error")
            }
        }
    }

    // --- Interactive publication features mirrored from FeedViewModel ---

    fun loadMyCollections() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.getMyCollections()
                if (response.isSuccessful) {
                    _myCollections.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading collections", e)
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
                android.util.Log.e("ProfileViewModel", "Error adding to collection", e)
            }
        }
    }

    private fun updatePublicationSaveStateLocally(publicationId: String, isSaved: Boolean) {
        val currentState = _profileState.value
        if (currentState is ProfileState.Success) {
            val updated = currentState.posts.map { pub ->
                if (pub.id == publicationId) pub.copy(isSaved = isSaved) else pub
            }
            _profileState.value = currentState.copy(posts = updated)
        }
    }

    fun toggleSave(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _profileState.value
                if (currentState is ProfileState.Success) {
                    val updated = currentState.posts.map { pub ->
                        if (pub.id == publicationId) pub.copy(isSaved = !pub.isSaved) else pub
                    }
                    _profileState.value = currentState.copy(posts = updated)
                }

                val response = RetrofitClient.publicationsApi.toggleSave(publicationId)
                if (!response.isSuccessful) {
                    android.util.Log.e("ProfileViewModel", "Error toggling save: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling save", e)
            }
        }
    }

    fun toggleLike(publicationId: String) {
        viewModelScope.launch {
            try {
                val currentState = _profileState.value
                if (currentState is ProfileState.Success) {
                    val updated = currentState.posts.map { pub ->
                        if (pub.id == publicationId) {
                            val newIsLiked = !pub.isLiked
                            val newLikesCount = if (newIsLiked) pub.likesCount + 1 else pub.likesCount - 1
                            pub.copy(isLiked = newIsLiked, likesCount = newLikesCount)
                        } else {
                            pub
                        }
                    }
                    _profileState.value = currentState.copy(posts = updated)
                }

                val response = RetrofitClient.publicationsApi.toggleLike(publicationId)
                if (!response.isSuccessful) {
                    android.util.Log.e("ProfileViewModel", "Error toggling like: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling like", e)
            }
        }
    }

    fun setAssessment(publicationId: String, color: Int, fit: Int, orig: Int, style: Int) {
        viewModelScope.launch {
            try {
                val currentState = _profileState.value
                if (currentState is ProfileState.Success) {
                    val updated = currentState.posts.map { pub ->
                        if (pub.id == publicationId) {
                            val newAvg = (color + fit + orig + style) / 4.0
                            pub.copy(averageAssessment = newAvg)
                        } else pub
                    }
                    _profileState.value = currentState.copy(posts = updated)
                }

                val request = AssessmentRequest(color, fit, orig, style)
                val response = RetrofitClient.publicationsApi.setAssessment(publicationId, request)
                if (!response.isSuccessful) {
                    android.util.Log.e("ProfileViewModel", "Error setting assessment: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error setting assessment", e)
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
                android.util.Log.e("ProfileViewModel", "Error loading root comments", e)
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
                android.util.Log.e("ProfileViewModel", "Error loading replies", e)
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
                    refreshPostsOnly()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error posting comment", e)
            }
        }
    }

    fun deleteComment(publicationId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.deleteComment(publicationId, commentId)
                if (response.isSuccessful) {
                    loadComments(publicationId)
                    refreshPostsOnly()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error deleting comment", e)
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
                android.util.Log.e("ProfileViewModel", "Error toggling comment like", e)
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
                android.util.Log.e("ProfileViewModel", "Error loading assessments", e)
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
                    android.util.Log.e("ProfileViewModel", "Error fetching avatar for user $userId", e)
                }
            }
            _userAvatars.value = currentMap
        }
    }
}
