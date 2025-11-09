package com.cs407.knot_client_android.data.model

/**
 * 地图帖子数据模型
 * 对应后端 map_post 表结构
 */
data class MapPost(
    val mapPostId: Long,
    val convId: Long,
    val creatorId: Long,
    val title: String,
    val description: String,
    val mediaJson: List<String>? = null,
    val locLat: Double,
    val locLng: Double,
    val locName: String,
    val geohash: String? = null,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val status: Int = 1, // 0=已删除 1=已发布
    val createdAt: String,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
    val postType: PostType = PostType.ALL
)

enum class PostType {
    ALL,      // 所有人可见
    REQUEST,  // 请求类型
    COMMENT   // 评论类型
}

