package com.bravetube.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bravetube.tv.AppGraph
import com.bravetube.tv.data.StreamItem
import com.bravetube.tv.ui.components.FocusCard
import com.bravetube.tv.ui.components.LoadingBlock
import com.bravetube.tv.ui.components.MessageBlock
import com.bravetube.tv.ui.components.VideoGrid
import com.bravetube.tv.ui.theme.YtChip
import com.bravetube.tv.ui.theme.YtSurface
import com.bravetube.tv.ui.theme.YtTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<StreamItem>>(emptyList())
        private set
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var filter by mutableStateOf("all")
        private set

    private var debounce: Job? = null

    fun append(text: String) = setQuery(query + text)

    fun backspace() = setQuery(query.dropLast(1))

    fun clear() = setQuery("")

    fun setFilter(f: String) {
        if (filter == f) return
        filter = f
        runSearch()
    }

    fun setQuery(q: String) {
        query = q
        debounce?.cancel()
        if (q.isBlank()) {
            results = emptyList()
            suggestions = emptyList()
            loading = false
            error = null
            return
        }
        debounce = viewModelScope.launch {
            delay(450)
            loadSuggestions()
            runSearch()
        }
    }

    fun submit(q: String = query) {
        debounce?.cancel()
        query = q
        AppGraph.prefs.addRecentSearch(q)
        runSearch()
    }

    private suspend fun loadSuggestions() {
        suggestions = try {
            AppGraph.repo.suggestions(query).take(8)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun runSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val res = AppGraph.repo.search(q, filter)
                results = res.items.filter { it.displayTitle.isNotBlank() }
                error = if (results.isEmpty()) "No results for \"$q\"." else null
            } catch (e: Exception) {
                results = emptyList()
                error = e.message ?: "Search failed."
            } finally {
                loading = false
            }
        }
    }
}

private val KEY_ROWS = listOf(
    "ABCDEFG",
    "HIJKLMN",
    "OPQRSTU",
    "VWXYZ'-",
    "0123456",
    "789 .&?",
)

private val FILTERS = listOf(
    "all" to "All",
    "videos" to "Videos",
    "channels" to "Channels",
    "playlists" to "Playlists",
    "music_songs" to "Music",
)

@Composable
fun SearchScreen(
    onItemClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    vm: SearchViewModel = viewModel(),
) {
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(6) {
            if (runCatching { firstKey.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(100)
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // ---------------------------------------------------------- keyboard
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 20.dp, top = 26.dp, bottom = 20.dp),
        ) {
            QueryBox(vm.query)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KEY_ROWS.forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { colIndex, ch ->
                            Key(
                                label = if (ch == ' ') "␣" else ch.toString(),
                                onClick = { vm.append(ch.toString()) },
                                modifier = if (rowIndex == 0 && colIndex == 0) {
                                    Modifier.focusRequester(firstKey)
                                } else Modifier,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Key("SPACE", onClick = { vm.append(" ") }, widthDp = 122)
                    Key("DEL", onClick = { vm.backspace() }, widthDp = 100)
                    Key("CLEAR", onClick = { vm.clear() }, widthDp = 122)
                }
            }

            if (vm.suggestions.isNotEmpty()) {
                Spacer16()
                Text(
                    "Suggestions",
                    style = MaterialTheme.typography.labelLarge,
                    color = YtTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                vm.suggestions.take(4).forEach { s ->
                    SuggestionRow(s) { vm.submit(s) }
                }
            }
        }

        // ----------------------------------------------------------- results
        Column(modifier = Modifier.fillMaxSize().padding(top = 26.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 8.dp, end = 42.dp, bottom = 14.dp),
            ) {
                items(FILTERS) { (key, label) ->
                    FilterChip(
                        label = label,
                        selected = vm.filter == key,
                        onClick = { vm.setFilter(key) },
                    )
                }
            }

            when {
                vm.query.isBlank() -> MessageBlock(
                    title = "Search YouTube",
                    body = "Use the on-screen keyboard, or press and hold the mic button on your remote " +
                        "if your TV supports voice input.",
                    modifier = Modifier.padding(top = 100.dp),
                )

                vm.loading && vm.results.isEmpty() -> LoadingBlock(height = 320)

                vm.results.isEmpty() -> MessageBlock(
                    title = vm.error ?: "No results",
                    modifier = Modifier.padding(top = 100.dp),
                )

                else -> VideoGrid(
                    items = vm.results,
                    onItemClick = onItemClick,
                    columnMinWidth = 280,
                    contentPadding = PaddingValues(start = 8.dp, end = 42.dp, bottom = 40.dp),
                )
            }
        }
    }
}

@Composable
private fun QueryBox(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(YtSurface)
            .border(1.dp, Color(0xFF3F3F3F), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = query.ifBlank { "Search" },
            style = MaterialTheme.typography.titleMedium,
            color = if (query.isBlank()) YtTextSecondary else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer16()
}

@Composable
private fun Key(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthDp: Int = 48,
) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        focusScale = 1.12f,
        borderWidthDp = 0,
        modifier = modifier.width(widthDp.dp),
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(if (focused) Color.White else YtChip),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (focused) Color.Black else Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SuggestionRow(text: String, onClick: () -> Unit) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        focusScale = 1.0f,
        borderWidthDp = 0,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) Color.White else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (focused) Color.Black else YtTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        focusScale = 1.05f,
        borderWidthDp = 0,
        modifier = Modifier,
    ) { focused ->
        Box(
            modifier = Modifier
                .background(
                    when {
                        focused -> Color.White
                        selected -> Color(0xFFF1F1F1)
                        else -> YtChip
                    }
                )
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (focused || selected) Color.Black else Color.White,
            )
        }
    }
}

@Composable
private fun Spacer16() =
    Box(Modifier.size(16.dp))
