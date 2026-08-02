package br.com.sicoi.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SicoiMobileTheme(content: @Composable () -> Unit) {
    val isDark = LocalThemeController.current.isDarkTheme

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary           = SicoiOrange,
            onPrimary         = SicoiBackgroundDark,
            primaryContainer  = SicoiOrangeDark,
            secondary         = SicoiBlue,
            onSecondary       = SicoiTextPrimaryDark,
            background        = SicoiBackgroundDark,
            onBackground      = SicoiTextPrimaryDark,
            surface           = SicoiSurfaceDark,
            onSurface         = SicoiTextPrimaryDark,
            surfaceVariant    = SicoiCardDark,
            onSurfaceVariant  = SicoiTextSecondaryDark,
            outline           = SicoiCardBorderDark,
            error             = SicoiError,
            onError           = SicoiTextPrimaryDark,
        )
    } else {
        lightColorScheme(
            primary           = SicoiOrange,
            onPrimary         = SicoiBackgroundLight,
            primaryContainer  = SicoiOrangeDark,
            secondary         = SicoiBlue,
            onSecondary       = SicoiTextPrimaryLight,
            background        = SicoiBackgroundLight,
            onBackground      = SicoiTextPrimaryLight,
            surface           = SicoiSurfaceLight,
            onSurface         = SicoiTextPrimaryLight,
            surfaceVariant    = SicoiCardLight,
            onSurfaceVariant  = SicoiTextSecondaryLight,
            outline           = SicoiCardBorderLight,
            error             = SicoiError,
            onError           = SicoiTextPrimaryLight,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = SicoiTypography,
        content      = content
    )
}

