package com.example.driprate.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicationDTO(
    val id: String = "",
    @SerialName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerialName("userId") val authorId: String? = null,
    @SerialName("userDisplayName") val authorName: String? = "Anonymous",

    // ЗМІНЮЄМО ТУТ: Шукаємо поле avatarUrl замість userAvatarUrl
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
    val imageUrl: String? get() = imageUrls.firstOrNull()
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
    @SerialName("userId") val authorId: String? = null, // Зв'язуємо з userId
    @SerialName("userDisplayName") val authorName: String? = null, // Зв'язуємо з userDisplayName
    @SerialName("avatarUrl") val authorAvatarUrl: String? = null, // Зв'язуємо з avatarUrl
    val text: String = "",
    val createdAt: String? = null,
    val likesCount: Int = 0,
    @SerialName("isLikedByMe") val isLiked: Boolean = false, // Зв'язуємо з isLikedByMe
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
data class GlobalFeedResponse(
    val publications: List<PublicationDTO> = emptyList()
)
