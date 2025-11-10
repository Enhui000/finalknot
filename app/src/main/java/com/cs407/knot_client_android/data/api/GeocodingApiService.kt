package com.cs407.knot_client_android.data.api

import com.cs407.knot_client_android.data.model.response.GeocodingResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mapbox Geocoding API 服务接口
 */
interface GeocodingApiService {
    /**
     * 反向地理编码：根据经纬度获取地名
     */
    @GET("geocoding/v5/mapbox.places/{longitude},{latitude}.json")
    suspend fun reverseGeocode(
        @Path("longitude") longitude: Double,
        @Path("latitude") latitude: Double,
        @Query("access_token") accessToken: String,
        @Query("limit") limit: Int = 1
    ): GeocodingResponse

    companion object {
        /**
         * 创建 Geocoding API 实例
         */
        fun create(): GeocodingApiService {
            return Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeocodingApiService::class.java)
        }
    }
}

