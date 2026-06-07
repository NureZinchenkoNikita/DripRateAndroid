package com.example.driprate.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthorInfo(
    val id: String? = null,
    val displayName: String? = null,
    val userName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class PublicationDTO(
    val id: String = "",
    @SerialName("publicationId") val pubId: String? = null,
    @SerialName("publicationld") val pubIdTypo: String? = null,
    
    @SerialName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerialName("images") val images: List<String>? = null,
    @SerialName("imageUrl") val imageUrlSingle: String? = null,

    @SerialName("userId") val authorId: String? = null,
    @SerialName("userDisplayName") val authorName: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("userName") val userName: String? = null,
    @SerialName("author") val author: AuthorInfo? = null,

    // Шукаємо поле avatarUrl замість userAvatarUrl
    @SerialName("avatarUrl") val authorAvatarUrl: String? = null,

    val description: String? = null,
    val createdAt: String? = null,
    val likesCount: Int = 0,
    @SerialName("isLikedByMe") val isLiked: Boolean = false,
    @SerialName("isSavedByMe") val isSaved: Boolean = false,
    val commentsCount: Int = 0,
    @SerialName("averageRating") val averageAssessment: Double? = null,
    val myAssessment: Int? = null,
    val tags: List<TagDTO> = emptyList(),
    val clothes: List<WardrobeItemDTO> = emptyList()
) {
    val realId: String get() = id.takeIf { it.isNotBlank() } ?: pubId ?: pubIdTypo ?: ""
    val realAuthorName: String get() = authorName?.takeIf { it.isNotBlank() } 
        ?: displayName?.takeIf { it.isNotBlank() } 
        ?: userName?.takeIf { it.isNotBlank() } 
        ?: author?.displayName?.takeIf { it.isNotBlank() }
        ?: author?.userName?.takeIf { it.isNotBlank() }
        ?: "Anonymous"
    val imageUrl: String? get() = imageUrls.firstOrNull() ?: images?.firstOrNull() ?: imageUrlSingle
    
    val allTagNames: List<String> get() {
        val parsed = description?.let { desc ->
            val regex = """#([a-zA-Z0-9_\u0400-\u04FF]+)""".toRegex()
            regex.findAll(desc).map { it.groupValues[1] }.toList()
        } ?: emptyList()
        val fromServer = tags.map { it.name }
        return (parsed + fromServer).distinct()
    }
}

@Serializable
data class TagDTO(
    val id: String = "",
    val name: String = "",
    val category: String? = null
)

@Serializable
data class AssessmentDTO(
    @SerialName("userId") val userId: String,
    @SerialName("userDisplayName") val userName: String? = "Anonymous",
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    val colorCoordination: Int = 0,
    val fitAndProportions: Int = 0,
    val originality: Int = 0,
    val overallStyle: Int = 0,
    val createdAt: String? = null
) {
    // Автоматично вираховуємо середній бал для UI
    val averageScore: Double get() = (colorCoordination + fitAndProportions + originality + overallStyle) / 4.0
}

@Serializable
data class MyAssessmentResponse(
    val score: Int
)

@Serializable
data class CommentDTO(
    val id: String = "",
    @SerialName("userId") val authorId: String? = null,
    @SerialName("userDisplayName") val authorName: String? = null,
    @SerialName("avatarUrl") val authorAvatarUrl: String? = null,
    val text: String = "",
    val createdAt: String? = null,
    val likesCount: Int = 0,
    @SerialName("isLikedByMe") val isLiked: Boolean = false,
    val parentCommentId: String? = null,
    val repliesCount: Int = 0,
    val replies: List<CommentDTO> = emptyList()
)

@Serializable
data class CreateCommentRequest(
    val text: String,
    val parentCommentId: String? = null
)

@Serializable
data class AssessmentRequest(
    val colorCoordination: Int,
    val fitAndProportions: Int,
    val originality: Int,
    val overallStyle: Int
)

@Serializable
data class AdvertisementDTO(
    val id: String = "",
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    
    @SerialName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerialName("images") val images: List<String>? = null,
    @SerialName("imageUrl") val imageUrlSingle: String? = null,

    @SerialName("targetUrl") val targetUrl: String? = null,
    @SerialName("linkUrl") val linkUrl: String? = null,
    @SerialName("url") val url: String? = null,

    @SerialName("advertiserName") val advertiserName: String? = null,
    @SerialName("brand") val brand: String? = null
) {
    val realImageUrl: String? get() = imageUrls.firstOrNull() ?: images?.firstOrNull() ?: imageUrlSingle
    val realTargetUrl: String? get() = targetUrl ?: linkUrl ?: url
    val realAdvertiserName: String? get() = advertiserName ?: brand
}

@Serializable
data class GlobalFeedResponse(
    val publications: List<PublicationDTO> = emptyList(),
    val advertisements: List<AdvertisementDTO> = emptyList()
) {
    fun toFeedItems(): List<FeedItem> {
        val items = mutableListOf<FeedItem>()
        // Simple logic: insert an ad every few publications
        publications.forEachIndexed { index, pub ->
            items.add(FeedItem.Publication(pub))
            if ((index + 1) % 5 == 0 && (index / 5) < advertisements.size) {
                items.add(FeedItem.Advertisement(advertisements[index / 5]))
            }
        }
        // If there are ads left, add them to the end
        val adsUsed = publications.size / 5
        if (adsUsed < advertisements.size) {
            for (i in adsUsed until advertisements.size) {
                items.add(FeedItem.Advertisement(advertisements[i]))
            }
        }
        return items
    }
}

sealed class FeedItem {
    data class Publication(val data: PublicationDTO) : FeedItem()
    data class Advertisement(val data: AdvertisementDTO) : FeedItem()
}
