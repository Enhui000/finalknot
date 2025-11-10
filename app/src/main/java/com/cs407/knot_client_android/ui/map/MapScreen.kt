package com.cs407.knot_client_android.ui.map

import android.Manifest
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Place
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cs407.knot_client_android.R
import com.cs407.knot_client_android.data.api.GeocodingApiService
import com.cs407.knot_client_android.data.local.MapPreferences
import com.cs407.knot_client_android.data.model.MapPost
import com.cs407.knot_client_android.data.model.PostType
import com.cs407.knot_client_android.ui.components.MapMarker
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
    val mapPreferences = remember { MapPreferences(context) }
    val scope = rememberCoroutineScope()
    
    // 位置状态
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var hasPermission by remember { mutableStateOf(locationManager.hasLocationPermission()) }
    var centerLocationName by remember { mutableStateOf<String?>(null) }
    
    // 假数据：多个地图帖子（在 Mountain View 区域）
    val mockMapPosts = remember {
        listOf(
            MapPost(
                mapPostId = 1,
                convId = 101,
                creatorId = 1001,
                title = "Best Coffee ☕",
                description = "Amazing latte art and cozy atmosphere!",
                mediaJson = listOf("url1", "url2"),
                locLat = 37.422,
                locLng = -122.084,
                locName = "Google Play Store",
                geohash = "9q9hvnf",
                viewCount = 156,
                likeCount = 42,
                commentCount = 8,
                status = 1,
                createdAt = "2024-11-09T10:30:00Z",
                postType = PostType.ALL
            ),
            MapPost(
                mapPostId = 2,
                convId = 102,
                creatorId = 1002,
                title = "Tech Meetup 🚀",
                description = "Weekly tech talks and networking",
                mediaJson = null,
                locLat = 37.425,
                locLng = -122.088,
                locName = "Mountain View Library",
                geohash = "9q9hvng",
                viewCount = 89,
                likeCount = 27,
                commentCount = 15,
                status = 1,
                createdAt = "2024-11-09T14:15:00Z",
                postType = PostType.REQUEST
            ),
            MapPost(
                mapPostId = 3,
                convId = 103,
                creatorId = 1003,
                title = "Yoga Class 🧘",
                description = "Morning yoga sessions every weekend",
                mediaJson = listOf("url3"),
                locLat = 37.427,
                locLng = -122.086,
                locName = "Shoreline Park",
                geohash = "9q9hvnh",
                viewCount = 234,
                likeCount = 68,
                commentCount = 22,
                status = 1,
                createdAt = "2024-11-09T08:00:00Z",
                postType = PostType.ALL
            ),
            MapPost(
                mapPostId = 4,
                convId = 104,
                creatorId = 1004,
                title = "Food Truck 🌮",
                description = "Best tacos in town!",
                mediaJson = null,
                locLat = 37.423,
                locLng = -122.090,
                locName = "Castro Street",
                geohash = "9q9hvne",
                viewCount = 312,
                likeCount = 95,
                commentCount = 41,
                status = 1,
                createdAt = "2024-11-09T12:00:00Z",
                postType = PostType.ALL
            ),
            MapPost(
                mapPostId = 5,
                convId = 105,
                creatorId = 1005,
                title = "Book Club 📚",
                description = "Monthly book discussions",
                mediaJson = listOf("url4", "url5"),
                locLat = 37.420,
                locLng = -122.082,
                locName = "Public Library",
                geohash = "9q9hvnc",
                viewCount = 145,
                likeCount = 38,
                commentCount = 19,
                status = 1,
                createdAt = "2024-11-09T16:30:00Z",
                postType = PostType.REQUEST
            ),
            MapPost(
                mapPostId = 6,
                convId = 106,
                creatorId = 1006,
                title = "Art Gallery 🎨",
                description = "Local artists exhibition",
                mediaJson = null,
                locLat = 37.428,
                locLng = -122.089,
                locName = "Art Center",
                geohash = "9q9hvni",
                viewCount = 198,
                likeCount = 52,
                commentCount = 28,
                status = 1,
                createdAt = "2024-11-09T13:45:00Z",
                postType = PostType.ALL
            ),
            MapPost(
                mapPostId = 7,
                convId = 107,
                creatorId = 1007,
                title = "Bike Repair 🚴",
                description = "Free bike maintenance workshop",
                mediaJson = listOf("url6"),
                locLat = 37.419,
                locLng = -122.085,
                locName = "Community Center",
                geohash = "9q9hvnb",
                viewCount = 167,
                likeCount = 44,
                commentCount = 13,
                status = 1,
                createdAt = "2024-11-09T09:15:00Z",
                postType = PostType.REQUEST
            ),
            MapPost(
                mapPostId = 8,
                convId = 108,
                creatorId = 1008,
                title = "Live Music 🎵",
                description = "Jazz night every Friday",
                mediaJson = null,
                locLat = 37.426,
                locLng = -122.091,
                locName = "Music Venue",
                geohash = "9q9hvnj",
                viewCount = 276,
                likeCount = 82,
                commentCount = 35,
                status = 1,
                createdAt = "2024-11-09T18:00:00Z",
                postType = PostType.ALL
            )
        )
    }
    
    // 创建 Geocoding API (用于反向地理编码)
    val mapboxToken = context.getString(R.string.mapbox_access_token)
    val geocodingApi = remember { GeocodingApiService.create() }
    
    // 用于节流的 Job
    var geocodingJob by remember { mutableStateOf<Job?>(null) }
    
    // 地图视口状态 - 使用上次保存的位置
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(mapPreferences.getLastZoom())
            center(Point.fromLngLat(
                mapPreferences.getLastLongitude(),
                mapPreferences.getLastLatitude()
            ))
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
    
    // 监听地图中心和缩放变化，获取中心点地名 + 保存位置
    LaunchedEffect(mapViewportState.cameraState) {
        val zoom = mapViewportState.cameraState?.zoom ?: return@LaunchedEffect
        val center = mapViewportState.cameraState?.center ?: return@LaunchedEffect
        
        // 取消之前的请求（节流）
        geocodingJob?.cancel()
        
        // 启动新的任务
        geocodingJob = launch {
            // 延迟 800ms 后执行（用户停止拖动后才执行）
            delay(800)
            
            // 保存当前地图位置
            mapPreferences.saveMapPosition(
                latitude = center.latitude(),
                longitude = center.longitude(),
                zoom = zoom
            )
            
            // 只在 zoom > 12 时显示地名
            if (zoom > 12.0) {
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
            } else {
                // zoom <= 12 时隐藏地名
                centerLocationName = null
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
            
            // 显示地图帖子 Markers
            mockMapPosts.forEach { post ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(post.locLng, post.locLat))
                    }
                ) {
                    MapMarker(
                        post = post,
                        onClick = {
                            // TODO: 点击 marker 后打开帖子详情
                            // navController.navigate("post_detail/${post.mapPostId}")
                        }
                    )
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


        // 在地图右上角显示一个按钮，点击之后可以重定向到用户当前位置
        // 模仿 FloatingActionButton 的样式和动画
        if (userLocation != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            // Apple-style 双阶段弹性动画
            val scale = remember { Animatable(1f) }
            
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    // 按下：快速放大一点点
                    scale.animateTo(
                        targetValue = 1.2f,
                        animationSpec = tween(
                            durationMillis = 170,
                            easing = LinearOutSlowInEasing
                        )
                    )
                } else {
                    // 松手：先缩回一点再弹回 1
                    scale.animateTo(
                        targetValue = 0.88f,
                        animationSpec = tween(
                            durationMillis = 155,
                            easing = FastOutLinearInEasing
                        )
                    )
                    // 然后自然回弹到 1
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 68.dp, end = 18.dp)
            ) {
                // 毛玻璃背景层 - Android 原生系统级模糊
                Box(
                    modifier = Modifier
                        // .size(45.dp)
                        .height(70.dp)
                        .width(40.dp)
                        .clip(CircleShape)
                        .graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                        .background(Color.White.copy(alpha = 0.65f))
                )
                
                // 主按钮
                Box(
                    modifier = Modifier
                        // .size(45.dp)
                        .height(70.dp)
                        .width(40.dp)
                        .scale(scale.value)
                        .border(
                            width = 0.5.dp,
                            color = Color(0xFFE5E7EB).copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .clickable(
                            onClick = {
                                scope.launch {
                                    userLocation?.let { location ->
                                        mapViewportState.easeTo(
                                            cameraOptions = CameraOptions.Builder()
                                                .center(location)
                                                .zoom(15.0)
                                                .build(),
                                            animationOptions = MapAnimationOptions.mapAnimationOptions {
                                                duration(2500) // 2.5秒的平滑动画
                                            }
                                        )
                                    }
                                }
                            },
                            indication = null,
                            interactionSource = interactionSource
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "current location",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(-45f), // 旋转 45 度
                        tint = if (isPressed)
                            Color(0xFF636EF1) // 按下时：蓝紫色
                        else
                            Color(0xFF6B7280) // 正常时：gray-600
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
