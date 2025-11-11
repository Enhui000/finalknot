package com.cs407.knot_client_android.data.model

/**
 * WebSocket 消息基类
 */
data class WebSocketMessage(
    val type: String
)

/**
 * 地图帖子创建通知 (MAP_POST_NEW)
 */
data class MapPostNewMessage(
    val type: String,
    val mapPostId: Long,
    val convId: Long,
    val creatorId: Long,
    val creatorUsername: String,
    val creatorAvatar: String?,
    val title: String,
    val description: String,
    val mediaUrls: List<String>?,
    val loc: LocationInfo,
    val createdAtMs: Long
)

/**
 * 位置信息
 */
data class LocationInfo(
    val lat: Double,
    val lng: Double,
    val name: String
)

/**
 * 新消息通知 (MSG_NEW)
 */
data class MessageNewMessage(
    val type: String,
    val convId: Long,
    val fromUid: Long,
    val msgId: Long,
    val contentText: String?
)

