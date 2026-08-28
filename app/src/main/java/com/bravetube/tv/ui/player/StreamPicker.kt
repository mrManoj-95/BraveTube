package com.bravetube.tv.ui.player

import com.bravetube.tv.data.PipedStream
import com.bravetube.tv.data.VideoDetails

/**
 * Picks which of YouTube's many renditions to feed ExoPlayer.
 *
 * Android TV boxes vary wildly in what they can hardware-decode, so H.264 (avc /
 * MPEG_4) and AAC (m4a) are preferred over VP9/AV1 and Opus. Falling back to a
 * lower-fidelity stream that actually plays beats a 4K stream that stutters.
 */
object StreamPicker {

    private fun PipedStream.isUsable() = !url.isNullOrBlank()

    private fun PipedStream.isH264(): Boolean {
        val c = (codec ?: "").lowercase()
        val m = (mimeType ?: "").lowercase()
        val f = (format ?: "").lowercase()
        return c.startsWith("avc") || m.contains("mp4") || f.contains("mpeg_4") || f.contains("m4a")
    }

    private fun PipedStream.isAac(): Boolean {
        val c = (codec ?: "").lowercase()
        val m = (mimeType ?: "").lowercase()
        val f = (format ?: "").lowercase()
        return c.startsWith("mp4a") || m.contains("mp4") || f.contains("m4a")
    }

    /** Distinct heights offered for this video, highest first — powers the quality menu. */
    fun availableHeights(details: VideoDetails): List<Int> =
        details.videoStreams
            .filter { it.isUsable() && it.heightPx > 0 }
            .map { it.heightPx }
            .distinct()
            .sortedDescending()

    /**
     * Best video-only rendition at or below [maxHeight] (0 = no cap).
     * Returns null when the video has no adaptive renditions.
     */
    fun bestVideo(details: VideoDetails, maxHeight: Int): PipedStream? {
        val pool = details.videoStreams.filter { it.isUsable() && it.videoOnly }
        if (pool.isEmpty()) return null

        val capped = if (maxHeight > 0) {
            pool.filter { it.heightPx in 1..maxHeight }.ifEmpty {
                // Everything is above the cap — take the smallest available instead.
                pool.filter { it.heightPx > 0 }.sortedBy { it.heightPx }.take(1)
            }
        } else {
            pool
        }
        if (capped.isEmpty()) return null

        val preferred = capped.filter { it.isH264() }.ifEmpty { capped }
        return preferred.maxWithOrNull(
            compareBy<PipedStream>({ it.heightPx }, { it.fps }, { it.bitrate })
        )
    }

    /** Best audio-only rendition, AAC preferred. */
    fun bestAudio(details: VideoDetails): PipedStream? {
        val pool = details.audioStreams.filter { it.isUsable() }
        if (pool.isEmpty()) return null
        val preferred = pool.filter { it.isAac() }.ifEmpty { pool }
        return preferred.maxByOrNull { it.bitrate }
    }

    /** Progressive stream that already contains audio — the universal fallback. */
    fun muxed(details: VideoDetails, maxHeight: Int): PipedStream? {
        val pool = details.videoStreams.filter { it.isUsable() && !it.videoOnly }
        if (pool.isEmpty()) return null
        val capped = if (maxHeight > 0) pool.filter { it.heightPx <= maxHeight } else pool
        return (capped.ifEmpty { pool }).maxByOrNull { it.heightPx }
    }

    /** The exact rendition at a user-chosen height, video-only. */
    fun videoAtHeight(details: VideoDetails, height: Int): PipedStream? {
        val pool = details.videoStreams.filter { it.isUsable() && it.videoOnly && it.heightPx == height }
        val preferred = pool.filter { it.isH264() }.ifEmpty { pool }
        return preferred.maxByOrNull { it.bitrate }
    }
}
