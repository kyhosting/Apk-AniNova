package com.aninova.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aninova.app.data.model.Result
import com.aninova.app.ui.components.*
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*
import com.aninova.app.ui.viewmodel.WatchlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    navController: NavController,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val watchlistState by viewModel.watchlistState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Background,
        bottomBar = { BottomNavBar(navController) },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        "Daftar Saya",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground,
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary,
                divider = { HorizontalDivider(color = Divider) },
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, selectedContentColor = Primary, unselectedContentColor = OnSurfaceVariant) {
                    Text("Watchlist", modifier = Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, selectedContentColor = Primary, unselectedContentColor = OnSurfaceVariant) {
                    Text("Riwayat", modifier = Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal))
                }
            }

            when (selectedTab) {
                0 -> when (val state = watchlistState) {
                    is Result.Loading -> LoadingIndicator()
                    is Result.Error -> ErrorMessage(state.message)
                    is Result.Success -> {
                        if (state.data.results.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.BookmarkBorder, null, tint = OnSurfaceVariant, modifier = Modifier.size(64.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Watchlist kamu kosong", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = OnBackground)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Tambah anime favorit ke watchlist!", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = { navController.navigate(Screen.Home.route) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(Icons.Filled.Explore, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Jelajahi Anime")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(state.data.results, key = { it.displaySlug }) { item ->
                                    ListItem(
                                        headlineContent = {
                                            Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = OnBackground, maxLines = 2)
                                        },
                                        supportingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Filled.PlayCircleOutline, null, tint = Primary, modifier = Modifier.size(12.dp))
                                                Text("Lanjutkan Nonton", style = MaterialTheme.typography.labelSmall, color = Primary)
                                            }
                                        },
                                        leadingContent = {
                                            AsyncImage(
                                                model = item.thumbnail,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(60.dp, 80.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CardBackground),
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { viewModel.removeFromWatchlist(item.displaySlug) }) {
                                                Icon(Icons.Filled.Delete, null, tint = Error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            navController.navigate(Screen.AnimeDetail.createRoute(item.displaySlug))
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Background),
                                    )
                                    HorizontalDivider(color = Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 88.dp))
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.History, null, tint = OnSurfaceVariant, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Riwayat tonton", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = OnBackground)
                            Spacer(Modifier.height(6.dp))
                            Text("Fitur akan segera hadir", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
