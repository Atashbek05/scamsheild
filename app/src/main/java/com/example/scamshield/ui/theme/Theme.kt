package com.example.scamshield.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary           = CyberCyan,
    onPrimary         = CyberBgDeep,
    primaryContainer  = CyberBgCardHigh,
    secondary         = CyberAccent,
    onSecondary       = CyberBgDeep,
    tertiary          = CyberAmber,
    background        = CyberBgDeep,
    onBackground      = CyberTextPrimary,
    surface           = CyberBgSurface,
    onSurface         = CyberTextPrimary,
    surfaceVariant    = CyberBgCard,
    onSurfaceVariant  = CyberTextSecondary,
    surfaceContainer  = CyberBgCardHigh,
    error             = CyberRed,
    onError           = CyberTextPrimary,
    outline           = CyberBorder,
    outlineVariant    = CyberBorderSubtle,
)

private val LightSchemeFallback = lightColorScheme()
private val DarkSchemeFallback  = darkColorScheme()

/**
 * @param mode 0 = cyber dark (default), 1 = system, 2 = strict material dark
 */
@Composable
fun ScamShieldTheme(
    mode: Int = 0,
    content: @Composable () -> Unit,
) {
    val scheme = when (mode) {
        1    -> if (isSystemInDarkTheme()) DarkSchemeFallback else LightSchemeFallback
        2    -> DarkSchemeFallback
        else -> CyberColorScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography  = Typography,
        content     = content,
    )
}
