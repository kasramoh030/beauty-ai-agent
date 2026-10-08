package com.aipn.connect.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/*
 * Depth kit — a small set of drawing primitives that make flat Compose surfaces
 * read as physical objects. Everything here uses only Compose's own graphics API,
 * so the app gains a dimensional look without pulling in a 3D or blur dependency.
 *
 * Three ingredients, used together:
 *   1. a drop shadow that falls away from a key light at the top-left,
 *   2. a bright rim on the top-left edge and a dark rim on the bottom-right,
 *   3. a slow specular sheen sweeping across the face.
 */

/** Per-theme depth values; light mode needs a softer, lower-contrast version. */
data class Depth(
    val elevation: Dp,
    val rimWidth: Dp,
    val rimLight: Color,
    val rimDark: Color,
    val shadow: Color,
    val shadowAlpha: Float,
    val glow: Color,
    val glowStrength: Float,
    val ambient: Color,
    val ambientTint: Color,
    val coolTint: Color,
    val sheenColor: Color,
)

private val DarkDepth = Depth(
    elevation = 16.dp,
    rimWidth = 1.5.dp,
    rimLight = Color(0x33FFFFFF),
    rimDark = Color(0x99000000),
    shadow = Color.Black,
    shadowAlpha = 0.55f,
    glowStrength = 1f,
    ambientFallback = Color(0xFF0A100E),
    ambientTint = Color(0x1FE9A23B),
    coolTint = Color(0x331B6E7A),
    sheenColor = Color(0x1FFFFFFF),
)

private val LightDepth = Depth(
    elevation = 10.dp,
    rimWidth = 1.dp,
    rimLight = Color(0xFFFFFFFF),
    rimDark = Color(0x24000000),
    shadow = Color(0xFF5A6863),
    shadowAlpha = 0.30f,
    glowStrength = 0.5f,
    ambientFallback = Color(0xFFF7F8F7),
    ambientTint = Color(0x14D08A22),
    coolTint = Color(0x141B6E7A),
    sheenColor = Color(0x33FFFFFF),
)

/**
 * Depth values for the theme the app is actually showing, not the system setting —
 * the user can pick light or dark by hand, and the depth must follow that choice.
 * The glow takes its colour from the palette's accent so it stays in key with
 * whichever theme is on screen.
 */
@Composable
fun rememberDepth(): Depth {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return remember(dark, scheme) {
        val base = if (dark) DarkDepth else LightDepth
        base.copy(
            glow = scheme.primary,
            ambient = scheme.background,
            ambientTint = scheme.primary.copy(alpha = if (dark) 0.10f else 0.08f),
            coolTint = scheme.secondary.copy(alpha = if (dark) 0.16f else 0.10f),
        )
    }
}

/**
 * Lifts a shape off the surface: a soft drop shadow offset away from the key light
 * at the top-left, plus an optional warm glow spilling onto the surface beneath.
 *
 * The shadow is drawn as a stack of widening, fading strokes rather than through
 * RenderNode elevation. Hardware elevation shadows are clipped to the layer's
 * rectangle on some GPUs and came back as bright blocks inside rounded surfaces;
 * drawing it by hand keeps the falloff, the direction and the colour predictable,
 * and the shape's own fill covers the inner half of the stroke.
 */
fun Modifier.raised(
    depth: Depth,
    shape: Shape,
    elevation: Dp = depth.elevation,
    glow: Float = 0f,
): Modifier = drawWithContent {
    val e = elevation.toPx()
    if (e > 0.5f) {
        val outline = shape.createOutline(size, layoutDirection, this)
        val steps = 8
        translate(left = e * 0.10f, top = e * 0.26f) {
            for (step in steps downTo 1) {
                val t = step / steps.toFloat()
                val falloff = (1f - t) * (1f - t)
                drawOutline(
                    outline = outline,
                    brush = SolidColor(depth.shadow.copy(alpha = depth.shadowAlpha * falloff)),
                    style = Stroke(width = e * 2f * t),
                )
            }
        }
    }

    if (glow > 0f) {
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.radialGradient(
                0.5f to depth.glow.copy(alpha = 0.26f * glow),
                1f to Color.Transparent,
                center = Offset(size.width / 2f, size.height / 2f),
                radius = max(size.width, size.height) * 0.95f,
            ),
        )
    }

    drawContent()
}

/**
 * Presses a shape into the surface: shadow on the top-left inner edge, highlight
 * on the bottom-right inner one. For fields and anything that should feel carved
 * out of the background.
 */
fun Modifier.sunken(depth: Depth, shape: Shape): Modifier = this.drawWithContent {
    drawContent()
    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        brush = Brush.linearGradient(
            0f to depth.rimDark,
            0.45f to Color.Transparent,
            1f to depth.rimLight,
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        alpha = 0.9f,
        style = Stroke(width = depth.rimWidth.toPx()),
    )
}

/** A light edge along the top-left, a dark one along the bottom-right. */
fun Modifier.rimLight(depth: Depth, shape: Shape): Modifier = this.drawWithContent {
    drawContent()
    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        brush = Brush.linearGradient(
            0f to depth.rimLight,
            0.5f to Color.Transparent,
            1f to depth.rimDark,
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        style = Stroke(width = depth.rimWidth.toPx()),
    )
}

/**
 * A slow diagonal highlight travelling across the content, as if a lamp were
 * moving over it.
 */
@Composable
fun Modifier.sheen(color: Color, durationMillis: Int = 5200): Modifier {
    val progress by rememberInfiniteTransition(label = "sheen").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheenProgress",
    )
    return this.drawWithContent {
        drawContent()
        val band = min(size.width, size.height) * 0.6f
        val x = -band + progress * (size.width + band * 2f)
        drawRect(
            brush = Brush.linearGradient(
                0f to Color.Transparent,
                0.5f to color,
                1f to Color.Transparent,
                start = Offset(x, 0f),
                end = Offset(x + band, size.height),
            ),
        )
    }
}

/**
 * Two soft pools of light behind the content — warm at the top-left where the key
 * light sits, cool teal at the bottom-right — so a screen that is mostly text
 * still reads as an object in a room.
 */
@Composable
fun AmbientBackground(
    depth: Depth,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawRect(depth.ambient)
                drawRect(
                    Brush.radialGradient(
                        0f to depth.ambientTint,
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.10f, size.height * 0.02f),
                        radius = size.maxDimension * 0.55f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        0f to depth.coolTint,
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.95f, size.height * 0.80f),
                        radius = size.maxDimension * 0.62f,
                    ),
                )
                drawContent()
            },
        content = content,
    )
}

private val Size.maxDimension: Float get() = max(width, height)