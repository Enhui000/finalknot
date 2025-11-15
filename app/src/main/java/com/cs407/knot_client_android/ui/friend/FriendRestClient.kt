package com.cs407.knot_client_android.ui.friend

import android.content.Context
import com.cs407.knot_client_android.data.local.TokenStore
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private const val FRIEND_HTTP_BASE = "http://localhost:8080"
private const val FRIEND_LIST_PATH = "/api/friends/list"
private const val FRIEND_REQUEST_LIST_PATH = "/api/friends/request/list"

class FriendRestClient(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStore = TokenStore(appContext)
    private val gson = Gson()
    private val client: OkHttpClient by lazy { OkHttpClient() }

    suspend fun fetchSnapshot(): FriendSnapshot = withContext(Dispatchers.IO) {
        val token = tokenStore.getAccessToken() ?: throw IllegalStateException("Not authenticated; unable to fetch friends")
        val bearer = "Bearer $token"

        val friendEnvelope = executeGet(FRIEND_LIST_PATH, bearer)
        if (!friendEnvelope.success) {
            throw IllegalStateException(friendEnvelope.message ?: friendEnvelope.error ?: "Failed to load friend list")
        }
        val friends = parseFriendList(friendEnvelope.data)

        val requestEnvelope = executeGet(FRIEND_REQUEST_LIST_PATH, bearer)
        if (!requestEnvelope.success) {
            throw IllegalStateException(requestEnvelope.message ?: requestEnvelope.error ?: "Failed to load friend requests")
        }
        val userId = tokenStore.getUserId()
        val (incoming, outgoing) = parseRequestLists(requestEnvelope.data, userId)

        FriendSnapshot(
            friends = friends,
            incomingRequests = incoming,
            outgoingRequests = outgoing
        )
    }

    private suspend fun executeGet(path: String, bearer: String): ApiEnvelope<JsonElement> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(FRIEND_HTTP_BASE + path)
            .header("Authorization", bearer)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body?.string() ?: "{}"
            val type = object : TypeToken<ApiEnvelope<JsonElement>>() {}.type
            return@use gson.fromJson<ApiEnvelope<JsonElement>>(body, type)
        }
    }

    private fun parseFriendList(element: JsonElement?): List<FriendSummary> {
        val array = when {
            element == null || element.isJsonNull -> return emptyList()
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject -> element.asJsonObject.getAsJsonArray("items") ?: element.asJsonObject.getAsJsonArray("data")
            else -> null
        } ?: return emptyList()

        return array.mapNotNull { parseFriendSummary(it) }
            .distinctBy { it.userId }
            .sortedBy { it.username ?: it.userId.toString() }
    }

    private fun parseFriendSummary(element: JsonElement): FriendSummary? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val userId = obj.getAsLongOrNull("userId") ?: obj.getAsJsonObject("friend")?.getAsLongOrNull("userId")
        ?: return null
        val username = obj.getAsStringOrNull("username")
            ?: obj.getAsJsonObject("friend")?.getAsStringOrNull("username")
        val avatar = obj.getAsStringOrNull("avatarUrl")
            ?: obj.getAsJsonObject("friend")?.getAsStringOrNull("avatarUrl")
        val convId = obj.getAsLongOrNull("convId") ?: obj.getAsJsonObject("conversation")?.getAsLongOrNull("id")
        val since = obj.getAsLongOrNull("since")
            ?: obj.getAsLongOrNull("createdAt")
            ?: obj.getAsLongOrNull("createdAtMs")

        return FriendSummary(
            userId = userId,
            username = username,
            avatarUrl = avatar,
            convId = convId,
            since = since
        )
    }

    private fun parseRequestLists(element: JsonElement?, currentUserId: Long?): Pair<List<FriendRequestUiModel>, List<FriendRequestUiModel>> {
        if (element == null || element.isJsonNull) return emptyList<FriendRequestUiModel>() to emptyList()

        val incoming = mutableListOf<FriendRequestUiModel>()
        val outgoing = mutableListOf<FriendRequestUiModel>()

        fun addRequests(array: JsonArray?) {
            array?.forEach { item ->
                parseRequest(item, currentUserId)?.let { model ->
                    if (model.isIncoming) {
                        incoming += model
                    } else {
                        outgoing += model
                    }
                }
            }
        }

        when {
            element.isJsonArray -> addRequests(element.asJsonArray)
            element.isJsonObject -> {
                val obj = element.asJsonObject
                addRequests(obj.getAsJsonArray("incoming"))
                addRequests(obj.getAsJsonArray("outgoing"))
                addRequests(obj.getAsJsonArray("requests"))
                obj.get("data")?.let { dataElem ->
                    if (dataElem.isJsonArray) {
                        addRequests(dataElem.asJsonArray)
                    } else if (dataElem.isJsonObject) {
                        addRequests(dataElem.asJsonObject.getAsJsonArray("incoming"))
                        addRequests(dataElem.asJsonObject.getAsJsonArray("outgoing"))
                    }
                }
            }
        }

        return incoming.distinctBy { it.requestId }.sortedByDescending { it.createdAtMs } to
                outgoing.distinctBy { it.requestId }.sortedByDescending { it.createdAtMs }
    }

    private fun parseRequest(element: JsonElement, currentUserId: Long?): FriendRequestUiModel? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val requestId = obj.getAsLongOrNull("requestId") ?: obj.getAsLongOrNull("id") ?: return null
        val requesterId = obj.getAsLongOrNull("requesterId")
            ?: obj.getAsJsonObject("requester")?.getAsLongOrNull("userId")
            ?: -1L
        val receiverId = obj.getAsLongOrNull("receiverId")
            ?: obj.getAsJsonObject("receiver")?.getAsLongOrNull("userId")
            ?: -1L
        val message = obj.getAsStringOrNull("message") ?: obj.getAsStringOrNull("remark") ?: ""
        val statusValue = obj.getAsIntOrNull("status") ?: obj.getAsIntOrNull("state") ?: 0
        val createdAt = obj.getAsLongOrNull("createdAtMs")
            ?: obj.getAsLongOrNull("createdAt")
            ?: obj.getAsLongOrNull("timestamp")
            ?: System.currentTimeMillis()
        val convId = obj.getAsLongOrNull("convId") ?: obj.getAsJsonObject("conversation")?.getAsLongOrNull("id")
        val requesterName = obj.getAsStringOrNull("requesterName")
            ?: obj.getAsJsonObject("requester")?.getAsStringOrNull("username")
        val requesterAvatar = obj.getAsStringOrNull("requesterAvatar")
            ?: obj.getAsJsonObject("requester")?.getAsStringOrNull("avatarUrl")
        val receiverName = obj.getAsStringOrNull("receiverName")
            ?: obj.getAsJsonObject("receiver")?.getAsStringOrNull("username")
        val explicitIncoming = obj.getAsBooleanOrNull("isIncoming")
        val isIncoming = explicitIncoming ?: when {
            currentUserId != null && receiverId == currentUserId -> true
            currentUserId != null && requesterId == currentUserId -> false
            else -> true
        }

        val status = when (statusValue) {
            1 -> FriendRequestStatus.ACCEPTED
            2 -> FriendRequestStatus.REJECTED
            else -> FriendRequestStatus.PENDING
        }

        return FriendRequestUiModel(
            requestId = requestId,
            requesterId = requesterId,
            receiverId = receiverId,
            message = message,
            status = status,
            createdAtMs = createdAt,
            convId = convId,
            requesterName = requesterName,
            requesterAvatar = requesterAvatar,
            receiverName = receiverName,
            isIncoming = isIncoming
        )
    }
}

data class FriendSnapshot(
    val friends: List<FriendSummary>,
    val incomingRequests: List<FriendRequestUiModel>,
    val outgoingRequests: List<FriendRequestUiModel>
)

data class ApiEnvelope<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)

private fun JsonObject.getAsStringOrNull(key: String): String? =
    if (has(key) && !get(key).isJsonNull) get(key).asString else null

private fun JsonObject.getAsLongOrNull(key: String): Long? =
    if (has(key) && !get(key).isJsonNull) runCatching { get(key).asLong }.getOrNull() else null

private fun JsonObject.getAsIntOrNull(key: String): Int? =
    if (has(key) && !get(key).isJsonNull) runCatching { get(key).asInt }.getOrNull() else null

private fun JsonObject.getAsBooleanOrNull(key: String): Boolean? =
    if (has(key) && !get(key).isJsonNull) runCatching { get(key).asBoolean }.getOrNull() else null

private fun JsonObject.getAsJsonArray(key: String): JsonArray? =
    if (has(key) && get(key).isJsonArray) get(key).asJsonArray else null