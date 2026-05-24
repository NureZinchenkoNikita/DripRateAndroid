package com.example.driprate.ui.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.api.auth.TokenManager
import com.example.driprate.data.model.WardrobeItemDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class CreatePostState {
    object Idle : CreatePostState()
    object Loading : CreatePostState()
    object Success : CreatePostState()
    data class Error(val message: String) : CreatePostState()
}

class CreatePostViewModel : ViewModel() {
    private val _state = MutableStateFlow<CreatePostState>(CreatePostState.Idle)
    val state: StateFlow<CreatePostState> = _state

    private val _wardrobeItems = MutableStateFlow<List<WardrobeItemDTO>>(emptyList())
    val wardrobeItems: StateFlow<List<WardrobeItemDTO>> = _wardrobeItems.asStateFlow()

    private val _selectedWardrobeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedWardrobeIds: StateFlow<Set<String>> = _selectedWardrobeIds.asStateFlow()

    init {
        loadWardrobe()
    }

    private fun loadWardrobe() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.wardrobeApi.getWardrobe()
                if (response.isSuccessful) {
                    _wardrobeItems.value = response.body() ?: emptyList()
                } else if (response.code() == 401) {
                    TokenManager.clear()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleWardrobeSelection(id: String) {
        val current = _selectedWardrobeIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedWardrobeIds.value = current
    }

    fun createPost(context: Context, imageUri: Uri, description: String, isUrgent: Boolean = false) {
        viewModelScope.launch {
            _state.value = CreatePostState.Loading
            try {
                val file = getFileFromUri(context, imageUri)
                if (file == null) {
                    _state.value = CreatePostState.Error("Failed to process image")
                    return@launch
                }

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("Image", file.name, requestFile)
                val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val isUrgentPart = isUrgent.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                
                val wardrobeParts = _selectedWardrobeIds.value.map { 
                    it.toRequestBody("text/plain".toMediaTypeOrNull())
                }

                val response = RetrofitClient.publicationsApi.createPublication(
                    image = body,
                    description = descriptionPart,
                    wardrobeItemIds = wardrobeParts,
                    tags = emptyList(),
                    isUrgent = isUrgentPart
                )

                if (response.isSuccessful) {
                    _state.value = CreatePostState.Success
                } else {
                    if (response.code() == 401) {
                        TokenManager.clear()
                    }
                    _state.value = CreatePostState.Error("Failed to create post: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = CreatePostState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return file
    }
}
