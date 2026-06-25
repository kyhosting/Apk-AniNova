package com.aninova.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aninova.app.data.model.Result
import com.aninova.app.ui.components.*
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*
import com.aninova.app.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        containerColor = Background,
        bottomBar = { BottomNavBar(navController) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Cari anime, donghua…", color = OnSurfaceVariant) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Primary) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { viewModel.onQueryChange("") }) { Icon(Icons.Filled.Clear, null, tint = OnSurfaceVariant) } }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Divider,
                    cursorColor = Primary,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant,
                ),
            )
            Spacer(Modifier.height(16.dp))

            when (val state = results) {
                null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Search, null, tint = Divider, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Ketik minimal 2 karakter untuk mencari",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                            )
                        }
                    }
                }
                is Result.Loading -> LoadingIndicator()
                is Result.Error -> ErrorMessage(state.message)
                is Result.Success -> {
                    val animeList = state.data.results
                    if (animeList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.SearchOff, null, tint = Divider, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Tidak ada hasil untuk \"$query\"", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                            }
                        }
                    } else {
                        Text(
                            "${animeList.size} hasil ditemukan",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(animeList) { anime ->
                                AnimeCardGrid(
                                    anime = anime,
                                    onClick = {
                                        navController.navigate(Screen.AnimeDetail.createRoute(anime.slug))
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
