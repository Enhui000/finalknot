package com.cs407.knot_client_android.data.model.response

import com.google.gson.annotations.SerializedName

data class ConversationMessagesResponse(
    @SerializedName("convId") val convId: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("messageList") val messageList: List<ConversationMessage>
)

data class ConversationMessage(
    @SerializedName("msgId") val msgId: Long,
    @SerializedName("convId") val convId: Long,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("msgType") val msgType: Int,
    @SerializedName("clientMsgId") val clientMsgId: String?,
    @SerializedName("contentText") val contentText: String?,
    @SerializedName("locLat") val locLat: Double?,
    @SerializedName("locLng") val locLng: Double?,
    @SerializedName("locName") val locName: String?,
    @SerializedName("locAccuracyM") val locAccuracyM: Double?,
    @SerializedName("geohash") val geohash: String?,
    @SerializedName("replyToMsgId") val replyToMsgId: Long?,
    @SerializedName("mediaUrl") val mediaUrl: String?,
    @SerializedName("mediaThumbUrl") val mediaThumbUrl: String?,
    @SerializedName("mediaMetaJson") val mediaMetaJson: String?,
    @SerializedName("msgStatus") val msgStatus: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("editedAt") val editedAt: String?,
    @SerializedName("deletedAt") val deletedAt: String?
)

