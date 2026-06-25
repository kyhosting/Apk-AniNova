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
class AnimeDetailViewModel @Inject constructor(
    private val repository: AnimeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val slug: String = savedStateHandle["slug"] ?: ""

    private val _detailState = MutableStateFlow<Result<AnimeDetail>>(Result.Loading)
    val detailState: StateFlow<Result<AnimeDetail>> = _detailState

    private val _likeState = MutableStateFlow<LikeData?>(null)
    val likeState: StateFlow<LikeData?> = _likeState

    private val _commentsState = MutableStateFlow<Result<CommentData>?>(null)
    val commentsState: StateFlow<Result<CommentData>?> = _commentsState

    private val _watchlistAdded = MutableStateFlow(false)
    val watchlistAdded: StateFlow<Boolean> = _watchlistAdded

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _detailState.value = Result.Loading
            _detailState.value = repository.getAnimeDetail(slug)
            loadLikes()
            loadComments()
            repository.incrementViews(slug)
        }
    }

    private fun loadLikes() {
        viewModelScope.launch {
            val result = repository.getLikes(slug)
            if (result is Result.Success) _likeState.value = result.data
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            val liked = _likeState.value?.liked ?: false
            val result = if (liked) repository.unlikeAnime(slug) else repository.likeAnime(slug)
            if (result is Result.Success) _likeState.value = result.data
        }
    }

    fun loadComments() {
        viewModelScope.launch {
            _commentsState.value = Result.Loading
            _commentsState.value = repository.getComments(slug)
        }
    }

    fun postComment(content: String) {
        viewModelScope.launch {
            repository.postComment(slug, content)
            loadComments()
        }
    }

    fun toggleWatchlist(title: String, thumbnail: String) {
        viewModelScope.launch {
            if (_watchlistAdded.value) {
                repository.removeFromWatchlist(slug)
                _watchlistAdded.value = false
            } else {
                repository.addToWatchlist(slug, title, thumbnail)
                _watchlistAdded.value = true
            }
        }
    }
}
