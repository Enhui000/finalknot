package com.cs407.knot_client_android.data.api

import com.cs407.knot_client_android.data.model.request.LoginRequest
import com.cs407.knot_client_android.data.model.request.RegisterRequest
import com.cs407.knot_client_android.data.model.response.CommonResponse
import com.cs407.knot_client_android.data.model.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 认证 API 服务接口
 */
interface AuthApiService {
    /**
     * 登录
     */
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    /**
     * 注册
     */
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): CommonResponse
}

