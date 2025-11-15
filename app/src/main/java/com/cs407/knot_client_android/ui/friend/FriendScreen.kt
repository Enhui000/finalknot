package com.cs407.knot_client_android.ui.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cs407.knot_client_android.navigation.Screen
import com.cs407.knot_client_android.ui.components.FloatingActionButton

@Composable
fun FriendScreen(
    navController: NavHostController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F6F4), // 顶部：温暖米白
                        Color(0xFFF3F0FA)  // 底部：淡紫色调
                    )
                )
            )
    ) {
        // 中心内容
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This is a Friend page",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 右下角浮动按钮 - 返回聊天页面
        FloatingActionButton(
            icon = Icons.Outlined.Email,
            onClick = {
                navController.navigate(Screen.Main.createRoute("CHAT")) {
                    popUpTo(Screen.Main.createRoute("MAP")) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 30.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendScreenPreview() {
    FriendScreen(navController = rememberNavController())
}

