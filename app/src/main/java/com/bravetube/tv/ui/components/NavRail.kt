package com.bravetube.tv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bravetube.tv.ui.theme.YtTextSecondary

enum class NavDest(val label: String, val icon: ImageVector) {
    Search("Search", Icons.Filled.Search),
    Home("Home", Icons.Filled.Home),
    Trending("Trending", Icons.Filled.Whatshot),
    History("History", Icons.Filled.History),
    Settings("Settings", Icons.Filled.Settings),
}

private const val RAIL_COLLAPSED = 78
private const val RAIL_EXPANDED = 224

@Composable
fun NavRail(
    current: NavDest,
    onSelect: (NavDest) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = (if (expanded) RAIL_EXPANDED else RAIL_COLLAPSED).dp,
        label = "railWidth",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF171717), Color(0xFF101010), Color(0x00000000))
                )
            )
            .onFocusChanged { expanded = it.hasFocus }
            .focusGroup()
            .padding(vertical = 24.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        NavDest.entries.forEach { dest ->
            RailItem(
                dest = dest,
                selected = dest == current,
                expanded = expanded,
                onClick = { onSelect(dest) },
            )
        }
    }
}

@Composable
private fun RailItem(
    dest: NavDest,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    FocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        focusScale = 1.0f,
        borderWidthDp = 0,
        modifier = Modifier.fillMaxWidth(),
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        focused -> Color.White
                        selected -> Color(0xFF303030)
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = when {
                focused -> Color.Black
                selected -> Color.White
                else -> YtTextSecondary
            }
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label,
                    tint = tint,
                    modifier = Modifier.size(26.dp),
                )
            }
            if (expanded) {
                HSpace(14)
                Text(
                    text = dest.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
