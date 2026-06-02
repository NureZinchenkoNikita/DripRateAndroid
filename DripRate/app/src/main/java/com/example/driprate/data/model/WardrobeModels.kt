package com.example.driprate.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WardrobeItemDTO(
    val id: String = "",
    val name: String = "",
    val brand: String? = null,
    @SerialName("photoUrl") val imageUrl: String? = null,
    @SerialName("estimatedPrice") val price: Double? = null,
    @SerialName("storeLink") val storeLink: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList()
)
