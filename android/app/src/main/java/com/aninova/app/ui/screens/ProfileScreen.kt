package com.aninova.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aninova.app.ui.components.BottomNavBar
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*
import com.aninova.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    Scaffold(
        containerColor = Background,
        bottomBar = { BottomNavBar(navController) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profil",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { padding ->
        if (isLoggedIn && currentUser != null) {
            val user = currentUser!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Primary.copy(alpha = 0.08f), Background),
                                )
                            )
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(PrimaryVariant, Primary, Color(0xFFFF5252)))
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    user.username.first().uppercaseChar().toString(),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OnPrimary,
                                    ),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                user.username,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = OnBackground,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            if (user.role == "admin") {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.linearGradient(listOf(PrimaryVariant, Primary)),
                                            RoundedCornerShape(20.dp),
                                        )
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.Star, null, tint = OnPrimary, modifier = Modifier.size(12.dp))
                                        Text("Admin", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = OnPrimary)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(SurfaceVariant, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.Person, null, tint = OnSurfaceVariant, modifier = Modifier.size(12.dp))
                                        Text("Member", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ProfileSectionLabel("Akun Saya")
                        ProfileMenuCard {
                            ProfileMenuItem(Icons.Filled.Bookmark, "Watchlist Saya") {
                                navController.navigate(Screen.Watchlist.route)
                            }
                            MenuDivider()
                            ProfileMenuItem(Icons.Filled.History, "Riwayat Tonton") {
                                navController.navigate(Screen.Watchlist.route)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        ProfileSectionLabel("Informasi")
                        ProfileMenuCard {
                            ProfileMenuItem(Icons.Filled.Info, "Tentang AniNova") {}
                            MenuDivider()
                            ProfileMenuItem(Icons.Filled.Shield, "Kebijakan Privasi") {}
                        }

                        Spacer(Modifier.height(16.dp))
                        ProfileSectionLabel("Akun")
                        ProfileMenuCard {
                            ProfileMenuItem(Icons.Filled.Logout, "Keluar", tint = Error) {
                                viewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))
                        Text(
                            "AniNova v1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Text(
                            "by Kicen Xensai",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PersonOff, null, modifier = Modifier.size(50.dp), tint = OnSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Belum Login",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = OnBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Login untuk akses profil, watchlist, dan riwayat tontonan kamu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { navController.navigate(Screen.Login.route) },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Masuk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Register.route) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary),
                ) {
                    Icon(Icons.Filled.PersonAdd, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buat Akun", color = Primary, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
        color = OnSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = Divider,
        modifier = Modifier.padding(start = 60.dp),
    )
}

@Composable
private fun ProfileMenuCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
    ) {
        content()
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = OnSurface,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = tint)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            }
        },
        trailingContent = {
            Icon(Icons.Filled.ChevronRight, null, tint = Divider, modifier = Modifier.size(18.dp))
        },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
