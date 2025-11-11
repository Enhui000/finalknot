package com.cs407.knot_client_android.data.repository

import android.content.Context
import com.cs407.knot_client_android.data.api.RetrofitProvider
import com.cs407.knot_client_android.data.local.TokenStore
import com.cs407.knot_client_android.data.model.request.NearbyRequest
import com.cs407.knot_client_android.data.model.request.NearbyRequestV2
import com.cs407.knot_client_android.data.model.response.MapPostNearby

/**
 * 地图帖子仓库
 * 负责处理地图帖子相关业务逻辑
 */
class MapPostRepository(context: Context, baseUrl: String) {
    private val apiService = RetrofitProvider.createMapPostService(baseUrl)
    private val tokenStore = TokenStore(context)

    /**
     * 获取附近的帖子 (V1 - 基于 geohash)
     * @param lat 纬度
     * @param lng 经度
     * @param zoomLevel 缩放级别
     * @param timeRange 时间范围（默认 7D）
     * @param postType 帖子类型（默认 ALL）
     * @param maxResults 最大结果数（默认 100）
     * @return 成功返回帖子列表，失败抛出异常
     */
    suspend fun getNearbyPosts(
        lat: Double,
        lng: Double,
        zoomLevel: Int,
        timeRange: String = "7D",
        postType: String = "ALL",
        maxResults: Int = 100
    ): List<MapPostNearby> {
        val token = tokenStore.getAccessToken()
        if (token.isNullOrBlank()) {
            error("未登录，无法获取附近帖子")
        }

        val request = NearbyRequest(
            lat = lat,
            lng = lng,
            zoomLevel = zoomLevel,
            timeRange = timeRange,
            postType = postType,
            maxResults = maxResults
        )

        val response = apiService.getNearbyPosts("Bearer $token", request)
        
        if (response.success && response.data != null) {
            return response.data
        } else {
            error(response.error ?: response.message)
        }
    }
    
    /**
     * 获取附近的帖子 (V2 - 基于 radius，小数据集优化)
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米，默认 5000m）
     * @param timeRange 时间范围（默认 7D，可配置）
     * @param postType 帖子类型（默认 ALL，可配置）
     * @param maxResults 最大结果数（默认 200，固定）
     * @return 成功返回帖子列表，失败抛出异常
     */
    suspend fun getNearbyPostsV2(
        lat: Double,
        lng: Double,
        radius: Int = 5000,
        timeRange: String = "7D",
        postType: String = "ALL",
        maxResults: Int = 200
    ): List<MapPostNearby> {
        val token = tokenStore.getAccessToken()
        if (token.isNullOrBlank()) {
            error("未登录，无法获取附近帖子")
        }

        val request = NearbyRequestV2(
            lat = lat,
            lng = lng,
            radius = radius,
            timeRange = timeRange,
            postType = postType,
            maxResults = maxResults
        )

        val response = apiService.getNearbyPostsV2("Bearer $token", request)
        
        if (response.success && response.data != null) {
            return response.data
        } else {
            error(response.error ?: response.message)
        }
    }
}

