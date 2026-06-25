package com.aninova.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aninova.app.data.model.*
import com.aninova.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AnimeRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<Result<AnimeListData>?>(null)
    val results: StateFlow<Result<AnimeListData>?> = _results

    init {
        viewModelScope.launch {
            _query
                .debounce(500)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { q -> search(q) }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        if (value.isBlank()) _results.value = null
    }

    fun search(q: String) {
        viewModelScope.launch {
            _results.value = Result.Loading
            _results.value = repository.search(q)
        }
    }
}
