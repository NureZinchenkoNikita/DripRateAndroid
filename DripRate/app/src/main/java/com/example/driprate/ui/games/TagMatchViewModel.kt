package com.example.driprate.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.TagMatchRequest
import com.example.driprate.data.api.TagMatchResponse
import com.example.driprate.data.api.TagMatchResult
import com.example.driprate.data.api.TagMatchTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TagMatchState {
    object Loading : TagMatchState()
    data class Success(val task: TagMatchTask, val result: TagMatchResponse? = null, val selectedTagId: String? = null) : TagMatchState()
    data class Error(val message: String) : TagMatchState()
    object Finished : TagMatchState()
}

class TagMatchViewModel : ViewModel() {
    private val _state = MutableStateFlow<TagMatchState>(TagMatchState.Loading)
    val state: StateFlow<TagMatchState> = _state.asStateFlow()

    private val _playedCount = MutableStateFlow(0)
    val playedCount: StateFlow<Int> = _playedCount.asStateFlow()

    private val _correctCount = MutableStateFlow(0)
    val correctCount: StateFlow<Int> = _correctCount.asStateFlow()

    init {
        loadNextTask()
    }

    fun resetGame() {
        _playedCount.value = 0
        _correctCount.value = 0
        loadNextTask()
    }

    private fun loadNextTask() {
        if (_playedCount.value >= 10) {
            _state.value = TagMatchState.Finished
            return
        }

        viewModelScope.launch {
            _state.value = TagMatchState.Loading
            try {
                val response = RetrofitClient.gamesApi.getTagMatchTask()
                if (response.isSuccessful) {
                    val tasks = response.body()
                    val task = tasks?.firstOrNull()
                    if (task != null) {
                        _state.value = TagMatchState.Success(task)
                    } else {
                        _state.value = TagMatchState.Error("No tasks available")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("TagMatchVM", "Task failed: code=${response.code()}, error=$errorBody")
                    _state.value = TagMatchState.Error("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("TagMatchVM", "Task exception", e)
                _state.value = TagMatchState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun submitTag(publicationId: String, tagId: String) {
        android.util.Log.d("TagMatchVM", "Submitting tag: pubId=$publicationId, tagId=$tagId")
        viewModelScope.launch {
            try {
                val response = RetrofitClient.gamesApi.submitTagMatch(
                    TagMatchRequest(
                        results = listOf(
                            TagMatchResult(
                                publicationId = publicationId,
                                tagIds = listOf(tagId)
                            )
                        )
                    )
                )
                if (response.isSuccessful) {
                    val result = response.body()?.firstOrNull()
                    if (result != null) {
                        if (result.isCorrect) _correctCount.value += 1
                        val currentState = _state.value
                        if (currentState is TagMatchState.Success) {
                            _state.value = currentState.copy(result = result, selectedTagId = tagId)
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("TagMatchVM", "Tag failed: code=${response.code()}, error=$errorBody")
                    _state.value = TagMatchState.Error("Server error (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("TagMatchVM", "Tag exception", e)
                _state.value = TagMatchState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun nextPost() {
        _playedCount.value += 1
        loadNextTask()
    }
}
