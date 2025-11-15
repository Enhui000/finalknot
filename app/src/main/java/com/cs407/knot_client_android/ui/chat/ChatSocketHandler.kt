package com.cs407.knot_client_android.ui.chat

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale

sealed interface ChatSocketEvent

data class ChatIncomingEvent(
    val convId: Long,
    val msgId: Long?,
    val fromUid: Long,
    val content: String,
    val serverTime: Long?
) : ChatSocketEvent

data class ChatAckEvent(
    val clientMsgId: String,
    val msgId: Long?,
    val serverTime: Long?
) : ChatSocketEvent

data class ChatInfoEvent(
    val message: String,
    val isError: Boolean
) : ChatSocketEvent

fun parseChatSocketMessage(raw: String): ChatSocketEvent? {
    return try {
        val element = JsonParser.parseString(raw)
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val type = obj.get("type")?.asString?.uppercase(Locale.ROOT) ?: return null
        when (type) {
            "MSG_NEW" -> obj.toIncomingEvent()
            "MSG_ACK" -> obj.toAckEvent()
            "MSG_ERROR" -> ChatInfoEvent(
                message = obj.get("message")?.asString ?: "Message failed",
                isError = true
            )
            else -> null
        }
    } catch (e: Exception) {
        ChatInfoEvent(message = "Failed to parse chat payload: ${e.message ?: "unknown"}", isError = true)
    }
}

private fun JsonObject.toIncomingEvent(): ChatIncomingEvent? {
    val convId = this.get("convId")?.asLong ?: this.get("conversationId")?.asLong ?: return null
    val fromUid = this.get("fromUid")?.asLong ?: this.get("fromUserId")?.asLong ?: return null
    val msgId = this.get("msgId")?.asLong
    val content = this.get("contentText")?.asString
        ?: this.get("content")?.asString
        ?: this.get("text")?.asString
        ?: ""
    val serverTime = this.get("serverTime")?.asLong ?: this.get("timestamp")?.asLong
    return ChatIncomingEvent(
        convId = convId,
        msgId = msgId,
        fromUid = fromUid,
        content = content,
        serverTime = serverTime
    )
}

private fun JsonObject.toAckEvent(): ChatAckEvent? {
    val clientMsgId = this.get("clientMsgId")?.asString ?: return null
    val msgId = this.get("msgId")?.asLong
    val serverTime = this.get("serverTime")?.asLong
    return ChatAckEvent(
        clientMsgId = clientMsgId,
        msgId = msgId,
        serverTime = serverTime
    )
}
