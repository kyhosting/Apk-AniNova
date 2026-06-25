package com.aninova.app.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.aninova.app.data.model.Result
import com.aninova.app.data.model.VideoQuality
import com.aninova.app.ui.components.LoadingIndicator
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*
import com.aninova.app.ui.viewmodel.EpisodeViewModel

@Composable
fun EpisodeScreen(
    navController: NavController,
    slug: String,
    viewModel: EpisodeViewModel = hiltViewModel(),
) {
    val episodeState by viewModel.episodeState.collectAsState()
    val videoState by viewModel.videoState.collectAsState()
    val navState by viewModel.navState.collectAsState()
    val context = LocalContext.current

    var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf(0) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(videoState) {
        val vs = videoState
        if (vs is Result.Success) {
            val sources = vs.data.sources
            val url = if (selectedQuality != null) {
                selectedQuality!!.url
            } else {
                sources?.maxByOrNull { it.quality?.length ?: 0 }?.url
                    ?: vs.data.directUrl
                    ?: vs.data.url
                    ?: sources?.firstOrNull()?.url
            }
            if (!url.isNullOrBlank()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                exoPlayer.play()
            }
        }
    }

    LaunchedEffect(selectedQuality) {
        val vs = videoState
        if (vs is Result.Success && selectedQuality != null) {
            val url = selectedQuality!!.url
            if (url.isNotBlank()) {
                val pos = exoPlayer.currentPosition
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                exoPlayer.seekTo(pos)
                exoPlayer.play()
            }
        }
    }

    Scaffold(containerColor = Background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                ) {
                    when (videoState) {
                        is Result.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                            }
                        }
                        is Result.Error -> {
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.VideocamOff, null, tint = OnSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Video tidak tersedia", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.load() }) {
                                    Text("Coba lagi", color = Primary)
                                }
                            }
                        }
                        null -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                            }
                        }
                        else -> {
                            AndroidView(
                                factory = {
                                    PlayerView(it).apply {
                                        player = exoPlayer
                                        useController = true
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .background(
                                Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))
                            )
                            .padding(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnBackground)
                            }
                            if (episodeState is Result.Success) {
                                val ep = (episodeState as Result.Success).data
                                Text(
                                    ep.animeTitle ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = OnBackground,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    if (videoState is Result.Success) {
                        val sources = (videoState as Result.Success).data.sources
                        if (!sources.isNullOrEmpty()) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                Box {
                                    IconButton(onClick = { showQualityMenu = true }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.HighQuality, null, tint = OnBackground, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showQualityMenu,
                                        onDismissRequest = { showQualityMenu = false },
                                        modifier = Modifier.background(Surface),
                                    ) {
                                        Text(
                                            "Pilih Resolusi",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Primary,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        )
                                        HorizontalDivider(color = Divider)
                                        sources.forEach { quality ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        if (selectedQuality == quality) {
                                                            Icon(Icons.Filled.CheckCircle, null, tint = Primary, modifier = Modifier.size(16.dp))
                                                        } else {
                                                            Spacer(Modifier.size(16.dp))
                                                        }
                                                        Text(quality.quality ?: "Auto", color = OnBackground)
                                                    }
                                                },
                                                onClick = {
                                                    selectedQuality = quality
                                                    showQualityMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (val state = episodeState) {
                        is Result.Loading -> {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceVariant)
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        is Result.Success -> {
                            val episode = state.data

                            Text(
                                episode.animeTitle ?: "",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OnBackground,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.PlayCircleOutline, null, tint = Primary, modifier = Modifier.size(16.dp))
                                Text(
                                    episode.title ?: episode.episode ?: slug,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                )
                                if (videoState is Result.Success) {
                                    val q = selectedQuality?.quality ?: (videoState as Result.Success).data.quality
                                    if (!q.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Primary.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(q, style = MaterialTheme.typography.labelSmall, color = Primary)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            episode.players?.let { players ->
                                if (players.isNotEmpty()) {
                                    Text(
                                        "Server",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = OnBackground,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(players.indices.toList()) { idx ->
                                            val player = players[idx]
                                            val isSelected = selectedServer == idx
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Primary else SurfaceVariant)
                                                    .clickable {
                                                        selectedServer = idx
                                                        selectedQuality = null
                                                        player.slug?.let { viewModel.loadVideoSource(it) }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Filled.PlayArrow, null, tint = if (isSelected) OnPrimary else Primary, modifier = Modifier.size(14.dp))
                                                    Text(
                                                        player.server,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = if (isSelected) OnPrimary else OnBackground,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                            }

                            HorizontalDivider(color = Divider)
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                navState?.previous?.let { prev ->
                                    OutlinedButton(
                                        onClick = {
                                            navController.navigate(Screen.Episode.createRoute(prev.slug)) {
                                                popUpTo(Screen.Episode.route) { inclusive = true }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Divider),
                                    ) {
                                        Icon(Icons.Filled.SkipPrevious, null, tint = OnSurfaceVariant)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Sebelumnya", color = OnSurfaceVariant)
                                    }
                                } ?: Spacer(Modifier.weight(1f))

                                navState?.next?.let { next ->
                                    Button(
                                        onClick = {
                                            navController.navigate(Screen.Episode.createRoute(next.slug)) {
                                                popUpTo(Screen.Episode.route) { inclusive = true }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("Selanjutnya", color = OnPrimary)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Filled.SkipNext, null, tint = OnPrimary)
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
