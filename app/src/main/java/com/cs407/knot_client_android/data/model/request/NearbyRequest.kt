package com.cs407.knot_client_android.data.model.request

/**
 * 获取附近帖子的请求参数 (V1 - 基于 geohash)
 */
data class NearbyRequest(
    val lat: Double,
    val lng: Double,
    val zoomLevel: Int,
    val timeRange: String = "7D",  // 时间范围：1D, 7D, 30D
    val postType: String = "ALL",  // 帖子类型：ALL, REQUEST, COMMENT
    val maxResults: Int = 100      // 最大结果数
)

/**
 * 获取附近帖子的请求参数 (V2 - 基于 radius)
 */
data class NearbyRequestV2(
    val lat: Double,
    val lng: Double,
    val radius: Int = 5000,        // 搜索半径（米）- 固定 5000m
    val timeRange: String = "7D",  // 时间范围：1D, 7D, 30D（可配置）
    val postType: String = "ALL",  // 帖子类型：ALL, REQUEST, COMMENT（可配置）
    val maxResults: Int = 200      // 最大结果数 - 固定 200
)

