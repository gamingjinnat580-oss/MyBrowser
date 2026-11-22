package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val OrbitDarkColorScheme = darkColorScheme(
    primary = OrbitCyan,
    onPrimary = OrbitVoid,
    primaryContainer = OrbitSurfaceElevated,
    onPrimaryContainer = OrbitCyan,
    secondary = OrbitBlue,
    onSecondary = Color.White,
    secondaryContainer = OrbitSurfaceVariant,
    onSecondaryContainer = OrbitTextPrimary,
    tertiary = OrbitViolet,
    onTertiary = Color.White,
    background = OrbitVoid,
    onBackground = OrbitTextPrimary,
    surface = OrbitSurface,
    onSurface = OrbitTextPrimary,
    surfaceVariant = OrbitSurfaceVariant,
    onSurfaceVariant = OrbitTextSecondary,
    outline = OrbitOutline,
    error = OrbitRose,
    onError = Color.White
)

private val OrbitLightColorScheme = lightColorScheme(
    primary = OrbitLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE9F5),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = OrbitLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = OrbitLightSurfaceVariant,
    onSecondaryContainer = OrbitLightText,
    tertiary = OrbitViolet,
    onTertiary = Color.White,
    background = OrbitLightBg,
    onBackground = OrbitLightText,
    surface = OrbitLightSurface,
    onSurface = OrbitLightText,
    surfaceVariant = OrbitLightSurfaceVariant,
    onSurfaceVariant = OrbitLightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = OrbitRose,
    onError = Color.White
)

val OrbitShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun OrbitBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OrbitDarkColorScheme else OrbitLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OrbitTypography,
        shapes = OrbitShapes,
        content = content
    )
}
