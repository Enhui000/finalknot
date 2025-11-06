package com.cs407.knot_client_android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.knot_client_android.ui.login.LoginScreen
import com.cs407.knot_client_android.ui.login.LoginScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
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
    }
}