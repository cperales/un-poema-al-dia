package com.webpoema.app

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class Poem(val title: String, val author: String, val text: String)

/**
 * Reads the shared "poem of the day" published as today.json by the CDK stack.
 * It is the app's only source of poems: none are bundled in the APK.
 *
 * The last successful response is cached, so the app and the notification
 * still show a poem when the phone is offline. With no network and no cache
 * (fresh install, offline), there is no poem.
 *
 * All network access is blocking: call these methods from [executor], never
 * from the main thread.
 */
object TodayRepository {

    private const val TAG = "TodayRepository"
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 4_000

    /** Two workers: the app and the notification receiver never queue behind each other. */
    val executor: ExecutorService = Executors.newFixedThreadPool(2)

    /** Network first, then the last cached today.json. Returns null if both fail. */
    fun fetchBlocking(context: Context): Poem? {
        val fetched = fetchFromNetwork()
        if (fetched != null) {
            PreferenceManager.setCachedTodayJson(context, fetched.second)
            return fetched.first
        }
        return PreferenceManager.getCachedTodayJson(context)?.let(::parsePoem)
    }

    private fun fetchFromNetwork(): Pair<Poem, String>? {
        val baseUrl = BuildConfig.TODAY_JSON_URL
        if (baseUrl.isBlank()) return null

        // today.json is served with max-age=300; a query timestamp keeps the
        // notification from showing a poem cached before the daily run.
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val url = "$baseUrl${separator}v=${System.currentTimeMillis()}"

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "today.json returned HTTP $code")
                return null
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parsePoem(body)?.let { it to body }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch today.json: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parsePoem(json: String): Poem? = try {
        val obj = JSONObject(json)
        val title = obj.optString("title").trim()
        val author = obj.optString("author").trim()
        val text = obj.optString("text")
        if (title.isEmpty() || text.isBlank()) null else Poem(title, author, text)
    } catch (e: Exception) {
        Log.w(TAG, "Malformed today.json: ${e.message}")
        null
    }
}
