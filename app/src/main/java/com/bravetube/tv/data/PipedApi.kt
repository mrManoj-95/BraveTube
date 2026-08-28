package com.bravetube.tv.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Thin Piped API client with automatic instance failover.
 *
 * Public instances go down constantly, so every request walks a candidate list
 * (preferred instance first) and the first one that answers becomes the new preferred.
 */
class PipedApi(private val prefs: Prefs) {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    /** Exposed so the player can reuse the same connection pool for media. */
    val httpClient: OkHttpClient get() = http

    // ------------------------------------------------------------- endpoints

    suspend fun trending(region: String = prefs.region): List<StreamItem> =
        decodeList(get("/trending?region=$region"))

    suspend fun search(query: String, filter: String = "videos"): SearchResponse =
        json.decodeFromString(get("/search?q=${enc(query)}&filter=$filter"))

    suspend fun suggestions(query: String): List<String> = try {
        json.decodeFromString<List<String>>(get("/suggestions?query=${enc(query)}"))
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun streams(videoId: String): VideoDetails =
        json.decodeFromString(get("/streams/$videoId"))

    suspend fun channel(channelId: String): ChannelDetails =
        json.decodeFromString(get("/channel/$channelId"))

    private fun decodeList(body: String): List<StreamItem> = json.decodeFromString(body)

    // ------------------------------------------------------------- transport

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** Preferred instance first, then the rest of the known list. */
    private fun candidates(): List<String> {
        val preferred = prefs.instance.trimEnd('/')
        return (listOf(preferred) + Prefs.DEFAULT_INSTANCES).distinct()
    }

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        for (base in candidates()) {
            try {
                val body = fetch(base.trimEnd('/') + path)
                if (prefs.instance != base) prefs.instance = base
                return@withContext body
            } catch (e: Exception) {
                Log.w(TAG, "instance failed: $base$path -> ${e.message}")
                lastError = e
            }
        }
        throw IOException(
            "All Piped instances failed. Check your TV's internet connection, " +
                "or pick a different instance in Settings.",
            lastError
        )
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        http.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code} from $url")
            }
            if (body.isBlank()) throw IOException("Empty response from $url")
            // Piped reports upstream problems as {"error": "...", "message": "..."}
            if (body.startsWith("{\"error\"")) throw IOException("Instance error: ${body.take(180)}")
            return body
        }
    }

    companion object {
        private const val TAG = "PipedApi"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; BraveTube) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
