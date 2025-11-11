package com.cs407.knot_client_android.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * 通用 API 响应包装类
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: String?
)

