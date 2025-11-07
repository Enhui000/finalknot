package com.cs407.knot_client_android.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cs407.knot_client_android.ui.components.BottomNavigationBar
import com.cs407.knot_client_android.ui.components.NavTab
import com.cs407.knot_client_android.navigation.Screen

@Composable
fun ProfileScreen(
    navController: NavHostController
) {
    var selectedTab by remember { mutableStateOf(NavTab.PROFILE) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 主内容
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This is a Profile page",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 底部导航栏 - 绝对定位在左下角，贴近设备圆角
        BottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                when (tab) {
                    NavTab.MAP -> {
                        navController.navigate(Screen.Map.route)
                    }
                    NavTab.CHAT -> {
                        navController.navigate(Screen.Chat.route)
                    }
                    NavTab.PROFILE -> {
                        // 已经在 Profile 页面，不需要导航
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, bottom = 30.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}

