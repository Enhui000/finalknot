package com.cs407.knot_client_android.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.knot_client_android.data.local.TokenStore
import com.cs407.knot_client_android.utils.SimpleWebSocketManager
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val tokenStore = TokenStore(app)
    val wsManager = SimpleWebSocketManager()

    fun connectIfNeeded() {
        viewModelScope.launch {
            val jwt = tokenStore.get()
            if (jwt != null) {
                // 模拟器用 10.0.2.2；真机换成电脑局域网 IP
                wsManager.connect("ws://10.0.2.2:10827/ws", jwt)
            }
        }
    }

    fun send(json: String) = wsManager.send(json)

    override fun onCleared() {
        wsManager.disconnect()
        super.onCleared()
    }
}
