package com.cs407.knot_client_android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.knot_client_android.data.local.TokenStore
import com.cs407.knot_client_android.ui.friend.FriendRestClient
import com.cs407.knot_client_android.ui.friend.FriendSummary
import com.cs407.knot_client_android.ui.main.MainViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

@Suppress("UnusedParameter")
@Composable
fun ChatScreen(
    navController: NavHostController
) {
    val mainVm = viewModel<MainViewModel>()
    val isConnected by mainVm.wsManager.connectionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val restClient = remember { FriendRestClient(context) }
    val gson = remember { Gson() }

    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    var selectedConvId by remember { mutableStateOf<Long?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var messageInput by rememberSaveable { mutableStateOf("") }
    val conversationMessages = remember { mutableStateMapOf<Long, List<ChatMessage>>() }
    val pendingByClientId = remember { mutableStateMapOf<String, Long>() }
    var hasShownConnectionSnack by remember { mutableStateOf(false) }

    val currentUid = remember { tokenStore.getUserId() }
    val selectedFriend = friends.firstOrNull { it.convId == selectedConvId }
    val availableConversations = friends.filter { it.convId != null }
    val selectedMessages = selectedConvId?.let { conversationMessages[it] } ?: emptyList()
    val messageListState = rememberLazyListState()

    suspend fun refreshFriends(showToast: Boolean) {
        isLoading = true
        runCatching { restClient.fetchSnapshot() }
            .onSuccess { snapshot ->
                val convFriends = snapshot.friends.filter { it.convId != null }
                friends = convFriends
                if (convFriends.isEmpty()) {
                    selectedConvId = null
                } else if (selectedConvId == null) {
                    selectedConvId = convFriends.first().convId
                } else if (convFriends.none { it.convId == selectedConvId }) {
                    selectedConvId = convFriends.firstOrNull()?.convId
                }
                if (showToast) {
                    snackbarHostState.showSnackbar("Friend list refreshed")
                }
            }
            .onFailure { error ->
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Failed to load friends"
                snackbarHostState.showSnackbar(message)
            }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        refreshFriends(showToast = false)
    }

    LaunchedEffect(isConnected) {
        if (!hasShownConnectionSnack) {
            hasShownConnectionSnack = true
        } else {
            val text = if (isConnected) "WebSocket connected" else "WebSocket disconnected"
            snackbarHostState.showSnackbar(text)
        }
    }

    LaunchedEffect(selectedMessages.size, selectedConvId) {
        if (selectedMessages.isNotEmpty()) {
            messageListState.animateScrollToItem(max(0, selectedMessages.lastIndex))
        }
    }

    LaunchedEffect(mainVm.wsManager) {
        mainVm.wsManager.incomingMessages.collect { raw ->
            when (val event = parseChatSocketMessage(raw)) {
                is ChatIncomingEvent -> {
                    val convId = event.convId
                    if (friends.none { it.convId == convId }) {
                        val placeholder = FriendSummary(
                            userId = event.fromUid,
                            username = null,
                            avatarUrl = null,
                            convId = convId,
                            since = event.serverTime
                        )
                        friends = (friends + placeholder)
                            .distinctBy { it.convId ?: it.userId }
                            .sortedBy { it.username ?: it.userId.toString() }
                        if (selectedConvId == null) {
                            selectedConvId = convId
                        }
                    }
                    val message = ChatMessage(
                        convId = convId,
                        msgId = event.msgId,
                        clientMsgId = "server-${event.msgId ?: System.currentTimeMillis()}",
                        senderId = event.fromUid,
                        content = event.content,
                        timestamp = event.serverTime ?: System.currentTimeMillis(),
                        isOwn = currentUid != null && currentUid == event.fromUid,
                        status = MessageStatus.SENT
                    )
                    val updated = (conversationMessages[convId] ?: emptyList()) + message
                    conversationMessages[convId] = updated
                    if (!message.isOwn && convId != selectedConvId) {
                        snackbarHostState.showSnackbar("New message in conversation $convId")
                    }
                }
                is ChatAckEvent -> {
                    val convId = pendingByClientId.remove(event.clientMsgId)
                    if (convId != null) {
                        val updated = (conversationMessages[convId] ?: emptyList()).map { existing ->
                            if (existing.clientMsgId == event.clientMsgId) {
                                existing.copy(
                                    status = MessageStatus.SENT,
                                    msgId = event.msgId ?: existing.msgId,
                                    timestamp = event.serverTime ?: existing.timestamp
                                )
                            } else {
                                existing
                            }
                        }
                        conversationMessages[convId] = updated
                    }
                }
                is ChatInfoEvent -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                null -> Unit
            }
        }
    }

    val brush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF8F6F4),
                Color(0xFFF3F0FA)
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChatHeaderCard(
                isConnected = isConnected,
                currentUid = currentUid,
                selectedFriend = selectedFriend,
                onRefresh = { scope.launch { refreshFriends(true) } }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6366F1)
                )
            }

            ConversationPicker(
                conversations = availableConversations,
                selectedConvId = selectedConvId,
                onConversationSelected = { friend -> friend.convId?.let { selectedConvId = it } }
            )

            if (selectedConvId == null) {
                EmptyConversationState()
            } else {
                MessageArea(
                    modifier = Modifier.weight(1f),
                    messages = selectedMessages,
                    listState = messageListState
                )
                MessageInputBar(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    onSend = {
                        val convId = selectedConvId ?: return@MessageInputBar
                        val text = messageInput.trim()
                        if (text.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("Message cannot be empty") }
                            return@MessageInputBar
                        }
                        if (!isConnected) {
                            scope.launch { snackbarHostState.showSnackbar("WebSocket offline, unable to send") }
                            return@MessageInputBar
                        }
                        val clientMsgId = "c-${System.currentTimeMillis()}-${Random.nextInt(1000, 9999)}"
                        val pending = ChatMessage(
                            convId = convId,
                            msgId = null,
                            clientMsgId = clientMsgId,
                            senderId = currentUid ?: -1L,
                            content = text,
                            timestamp = System.currentTimeMillis(),
                            isOwn = true,
                            status = MessageStatus.PENDING
                        )
                        val updated = (conversationMessages[convId] ?: emptyList()) + pending
                        conversationMessages[convId] = updated
                        pendingByClientId[clientMsgId] = convId
                        val payload = gson.toJson(
                            mapOf(
                                "type" to "MSG_SEND",
                                "convId" to convId,
                                "clientMsgId" to clientMsgId,
                                "msgType" to 0,
                                "contentText" to text
                            )
                        )
                        mainVm.send(payload)
                        messageInput = ""
                    },
                    enabled = isConnected && messageInput.isNotBlank() && selectedConvId != null
                )
            }
        }
    }
}

@Composable
private fun ChatHeaderCard(
    isConnected: Boolean,
    currentUid: Long?,
    selectedFriend: FriendSummary?,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) Color(0xFF16A34A) else Color(0xFFB91C1C)
                    )
                    Text(
                        text = "Logged in UID: ${currentUid ?: "unknown"}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "Refresh conversations")
                }
            }
            if (selectedFriend != null) {
                Text(
                    text = "Chatting with ${selectedFriend.username ?: "UID ${selectedFriend.userId}"}",
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "Conversation ID: ${selectedFriend.convId ?: "unknown"}",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            } else {
                Text(
                    text = "Select a friend with a valid conversation to start messaging.",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@Composable
private fun ConversationPicker(
    conversations: List<FriendSummary>,
    selectedConvId: Long?,
    onConversationSelected: (FriendSummary) -> Unit
) {
    if (conversations.isEmpty()) {
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(conversations, key = { it.convId ?: it.userId }) { friend ->
            val isSelected = friend.convId == selectedConvId
            Surface(
                modifier = Modifier.clickable(enabled = friend.convId != null) {
                    friend.convId?.let { onConversationSelected(friend) }
                },
                shape = RoundedCornerShape(24.dp),
                color = if (isSelected) Color(0xFF6366F1) else Color.White,
                contentColor = if (isSelected) Color.White else Color(0xFF111827)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = friend.username ?: "UID ${friend.userId}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "UID ${friend.userId}",
                        fontSize = 11.sp,
                        color = if (isSelected) Color(0xFFE0E7FF) else Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No conversations yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111827)
            )
            Text(
                text = "Add a friend to automatically create a chat channel.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MessageArea(
    modifier: Modifier,
    messages: List<ChatMessage>,
    listState: LazyListState
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.clientMsgId }) { message ->
                MessageBubble(message = message)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isOwn) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isOwn) Color(0xFF6366F1) else Color(0xFFF3F4F6)
    val textColor = if (message.isOwn) Color.White else Color(0xFF111827)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (message.isOwn) "You (UID ${message.senderId})" else "UID ${message.senderId}",
                fontSize = 11.sp,
                color = if (message.isOwn) Color(0xFFE0E7FF) else Color(0xFF6B7280)
            )
            Text(
                text = message.content,
                fontSize = 15.sp,
                color = textColor
            )
            val status = when (message.status) {
                MessageStatus.PENDING -> "Sending..."
                MessageStatus.SENT -> "Delivered"
                MessageStatus.FAILED -> "Failed"
            }
            Text(
                text = status,
                fontSize = 11.sp,
                color = if (message.isOwn) Color(0xFFDDE6FF) else Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text("Type a message") }
        )
        Button(
            onClick = onSend,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Text(text = "Send", color = Color.White)
        }
    }
}
