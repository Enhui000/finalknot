package com.cs407.knot_client_android.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_tokens", Context.MODE_PRIVATE)

//    fun save(accessToken: String, refreshToken: String) {
//        prefs.edit()
//            .putString("access_token", accessToken)
//            .putString("refresh_token", refreshToken)
//            .apply()
//    }
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
            }
            if (username != null) {
                putString("username", username)
            }
        }.apply()
    }

    /** 单独获取 access token */
    fun getAccessToken(): String? = prefs.getString("access_token", null)

    /** 单独获取 refresh token */
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    /** 如果你只想要 access token，也可以保留这个简化别名 */
    fun get(): String? = getAccessToken()

    fun getUserId(): Long? =
        if (prefs.contains("user_id")) prefs.getLong("user_id", -1L) else null

    fun getUsername(): String? = prefs.getString("username", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
