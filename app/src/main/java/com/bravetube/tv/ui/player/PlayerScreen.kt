package com.bravetube.tv.ui.player

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bravetube.tv.AppGraph
import com.bravetube.tv.data.PipedApi
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.data.VideoDetails
import com.bravetube.tv.data.formatPosition
import com.bravetube.tv.data.formatViews
import com.bravetube.tv.ui.components.FocusCard
import com.bravetube.tv.ui.components.FullScreenLoader
import com.bravetube.tv.ui.components.MessageBlock
import com.bravetube.tv.ui.components.VideoCard
import com.bravetube.tv.ui.theme.YtRed
import com.bravetube.tv.ui.theme.YtSurface
import com.bravetube.tv.ui.theme.YtTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEEK_STEP_MS = 10_000L
private const val SEEK_JUMP_MS = 30_000L
private const val CONTROLS_TIMEOUT_MS = 4_500L

class PlayerViewModel : ViewModel() {
    var details by mutableStateOf<VideoDetails?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var loadedId: String? = null

    fun load(videoId: String, force: Boolean = false) {
        if (!force && loadedId == videoId && details != null) return
        loadedId = videoId
        loading = true
        error = null
        details = null
        viewModelScope.launch {
            try {
                details = AppGraph.repo.video(videoId)
            } catch (e: Exception) {
                error = e.message ?: "Couldn't load this video."
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun PlayerScreen(
    videoId: String,
    seed: StreamItem?,
    onPlayVideo: (StreamItem) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    vm: PlayerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = AppGraph.prefs

    LaunchedEffect(videoId) { vm.load(videoId) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(Unit) { onDispose { exo.release() } }

    // ---------------------------------------------------------------- state
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var showQuality by remember { mutableStateOf(false) }
    var showUpNext by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var buffered by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var chosenHeight by remember { mutableIntStateOf(prefs.maxHeight) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    fun poke() {
        controlsVisible = true
        interactionTick++
    }

    // -------------------------------------------------------- prepare media
    val details = vm.details
    LaunchedEffect(details, chosenHeight) {
        val d = details ?: return@LaunchedEffect
        playbackError = null
        val source = createMediaSource(context, d, chosenHeight)
        if (source == null) {
            playbackError = "No playable stream was returned for this video. " +
                "Try a different Piped instance in Settings."
            return@LaunchedEffect
        }
        val resume = exo.currentPosition.takeIf { exo.currentMediaItem != null && it > 1_000 } ?: 0L
        exo.setMediaSource(source)
        exo.prepare()
        if (resume > 0) exo.seekTo(resume)
        exo.playWhenReady = true

        seed?.let { prefs.addToHistory(it) }
            ?: prefs.addToHistory(
                StreamItem(
                    url = "/watch?v=$videoId",
                    title = d.title,
                    thumbnail = d.thumbnailUrl,
                    uploaderName = d.uploader,
                    duration = d.duration,
                )
            )
    }

    // --------------------------------------------------------- player events
    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    val next = vm.details?.relatedStreams?.firstOrNull { it.isVideo && it.videoId != null }
                    if (next != null) onPlayVideo(next) else onExit()
                }
            }

            override fun onPlayerError(err: androidx.media3.common.PlaybackException) {
                playbackError = "Playback failed (${err.errorCodeName}). " +
                    "Lower the quality in Settings, or switch Piped instance."
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener) }
    }

    LaunchedEffect(exo) {
        while (true) {
            position = exo.currentPosition
            buffered = exo.bufferedPosition
            duration = exo.duration.takeIf { it > 0 } ?: ((vm.details?.duration ?: 0L) * 1000L)
            delay(500)
        }
    }

    // auto-hide the overlay
    LaunchedEffect(interactionTick, controlsVisible, showQuality, showUpNext) {
        if (!controlsVisible || showQuality || showUpNext) return@LaunchedEffect
        delay(CONTROLS_TIMEOUT_MS)
        controlsVisible = false
    }

    // ------------------------------------------------------------ back stack
    BackHandler(enabled = showQuality) { showQuality = false }
    BackHandler(enabled = showUpNext) { showUpNext = false }
    BackHandler(enabled = !showQuality && !showUpNext && controlsVisible) { controlsVisible = false }

    // ------------------------------------------------------------------- ui
    val stage = remember { FocusRequester() }
    LaunchedEffect(showQuality, showUpNext) {
        // The stage owns D-pad handling, so make sure it actually gets focus back
        // once any overlay closes. Retry briefly in case the node isn't attached yet.
        if (!showQuality && !showUpNext) {
            repeat(6) {
                if (runCatching { stage.requestFocus() }.isSuccess) return@LaunchedEffect
                delay(100)
            }
        }
    }

    val overlayOpen = showQuality || showUpNext

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(stage)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || overlayOpen) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!controlsVisible) poke() else {
                            if (exo.isPlaying) exo.pause() else exo.play()
                            poke()
                        }
                        true
                    }

                    Key.MediaPlayPause -> {
                        if (exo.isPlaying) exo.pause() else exo.play(); poke(); true
                    }

                    Key.MediaPlay -> { exo.play(); poke(); true }
                    Key.MediaPause -> { exo.pause(); poke(); true }

                    Key.DirectionLeft, Key.MediaRewind -> {
                        val step = if (event.key == Key.MediaRewind) SEEK_JUMP_MS else SEEK_STEP_MS
                        exo.seekTo((exo.currentPosition - step).coerceAtLeast(0L))
                        poke(); true
                    }

                    Key.DirectionRight, Key.MediaFastForward -> {
                        val step = if (event.key == Key.MediaFastForward) SEEK_JUMP_MS else SEEK_STEP_MS
                        val max = exo.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        exo.seekTo((exo.currentPosition + step).coerceAtMost(max))
                        poke(); true
                    }

                    Key.DirectionUp -> { showQuality = true; controlsVisible = true; true }
                    Key.DirectionDown -> { showUpNext = true; controlsVisible = true; true }
                    else -> false
                }
            }
            .focusable(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    player = exo
                }
            },
            update = { it.player = exo },
        )

        if (vm.loading) {
            FullScreenLoader("Loading video…")
        }

        val fatal = vm.error ?: playbackError
        if (fatal != null) {
            Box(
                Modifier.fillMaxSize().background(Color(0xE6000000)),
                contentAlignment = Alignment.Center,
            ) {
                MessageBlock(
                    title = "Can't play this video",
                    body = fatal,
                    actionLabel = "Go back",
                    onAction = onExit,
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            ControlsOverlay(
                title = details?.title ?: seed?.displayTitle ?: "",
                channel = details?.uploader ?: seed?.uploaderName ?: "",
                views = details?.views ?: seed?.views ?: -1,
                isPlaying = isPlaying,
                position = position,
                buffered = buffered,
                duration = duration,
                qualityLabel = if (chosenHeight == 0) "Auto" else "${chosenHeight}p",
                isLive = details?.livestream == true,
            )
        }

        if (showQuality && details != null) {
            QualitySheet(
                heights = StreamPicker.availableHeights(details),
                current = chosenHeight,
                onPick = {
                    chosenHeight = it
                    prefs.maxHeight = it
                    showQuality = false
                    poke()
                },
                onDismiss = { showQuality = false },
            )
        }

        if (showUpNext && details != null) {
            UpNextRow(
                items = details.relatedStreams.filter { it.isVideo }.take(20),
                onPick = { showUpNext = false; onPlayVideo(it) },
            )
        }
    }
}

// ------------------------------------------------------------------ overlay

@Composable
private fun ControlsOverlay(
    title: String,
    channel: String,
    views: Long,
    isPlaying: Boolean,
    position: Long,
    buffered: Long,
    duration: Long,
    qualityLabel: String,
    isLive: Boolean,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xB3000000),
                    0.28f to Color(0x00000000),
                    0.62f to Color(0x00000000),
                    1f to Color(0xE6000000),
                )
            )
    ) {
        // top: what's playing
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 48.dp, top = 34.dp, end = 48.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                channel.takeIf { it.isNotBlank() },
                formatViews(views).takeIf { it.isNotBlank() },
                if (isLive) "LIVE" else null,
            ).joinToString("  •  ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.titleMedium,
                    color = YtTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // bottom: scrubber + hints
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, bottom = 34.dp)
        ) {
            Scrubber(position = position, buffered = buffered, duration = duration)

            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isLive) "LIVE" else "${formatPosition(position)} / ${formatPosition(duration)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Box(Modifier.weight(1f))
                Text(
                    text = if (isPlaying) "Playing" else "Paused",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPlaying) Color.White else YtRed,
                )
                Text(
                    text = "   •   $qualityLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = YtTextSecondary,
                )
            }

            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Hint("OK", "play / pause")
                Hint("◀ ▶", "seek 10s")
                Hint("▲", "quality")
                Hint("▼", "up next")
                Hint("BACK", "exit")
            }
        }
    }
}

@Composable
private fun Hint(keyLabel: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0x33FFFFFF))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(keyLabel, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Text(
            "  $action",
            style = MaterialTheme.typography.labelMedium,
            color = YtTextSecondary,
        )
    }
}

@Composable
private fun Scrubber(position: Long, buffered: Long, duration: Long) {
    val total = duration.coerceAtLeast(1L).toFloat()
    val playedFraction = (position.toFloat() / total).coerceIn(0f, 1f)
    val bufferedFraction = (buffered.toFloat() / total).coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0x59FFFFFF))
    ) {
        Box(
            Modifier
                .fillMaxWidth(bufferedFraction)
                .height(6.dp)
                .background(Color(0x99FFFFFF))
        )
        Box(
            Modifier
                .fillMaxWidth(playedFraction)
                .height(6.dp)
                .background(YtRed)
        )
    }
}

// ------------------------------------------------------------- quality menu

@Composable
private fun QualitySheet(
    heights: List<Int>,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(360.dp)
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(YtSurface)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Quality",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            QualityOption("Auto (highest available)", current == 0, Modifier.focusRequester(first)) {
                onPick(0)
            }
            heights.forEach { h ->
                QualityOption("${h}p", current == h) { onPick(h) }
            }
        }
    }
}

@Composable
private fun QualityOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        focusScale = 1.02f,
        borderWidthDp = 0,
        modifier = modifier.fillMaxWidth(),
    ) { focused ->
        Box(
            Modifier
                .fillMaxWidth()
                .background(if (focused) Color.White else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = if (selected) "$label  ✓" else label,
                style = MaterialTheme.typography.titleMedium,
                color = if (focused) Color.Black else Color.White,
            )
        }
    }
}

// ----------------------------------------------------------------- up next

@Composable
private fun UpNextRow(items: List<StreamItem>, onPick: (StreamItem) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0x00000000), Color(0xF2000000)))
                )
                .padding(top = 40.dp, bottom = 20.dp)
        ) {
            Text(
                "Up next",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 48.dp, bottom = 10.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 42.dp, end = 42.dp, bottom = 8.dp),
            ) {
                items(items) { item ->
                    VideoCard(
                        item = item,
                        onClick = { onPick(item) },
                        widthDp = 250,
                        modifier = if (items.firstOrNull() === item) {
                            Modifier.focusRequester(first)
                        } else Modifier,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------ media sources

@androidx.annotation.OptIn(UnstableApi::class)
private fun createMediaSource(
    context: Context,
    details: VideoDetails,
    maxHeight: Int,
): MediaSource? {
    val factory = DefaultHttpDataSource.Factory()
        .setUserAgent(PipedApi.USER_AGENT)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)

    val hls = details.hls
    if (details.livestream && !hls.isNullOrBlank()) {
        return HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(hls))
    }

    val video = StreamPicker.bestVideo(details, maxHeight)
    val audio = StreamPicker.bestAudio(details)
    val videoUrl = video?.url
    val audioUrl = audio?.url

    if (videoUrl != null && audioUrl != null) {
        val videoSource = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
        val audioSource = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(audioUrl))
        return MergingMediaSource(videoSource, audioSource)
    }

    val muxedUrl = StreamPicker.muxed(details, maxHeight)?.url
    if (muxedUrl != null) {
        return ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(muxedUrl))
    }

    if (!hls.isNullOrBlank()) {
        return HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(hls))
    }
    return null
}
