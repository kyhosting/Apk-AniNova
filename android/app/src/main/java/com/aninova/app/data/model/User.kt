package com.aninova.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("created_at") val createdAt: String?,
)

data class AuthData(
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: User?,
    @SerializedName("message") val message: String?,
)

data class LoginRequest(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String,
)

data class SendOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String = "",
    @SerializedName("password") val password: String = "",
)

data class OtpResponse(
    @SerializedName("sent") val sent: Boolean?,
    @SerializedName("dev_otp") val devOtp: String?,
    @SerializedName("message") val message: String?,
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp_code") val otpCode: String,
)

data class CommentRequest(
    @SerializedName("content") val content: String,
)

data class WatchlistRequest(
    @SerializedName("slug") val slug: String,
    @SerializedName("title") val title: String,
    @SerializedName("thumbnail") val thumbnail: String,
)

data class HistoryRequest(
    @SerializedName("slug") val slug: String,
    @SerializedName("episode_slug") val episodeSlug: String,
    @SerializedName("title") val title: String,
    @SerializedName("thumbnail") val thumbnail: String,
)
