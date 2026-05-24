package com.example.driprate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WardrobeItemDTO(
    val id: String = "",
    val name: String = "",
    val brand: String? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val tags: List<String> = emptyList()
)
