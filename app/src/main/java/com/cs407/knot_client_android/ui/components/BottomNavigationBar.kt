package com.cs407.knot_client_android.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cs407.knot_client_android.R

enum class NavTab {
    MAP,
    CHAT,
    PROFILE,
    FRIEND
}

@Composable
fun BottomNavigationBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        // 毛玻璃背景层 - Android 原生系统级模糊
        Box(
            modifier = Modifier
                .width(272.dp)
                .height(70.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    renderEffect = RenderEffect
                        .createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .background(Color.White.copy(alpha = 0f))
        )
        
        // 主容器 (带边框和半透明背景)
        BoxWithConstraints(
            modifier = Modifier
                .width(272.dp)
                .height(70.dp)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE5E7EB).copy(alpha = 0.6f), // 边框也略微透明
                    shape = RoundedCornerShape(44.dp)
                )
                .clip(CircleShape)
        ) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight
            
            // 每个按钮区域的宽度 (平均分配)
            val buttonWidth = containerWidth / 3
            
            // 滑块尺寸 (稍微小一点，留出间距)
            val sliderWidth = buttonWidth - 8.dp  // 每边留 4dp 间距
            val sliderHeight = containerHeight - 8.dp  // 上下各 4dp
            
            // 计算滑块位置 (基于按钮区域中心对齐)
            val sliderOffsetX by animateDpAsState(
                targetValue = when (selectedTab) {
                    NavTab.MAP -> 4.dp  // 第一个位置
                    NavTab.CHAT -> buttonWidth + 4.dp  // 第二个位置
                    NavTab.PROFILE -> buttonWidth * 2 + 4.dp  // 第三个位置
                    NavTab.FRIEND -> buttonWidth + 4.dp
                },
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 200f
                ),
                label = "slider offset"
            )
            
            // 白色滑块 (对应 web 的滑块)
            Box(
                modifier = Modifier
                    .offset(x = sliderOffsetX, y = 4.dp)  // top: 4px
                    .width(sliderWidth)
                    .height(sliderHeight)
                    .scale(1.02f)
                    .clip(RoundedCornerShape(44.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.75f),
                                Color.White.copy(alpha = 0.75f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x26636EF1), // 蓝紫色边框
                        shape = RoundedCornerShape(44.dp)
                    )
            )
            
            // 三个按钮
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Map 按钮
                IconButton(
                    icon = Icons.Outlined.Place,
                    isSelected = selectedTab == NavTab.MAP,
                    onClick = { onTabSelected(NavTab.MAP) },
                    modifier = Modifier.weight(1f)
                )
                
                // Message 按钮
                IconButton(
                    icon = Icons.Outlined.Email,
                    isSelected = selectedTab == NavTab.CHAT,
                    onClick = { onTabSelected(NavTab.CHAT) },
                    modifier = Modifier.weight(1f)
                )
                
                // Profile 按钮 (头像样式)
                ProfileButton(
                    isSelected = selectedTab == NavTab.PROFILE,
                    onClick = { onTabSelected(NavTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IconButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = if (isSelected) 
                Color(0xFF636EF1) // 蓝紫色 #636ef1
            else 
                Color(0xFF6B7280) // gray-600
        )
    }
}

@Composable
private fun ProfileButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "profile scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(scale)
        ) {
            // 头像图片
            Image(
                painter = painterResource(id = R.drawable.user_avatar),
                contentDescription = "Profile Avatar",
                modifier = Modifier
                    .size(33.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center),
                contentScale = ContentScale.Crop
            )
            
            // 选中时的蓝紫色边框
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = Color(0xFF636EF1),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

