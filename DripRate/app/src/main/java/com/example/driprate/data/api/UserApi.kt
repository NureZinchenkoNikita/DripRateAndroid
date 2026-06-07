package com.example.driprate.data.api

import com.example.driprate.data.model.ChangePasswordRequest
import com.example.driprate.data.model.UpdateProfileRequest
import com.example.driprate.data.model.UserDTO
import com.example.driprate.data.model.TagDTO
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    @GET("api/Users/@me")
    suspend fun getMyProfile(): Response<UserDTO>

    @GET("api/Users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<UserDTO>

    @PATCH("api/Users/@me")
    suspend fun updateProfile(@Body profile: UpdateProfileRequest): Response<UserDTO>

    @PUT("api/Auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>


    @Multipart
    @PATCH("api/Users/@me/avatar")
    suspend fun updateAvatar(@Part avatar: MultipartBody.Part): Response<Unit>

    @DELETE("api/Users/@me/avatar")
    suspend fun deleteAvatar(): Response<Unit>
    @POST("api/Users/{id}/follow")
    suspend fun followUser(@Path("id") userId: String): Response<Unit>

    @DELETE("api/Users/{id}/follow")
    suspend fun unfollowUser(@Path("id") userId: String): Response<Unit>

    @GET("api/Users/{id}/followers")
    suspend fun getFollowers(@Path("id") userId: String): Response<List<UserDTO>>

    @GET("api/Users/{id}/following")
    suspend fun getFollowing(@Path("id") userId: String): Response<List<UserDTO>>

    @GET("api/Users/@me/preferences")
    suspend fun getMyPreferences(): Response<List<TagDTO>>

    @PUT("api/Users/@me/preferences")
    suspend fun updatePreferences(@Body tagIds: List<String>): Response<Unit>
}
