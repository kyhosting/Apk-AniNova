package com.aninova.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.aninova.app.data.model.AnimeListData
import com.aninova.app.data.model.Result
import com.aninova.app.data.repository.AnimeRepository
import com.aninova.app.ui.components.*
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.Background
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenreViewModel @Inject constructor(
    private val repository: AnimeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val slug: String = savedStateHandle["slug"] ?: ""
    private val _state = MutableStateFlow<Result<AnimeListData>>(Result.Loading)
    val state: StateFlow<Result<AnimeListData>> = _state

    init {
        viewModelScope.launch {
            _state.value = repository.getByGenre(slug)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreScreen(
    navController: NavController,
    slug: String,
    viewModel: GenreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val title = slug.replace("-", " ").capitalize(Locale.current)

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { padding ->
        when (val s = state) {
            is Result.Loading -> LoadingIndicator(Modifier.padding(padding))
            is Result.Error -> ErrorMessage(s.message, Modifier.padding(padding))
            is Result.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.data.results) { anime ->
                        AnimeCardGrid(
                            anime = anime,
                            onClick = { navController.navigate(Screen.AnimeDetail.createRoute(anime.slug)) },
                        )
                    }
                }
            }
        }
    }
}
