package com.example.driprate.data.api

import com.example.driprate.data.model.TagDTO
import retrofit2.Response
import retrofit2.http.GET

interface MetaApi {
    @GET("api/Meta/tags")
    suspend fun getTags(): Response<List<TagDTO>>
}
