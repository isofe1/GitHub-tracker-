package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GitHubBlueLight,
    onPrimary = Color.Black,
    primaryContainer = GitHubBlue,
    onPrimaryContainer = Color.White,
    secondary = GitHubGreenLight,
    onSecondary = Color.Black,
    secondaryContainer = GitHubGreen,
    onSecondaryContainer = Color.White,
    tertiary = GitHubPurple,
    background = GitHubDarkBg,
    onBackground = Color(0xFFC9D1D9),
    surface = GitHubDarkSurface,
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = GitHubDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF8B949E),
    outline = GitHubDarkBorder,
    error = GitHubRed
)

private val LightColorScheme = lightColorScheme(
    primary = GitHubBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF4FF),
    onPrimaryContainer = Color(0xFF0969DA),
    secondary = GitHubGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAFBE1),
    onSecondaryContainer = Color(0xFF1A7F37),
    tertiary = GitHubPurple,
    background = GitHubLightBg,
    onBackground = Color(0xFF1F2328),
    surface = GitHubLightSurface,
    onSurface = Color(0xFF1F2328),
    surfaceVariant = GitHubLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF656D76),
    outline = GitHubLightBorder,
    error = GitHubRed
)

@Composable
fun GitHubDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to keep rich GitHub brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
