package com.cs407.knot_client_android.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    
    // 三个高度状态：收起、半展开、全展开
    val collapsedHeight = 70.dp
    val expandedHeight = screenHeight * 0.5f  // 半展开：50%
    val maxExpandedHeight = screenHeight * 0.94f  // 全展开：94%
    
    // 动画状态
    val animatedHeight = remember { Animatable(collapsedHeight.value) }
    val coroutineScope = rememberCoroutineScope()
    
    // 当前高度（Dp）
    val currentHeight = animatedHeight.value.dp
    
    // 展开进度 (0f = 收起, 1f = 半展开) - 用于内部动画
    val progress = ((animatedHeight.value - collapsedHeight.value) / 
                    (expandedHeight.value - collapsedHeight.value)).coerceIn(0f, 1f)
    
    // 完整展开进度 (0f = 收起, 1f = 半展开, 2f = 全展开) - 用于通知外部
    val fullProgress = when {
        animatedHeight.value <= expandedHeight.value -> {
            ((animatedHeight.value - collapsedHeight.value) / 
             (expandedHeight.value - collapsedHeight.value)).coerceIn(0f, 1f)
        }
        else -> {
            1f + ((animatedHeight.value - expandedHeight.value) / 
                  (maxExpandedHeight.value - expandedHeight.value)).coerceIn(0f, 1f)
        }
    }
    
    // 通知外部展开进度变化
    LaunchedEffect(fullProgress) {
        onExpandProgressChange(fullProgress)
    }
    
    // 记录拖动起始高度
    var dragStartHeight by remember { mutableStateOf(0f) }

    // 搜索框输入状态
    var searchQuery by remember { mutableStateOf("") }
    
    // 拖动结束后的处理 - 支持三个状态：收起(70dp)、半展开(50%)、全展开(93%)
    fun snapToTarget() {
        coroutineScope.launch {
            val current = animatedHeight.value
            
            // 定义三个吸附点
            val snapPoints = listOf(
                collapsedHeight.value,      // 70dp
                expandedHeight.value,        // 50%
                maxExpandedHeight.value      // 93%
            )
            
            // 找到最接近的吸附点
            val target = snapPoints.minByOrNull { kotlin.math.abs(it - current) } ?: expandedHeight.value
            
            animatedHeight.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 120f
                )
            )
        }
    }
    
    Box(modifier = modifier) {
        // 判断是否处于第二阶段（半展开到全展开）
        val isPhase2 = animatedHeight.value > expandedHeight.value
        
        // 当前宽度：三段式变化
        // 阶段1: 272.dp -> (screenWidth - 16.dp)
        // 阶段2: (screenWidth - 16.dp) -> screenWidth
        val currentWidth = if (isPhase2) {
            val phase2Progress = ((animatedHeight.value - expandedHeight.value) / 
                                  (maxExpandedHeight.value - expandedHeight.value)).coerceIn(0f, 1f)
            (screenWidth - 16.dp) + 16.dp * phase2Progress
        } else {
            272.dp + (screenWidth - 272.dp - 16.dp) * progress
        }
        
        // 动态圆角：三段式变化
        // 阶段1: 44.dp -> 51.dp (半展开状态)
        // 阶段2: 51.dp -> 42.dp (完全展开时)
        val currentCornerRadius = if (isPhase2) {
            val phase2Progress = ((animatedHeight.value - expandedHeight.value) / 
                                  (maxExpandedHeight.value - expandedHeight.value)).coerceIn(0f, 1f)
            if (phase2Progress < 0.5f) {
                51.dp - 18.dp * phase2Progress
            } else {
                42.dp + (51.dp - 42.dp) * (phase2Progress - 0.5f)
            }
        } else {
            44.dp + 7.dp * progress
        }
        
        // 毛玻璃背景层 - Android 原生系统级模糊
        // 动态透明度：收起时 0.5，展开时 0.9
        val blurAlpha = 0.5f + 0.4f * progress
        
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
                // .background(Color.White.copy(alpha = blurAlpha))
                .background(if (progress < 0.8f) Color.White.copy(alpha = blurAlpha) else Color(0xFFF8F6F4).copy(alpha = blurAlpha))
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
                    .width(272.dp)
                    .height(70.dp)
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
                                        
                                        // 实时跟随手指，不触发任何自动动画，最高可以拖到80%
                                        val newHeight = (animatedHeight.value - dragAmount).coerceIn(
                                            collapsedHeight.value,
                                            maxExpandedHeight.value
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
                        // .background(Color(0xFFF8F6F4).copy(alpha = blurAlpha))
                        .clip(RoundedCornerShape(currentCornerRadius))
                        .alpha(progress)
                        .clickable (
                            enabled = true,
                            onClick = {},  // 消费点击事件，不让它穿透
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .background(Color(0xFFF8F6F4).copy(alpha = blurAlpha))
                ) {
                    // 拖动指示器（始终可拖动） 
                    Box(
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth()
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
                                                
                                                // 实时跟随手指，不触发任何自动动画，最高可以拖到80%
                                                val newHeight = (animatedHeight.value - dragAmount).coerceIn(
                                                    collapsedHeight.value,
                                                    maxExpandedHeight.value
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

                        // 顶部：搜索框 + 头像
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(horizontal = 8.dp), // 给点左右边距
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
                                        color = Color(0xFFDADADA),
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // POSTS 标题
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "POSTS",
                            fontSize = 24.sp, // 稍微小一点，更精致
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1B1F)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (progress > 0.76f) {
                            // 副标题（Slogan）
                            Text(
                                text = "Share your little footprints with close friends 🌍",
                                fontSize = 14.sp,
                                color = Color(0xFF9B9B9B),
                                fontWeight = FontWeight.Medium
                            )
    
                            Spacer(modifier = Modifier.height(24.dp))


                        }

                    }
                }
            }
        }
    }
}

