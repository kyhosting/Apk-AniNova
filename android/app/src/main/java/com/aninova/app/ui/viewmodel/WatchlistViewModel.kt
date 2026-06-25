package com.aninova.app.ui.viewmodel

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
class WatchlistViewModel @Inject constructor(
    private val repository: AnimeRepository,
) : ViewModel() {

    private val _watchlistState = MutableStateFlow<Result<WatchlistData>>(Result.Loading)
    val watchlistState: StateFlow<Result<WatchlistData>> = _watchlistState

    private val _historyState = MutableStateFlow<Result<HistoryData>?>(null)
    val historyState: StateFlow<Result<HistoryData>?> = _historyState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _watchlistState.value = Result.Loading
            _watchlistState.value = repository.getWatchlist()
            loadHistory()
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = Result.Loading
            _historyState.value = repository.getHistory()
        }
    }

    fun removeFromWatchlist(slug: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(slug)
            load()
        }
    }
}
