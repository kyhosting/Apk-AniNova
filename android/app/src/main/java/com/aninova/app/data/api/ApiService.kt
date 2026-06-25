package com.aninova.app.data.api

import com.aninova.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Home & Lists ─────────────────────────────────────────────────────────
    @GET("v1/home")
    suspend fun getHome(@Query("page") page: Int = 1): Response<ApiResponse<HomeData>>

    @GET("v1/latest")
    suspend fun getLatest(@Query("page") page: Int = 1): Response<ApiResponse<AnimeListData>>

    @GET("v1/popular")
    suspend fun getPopular(@Query("page") page: Int = 1): Response<ApiResponse<AnimeListData>>

    @GET("v1/trending")
    suspend fun getTrending(): Response<ApiResponse<AnimeListData>>

    @GET("v1/ongoing")
    suspend fun getOngoing(@Query("page") page: Int = 1): Response<ApiResponse<AnimeListData>>

    @GET("v1/completed")
    suspend fun getCompleted(@Query("page") page: Int = 1): Response<ApiResponse<AnimeListData>>

    @GET("v1/movie")
    suspend fun getMovies(@Query("page") page: Int = 1): Response<ApiResponse<AnimeListData>>

    // ── Search & Genre ────────────────────────────────────────────────────────
    @GET("v1/search/{query}")
    suspend fun search(@Path("query") query: String): Response<ApiResponse<AnimeListData>>

    @GET("v1/genres")
    suspend fun getGenres(): Response<ApiResponse<GenreListData>>

    @GET("v1/genre/{slug}")
    suspend fun getByGenre(
        @Path("slug") slug: String,
        @Query("page") page: Int = 1,
    ): Response<ApiResponse<AnimeListData>>

    // ── Anime Detail ──────────────────────────────────────────────────────────
    @GET("v1/anime/{slug}")
    suspend fun getAnimeDetail(@Path("slug") slug: String): Response<ApiResponse<AnimeDetail>>

    // ── Episodes & Video ──────────────────────────────────────────────────────
    @GET("v1/episode/{slug}")
    suspend fun getEpisode(@Path("slug") slug: String): Response<ApiResponse<EpisodeDetail>>

    @GET("v1/episode/{slug}/navigation")
    suspend fun getEpisodeNavigation(@Path("slug") slug: String): Response<ApiResponse<EpisodeNavigation>>

    @GET("v1/video-source/{slug}")
    suspend fun getVideoSource(@Path("slug") slug: String): Response<ApiResponse<VideoSource>>

    // ── Social ────────────────────────────────────────────────────────────────
    @GET("v1/likes/{slug}")
    suspend fun getLikes(@Path("slug") slug: String): Response<ApiResponse<LikeData>>

    @POST("v1/likes/{slug}")
    suspend fun likeAnime(@Path("slug") slug: String): Response<ApiResponse<LikeData>>

    @DELETE("v1/likes/{slug}")
    suspend fun unlikeAnime(@Path("slug") slug: String): Response<ApiResponse<LikeData>>

    @GET("v1/comments/{slug}")
    suspend fun getComments(@Path("slug") slug: String): Response<ApiResponse<CommentData>>

    @POST("v1/comments/{slug}")
    suspend fun postComment(
        @Path("slug") slug: String,
        @Body body: CommentRequest,
    ): Response<ApiResponse<Comment>>

    @POST("v1/views/{slug}")
    suspend fun incrementViews(@Path("slug") slug: String): Response<ApiResponse<Any>>

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("v1/auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest): Response<ApiResponse<OtpResponse>>

    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<ApiResponse<AuthData>>

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<AuthData>>

    @POST("v1/auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>

    @GET("v1/auth/me")
    suspend fun getMe(): Response<ApiResponse<User>>

    // ── Watchlist ─────────────────────────────────────────────────────────────
    @GET("v1/user/watchlist")
    suspend fun getWatchlist(): Response<ApiResponse<WatchlistData>>

    @POST("v1/user/watchlist")
    suspend fun addToWatchlist(@Body body: WatchlistRequest): Response<ApiResponse<Any>>

    @DELETE("v1/user/watchlist/{slug}")
    suspend fun removeFromWatchlist(@Path("slug") slug: String): Response<ApiResponse<Any>>

    // ── History ───────────────────────────────────────────────────────────────
    @GET("v1/user/history")
    suspend fun getHistory(): Response<ApiResponse<HistoryData>>

    @POST("v1/user/history")
    suspend fun addHistory(@Body body: HistoryRequest): Response<ApiResponse<Any>>
}
