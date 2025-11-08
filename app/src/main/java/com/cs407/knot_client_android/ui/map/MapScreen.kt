package com.cs407.knot_client_android.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cs407.knot_client_android.R
import com.cs407.knot_client_android.data.api.MapboxGeocodingApi
import com.cs407.knot_client_android.utils.LocationManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    var centerLocationName by remember { mutableStateOf<String?>(null) }
    
    // 创建 Geocoding API (用于反向地理编码)
    val mapboxToken = context.getString(R.string.mapbox_access_token)
    val geocodingApi = remember { MapboxGeocodingApi.create() }
    
    // 用于节流的 Job
    var geocodingJob by remember { mutableStateOf<Job?>(null) }
    
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
                    // 平滑移动到用户位置
                    mapViewportState.easeTo(
                        cameraOptions = CameraOptions.Builder()
                            .center(point)
                            .zoom(15.0)
                            .build(),
                        animationOptions = MapAnimationOptions.mapAnimationOptions {
                            duration(1500) // 1.5秒的平滑动画
                        }
                    )
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
                // 平滑移动到用户位置
                mapViewportState.easeTo(
                    cameraOptions = CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build(),
                    animationOptions = MapAnimationOptions.mapAnimationOptions {
                        duration(1500) // 1.5秒的平滑动画
                    }
                )
            }
        }
    }
    
    // 监听地图中心和缩放变化，获取中心点地名
    LaunchedEffect(mapViewportState.cameraState) {
        val zoom = mapViewportState.cameraState?.zoom ?: return@LaunchedEffect
        val center = mapViewportState.cameraState?.center ?: return@LaunchedEffect
        
        // 取消之前的请求（节流）
        geocodingJob?.cancel()
        
        // 只在 zoom > 12 时显示地名
        if (zoom > 12.0) {
            // 延迟 800ms 后再执行（用户停止拖动后才请求）
            geocodingJob = launch {
                delay(800)
                
                try {
                    // 执行反向地理编码
                    val response = geocodingApi.reverseGeocode(
                        longitude = center.longitude(),
                        latitude = center.latitude(),
                        accessToken = mapboxToken
                    )
                    
                    // 提取简短地名（例如：Monterey, Mountain View）
                    val placeName = response.features?.firstOrNull()?.place_name
                    if (placeName != null) {
                        // 分割地名，只保留前两部分（城市和州/地区）
                        val parts = placeName.split(",").take(2)
                        centerLocationName = parts.joinToString("\n").trim()
                    } else {
                        centerLocationName = null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    centerLocationName = null
                }
            }
        } else {
            // zoom <= 12 时隐藏地名
            centerLocationName = null
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
        ) {
            // 用户位置指示器
            userLocation?.let { location ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(location)
                    }
                ) {
                    // 创建无限循环的呼吸动画
                    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
                    
                    // 微妙的缩放动画 (0.90 到 1.10)
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.90f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500), // 1.5秒一个循环
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    // 外圈扩散效果的透明度
                    val outerAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )
                    
                    // 外圈扩散效果的缩放
                    val outerScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "outerScale"
                    )
                    
                    Box(contentAlignment = Alignment.Center) {
                        // 外圈扩散效果（微妙的）
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(42.dp))
                                .scale(outerScale)
                                .alpha(outerAlpha)
                                .background(Color(0xFF4A90E2).copy(alpha = 0.3f), CircleShape)
                        )
                        
                        // 主要的位置指示器 - 蓝色圆点带白色边框
                        Box(
                            modifier = Modifier
                                .size(27.dp)
                                .scale(scale)
                                .border(4.dp, Color.White, CircleShape)
                                .background(Color(0xFF4A90E2), CircleShape)
                        )
                    }
                }
            }
        }
        
        // 显示中心点地名（只在 zoom > 12 时显示）- 带优雅的进入和退出动画
        // 使用 remember 保存最后一个非空的地名，用于 exit 动画
        var displayedLocationName by remember { mutableStateOf("") }
        
        LaunchedEffect(centerLocationName) {
            centerLocationName?.let {
                displayedLocationName = it
            }
        }
        
        AnimatedVisibility(
            visible = centerLocationName != null,
            enter = fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 400
                )
            ) + scaleOut(
                targetScale = 0.6f,
                animationSpec = tween(
                    durationMillis = 400
                )
            ),
            label = "LocationNameVisibility",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 85.dp, start = 24.dp)
        ) {
            val lines = displayedLocationName.split("\n")
            val mainName = lines.getOrNull(0)?.trim() ?: ""
            val subName = lines.getOrNull(1)?.trim() ?: ""

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 使用 Box 让阴影和主文字重叠
                Box {
                    // 阴影文字
                    Text(
                        text = subName,
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .alpha(0.6f),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    // 主文字
                    Text(
                        text = subName,
                        color = Color.Black.copy(alpha = 0.8f),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.padding(5.dp))
                // 次标题 - 小一号，灰一点
                if (subName.isNotEmpty()) {
                    Text(
                        text = mainName,
                        color = Color(0x99333333), // 60% 深灰
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // DEBUG: DO NOT DELETE THIS CODE
        // // 显示当前位置信息（调试用）- 白色半透明背景
        // userLocation?.let { location ->
        //     Text(
        //         text = "位置: ${location.latitude()}, ${location.longitude()}",
        //         modifier = Modifier
        //             .align(Alignment.TopCenter)
        //             .padding(16.dp)
        //             .background(
        //                 color = Color.White.copy(alpha = 0.9f),
        //                 shape = RoundedCornerShape(8.dp)
        //             )
        //             .padding(horizontal = 12.dp, vertical = 6.dp),
        //         color = Color.Black,
        //         fontSize = 12.sp
        //     )
        // }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen(navController = rememberNavController())
}
