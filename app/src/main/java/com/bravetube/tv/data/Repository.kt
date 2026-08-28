package com.bravetube.tv.data

/** What a home-screen carousel is made of. */
sealed class ShelfSource {
    object Trending : ShelfSource()
    data class Query(val query: String) : ShelfSource()
    object History : ShelfSource()
}

data class ShelfDef(val id: String, val title: String, val source: ShelfSource)

class Repository(val api: PipedApi, val prefs: Prefs) {

    /**
     * The home feed. Piped has no personalised feed, so the shelves below approximate
     * YouTube's home layout with trending plus one curated query per category.
     */
    fun homeShelves(): List<ShelfDef> {
        val regionName = Prefs.REGIONS.firstOrNull { it.first == prefs.region }?.second ?: prefs.region
        val shelves = mutableListOf<ShelfDef>()

        if (prefs.history().isNotEmpty()) {
            shelves += ShelfDef("history", "Continue watching", ShelfSource.History)
        }
        shelves += ShelfDef("trending", "Trending in $regionName", ShelfSource.Trending)
        shelves += listOf(
            ShelfDef("music", "Music", ShelfSource.Query("music videos $regionName")),
            ShelfDef("gaming", "Gaming", ShelfSource.Query("gaming highlights")),
            ShelfDef("news", "News", ShelfSource.Query("news today")),
            ShelfDef("movies", "Movies & shows", ShelfSource.Query("full movie")),
            ShelfDef("sports", "Sports", ShelfSource.Query("sports highlights")),
            ShelfDef("tech", "Technology", ShelfSource.Query("technology review")),
            ShelfDef("comedy", "Comedy", ShelfSource.Query("stand up comedy")),
            ShelfDef("cooking", "Food & cooking", ShelfSource.Query("cooking recipe")),
            ShelfDef("learning", "Learning", ShelfSource.Query("documentary explained")),
        )
        return shelves
    }

    suspend fun loadShelf(def: ShelfDef): List<StreamItem> = when (val s = def.source) {
        is ShelfSource.History -> prefs.history()
        is ShelfSource.Trending -> api.trending().filter { it.isVideo }.take(30)
        is ShelfSource.Query -> api.search(s.query, "videos").items.filter { it.isVideo }.take(30)
    }

    suspend fun search(query: String, filter: String) = api.search(query, filter)

    suspend fun suggestions(query: String) = api.suggestions(query)

    suspend fun video(videoId: String) = api.streams(videoId)

    suspend fun channel(channelId: String) = api.channel(channelId)
}
