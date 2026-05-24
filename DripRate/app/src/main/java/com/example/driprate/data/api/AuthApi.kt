package com.example.driprate.data.api

import com.example.driprate.data.model.AuthResponse
import com.example.driprate.data.model.LoginRequest
import com.example.driprate.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/Auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<String>

    @POST("api/Auth/login")
    suspend fun login(@Body request: LoginRequest): Response<String>
}
