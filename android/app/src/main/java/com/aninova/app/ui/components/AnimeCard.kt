package com.aninova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aninova.app.data.model.AnimeCard
import com.aninova.app.ui.theme.*

@Composable
fun AnimeCardItem(
    anime: AnimeCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(130.dp)
            .height(195.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = anime.thumbnail,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE6000000)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    )
                )
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                anime.eps?.let {
                    Text(
                        text = "Ep $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                    )
                }
            }
        }

        anime.type?.let { type ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = type,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnPrimary,
                )
            }
        }
    }
}

@Composable
fun AnimeCardGrid(
    anime: AnimeCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(CardBackground)
        ) {
            AsyncImage(
                model = anime.thumbnail,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            anime.type?.let { type ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .background(Primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = type, style = MaterialTheme.typography.labelSmall, color = OnPrimary)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = anime.title,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
