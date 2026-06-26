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
import androidx.compose.ui.unit.sp
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
    var synopsisExpanded by remember { mutableStateOf(false) }

    Scaffold(containerColor = Background) { padding ->
        when (val state = detailState) {
            is Result.Loading -> DetailShimmer(Modifier.padding(padding))
            is Result.Error -> ErrorMessage(
                message = state.message,
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load() },
            )
            is Result.Success -> {
                val anime = state.data
                val episodes = anime.episodes ?: emptyList()
                val displayedEps = if (showAllEpisodes) episodes else episodes.take(12)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
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
                                            listOf(Color(0x55000000), Color(0xFF0D0D0D)),
                                            startY = 100f,
                                        )
                                    )
                            )
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color(0x88000000), RoundedCornerShape(50)),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            anime.rating?.let { rating ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFFCA28), modifier = Modifier.size(13.dp))
                                        Text(rating, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                anime.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = OnBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                anime.type?.let { InfoChip(it, chipType = ChipType.TYPE) }
                                anime.status?.let { InfoChip(it, chipType = ChipType.STATUS) }
                                anime.year?.let { InfoChip(it, chipType = ChipType.NEUTRAL) }
                            }
                            Spacer(Modifier.height(8.dp))
                            anime.genres?.let { genres ->
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(genres) { genre ->
                                        Box(
                                            modifier = Modifier
                                                .background(SurfaceVariant, RoundedCornerShape(20.dp))
                                                .clickable {
                                                    navController.navigate(
                                                        Screen.GenreList.createRoute(genre.lowercase().replace(" ", "-"))
                                                    )
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(genre, style = MaterialTheme.typography.labelSmall, color = Primary)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        episodes.firstOrNull()?.let {
                                            navController.navigate(Screen.Episode.createRoute(it.slug))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = episodes.isNotEmpty(),
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tonton", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.toggleWatchlist(anime.title, anime.thumbnail) },
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = ButtonDefaults.outlinedButtonBorder,
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Icon(
                                        if (watchlistAdded) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (watchlistAdded) Primary else OnSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.toggleLike() },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = ButtonDefaults.outlinedButtonBorder,
                                ) {
                                    Icon(
                                        if (likeState?.liked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (likeState?.liked == true) Error else OnSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("${likeState?.totalLikes ?: 0}", color = OnSurfaceVariant)
                                }
                            }
                        }
                    }

                    anime.synopsis?.let { synopsis ->
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                SectionTitle("Sinopsis")
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    synopsis,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = OnSurface,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (synopsis.length > 200) {
                                    TextButton(
                                        onClick = { synopsisExpanded = !synopsisExpanded },
                                        contentPadding = PaddingValues(0.dp),
                                    ) {
                                        Text(
                                            if (synopsisExpanded) "Tampilkan lebih sedikit" else "Lihat selengkapnya",
                                            color = Primary,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
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
                                SectionTitle("Episode (${episodes.size})")
                                if (episodes.size > 12) {
                                    TextButton(onClick = { showAllEpisodes = !showAllEpisodes }) {
                                        Text(
                                            if (showAllEpisodes) "Tampilkan sebagian" else "Semua episode",
                                            color = Primary,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (episodes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Belum ada episode tersedia", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    items(displayedEps) { ep ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    ep.title ?: ep.episode ?: ep.slug,
                                    color = OnSurface,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                ep.episode?.let {
                                    Text(it, color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                }
                            },
                            trailingContent = {
                                Icon(Icons.Filled.ChevronRight, null, tint = Divider, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .clickable { navController.navigate(Screen.Episode.createRoute(ep.slug)) }
                                .padding(horizontal = 8.dp),
                            colors = ListItemDefaults.colors(containerColor = Background),
                        )
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            SectionTitle("Komentar")
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { Text("Tulis komentar…", color = OnSurfaceVariant) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Divider,
                                        focusedTextColor = OnBackground,
                                        unfocusedTextColor = OnBackground,
                                        focusedContainerColor = SurfaceVariant,
                                        unfocusedContainerColor = SurfaceVariant,
                                    ),
                                )
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            viewModel.postComment(commentText)
                                            commentText = ""
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Primary),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = OnPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    when (val cs = commentsState) {
                        is Result.Success -> {
                            if (cs.data.results.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("Belum ada komentar. Jadi yang pertama!", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            items(cs.data.results) { comment ->
                                ListItem(
                                    headlineContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(comment.username, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Primary)
                                            Text(comment.createdAt.take(10), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                        }
                                    },
                                    supportingContent = {
                                        Text(comment.content, color = OnSurface, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                                    },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                comment.username.first().uppercaseChar().toString(),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Primary,
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    colors = ListItemDefaults.colors(containerColor = Background),
                                )
                                HorizontalDivider(color = Divider, thickness = 0.5.dp)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

private enum class ChipType { TYPE, STATUS, NEUTRAL }

@Composable
private fun InfoChip(label: String, chipType: ChipType = ChipType.NEUTRAL) {
    val (bgColor, textColor) = when (chipType) {
        ChipType.TYPE -> Pair(Primary.copy(alpha = 0.15f), Primary)
        ChipType.STATUS -> Pair(Success.copy(alpha = 0.15f), Success)
        ChipType.NEUTRAL -> Pair(SurfaceVariant, OnSurfaceVariant)
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = textColor)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = OnBackground,
    )
}
