package com.cs407.knot_client_android.data.api

import com.cs407.knot_client_android.data.model.request.NearbyRequest
import com.cs407.knot_client_android.data.model.request.NearbyRequestV2
import com.cs407.knot_client_android.data.model.response.ApiResponse
import com.cs407.knot_client_android.data.model.response.ConversationMessagesResponse
import com.cs407.knot_client_android.data.model.response.MapPostDetailResponse
import com.cs407.knot_client_android.data.model.response.NearbyMapPostsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
    
    /**
     * 获取帖子详情
     */
    @GET("/api/mapPost/{mapPostId}")
    suspend fun getMapPostDetail(
        @Header("Authorization") token: String,
        @Path("mapPostId") mapPostId: Long
    ): ApiResponse<MapPostDetailResponse>
    
    /**
     * 获取会话消息（评论）
     */
    @GET("/api/conversation/messages")
    suspend fun getConversationMessages(
        @Header("Authorization") token: String,
        @Query("conversationId") conversationId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<ConversationMessagesResponse>
}

