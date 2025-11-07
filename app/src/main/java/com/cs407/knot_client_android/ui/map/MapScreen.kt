package com.cs407.knot_client_android.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@Composable
fun MapScreen(
    navController: NavHostController
) {
    // 禁用侧滑返回
    BackHandler(enabled = true) {
        // 什么都不做，阻止返回
    }
    
    MapboxMap(
        Modifier.fillMaxSize(),
        mapViewportState = rememberMapViewportState {
            setCameraOptions {
                zoom(13.0) // 城市级别缩放
                center(Point.fromLngLat(-89.4, 43.07)) // 麦迪逊坐标
                pitch(0.0)
                bearing(0.0)
            }
        },
        logo = {
            // 隐藏 Mapbox logo
        },
        scaleBar = {
            // 隐藏比例尺（经纬度显示）
        },
        attribution = {
            // 隐藏 attribution
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen(navController = rememberNavController())
}
