package com.cs407.knot_client_android.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_tokens", Context.MODE_PRIVATE)

    fun save(
        accessToken: String,
        refreshToken: String,
        userId: Long? = null,
        username: String? = null
    ) {
        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            if (userId != null) {
                putLong("user_id", userId)
            } else {
                remove("user_id")
            }
            if (username != null) {
                putString("username", username)
            } else {
                remove("username")
            }
        }.apply()
    }

    /** 单独获取 access token */
    fun getAccessToken(): String? = prefs.getString("access_token", null)

    /** 单独获取 refresh token */
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    /** 获取当前登录用户 ID，如果尚未登录则返回 null */
    fun getUserId(): Long? =
        if (prefs.contains("user_id")) prefs.getLong("user_id", -1L).takeIf { it >= 0 } else null

    /** 获取当前登录用户名 */
    fun getUsername(): String? = prefs.getString("username", null)

    /** 如果你只想要 access token，也可以保留这个简化别名 */
    fun get(): String? = getAccessToken()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
