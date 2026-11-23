package com.mechanicar.gastos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

private val Steel = Color(0xFF1B4F72)
private val SteelDark = Color(0xFF0E2F45)
private val Copper = Color(0xFFC46B2F)
private val Sand = Color(0xFFF7F4EF)
private val Ink = Color(0xFF1C1B1A)
private val Mist = Color(0xFFE4EBF2)

private val LightColors = lightColorScheme(
    primary = Steel,
    onPrimary = Color.White,
    primaryContainer = Mist,
    onPrimaryContainer = SteelDark,
    secondary = Copper,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3D9C4),
    onSecondaryContainer = Color(0xFF4A2508),
    background = Sand,
    onBackground = Ink,
    surface = Color(0xFFFFFCF8),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEF3),
    onSurfaceVariant = Color(0xFF4A5560),
    outline = Color(0xFF8A96A1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8EB6D4),
    onPrimary = SteelDark,
    primaryContainer = Steel,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE0A06C),
    onSecondary = Color(0xFF3A1C05),
    background = Color(0xFF12171C),
    onBackground = Color(0xFFE8EEF3),
    surface = Color(0xFF1A2128),
    onSurface = Color(0xFFE8EEF3),
    surfaceVariant = Color(0xFF2A343E),
    onSurfaceVariant = Color(0xFFB7C2CC)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
)

@Composable
fun GastosCorsaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
