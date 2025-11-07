package com.cs407.knot_client_android.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs407.knot_client_android.R
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun ExpandableBottomSheet(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    isDraggable: Boolean,
    modifier: Modifier = Modifier,
    onExpandProgressChange: (Float) -> Unit = {} // 回调展开进度
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // 收起和展开的高度
    val collapsedHeight = 64.dp
    val expandedHeight = screenHeight * 0.5f
    
    // 动画状态
    val animatedHeight = remember { Animatable(collapsedHeight.value) }
    val coroutineScope = rememberCoroutineScope()
    
    // 当前高度（Dp）
    val currentHeight = animatedHeight.value.dp
    
    // 展开进度 (0f = 收起, 1f = 展开)
    val progress = ((animatedHeight.value - collapsedHeight.value) / 
                    (expandedHeight.value - collapsedHeight.value)).coerceIn(0f, 1f)
    
    // 通知外部展开进度变化
    LaunchedEffect(progress) {
        onExpandProgressChange(progress)
    }
    
    // 记录拖动起始高度
    var dragStartHeight by remember { mutableStateOf(0f) }

    // 搜索框输入状态
    var searchQuery by remember { mutableStateOf("") }
    
    // 拖动结束后的处理
    fun snapToTarget() {
        coroutineScope.launch {
            val totalRange = expandedHeight.value - collapsedHeight.value
            val dragDistance = animatedHeight.value - dragStartHeight
            val dragPercentage = kotlin.math.abs(dragDistance) / totalRange
            
            // 如果拖动超过总范围的 20%，则自动完成展开/收起
            val target = if (dragPercentage > 0.2f) {
                // 根据拖动方向决定目标
                if (dragDistance > 0) {
                    // 高度增加 → 向上拖动 → 展开
                    expandedHeight.value
                } else {
                    // 高度减少 → 向下拖动 → 收起
                    collapsedHeight.value
                }
            } else {
                // 拖动不足 20%，回到起始位置
                dragStartHeight
            }
            
            animatedHeight.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 120f
                )
            )
        }
    }
    
    Box(modifier = modifier) {
        // 当前宽度（收起时窄，展开时宽）
        // 展开时宽度 = screenWidth - 16.dp（因为 MainScreen 左右各有 8dp padding）
        val currentWidth = 254.dp + (screenWidth - 254.dp - 16.dp) * progress
        
        // 动态圆角（收起时 32dp，展开时 48dp）
        val currentCornerRadius = 32.dp + 16.dp * progress
        
        // 毛玻璃背景层 - Android 原生系统级模糊
        // 动态透明度：收起时 0.6，展开时 0.9
        val blurAlpha = 0.6f + 0.3f * progress
        
        Box(
            modifier = Modifier
                .width(currentWidth)
                .height(currentHeight)
                .clip(RoundedCornerShape(currentCornerRadius))
                .graphicsLayer {
                    renderEffect = RenderEffect
                        .createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .background(Color.White.copy(alpha = blurAlpha))
        )
        
        // 主容器
        Box(
            modifier = Modifier
                .width(currentWidth)
                .height(currentHeight)
                .border(1.dp, Color(0xFFE5E7EB).copy(alpha = 0.6f), RoundedCornerShape(currentCornerRadius))
                .clip(RoundedCornerShape(currentCornerRadius))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            // 收起状态：显示导航栏（整个区域可拖动）
            Box(
                modifier = Modifier
                    .width(254.dp)
                    .height(64.dp)
                    .alpha(1f - progress)
                    .then(
                        if (isDraggable) {
                            Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        // 记录起始高度
                                        dragStartHeight = animatedHeight.value
                                    },
                                    onDragEnd = {
                                        snapToTarget()
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        
                                        // 实时跟随手指，不触发任何自动动画
                                        val newHeight = (animatedHeight.value - dragAmount).coerceIn(
                                            collapsedHeight.value,
                                            expandedHeight.value
                                        )
                                        coroutineScope.launch {
                                            animatedHeight.snapTo(newHeight)
                                        }
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
            
            // 展开状态：显示内容
            if (progress > 0.0f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(progress)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .background(Color.White.copy(alpha = blurAlpha))
                ) {
                    // 拖动指示器（始终可拖动） 
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                            .align(Alignment.CenterHorizontally)
                            .then(
                                if (isDraggable) {  // 移除 progress 限制，始终可拖动
                                    Modifier.pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                // 记录起始高度
                                                dragStartHeight = animatedHeight.value
                                            },
                                            onDragEnd = {
                                                snapToTarget()
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                
                                                // 实时跟随手指，不触发任何自动动画
                                                val newHeight = (animatedHeight.value - dragAmount).coerceIn(
                                                    collapsedHeight.value,
                                                    expandedHeight.value
                                                )
                                                coroutineScope.launch {
                                                    animatedHeight.snapTo(newHeight)
                                                }
                                            }
                                        )
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFD1D5DB))
                        )
                    }


                    // 顶部：搜索框 + 头像
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 搜索框（样式同步登录页输入框）
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            placeholder = {
                                Text(
                                    "Search posts",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 15.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF9B8FD9).copy(alpha = 0.7f)
                                )
                            },
                            shape = RoundedCornerShape(32.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFFB5A8FF), // 淡紫色
                                unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                                focusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // 头像
                        Image(
                            painter = painterResource(id = R.drawable.user_avatar),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFFB0B0B0), // 柔和浅灰色边框
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Spacer(modifier = Modifier.height(8.dp))
                    
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // POSTS 标题
                    Text(
                        text = "POSTS",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 占位内容
                    Text(
                        text = "这里显示帖子内容\n\nPlaceholder Content",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

