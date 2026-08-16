package com.sprachbruecke.translator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary         = CloudModeColor,
    secondary       = OnDeviceModeColor,
    error           = ErrorModeColor,
    surface         = androidx.compose.ui.graphics.Color.White,
    onSurface       = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surfaceVariant  = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
)

private val DarkColorScheme = darkColorScheme(
    primary         = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    secondary       = OnDeviceModeColor,
    error           = ErrorModeColor,
)

@Composable
fun SprachBrueckeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Deaktiviert für konsistentes Design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparente Statusleiste für Edge-to-Edge
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
