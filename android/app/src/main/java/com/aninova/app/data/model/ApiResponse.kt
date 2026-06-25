package com.aninova.app.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?,
    @SerializedName("meta") val meta: Meta?,
    @SerializedName("error_code") val errorCode: String?,
    // Flat auth fields (login/register return token+user at root level)
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: com.aninova.app.data.model.User?,
)

data class Meta(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("execution_time_ms") val executionTimeMs: Double,
    @SerializedName("cache_hit") val cacheHit: Boolean,
    @SerializedName("version") val version: String,
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = 0) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
