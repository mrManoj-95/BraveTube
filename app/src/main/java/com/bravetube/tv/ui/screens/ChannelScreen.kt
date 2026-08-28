package com.bravetube.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bravetube.tv.AppGraph
import com.bravetube.tv.data.ChannelDetails
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.data.formatSubs
import com.bravetube.tv.ui.components.FullScreenLoader
import com.bravetube.tv.ui.components.MessageBlock
import com.bravetube.tv.ui.components.VideoGrid
import com.bravetube.tv.ui.theme.YtChip
import com.bravetube.tv.ui.theme.YtTextSecondary
import kotlinx.coroutines.launch

class ChannelViewModel : ViewModel() {
    var details by mutableStateOf<ChannelDetails?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var loadedId: String? = null

    fun load(channelId: String, force: Boolean = false) {
        if (!force && loadedId == channelId && details != null) return
        loadedId = channelId
        loading = true
        error = null
        details = null
        viewModelScope.launch {
            try {
                details = AppGraph.repo.channel(channelId)
            } catch (e: Exception) {
                error = e.message ?: "Couldn't load this channel."
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun ChannelScreen(
    channelId: String,
    onItemClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    vm: ChannelViewModel = viewModel(),
) {
    LaunchedEffect(channelId) { vm.load(channelId) }

    val details = vm.details

    when {
        vm.loading -> FullScreenLoader("Loading channel…")

        details == null -> MessageBlock(
            title = vm.error ?: "Channel unavailable",
            actionLabel = "Retry",
            onAction = { vm.load(channelId, force = true) },
            modifier = modifier.fillMaxSize().padding(top = 160.dp),
        )

        else -> VideoGrid(
            items = details.relatedStreams,
            onItemClick = onItemClick,
            modifier = modifier,
            header = { ChannelHeader(details) },
        )
    }
}

@Composable
private fun ChannelHeader(details: ChannelDetails) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        if (!details.bannerUrl.isNullOrBlank()) {
            AsyncImage(
                model = details.bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(YtChip),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(YtChip)
            ) {
                AsyncImage(
                    model = details.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = details.name ?: "Channel",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subs = formatSubs(details.subscriberCount)
                if (subs.isNotBlank()) {
                    Text(
                        text = subs,
                        style = MaterialTheme.typography.bodyMedium,
                        color = YtTextSecondary,
                    )
                }
            }
        }
        val description = details.description.orEmpty()
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = YtTextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
