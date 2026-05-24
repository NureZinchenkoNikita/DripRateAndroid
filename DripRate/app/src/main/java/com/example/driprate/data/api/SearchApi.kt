package com.example.driprate.data.api

import com.example.driprate.data.model.PublicationDTO
import com.example.driprate.data.model.UserDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {
    @GET("api/Search/publications")
    suspend fun searchPublications(
        @Query("query") query: String,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 20
    ): Response<List<PublicationDTO>>

    @GET("api/Search/users")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 20
    ): Response<List<UserDTO>>
}
