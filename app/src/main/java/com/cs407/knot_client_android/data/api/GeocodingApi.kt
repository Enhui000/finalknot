package com.cs407.knot_client_android.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class GeocodingResponse(
    val features: List<Feature>?
)

data class Feature(
    val place_name: String?,
    val text: String?
)

interface MapboxGeocodingApi {
    @GET("geocoding/v5/mapbox.places/{longitude},{latitude}.json")
    suspend fun reverseGeocode(
        @Path("longitude") longitude: Double,
        @Path("latitude") latitude: Double,
        @Query("access_token") accessToken: String,
        @Query("limit") limit: Int = 1
    ): GeocodingResponse

    companion object {
        fun create(): MapboxGeocodingApi {
            return Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MapboxGeocodingApi::class.java)
        }
    }
}

