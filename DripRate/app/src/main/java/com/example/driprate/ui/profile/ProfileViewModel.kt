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

    private var currentUserId: String? = null

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
                val response = RetrofitClient.publicationsApi.deletePublication(publicationId)
                if (response.isSuccessful) {
                    loadProfile(currentUserId, silent = true) // Reload
                } else if (response.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateProfile(username: String?, displayName: String?, bio: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val request = com.example.driprate.data.model.UpdateProfileRequest(
                    userName = username,
                    displayName = displayName,
                    bio = bio
                )
                val response = RetrofitClient.userApi.updateProfile(request)
                if (response.isSuccessful) {
                    loadProfile(currentUserId, silent = true)
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
                    loadProfile(currentUserId, silent = true)
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
                    android.util.Log.d("ProfileViewModel", "createCollection success, calling loadProfile")
                    loadProfile(currentUserId, silent = true)
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
                val response = RetrofitClient.collectionsApi.deleteCollection(collectionId)
                if (response.isSuccessful) {
                    loadProfile(currentUserId, silent = true)
                } else if (response.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {}
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.followUser(userId)
                if (response.isSuccessful) {
                    loadProfile(currentUserId, silent = true)
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
                    loadProfile(currentUserId, silent = true)
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
                    loadProfile(currentUserId, silent = true)
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
                    loadProfile(currentUserId, silent = true)
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
                val response = RetrofitClient.wardrobeApi.deleteWardrobeItem(id)
                if (response.isSuccessful) {
                    loadProfile(currentUserId, silent = true)
                    onComplete(true)
                } else {
                    if (response.code() == 401) TokenManager.clear()
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
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
}
