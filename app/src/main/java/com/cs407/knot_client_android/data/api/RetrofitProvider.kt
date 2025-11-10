package com.cs407.knot_client_android.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit 提供者
 * 统一创建和配置 Retrofit 实例
 */
object RetrofitProvider {
    /**
     * 创建带日志的 OkHttpClient
     */
    private fun createLoggingClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    /**
     * 创建认证 API 服务
     * @param baseUrl 后端地址，模拟器使用 http://10.0.2.2:8080
     */
    fun createAuthService(baseUrl: String): AuthApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createLoggingClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    /**
     * 创建 Geocoding API 服务
     */
    fun createGeocodingService(): GeocodingApiService {
        return GeocodingApiService.create()
    }
}
