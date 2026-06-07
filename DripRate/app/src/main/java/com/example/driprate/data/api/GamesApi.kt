package com.example.driprate.data.api

import com.example.driprate.data.model.PublicationDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GamesApi {
    @GET("api/Games/first-impression")
    suspend fun getFirstImpressionPost(): Response<List<PublicationDTO>>

    @POST("api/Games/first-impression")
    suspend fun submitFirstImpressionRating(@Body request: FirstImpressionRatingRequest): Response<Unit>

    @GET("api/Games/guess-price")
    suspend fun getGuessPricePost(): Response<List<PublicationDTO>>

    @POST("api/Games/guess-price")
    suspend fun submitGuessPrice(@Body request: GuessPriceRequest): Response<List<GuessPriceResponse>>

    @GET("api/Games/tag-match")
    suspend fun getTagMatchTask(): Response<List<TagMatchTask>>

    @POST("api/Games/tag-match")
    suspend fun submitTagMatch(@Body request: TagMatchRequest): Response<List<TagMatchResponse>>
}

@kotlinx.serialization.Serializable
data class TagMatchTask(
    @kotlinx.serialization.SerialName("publicationId") val id: String,
    @kotlinx.serialization.SerialName("images") val imageUrls: List<String>,
    @kotlinx.serialization.SerialName("tags") val options: List<TagDTO>
)

@kotlinx.serialization.Serializable
data class TagDTO(
    val id: String,
    val name: String
)

@kotlinx.serialization.Serializable
data class TagMatchRequest(
    @kotlinx.serialization.SerialName("Results") val results: List<TagMatchResult>
)

@kotlinx.serialization.Serializable
data class TagMatchResult(
    @kotlinx.serialization.SerialName("PublicationId") val publicationId: String,
    @kotlinx.serialization.SerialName("TagIds") val tagIds: List<String>
)

@kotlinx.serialization.Serializable
data class TagMatchResponse(
    @kotlinx.serialization.SerialName("publicationId") val publicationId: String,
    @kotlinx.serialization.SerialName("correctTagIds") val correctTagIds: List<String>,
    @kotlinx.serialization.SerialName("selectedTagIds") val selectedTagIds: List<String>,
    @kotlinx.serialization.SerialName("score") val score: Int,
    val message: String? = null
) {
    val isCorrect: Boolean get() = selectedTagIds.any { it in correctTagIds } || score > 0
}

@kotlinx.serialization.Serializable
data class FirstImpressionRatingRequest(
    val publicationId: String,
    val isLiked: Boolean
)

@kotlinx.serialization.Serializable
data class GuessPriceRequest(
    @kotlinx.serialization.SerialName("Results") val results: List<GuessPriceResult>
)

@kotlinx.serialization.Serializable
data class GuessPriceResult(
    @kotlinx.serialization.SerialName("PublicationId") val publicationId: String,
    val guessedPrice: Double
)

@kotlinx.serialization.Serializable
data class GuessPriceResponse(
    @kotlinx.serialization.SerialName("realPrice") val actualPrice: Double,
    val difference: Double,
    val message: String? = null
)
