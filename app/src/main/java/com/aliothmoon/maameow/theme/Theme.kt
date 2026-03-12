package com.aliothmoon.maameow.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val MaaColorScheme = lightColorScheme(

    /* ---------- Primary ---------- */
    primary = Color(0xFF1F5FBF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE7FF),
    onPrimaryContainer = Color(0xFF0B1D3A),

    /* ---------- Secondary ---------- */
    secondary = Color(0xFF5F6F8A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E7EE),
    onSecondaryContainer = Color(0xFF1A2433),

    /* ---------- Tertiary ---------- */
    tertiary = Color(0xFF3A7D6C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9EFE9),
    onTertiaryContainer = Color(0xFF0E2A24),

    /* ---------- Error ---------- */
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    /* ---------- Background / Surface ---------- */
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1C1F24),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1F24),

    surfaceVariant = Color(0xFFEDEFF2),
    onSurfaceVariant = Color(0xFF5F6368),

    /* ---------- Outline & Misc ---------- */
    outline = Color(0xFFD0D5DD),
    outlineVariant = Color(0xFFE4E7EC),

    scrim = Color(0xFF000000)
)

val MaaShapes = Shapes(
    extraLarge = RoundedCornerShape(8.dp)
)

object MaaThemeAlphas {
    const val Disabled = 0.38f
    const val Secondary = 0.60f
    const val Medium = 0.74f
}

@Composable
fun MaaMeowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaaColorScheme,
        typography = Typography,
        shapes = MaaShapes,
        content = content
    )
}
