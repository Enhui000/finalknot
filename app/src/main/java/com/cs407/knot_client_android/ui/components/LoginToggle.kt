package com.cs407.knot_client_android.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginToggle(
    isLogin: Boolean,
    onToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color(0xFFE8E8E8))
    ) {
        val totalWidth = maxWidth
        val halfWidth = totalWidth / 2
        
        val indicatorOffset by animateDpAsState(
            targetValue = if (isLogin) 0.dp else halfWidth,
            animationSpec = tween(durationMillis = 300),
            label = "indicator offset"
        )
        
        // Sliding indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(halfWidth)
                .height(50.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(Color.White)
        )
        
        // Toggle options
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Login option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        onClick = { onToggleChange(true) },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Login",
                    fontSize = 15.sp,
                    fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLogin) Color(0xFF2C2C2C) else Color(0xFF888888)
                )
            }
            
            // Register option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        onClick = { onToggleChange(false) },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Register",
                    fontSize = 15.sp,
                    fontWeight = if (!isLogin) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isLogin) Color(0xFF2C2C2C) else Color(0xFF888888)
                )
            }
        }
    }
}

