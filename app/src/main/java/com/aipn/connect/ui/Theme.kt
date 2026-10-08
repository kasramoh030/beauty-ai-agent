package com.aipn.connect.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aipn.connect.R
import com.aipn.connect.ThemeChoice

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

/**
 * True when the app is painting its dark canvas. Several accents below are
 * hand-picked for that canvas and read as mud on the light one, so they ask this
 * rather than hard-coding a branch at every call site.
 */
@Composable
fun isDarkCanvas(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Fill for "this row is the active one" in the drawer and the model picker. */
@Composable
fun selectedFill(): Color =
    if (isDarkCanvas()) SelectedTeal else Color(0xFFDCEFE6)

/** Fill behind the green "free" badge; its text colour flips with it. */
@Composable
fun freeBadgeFill(): Color =
    if (isDarkCanvas()) SelectedTeal else FreeGreen.copy(alpha = 0.18f)

/** Text colour that stays legible on [freeBadgeFill]. */
@Composable
fun freeBadgeContent(): Color =
    if (isDarkCanvas()) FreeGreen else Color(0xFF0F6B4C)

/** An opaque tint of the accent over the surface — bubbles must not be see-through. */
@Composable
fun accentTint(fraction: Float): Color = Color(
    red = lerpColorValue(MaterialTheme.colorScheme.surface.red, MaterialTheme.colorScheme.primary.red, fraction),
    green = lerpColorValue(MaterialTheme.colorScheme.surface.green, MaterialTheme.colorScheme.primary.green, fraction),
    blue = lerpColorValue(MaterialTheme.colorScheme.surface.blue, MaterialTheme.colorScheme.primary.blue, fraction),
    alpha = 1f,
)

private fun lerpColorValue(from: Float, to: Float, fraction: Float): Float =
    from + (to - from) * fraction

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

/*
 * Palettes. Every one of them sets the full set of roles the UI reads, so no
 * component ever falls back to a stock M3 purple, and every one of them gives
 * the dark schemes a light onSurface/onBackground — that is what keeps text and
 * icons legible once the tree is wrapped in a Surface.
 */

/** Deep navy night: the amber accent reads warm against a cold sky. */
private val MidnightColors = darkColorScheme(
    primary = Color(0xFF8FB8FF),
    onPrimary = Color(0xFF06172E),
    primaryContainer = Color(0xFF1E3A63),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF7FD8C4),
    onSecondary = Color(0xFF04302A),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFEFF4FB),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFEFF4FB),
    surfaceVariant = Color(0xFF1A2436),
    onSurfaceVariant = Color(0xFFA3B1C6),
    outline = Color(0xFF33415A),
    outlineVariant = Color(0xFF243149),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** True black for OLED panels, with a hard white line for every piece of text. */
private val OledColors = darkColorScheme(
    primary = Color(0xFFF5C77E),
    onPrimary = Color(0xFF1A1206),
    primaryContainer = Color(0xFF3A2A0D),
    onPrimaryContainer = Color(0xFFFFE3B0),
    secondary = Color(0xFF6FE3B0),
    onSecondary = Color(0xFF00281A),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFB4B4B4),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFFF9A8F),
    onError = Color(0xFF3B0906),
)

/** Warm paper: cream surfaces with the amber accent left to do the talking. */
private val SandColors = lightColorScheme(
    primary = Color(0xFFA9681A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE6C2),
    onPrimaryContainer = Color(0xFF4A2C05),
    secondary = Color(0xFF2F6B4F),
    onSecondary = Color.White,
    background = Color(0xFFFCF7EF),
    onBackground = Color(0xFF1E1A14),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF1E1A14),
    surfaceVariant = Color(0xFFF3EADB),
    onSurfaceVariant = Color(0xFF6B6154),
    outline = Color(0xFFD5C9B6),
    outlineVariant = Color(0xFFE9E0D1),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

/** Cool sky: a pale blue-white canvas, the light counterpart to Midnight. */
private val OceanColors = lightColorScheme(
    primary = Color(0xFF17607F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9E7F5),
    onPrimaryContainer = Color(0xFF06303F),
    secondary = Color(0xFF166B5A),
    onSecondary = Color.White,
    background = Color(0xFFF3F8FB),
    onBackground = Color(0xFF12191C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12191C),
    surfaceVariant = Color(0xFFE7F0F5),
    onSurfaceVariant = Color(0xFF51646D),
    outline = Color(0xFFC2D3DB),
    outlineVariant = Color(0xFFDEE9EE),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

/** Every palette, keyed by the choice stored in preferences. */
fun paletteFor(choice: ThemeChoice, systemDark: Boolean): ColorScheme = when (choice) {
    ThemeChoice.SYSTEM -> if (systemDark) DarkColors else LightColors
    ThemeChoice.LIGHT -> LightColors
    ThemeChoice.DARK -> DarkColors
    ThemeChoice.MIDNIGHT -> MidnightColors
    ThemeChoice.OLED -> OledColors
    ThemeChoice.SAND -> SandColors
    ThemeChoice.OCEAN -> OceanColors
}

/** Whether [choice] resolves to a dark canvas, whatever the system is doing. */
fun isDarkTheme(choice: ThemeChoice, systemDark: Boolean): Boolean =
    paletteFor(choice, systemDark).background.luminance() < 0.5f

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
    choice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val scheme = paletteFor(choice, isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
    ) {
        // A Surface is not optional decoration here: it is what supplies
        // LocalContentColor. Without it every Text and Icon that does not name a
        // colour falls back to Color.Black, which is invisible on the dark
        // palettes and made the settings page black-on-black in the light one.
        Surface(
            color = scheme.background,
            contentColor = scheme.onBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
        }
    }
}