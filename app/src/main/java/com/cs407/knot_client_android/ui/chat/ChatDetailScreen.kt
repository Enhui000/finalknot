package com.cs407.knot_client_android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cs407.knot_client_android.navigation.Screen
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavHostController,
    state: ChatDetailUiState,
    onDraftChange: (String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8F6F4), Color(0xFFF3F0FA))
                )
            )
    ) {
        TopAppBar(
            title = {
                Text(text = state.title, fontSize = 20.sp)
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navController.navigate(Screen.Main.createRoute("CHAT")) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Color(0x11000000))

        val listState = rememberLazyListState()

        LaunchedEffect(state.messages.size) {
            if (state.messages.isNotEmpty()) {
                listState.animateScrollToItem(state.messages.lastIndex)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                    //reverseLayout = true   // 最近的在底部
                ) {
                    //items(state.messages.asReversed(), key = { it.clientMsgId!! }) { msg ->
                    items(
                        items = state.messages,
                        key = { it.clientMsgId ?: "server-${it.msgId}" }
                    ) { msg ->
                        MessageBubble(msg)
                        //Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        // 底部输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = state.draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Type a message") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                onClick = {
                    onSendMessage(state.draft)
//                    val t = input.trim()
//                    if (t.isNotEmpty()) {
//                        onSendMessage(t)
//                        input = ""
//                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color(0xFF4A6CF7)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: MessageUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (msg.isMine) 16.dp else 2.dp,
                            topEnd = if (msg.isMine) 2.dp else 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .background(
                        if (msg.isMine) Color(0xFF4A6CF7) else Color.White
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg.contentText,
                    color = if (msg.isMine) Color.White else Color.Black,
                    fontSize = 14.sp
                )
            }

            Text(
                text = msg.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}
