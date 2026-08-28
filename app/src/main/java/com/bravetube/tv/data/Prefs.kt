package com.bravetube.tv.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    // Field / row separators for the flat strings stored in SharedPreferences.
    private val SEP = '\u001F'
    private val ROW = '\u001E'

    private val sp: SharedPreferences =
        context.getSharedPreferences("bravetube", Context.MODE_PRIVATE)

    /** Base URL of the Piped instance currently in use (no trailing slash). */
    var instance: String
        get() = sp.getString(KEY_INSTANCE, null) ?: DEFAULT_INSTANCES.first()
        set(v) = sp.edit().putString(KEY_INSTANCE, v.trimEnd('/')).apply()

    /** Trending region, ISO-3166 alpha-2. */
    var region: String
        get() = sp.getString(KEY_REGION, null) ?: "IN"
        set(v) = sp.edit().putString(KEY_REGION, v).apply()

    /** Preferred max video height in pixels. 0 = highest available. */
    var maxHeight: Int
        get() = sp.getInt(KEY_QUALITY, 1080)
        set(v) = sp.edit().putInt(KEY_QUALITY, v).apply()

    /** Recent search terms, newest first. */
    var recentSearches: List<String>
        get() = sp.getString(KEY_RECENT, "")
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(v) = sp.edit()
            .putString(KEY_RECENT, v.distinct().take(20).joinToString(SEP.toString()))
            .apply()

    fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        recentSearches = listOf(q) + recentSearches.filter { !it.equals(q, ignoreCase = true) }
    }

    fun clearRecentSearches() {
        recentSearches = emptyList()
    }

    /** Watch history: "videoId|title|thumb|uploader|durationSeconds", newest first. */
    fun history(): List<StreamItem> =
        (sp.getString(KEY_HISTORY, "") ?: "")
            .split(ROW)
            .filter { it.isNotBlank() }
            .mapNotNull { row ->
                val p = row.split(SEP)
                if (p.size < 5) return@mapNotNull null
                StreamItem(
                    url = "/watch?v=${p[0]}",
                    type = "stream",
                    title = p[1],
                    thumbnail = p[2].takeIf { it.isNotBlank() },
                    uploaderName = p[3].takeIf { it.isNotBlank() },
                    duration = p[4].toLongOrNull() ?: -1,
                )
            }

    fun addToHistory(item: StreamItem) {
        val id = item.videoId ?: return
        val row = listOf(
            id,
            (item.title ?: "").replace(SEP, ' ').replace(ROW, ' '),
            item.thumbnail ?: "",
            (item.uploaderName ?: "").replace(SEP, ' ').replace(ROW, ' '),
            item.duration.toString(),
        ).joinToString(SEP.toString())

        val existing = (sp.getString(KEY_HISTORY, "") ?: "")
            .split(ROW)
            .filter { it.isNotBlank() && it.substringBefore(SEP) != id }

        sp.edit()
            .putString(KEY_HISTORY, (listOf(row) + existing).take(60).joinToString(ROW.toString()))
            .apply()
    }

    fun clearHistory() {
        sp.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_INSTANCE = "instance"
        private const val KEY_REGION = "region"
        private const val KEY_QUALITY = "quality"
        private const val KEY_RECENT = "recent"
        private const val KEY_HISTORY = "history"

        /**
         * Public Piped API instances. The app falls back down this list automatically
         * when one is unreachable, and remembers whichever answered last.
         * Current list: https://piped-instances.kavin.rocks/
         */
        val DEFAULT_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://api.piped.private.coffee",
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.ducks.party",
            "https://pipedapi.reallyaweso.me",
            "https://pipedapi.phoenixthrush.com",
            "https://piped-api.codespace.cz",
        )

        val REGIONS = listOf(
            "IN" to "India",
            "US" to "United States",
            "GB" to "United Kingdom",
            "CA" to "Canada",
            "AU" to "Australia",
            "DE" to "Germany",
            "FR" to "France",
            "JP" to "Japan",
            "KR" to "South Korea",
            "BR" to "Brazil",
            "SG" to "Singapore",
            "AE" to "UAE",
        )

        val QUALITIES = listOf(
            0 to "Highest available",
            1080 to "1080p",
            720 to "720p",
            480 to "480p",
            360 to "360p",
        )
    }
}
