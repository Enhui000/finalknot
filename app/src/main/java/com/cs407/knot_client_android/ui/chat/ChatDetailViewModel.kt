package com.cs407.knot_client_android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.knot_client_android.data.api.MessageApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.cs407.knot_client_android.data.api.MessageDto
import com.cs407.knot_client_android.data.local.TokenStore
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.format.DateTimeFormatter

data class MessageUi(
    val msgId: Long?,          // 服务器确认后才有
    val clientMsgId: String?,   // 本地生成
    val fromUid: Long,
    val convId: Long,
    val contentText: String,
    val isMine: Boolean,
    val time: LocalDateTime,
    val sending: Boolean = false
)

data class ChatDetailUiState(
    val convId: Long,
    val title: String,
    val draft: String = "",
    val messages: List<MessageUi> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class ChatDetailViewModel(
    private val convId: Long,
    private val title: String,
    private val myUid: Long,
    private val messageApi: MessageApi,
    private val tokenStore: TokenStore,
    private val sendRawJson: (String) -> Unit
) : ViewModel() {

    private val _ui = MutableStateFlow(
        ChatDetailUiState(convId = convId, title = title, draft = "", loading = true, messages = emptyList())
    )
    val ui: StateFlow<ChatDetailUiState> = _ui.asStateFlow()

    fun updateDraft(newText: String) {
        _ui.update { it.copy(draft = newText) }
    }

    /** UI 调用：发送一条文本消息 */
    // 发送文本消息（走 WebSocket）
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // 1) 生成唯一 clientMsgId
        val clientMsgId = "c-${System.currentTimeMillis()}"

        // 2) 本地 UI 立刻加一条“临时消息”
        val newMsg = MessageUi(
            msgId = -1,                        // 等服务器分配
            clientMsgId = clientMsgId,
            fromUid = myUid,
            convId = convId,
            contentText = content,
            isMine = true,
            time = LocalDateTime.now(),
            sending = true                     // 标记正在发送（可选）
        )

        _ui.update { state ->
            state.copy(
                messages = state.messages + newMsg,
                draft = ""                     // 清空输入框
            )
        }

        // 3) 构造 WebSocket JSON 并发送
        val msg = MsgSend(
            convId = convId,
            clientMsgId = clientMsgId,
            msgType = 0,
            contentText = content
        )
        val json = msg.toJson()

        sendRawJson(json)
    }


    /** 收到 MSG_NEW 的时候调用 */
    fun onMsgNew(fromUid: Long, msgId: Long, content: String) {
        val msg = MessageUi(
            msgId = msgId,
            clientMsgId = null,
            fromUid = fromUid,
            convId = convId,
            contentText = content,
            isMine = (fromUid == myUid),
            time = LocalDateTime.now(),
            sending = false
        )

        _ui.update { state ->
            state.copy(messages = state.messages + msg)
        }
    }

    /** 收到 MSG_ACK 的时候调用，补上 msgId */
    fun onMsgAck(msgId: Long, clientMsgId: String) {
        _ui.update { state ->
            val updated = state.messages.map { m ->
                if (m.clientMsgId == clientMsgId)
                    m.copy(
                        msgId = msgId,
                        sending = false   // ⭐ 去掉发送中状态
                    )
                else m
            }
            state.copy(messages = updated)
        }
    }


    fun loadHistory(page: Int = 1, size: Int = 20) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val token = tokenStore.getAccessToken()
                if (token == null) {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = "No token"
                    )
                    return@launch
                }

                val resp = messageApi.getMessages(
                    authorization = "Bearer $token",
                    conversationId = convId,
                    page = page,
                    size = size
                )

                val pageData = resp.data
                if (resp.success && pageData != null) {
                    //val msgs = pageData.messageList.map { it.toUi(myUid) }
                    val msgs = pageData.messageList
                        .map { it.toUi(myUid) }
                        .sortedBy { it.time }
                    _ui.value = _ui.value.copy(
                        messages = msgs,
                        loading = false,
                        error = null
                    )
                } else {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = resp.message ?: resp.error
                    )
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = e.message
                )
            }
        }
    }

}

private fun MessageDto.toUi(myUid: Long): MessageUi {
    val time = try {
        // "2025-11-09T21:15:21"
        LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (_: Exception) {
        LocalDateTime.now()
    }

    return MessageUi(
        msgId = msgId,
        clientMsgId = clientMsgId,
        fromUid = senderId,              // ✅ 用 senderId
        convId = convId,
        contentText = contentText,
        isMine = senderId == myUid,      // ✅ 判断是不是自己
        time = time
    )
}
