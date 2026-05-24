package com.example.driprate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driprate.data.api.RetrofitClient
import com.example.driprate.data.model.LoginRequest
import com.example.driprate.data.model.RegisterRequest
import com.example.driprate.data.api.auth.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.authApi.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.trim('"')
                    if (token != null) {
                        _authState.value = AuthState.Success(token)
                    } else {
                        _authState.value = AuthState.Error("Empty response body")
                    }
                } else {
                    val errorMsg = try {
                        val errorStr = response.errorBody()?.string()
                        if (!errorStr.isNullOrBlank() && errorStr.startsWith("{")) {
                            // Парсимо JSON і дістаємо поле title
                            org.json.JSONObject(errorStr)
                                .optString("title", "Invalid email or password.")
                        } else {
                            errorStr ?: "Login failed: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Invalid email or password."
                    }
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.authApi.register(RegisterRequest(username, email, password))
                if (response.isSuccessful) {
                    // Після успішної реєстрації автоматично викликаємо логін
                    val loginResponse = RetrofitClient.authApi.login(LoginRequest(email, password))
                    if (loginResponse.isSuccessful) {
                        val token = loginResponse.body()?.trim('"')
                        if (token != null) {
                            _authState.value = AuthState.Success(token)
                        } else {
                            _authState.value = AuthState.Error("Registered, but failed to get token")
                        }
                    } else {
                        _authState.value = AuthState.Error("Registered successfully, but login failed. Please sign in manually.")
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed: ${response.code()}"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
