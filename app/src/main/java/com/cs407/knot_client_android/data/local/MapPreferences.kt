package com.cs407.knot_client_android.data.local

import android.content.Context
import android.content.SharedPreferences

class MapPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "map_preferences",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_LAST_LATITUDE = "last_latitude"
        private const val KEY_LAST_LONGITUDE = "last_longitude"
        private const val KEY_LAST_ZOOM = "last_zoom"
        
        // 默认位置：Mountain View (假数据中心区域)
        private const val DEFAULT_LATITUDE = 37.424
        private const val DEFAULT_LONGITUDE = -122.087
        private const val DEFAULT_ZOOM = 7.0 // 更大视野，能看到整个区域
    }
    
    fun saveMapPosition(latitude: Double, longitude: Double, zoom: Double) {
        prefs.edit().apply {
            putFloat(KEY_LAST_LATITUDE, latitude.toFloat())
            putFloat(KEY_LAST_LONGITUDE, longitude.toFloat())
            putFloat(KEY_LAST_ZOOM, zoom.toFloat())
            apply()
        }
    }
    
    fun getLastLatitude(): Double {
        return prefs.getFloat(KEY_LAST_LATITUDE, DEFAULT_LATITUDE.toFloat()).toDouble()
    }
    
    fun getLastLongitude(): Double {
        return prefs.getFloat(KEY_LAST_LONGITUDE, DEFAULT_LONGITUDE.toFloat()).toDouble()
    }
    
    fun getLastZoom(): Double {
        return prefs.getFloat(KEY_LAST_ZOOM, DEFAULT_ZOOM.toFloat()).toDouble()
    }
    
    fun hasLastPosition(): Boolean {
        return prefs.contains(KEY_LAST_LATITUDE)
    }
}

