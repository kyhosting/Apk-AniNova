package com.aninova.app.data.repository

import com.aninova.app.data.api.ApiService
import com.aninova.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepository @Inject constructor(
    private val api: ApiService,
) {
    private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<ApiResponse<T>>): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = block()
                if (response.isSuccessful) {
                    val body = response.body()
                    val isOk = body?.status == "success" || body?.status == "ok"
                    if (isOk && body?.data != null) {
                        Result.Success(body.data)
                    } else {
                        Result.Error(body?.message ?: "Unknown error")
                    }
                } else {
                    Result.Error("HTTP ${response.code()}: ${response.message()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error(e.localizedMessage ?: "Network error")
            }
        }

    suspend fun getHome(page: Int = 1) = safeCall { api.getHome(page) }
    suspend fun getLatest(page: Int = 1) = safeCall { api.getLatest(page) }
    suspend fun getPopular(page: Int = 1) = safeCall { api.getPopular(page) }
    suspend fun getTrending() = safeCall { api.getTrending() }
    suspend fun getOngoing(page: Int = 1) = safeCall { api.getOngoing(page) }
    suspend fun getCompleted(page: Int = 1) = safeCall { api.getCompleted(page) }
    suspend fun getMovies(page: Int = 1) = safeCall { api.getMovies(page) }
    suspend fun search(query: String) = safeCall { api.search(query) }
    suspend fun getGenres() = safeCall { api.getGenres() }
    suspend fun getByGenre(slug: String, page: Int = 1) = safeCall { api.getByGenre(slug, page) }
    suspend fun getAnimeDetail(slug: String) = safeCall { api.getAnimeDetail(slug) }
    suspend fun getEpisode(slug: String) = safeCall { api.getEpisode(slug) }
    suspend fun getEpisodeNavigation(slug: String) = safeCall { api.getEpisodeNavigation(slug) }
    suspend fun getVideoSource(slug: String) = safeCall { api.getVideoSource(slug) }
    suspend fun getLikes(slug: String) = safeCall { api.getLikes(slug) }
    suspend fun likeAnime(slug: String) = safeCall { api.likeAnime(slug) }
    suspend fun unlikeAnime(slug: String) = safeCall { api.unlikeAnime(slug) }
    suspend fun getComments(slug: String) = safeCall { api.getComments(slug) }
    suspend fun postComment(slug: String, content: String) =
        safeCall { api.postComment(slug, CommentRequest(content)) }
    suspend fun incrementViews(slug: String) = safeCall { api.incrementViews(slug) }
    suspend fun getWatchlist() = safeCall { api.getWatchlist() }
    suspend fun addToWatchlist(slug: String, title: String, thumbnail: String) =
        safeCall { api.addToWatchlist(WatchlistRequest(slug, title, thumbnail)) }
    suspend fun removeFromWatchlist(slug: String) = safeCall { api.removeFromWatchlist(slug) }
    suspend fun getHistory() = safeCall { api.getHistory() }
    suspend fun addHistory(slug: String, epSlug: String, title: String, thumbnail: String) =
        safeCall { api.addHistory(HistoryRequest(slug, epSlug, title, thumbnail)) }
}
