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
import com.cs407.knot_client_android.data.model.response.MapPostNearby
import com.cs407.knot_client_android.data.repository.MapPostRepository
import com.cs407.knot_client_android.ui.components.MapMarker
import com.cs407.knot_client_android.utils.LocationManager
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationSourceOptions
import com.mapbox.maps.plugin.annotation.ClusterOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import android.graphics.Color as AndroidColor
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cs407.knot_client_android.ui.main.MainViewModel
import com.cs407.knot_client_android.data.model.WebSocketMessage
import com.cs407.knot_client_android.data.model.MapPostNewMessage
import com.google.gson.Gson

@Composable
fun MapScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    onPostSelected: (MapPostNearby) -> Unit = {}
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val mapPreferences = remember { MapPreferences(context) }
    val scope = rememberCoroutineScope()
    
    // API Repository
    val mapPostRepository = remember { 
        MapPostRepository(context, "http://10.0.2.2:8080") 
    }
    
    // ⚡ 静态标志：地图直接显示，无动画
    // 因为地图会在 MainScreen 加载时就开始初始化
    // 当用户看到时，地图已经准备好了
    var showMarkers by remember { mutableStateOf(false) }
    
    // 为每个 marker 单独管理显示状态，用于动画（使用 Set 来追踪已显示的 marker）
    var visibleMarkerIds by remember { mutableStateOf(setOf<Long>()) }
    
    // 地图帖子数据状态 - 使用 Map 进行本地缓存和去重
    var mapPostsCache by remember { mutableStateOf<Map<Long, MapPostNearby>>(emptyMap()) }
    val mapPosts: List<MapPostNearby> by remember { derivedStateOf { mapPostsCache.values.toList() } }
    var isLoadingPosts by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 位置状态
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var hasPermission by remember { mutableStateOf(locationManager.hasLocationPermission()) }
    var centerLocationName by remember { mutableStateOf<String?>(null) }
    
    // // 假数据：多个地图帖子（在 Mountain View 区域）
    // val mockMapPosts = remember {
    //     listOf(
    //         MapPost(
    //             mapPostId = 1,
    //             convId = 101,
    //             creatorId = 1001,
    //             title = "Best Coffee ☕",
    //             description = "Amazing latte art and cozy atmosphere!",
    //             mediaJson = listOf("url1", "url2"),
    //             locLat = 37.422,
    //             locLng = -122.084,
    //             locName = "Google Play Store",
    //             geohash = "9q9hvnf",
    //             viewCount = 156,
    //             likeCount = 42,
    //             commentCount = 8,
    //             status = 1,
    //             createdAt = "2024-11-09T10:30:00Z",
    //             postType = PostType.ALL
    //         ),
    //         MapPost(
    //             mapPostId = 2,
    //             convId = 102,
    //             creatorId = 1002,
    //             title = "Tech Meetup 🚀",
    //             description = "Weekly tech talks and networking",
    //             mediaJson = null,
    //             locLat = 37.425,
    //             locLng = -122.088,
    //             locName = "Mountain View Library",
    //             geohash = "9q9hvng",
    //             viewCount = 89,
    //             likeCount = 27,
    //             commentCount = 15,
    //             status = 1,
    //             createdAt = "2024-11-09T14:15:00Z",
    //             postType = PostType.REQUEST
    //         ),
    //         MapPost(
    //             mapPostId = 3,
    //             convId = 103,
    //             creatorId = 1003,
    //             title = "Yoga Class 🧘",
    //             description = "Morning yoga sessions every weekend",
    //             mediaJson = listOf("url3"),
    //             locLat = 37.427,
    //             locLng = -122.086,
    //             locName = "Shoreline Park",
    //             geohash = "9q9hvnh",
    //             viewCount = 234,
    //             likeCount = 68,
    //             commentCount = 22,
    //             status = 1,
    //             createdAt = "2024-11-09T08:00:00Z",
    //             postType = PostType.ALL
    //         ),
    //         MapPost(
    //             mapPostId = 4,
    //             convId = 104,
    //             creatorId = 1004,
    //             title = "Food Truck 🌮",
    //             description = "Best tacos in town!",
    //             mediaJson = null,
    //             locLat = 37.423,
    //             locLng = -122.090,
    //             locName = "Castro Street",
    //             geohash = "9q9hvne",
    //             viewCount = 312,
    //             likeCount = 95,
    //             commentCount = 41,
    //             status = 1,
    //             createdAt = "2024-11-09T12:00:00Z",
    //             postType = PostType.ALL
    //         ),
    //         MapPost(
    //             mapPostId = 5,
    //             convId = 105,
    //             creatorId = 1005,
    //             title = "Book Club 📚",
    //             description = "Monthly book discussions",
    //             mediaJson = listOf("url4", "url5"),
    //             locLat = 37.420,
    //             locLng = -122.082,
    //             locName = "Public Library",
    //             geohash = "9q9hvnc",
    //             viewCount = 145,
    //             likeCount = 38,
    //             commentCount = 19,
    //             status = 1,
    //             createdAt = "2024-11-09T16:30:00Z",
    //             postType = PostType.REQUEST
    //         ),
    //         MapPost(
    //             mapPostId = 6,
    //             convId = 106,
    //             creatorId = 1006,
    //             title = "Art Gallery 🎨",
    //             description = "Local artists exhibition",
    //             mediaJson = null,
    //             locLat = 37.428,
    //             locLng = -122.089,
    //             locName = "Art Center",
    //             geohash = "9q9hvni",
    //             viewCount = 198,
    //             likeCount = 52,
    //             commentCount = 28,
    //             status = 1,
    //             createdAt = "2024-11-09T13:45:00Z",
    //             postType = PostType.ALL
    //         ),
    //         MapPost(
    //             mapPostId = 7,
    //             convId = 107,
    //             creatorId = 1007,
    //             title = "Bike Repair 🚴",
    //             description = "Free bike maintenance workshop",
    //             mediaJson = listOf("url6"),
    //             locLat = 37.419,
    //             locLng = -122.085,
    //             locName = "Community Center",
    //             geohash = "9q9hvnb",
    //             viewCount = 167,
    //             likeCount = 44,
    //             commentCount = 13,
    //             status = 1,
    //             createdAt = "2024-11-09T09:15:00Z",
    //             postType = PostType.REQUEST
    //         ),
    //         MapPost(
    //             mapPostId = 8,
    //             convId = 108,
    //             creatorId = 1008,
    //             title = "Live Music 🎵",
    //             description = "Jazz night every Friday",
    //             mediaJson = null,
    //             locLat = 37.426,
    //             locLng = -122.091,
    //             locName = "Music Venue",
    //             geohash = "9q9hvnj",
    //             viewCount = 276,
    //             likeCount = 82,
    //             commentCount = 35,
    //             status = 1,
    //             createdAt = "2024-11-09T18:00:00Z",
    //             postType = PostType.ALL
    //         )
    //     )
    // }
    
    // 跟踪当前的 zoom 级别（用于控制 cluster/detail 切换）
    var currentZoom by remember { mutableStateOf(7.0) }
    
    // 创建 Geocoding API (用于反向地理编码)
    val mapboxToken = context.getString(R.string.mapbox_access_token)
    val geocodingApi = remember { GeocodingApiService.create() }
    
    // 用于节流的 Job
    var geocodingJob by remember { mutableStateOf<Job?>(null) }
    var fetchPostsJob by remember { mutableStateOf<Job?>(null) }
    
    // 加载附近帖子的函数（带节流）- 使用 V2 API（基于 radius）
    fun fetchNearbyPosts(lat: Double, lng: Double, zoom: Double) {
        // 取消之前的请求
        fetchPostsJob?.cancel()
        
        
        // 1.5 秒节流
        fetchPostsJob = scope.launch {
            delay(1500) // 1.5 秒延迟
            
            try {
                isLoadingPosts = true
                errorMessage = null
                
                // 使用 V2 API（基于 radius，小数据集优化）
                val posts = mapPostRepository.getNearbyPostsV2(
                    lat = lat,
                    lng = lng,
                    radius = 500000,         // 固定 500000m 半径
                    timeRange = "7D",      // 固定 7 天（可配置）
                    postType = "ALL",      // 固定 ALL 类型（可配置）
                    maxResults = 200       // 固定 200 条
                )
                
                // 合并新数据到缓存（去重）
                val updatedCache = mapPostsCache.toMutableMap()
                val newPostIds = mutableListOf<Long>()
                
                posts.forEach { post ->
                    if (!updatedCache.containsKey(post.mapPostId)) {
                        updatedCache[post.mapPostId] = post
                        newPostIds.add(post.mapPostId)
                    }
                }
                
                mapPostsCache = updatedCache
                showMarkers = true
                
                // 依次显示新的 marker（带动画）
                newPostIds.forEach { postId ->
                    delay(80L) // 每个 marker 间隔 80ms
                    visibleMarkerIds = visibleMarkerIds + postId
                }
                
            } catch (e: Exception) {
                errorMessage = e.message ?: "加载帖子失败"
                snackbarHostState.showSnackbar(errorMessage!!)
            } finally {
                isLoadingPosts = false
            }
        }
    }
    
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
    
    // ⚡ 首次加载地图帖子（用户位置获取后）
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            // 等待一下地图初始化
            delay(800)
            // 使用用户位置或地图中心加载帖子
            val center = mapViewportState.cameraState?.center ?: location
            val zoom = mapViewportState.cameraState?.zoom ?: 15.0
            fetchNearbyPosts(center.latitude(), center.longitude(), zoom)
        }
    }
    
    // 🔔 监听 WebSocket 消息（实时推送新帖子）
    LaunchedEffect(Unit) {
        mainViewModel.wsManager.rawMessages.collect { message ->
            message?.let {
                try {
                    // 解析消息类型
                    val gson = Gson()
                    val baseMessage = gson.fromJson(it, WebSocketMessage::class.java)
                    
                    if (baseMessage.type == "MAP_POST_NEW") {
                        // 解析完整消息
                        val mapPostNew = gson.fromJson(it, MapPostNewMessage::class.java)
                        
                        // 转换为 MapPostNearby 格式
                        val newPost = MapPostNearby(
                            mapPostId = mapPostNew.mapPostId,
                            convId = mapPostNew.convId,
                            title = mapPostNew.title,
                            description = mapPostNew.description,
                            mediaUrls = mapPostNew.mediaUrls,
                            locLat = mapPostNew.loc.lat,
                            locLng = mapPostNew.loc.lng,
                            locName = mapPostNew.loc.name,
                            distance = 0.0,  // 暂时设置为 0
                            creatorId = mapPostNew.creatorId,
                            creatorUsername = mapPostNew.creatorUsername,
                            creatorAvatar = mapPostNew.creatorAvatar,
                            viewCount = 0,
                            likeCount = 0,
                            commentCount = 0,
                            postType = "ALL",
                            createdAtMs = mapPostNew.createdAtMs
                        )
                        
                        // 添加到缓存（去重）
                        if (!mapPostsCache.containsKey(newPost.mapPostId)) {
                            mapPostsCache = mapPostsCache + (newPost.mapPostId to newPost)
                            
                            // 延迟一下，然后显示动画
                            delay(300)
                            visibleMarkerIds = visibleMarkerIds + newPost.mapPostId
                            
                            // 显示提示
                            snackbarHostState.showSnackbar("🎉 ${mapPostNew.creatorUsername} 发布了新帖子！")
                        }
                    }
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
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
                            .bearing(0.0)  // 旋转到正北方向
                            .pitch(0.0)    // 重置倾斜角度
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
                        .bearing(0.0)  // 旋转到正北方向
                        .pitch(0.0)    // 重置倾斜角度
                        .build(),
                    animationOptions = MapAnimationOptions.mapAnimationOptions {
                        duration(1500) // 1.5秒的平滑动画
                    }
                )
            }
        }
    }
    
    // 监听地图中心和缩放变化，获取中心点地名 + 保存位置 + 加载附近帖子
    LaunchedEffect(mapViewportState.cameraState) {
        val zoom = mapViewportState.cameraState?.zoom ?: return@LaunchedEffect
        val center = mapViewportState.cameraState?.center ?: return@LaunchedEffect
        
        // 更新当前 zoom 级别（用于控制 cluster/detail 显示）
        currentZoom = zoom
        
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
        
        // 🔄 加载附近的帖子（使用 1.5秒 节流）
        fetchNearbyPosts(center.latitude(), center.longitude(), zoom)
    }
    
    // ⚡ 直接显示地图，无动画
    // 地图会在 MainScreen 加载时就开始初始化
    Box(modifier = Modifier.fillMaxSize()) {
        // 地图内容 - 使用 MapStyle + PointAnnotationGroup Clustering
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
            
            // Mapbox 原生 Clustering：zoom ≤ 13 时显示蓝色聚合圆圈
            if (showMarkers && currentZoom <= 13.0 && mapPosts.isNotEmpty()) {
                PointAnnotationGroup(
                    annotations = mapPosts.map { post ->
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(post.locLng, post.locLat))
                    },
                    annotationConfig = AnnotationConfig(
                        annotationSourceOptions = AnnotationSourceOptions(
                            clusterOptions = ClusterOptions(
                                // Cluster 圆圈颜色：统一蓝色
                                colorLevels = listOf(
                                    Pair(0, AndroidColor.rgb(76, 144, 226))  // 蓝色
                                ),
                                // Cluster 文字颜色
                                textColorExpression = Expression.color(AndroidColor.WHITE),
                                // Cluster 文字大小
                                textSize = 14.0,
                                // Cluster 圆圈半径
                                circleRadiusExpression = literal(25.0),
                                // 聚合半径
                                clusterRadius = 50L,
                                // 最大聚合的 zoom 级别（13 以下都会 cluster）
                                clusterMaxZoom = 13L
                            )
                        )
                    )
                ) {
                    // 点击 cluster 时，放大到 zoom 13.5 并移动到 cluster 位置
                    // interactionsState.onClusterClicked { cluster ->
                    //     scope.launch {
                    //         // 从 annotatedFeature 获取几何信息
                    //         val geometry = cluster.annotatedFeature.feature.geometry()
                    //         if (geometry is Point) {
                    //             mapViewportState.easeTo(
                    //                 cameraOptions = CameraOptions.Builder()
                    //                     .center(geometry)  // 移动到 cluster 中心
                    //                     .zoom(13.5)  // 放大到 13.5，刚好能看到详细卡片
                    //                     .build(),
                    //                 animationOptions = MapAnimationOptions.mapAnimationOptions {
                    //                     duration(800)  // 800ms 的平滑动画
                    //                 }
                    //             )
                    //         }
                    //     }
                    //     true  // 消费事件
                    // }
                    interactionsState.onClusterClicked { cluster ->
                        // 拿到聚类点的中心坐标
                        val point = cluster.originalFeature.geometry() as? com.mapbox.geojson.Point
                        if (point != null) {
                            // 放大到能分裂的一个经验 zoom（13.5~14.5 之间看你的数据分布）
                            mapViewportState.easeTo(
                                cameraOptions = CameraOptions.Builder()
                                    .center(point)
                                    .zoom(13.8) // 你原来用 13.5 也行
                                    .bearing(0.0)  // 旋转到正北方向
                                    .pitch(0.0)    // 重置倾斜角度
                                    .build(),
                                animationOptions = MapAnimationOptions.mapAnimationOptions {
                                    duration(1800)
                                }
                            )
                        }
                        true // 消费点击
                    }
                }
            }
            
            // ViewAnnotation 详细卡片：zoom > 13 时显示
            if (showMarkers && currentZoom > 13.0 && mapPosts.isNotEmpty()) {
                mapPosts.forEach { post ->
                    // 只渲染已经设置为可见的 markers
                    if (visibleMarkerIds.contains(post.mapPostId)) {
                        ViewAnnotation(
                            options = viewAnnotationOptions {
                                geometry(Point.fromLngLat(post.locLng, post.locLat))
                            }
                        ) {
                            // 使用 scale 动画来实现进入效果
                            val scale = remember { Animatable(0.7f) }
                            
                            LaunchedEffect(Unit) {
                                scale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .scale(scale.value)
                                    .alpha(scale.value)
                            ) {
                                MapMarker(
                                    post = post,
                                    onClick = {
                                        // 点击 marker 后通知 MainScreen 打开帖子详情 Sheet
                                        onPostSelected(post)
                                    }
                                )
                            }
                        }
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
                                                .bearing(0.0)  // 旋转到正北方向
                                                .pitch(0.0)    // 重置倾斜角度
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
        
        // DEBUG: 显示 zoom 级别和 marker 模式 DO NOT DELETE THIS CODE
        Text(
            text = "Zoom: ${"%.1f".format(currentZoom)} | ${if (currentZoom > 13.0) "Details" else "Clusters"}",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.Black,
            fontSize = 12.sp
        )
        
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
        
        // Loading Indicator - 左上角
        if (isLoadingPosts) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 70.dp)
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .padding(4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF4C90E2),
                    strokeWidth = 2.dp
                )
            }
        }
        
        // Snackbar Host - 底部
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}

// Preview 需要 MainViewModel，暂时禁用
//@Preview(showBackground = true)
//@Composable
//fun MapScreenPreview() {
//    MapScreen(navController = rememberNavController())
//}
