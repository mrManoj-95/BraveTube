package com.bravetube.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bravetube.tv.data.Shelf
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.data.formatDuration
import com.bravetube.tv.data.formatSubs
import com.bravetube.tv.data.formatViews
import com.bravetube.tv.ui.theme.YtChip
import com.bravetube.tv.ui.theme.YtTextSecondary

const val CARD_WIDTH = 288
private const val THUMB_RATIO = 16f / 9f

@Composable
fun VideoCard(
    item: StreamItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthDp: Int? = CARD_WIDTH,
    showChannel: Boolean = true,
) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = if (widthDp != null) modifier.width(widthDp.dp) else modifier.fillMaxWidth(),
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) YtChip else Color.Transparent)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(THUMB_RATIO)
                    .clip(RoundedCornerShape(10.dp))
                    .background(YtChip)
            ) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (item.duration > 0) {
                    Pill(
                        text = formatDuration(item.duration),
                        background = Color(0xCC000000),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                    )
                }
            }

            VSpace(8)

            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(46.dp),
            )

            val uploader = item.uploaderName.orEmpty()
            if (showChannel && uploader.isNotBlank()) {
                Text(
                    text = uploader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = YtTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val meta = listOfNotNull(
                formatViews(item.views).takeIf { it.isNotBlank() },
                item.uploadedDate?.takeIf { it.isNotBlank() },
            ).joinToString(" • ")

            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = YtTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ChannelCard(
    item: StreamItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthDp: Int? = CARD_WIDTH,
) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = if (widthDp != null) modifier.width(widthDp.dp) else modifier.fillMaxWidth(),
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) YtChip else Color.Transparent)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.thumbnail ?: item.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(YtChip),
            )
            HSpace(12)
            Column {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subs = formatSubs(item.subscribers)
                if (subs.isNotBlank()) {
                    Text(
                        text = subs,
                        style = MaterialTheme.typography.labelMedium,
                        color = YtTextSecondary,
                    )
                }
            }
        }
    }
}

/** One horizontal carousel, YouTube-style. */
@Composable
fun ShelfRow(
    shelf: Shelf,
    onItemClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 18.dp)) {
        Text(
            text = shelf.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
        )

        when {
            shelf.loading -> LoadingBlock(height = 210)

            shelf.error != null -> Text(
                text = shelf.error,
                style = MaterialTheme.typography.bodyMedium,
                color = YtTextSecondary,
                modifier = Modifier.padding(start = 48.dp, bottom = 20.dp),
            )

            shelf.items.isEmpty() -> Text(
                text = "Nothing here right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = YtTextSecondary,
                modifier = Modifier.padding(start = 48.dp, bottom = 20.dp),
            )

            else -> LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 42.dp,
                    end = 42.dp,
                    top = 6.dp,
                    bottom = 10.dp,
                ),
            ) {
                items(shelf.items) { item ->
                    if (item.isChannel) {
                        ChannelCard(item = item, onClick = { onItemClick(item) })
                    } else {
                        VideoCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}
