package com.example.driprate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val displayName: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String? = null
)
