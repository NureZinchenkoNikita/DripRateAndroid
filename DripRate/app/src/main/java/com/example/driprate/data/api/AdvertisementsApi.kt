package com.example.driprate.data.api

import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path

interface AdvertisementsApi {
    @POST("api/Advertisements/{id}/view")
    suspend fun registerView(@Path("id") adId: String): Response<Unit>
}
