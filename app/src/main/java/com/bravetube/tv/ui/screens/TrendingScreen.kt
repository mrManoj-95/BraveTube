package com.bravetube.tv.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bravetube.tv.AppGraph
import com.bravetube.tv.data.Prefs
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.ui.components.FullScreenLoader
import com.bravetube.tv.ui.components.MessageBlock
import com.bravetube.tv.ui.components.VideoGrid
import kotlinx.coroutines.launch

class TrendingViewModel : ViewModel() {
    var items by mutableStateOf<List<StreamItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var loadedRegion: String? = null

    fun load(force: Boolean = false) {
        val region = AppGraph.prefs.region
        if (!force && loadedRegion == region && items.isNotEmpty()) return
        loadedRegion = region
        loading = true
        error = null
        viewModelScope.launch {
            try {
                items = AppGraph.api.trending(region).filter { it.isVideo }
                if (items.isEmpty()) error = "Trending is empty on this instance."
            } catch (e: Exception) {
                items = emptyList()
                error = e.message ?: "Couldn't load trending."
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun TrendingScreen(
    onItemClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    vm: TrendingViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }

    val regionName = Prefs.REGIONS.firstOrNull { it.first == AppGraph.prefs.region }?.second
        ?: AppGraph.prefs.region

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Trending in $regionName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 48.dp, top = 28.dp, bottom = 16.dp),
        )
        when {
            vm.loading && vm.items.isEmpty() -> FullScreenLoader("Loading trending…")
            vm.items.isEmpty() -> MessageBlock(
                title = vm.error ?: "Nothing to show",
                actionLabel = "Retry",
                onAction = { vm.load(force = true) },
                modifier = Modifier.padding(top = 80.dp),
            )
            else -> VideoGrid(items = vm.items, onItemClick = onItemClick)
        }
    }
}
