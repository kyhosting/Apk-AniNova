package com.aninova.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aninova.app.data.model.*
import com.aninova.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpisodeViewModel @Inject constructor(
    private val repository: AnimeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val slug: String = savedStateHandle["slug"] ?: ""

    private val _episodeState = MutableStateFlow<Result<EpisodeDetail>>(Result.Loading)
    val episodeState: StateFlow<Result<EpisodeDetail>> = _episodeState

    private val _videoState = MutableStateFlow<Result<VideoSource>?>(null)
    val videoState: StateFlow<Result<VideoSource>?> = _videoState

    private val _navState = MutableStateFlow<EpisodeNavigation?>(null)
    val navState: StateFlow<EpisodeNavigation?> = _navState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _episodeState.value = Result.Loading
            val result = repository.getEpisode(slug)
            _episodeState.value = result
            if (result is Result.Success) {
                val firstPlayer = result.data.players?.firstOrNull()
                firstPlayer?.slug?.let { loadVideoSource(it) }
            }
            loadNavigation()
        }
    }

    fun loadVideoSource(playerSlug: String) {
        viewModelScope.launch {
            _videoState.value = Result.Loading
            _videoState.value = repository.getVideoSource(playerSlug)
        }
    }

    private fun loadNavigation() {
        viewModelScope.launch {
            val result = repository.getEpisodeNavigation(slug)
            if (result is Result.Success) _navState.value = result.data
        }
    }
}
