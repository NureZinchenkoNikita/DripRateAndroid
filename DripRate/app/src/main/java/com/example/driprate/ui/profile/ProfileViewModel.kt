package com.example.driprate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.data.model.PublicationDTO
import com.example.driprate.data.model.UserDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(
        val user: UserDTO,
        val posts: List<PublicationDTO>,
        val collections: List<com.example.driprate.data.api.CollectionDTO>,
        val isOwnProfile: Boolean
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    fun loadProfile(userId: String? = null) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
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
                    if (isOwn) {
                        val collectionsResponse = RetrofitClient.collectionsApi.getMyCollections()
                        if (collectionsResponse.isSuccessful) {
                            collections = collectionsResponse.body() ?: emptyList()
                        } else if (collectionsResponse.code() == 401) {
                            TokenManager.clear()
                        }
                    }

                    _profileState.value = ProfileState.Success(user, posts, collections, isOwn)
                } else {
                    if (userResponse.code() == 401) {
                        TokenManager.clear()
                    }
                    _profileState.value = ProfileState.Error("Error: ${userResponse.code()}")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deletePost(publicationId: String, userId: String?) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.deletePublication(publicationId)
                if (response.isSuccessful) {
                    loadProfile(userId) // Reload
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
                    loadProfile()
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
                    loadProfile()
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
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.createCollection(
                    com.example.driprate.data.api.CreateCollectionRequest(name)
                )
                if (response.isSuccessful) {
                    loadProfile()
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
                    loadProfile()
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
                    loadProfile(userId)
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
                    loadProfile(userId)
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
}
