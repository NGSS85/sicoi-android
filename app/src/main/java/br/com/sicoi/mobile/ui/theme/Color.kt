package br.com.sicoi.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeController = compositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}

interface ThemeController {
    val isDarkTheme: Boolean
    fun toggleTheme()
}

// Paleta SICOI — Cores base
val SicoiOrange         = Color(0xFFF97316)  // Laranja primário (accent)
val SicoiOrangeLight    = Color(0xFFFB923C)
val SicoiOrangeDark     = Color(0xFFEA6A00)

val SicoiBlue           = Color(0xFF3B82F6)  // Azul industrial
val SicoiBlueDark       = Color(0xFF1D4ED8)
val SicoiBlueLight      = Color(0xFF60A5FA)

val SicoiSuccess        = Color(0xFF10B981)  // Verde sucesso
val SicoiWarning        = Color(0xFFF59E0B)  // Amarelo aviso
val SicoiError          = Color(0xFFEF4444)  // Vermelho erro
val SicoiEmergency      = Color(0xFFDC2626)  // Vermelho emergência

// Tema Escuro
val SicoiBackgroundDark    = Color(0xFF020817)
val SicoiSurfaceDark       = Color(0xFF0F172A)
val SicoiCardDark          = Color(0xFF1E293B)
val SicoiCardBorderDark    = Color(0xFF334155)
val SicoiTextPrimaryDark   = Color(0xFFF8FAFC)
val SicoiTextSecondaryDark = Color(0xFF94A3B8)
val SicoiTextMutedDark     = Color(0xFF475569)
val SicoiDividerDark       = Color(0xFF1E293B)

// Tema Claro (cores modernas e profissionais com bom contraste)
val SicoiBackgroundLight    = Color(0xFFF8FAFC)
val SicoiSurfaceLight       = Color(0xFFFFFFFF)
val SicoiCardLight          = Color(0xFFF1F5F9)
val SicoiCardBorderLight    = Color(0xFFE2E8F0)
val SicoiTextPrimaryLight   = Color(0xFF0F172A)
val SicoiTextSecondaryLight = Color(0xFF475569)
val SicoiTextMutedLight     = Color(0xFF64748B)
val SicoiDividerLight       = Color(0xFFE2E8F0)

// Acessores Dinâmicos
val SicoiBackground: Color     @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiBackgroundDark else SicoiBackgroundLight
val SicoiSurface: Color        @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiSurfaceDark else SicoiSurfaceLight
val SicoiCard: Color           @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiCardDark else SicoiCardLight
val SicoiCardBorder: Color     @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiCardBorderDark else SicoiCardBorderLight
val SicoiTextPrimary: Color    @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiTextPrimaryDark else SicoiTextPrimaryLight
val SicoiTextSecondary: Color  @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiTextSecondaryDark else SicoiTextSecondaryLight
val SicoiTextMuted: Color      @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiTextMutedDark else SicoiTextMutedLight
val SicoiDivider: Color        @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiDividerDark else SicoiDividerLight

// Bordas com contraste melhorado para modo claro
val SicoiOrangeBorder: Color   @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiOrange.copy(alpha = 0.4f) else SicoiOrangeDark.copy(alpha = 0.85f)
val SicoiWarningBorder: Color  @Composable get() = if (LocalThemeController.current.isDarkTheme) SicoiWarning.copy(alpha = 0.4f) else SicoiWarning.copy(alpha = 0.85f)

