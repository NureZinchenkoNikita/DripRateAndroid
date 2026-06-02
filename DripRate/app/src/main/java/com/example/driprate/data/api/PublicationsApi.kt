package com.example.driprate.data.api

import com.example.driprate.data.model.AssessmentDTO
import com.example.driprate.data.model.AssessmentRequest
import com.example.driprate.data.model.CommentDTO
import com.example.driprate.data.model.CreateCommentRequest
import com.example.driprate.data.model.MyAssessmentResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface PublicationsApi {
    @POST("api/Publications/{id}/like")
    suspend fun toggleLike(@Path("id") id: String): Response<Unit>

    @POST("api/Publications/{id}/save")
    suspend fun toggleSave(@Path("id") publicationId: String): Response<Unit>

    @POST("api/Publications/{id}/assessments")
    suspend fun setAssessment(
        @Path("id") id: String,
        @Body score: AssessmentRequest
    ): Response<Unit>
    // Отримати список усіх, хто оцінив
    @GET("api/Publications/{id}/assessments/list")
    suspend fun getAssessmentsList(@Path("id") publicationId: String): Response<List<AssessmentDTO>>

    // Отримати мою оцінку (якщо раптом треба буде окремо)
    @GET("api/Publications/{id}/assessments/my")
    suspend fun getMyAssessment(@Path("id") publicationId: String): Response<MyAssessmentResponse>
    @GET("api/Publications/{id}/comments")
    suspend fun getComments(
        @Path("id") publicationId: String,
        @Query("parentCommentId") parentCommentId: String? = null // Додаємо цей параметр!
    ): Response<List<CommentDTO>>

    @POST("api/Publications/{id}/comments")
    suspend fun postComment(
        @Path("id") id: String,
        @Body request: CreateCommentRequest
    ): Response<Unit>

    @POST("api/Publications/{id}/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("id") publicationId: String,
        @Path("commentId") commentId: String
    ): Response<Unit>

    @DELETE("api/Publications/{id}")
    suspend fun deletePublication(@Path("id") id: String): Response<Unit>

    @DELETE("api/Publications/{id}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("id") publicationId: String,
        @Path("commentId") commentId: String
    ): Response<Unit>

    @Multipart
    @POST("api/Publications")
    suspend fun createPublication(
        @Part image: MultipartBody.Part,
        @Part("Description") description: RequestBody,
        @Part("ClothIds") wardrobeItemIds: List<@JvmSuppressWildcards RequestBody>,
        @Part("Tags") tags: List<@JvmSuppressWildcards RequestBody>,
        @Part("IsUrgentRatingRequested") isUrgent: RequestBody
    ): Response<Unit>
}
