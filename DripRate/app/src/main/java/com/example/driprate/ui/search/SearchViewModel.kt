package com.example.driprate.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.data.model.PublicationDTO
import com.example.driprate.data.model.UserDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                    _searchState.value = SearchResultState.Success(
                        publications = pubResponse.body() ?: emptyList(),
                        users = userResponse.body() ?: emptyList()
                    )
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
                    // Update the local state to show following
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
                    // Update the local state to show not following
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
}
