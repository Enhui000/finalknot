package com.cs407.knot_client_android.data.api

import com.cs407.knot_client_android.data.model.response.UserSettingsResponse
import com.cs407.knot_client_android.data.model.request.UpdateUserSettingsRequest
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Body

/**
 * 用户 API 服务接口
 */
interface UserApiService {
    /**
     * 获取用户设置
     * @param authorization Bearer token
     */
    @GET("/api/user/settings")
    suspend fun getUserSettings(
        @Header("Authorization") authorization: String
    ): UserSettingsResponse

    @PUT("/api/user/settings")
    suspend fun updateUserSettings(
        @Header("Authorization") authorization: String,
        @Body request: UpdateUserSettingsRequest
    ): UserSettingsResponse
}

