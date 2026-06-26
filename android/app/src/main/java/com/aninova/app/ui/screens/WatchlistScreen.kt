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
            TopAppBar(
                title = {
                    Text(
                        "Daftar Saya",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Background,
                contentColor = Primary,
                divider = { HorizontalDivider(color = Divider) },
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Primary,
                        )
                    }
                },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = Primary,
                    unselectedContentColor = OnSurfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.Bookmark, null, modifier = Modifier.size(16.dp))
                        Text(
                            "Watchlist",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            ),
                        )
                    }
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = Primary,
                    unselectedContentColor = OnSurfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.History, null, modifier = Modifier.size(16.dp))
                        Text(
                            "Riwayat",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> when (val state = watchlistState) {
                    is Result.Loading -> LoadingIndicator()
                    is Result.Error -> ErrorMessage(state.message, onRetry = { viewModel.load() })
                    is Result.Success -> {
                        if (state.data.results.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.BookmarkBorder,
                                title = "Watchlist kosong",
                                subtitle = "Tambah anime favorit ke watchlist!",
                                actionLabel = "Jelajahi Anime",
                                onAction = { navController.navigate(Screen.Home.route) },
                            )
                        } else {
                            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(state.data.results, key = { it.displaySlug }) { item ->
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                item.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = OnBackground,
                                                maxLines = 2,
                                            )
                                        },
                                        supportingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(top = 4.dp),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Primary.copy(alpha = 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Icon(Icons.Filled.PlayCircleOutline, null, tint = Primary, modifier = Modifier.size(10.dp))
                                                        Text("Lanjutkan Nonton", style = MaterialTheme.typography.labelSmall, color = Primary)
                                                    }
                                                }
                                            }
                                        },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp, 82.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CardBackground),
                                            ) {
                                                AsyncImage(
                                                    model = item.thumbnail,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { viewModel.removeFromWatchlist(item.displaySlug) }) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    null,
                                                    tint = Error.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .clickable {
                                                navController.navigate(Screen.AnimeDetail.createRoute(item.displaySlug))
                                            }
                                            .padding(horizontal = 4.dp),
                                        colors = ListItemDefaults.colors(containerColor = Background),
                                    )
                                    HorizontalDivider(color = Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 84.dp))
                                }
                            }
                        }
                    }
                }
                1 -> {
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = "Riwayat tonton",
                        subtitle = "Fitur riwayat akan segera hadir!",
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = OnSurfaceVariant, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = OnBackground)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Explore, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}
