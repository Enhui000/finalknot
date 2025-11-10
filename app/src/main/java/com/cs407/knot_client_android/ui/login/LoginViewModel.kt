// ui/login/LoginViewModel.kt
package com.cs407.knot_client_android.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.knot_client_android.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    // 修改为你的后台地址：模拟器用 10.0.2.2，真机用电脑局域网 IP
    private val repo = AuthRepository(app, baseUrl = "http://10.0.2.2:8080")

    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    fun submit(isLogin: Boolean, username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try {
                // 简单校验
                if (username.isBlank() || password.isBlank()) {
                    error.value = "Username / Password 不能为空"
                    return@launch
                }
                if (isLogin) {
                    repo.login(username, password)
                } else {
                    repo.registerThenLoginIfNeeded(username, password)
                }
                onSuccess()
            } catch (e: Exception) {
                error.value = e.message ?: if (isLogin) "登录失败" else "注册失败"
            } finally {
                loading.value = false
            }
        }
    }
}
