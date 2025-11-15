package com.cs407.knot_client_android.ui.chat

data class ChatMessage(
    val convId: Long,
    val msgId: Long?,
    val clientMsgId: String,
    val senderId: Long,
    val content: String,
    val timestamp: Long,
    val isOwn: Boolean,
    val status: MessageStatus
)

enum class MessageStatus { PENDING, SENT, FAILED }
