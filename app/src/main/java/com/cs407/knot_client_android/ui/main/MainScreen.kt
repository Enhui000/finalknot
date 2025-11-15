package com.cs407.knot_client_android.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.cs407.knot_client_android.ui.chat.ChatScreen
import com.cs407.knot_client_android.ui.components.AddPlaceSheet
import com.cs407.knot_client_android.ui.components.BottomNavigationBar
import com.cs407.knot_client_android.ui.components.ExpandableBottomSheet
import com.cs407.knot_client_android.ui.components.FloatingActionButton
import com.cs407.knot_client_android.ui.components.NavTab
import com.cs407.knot_client_android.ui.map.MapScreen
import com.cs407.knot_client_android.ui.profile.ProfileScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.cs407.knot_client_android.navigation.Screen
import com.cs407.knot_client_android.ui.friend.FriendScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect


@Composable
fun MainScreen(
    navController: NavHostController,
    initialTab: String = "MAP"
) {
    val mainVm = viewModel<MainViewModel>()

    LaunchedEffect(Unit) {
        mainVm.connectIfNeeded()   // 进入主界面即自动 AUTH + 心跳
    }

    var selectedTab by remember { 
        mutableStateOf(
            when (initialTab) {
                "CHAT" -> NavTab.CHAT
                "PROFILE" -> NavTab.PROFILE
                "FRIEND" -> NavTab.FRIEND
                else -> NavTab.MAP
            }
        ) 
    }
    
    // 展开进度（0f = 收起, 1f = 半展开, 2f = 全展开）
    var expandProgress by remember { mutableStateOf(0f) }
    
    // 控制 MapScreen 中 Add Sheet 的显示
    var isAddSheetVisible by remember { mutableStateOf(false) }
    
    // 根据展开进度计算 padding：
    // 收起时 30dp，半展开时 8dp，全展开时 0dp
    val currentPadding = when {
        expandProgress <= 1f -> {
            // 第一阶段：30dp -> 8dp
            (30 - (30 - 8) * expandProgress).dp
        }
        else -> {
            // 第二阶段：8dp -> 0dp
            val phase2Progress = expandProgress - 1f
            (8 - 8 * phase2Progress).dp
        }
    }
    
    // 禁用侧滑返回
    BackHandler(enabled = true) {
        // 什么都不做，阻止返回到登录页
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 所有页面都保持存在，只是控制可见性
        // 这样 MapScreen 不会被销毁，避免每次重新加载地图
        
        // Map 页面 - 永远存在
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selectedTab == NavTab.MAP) Modifier
                    else Modifier.alpha(0f)
                )
        ) {
            MapScreen(navController = navController)
        }
        
        // Chat 页面 - 永远存在，但可能不可见
        if (selectedTab == NavTab.CHAT) {
            ChatScreen(navController)
        }
        
        // Profile 页面 - 永远存在，但可能不可见
        if (selectedTab == NavTab.PROFILE) {
            ProfileScreen(navController)
        }
        
        // 可展开的底部导航栏 - 只在 MapScreen 时可以拖动白色滑块
        ExpandableBottomSheet(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
            },
            isDraggable = selectedTab == NavTab.MAP,
            onExpandProgressChange = { progress ->
                expandProgress = progress
            },
            modifier = Modifier
                .zIndex(1f) // 设置较低的 z-index
                .align(Alignment.BottomStart)
                .padding(start = currentPadding, bottom = currentPadding)
        )
        
        // 圆形浮动按钮 - 根据不同页面显示不同图标，展开时消失
        FloatingActionButton(
            icon = when (selectedTab) {
                NavTab.MAP -> Icons.Filled.Add // 地图页面：添加按钮
                NavTab.CHAT -> Icons.Filled.Person // 聊天页面：搜索按钮
                NavTab.PROFILE -> null // 个人资料页面：不显示
                NavTab.FRIEND -> Icons.Filled.Person // 好友页面：搜索按钮
            },
            onClick = {
                when (selectedTab) {
                    NavTab.MAP -> {
                        // 打开 Add Sheet
                        isAddSheetVisible = true
                    }
                    NavTab.CHAT -> {
                        // 跳转至好友页面
                        navController.navigate(Screen.Friend.route)
                    }
                    NavTab.PROFILE -> {
                        // 不显示按钮
                    }
                    NavTab.FRIEND -> {
                        // 跳转至好友页面
                        navController.navigate(Screen.Friend.route)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = currentPadding, bottom = currentPadding)
                .alpha(1f - expandProgress) // 展开时淡出消失
        )
        
        // Add Place Sheet - 覆盖在所有元素之上
        AddPlaceSheet(
            isVisible = isAddSheetVisible,
            onDismiss = { isAddSheetVisible = false },
            modifier = Modifier
                .zIndex(100f) // 最高 z-index，覆盖所有元素
                .align(Alignment.BottomCenter)
        )
    }
}

