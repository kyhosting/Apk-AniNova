package com.aninova.app.data.model

import com.google.gson.annotations.SerializedName

data class AnimeCard(
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String?,
    @SerializedName("headline") val headline: String?,
    @SerializedName("eps") val eps: Int?,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("slug") val slug: String,
)

data class HomeSection(
    @SerializedName("section") val section: String,
    @SerializedName("cards") val cards: List<AnimeCard>,
)

data class HomeData(
    @SerializedName("results") val results: List<HomeSection>,
    @SerializedName("page") val page: Int,
    @SerializedName("total") val total: Int,
)

data class AnimeListData(
    @SerializedName("results") val results: List<AnimeCard>,
    @SerializedName("page") val page: Int,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("total") val total: Int?,
)

data class AnimeDetail(
    @SerializedName("title") val title: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("synopsis") val synopsis: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("episodes") val episodes: List<EpisodeItem>?,
    @SerializedName("total_episodes") val totalEpisodes: Int?,
    @SerializedName("year") val year: String?,
    @SerializedName("studio") val studio: String?,
)

data class EpisodeItem(
    @SerializedName("title") val title: String?,
    @SerializedName("slug") val slug: String,
    @SerializedName("episode") val episode: String?,
    @SerializedName("url") val url: String?,
)

data class Genre(
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
)

data class GenreListData(
    @SerializedName("results") val results: List<Genre>,
)

data class LikeData(
    @SerializedName("liked") val liked: Boolean,
    @SerializedName("total_likes") val totalLikes: Int,
)

data class CommentData(
    @SerializedName("results") val results: List<Comment>,
    @SerializedName("total") val total: Int,
)

data class Comment(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String,
)

data class WatchlistData(
    @SerializedName("watchlist") val watchlist: List<WatchlistItem>?,
    @SerializedName("results") val resultsList: List<WatchlistItem>?,
    @SerializedName("total") val total: Int?,
) {
    val results: List<WatchlistItem> get() = watchlist ?: resultsList ?: emptyList()
}

data class WatchlistItem(
    @SerializedName("slug") val slug: String?,
    @SerializedName("anime_slug") val animeSlug: String?,
    @SerializedName("title") val titleField: String?,
    @SerializedName("anime_title") val animeTitle: String?,
    @SerializedName("thumbnail") val thumbnailField: String?,
    @SerializedName("anime_thumbnail") val animeThumbnail: String?,
    @SerializedName("added_at") val addedAt: String?,
) {
    val displaySlug: String get() = animeSlug ?: slug ?: ""
    val title: String get() = animeTitle ?: titleField ?: ""
    val thumbnail: String get() = animeThumbnail ?: thumbnailField ?: ""
}

data class HistoryData(
    @SerializedName("results") val results: List<HistoryItem>,
    @SerializedName("total") val total: Int,
)

data class HistoryItem(
    @SerializedName("slug") val slug: String,
    @SerializedName("episode_slug") val episodeSlug: String?,
    @SerializedName("title") val title: String,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("watched_at") val watchedAt: String?,
)
