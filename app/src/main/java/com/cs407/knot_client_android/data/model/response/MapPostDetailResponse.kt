package com.cs407.knot_client_android.data.model.response

import com.google.gson.annotations.SerializedName

data class MapPostDetailResponse(
    @SerializedName("mapPostId") val mapPostId: Long,
    @SerializedName("convId") val convId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("mediaUrls") val mediaUrls: List<String>?,
    @SerializedName("creatorId") val creatorId: Long,
    @SerializedName("creatorUsername") val creatorUsername: String,
    @SerializedName("creatorAvatar") val creatorAvatar: String?,
    @SerializedName("locLat") val locLat: Double,
    @SerializedName("locLng") val locLng: Double,
    @SerializedName("locName") val locName: String?,
    @SerializedName("viewCount") val viewCount: Int,
    @SerializedName("likeCount") val likeCount: Int,
    @SerializedName("commentCount") val commentCount: Int,
    @SerializedName("createdAtMs") val createdAtMs: Long,
    @SerializedName("postType") val postType: String,
    @SerializedName("isLikedByCurrentUser") val isLikedByCurrentUser: Boolean
)

