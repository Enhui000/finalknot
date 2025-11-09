package com.cs407.knot_client_android.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okio.ByteString

class SimpleWebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    fun connect(url: String) {
        if (webSocket != null) {
            addLog("⚠️ 已经连接，请先断开")
            return
        }

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = true
                addLog("✅ 连接成功: $url")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                addLog("⬇️ 收到: $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                addLog("⬇️ 收到二进制: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                addLog("⚠️ 正在关闭: code=$code reason=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = false
                addLog("❌ 已断开: code=$code reason=$reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = false
                addLog("❌ 错误: ${t.message}")
            }
        })
    }

    fun send(message: String) {
        if (webSocket == null || !_connectionState.value) {
            addLog("⚠️ 未连接，无法发送")
            return
        }

        val success = webSocket?.send(message) ?: false
        if (success) {
            addLog("⬆️ 发送: $message")
        } else {
            addLog("❌ 发送失败")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "用户主动断开")
        webSocket = null
        _connectionState.value = false
    }

    fun clearLogs() {
        _messages.value = emptyList()
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _messages.value = _messages.value + "[$timestamp] $message"
    }
}

