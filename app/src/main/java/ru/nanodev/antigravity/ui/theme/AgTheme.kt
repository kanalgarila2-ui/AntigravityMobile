package ru.nanodev.antigravity.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import ru.nanodev.antigravity.ui.motion.AgAnim

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF1F3B63),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFC58AF9),
    onSecondary = Color(0xFF3E1A6B),
    tertiary = Color(0xFFF6AE74),
    onTertiary = Color(0xFF4A2300),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFE3E6EE),
    surface = Color(0xFF141821),
    onSurface = Color(0xFFE3E6EE),
    surfaceVariant = Color(0xFF1E2430),
    onSurfaceVariant = Color(0xFF9AA3B5),
    outline = Color(0xFF3A4253),
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF7B4FD6),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFB8561C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF1B1F2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F2A),
    surfaceVariant = Color(0xFFE4E8F2),
    onSurfaceVariant = Color(0xFF4A5366),
    outline = Color(0xFFB8C0D0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun AgTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, content = content)
}

/** Фон за стеклом: градиент медленно дрейфует, оттенок плавно меняется. */
@Composable
fun AgBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(AgAnim.SPLASH_BACKDROP_DRIFT.spec<Float>(), RepeatMode.Reverse),
        label = "drift"
    )
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(AgAnim.BACKDROP_HUE_SHIFT.spec<Float>(), RepeatMode.Reverse),
        label = "hue"
    )
    val mid = lerp(Color(0xFF111A33), Color(0xFF162B4D), hue)
    val glow = lerp(Color(0xFF1A1433), Color(0xFF22164A), hue)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0B0E14), mid, glow, Color(0xFF0B0E14)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f + 400f * drift, 1600f - 300f * hue)
                )
            )
    ) { content() }
}

/**
 * «Liquid glass»: полупрозрачная заливка, светлая кромка и тонкий блик.
 * Визуальное приближение: настоящее размытие фона требует RenderEffect и грузит GPU.
 */
fun Modifier.glass(shape: Shape, alpha: Float = 0.10f): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = alpha * 1.6f),
                Color.White.copy(alpha = alpha * 0.5f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.Transparent,
                Color.White.copy(alpha = 0.12f)
            )
        ),
        shape = shape
    )

val GlassCornerRadius = 24.dp
