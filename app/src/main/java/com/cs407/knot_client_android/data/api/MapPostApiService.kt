package com.cs407.knot_client_android.data.api

import com.cs407.knot_client_android.data.model.request.NearbyRequest
import com.cs407.knot_client_android.data.model.request.NearbyRequestV2
import com.cs407.knot_client_android.data.model.response.NearbyMapPostsResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 地图帖子 API 服务接口
 */
interface MapPostApiService {
    /**
     * 获取附近的帖子 (V1 - 基于 geohash)
     */
    @POST("/api/mapPost/nearby")
    suspend fun getNearbyPosts(
        @Header("Authorization") token: String,
        @Body request: NearbyRequest
    ): NearbyMapPostsResponse
    
    /**
     * 获取附近的帖子 (V2 - 基于 radius)
     */
    @POST("/api/mapPost/v2/nearby")
    suspend fun getNearbyPostsV2(
        @Header("Authorization") token: String,
        @Body request: NearbyRequestV2
    ): NearbyMapPostsResponse
}

