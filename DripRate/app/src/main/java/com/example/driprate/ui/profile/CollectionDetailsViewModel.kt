package com.example.driprate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.data.model.PublicationDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CollectionDetailsState {
    object Loading : CollectionDetailsState()
    data class Success(val name: String, val items: List<PublicationDTO>) : CollectionDetailsState()
    data class Error(val message: String) : CollectionDetailsState()
}

class CollectionDetailsViewModel : ViewModel() {
    private val _state = MutableStateFlow<CollectionDetailsState>(CollectionDetailsState.Loading)
    val state: StateFlow<CollectionDetailsState> = _state

    fun loadCollection(collectionId: String, collectionName: String) {
        viewModelScope.launch {
            _state.value = CollectionDetailsState.Loading
            try {
                val response = RetrofitClient.collectionsApi.getCollectionItems(collectionId)
                if (response.isSuccessful) {
                    _state.value = CollectionDetailsState.Success(collectionName, response.body() ?: emptyList())
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    }
                    _state.value = CollectionDetailsState.Error("Failed to load collection items: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = CollectionDetailsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun removeFromCollection(collectionId: String, publicationId: String, collectionName: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.collectionsApi.removeItemFromCollection(collectionId, publicationId)
                if (response.isSuccessful) {
                    loadCollection(collectionId, collectionName)
                }
            } catch (e: Exception) {}
        }
    }
}
