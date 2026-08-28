package com.bravetube.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.data.extractVideoId
import com.bravetube.tv.ui.components.NavDest
import com.bravetube.tv.ui.components.NavRail
import com.bravetube.tv.ui.player.PlayerScreen
import com.bravetube.tv.ui.screens.ChannelScreen
import com.bravetube.tv.ui.screens.HistoryScreen
import com.bravetube.tv.ui.screens.HomeScreen
import com.bravetube.tv.ui.screens.SearchScreen
import com.bravetube.tv.ui.screens.SettingsScreen
import com.bravetube.tv.ui.screens.TrendingScreen
import com.bravetube.tv.ui.theme.BraveTubeTheme
import com.bravetube.tv.ui.theme.YtBackground

sealed class Route {
    data object Home : Route()
    data object Search : Route()
    data object Trending : Route()
    data object History : Route()
    data object Settings : Route()
    data class Channel(val channelId: String) : Route()
    data class Player(val videoId: String, val seed: StreamItem?) : Route()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(application)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val deepLinkVideoId = intent?.data?.toString()?.let { extractVideoId(it) }

        setContent {
            BraveTubeTheme {
                RootApp(initialVideoId = deepLinkVideoId)
            }
        }
    }
}

@Composable
private fun RootApp(initialVideoId: String? = null) {
    val stack = remember {
        mutableStateListOf<Route>(Route.Home).also { s ->
            if (initialVideoId != null) s.add(Route.Player(initialVideoId, null))
        }
    }
    // Bumping this forces screens that read prefs directly to recompose after a settings change.
    var settingsRevision by remember { mutableStateOf(0) }

    val current = stack.last()

    BackHandler(enabled = stack.size > 1) {
        stack.removeAt(stack.lastIndex)
    }

    fun goTop(route: Route) {
        stack.clear()
        stack.add(route)
    }

    fun open(item: StreamItem) {
        when {
            item.isChannel -> item.channelId?.let { stack.add(Route.Channel(it)) }
            else -> item.videoId?.let { stack.add(Route.Player(it, item)) }
        }
    }

    Box(Modifier.fillMaxSize().background(YtBackground)) {
        if (current is Route.Player) {
            PlayerScreen(
                videoId = current.videoId,
                seed = current.seed,
                onPlayVideo = { next ->
                    next.videoId?.let { id ->
                        stack.removeAt(stack.lastIndex)
                        stack.add(Route.Player(id, next))
                    }
                },
                onExit = {
                    if (stack.size > 1) stack.removeAt(stack.lastIndex) else goTop(Route.Home)
                },
            )
            return@Box
        }

        Row(Modifier.fillMaxSize()) {
            NavRail(
                current = current.toNavDest(),
                onSelect = { dest ->
                    goTop(
                        when (dest) {
                            NavDest.Search -> Route.Search
                            NavDest.Home -> Route.Home
                            NavDest.Trending -> Route.Trending
                            NavDest.History -> Route.History
                            NavDest.Settings -> Route.Settings
                        }
                    )
                },
            )

            Box(Modifier.fillMaxSize()) {
                when (current) {
                    is Route.Home -> HomeScreen(onItemClick = { open(it) })
                    is Route.Search -> SearchScreen(onItemClick = { open(it) })
                    is Route.Trending -> TrendingScreen(onItemClick = { open(it) })
                    is Route.History -> key(settingsRevision) {
                        HistoryScreen(onItemClick = { open(it) })
                    }
                    is Route.Settings -> SettingsScreen(
                        onSettingsChanged = { settingsRevision++ },
                    )
                    is Route.Channel -> ChannelScreen(
                        channelId = current.channelId,
                        onItemClick = { open(it) },
                    )
                    is Route.Player -> Unit // handled above
                }
            }
        }
    }
}

private fun Route.toNavDest(): NavDest = when (this) {
    is Route.Search -> NavDest.Search
    is Route.Trending -> NavDest.Trending
    is Route.History -> NavDest.History
    is Route.Settings -> NavDest.Settings
    else -> NavDest.Home
}
