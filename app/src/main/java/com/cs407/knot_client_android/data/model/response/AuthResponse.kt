package com.cs407.knot_client_android.data.model.response

/**
 * 登录响应
 */
data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val data: TokenData?
)

/**
 * Token 数据
 */
data class TokenData(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String?,
    val userId: Int?,
    val username: String?,
    val error: String?
)

/**
 * 通用响应（注册等）
 */
data class CommonResponse(
    val success: Boolean = true,
    val message: String? = null
)

