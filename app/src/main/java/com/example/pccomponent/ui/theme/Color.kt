package com.example.pccomponent.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object AppColors {
    val Background = Color(0xFF0D1117)
    val Panel = Color(0xFF161B22)
    val PanelRaised = Color(0xFF202938)
    val Accent = Color(0xFFB6A9CF)
    val Muted = Color(0xFF8B98A9)
    val Compatible = Accent
    val Warning = Color(0xFFD8B486)
    val Incompatible = Color(0xFFD49CA5)
    val AccentSoft = Color(0xFFD0C5E2)

    val Scheme = darkColorScheme(
        primary = Accent,
        onPrimary = Background,
        background = Background,
        onBackground = Color(0xFFE6EDF3),
        surface = Panel,
        onSurface = Color(0xFFE6EDF3),
        surfaceVariant = PanelRaised,
        onSurfaceVariant = Muted,
        error = Incompatible,
        onError = Background
    )
}
