package com.bravetube.tv.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bravetube.tv.AppGraph
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.ui.components.MessageBlock
import com.bravetube.tv.ui.components.TvButton
import com.bravetube.tv.ui.components.VideoGrid

@Composable
fun HistoryScreen(
    onItemClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var items by remember { mutableStateOf(AppGraph.prefs.history()) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (items.isNotEmpty()) {
                TvButton(
                    text = "Clear history",
                    onClick = {
                        AppGraph.prefs.clearHistory()
                        items = emptyList()
                    },
                )
            }
        }

        if (items.isEmpty()) {
            MessageBlock(
                title = "Nothing watched yet",
                body = "Videos you play show up here so you can pick them back up.",
                modifier = Modifier.padding(top = 90.dp),
            )
        } else {
            VideoGrid(items = items, onItemClick = onItemClick)
        }
    }
}
