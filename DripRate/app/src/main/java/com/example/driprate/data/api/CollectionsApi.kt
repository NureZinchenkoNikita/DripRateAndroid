package com.example.driprate.data.api

import com.example.driprate.data.model.PublicationDTO
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

@Serializable
data class CollectionDTO(
    val id: String,
    val name: String,
    val isPublic: Boolean,
    val itemsCount: Int
)

@Serializable
data class CreateCollectionRequest(
    val name: String,
    val isPublic: Boolean = false
)

interface CollectionsApi {
    @GET("api/Collections/@me")
    suspend fun getMyCollections(): Response<List<CollectionDTO>>

    @POST("api/Collections")
    suspend fun createCollection(@Body request: CreateCollectionRequest): Response<String>

    @POST("api/Collections/{id}/items/{publicationId}")
    suspend fun addItemToCollection(
        @Path("id") collectionId: String,
        @Path("publicationId") publicationId: String
    ): Response<Unit>

    @DELETE("api/Collections/{id}/items/{publicationId}")
    suspend fun removeItemFromCollection(
        @Path("id") collectionId: String,
        @Path("publicationId") publicationId: String
    ): Response<Unit>

    @DELETE("api/Collections/{id}")
    suspend fun deleteCollection(@Path("id") collectionId: String): Response<Unit>

    @GET("api/Collections/{id}/v2")
    suspend fun getCollectionItems(@Path("id") collectionId: String): Response<List<PublicationDTO>>

    @GET("api/Collections/user/{id}")
    suspend fun getUserCollections(@Path("id") userId: String): Response<List<CollectionDTO>>
}
