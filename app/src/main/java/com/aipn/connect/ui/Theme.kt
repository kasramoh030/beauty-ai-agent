package com.aipn.connect.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aipn.connect.R

/**
 * Palette lifted from the reference design: a near-black green tinted canvas, a
 * translucent panel colour, and amber as the single accent with green used only
 * for "free" badges and confirmations.
 */
val Amber = Color(0xFFE9A23B)
val AmberSoft = Color(0xFFF3C489)
val Teal = Color(0xFF12333D)
val FreeGreen = Color(0xFF34D399)
val SelectedTeal = Color(0xFF0E3129)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1206),
    primaryContainer = Amber,
    onPrimaryContainer = Color(0xFF1A1206),
    secondary = FreeGreen,
    onSecondary = Color(0xFF05261C),
    background = Color(0xFF0A100E),
    onBackground = Color(0xFFF3F6F4),
    surface = Color(0xFF0E1512),
    onSurface = Color(0xFFF3F6F4),
    surfaceVariant = Color(0xFF141D19),
    onSurfaceVariant = Color(0xFF8A9691),
    outline = Color(0xFF2A3532),
    outlineVariant = Color(0xFF1E2724),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0A0A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB9762A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEFD9),
    onPrimaryContainer = Color(0xFF5C3A0C),
    secondary = Color(0xFF12805C),
    onSecondary = Color.White,
    background = Color(0xFFF7F8F7),
    onBackground = Color(0xFF111614),
    surface = Color.White,
    onSurface = Color(0xFF111614),
    surfaceVariant = Color(0xFFF1F3F2),
    onSurfaceVariant = Color(0xFF5C6663),
    outline = Color(0xFFD5DBD9),
    outlineVariant = Color(0xFFE7EBEA),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

/** Vazirmatn - the geometric Persian face used by the reference design. */
val Vazir = FontFamily(
    Font(R.font.vazirmatnregular, FontWeight.Normal),
    Font(R.font.vazirmatnmedium, FontWeight.Medium),
    Font(R.font.vazirmatnsemibold, FontWeight.SemiBold),
    Font(R.font.vazirmatnbold, FontWeight.Bold),
    Font(R.font.vazirmatnblack, FontWeight.Black),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 42.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 38.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Vazir,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
    ),
)

/** Monospace style used for code blocks rendered from the model's answer. */
val CodeTextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp)

@Composable
fun ApnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}