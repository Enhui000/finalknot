package com.cs407.knot_client_android.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.knot_client_android.data.model.response.UserSettings
import com.cs407.knot_client_android.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.cs407.knot_client_android.data.model.request.UpdateUserSettingsRequest

/**
 * Profile 页面 ViewModel
 */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = UserRepository(app, baseUrl = "http://10.0.2.2:8080")

    // 加载状态
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 用户设置数据
    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings

    /**
     * 加载用户设置
     */
    fun loadUserSettings() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val settings = repository.getUserSettings()
                _userSettings.value = settings
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load user settings"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 更新用户设置
     */
    suspend fun updateUserSettings(nickname: String, statusMessage: String, email: String, gender: String, birthdate: String, privacyLevel: String, discoverable: Boolean): Boolean {
        return try {
            _loading.value = true
            _error.value = null
            
            val request = UpdateUserSettingsRequest(nickname, statusMessage, email, gender, birthdate, privacyLevel, discoverable)
            val response = repository.updateUserSettings(request)
            
            // 更新本地状态
            _userSettings.value = response
            
            _loading.value = false
            true
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to update user settings"
            _loading.value = false
            false
        }
    }
}

