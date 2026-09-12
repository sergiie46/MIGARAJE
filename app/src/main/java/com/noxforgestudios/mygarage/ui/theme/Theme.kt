package com.noxforgestudios.mygarage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.noxforgestudios.mygarage.domain.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4E9BFF),
    secondary = Color(0xFF66D8FF),
    background = Color(0xFF080808),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF242424),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerLow = Color(0xFF101010)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0066CC),
    secondary = Color(0xFF006A85),
    background = Color(0xFFF6F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EFF8)
)

@Composable
fun MiGarajeTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography(), content = content)
}
