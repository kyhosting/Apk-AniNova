package com.aninova.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aninova.app.data.model.Result
import com.aninova.app.ui.components.*
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*
import com.aninova.app.ui.viewmodel.AnimeDetailViewModel

@Composable
fun AnimeDetailScreen(
    navController: NavController,
    slug: String,
    viewModel: AnimeDetailViewModel = hiltViewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()
    val likeState by viewModel.likeState.collectAsState()
    val commentsState by viewModel.commentsState.collectAsState()
    val watchlistAdded by viewModel.watchlistAdded.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var showAllEpisodes by remember { mutableStateOf(false) }

    Scaffold(containerColor = Background) { padding ->
        when (val state = detailState) {
            is Result.Loading -> LoadingIndicator(Modifier.padding(padding))
            is Result.Error -> ErrorMessage(state.message, Modifier.padding(padding))
            is Result.Success -> {
                val anime = state.data
                val episodes = anime.episodes ?: emptyList()
                val displayedEps = if (showAllEpisodes) episodes else episodes.take(12)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                            AsyncImage(
                                model = anime.thumbnail,
                                contentDescription = anime.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0x44000000), Color(0xFF0D0D0D)),
                                        )
                                    )
                            )
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnBackground)
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(anime.title, style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                anime.type?.let { Chip(it) }
                                anime.status?.let { Chip(it) }
                                anime.year?.let { Chip(it) }
                                anime.rating?.let { Chip("⭐ $it") }
                            }
                            Spacer(Modifier.height(8.dp))
                            anime.genres?.let { genres ->
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(genres) { genre ->
                                        Box(
                                            modifier = Modifier
                                                .background(SurfaceVariant, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    navController.navigate(Screen.GenreList.createRoute(genre.lowercase().replace(" ", "-")))
                                                }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(genre, style = MaterialTheme.typography.bodySmall, color = Primary)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        episodes.firstOrNull()?.let {
                                            navController.navigate(Screen.Episode.createRoute(it.slug))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Watch Now")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.toggleWatchlist(anime.title, anime.thumbnail) },
                                    border = ButtonDefaults.outlinedButtonBorder,
                                ) {
                                    Icon(
                                        if (watchlistAdded) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (watchlistAdded) Primary else OnSurfaceVariant,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.toggleLike() },
                                    border = ButtonDefaults.outlinedButtonBorder,
                                ) {
                                    Icon(
                                        if (likeState?.liked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (likeState?.liked == true) Error else OnSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("${likeState?.totalLikes ?: 0}", color = OnSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        anime.synopsis?.let { synopsis ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Text("Synopsis", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(4.dp))
                                Text(synopsis, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Episodes (${episodes.size})", style = MaterialTheme.typography.titleLarge)
                                if (episodes.size > 12) {
                                    TextButton(onClick = { showAllEpisodes = !showAllEpisodes }) {
                                        Text(if (showAllEpisodes) "Show Less" else "Show All", color = Primary)
                                    }
                                }
                            }
                        }
                    }

                    items(displayedEps) { ep ->
                        ListItem(
                            headlineContent = {
                                Text(ep.title ?: ep.episode ?: ep.slug, color = OnSurface)
                            },
                            supportingContent = {
                                Text(ep.episode ?: "", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            },
                            leadingContent = {
                                Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Primary)
                            },
                            modifier = Modifier
                                .clickable { navController.navigate(Screen.Episode.createRoute(ep.slug)) }
                                .padding(horizontal = 8.dp),
                            colors = ListItemDefaults.colors(containerColor = Background),
                        )
                        Divider(color = Divider, thickness = 0.5.dp)
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("Comments", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { Text("Write a comment…", color = OnSurfaceVariant) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Divider,
                                        focusedTextColor = OnBackground,
                                        unfocusedTextColor = OnBackground,
                                        focusedContainerColor = SurfaceVariant,
                                        unfocusedContainerColor = SurfaceVariant,
                                    ),
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            viewModel.postComment(commentText)
                                            commentText = ""
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Primary),
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = OnPrimary)
                                }
                            }
                        }
                    }

                    when (val cs = commentsState) {
                        is Result.Success -> {
                            items(cs.data.results) { comment ->
                                ListItem(
                                    headlineContent = {
                                        Text(comment.username, style = MaterialTheme.typography.titleMedium, color = Primary)
                                    },
                                    supportingContent = {
                                        Text(comment.content, color = OnSurface)
                                    },
                                    trailingContent = {
                                        Text(comment.createdAt.take(10), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    colors = ListItemDefaults.colors(containerColor = Background),
                                )
                                Divider(color = Divider, thickness = 0.5.dp)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String) {
    Box(
        modifier = Modifier
            .background(SurfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
    }
}
