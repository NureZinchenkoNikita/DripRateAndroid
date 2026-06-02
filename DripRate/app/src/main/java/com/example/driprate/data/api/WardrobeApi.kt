package com.example.driprate.data.api

import com.example.driprate.data.model.WardrobeItemDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface WardrobeApi {
    @GET("api/Wardrobe")
    suspend fun getWardrobe(
        @Query("userId") userId: String? = null
    ): Response<List<WardrobeItemDTO>>

    @GET("api/Wardrobe/{id}")
    suspend fun getWardrobeItem(
        @Path("id") id: String
    ): Response<WardrobeItemDTO>

    @Multipart
    @POST("api/Wardrobe")
    suspend fun addWardrobeItem(
        @Part("Name") name: RequestBody,
        @Part("Brand") brand: RequestBody?,
        @Part("StoreLink") storeLink: RequestBody?,
        @Part("EstimatedPrice") estimatedPrice: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): Response<String>

    @Multipart
    @PUT("api/Wardrobe/{id}")
    suspend fun updateWardrobeItem(
        @Path("id") id: String,
        @Part("Name") name: RequestBody,
        @Part("Brand") brand: RequestBody?,
        @Part("StoreLink") storeLink: RequestBody?,
        @Part("EstimatedPrice") estimatedPrice: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): Response<Unit>

    @DELETE("api/Wardrobe/{id}")
    suspend fun deleteWardrobeItem(
        @Path("id") id: String
    ): Response<Unit>
}
