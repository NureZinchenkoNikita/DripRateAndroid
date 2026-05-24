package com.example.driprate.data.api

import com.example.driprate.data.model.WardrobeItemDTO
import retrofit2.Response
import retrofit2.http.GET

interface WardrobeApi {
    @GET("api/Wardrobe")
    suspend fun getWardrobe(): Response<List<WardrobeItemDTO>>
}
