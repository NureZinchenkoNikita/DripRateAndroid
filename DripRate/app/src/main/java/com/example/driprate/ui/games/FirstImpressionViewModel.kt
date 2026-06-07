package com.example.driprate.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.FirstImpressionRatingRequest
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.model.PublicationDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FirstImpressionState {
    object Loading : FirstImpressionState()
    data class Success(val currentPost: PublicationDTO) : FirstImpressionState()
    data class Error(val message: String) : FirstImpressionState()
    object Finished : FirstImpressionState()
}

class FirstImpressionViewModel : ViewModel() {
    private val _state = MutableStateFlow<FirstImpressionState>(FirstImpressionState.Loading)
    val state: StateFlow<FirstImpressionState> = _state.asStateFlow()

    private val _ratedCount = MutableStateFlow(0)
    val ratedCount: StateFlow<Int> = _ratedCount.asStateFlow()

    private val _likedCount = MutableStateFlow(0)
    val likedCount: StateFlow<Int> = _likedCount.asStateFlow()

    init {
        loadNextTask()
    }

    fun resetGame() {
        _ratedCount.value = 0
        _likedCount.value = 0
        loadNextTask()
    }

    private fun loadNextTask() {
        if (_ratedCount.value >= 10) {
            _state.value = FirstImpressionState.Finished
            return
        }

        viewModelScope.launch {
            _state.value = FirstImpressionState.Loading
            try {
                // Fetch the "task" from the server - it returns a list
                val response = RetrofitClient.gamesApi.getFirstImpressionPost()
                if (response.isSuccessful) {
                    val posts = response.body()
                    val post = posts?.firstOrNull()
                    if (post != null) {
                        android.util.Log.d("FirstImpressionVM", "Parsed post: id=${post.id}, name=${post.authorName}, images=${post.imageUrls.size}")
                        _state.value = FirstImpressionState.Success(post)
                    } else {
                        _state.value = FirstImpressionState.Error("No tasks available right now")
                    }
                } else {
                    _state.value = FirstImpressionState.Error("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FirstImpressionVM", "Error loading task", e)
                _state.value = FirstImpressionState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun submitRating(publicationId: String, isLiked: Boolean) {
        viewModelScope.launch {
            // 1. Submit answer to server ( Отправка ответа )
            try {
                RetrofitClient.gamesApi.submitFirstImpressionRating(
                    FirstImpressionRatingRequest(publicationId, isLiked)
                )
            } catch (e: Exception) {
                android.util.Log.e("FirstImpressionVM", "Error submitting rating", e)
            }

            // 2. Update local session state
            _ratedCount.value += 1
            if (isLiked) _likedCount.value += 1
            
            // 3. Request next task ( Запрос задания )
            loadNextTask()
        }
    }
}
