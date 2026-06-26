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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.aninova.app.data.model.Result
import com.aninova.app.data.model.VideoQuality
import com.aninova.app.ui.components.LoadingIndicator
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*
import com.aninova.app.ui.viewmodel.AuthViewModel
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableIntStateOf(0) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isPlayerReady by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = "Gagal memutar video. Coba server/kualitas lain."
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isPlayerReady = true
                        playerError = null
                    }
                }
            })
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer.playbackState == Player.STATE_READY) exoPlayer.play()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.stop()
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoState, isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect
        val vs = videoState
        if (vs is Result.Success) {
            playerError = null
            isPlayerReady = false
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
                try {
                    exoPlayer.setMediaItem(MediaItem.fromUri(url))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } catch (e: Exception) {
                    playerError = "Gagal memuat video."
                }
            } else {
                playerError = "URL video tidak tersedia. Coba server lain."
            }
        }
    }

    LaunchedEffect(selectedQuality) {
        val vs = videoState
        if (vs is Result.Success && selectedQuality != null) {
            val url = selectedQuality!!.url
            if (url.isNotBlank()) {
                try {
                    val pos = exoPlayer.currentPosition
                    exoPlayer.setMediaItem(MediaItem.fromUri(url))
                    exoPlayer.prepare()
                    exoPlayer.seekTo(pos)
                    exoPlayer.play()
                } catch (e: Exception) {
                    playerError = "Gagal memuat kualitas ini."
                }
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
                        .background(Color(0xFF080808)),
                ) {
                    if (!isLoggedIn) {
                        LoginPromptOverlay(
                            onLogin = { navController.navigate(Screen.Login.route) },
                            onRegister = { navController.navigate(Screen.Register.route) },
                        )
                    } else {
                        when {
                            playerError != null -> {
                                PlayerErrorState(
                                    message = playerError!!,
                                    onRetry = {
                                        playerError = null
                                        viewModel.load()
                                    },
                                )
                            }
                            videoState is Result.Loading || videoState == null -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                                        Spacer(Modifier.height(12.dp))
                                        Text("Memuat video...", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                                    }
                                }
                            }
                            videoState is Result.Error -> {
                                PlayerErrorState(
                                    message = (videoState as Result.Error).message,
                                    onRetry = { viewModel.load() },
                                )
                            }
                            else -> {
                                androidx.compose.ui.viewinterop.AndroidView(
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
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                            .padding(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                exoPlayer.stop()
                                navController.popBackStack()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                            }
                            if (episodeState is Result.Success) {
                                val ep = (episodeState as Result.Success).data
                                Text(
                                    ep.animeTitle ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    if (videoState is Result.Success && isLoggedIn) {
                        val sources = (videoState as Result.Success).data.sources
                        if (!sources.isNullOrEmpty()) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                IconButton(onClick = { showQualityMenu = true }) {
                                    Icon(Icons.Filled.HighQuality, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false },
                                    modifier = Modifier.background(Surface),
                                ) {
                                    Text(
                                        "Pilih Kualitas",
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

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (val state = episodeState) {
                        is Result.Loading -> {
                            repeat(2) {
                                Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceVariant))
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
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.PlayCircle, null, tint = Primary, modifier = Modifier.size(13.dp))
                                        Text(
                                            episode.title ?: episode.episode ?: slug,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Primary,
                                        )
                                    }
                                }
                                if (videoState is Result.Success) {
                                    val q = selectedQuality?.quality ?: (videoState as Result.Success).data.quality
                                    if (!q.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SurfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(q, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
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
                                        modifier = Modifier.padding(bottom = 10.dp),
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(players.indices.toList()) { idx ->
                                            val player = players[idx]
                                            val isSelected = selectedServer == idx
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) Primary else SurfaceVariant)
                                                    .clickable {
                                                        selectedServer = idx
                                                        selectedQuality = null
                                                        playerError = null
                                                        player.slug?.let { viewModel.loadVideoSource(it) }
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(
                                                        if (isSelected) Icons.Filled.PlayArrow else Icons.Filled.PlayCircle,
                                                        null,
                                                        tint = if (isSelected) OnPrimary else Primary,
                                                        modifier = Modifier.size(14.dp),
                                                    )
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
                                            exoPlayer.stop()
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
                                            exoPlayer.stop()
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

@Composable
private fun LoginPromptOverlay(onLogin: () -> Unit, onRegister: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, null, tint = Primary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Login untuk menonton",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Masuk atau daftar untuk menikmati anime & donghua",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.Login, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Masuk")
                }
                OutlinedButton(
                    onClick = onRegister,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Primary),
                ) {
                    Text("Daftar", color = Primary)
                }
            }
        }
    }
}

@Composable
private fun PlayerErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Filled.VideocamOff, null, tint = OnSurfaceVariant, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Coba Lagi")
            }
        }
    }
}
