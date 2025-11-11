package com.cs407.knot_client_android.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
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
    val targetHeight = screenHeight * 0.92f
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
//                    targetValue = 0f,
//                    animationSpec = spring(
//                        dampingRatio = 0.75f, // 快速下滑
//                        stiffness = Spring.StiffnessLow
//                    )
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 750, // 从200~500之间调节速度
                        easing = FastOutSlowInEasing
                    )
                )
                // 动画结束后通知外部关闭
                onDismiss()
            } else {
                // 回弹到原位
                animatedHeight.animateTo(
                    // targetValue = targetHeight.value,
                    // animationSpec = spring(
                    //     dampingRatio = 0.75f, // 快速回弹
                    //     stiffness = Spring.StiffnessLow
                    // )
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 750, // 从200~500之间调节速度
                        easing = FastOutSlowInEasing
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
                .padding(0.dp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ){
                        Column{
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
                        // button that dismiss the sheet
                        Box {
                            // 动画状态管理
                            val buttonInteractionSource = remember { MutableInteractionSource() }
                            val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
                            
                            // Apple-style 双阶段弹性动画
                            val buttonScale = remember { Animatable(1f) }
                            
                            LaunchedEffect(isButtonPressed) {
                                if (isButtonPressed) {
                                    // 按下：快速放大一点点
                                    buttonScale.animateTo(
                                        targetValue = 1.2f,
                                        animationSpec = tween(
                                            durationMillis = 170, 
                                            easing = LinearOutSlowInEasing
                                        )
                                    )
                                } else {
                                    // 松手：先缩回一点再弹回 1
                                    buttonScale.animateTo(
                                        targetValue = 0.88f,
                                        animationSpec = tween(
                                            durationMillis = 155, 
                                            easing = FastOutLinearInEasing
                                        )
                                    )
                                    // 然后自然回弹到 1
                                    buttonScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                            
                            // 毛玻璃背景层 - Android 原生系统级模糊
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
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
                                    .size(70.dp)
                                    .scale(buttonScale.value)
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFE5E7EB).copy(alpha = 0.6f), // 边框也略微透明
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
                                        onClick = onDismiss,
                                        indication = null,
                                        interactionSource = buttonInteractionSource
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = if (isButtonPressed) 
                                        Color(0xFF636EF1) // 按下时：蓝紫色，与 BottomNavigationBar 选中颜色一致
                                    else 
                                        Color(0xFF6B7280) // 正常时：gray-600，与 BottomNavigationBar 未选中颜色一致
                                )
                            }
                        }
                    }
                }

                // 添加内容：：：TODO
                
            }
        }
    }
}

