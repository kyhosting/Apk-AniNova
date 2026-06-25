package com.aninova.app.data.api

import com.aninova.app.data.local.TokenDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TokenInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore,
) : Interceptor {

    companion object {
        private const val CHROME_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.token.first() }
        val builder = chain.request().newBuilder()
            .header("User-Agent", CHROME_UA)
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
