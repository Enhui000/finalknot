package com.cs407.knot_client_android.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
//import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cs407.knot_client_android.R

@Composable
fun ProfileScreen(
    navController: NavHostController
) {
    // Profile 页面内容 - 不再包含导航栏
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 主体：浅色柔和多段渐变
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFBEFE8),
                        Color(0xFFF2E5F5),
                        Color(0xFFCCCBE0)
                    ),
                    start = Offset(0f, 0f),       // 左上
                    end = Offset(1000f, 1000f)    // 右下，控制斜率
                )
            )
            // 顶部柔光层
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x44FFFFFF),
                        Color(0x11FFFFFF),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(250f, 150f),
                    radius = 700f
                )
            )
            // 底部轻微暗角：增强层次
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x14000000)
                    )
                )
            ).background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            
            // 按钮行 - Logout 左对齐，Edit 右对齐
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logout 按钮 - 左对齐，带动画
                val logoutInteractionSource = remember { MutableInteractionSource() }
                val logoutIsPressed by logoutInteractionSource.collectIsPressedAsState()
                val logoutScale = remember { Animatable(1f) }
                
                LaunchedEffect(logoutIsPressed) {
                    if (logoutIsPressed) {
                        logoutScale.animateTo(
                            targetValue = 1.2f,
                            animationSpec = tween(
                                durationMillis = 170,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    } else {
                        logoutScale.animateTo(
                            targetValue = 0.88f,
                            animationSpec = tween(
                                durationMillis = 155,
                                easing = FastOutLinearInEasing
                            )
                        )
                        logoutScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
                
                Button(
                    onClick = { /* TODO: 处理登出事件 */ },
                    modifier = Modifier
                        .scale(logoutScale.value),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(42.dp),
                    interactionSource = logoutInteractionSource
                ) {
                    Text(
                        text = "Logout",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF444444)
                    )
                }
                
                // Edit 按钮 - 右对齐，带动画
                val editInteractionSource = remember { MutableInteractionSource() }
                val editIsPressed by editInteractionSource.collectIsPressedAsState()
                val editScale = remember { Animatable(1f) }
                
                LaunchedEffect(editIsPressed) {
                    if (editIsPressed) {
                        editScale.animateTo(
                            targetValue = 1.2f,
                            animationSpec = tween(
                                durationMillis = 170,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    } else {
                        editScale.animateTo(
                            targetValue = 0.88f,
                            animationSpec = tween(
                                durationMillis = 155,
                                easing = FastOutLinearInEasing
                            )
                        )
                        editScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
                
                Button(
                    onClick = { /* TODO: 处理编辑事件 */ },
                    modifier = Modifier
                        .scale(editScale.value),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(42.dp),
                    interactionSource = editInteractionSource
                ) {
                    Text(
                        text = "Edit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF444444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))
            
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .border(width = 2.dp, color = Color.White.copy(alpha = 0.15f), shape = CircleShape)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // user avatar
                Image(
                    painter = painterResource(id = R.drawable.user_avatar),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🧾 名称
            Text(
                text = "User Name",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2D33)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 用户 message
            Text(
                text = "You haven't set a status message yet!",
                fontSize = 14.sp,
                color = Color(0xFF5B5B65) // 中等灰色
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 📱 信息卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(42.dp))
                    .background(Color.White.copy(alpha = 0.45f))
                    .padding(10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nickname
                    UserInfoItem(
                        icon = Icons.Default.Person,
                        label = "Nickname",
                        value = "fly"
                    )
                    
                    // Email
                    UserInfoItem(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = "username.fly@wisc.edu"
                    )
                    
                    // Gender
                    UserInfoItem(
                        icon = Icons.Default.Face,
                        label = "Gender",
                        value = "female"
                    )
                    
                    // Birthday
                    UserInfoItem(
                        icon = Icons.Default.DateRange,
                        label = "Birthday",
                        value = "not_set"
                    )
                    
                    // Privacy Level
                    UserInfoItem(
                        icon = Icons.Default.Lock,
                        label = "Privacy",
                        value = "public"
                    )
                    
                    // Discoverable
                    UserInfoItem(
                        icon = Icons.Default.LocationOn,
                        label = "Discoverable",
                        value = "PUBLIC"
                    )
                }
            
            }
        }
    }
}

@Composable
private fun UserInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8E8E93), // 可见的灰色
            modifier = Modifier.size(28.dp) // 增大图标
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 标签
        Text(
            text = label,
            fontSize = 18.sp, // 增大字体
            fontWeight = FontWeight.Medium,
            color = Color(0xFF505058), // 可见的深灰色
            modifier = Modifier.weight(1f)
        )
        
        // 值
        Text(
            text = value,
            fontSize = 16.sp, // 增大字体
            fontWeight = FontWeight.Normal,
            color = Color(0xFF7B7D86) // 可见的灰色
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}

