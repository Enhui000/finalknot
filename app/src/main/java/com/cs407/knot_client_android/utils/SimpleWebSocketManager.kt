package com.cs407.knot_client_android.utils

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString

class SimpleWebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var lastJwt: String? = null
    private val gson = Gson()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

//    fun connect(url: String) {
//        if (webSocket != null) {
//            addLog("⚠️ 已经连接，请先断开")
//            return
//        }
//
//        val request = Request.Builder()
//            .url(url)
//            .build()
//
//        webSocket = client.newWebSocket(request, object : WebSocketListener() {
//            override fun onOpen(webSocket: WebSocket, response: Response) {
//                _connectionState.value = true
//                addLog("✅ 连接成功: $url")
//            }
//
//            override fun onMessage(webSocket: WebSocket, text: String) {
//                addLog("⬇️ 收到: $text")
//            }
//
//            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
//                addLog("⬇️ 收到二进制: ${bytes.hex()}")
//            }
//
//            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
//                addLog("⚠️ 正在关闭: code=$code reason=$reason")
//            }
//
//            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
//                _connectionState.value = false
//                addLog("❌ 已断开: code=$code reason=$reason")
//            }
//
//            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
//                _connectionState.value = false
//                addLog("❌ 错误: ${t.message}")
//            }
//        })
//    }

    /** 保留原 API：仅连接，不发 AUTH */
    fun connect(url: String) = connect(url, jwt = null)

    /** 新：连接 + 可选 JWT，onOpen 自动 AUTH + 30s HEARTBEAT（TTL=90s 足够） */
    fun connect(url: String, jwt: String?) {
        if (webSocket != null) {
            addLog("⚠️ 已经连接，请先断开")
            return
        }
        lastJwt = jwt

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = true
                addLog("✅ 连接成功: $url")

                // 1) 连接即发 AUTH（用 accessToken）
                lastJwt?.let { sendAuth(it) }
                    ?: addLog("⚠️ 未提供 accessToken，已跳过 AUTH")

                // 2) 开心跳
                startHeartbeat()
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
                heartbeatJob?.cancel()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = false
                addLog("❌ 错误: ${t.message}")
                heartbeatJob?.cancel()
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

    private fun sendAuth(token: String) {
        val json = gson.toJson(mapOf("type" to "AUTH", "token" to token))
        webSocket?.send(json)
        addLog("⬆️ 发送 AUTH: $json")
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30_000) // 30s 一次，TTL=90s 足够
                val hb = gson.toJson(mapOf("type" to "HEARTBEAT"))
                webSocket?.send(hb)
                addLog("⬆️ 发送 HEARTBEAT: $hb")
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}

