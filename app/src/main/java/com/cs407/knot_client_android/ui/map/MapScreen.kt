package com.cs407.knot_client_android.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.cs407.knot_client_android.ui.components.BottomNavigationBar
import com.cs407.knot_client_android.ui.components.NavTab
import com.cs407.knot_client_android.navigation.Screen

@Composable
fun MapScreen(
    navController: NavHostController
) {
    var selectedTab by remember { mutableStateOf(NavTab.MAP) }
    
    // 禁用侧滑返回
    BackHandler(enabled = true) {
        // 什么都不做，阻止返回
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 地图内容
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
        
        // 底部导航栏 - 绝对定位在左下角，贴近设备圆角
        BottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                when (tab) {
                    NavTab.MAP -> {
                        // 已经在 Map 页面，不需要导航
                    }
                    NavTab.CHAT -> {
                        navController.navigate(Screen.Chat.route)
                    }
                    NavTab.PROFILE -> {
                        navController.navigate(Screen.Profile.route)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, bottom = 30.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen(navController = rememberNavController())
}
