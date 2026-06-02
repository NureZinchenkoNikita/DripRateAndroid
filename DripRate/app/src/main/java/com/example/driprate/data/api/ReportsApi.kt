package com.example.driprate.data.api

import com.example.driprate.data.model.CreateReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportsApi {
    @POST("api/Reports")
    suspend fun sendReport(@Body request: CreateReportRequest): Response<Unit>
}
