package ru.nanodev.antigravity.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nanodev.antigravity.ui.motion.AgAnim

/** Метка «BETA» для экспериментальных функций: плавно появляется и мягко пульсирует. */
@Composable
fun BetaBadge(modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = AgAnim.BETA_BADGE_FADE.spec<Float>(),
        label = "beta-appear"
    )
    val pulse by rememberInfiniteTransition(label = "beta").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(AgAnim.REMOTE_BADGE_PULSE.spec<Float>(), RepeatMode.Reverse),
        label = "beta-pulse"
    )
    Surface(
        modifier = modifier.alpha(appear * pulse),
        shape = RoundedCornerShape(6.dp),
        color = Color(0x33FDBA74)
    ) {
        Text(
            text = "BETA",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color(0xFFFDBA74),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
