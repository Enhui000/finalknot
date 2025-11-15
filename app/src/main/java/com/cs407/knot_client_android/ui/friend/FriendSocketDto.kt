package com.cs407.knot_client_android.ui.friend

/**
 * WebSocket DTOs with nullable fields so the backend can evolve safely.
 */
internal data class FriendUserDto(
    val userId: Long? = null,
    val username: String? = null,
    val avatarUrl: String? = null
)

internal data class FriendRequestViewDto(
    val requestId: Long? = null,
    val requesterId: Long? = null,
    val receiverId: Long? = null,
    val message: String? = null,
    val status: Int? = null,
    val createdAtMs: Long? = null,
    val convId: Long? = null
)

internal data class FriendSummaryDto(
    val userId: Long? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val convId: Long? = null,
    val since: Long? = null
)