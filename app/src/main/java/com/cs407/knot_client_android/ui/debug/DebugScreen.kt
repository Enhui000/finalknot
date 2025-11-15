package com.cs407.knot_client_android.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cs407.knot_client_android.navigation.Screen
import com.cs407.knot_client_android.ui.components.FloatingActionButton
import com.cs407.knot_client_android.utils.SimpleWebSocketManager

@Composable
fun DebugScreen(
    navController: NavHostController
) {
    val wsManager = remember { SimpleWebSocketManager() }
    val isConnected by wsManager.connectionState.collectAsState()
    val messages by wsManager.messages.collectAsState()
    
    var wsUrl by remember { mutableStateOf("ws://10.0.2.2:10827/ws") }
    var messageInput by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()

    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F6F4),
                        Color(0xFFF3F0FA)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题 + 状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WS 调试",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFEF5350)
                ) {
                    Text(
                        text = if (isConnected) "已连接" else "未连接",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // WebSocket URL 输入
            OutlinedTextField(
                value = wsUrl,
                onValueChange = { wsUrl = it },
                label = { Text("WebSocket URL") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnected,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 连接/断开按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isConnected) {
                            wsManager.disconnect()
                        } else {
                            wsManager.connect(wsUrl)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFFEF5350) else Color(0xFF6366F1)
                    )
                ) {
                    Text(if (isConnected) "断开" else "连接")
                }
                
                Button(
                    onClick = { wsManager.clearLogs() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9E9E9E)
                    )
                ) {
                    Text("清空日志")
                }
            }

            // 消息输入
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                label = { Text("JSON 消息") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("""{"type":"HEARTBEAT"}""") },
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            // 发送按钮
            Button(
                onClick = {
                    if (messageInput.isNotEmpty()) {
                        wsManager.send(messageInput)
                        messageInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isConnected && messageInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                )
            ) {
                Text("发送")
            }

            // 日志区域
            Text(
                text = "日志 (${messages.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1F2937).copy(alpha = 0.95f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages) { message ->
                        Text(
                            text = message,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE5E7EB),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 返回按钮
        FloatingActionButton(
            icon = Icons.Outlined.Home,
            onClick = {
                wsManager.disconnect()
                navController.navigate(Screen.Login.route)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DebugScreenPreview() {
    DebugScreen(navController = rememberNavController())
}

