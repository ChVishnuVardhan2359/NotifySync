package com.notifysync.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blue500,
    onPrimary = Color_White,
    secondary = Teal400,
    tertiary = Green500,
    background = Slate100,
)

private val DarkColors = darkColorScheme(
    primary = Blue500,
    secondary = Teal400,
    tertiary = Green500,
    background = Slate900,
    surface = Slate800,
)

@Composable
fun NotifySyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NotifyTypography,
        content = content,
    )
}
