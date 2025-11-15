package com.cs407.knot_client_android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cs407.knot_client_android.ui.login.LoginScreen
import com.cs407.knot_client_android.ui.main.MainScreen
import com.cs407.knot_client_android.ui.friend.FriendScreen
import com.cs407.knot_client_android.ui.debug.DebugScreen
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.knot_client_android.data.local.TokenStore
import com.cs407.knot_client_android.ui.chat.ChatDetailRoute
import com.cs407.knot_client_android.ui.main.MainViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main/{selectedTab}") {
        fun createRoute(selectedTab: String = "MAP") = "main/$selectedTab"
    }
    object Friend : Screen("friend")
    object Debug : Screen("debug")

    object ChatDetail : Screen("chat/{convId}/{title}") {
        fun createRoute(convId: Long, title: String): String {
            // title 里可能有空格/中文，要编码一下
            val encoded = Uri.encode(title)
            return "chat/$convId/$encoded"
        }
    }
}

// 主要的 Navigation 设置函数
@Composable
fun SetupNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route 
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("selectedTab") {
                    type = NavType.StringType
                    defaultValue = "MAP"
                }
            )
        ) { backStackEntry ->
            val selectedTab = backStackEntry.arguments?.getString("selectedTab") ?: "MAP"
            MainScreen(
                navController = navController,
                initialTab = selectedTab
            )
        }
        composable(route = Screen.Friend.route) {
            FriendScreen(navController = navController)
        }
        composable(route = Screen.Debug.route) {
            DebugScreen(navController = navController)
        }
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("convId") { type = NavType.LongType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getLong("convId")!!
            val title = backStackEntry.arguments?.getString("title") ?: "Chat"

            // 从 NavGraph 作用域拿到 MainViewModel（里面有 WebSocket）
            //val mainVm: MainViewModel = viewModel()
            val mainBackStackEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Main.route)
            }
            val mainVm: MainViewModel = viewModel(mainBackStackEntry)

            // TODO: 把当前用户 id 换成真实的（可以从 TokenStore 里取）
            //val myUid = 2L
            val context = LocalContext.current
            val tokenStore = remember(context.applicationContext) {
                TokenStore(context.applicationContext)
            }

            val myUid = tokenStore.getUserId()
            if (myUid == null) {
                Log.w("NavGraph", "Missing user id in TokenStore, skip entering chat detail")
                return@composable
            }

            ChatDetailRoute(
                navController = navController,
                convId = convId,
                title = title,
                myUid = myUid,
                mainVm = mainVm
            )
        }
    }
}