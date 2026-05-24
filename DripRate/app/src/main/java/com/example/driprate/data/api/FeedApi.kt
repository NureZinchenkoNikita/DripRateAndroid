package com.example.driprate.data.api

import com.example.driprate.data.model.GlobalFeedResponse
import com.example.driprate.data.model.PublicationDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
interface FeedApi {
    @GET("api/Feed/global")
    suspend fun getGlobalFeed(
        @Query("skip") skip: Int,
        @Query("take") take: Int
    ): Response<GlobalFeedResponse> // Залишаємо об'єкт (тут є реклама)

    @GET("api/Feed/subscriptions")
    suspend fun getSubscriptionsFeed(
        @Query("skip") skip: Int,
        @Query("take") take: Int
    ): Response<List<PublicationDTO>> // Повертаємо масив

    @GET("api/Feed/top")
    suspend fun getTopFeed(
        @Query("skip") skip: Int,
        @Query("take") take: Int
    ): Response<List<PublicationDTO>> // Повертаємо масив

    @GET("api/Feed/urgent")
    suspend fun getUrgentFeed(
        @Query("skip") skip: Int,
        @Query("take") take: Int
    ): Response<List<PublicationDTO>> // Повертаємо масив

    @GET("api/Feed/user/{userId}")
    suspend fun getUserFeed(
        @Path("userId") userId: String,
        @Query("skip") skip: Int,
        @Query("take") take: Int
    ): Response<List<PublicationDTO>> // Повертаємо масив
}
