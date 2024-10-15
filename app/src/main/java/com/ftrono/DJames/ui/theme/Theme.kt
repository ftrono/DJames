package com.ftrono.DJames.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView


//Color scheme:
private val DarkColorScheme = darkColorScheme(
    primary = colorPrimary,
    secondary = colorPrimary,
    tertiary = colorPrimary,
    background = windowBackground,
    surface = windowBackground,
    onPrimary = windowBackground,
    onSecondary = windowBackground,
    onTertiary = windowBackground,
    onBackground = windowBackground,
    onSurface = windowBackground
)

//THEME:
@Composable
fun DJamesTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {

        SideEffect {
            val window = (view.context as Activity).window
            //SYSTEM BARS COLORS:
            window.statusBarColor = windowBackground.toArgb()
            window.navigationBarColor = windowBackground.toArgb()
        }

        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun ClockTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {

        SideEffect {
            val window = (view.context as Activity).window
            //SYSTEM BARS COLORS:
            window.statusBarColor = black.toArgb()
            window.navigationBarColor = black.toArgb()
        }

        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

