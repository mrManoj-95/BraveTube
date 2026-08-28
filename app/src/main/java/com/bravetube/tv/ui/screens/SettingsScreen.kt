package com.bravetube.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bravetube.tv.AppGraph
import com.bravetube.tv.data.Prefs
import com.bravetube.tv.ui.components.FocusCard
import com.bravetube.tv.ui.components.TvButton
import com.bravetube.tv.ui.theme.YtRed
import com.bravetube.tv.ui.theme.YtSurface
import com.bravetube.tv.ui.theme.YtTextSecondary

@Composable
fun SettingsScreen(
    onSettingsChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = AppGraph.prefs
    var instance by remember { mutableStateOf(prefs.instance) }
    var region by remember { mutableStateOf(prefs.region) }
    var quality by remember { mutableStateOf(prefs.maxHeight) }
    var toast by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 48.dp, end = 64.dp, top = 28.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "BraveTube 1.0.0 • no account, no ads, no tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = YtTextSecondary,
                modifier = Modifier.padding(bottom = 22.dp),
            )
        }

        item { SettingHeader("Content source", "Piped instance used for search, trending and playback. If videos stop loading, try another one.") }
        items(Prefs.DEFAULT_INSTANCES.size) { i ->
            val value = Prefs.DEFAULT_INSTANCES[i]
            OptionRow(
                title = value.removePrefix("https://"),
                selected = instance.trimEnd('/') == value,
                onClick = {
                    prefs.instance = value
                    instance = value
                    toast = "Switched to ${value.removePrefix("https://")}"
                    onSettingsChanged()
                },
            )
        }

        item { SettingHeader("Trending region", "Which country's trending list the Home and Trending screens show.") }
        items(Prefs.REGIONS.size) { i ->
            val (code, name) = Prefs.REGIONS[i]
            OptionRow(
                title = name,
                subtitle = code,
                selected = region == code,
                onClick = {
                    prefs.region = code
                    region = code
                    toast = "Region set to $name"
                    onSettingsChanged()
                },
            )
        }

        item { SettingHeader("Video quality", "Highest resolution the player will pick. Lower it if playback stutters on older TV hardware.") }
        items(Prefs.QUALITIES.size) { i ->
            val (px, label) = Prefs.QUALITIES[i]
            OptionRow(
                title = label,
                selected = quality == px,
                onClick = {
                    prefs.maxHeight = px
                    quality = px
                    toast = "Quality set to $label"
                },
            )
        }

        item {
            SettingHeader("Data", "Everything is stored on this device only.")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            ) {
                TvButton(text = "Clear watch history", onClick = {
                    prefs.clearHistory()
                    toast = "Watch history cleared"
                    onSettingsChanged()
                })
                TvButton(text = "Clear recent searches", onClick = {
                    prefs.clearRecentSearches()
                    toast = "Recent searches cleared"
                })
            }
            val message = toast
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = YtRed,
                )
            }
        }
    }
}

@Composable
private fun SettingHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = YtTextSecondary,
            )
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        focusScale = 1.01f,
        borderWidthDp = 0,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) Color.White else YtSurface)
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            selected && focused -> YtRed
                            selected -> YtRed
                            focused -> Color(0xFFDDDDDD)
                            else -> Color(0xFF3A3A3A)
                        }
                    )
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (focused) Color.Black else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (focused) Color(0xFF555555) else YtTextSecondary,
                    )
                }
            }
        }
    }
}
