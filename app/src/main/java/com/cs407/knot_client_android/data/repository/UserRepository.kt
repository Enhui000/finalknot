package com.cs407.knot_client_android.data.repository

import android.content.Context
import com.cs407.knot_client_android.data.api.RetrofitProvider
import com.cs407.knot_client_android.data.local.TokenStore
import com.cs407.knot_client_android.data.model.request.UpdateUserSettingsRequest
import com.cs407.knot_client_android.data.model.response.UserSettings

/**
 * 用户仓库
 * 负责处理用户相关业务逻辑
 */
class UserRepository(context: Context, baseUrl: String) {
    private val apiService = RetrofitProvider.createUserService(baseUrl)
    private val tokenStore = TokenStore(context)

    /**
     * 获取用户设置
     * @return UserSettings 或 null（如果失败）
     */
    suspend fun getUserSettings(): UserSettings? {
        val accessToken = tokenStore.getAccessToken()
        if (accessToken.isNullOrEmpty()) {
            error("No access token found. Please login first.")
        }

        val response = apiService.getUserSettings("Bearer $accessToken")
        if (response.success && response.data != null) {
            return response.data
        } else {
            error(response.message ?: response.error ?: "Failed to get user settings")
        }
    }

    suspend fun updateUserSettings(request: UpdateUserSettingsRequest): UserSettings? {
        val accessToken = tokenStore.getAccessToken()
        if (accessToken.isNullOrEmpty()) {
            error("No access token found. Please login first.")
        }

        val response = apiService.updateUserSettings("Bearer $accessToken", request)
        if (response.success && response.data != null) {
            return response.data
        }
        else {
            error(response.message ?: response.error ?: "Failed to update user settings")
        }
    }
}

