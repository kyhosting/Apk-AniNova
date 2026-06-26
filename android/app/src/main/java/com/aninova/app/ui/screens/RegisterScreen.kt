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
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(1) }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.Success -> {
                if (s.message.contains("OTP")) {
                    otpSent = true
                    step = 2
                    viewModel.resetState()
                } else {
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
            else -> {}
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
                    Brush.verticalGradient(listOf(Primary.copy(alpha = 0.07f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            AniNovaLogo()

            Spacer(Modifier.height(10.dp))
            Text(
                if (step == 1) "Buat Akun Baru" else "Verifikasi Email",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = OnBackground),
            )
            Text(
                if (step == 1) "Daftar dan mulai nonton anime gratis!" else "Cek email kamu untuk kode OTP",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepIndicator(1, step >= 1, "Data Akun")
                Box(modifier = Modifier.width(60.dp).height(2.dp).clip(RoundedCornerShape(2.dp)).background(if (step >= 2) Primary else Divider))
                StepIndicator(2, step >= 2, "Verifikasi")
            }

            Spacer(Modifier.height(20.dp))

            AnimatedContent(targetState = step, label = "step") { currentStep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, Divider),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        if (currentStep == 1) {
                            Text("Informasi Akun", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OnBackground)
                            Spacer(Modifier.height(20.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Filled.Person, null, tint = Primary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                supportingText = { Text("3-30 karakter, huruf/angka/_/-", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall) },
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                leadingIcon = { Icon(Icons.Filled.Email, null, tint = Primary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                supportingText = { Text("Kode OTP akan dikirim ke email ini", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall) },
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Primary) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = OnSurfaceVariant)
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                supportingText = { Text("Minimal 6 karakter", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall) },
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                label = { Text("Konfirmasi Password") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.LockOpen,
                                        null,
                                        tint = if (passwordConfirm.isNotEmpty() && password != passwordConfirm) Error else Primary,
                                    )
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                isError = passwordConfirm.isNotEmpty() && password != passwordConfirm,
                                supportingText = if (passwordConfirm.isNotEmpty() && password != passwordConfirm) {
                                    { Text("Password tidak sama", color = Error, style = MaterialTheme.typography.labelSmall) }
                                } else null,
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
                                    Icon(Icons.Filled.ErrorOutline, null, tint = Error, modifier = Modifier.size(16.dp))
                                    Text(msg, color = Error, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.sendOtp(email, username, password) },
                                enabled = uiState !is AuthUiState.Loading
                                    && username.isNotBlank() && email.isNotBlank()
                                    && password.length >= 6 && password == passwordConfirm,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                if (uiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                } else {
                                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Kirim Kode OTP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        } else {
                            Text("Verifikasi OTP", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OnBackground)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Primary.copy(alpha = 0.08f))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.MailOutline, null, tint = Primary, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Kode dikirim ke:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                    Text(email, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = OnBackground)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            OutlinedTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 6) otp = it },
                                label = { Text("Kode OTP 6 Digit") },
                                leadingIcon = { Icon(Icons.Filled.Pin, null, tint = Primary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                supportingText = { Text("Kode berlaku 10 menit", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall) },
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
                                    Icon(Icons.Filled.ErrorOutline, null, tint = Error, modifier = Modifier.size(16.dp))
                                    Text(msg, color = Error, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.register(email, username, password, otp) },
                                enabled = uiState !is AuthUiState.Loading && otp.length == 6,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                if (uiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                } else {
                                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Verifikasi & Daftar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    step = 1
                                    otpSent = false
                                    otp = ""
                                    viewModel.resetState()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Divider),
                            ) {
                                Icon(Icons.Filled.ArrowBack, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kembali & Edit Data", color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Sudah punya akun?", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    viewModel.resetState()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }) {
                    Text("Login", color = Primary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StepIndicator(number: Int, active: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    if (active)
                        Brush.linearGradient(listOf(PrimaryVariant, Primary))
                    else
                        Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (active && number == 1) {
                Icon(Icons.Filled.Check, null, tint = OnPrimary, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    "$number",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (active) OnPrimary else OnSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) Primary else OnSurfaceVariant)
    }
}
