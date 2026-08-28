package com.bravetube.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bravetube.tv.data.StreamItem

/**
 * Responsive grid of results. Cards keep the same look as the home shelves so the
 * whole app reads as one surface.
 */
@Composable
fun VideoGrid(
    items: List<StreamItem>,
    onItemClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    columnMinWidth: Int = 300,
    contentPadding: PaddingValues = PaddingValues(
        start = 42.dp, end = 42.dp, top = 8.dp, bottom = 40.dp
    ),
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = columnMinWidth.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        items(items) { item ->
            if (item.isChannel) {
                ChannelCard(item = item, onClick = { onItemClick(item) }, widthDp = null)
            } else {
                VideoCard(item = item, onClick = { onItemClick(item) }, widthDp = null)
            }
        }
    }
}
