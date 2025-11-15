package com.cs407.knot_client_android.data

import android.content.Context
import com.cs407.knot_client_android.data.api.*
import com.cs407.knot_client_android.data.local.TokenStore

class AuthRepository(context: Context, baseUrl: String) {
    private val api = RetrofitProvider.createAuth(baseUrl)
    private val tokenStore = TokenStore(context)

    suspend fun login(username: String, password: String) {
        val resp = api.login(LoginReq(username, password))
        if (resp.success && resp.data != null) {
            tokenStore.save(resp.data.accessToken, resp.data.refreshToken)
        } else {
            error(resp.message ?: "Login failed")
        }
    }

    suspend fun registerThenLoginIfNeeded(username: String, password: String) {
        val resp = api.register(RegisterReq(username, password))
        if (resp.success) {
            login(username, password)
        } else {
            error(resp.message ?: "Register failed")
        }
    }
}
