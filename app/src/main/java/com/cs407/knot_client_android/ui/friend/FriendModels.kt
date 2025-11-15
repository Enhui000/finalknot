package com.cs407.knot_client_android.ui.friend

/**
 * UI layer models backing the friend system.
 */
data class FriendUiState(
    val isConnected: Boolean = false,
    val incomingRequests: List<FriendRequestUiModel> = emptyList(),
    val outgoingRequests: List<FriendRequestUiModel> = emptyList(),
    val friends: List<FriendSummary> = emptyList()
)

enum class FriendRequestStatus { PENDING, ACCEPTED, REJECTED }

data class FriendRequestUiModel(
    val requestId: Long,
    val requesterId: Long,
    val receiverId: Long,
    val message: String,
    val status: FriendRequestStatus,
    val createdAtMs: Long,
    val convId: Long?,
    val requesterName: String? = null,
    val requesterAvatar: String? = null,
    val receiverName: String? = null,
    val isIncoming: Boolean
)

data class FriendSummary(
    val userId: Long,
    val username: String?,
    val avatarUrl: String?,
    val convId: Long?,
    val since: Long?
)

data class FriendStateUpdate(
    val state: FriendUiState,
    val message: String? = null,
    val isError: Boolean = false
)