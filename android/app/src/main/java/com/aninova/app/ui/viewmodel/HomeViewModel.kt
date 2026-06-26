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
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository,
) : ViewModel() {

    private val _homeState = MutableStateFlow<Result<HomeData>>(Result.Loading)
    val homeState: StateFlow<Result<HomeData>> = _homeState

    private val _latestState = MutableStateFlow<Result<AnimeListData>>(Result.Loading)
    val latestState: StateFlow<Result<AnimeListData>> = _latestState

    init {
        loadHome()
        loadLatest()
    }

    fun load() = loadHome()

    fun loadHome(page: Int = 1) {
        viewModelScope.launch {
            _homeState.value = Result.Loading
            _homeState.value = repository.getHome(page)
        }
    }

    fun loadLatest(page: Int = 1) {
        viewModelScope.launch {
            _latestState.value = Result.Loading
            _latestState.value = repository.getLatest(page)
        }
    }
}
