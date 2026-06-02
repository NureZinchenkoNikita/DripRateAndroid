package com.example.driprate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateReportRequest(
    val targetId: String,
    val targetType: String, // "Publication", "Comment", "User"
    val reason: String
)
