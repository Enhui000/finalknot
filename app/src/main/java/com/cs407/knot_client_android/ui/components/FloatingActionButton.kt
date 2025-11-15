package com.cs407.knot_client_android.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FloatingActionButton(
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 如果没有图标，不显示按钮
    if (icon == null) return
    
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
        modifier = modifier
    ) {
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
                .scale(scale.value)
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
                    onClick = onClick,
                    indication = null,
                    interactionSource = interactionSource
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = if (isPressed) 
                    Color(0xFF636EF1) // 按下时：蓝紫色，与 BottomNavigationBar 选中颜色一致
                else 
                    Color(0xFF6B7280) // 正常时：gray-600，与 BottomNavigationBar 未选中颜色一致
            )
        }
    }
}

