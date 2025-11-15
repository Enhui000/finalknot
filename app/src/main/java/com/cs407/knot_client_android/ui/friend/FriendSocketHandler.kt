package com.cs407.knot_client_android.ui.friend

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale

internal data class PendingSendContext(
    val placeholderId: Long?,
    val existingRequestId: Long?,
    val receiverId: Long,
    val message: String
)

internal fun handleFriendSocketMessage(
    rawMessage: String,
    currentState: FriendUiState,
    pendingSends: SnapshotStateList<PendingSendContext>,
    gson: Gson
): FriendStateUpdate {
    return try {
        val element = JsonParser.parseString(rawMessage)
        if (!element.isJsonObject) {
            return FriendStateUpdate(currentState)
        }
        val obj = element.asJsonObject
        val type = obj.get("type")?.asString ?: return FriendStateUpdate(currentState)
        when (type.uppercase(Locale.ROOT)) {
            "FRIEND_REQUEST_NEW" -> handleFriendRequestNew(obj, currentState, gson)
            "FRIEND_REQUEST_ACK" -> handleFriendRequestAck(obj, currentState, pendingSends)
            "FRIEND_ADDED" -> handleFriendAdded(obj, currentState, gson)
            "FRIEND_REQUEST_LIST" -> handleFriendRequestList(obj, currentState, gson)
            "FRIEND_LIST" -> handleFriendList(obj, currentState, gson)
            else -> FriendStateUpdate(currentState)
        }
    } catch (e: Exception) {
        FriendStateUpdate(
            state = currentState,
            message = "Failed to parse friend message: ${e.message ?: "unknown"}",
            isError = true
        )
    }
}

private fun handleFriendRequestNew(
    obj: JsonObject,
    currentState: FriendUiState,
    gson: Gson
): FriendStateUpdate {
    val requestId = obj.get("requestId")?.asLong ?: return FriendStateUpdate(currentState)
    val timestamp = obj.get("timestamp")?.asLong ?: System.currentTimeMillis()
    val message = obj.get("message")?.asString ?: ""
    val receiverId = obj.get("receiverId")?.asLong
    val fromUser = obj.getAsJsonObject("fromUser")?.let { gson.fromJson(it, FriendUserDto::class.java) }

    val existing = currentState.incomingRequests.find { it.requestId == requestId }
    val requesterId = fromUser?.userId ?: existing?.requesterId ?: -1L
    val resolvedReceiverId = receiverId ?: existing?.receiverId ?: -1L

    val updatedRequest = FriendRequestUiModel(
        requestId = requestId,
        requesterId = requesterId,
        receiverId = resolvedReceiverId,
        message = message.ifBlank { existing?.message ?: "" },
        status = FriendRequestStatus.PENDING,
        createdAtMs = timestamp,
        convId = existing?.convId,
        requesterName = fromUser?.username ?: existing?.requesterName,
        requesterAvatar = fromUser?.avatarUrl ?: existing?.requesterAvatar,
        receiverName = existing?.receiverName,
        isIncoming = true
    )

    val filtered = currentState.incomingRequests.filterNot { it.requestId == requestId }
    val updatedIncoming = (filtered + updatedRequest).sortedByDescending { it.createdAtMs }
    val displayName = updatedRequest.requesterName ?: "User ${updatedRequest.requesterId}"
    return FriendStateUpdate(
        currentState.copy(incomingRequests = updatedIncoming),
        message = "New friend request from $displayName"
    )
}

private fun handleFriendRequestAck(
    obj: JsonObject,
    currentState: FriendUiState,
    pendingSends: SnapshotStateList<PendingSendContext>
): FriendStateUpdate {
    val status = obj.get("status")?.asString?.lowercase(Locale.ROOT) ?: return FriendStateUpdate(currentState)
    val requestId = obj.get("requestId")?.asLong
    val timestamp = obj.get("timestamp")?.asLong ?: System.currentTimeMillis()
    val convId = obj.get("convId")?.asLong
    val message = obj.get("message")?.asString

    return when (status) {
        "sent" -> handleAckSent(requestId, timestamp, currentState, pendingSends)
        "accepted" -> handleAckAccepted(requestId, timestamp, convId, currentState)
        "rejected" -> handleAckRejected(requestId, currentState)
        "error", "failed" -> handleAckError(message, currentState, pendingSends)
        else -> FriendStateUpdate(currentState)
    }
}

private fun handleAckSent(
    requestId: Long?,
    timestamp: Long,
    currentState: FriendUiState,
    pendingSends: SnapshotStateList<PendingSendContext>
): FriendStateUpdate {
    if (requestId == null) return FriendStateUpdate(currentState)
    val context = pendingSends.removeFirstOrNull()
    return if (context != null) {
        if (context.placeholderId != null) {
            val placeholderId = context.placeholderId
            val base = currentState.outgoingRequests.find { it.requestId == placeholderId }
            val fallback = base ?: FriendRequestUiModel(
                requestId = placeholderId,
                requesterId = -1L,
                receiverId = context.receiverId,
                message = context.message,
                status = FriendRequestStatus.PENDING,
                createdAtMs = timestamp,
                convId = null,
                requesterName = "You",
                requesterAvatar = null,
                receiverName = null,
                isIncoming = false
            )
            val updatedEntry = fallback.copy(
                requestId = requestId,
                receiverId = context.receiverId,
                message = context.message.ifBlank { fallback.message },
                status = FriendRequestStatus.PENDING,
                createdAtMs = timestamp,
                convId = null,
                isIncoming = false
            )
            val filtered = currentState.outgoingRequests.filterNot { it.requestId == placeholderId }
            val newOutgoing = (filtered + updatedEntry).sortedByDescending { it.createdAtMs }
            FriendStateUpdate(
                currentState.copy(outgoingRequests = newOutgoing),
                message = "Friend request sent"
            )
        } else if (context.existingRequestId != null) {
            val existingId = context.existingRequestId
            val mapped = currentState.outgoingRequests.map { req ->
                if (req.requestId == existingId || req.requestId == requestId) {
                    req.copy(
                        requestId = requestId,
                        receiverId = if (context.receiverId > 0) context.receiverId else req.receiverId,
                        message = context.message.ifBlank { req.message },
                        status = FriendRequestStatus.PENDING,
                        createdAtMs = timestamp,
                        convId = null
                    )
                } else {
                    req
                }
            }
            val newOutgoing = mapped.sortedByDescending { it.createdAtMs }
            FriendStateUpdate(
                currentState.copy(outgoingRequests = newOutgoing),
                message = "Friend request resent"
            )
        } else {
            FriendStateUpdate(currentState)
        }
    } else {
        val mapped = currentState.outgoingRequests.map { req ->
            if (req.requestId == requestId) {
                req.copy(status = FriendRequestStatus.PENDING, createdAtMs = timestamp)
            } else req
        }
        FriendStateUpdate(currentState.copy(outgoingRequests = mapped.sortedByDescending { it.createdAtMs }))
    }
}

private fun handleAckAccepted(
    requestId: Long?,
    timestamp: Long,
    convId: Long?,
    currentState: FriendUiState
): FriendStateUpdate {
    if (requestId == null) return FriendStateUpdate(currentState)
    val incoming = currentState.incomingRequests.find { it.requestId == requestId }
    if (incoming != null) {
        val updatedIncoming = currentState.incomingRequests.filterNot { it.requestId == requestId }
        val newFriend = FriendSummary(
            userId = incoming.requesterId,
            username = incoming.requesterName,
            avatarUrl = incoming.requesterAvatar,
            convId = convId ?: incoming.convId,
            since = timestamp
        )
        val updatedFriends = (currentState.friends + newFriend)
            .distinctBy { it.userId }
            .sortedBy { it.username ?: it.userId.toString() }
    val displayName = incoming.requesterName ?: "User ${incoming.requesterId}"
        return FriendStateUpdate(
            currentState.copy(
                incomingRequests = updatedIncoming,
                friends = updatedFriends
            ),
            message = "Accepted $displayName's request"
        )
    }

    val outgoing = currentState.outgoingRequests.find { it.requestId == requestId }
    val updatedOutgoing = currentState.outgoingRequests.filterNot { it.requestId == requestId }
    val message = outgoing?.let { "Your friend request was accepted" }
    return FriendStateUpdate(
        currentState.copy(outgoingRequests = updatedOutgoing.sortedByDescending { it.createdAtMs }),
        message = message
    )
}

private fun handleAckRejected(
    requestId: Long?,
    currentState: FriendUiState
): FriendStateUpdate {
    if (requestId == null) return FriendStateUpdate(currentState)
    var updatedState = currentState
    var infoMessage: String? = null
    val incoming = currentState.incomingRequests.find { it.requestId == requestId }
    if (incoming != null) {
        updatedState = updatedState.copy(
            incomingRequests = updatedState.incomingRequests.filterNot { it.requestId == requestId }
        )
        infoMessage = "Rejected ${incoming.requesterName ?: "User ${incoming.requesterId}"}'s friend request"
    }

    val outgoing = updatedState.outgoingRequests.find { it.requestId == requestId }
    if (outgoing != null) {
        val mapped = updatedState.outgoingRequests.map { req ->
            if (req.requestId == requestId) {
                req.copy(status = FriendRequestStatus.REJECTED)
            } else req
        }
        updatedState = updatedState.copy(outgoingRequests = mapped.sortedByDescending { it.createdAtMs })
        if (incoming == null) {
            infoMessage = "Friend request was rejected by the recipient"
        }
    }

    return FriendStateUpdate(updatedState, infoMessage)
}

private fun handleAckError(
    backendMessage: String?,
    currentState: FriendUiState,
    pendingSends: SnapshotStateList<PendingSendContext>
): FriendStateUpdate {
    val context = pendingSends.removeFirstOrNull()
    val stateAfter = if (context?.placeholderId != null) {
        currentState.copy(
            outgoingRequests = currentState.outgoingRequests.filterNot { it.requestId == context.placeholderId }
        )
    } else {
        currentState
    }
    val msg = backendMessage ?: "Friend action failed"
    return FriendStateUpdate(stateAfter, msg, isError = true)
}

private fun handleFriendAdded(
    obj: JsonObject,
    currentState: FriendUiState,
    gson: Gson
): FriendStateUpdate {
    val requestId = obj.get("requestId")?.asLong
    val friendDto = obj.getAsJsonObject("friend")?.let { gson.fromJson(it, FriendUserDto::class.java) }
    val convId = obj.get("convId")?.asLong
    val timestamp = obj.get("timestamp")?.asLong ?: System.currentTimeMillis()
    if (friendDto?.userId == null) return FriendStateUpdate(currentState)

    val filteredOutgoing = requestId?.let { id ->
        currentState.outgoingRequests.filterNot { it.requestId == id }
    } ?: currentState.outgoingRequests
    val filteredIncoming = requestId?.let { id ->
        currentState.incomingRequests.filterNot { it.requestId == id }
    } ?: currentState.incomingRequests

    val newFriend = FriendSummary(
        userId = friendDto.userId,
        username = friendDto.username,
        avatarUrl = friendDto.avatarUrl,
        convId = convId,
        since = timestamp
    )
    val updatedFriends = (currentState.friends + newFriend)
        .distinctBy { it.userId }
        .sortedBy { it.username ?: it.userId.toString() }
    val displayName = friendDto.username ?: "User ${friendDto.userId}"
    return FriendStateUpdate(
        currentState.copy(
            outgoingRequests = filteredOutgoing.sortedByDescending { it.createdAtMs },
            incomingRequests = filteredIncoming.sortedByDescending { it.createdAtMs },
            friends = updatedFriends
        ),
        message = "You and $displayName are now friends"
    )
}

private fun handleFriendRequestList(
    obj: JsonObject,
    currentState: FriendUiState,
    gson: Gson
): FriendStateUpdate {
    val requestsElement = obj.get("requests")
    if (requestsElement == null || !requestsElement.isJsonArray) {
        return FriendStateUpdate(currentState)
    }
    val selfId = obj.get("selfId")?.asLong
    if (selfId == null) {
        return FriendStateUpdate(currentState)
    }
    val dtoArray: Array<FriendRequestViewDto> = gson.fromJson(requestsElement, Array<FriendRequestViewDto>::class.java)
    val incoming = mutableListOf<FriendRequestUiModel>()
    val outgoing = mutableListOf<FriendRequestUiModel>()
    dtoArray.forEach { dto ->
        val reqId = dto.requestId ?: return@forEach
        val requesterId = dto.requesterId ?: -1L
        val receiverId = dto.receiverId ?: -1L
        val status = when (dto.status) {
            1 -> FriendRequestStatus.ACCEPTED
            2 -> FriendRequestStatus.REJECTED
            else -> FriendRequestStatus.PENDING
        }
        val model = FriendRequestUiModel(
            requestId = reqId,
            requesterId = requesterId,
            receiverId = receiverId,
            message = dto.message ?: "",
            status = status,
            createdAtMs = dto.createdAtMs ?: System.currentTimeMillis(),
            convId = dto.convId,
            requesterName = null,
            requesterAvatar = null,
            receiverName = null,
            isIncoming = receiverId == selfId
        )
        if (status == FriendRequestStatus.PENDING && model.isIncoming) {
            incoming += model
        } else if (status == FriendRequestStatus.PENDING || status == FriendRequestStatus.REJECTED) {
            outgoing += model.copy(isIncoming = false)
        }
    }
    val updatedState = currentState.copy(
        incomingRequests = incoming.sortedByDescending { it.createdAtMs },
        outgoingRequests = mergeOutgoingWithExisting(currentState, outgoing)
    )
    return FriendStateUpdate(updatedState, message = "Friend requests synced")
}

private fun handleFriendList(
    obj: JsonObject,
    currentState: FriendUiState,
    gson: Gson
): FriendStateUpdate {
    val friendsElement: JsonElement = obj.get("friends") ?: return FriendStateUpdate(currentState)
    if (!friendsElement.isJsonArray) return FriendStateUpdate(currentState)
    val dtoArray: Array<FriendSummaryDto> = gson.fromJson(friendsElement, Array<FriendSummaryDto>::class.java)
    val friends = dtoArray.mapNotNull { dto ->
        dto.userId?.let { id ->
            FriendSummary(
                userId = id,
                username = dto.username,
                avatarUrl = dto.avatarUrl,
                convId = dto.convId,
                since = dto.since
            )
        }
    }.distinctBy { it.userId }
        .sortedBy { it.username ?: it.userId.toString() }
    return FriendStateUpdate(
        currentState.copy(friends = friends),
        message = "Friend list synced"
    )
}

private fun mergeOutgoingWithExisting(
    currentState: FriendUiState,
    incoming: List<FriendRequestUiModel>
): List<FriendRequestUiModel> {
    if (incoming.isEmpty()) return currentState.outgoingRequests
    val existingById = currentState.outgoingRequests.associateBy { it.requestId }
    val merged = incoming.map { req ->
        val existing = existingById[req.requestId]
        if (existing != null) {
            existing.copy(
                status = req.status,
                message = req.message.ifBlank { existing.message },
                createdAtMs = req.createdAtMs,
                convId = req.convId,
                receiverId = if (req.receiverId > 0) req.receiverId else existing.receiverId
            )
        } else {
            req
        }
    }
    return (existingById.values.filter { existing -> incoming.none { it.requestId == existing.requestId } } + merged)
        .sortedByDescending { it.createdAtMs }
}

private fun <T> MutableList<T>.removeFirstOrNull(): T? = if (isNotEmpty()) removeAt(0) else null
