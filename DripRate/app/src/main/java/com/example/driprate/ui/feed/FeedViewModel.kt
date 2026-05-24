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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedState {
    object Loading : FeedState()
    data class Success(val items: List<PublicationDTO>) : FeedState()
    data class Error(val message: String) : FeedState()
}

class FeedViewModel : ViewModel() {
    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState

    // Стан для коментарів конкретного поста
    private val _comments = MutableStateFlow<List<CommentDTO>>(emptyList())
    val comments: StateFlow<List<CommentDTO>> = _comments

    private val _myCollections = MutableStateFlow<List<CollectionDTO>>(emptyList())
    val myCollections = _myCollections.asStateFlow()

    private val _userAvatars = MutableStateFlow<Map<String, String>>(emptyMap())
    val userAvatars: StateFlow<Map<String, String>> = _userAvatars.asStateFlow()

    private var currentTab = 0

    init {
        loadMyCollections()
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

            // Беремо ID авторів, для яких ми ще НЕ завантажували аватарку в цьому сеансі
            val uniqueUserIds = posts.mapNotNull { it.authorId }
                .distinct()
                .filter { !currentMap.containsKey(it) }

            uniqueUserIds.forEach { userId ->
                try {
                    // Викликаємо твій чинний API запит профілю
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
            // Оновлюємо стейт, UI відразу це побачить
            _userAvatars.value = currentMap
        }
    }
    private val _assessments = MutableStateFlow<List<AssessmentDTO>>(emptyList())
    val assessments: StateFlow<List<AssessmentDTO>> = _assessments.asStateFlow()




    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.deleteCollection(collectionId)
                if (response.isSuccessful) {
                    loadMyCollections()
                    if (currentTab == 4 && selectedCollectionId == collectionId) {
                        loadGlobalFeed()
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
                    updatePublicationSaveStateLocally(publicationId, true) // Оновлюємо без перезавантаження
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
                    updatePublicationSaveStateLocally(publicationId, false) // Оновлюємо без перезавантаження
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error removing from collection", e)
            }
        }
    }
    private fun updatePublicationSaveStateLocally(publicationId: String, isSaved: Boolean) {
        val currentState = _feedState.value
        if (currentState is FeedState.Success) {
            val updatedItems = currentState.items.map { pub ->
                if (pub.id == publicationId) pub.copy(isSaved = isSaved) else pub
            }
            _feedState.value = FeedState.Success(updatedItems)
        }
    }
    fun toggleSave(publicationId: String) {
        viewModelScope.launch {
            try {
                // 1. ОПТИМІСТИЧНЕ ОНОВЛЕННЯ: Миттєво міняємо стан закладинки
                val currentState = _feedState.value
                if (currentState is FeedState.Success) {
                    val updatedItems = currentState.items.map { pub ->
                        if (pub.id == publicationId) pub.copy(isSaved = !pub.isSaved) else pub
                    }
                    _feedState.value = FeedState.Success(updatedItems)
                }

                // 2. ФОНОВИЙ ЗАПИТ: Відправляємо швидке збереження на бекенд
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
                // 1. ОПТИМІСТИЧНЕ ОНОВЛЕННЯ: Миттєво міняємо стан у локальному списку, щоб не було мигання
                val currentState = _feedState.value
                if (currentState is FeedState.Success) {
                    val updatedItems = currentState.items.map { pub ->
                        if (pub.id == publicationId) {
                            val newIsLiked = !pub.isLiked
                            val newLikesCount = if (newIsLiked) pub.likesCount + 1 else pub.likesCount - 1
                            pub.copy(isLiked = newIsLiked, likesCount = newLikesCount)
                        } else {
                            pub
                        }
                    }
                    _feedState.value = FeedState.Success(updatedItems)
                }

                // 2. ФОНОВИЙ ЗАПИТ: Відправляємо лайк на бекенд
                val response = RetrofitClient.publicationsApi.toggleLike(publicationId)
                if (!response.isSuccessful) {
                    android.util.Log.e("FeedViewModel", "Error toggling like: ${response.code()}")
                    // За бажанням тут можна відкотити лайк назад, якщо сталася помилка сервера
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error toggling like", e)
            }
        }
    }

    fun setAssessment(publicationId: String,
                      color: Int,
                      fit: Int,
                      orig: Int,
                      style: Int) {
        viewModelScope.launch {
            try {
                // 1. ОПТИМІСТИЧНЕ ОНОВЛЕННЯ (Миттєва зміна на екрані без блимання)
                val currentState = _feedState.value
                if (currentState is FeedState.Success) {
                    val updatedItems = currentState.items.map { pub ->
                        if (pub.id == publicationId) {
                            // Вираховуємо середнє значення для краси
                            val newAvg = (color + fit + orig + style) / 4.0
                            pub.copy(averageAssessment = newAvg)
                        } else pub
                    }
                    _feedState.value = FeedState.Success(updatedItems)
                }

                // 2. ФОНОВИЙ ЗАПИТ НА СЕРВЕР (без виклику refreshCurrentFeed!)
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
                // Викликаємо API без parentCommentId
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
                // Просимо бекенд дати відповіді саме для цього parentCommentId
                val response = RetrofitClient.publicationsApi.getComments(publicationId, parentCommentId)
                if (response.isSuccessful) {
                    val newReplies = response.body() ?: emptyList()

                    // Оновлюємо наше дерево коментарів
                    val currentTree = _comments.value
                    _comments.value = insertRepliesIntoTree(currentTree, parentCommentId, newReplies)
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error loading replies", e)
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
                // Знайшли батька! Додаємо йому відповіді
                comment.copy(replies = newReplies)
            } else if (comment.replies.isNotEmpty()) {
                // Шукаємо глибше
                comment.copy(replies = insertRepliesIntoTree(comment.replies, targetParentId, newReplies))
            } else {
                comment
            }
        }
    }

    fun postComment(publicationId: String, content: String, parentCommentId: String? = null) {
        viewModelScope.launch {
            try {
                // Передаємо parentCommentId у запит
                val response = RetrofitClient.publicationsApi.postComment(
                    publicationId,
                    CreateCommentRequest(content, parentCommentId)
                )
                if (response.isSuccessful) {
                    loadComments(publicationId)
                    refreshCurrentFeed()
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error posting comment", e)
            }
        }
    }


    private var selectedCollectionId: String? = null

    fun loadCollectionItems(collectionId: String) {
        currentTab = 4
        selectedCollectionId = collectionId
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            try {
                val response = RetrofitClient.collectionsApi.getCollectionItems(collectionId)
                if (response.isSuccessful) {
                    _feedState.value = FeedState.Success(response.body() ?: emptyList())
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
                    // ТІЛЬКИ ДЛЯ ГЛОБАЛЬНОЇ дістаємо з .publications
                    _feedState.value = FeedState.Success(response.body()?.publications ?: emptyList())
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
                            _feedState.value = FeedState.Success(itemsResponse.body() ?: emptyList())
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
                    _feedState.value = FeedState.Success(publications)
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    }
                    _feedState.value = FeedState.Error("Failed to load feed: ${response.code()}")
                }
            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Unknown error")
            }
        }
    }
    // Додаємо стан для ID поточного користувача
    private val _myUserId = MutableStateFlow<String?>(null)
    val myUserId: StateFlow<String?> = _myUserId.asStateFlow()

    init {
        loadMyCollections()
        loadMyProfile()
    }

    // Завантажуємо свій профіль, щоб знати свій ID
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
            _assessments.value = emptyList() // Очищаємо перед новим завантаженням
            try {
                val response = RetrofitClient.publicationsApi.getAssessmentsList(publicationId)
                if (response.isSuccessful) {
                    _assessments.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error loading assessments", e)
            }
        }
    }

    // Функція видалення коментаря
    fun deleteComment(publicationId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.publicationsApi.deleteComment(publicationId, commentId)
                if (response.isSuccessful) {
                    loadComments(publicationId) // Перезавантажуємо дерево коментарів
                    refreshCurrentFeed() // Оновлюємо лічильник коментарів у стрічці
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error deleting comment", e)
            }
        }
    }
    fun toggleCommentLike(publicationId: String, commentId: String) {
        viewModelScope.launch {
            try {
                // Відправляємо запит на бекенд
                val response = RetrofitClient.publicationsApi.toggleCommentLike(publicationId, commentId)
                if (response.isSuccessful) {
                    // Якщо успішно, оновлюємо локальне дерево коментарів, щоб UI миттєво змінився
                    val currentTree = _comments.value
                    _comments.value = updateLikeInTree(currentTree, commentId)
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Error toggling comment like", e)
            }
        }
    }

    // Рекурсивна функція для зміни лайка у дереві
    private fun updateLikeInTree(comments: List<CommentDTO>, targetCommentId: String): List<CommentDTO> {
        return comments.map { comment ->
            if (comment.id == targetCommentId) {
                // Знайшли потрібний коментар -> змінюємо лайк
                val newIsLiked = !comment.isLiked
                val newLikesCount = if (newIsLiked) comment.likesCount + 1 else comment.likesCount - 1
                comment.copy(isLiked = newIsLiked, likesCount = newLikesCount)
            } else if (comment.replies.isNotEmpty()) {
                // Шукаємо глибше у відповідях
                comment.copy(replies = updateLikeInTree(comment.replies, targetCommentId))
            } else {
                comment
            }
        }
    }
}
