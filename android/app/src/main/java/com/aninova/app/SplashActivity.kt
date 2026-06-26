package com.aninova.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aninova.app.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AniNovaTheme {
                SplashScreen(
                    onFinished = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }

    val logoScale by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(600),
        label = "logoAlpha",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(500),
        label = "textAlpha",
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (phase >= 3) 1f else 0f,
        animationSpec = tween(500),
        label = "taglineAlpha",
    )
    val barAlpha by animateFloatAsState(
        targetValue = if (phase >= 4) 1f else 0f,
        animationSpec = tween(400),
        label = "barAlpha",
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "progress",
    )

    LaunchedEffect(Unit) {
        delay(100)
        phase = 1
        delay(400)
        phase = 2
        delay(300)
        phase = 3
        delay(200)
        phase = 4
        progress = 0.3f
        delay(400)
        progress = 0.65f
        delay(500)
        progress = 1f
        delay(800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x22E53935), Color.Transparent),
                        radius = 900f,
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryVariant, Primary, Color(0xFFFF5252)),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayCircleFilled,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(54.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "AniNova",
                modifier = Modifier.alpha(textAlpha),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp,
                ),
                color = OnBackground,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Anime & Donghua Platform",
                modifier = Modifier.alpha(taglineAlpha),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                color = Primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(60.dp))

            Column(
                modifier = Modifier
                    .alpha(barAlpha)
                    .width(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Primary,
                    trackColor = Divider,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    when {
                        animatedProgress < 0.4f -> "Memuat konten..."
                        animatedProgress < 0.7f -> "Menyiapkan streaming..."
                        else -> "Siap!"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(taglineAlpha)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "by Kicen Xensai",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
