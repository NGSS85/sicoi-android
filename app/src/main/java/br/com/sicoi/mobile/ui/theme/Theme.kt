package br.com.sicoi.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SicoiColorScheme = darkColorScheme(
    primary           = SicoiOrange,
    onPrimary         = SicoiBackground,
    primaryContainer  = SicoiOrangeDark,
    secondary         = SicoiBlue,
    onSecondary       = SicoiTextPrimary,
    background        = SicoiBackground,
    onBackground      = SicoiTextPrimary,
    surface           = SicoiSurface,
    onSurface         = SicoiTextPrimary,
    surfaceVariant    = SicoiCard,
    onSurfaceVariant  = SicoiTextSecondary,
    outline           = SicoiCardBorder,
    error             = SicoiError,
    onError           = SicoiTextPrimary,
)

@Composable
fun SicoiMobileTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SicoiBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SicoiColorScheme,
        typography   = SicoiTypography,
        content      = content
    )
}
