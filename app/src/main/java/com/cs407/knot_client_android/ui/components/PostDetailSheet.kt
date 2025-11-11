package com.cs407.knot_client_android.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.FavoriteBorder
//import androidx.compose.material.icons.outlined.ChatBubbleOutline
//import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs407.knot_client_android.R
import com.cs407.knot_client_android.data.model.response.MapPostNearby
import kotlinx.coroutines.launch

// Mock Comment Data
data class Comment(
    val commentId: Long,
    val username: String,
    val avatar: Int, // Resource ID
    val content: String,
    val timestamp: String,
    val likeCount: Int
)

@Composable
fun PostDetailSheet(
    post: MapPostNearby?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // 两个高度状态：半展开(50%)、全展开(94%)
    val collapsedHeight = 0.dp  // 完全收起时为 0
    val halfExpandedHeight = screenHeight * 0.5f  // 半展开：50%
    val fullExpandedHeight = screenHeight * 0.94f  // 全展开：94%
    
    // 动画状态
    val animatedHeight = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    // 记录拖动起始高度
    var dragStartHeight by remember { mutableStateOf(0f) }
    
    // 监听 isVisible 变化，触发动画
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // 展开到半展开状态
            animatedHeight.animateTo(
                targetValue = halfExpandedHeight.value,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            // 收起
            animatedHeight.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
    
    // 拖动结束后的处理 - 支持三个状态：关闭(0)、半展开(50%)、全展开(94%)
    fun snapToTarget() {
        coroutineScope.launch {
            val current = animatedHeight.value
            
            // 定义三个吸附点
            val snapPoints = listOf(
                0f,                           // 关闭
                halfExpandedHeight.value,     // 50%
                fullExpandedHeight.value      // 94%
            )
            
            // 找到最接近的吸附点
            val target = snapPoints.minByOrNull { kotlin.math.abs(it - current) } ?: halfExpandedHeight.value
            
            animatedHeight.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 120f
                )
            )
            
            // 如果吸附到关闭状态，通知外部
            if (target == 0f) {
                onDismiss()
            }
        }
    }
    
    // 监听动画高度，当接近 0 时自动同步状态
    LaunchedEffect(animatedHeight.value) {
        if (animatedHeight.value < 5f && isVisible) {
            onDismiss()
        }
    }
    
    // 当前高度
    val currentHeight = animatedHeight.value.dp
    
    // 计算展开进度
    // Phase 1: 0 -> 0.5 (收起 -> 半展开)
    // Phase 2: 0.5 -> 1.0 (半展开 -> 全展开)
    val progress = when {
        animatedHeight.value <= halfExpandedHeight.value -> {
            (animatedHeight.value / halfExpandedHeight.value).coerceIn(0f, 1f)
        }
        else -> {
            1f + ((animatedHeight.value - halfExpandedHeight.value) / 
                  (fullExpandedHeight.value - halfExpandedHeight.value)).coerceIn(0f, 1f)
        }
    }
    
    // 判断是否处于第二阶段（半展开到全展开）
    val isPhase2 = animatedHeight.value > halfExpandedHeight.value
    
    // 动态 padding：16dp (半展开) -> 0dp (全展开)
    val currentPadding = if (isPhase2) {
        val phase2Progress = ((animatedHeight.value - halfExpandedHeight.value) / 
                              (fullExpandedHeight.value - halfExpandedHeight.value)).coerceIn(0f, 1f)
        8.dp * (1f - phase2Progress)
    } else {
        8.dp
    }
    
    // 动态圆角：51.dp (半展开) -> 42.dp (全展开)
    val currentCornerRadius = if (isPhase2) {
        val phase2Progress = ((animatedHeight.value - halfExpandedHeight.value) / 
                              (fullExpandedHeight.value - halfExpandedHeight.value)).coerceIn(0f, 1f)
        51.dp - 9.dp * phase2Progress
    } else {
        51.dp
    }
    
    // Mock Comments Data
    val mockComments = remember {
        listOf(
            Comment(
                commentId = 1,
                username = "Alice",
                avatar = R.drawable.user_avatar,
                content = "Great place! Had an amazing time here 🎉",
                timestamp = "2 hours ago",
                likeCount = 12
            ),
            Comment(
                commentId = 2,
                username = "Bob",
                avatar = R.drawable.user_avatar,
                content = "Thanks for sharing! Will definitely visit soon.",
                timestamp = "5 hours ago",
                likeCount = 8
            ),
            Comment(
                commentId = 3,
                username = "Charlie",
                avatar = R.drawable.user_avatar,
                content = "The coffee here is absolutely fantastic! ☕️",
                timestamp = "1 day ago",
                likeCount = 15
            ),
            Comment(
                commentId = 4,
                username = "Diana",
                avatar = R.drawable.user_avatar,
                content = "Perfect spot for a weekend hangout!",
                timestamp = "2 days ago",
                likeCount = 6
            ),
            Comment(
                commentId = 5,
                username = "Eve",
                avatar = R.drawable.user_avatar,
                content = "Love the atmosphere here 💕",
                timestamp = "3 days ago",
                likeCount = 20
            )
        )
    }
    
    // Sheet 容器
    if (animatedHeight.value > 0f && post != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(currentHeight)
                .padding(start = currentPadding, end = currentPadding, bottom = currentPadding)
        ) {
            // 毛玻璃背景层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(currentCornerRadius))
                    .graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                    .background(Color.White.copy(alpha = 0.65f))
            )
            
            // 主容器
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        1.dp,
                        Color(0xFFE5E7EB).copy(alpha = 0.6f),
                        RoundedCornerShape(currentCornerRadius)
                    )
                    .clip(RoundedCornerShape(currentCornerRadius))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.2f)
                            )
                        )
                    )
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
                        .background(Color(0xFFF8F6F4))
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(Modifier.height(24.dp))
                    
                    // 可滚动内容
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 帖子内容区域（始终显示）- 带拖动手势
                        item {
                            PostContentSection(
                                post = post,
                                onDrag = { dragAmount ->
                                    // 实时跟随手指
                                    val newHeight = (animatedHeight.value - dragAmount).coerceIn(
                                        0f,
                                        fullExpandedHeight.value
                                    )
                                    coroutineScope.launch {
                                        animatedHeight.snapTo(newHeight)
                                    }
                                },
                                onDragStart = {
                                    dragStartHeight = animatedHeight.value
                                },
                                onDragEnd = {
                                    snapToTarget()
                                }
                            )
                        }
                        
                        // 评论区域（只在第二阶段显示）
                        if (isPhase2) {
                            item {
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = "COMMENTS (${mockComments.size})",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1B1F)
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                            
                            items(mockComments) { comment ->
                                CommentItem(comment = comment)
                                Spacer(Modifier.height(12.dp))
                            }
                            
                            // 底部留白
                            item {
                                Spacer(Modifier.height(48.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostContentSection(
    post: MapPostNearby,
    onDrag: (Float) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            }
    ) {
        // 帖子标题
        Text(
            text = post.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1B1F)
        )
        
        Spacer(Modifier.height(12.dp))
        
        // 作者信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A90E2))
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column {
                Text(
                    text = post.creatorUsername ?: "Unknown User",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1B1F)
                )
                Text(
                    text = "${(post.distance / 1000).toInt()} km away",
                    fontSize = 13.sp,
                    color = Color(0xFF9B9B9B)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 帖子描述
        Text(
            text = post.description ?: "No description available",
            fontSize = 16.sp,
            color = Color(0xFF4A5568),
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 位置信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = Color(0xFF9B9B9B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = post.locName ?: "Unknown Location",
                fontSize = 14.sp,
                color = Color(0xFF9B9B9B)
            )
        }
        
        Spacer(Modifier.height(20.dp))
        
        // 互动统计
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Outlined.FavoriteBorder,
                count = post.viewCount,
                label = "Views"
            )
            StatItem(
                icon = Icons.Outlined.FavoriteBorder,
                count = post.likeCount,
                label = "Likes"
            )
            StatItem(
                icon = Icons.Outlined.Create,
                count = post.commentCount,
                label = "Comments"
            )
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF636EF1),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1B1F)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF9B9B9B)
        )
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // 头像
        Image(
            painter = painterResource(id = comment.avatar),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 用户名和时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = comment.username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1B1F)
                )
                Text(
                    text = comment.timestamp,
                    fontSize = 12.sp,
                    color = Color(0xFF9B9B9B)
                )
            }
            
            Spacer(Modifier.height(6.dp))
            
            // 评论内容
            Text(
                text = comment.content,
                fontSize = 14.sp,
                color = Color(0xFF4A5568),
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            // 点赞数
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Likes",
                    tint = Color(0xFF9B9B9B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = comment.likeCount.toString(),
                    fontSize = 12.sp,
                    color = Color(0xFF9B9B9B)
                )
            }
        }
    }
}

