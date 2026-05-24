package com.example.driprate.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: String = "",
    val userName: String = "",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val publicationsCount: Int = 0,
    val isFollowing: Boolean = false
)

@Serializable
data class UpdateProfileRequest(
    val userName: String? = null,
    val displayName: String? = null,
    val bio: String? = null
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
