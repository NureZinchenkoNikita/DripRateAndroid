package com.example.driprate.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.GuessPriceRequest
import com.example.driprate.data.api.GuessPriceResponse
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.model.PublicationDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GuessPriceState {
    object Loading : GuessPriceState()
    data class Success(val currentPost: PublicationDTO, val result: GuessPriceResponse? = null) : GuessPriceState()
    data class Error(val message: String) : GuessPriceState()
    object Finished : GuessPriceState()
}

class GuessPriceViewModel : ViewModel() {
    private val _state = MutableStateFlow<GuessPriceState>(GuessPriceState.Loading)
    val state: StateFlow<GuessPriceState> = _state.asStateFlow()

    private val _playedCount = MutableStateFlow(0)
    val playedCount: StateFlow<Int> = _playedCount.asStateFlow()

    private val _totalDifference = MutableStateFlow(0.0)
    val totalDifference: StateFlow<Double> = _totalDifference.asStateFlow()

    init {
        loadNextTask()
    }

    fun resetGame() {
        _playedCount.value = 0
        _totalDifference.value = 0.0
        loadNextTask()
    }

    private fun loadNextTask() {
        if (_playedCount.value >= 10) {
            _state.value = GuessPriceState.Finished
            return
        }

        viewModelScope.launch {
            _state.value = GuessPriceState.Loading
            try {
                val response = RetrofitClient.gamesApi.getGuessPricePost()
                if (response.isSuccessful) {
                    val posts = response.body()
                    val post = posts?.firstOrNull()
                    if (post != null) {
                        _state.value = GuessPriceState.Success(post)
                    } else {
                        _state.value = GuessPriceState.Error("No tasks available right now")
                    }
                } else {
                    _state.value = GuessPriceState.Error("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = GuessPriceState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun submitGuess(publicationId: String, guessedPrice: Double) {
        android.util.Log.d("GuessPriceVM", "Submitting guess: id=$publicationId, price=$guessedPrice")
        viewModelScope.launch {
            try {
                val response = RetrofitClient.gamesApi.submitGuessPrice(
                    GuessPriceRequest(
                        results = listOf(
                            com.example.driprate.data.api.GuessPriceResult(
                                publicationId = publicationId,
                                guessedPrice = guessedPrice
                            )
                        )
                    )
                )
                if (response.isSuccessful) {
                    val result = response.body()?.firstOrNull()
                    android.util.Log.d("GuessPriceVM", "Guess success: actual=${result?.actualPrice}, diff=${result?.difference}")
                    if (result != null) {
                        _totalDifference.value += result.difference
                        val currentState = _state.value
                        if (currentState is GuessPriceState.Success) {
                            _state.value = currentState.copy(result = result)
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("GuessPriceVM", "Guess failed: code=${response.code()}, error=$errorBody")
                    _state.value = GuessPriceState.Error("Server error (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("GuessPriceVM", "Guess exception", e)
                _state.value = GuessPriceState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun nextPost() {
        _playedCount.value += 1
        loadNextTask()
    }
}
