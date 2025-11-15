package com.cs407.knot_client_android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AddPlaceSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val targetHeight = screenHeight * 0.75f
    val coroutineScope = rememberCoroutineScope()
    
    // 动画高度：从 0 到 targetHeight
    val animatedHeight = remember { Animatable(0f) }
    
    // 记录拖动起始高度
    var dragStartHeight by remember { mutableStateOf(0f) }
    
    // 监听 isVisible 变化，触发动画
    LaunchedEffect(isVisible) {
        if (isVisible) {
            animatedHeight.animateTo(
                targetValue = targetHeight.value,
                animationSpec = spring(
                    dampingRatio = 0.75f, // 更高的 dampingRatio，更快更稳
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            animatedHeight.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.75f, // 快速下滑
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
    
    // 拖动结束后的处理
    fun snapToTarget() {
        coroutineScope.launch {
            val current = animatedHeight.value
            val threshold = targetHeight.value * 0.5f // 如果拖动超过 50%，则关闭
            
            if (current < threshold) {
                // 关闭 sheet
                animatedHeight.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.75f, // 快速下滑
                        stiffness = Spring.StiffnessLow
                    )
                )
                // 动画结束后通知外部关闭
                onDismiss()
            } else {
                // 回弹到原位
                animatedHeight.animateTo(
                    targetValue = targetHeight.value,
                    animationSpec = spring(
                        dampingRatio = 0.75f, // 快速回弹
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }
    
    // 监听动画高度，当接近 0 时自动同步状态
    LaunchedEffect(animatedHeight.value) {
        // 如果高度已经很小（< 5dp），认为已关闭，同步状态
        if (animatedHeight.value < 5f && isVisible) {
            onDismiss()
        }
    }
    
    // Sheet 容器
    if (animatedHeight.value > 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(5.dp)
                .height(animatedHeight.value.dp)
                .clip(RoundedCornerShape(51.0f.dp)) // 更大的圆角
                .background(Color(0xFFF8F6F4)) // 米黄色，不透明
                .clickable(
                    enabled = true,
                    onClick = {}, // 消费点击事件，防止穿透
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                // 顶部拖动指示器 - 可以拖动关闭
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
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
                                    
                                    // 实时跟随手指，只允许向下拖（减小高度）
                                    val newHeight = (animatedHeight.value - dragAmount).coerceIn(
                                        0f,
                                        targetHeight.value
                                    )
                                    coroutineScope.launch {
                                        animatedHeight.snapTo(newHeight)
                                    }
                                }
                            )
                        },
//                    contentAlignment = Alignment.TopCenter
                ) {
                    // 指示器横条
                    // Box(
                    //     modifier = Modifier
                    //         .width(48.dp)
                    //         .height(4.dp)
                    //         .clip(RoundedCornerShape(2.dp))
                    //         .background(Color(0xFFD0D0D0))
                    // )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "DROP A KNOT",
                        fontSize = 24.sp, // 稍微小一点，更精致
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1B1F)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Mark your favorite location on the map 📍",
                        fontSize = 14.sp,
                        color = Color(0xFF9B9B9B),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // 添加内容：：：TODO
                
            }
        }
    }
}

