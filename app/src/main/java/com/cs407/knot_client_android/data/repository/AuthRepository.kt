package com.cs407.knot_client_android.data.repository

import android.content.Context
import com.cs407.knot_client_android.data.api.RetrofitProvider
import com.cs407.knot_client_android.data.local.TokenStore
import com.cs407.knot_client_android.data.model.request.LoginRequest
import com.cs407.knot_client_android.data.model.request.RegisterRequest

/**
 * 认证仓库
 * 负责处理登录、注册等认证相关业务逻辑
 */
class AuthRepository(context: Context, baseUrl: String) {
    private val apiService = RetrofitProvider.createAuthService(baseUrl)
    private val tokenStore = TokenStore(context)

    /**
     * 登录
     */
    suspend fun login(username: String, password: String) {
        val response = apiService.login(LoginRequest(username, password))
        if (response.success && response.data != null) {
            tokenStore.save(response.data.accessToken, response.data.refreshToken)
        } else {
            error(response.message ?: "Login failed")
        }
    }

    /**
     * 注册并自动登录
     */
    suspend fun registerThenLoginIfNeeded(username: String, password: String) {
        val response = apiService.register(RegisterRequest(username, password))
        if (response.success) {
            login(username, password)
        } else {
            error(response.message ?: "Register failed")
        }
    }

    /**
     * 获取 Access Token
     */
    fun getAccessToken(): String? = tokenStore.getAccessToken()

    /**
     * 获取 Refresh Token
     */
    fun getRefreshToken(): String? = tokenStore.getRefreshToken()

    /**
     * 清除登录状态
     */
    fun logout() {
        tokenStore.clear()
    }
}

