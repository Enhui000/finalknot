package com.cs407.knot_client_android.ui.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.knot_client_android.navigation.Screen
import com.cs407.knot_client_android.ui.components.FloatingActionButton
import com.cs407.knot_client_android.ui.main.MainViewModel
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FriendScreen(
    navController: NavHostController
) {
    val mainVm = viewModel<MainViewModel>()
    val isConnected by mainVm.wsManager.connectionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val pendingSends = remember { mutableStateListOf<PendingSendContext>() }
    val context = LocalContext.current
    val restClient = remember { FriendRestClient(context) }

    var friendState by remember { mutableStateOf(FriendUiState()) }
    var receiverInput by rememberSaveable { mutableStateOf("") }
    var messageInput by rememberSaveable { mutableStateOf("") }
    var nextTempId by remember { mutableStateOf(-1L) }
    var hasShownConnectionToast by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mainVm.connectIfNeeded()
        runCatching { restClient.fetchSnapshot() }
            .onSuccess { snapshot ->
                friendState = friendState.copy(
                    incomingRequests = snapshot.incomingRequests,
                    outgoingRequests = snapshot.outgoingRequests,
                    friends = snapshot.friends
                )
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                val message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Failed to load friend data"
                snackbarHostState.showSnackbar(message)
            }
    }

    LaunchedEffect(isConnected) {
        friendState = friendState.copy(isConnected = isConnected)
        if (hasShownConnectionToast) {
            val text = if (isConnected) "WebSocket connected" else "WebSocket disconnected"
            snackbarHostState.showSnackbar(text)
        } else {
            hasShownConnectionToast = true
        }
    }

    LaunchedEffect(mainVm.wsManager) {
        mainVm.wsManager.incomingMessages.collect { raw ->
            val update = handleFriendSocketMessage(raw, friendState, pendingSends, gson)
            friendState = update.state
            update.message?.let { msg ->
                snackbarHostState.showSnackbar(
                    message = msg,
                    withDismissAction = true
                )
            }
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8F6F4),
                Color(0xFFF3F0FA)
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                icon = Icons.Outlined.Email,
                onClick = {
                    navController.navigate(Screen.Main.createRoute("CHAT")) {
                        popUpTo(Screen.Main.createRoute("MAP")) { inclusive = true }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = paddingValues.calculateTopPadding() + 24.dp,
                    bottom = paddingValues.calculateBottomPadding() + 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FriendHeaderCard(
                        isConnected = friendState.isConnected,
                        onRefresh = {
                            scope.launch {
                                val connected = friendState.isConnected
                                if (!connected) {
                                    snackbarHostState.showSnackbar("WebSocket offline, falling back to HTTP sync")
                                }
                                try {
                                    val snapshot = restClient.fetchSnapshot()
                                    friendState = friendState.copy(
                                        incomingRequests = snapshot.incomingRequests,
                                        outgoingRequests = snapshot.outgoingRequests,
                                        friends = snapshot.friends
                                    )
                                    snackbarHostState.showSnackbar("Friend data refreshed")
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    val message = e.message?.takeIf { it.isNotBlank() }
                                        ?: "Failed to refresh friend data"
                                    snackbarHostState.showSnackbar(message)
                                }
                                if (connected) {
                                    mainVm.send(gson.toJson(mapOf("type" to "FRIEND_REQUEST_LIST")))
                                    mainVm.send(gson.toJson(mapOf("type" to "FRIEND_LIST")))
                                }
                            }
                        }
                    )
                }

                item {
                    SendRequestCard(
                        receiverInput = receiverInput,
                        onReceiverChange = { receiverInput = it },
                        messageInput = messageInput,
                        onMessageChange = { messageInput = it },
                        isConnected = friendState.isConnected,
                        onSendClick = {
                            val receiverId = receiverInput.trim().toLongOrNull()
                            if (receiverId == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid user ID")
                                }
                                return@SendRequestCard
                            }
                            val message = messageInput.ifBlank { "Hi! Let's connect as friends." }
                            if (!friendState.isConnected) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("WebSocket is offline, unable to send")
                                }
                                return@SendRequestCard
                            }

                            val tempId = nextTempId
                            nextTempId -= 1

                            val placeholder = FriendRequestUiModel(
                                requestId = tempId,
                                requesterId = -1L,
                                receiverId = receiverId,
                                message = message,
                                status = FriendRequestStatus.PENDING,
                                createdAtMs = System.currentTimeMillis(),
                                convId = null,
                                requesterName = "You",
                                requesterAvatar = null,
                                receiverName = null,
                                isIncoming = false
                            )

                            friendState = friendState.copy(
                                outgoingRequests = (friendState.outgoingRequests + placeholder)
                                    .sortedByDescending { it.createdAtMs }
                            )
                            pendingSends.add(
                                PendingSendContext(
                                    placeholderId = tempId,
                                    existingRequestId = null,
                                    receiverId = receiverId,
                                    message = message
                                )
                            )
                            mainVm.send(
                                gson.toJson(
                                    mapOf(
                                        "type" to "FRIEND_REQUEST_SEND",
                                        "receiverId" to receiverId,
                                        "message" to message
                                    )
                                )
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar("Friend request sent, waiting for confirmation")
                            }
                            receiverInput = ""
                            messageInput = ""
                        }
                    )
                }

                item {
                    SectionTitle(
                        title = "Incoming requests",
                        subtitle = "Review and respond to new invites"
                    )
                }

                if (friendState.incomingRequests.isEmpty()) {
                    item {
                        EmptyHintCard(text = "No pending friend requests yet")
                    }
                } else {
                    items(friendState.incomingRequests, key = { it.requestId }) { request ->
                        IncomingRequestCard(
                            request = request,
                            isConnected = friendState.isConnected,
                            onAccept = {
                                if (!friendState.isConnected) {
                                    scope.launch { snackbarHostState.showSnackbar("WebSocket offline, please retry later") }
                                } else {
                                    mainVm.send(
                                        gson.toJson(
                                            mapOf(
                                                "type" to "FRIEND_REQUEST_ACCEPT",
                                                "requestId" to request.requestId
                                            )
                                        )
                                    )
                                    scope.launch { snackbarHostState.showSnackbar("Accept request sent") }
                                }
                            },
                            onReject = {
                                if (!friendState.isConnected) {
                                    scope.launch { snackbarHostState.showSnackbar("WebSocket offline, please retry later") }
                                } else {
                                    mainVm.send(
                                        gson.toJson(
                                            mapOf(
                                                "type" to "FRIEND_REQUEST_REJECT",
                                                "requestId" to request.requestId
                                            )
                                        )
                                    )
                                    scope.launch { snackbarHostState.showSnackbar("Reject request sent") }
                                }
                            }
                        )
                    }
                }

                item {
                    SectionTitle(
                        title = "Sent requests",
                        subtitle = "Track invites awaiting responses"
                    )
                }

                if (friendState.outgoingRequests.isEmpty()) {
                    item {
                        EmptyHintCard(text = "You have not sent any requests yet")
                    }
                } else {
                    items(friendState.outgoingRequests, key = { it.requestId }) { request ->
                        OutgoingRequestCard(
                            request = request,
                            isConnected = friendState.isConnected,
                            onResend = {
                                if (!friendState.isConnected) {
                                    scope.launch { snackbarHostState.showSnackbar("WebSocket offline, please retry later") }
                                } else {
                                    friendState = friendState.copy(
                                        outgoingRequests = friendState.outgoingRequests.map {
                                            if (it.requestId == request.requestId) {
                                                it.copy(
                                                    status = FriendRequestStatus.PENDING,
                                                    createdAtMs = System.currentTimeMillis()
                                                )
                                            } else it
                                        }.sortedByDescending { it.createdAtMs }
                                    )
                                    pendingSends.add(
                                        PendingSendContext(
                                            placeholderId = null,
                                            existingRequestId = request.requestId,
                                            receiverId = request.receiverId,
                                            message = request.message
                                        )
                                    )
                                    mainVm.send(
                                        gson.toJson(
                                            mapOf(
                                                "type" to "FRIEND_REQUEST_SEND",
                                                "receiverId" to request.receiverId,
                                                "message" to request.message
                                            )
                                        )
                                    )
                                    scope.launch { snackbarHostState.showSnackbar("Friend request resent") }
                                }
                            }
                        )
                    }
                }

                item {
                    SectionTitle(
                        title = "Friends",
                        subtitle = "All established conversations"
                    )
                }

                if (friendState.friends.isEmpty()) {
                    item {
                        EmptyHintCard(text = "No friends yet – send a request to get started!")
                    }
                } else {
                    items(friendState.friends, key = { it.userId }) { friend ->
                        FriendItemCard(friend)
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendHeaderCard(
    isConnected: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Friend Management",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "Real-time WebSocket sync keeps requests and friends fresh.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                IconButton(onRefresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh friend data",
                        tint = Color(0xFF6366F1)
                    )
                }
            }

            StatusChip(
                text = if (isConnected) "Connected" else "Disconnected",
                background = if (isConnected) Color(0x334CAF50) else Color(0x33EF5350),
                contentColor = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun SendRequestCard(
    receiverInput: String,
    onReceiverChange: (String) -> Unit,
    messageInput: String,
    onMessageChange: (String) -> Unit,
    isConnected: Boolean,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.82f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Send a friend request",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            OutlinedTextField(
                value = receiverInput,
                onValueChange = onReceiverChange,
                label = { Text("Target user UID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )
            OutlinedTextField(
                value = messageInput,
                onValueChange = onMessageChange,
                label = { Text("Greeting message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(18.dp)
            )
            Button(
                onClick = onSendClick,
                enabled = receiverInput.isNotBlank() && isConnected,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text(text = if (isConnected) "Send" else "Connect first", color = Color.White)
            }
            Text(
                text = "Tip: All friend actions use numeric UIDs over WebSocket, so double-check the ID before sending.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequestUiModel,
    isConnected: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UserAvatar(name = request.requesterName, fallbackId = request.requesterId)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = request.requesterName ?: "User ${request.requesterId}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "UID ${request.requesterId}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    formatTimestamp(request.createdAtMs)?.let { time ->
                        Text(
                            text = "Requested on $time",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            if (request.message.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = request.message,
                        fontSize = 13.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                        Text("Reject")
                }
                Button(
                    onClick = onAccept,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                ) {
                        Text("Accept", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OutgoingRequestCard(
    request: FriendRequestUiModel,
    isConnected: Boolean,
    onResend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.88f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Sent to user ${request.receiverId}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )
                    formatTimestamp(request.createdAtMs)?.let { time ->
                        Text(
                            text = "Sent on $time",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Text(
                        text = "UID ${request.receiverId}",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                val (statusText, bg, fg) = when (request.status) {
                    FriendRequestStatus.PENDING -> Triple("Awaiting response", Color(0x336366F1), Color(0xFF4338CA))
                    FriendRequestStatus.REJECTED -> Triple("Rejected", Color(0x33EF4444), Color(0xFFB91C1C))
                    FriendRequestStatus.ACCEPTED -> Triple("Added", Color(0x3334D399), Color(0xFF047857))
                }
                StatusChip(text = statusText, background = bg, contentColor = fg)
            }

            if (request.message.isNotBlank()) {
                Text(
                    text = request.message,
                    fontSize = 13.sp,
                    color = Color(0xFF374151),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (request.status == FriendRequestStatus.REJECTED) {
                Button(
                    onClick = onResend,
                    enabled = isConnected,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Resend", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FriendItemCard(friend: FriendSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserAvatar(name = friend.username, fallbackId = friend.userId)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.username ?: "User ${friend.userId}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "UID ${friend.userId}",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                friend.since?.let { since ->
                    formatTimestamp(since)?.let { time ->
                        Text(
                            text = "Friends since $time",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                friend.convId?.let { id ->
                    Text(
                        text = "Conversation ID: $id",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun EmptyHintCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.7f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, background: Color, contentColor: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun UserAvatar(name: String?, fallbackId: Long, size: Dp = 48.dp) {
    val display = name?.firstOrNull()?.uppercase() ?: fallbackId.toString().takeLast(2)
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF6366F1).copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = display,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4338CA)
        )
    }
}

private fun formatTimestamp(timestamp: Long?): String? {
    if (timestamp == null || timestamp <= 0) return null
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
private fun FriendScreenPreview() {
    FriendScreen(navController = rememberNavController())
}
