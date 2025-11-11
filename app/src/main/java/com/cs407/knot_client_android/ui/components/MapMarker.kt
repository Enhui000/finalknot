package com.cs407.knot_client_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs407.knot_client_android.data.model.response.MapPostNearby

/**
 * 自定义长方形地图 Marker
 */
@Composable
fun MapMarker(
    post: MapPostNearby,
    onClick: () -> Unit = {}
) {
    val cornerRadius = RoundedCornerShape(15.dp)
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 长方形卡片
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(56.dp)
                .shadow(4.dp, cornerRadius)
                .clip(cornerRadius)
                .background(Color(0xFFF5F0E8)) // 米黄色背景
                .border(2.dp, Color(0xFF8B7355), cornerRadius)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Color(0xFF8B7355)),
                    onClick = onClick
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题
                Text(
                    text = post.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2416),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 底部用户信息栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 用户头像
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            // 灰色背景
                            .background(Color(0xFFF8F6F4))
                    ) {
                        // 如果有头像URL可以在这里加载图片
                        // 现在显示一个简单的背景色
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    // 用户名
                    Text(
                        text = post.creatorUsername,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5D4E37),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
        
        // 底部小三角形指示器
        Box(
            modifier = Modifier
                .size(0.dp, 8.dp)
                .offset(y = (-1).dp)
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(12.dp, 8.dp)
            ) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width / 2, size.height)
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFF8B7355)
                )
            }
        }
    }
}

