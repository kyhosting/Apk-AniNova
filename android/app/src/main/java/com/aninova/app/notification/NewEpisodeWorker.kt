package com.aninova.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aninova.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NewEpisodeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "aninova_episode_check"
        private const val PREFS_NAME = "episode_notif_prefs"
        private const val KEY_KNOWN_SLUGS = "known_slugs"
        private const val KEY_FIRST_RUN = "first_run_done"
        private const val MAX_STORED_SLUGS = 300
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        return try {
            val latestEpisodes = fetchLatestEpisodes() ?: return Result.success()

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isFirstRun = !prefs.getBoolean(KEY_FIRST_RUN, false)
            val knownSlugs = prefs.getStringSet(KEY_KNOWN_SLUGS, emptySet())?.toMutableSet()
                ?: mutableSetOf()

            val newEpisodes = latestEpisodes.filter { (slug, _) -> slug !in knownSlugs }

            if (newEpisodes.isNotEmpty() && !isFirstRun) {
                showNotification(newEpisodes)
            }

            val allSlugs = knownSlugs.toMutableSet().apply {
                addAll(latestEpisodes.map { it.first })
            }
            val trimmed = if (allSlugs.size > MAX_STORED_SLUGS) {
                allSlugs.toList().takeLast(MAX_STORED_SLUGS).toSet()
            } else {
                allSlugs
            }

            prefs.edit()
                .putStringSet(KEY_KNOWN_SLUGS, trimmed)
                .putBoolean(KEY_FIRST_RUN, true)
                .apply()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchLatestEpisodes(): List<Pair<String, String>>? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${BuildConfig.BASE_URL}v1/latest"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val data = json.optJSONObject("data") ?: return@withContext null
                val results = data.optJSONArray("results") ?: return@withContext null

                val episodes = mutableListOf<Pair<String, String>>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val slug = item.optString("slug", "")
                    if (slug.isBlank()) continue
                    val title = item.optString("headline", item.optString("title", "Anime Baru"))
                    episodes.add(Pair(slug, title))
                }
                episodes
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun showNotification(newEpisodes: List<Pair<String, String>>) {
        if (newEpisodes.size == 1) {
            val (_, title) = newEpisodes[0]
            NotificationHelper.showNewEpisodeNotification(
                context = applicationContext,
                title = "Episode Baru! \uD83C\uDF9E\uFE0F",
                body = title,
                notifId = newEpisodes[0].first.hashCode(),
            )
        } else {
            val titleList = newEpisodes.take(3).joinToString(", ") { it.second }
            val more = if (newEpisodes.size > 3) " +${newEpisodes.size - 3} lainnya" else ""
            NotificationHelper.showNewEpisodeNotification(
                context = applicationContext,
                title = "${newEpisodes.size} Episode Baru! \uD83C\uDF9E\uFE0F",
                body = "$titleList$more",
                notifId = 100_001,
            )
        }
    }
}
