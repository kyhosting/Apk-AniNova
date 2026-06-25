package com.aninova.app.data.repository

import com.aninova.app.data.api.ApiService
import com.aninova.app.data.local.TokenDataStore
import com.aninova.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
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
                    } else if (isOk) {
                        @Suppress("UNCHECKED_CAST")
                        Result.Success(Unit as T)
                    } else {
                        Result.Error(body?.message ?: "Terjadi kesalahan")
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    val msg = try {
                        val json = org.json.JSONObject(errBody ?: "{}")
                        json.optString("message", "HTTP ${response.code()}")
                    } catch (e: Exception) {
                        "HTTP ${response.code()}"
                    }
                    Result.Error(msg, response.code())
                }
            } catch (e: Exception) {
                Result.Error(e.localizedMessage ?: "Koneksi gagal")
            }
        }

    private suspend fun safeAuthCall(block: suspend () -> retrofit2.Response<ApiResponse<AuthData>>): Result<AuthData> =
        withContext(Dispatchers.IO) {
            try {
                val response = block()
                if (response.isSuccessful) {
                    val body = response.body()
                    val isOk = body?.status == "success" || body?.status == "ok"
                    if (!isOk) return@withContext Result.Error(body?.message ?: "Login gagal")
                    val authData = if (body?.data != null) {
                        body.data
                    } else {
                        AuthData(
                            token = body?.token,
                            user = body?.user,
                            message = body?.message,
                        )
                    }
                    Result.Success(authData)
                } else {
                    val errBody = response.errorBody()?.string()
                    val msg = try {
                        val json = org.json.JSONObject(errBody ?: "{}")
                        json.optString("message", "HTTP ${response.code()}")
                    } catch (e: Exception) {
                        "HTTP ${response.code()}"
                    }
                    Result.Error(msg, response.code())
                }
            } catch (e: Exception) {
                Result.Error(e.localizedMessage ?: "Koneksi gagal")
            }
        }

    suspend fun sendOtp(email: String, username: String = "", password: String = "") =
        safeCall { api.sendOtp(SendOtpRequest(email, username, password)) }

    suspend fun register(email: String, username: String, password: String, otp: String): Result<AuthData> {
        val result = safeAuthCall { api.register(RegisterRequest(email, otp)) }
        if (result is Result.Success) {
            result.data.token?.let { tokenDataStore.saveToken(it) }
        }
        return result
    }

    suspend fun login(email: String, password: String): Result<AuthData> {
        val result = safeAuthCall { api.login(LoginRequest(login = email, password = password)) }
        if (result is Result.Success) {
            result.data.token?.let { tokenDataStore.saveToken(it) }
        }
        return result
    }

    suspend fun logout() {
        safeCall { api.logout() }
        tokenDataStore.clearToken()
    }

    suspend fun getMe() = safeCall { api.getMe() }

    val tokenFlow = tokenDataStore.token
}
