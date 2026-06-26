package com.aninova.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*
import com.aninova.app.ui.viewmodel.AuthUiState
import com.aninova.app.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) navController.navigate(Screen.Home.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            val hasPrevious = navController.previousBackStackEntry != null
            if (hasPrevious) {
                navController.popBackStack()
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Primary.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))

            AniNovaLogo()

            Spacer(Modifier.height(10.dp))
            Text(
                "Selamat Datang!",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground,
                ),
            )
            Text(
                "Masuk untuk lanjut nonton anime",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, Divider),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Login",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground,
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email atau Username") },
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = Primary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Primary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null,
                                    tint = OnSurfaceVariant,
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                        val msg = (uiState as? AuthUiState.Error)?.message ?: ""
                        Row(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Error.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Error, null, tint = Error, modifier = Modifier.size(16.dp))
                            Text(msg, color = Error, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.login(email, password) },
                        enabled = uiState !is AuthUiState.Loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Filled.Login, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Masuk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Belum punya akun?", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    viewModel.resetState()
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }) {
                    Text(
                        "Daftar Sekarang",
                        color = Primary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun AniNovaLogo() {
    Box(
        modifier = Modifier
            .size(94.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(PrimaryVariant, Primary, Color(0xFFFF5252)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = OnPrimary,
            modifier = Modifier.size(46.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "AniNova",
        style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.5).sp,
        ),
        color = Primary,
    )
    Text(
        "ANIME & DONGHUA PLATFORM",
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
        color = OnSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = Divider,
    cursorColor = Primary,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
    focusedContainerColor = SurfaceVariant,
    unfocusedContainerColor = SurfaceVariant,
    focusedLabelColor = Primary,
    unfocusedLabelColor = OnSurfaceVariant,
)
