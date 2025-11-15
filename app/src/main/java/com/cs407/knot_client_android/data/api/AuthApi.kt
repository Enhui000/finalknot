package com.cs407.knot_client_android.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginReq(val username: String, val password: String)

data class LoginResp(
    val success: Boolean,
    val message: String?,
    val data: TokenData?
)

data class TokenData(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String?,
    val userId: Int?,
    val username: String?,
    val error: String?
)

data class RegisterReq(val username: String, val password: String)
data class SimpleResp(val success: Boolean = true, val message: String? = null)

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body req: LoginReq): LoginResp

    @POST("/api/auth/register")
    suspend fun register(@Body req: RegisterReq): SimpleResp
}
