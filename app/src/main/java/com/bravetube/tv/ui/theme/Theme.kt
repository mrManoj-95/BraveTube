package com.bravetube.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// YouTube-dark palette
val YtBackground = Color(0xFF0F0F0F)
val YtSurface = Color(0xFF1F1F1F)
val YtSurfaceHigh = Color(0xFF2A2A2A)
val YtRed = Color(0xFFFF0033)
val YtTextPrimary = Color(0xFFF1F1F1)
val YtTextSecondary = Color(0xFFAAAAAA)
val YtChip = Color(0xFF272727)
val YtFocus = Color(0xFFFFFFFF)

private val Scheme = darkColorScheme(
    primary = YtRed,
    onPrimary = Color.White,
    secondary = YtSurfaceHigh,
    onSecondary = YtTextPrimary,
    background = YtBackground,
    onBackground = YtTextPrimary,
    surface = YtSurface,
    onSurface = YtTextPrimary,
    surfaceVariant = YtChip,
    onSurfaceVariant = YtTextSecondary,
    outline = Color(0xFF3F3F3F),
)

/** Slightly larger than phone defaults — this is read from across a room. */
private val TvTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun BraveTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = TvTypography,
        content = content,
    )
}
