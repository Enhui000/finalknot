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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.FavoriteBorder
//import androidx.compose.material.icons.outlined.ChatBubbleOutline
//import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.material.icons.outlined.Face
import androidx.compose.ui.platform.LocalContext
import com.cs407.knot_client_android.R
import com.cs407.knot_client_android.data.api.RetrofitProvider
import com.cs407.knot_client_android.data.local.TokenStore
import com.cs407.knot_client_android.data.model.response.ConversationMessage
import com.cs407.knot_client_android.data.model.response.MapPostDetailResponse
import com.cs407.knot_client_android.data.model.response.MapPostNearby
import com.cs407.knot_client_android.data.model.WebSocketMessage
import com.cs407.knot_client_android.data.model.MessageNewMessage
import com.cs407.knot_client_android.ui.main.MainViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Comment Data Class (converted from ConversationMessage)
data class Comment(
    val commentId: Long,
    val username: String,
    val content: String,
    val timestamp: String,
    val likeCount: Int
)

@Composable
fun PostDetailSheet(
    post: MapPostNearby?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 状态管理
    var postDetail by remember { mutableStateOf<MapPostDetailResponse?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoadingDetail by remember { mutableStateOf(false) }
    var isLoadingComments by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localCommentCount by remember { mutableStateOf(0) }
    
    // 加载帖子详情和评论
    LaunchedEffect(post?.mapPostId, isVisible) {
        if (isVisible && post != null) {
            // 加载帖子详情
            isLoadingDetail = true
            errorMessage = null
            try {
                val tokenStore = TokenStore(context)
                val token = tokenStore.getAccessToken()
                val apiService = RetrofitProvider.createMapPostService("http://10.0.2.2:8080")
                
                val response = apiService.getMapPostDetail("Bearer $token", post.mapPostId)
                if (response.success && response.data != null) {
                    postDetail = response.data
                    localCommentCount = response.data.commentCount
                    
                    // 加载评论
                    isLoadingComments = true
                    try {
                        val commentsResponse = apiService.getConversationMessages(
                            token = "Bearer $token",
                            conversationId = response.data.convId,
                            page = 1,
                            size = 20
                        )
                        if (commentsResponse.success && commentsResponse.data != null) {
                            // 转换 ConversationMessage 到 Comment
                            comments = commentsResponse.data.messageList.map { msg: ConversationMessage ->
                                Comment(
                                    commentId = msg.msgId,
                                    username = "User ${msg.senderId}", // TODO: 需要获取用户名
                                    content = msg.contentText ?: "",
                                    timestamp = formatTimestamp(msg.createdAt),
                                    likeCount = 0 // TODO: 需要获取点赞数
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoadingComments = false
                    }
                } else {
                    errorMessage = response.message ?: "获取帖子详情失败"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message ?: "网络错误"
            } finally {
                isLoadingDetail = false
            }
        }
    }
    
    // 🔔 监听 WebSocket 消息（实时接收新评论）
    LaunchedEffect(isVisible, postDetail?.convId) {
        postDetail?.let { currentPostDetail ->
            if (isVisible) {
                mainViewModel.wsManager.rawMessages.collect { message ->
                    message?.let {
                        try {
                            val gson = Gson()
                            val baseMessage = gson.fromJson(it, WebSocketMessage::class.java)
                            
                            if (baseMessage.type == "MSG_NEW") {
                                val msgNew = gson.fromJson(it, MessageNewMessage::class.java)
                                
                                // 只处理当前对话的消息
                                if (msgNew.convId == currentPostDetail.convId) {
                                    // 创建新评论
                                    val newComment = Comment(
                                        commentId = msgNew.msgId,
                                        username = "User ${msgNew.fromUid}",
                                        content = msgNew.contentText ?: "",
                                        timestamp = "just now",
                                        likeCount = 0
                                    )
                                    
                                    // 添加到评论列表开头
                                    comments = listOf(newComment) + comments
                                    
                                    // 本地评论数 +1
                                    localCommentCount += 1
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    
    PostDetailSheetContent(
        post = post,
        postDetail = postDetail,
        comments = comments,
        localCommentCount = localCommentCount,
        isVisible = isVisible,
        isLoadingDetail = isLoadingDetail,
        isLoadingComments = isLoadingComments,
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        mainViewModel = mainViewModel,
        modifier = modifier
    )
}

// 格式化时间戳
private fun formatTimestamp(timestamp: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timestamp)
        val now = Date()
        val diff = now.time - (date?.time ?: 0)
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "just now"
        }
    } catch (e: Exception) {
        timestamp
    }
}

@Composable
private fun PostDetailSheetContent(
    post: MapPostNearby?,
    postDetail: MapPostDetailResponse?,
    comments: List<Comment>,
    localCommentCount: Int,
    isVisible: Boolean,
    isLoadingDetail: Boolean,
    isLoadingComments: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    
    // 测量内容高度
    var contentHeightPx by remember { mutableStateOf(0) }
    val contentHeight = with(density) { contentHeightPx.toDp() }
    
    // 输入框状态
    var commentText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var shouldFocusInput by remember { mutableStateOf(false) }
    
    // 两个高度状态：半展开(动态)、全展开(94%)
    // 半展开高度：内容高度 + padding，但不超过屏幕的 70%
    val halfExpandedHeight = remember(contentHeight) {
        if (contentHeight > 0.dp) {
            (contentHeight + 48.dp).coerceAtMost(screenHeight * 0.7f)
        } else {
            screenHeight * 0.5f  // 默认值，在测量完成前使用
        }
    }
    val fullExpandedHeight = screenHeight * 0.94f  // 全展开：94%
    
    // 动画状态
    val animatedHeight = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    // 记录拖动起始高度
    var dragStartHeight by remember { mutableStateOf(0f) }
    
    // 监听 isVisible 和 halfExpandedHeight 变化，触发动画
    LaunchedEffect(isVisible, halfExpandedHeight) {
        if (isVisible) {
            // 展开到半展开状态（动态高度）
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
    
    // 点击评论按钮的处理
    fun onCommentClick() {
        coroutineScope.launch {
            // 如果不是全展开状态，先展开到全展开
            if (animatedHeight.value < fullExpandedHeight.value) {
                animatedHeight.animateTo(
                    targetValue = fullExpandedHeight.value,
                    animationSpec = spring(
                        dampingRatio = 0.70f,
                        stiffness = 120f
                    )
                )
            }
            // 设置标志以请求焦点
            shouldFocusInput = true
        }
    }
    
    // 当需要聚焦输入框时，请求焦点
    LaunchedEffect(shouldFocusInput) {
        if (shouldFocusInput) {
            kotlinx.coroutines.delay(300) // 等待动画完成
            focusRequester.requestFocus()
            shouldFocusInput = false
        }
    }
    
    // 关闭按钮触发的关闭动画
    fun closeWithAnimation() {
        coroutineScope.launch {
            animatedHeight.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessLow
                )
            )
            // 动画完成后通知外部
            onDismiss()
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
                        .clip(RoundedCornerShape(currentCornerRadius))
                        .padding(horizontal = 28.dp)
                ) {
                    Spacer(Modifier.height(28.dp))
                    
                    // 可滚动内容
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 帖子内容区域（始终显示）- 带拖动手势和高度测量
                        item {
                            if (isLoadingDetail) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = Color(0xFF636EF1)
                                    )
                                }
                            } else if (errorMessage != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = errorMessage,
                                        fontSize = 14.sp,
                                        color = Color(0xFFE53E3E)
                                    )
                                }
                            } else if (postDetail != null) {
                                PostContentSection(
                                    postDetail = postDetail,
                                    post = post,
                                    localCommentCount = localCommentCount,
                                    onCommentClick = { onCommentClick() },
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
                                    },
                                    onHeightMeasured = { heightPx ->
                                        contentHeightPx = heightPx
                                    }
                                )
                            }
                        }
                        
                        // 评论区域（只在第二阶段显示）
                        if (isPhase2) {
                            item {
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = "COMMENTS ($localCommentCount)",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1B1F)
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                            
                            // 加载中状态
                            if (isLoadingComments) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            color = Color(0xFF636EF1)
                                        )
                                    }
                                }
                            } else {
                                items(comments) { comment ->
                                    CommentItem(comment = comment)
                                    Spacer(Modifier.height(12.dp))
                                }
                                
                                // 空状态
                                if (comments.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No comments yet",
                                                fontSize = 14.sp,
                                                color = Color(0xFF9B9B9B)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // 评论输入框
                            item {
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(24.dp))
                                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 16.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = commentText,
                                        onValueChange = { commentText = it },
                                        placeholder = {
                                            Text(
                                                text = "Write a comment...",
                                                color = Color(0xFF9B9B9B),
                                                fontSize = 14.sp
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(focusRequester),
                                        singleLine = false,
                                        maxLines = 4
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            if (commentText.isNotBlank() && postDetail != null) {
                                                // 生成唯一的 clientMsgId（使用时间戳）
                                                val clientMsgId = "c-${System.currentTimeMillis()}"
                                                
                                                // 构建 WebSocket 消息
                                                val message = mapOf(
                                                    "type" to "MSG_SEND",
                                                    "convId" to postDetail.convId,
                                                    "clientMsgId" to clientMsgId,
                                                    "msgType" to 0,  // 0 = 文本消息
                                                    "contentText" to commentText
                                                )
                                                
                                                // 通过 WebSocket 发送
                                                val gson = Gson()
                                                val json = gson.toJson(message)
                                                mainViewModel.send(json)
                                                
                                                // 清空输入框
                                                commentText = ""
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = if (commentText.isNotBlank()) Color(0xFF636EF1) else Color(0xFF9B9B9B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            
                            // 底部留白
                            item {
                                Spacer(Modifier.height(48.dp))
                            }
                        }
                    }
                }
                
                // 右上角关闭按钮 - 浮动在内容之上
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 24.dp)
                ) {
                    // 动画状态管理
                    val closeButtonInteractionSource = remember { MutableInteractionSource() }
                    val isCloseButtonPressed by closeButtonInteractionSource.collectIsPressedAsState()
                    
                    // Apple-style 双阶段弹性动画
                    val closeButtonScale = remember { Animatable(1f) }
                    
                    LaunchedEffect(isCloseButtonPressed) {
                        if (isCloseButtonPressed) {
                            // 按下：快速放大一点点
                            closeButtonScale.animateTo(
                                targetValue = 1.2f,
                                animationSpec = tween(
                                    durationMillis = 170, 
                                    easing = LinearOutSlowInEasing
                                )
                            )
                        } else {
                            // 松手：先缩回一点再弹回 1
                            closeButtonScale.animateTo(
                                targetValue = 0.88f,
                                animationSpec = tween(
                                    durationMillis = 155, 
                                    easing = FastOutLinearInEasing
                                )
                            )
                            // 然后自然回弹到 1
                            closeButtonScale.animateTo(
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
                            .size(56.dp)
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
                            .size(56.dp)
                            .scale(closeButtonScale.value)
                            .border(
                                width = 1.dp,
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
                                onClick = { closeWithAnimation() },
                                indication = null,
                                interactionSource = closeButtonInteractionSource
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(30.dp),
                            tint = if (isCloseButtonPressed) 
                                Color(0xFF636EF1) // 按下时：蓝紫色
                            else 
                                Color(0xFF6B7280) // 正常时：gray-600
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostContentSection(
    postDetail: MapPostDetailResponse,
    post: MapPostNearby?,
    localCommentCount: Int, // 本地评论数（实时更新）
    onCommentClick: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onHeightMeasured: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                // 测量内容高度
                onHeightMeasured(coordinates.size.height)
            }
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
            text = postDetail.title,
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
                    text = postDetail.creatorUsername,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1B1F)
                )
                Text(
                    text = "${(post?.distance ?: 0.0 / 1000).toInt()} meters away",
                    fontSize = 13.sp,
                    color = Color(0xFF9B9B9B)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 帖子描述
        Text(
            text = postDetail.description ?: "No description available",
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
                text = postDetail.locName ?: "Unknown Location",
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
                icon = Icons.Outlined.Face,
                count = postDetail.viewCount,
                label = "Views"
            )
            StatItem(
                icon = Icons.Outlined.FavoriteBorder,
                count = postDetail.likeCount,
                label = "Likes"
            )
            StatItem(
                icon = Icons.Outlined.Create,
                count = localCommentCount,
                label = "Comnt",
                onClick = onCommentClick
            )
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(8.dp)
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
            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF4A90E2))
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

