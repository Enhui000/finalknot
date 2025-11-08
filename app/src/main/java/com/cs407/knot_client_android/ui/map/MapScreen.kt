package com.cs407.knot_client_android.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cs407.knot_client_android.utils.LocationManager
import com.mapbox.geojson.Point
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val scope = rememberCoroutineScope()
    
    // 位置状态
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var hasPermission by remember { mutableStateOf(locationManager.hasLocationPermission()) }
    
    // 地图视口状态
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(13.0)
            center(Point.fromLngLat(-89.4, 43.07)) // 默认麦迪逊坐标
            pitch(0.0)
            bearing(0.0)
        }
    }
    
    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it }
        if (hasPermission) {
            // 获取当前位置
            scope.launch {
                val location = locationManager.getCurrentLocation()
                location?.let {
                    val point = Point.fromLngLat(it.longitude, it.latitude)
                    userLocation = point
                    // 更新地图中心到用户位置
                    mapViewportState.setCameraOptions {
                        center(point)
                        zoom(15.0)
                    }
                }
            }
        }
    }
    
    // 实时位置更新
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            locationManager.getLocationUpdates().collect { location ->
                userLocation = Point.fromLngLat(location.longitude, location.latitude)
            }
        }
    }
    
    // 首次加载时请求权限
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // 已有权限，直接获取位置
            val location = locationManager.getCurrentLocation()
            location?.let {
                val point = Point.fromLngLat(it.longitude, it.latitude)
                userLocation = point
                mapViewportState.setCameraOptions {
                    center(point)
                    zoom(15.0)
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 地图内容 - 使用 MapStyle
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = {
                MapStyle(style = Style.MAPBOX_STREETS)
            },
            compass = {
                // 隐藏指南针
            },
            logo = {
                // 隐藏 Mapbox logo
            },
            scaleBar = {
                // 隐藏比例尺
            },
            attribution = {
                // 隐藏 attribution
            }
        )
        
        // 显示当前位置信息（调试用）
        userLocation?.let { location ->
            Text(
                text = "location: ${location.latitude()}, ${location.longitude()}",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 52.dp, start = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen(navController = rememberNavController())
}
