package com.cs407.knot_client_android.ui.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun LoginScreen(
    navController: NavHostController
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) } // true = Login, false = Register
    
    // 动画状态
    var animationStarted by remember { mutableStateOf(false) }
    
    // 每个元素的动画 - 递增延迟
    val animationDuration = 600
    val animationOffset = 400f
    
    // 1. Knot 标题
    val titleOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "title offset"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 0, easing = LinearEasing),
        label = "title alpha"
    )
    
    // 2. Slogan
    val sloganOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "slogan offset"
    )
    val sloganAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 100, easing = LinearEasing),
        label = "slogan alpha"
    )
    
    // 3. Username 标签
    val usernameLabelOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "username label offset"
    )
    val usernameLabelAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 200, easing = LinearEasing),
        label = "username label alpha"
    )
    
    // 4. Username 输入框
    val usernameFieldOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "username field offset"
    )
    val usernameFieldAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 300, easing = LinearEasing),
        label = "username field alpha"
    )
    
    // 5. Password 标签
    val passwordLabelOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "password label offset"
    )
    val passwordLabelAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 400, easing = LinearEasing),
        label = "password label alpha"
    )
    
    // 6. Password 输入框
    val passwordFieldOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "password field offset"
    )
    val passwordFieldAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 500, easing = LinearEasing),
        label = "password field alpha"
    )
    
    // 7. Toggle
    val toggleOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 600, easing = FastOutSlowInEasing),
        label = "toggle offset"
    )
    val toggleAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 600, easing = LinearEasing),
        label = "toggle alpha"
    )
    
    // 8. Button
    val buttonOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else animationOffset,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "button offset"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, delayMillis = 700, easing = LinearEasing),
        label = "button alpha"
    )
    
    // 启动动画
    LaunchedEffect(Unit) {
        animationStarted = true
    }
    
    // 背景色 #F8F6F4 - 更温暖的米白色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F4)),
        contentAlignment = BiasAlignment(0f, -0.15f) // 稍微往上偏移，视觉中心
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            // 1. Knot 标题 - 带动画
            Text(
                text = "Knot",
                fontSize = 62.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2C2C2C),
                letterSpacing = (-1.2).sp,
                modifier = Modifier
                    .offset { IntOffset(0, titleOffsetY.roundToInt()) }
                    .alpha(titleAlpha)
            )
            
            // 2. Slogan - 带动画
            Text(
                text = "Where friendships leave their marks.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF7A7A7A),
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .offset { IntOffset(0, sloganOffsetY.roundToInt()) }
                    .alpha(sloganAlpha),
                letterSpacing = 0.4.sp
            )
            
            Spacer(modifier = Modifier.height(60.dp))
            
            // 3. Username 标签 - 带动画
            Text(
                text = "Username",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4A4A4A),
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .offset { IntOffset(0, usernameLabelOffsetY.roundToInt()) }
                    .alpha(usernameLabelAlpha),
                letterSpacing = 0.5.sp
            )
            
            // 4. Username 输入框 - 带动画
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, usernameFieldOffsetY.roundToInt()) }
                    .alpha(usernameFieldAlpha),
                placeholder = { 
                    Text(
                        "Enter your username", 
                        color = Color(0xFFAAAAAA),
                        fontSize = 15.sp
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Username",
                        tint = Color(0xFF9B8FD9).copy(alpha = 0.7f) // 图标调淡
                    )
                },
                shape = RoundedCornerShape(18.dp), // 更大圆角
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFFB5A8FF), // 淡紫色
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 5. Password 标签 - 带动画
            Text(
                text = "Password",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4A4A4A),
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .offset { IntOffset(0, passwordLabelOffsetY.roundToInt()) }
                    .alpha(passwordLabelAlpha),
                letterSpacing = 0.5.sp
            )
            
            // 6. Password 输入框 - 带动画
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, passwordFieldOffsetY.roundToInt()) }
                    .alpha(passwordFieldAlpha),
                placeholder = { 
                    Text(
                        "Enter your password", 
                        color = Color(0xFFAAAAAA),
                        fontSize = 15.sp
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = Color(0xFF9B8FD9).copy(alpha = 0.7f) // 图标调淡
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(18.dp), // 更大圆角
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFFB5A8FF), // 淡紫色
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(52.dp))
            
            // 7. Toggle Switch - 带动画
            CustomToggle(
                isLogin = isLogin,
                onToggleChange = { isLogin = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, toggleOffsetY.roundToInt()) }
                    .alpha(toggleAlpha)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 8. Action Button - 带动画
            Button(
                onClick = { 
                    if (isLogin) {
                        // TODO: Login logic
                    } else {
                        // TODO: Register logic
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .offset { IntOffset(0, buttonOffsetY.roundToInt()) }
                    .alpha(buttonAlpha),
                shape = RoundedCornerShape(18.dp), // 更大圆角
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp) // 移除默认内边距，保持与 toggle 一致
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFB5A8FF),
                                    Color(0xFF9B8FD9)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLogin) "Login" else "Register",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CustomToggle(
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(navController = rememberNavController())
}