package com.aninova.app.data.model

import com.google.gson.annotations.SerializedName

data class EpisodeDetail(
    @SerializedName("title") val title: String?,
    @SerializedName("slug") val slug: String,
    @SerializedName("anime_slug") val animeSlug: String?,
    @SerializedName("anime_title") val animeTitle: String?,
    @SerializedName("episode") val episode: String?,
    @SerializedName("players") val players: List<Player>?,
    @SerializedName("thumbnail") val thumbnail: String?,
)

data class Player(
    @SerializedName("server") val server: String,
    @SerializedName("url") val url: String?,
    @SerializedName("slug") val slug: String?,
)

data class VideoSource(
    @SerializedName("server") val server: String,
    @SerializedName("url") val url: String?,
    @SerializedName("direct_url") val directUrl: String?,
    @SerializedName("quality") val quality: String?,
    @SerializedName("sources") val sources: List<VideoQuality>?,
)

data class VideoQuality(
    @SerializedName("url") val url: String,
    @SerializedName("quality") val quality: String?,
)

data class EpisodeNavigation(
    @SerializedName("previous") val previous: EpisodeNav?,
    @SerializedName("next") val next: EpisodeNav?,
)

data class EpisodeNav(
    @SerializedName("slug") val slug: String,
    @SerializedName("title") val title: String?,
    @SerializedName("episode") val episode: String?,
)
