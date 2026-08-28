package com.bravetube.tv.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bravetube.tv.AppGraph
import com.bravetube.tv.data.Shelf
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.ui.components.MessageBlock
import com.bravetube.tv.ui.components.ShelfRow
import com.bravetube.tv.ui.theme.YtRed
import com.bravetube.tv.ui.theme.YtTextSecondary
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class HomeViewModel : ViewModel() {

    var shelves by mutableStateOf<List<Shelf>>(emptyList())
        private set
    var fatalError by mutableStateOf<String?>(null)
        private set

    private var loadedForRegion: String? = null

    fun loadIfNeeded(force: Boolean = false) {
        val repo = AppGraph.repo
        if (!force && loadedForRegion == repo.prefs.region && shelves.isNotEmpty()) return
        loadedForRegion = repo.prefs.region
        fatalError = null

        val defs = repo.homeShelves()
        shelves = defs.map { Shelf(id = it.id, title = it.title, loading = true) }

        // Keep concurrency low — public instances rate-limit aggressively.
        val gate = Semaphore(3)
        defs.forEach { def ->
            viewModelScope.launch {
                gate.withPermit {
                    try {
                        val items = repo.loadShelf(def)
                        update(def.id) { it.copy(items = items, loading = false, error = null) }
                    } catch (e: Exception) {
                        update(def.id) {
                            it.copy(loading = false, error = e.message ?: "Couldn't load this row.")
                        }
                    }
                }
            }
        }
    }

    private fun update(id: String, transform: (Shelf) -> Shelf) {
        shelves = shelves.map { if (it.id == id) transform(it) else it }
    }
}

@Composable
fun HomeScreen(
    onItemClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.loadIfNeeded() }

    val shelves = vm.shelves
    val allFailed = shelves.isNotEmpty() && shelves.all { !it.loading && it.error != null }

    if (allFailed) {
        MessageBlock(
            title = "Can't reach any Piped instance",
            body = shelves.firstOrNull()?.error
                ?: "Check the TV's network, or choose a different instance in Settings.",
            actionLabel = "Retry",
            onAction = { vm.loadIfNeeded(force = true) },
            modifier = modifier.fillMaxSize().padding(top = 160.dp),
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 22.dp, bottom = 48.dp),
    ) {
        item { HomeHeader() }
        items(shelves) { shelf ->
            ShelfRow(shelf = shelf, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Brave",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = "Tube",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = YtRed,
                )
            }
            Text(
                text = "No ads, no sign-in, no tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = YtTextSecondary,
            )
        }
    }
}
