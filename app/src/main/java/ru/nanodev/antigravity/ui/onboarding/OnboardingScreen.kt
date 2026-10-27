package ru.nanodev.antigravity.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.nanodev.antigravity.R
import ru.nanodev.antigravity.auth.ApiKeyStore
import ru.nanodev.antigravity.net.Connectivity
import ru.nanodev.antigravity.net.GeminiApi
import ru.nanodev.antigravity.net.KeyCheck
import ru.nanodev.antigravity.ui.motion.AgAnim
import ru.nanodev.antigravity.ui.theme.glass

/** Состояние проверки ключа на экране входа. */
private sealed interface OnbState {
    data object Idle : OnbState
    data object Checking : OnbState
    data class Valid(val models: Int) : OnbState
    data class Invalid(val reason: String) : OnbState
    data class Failed(val reason: String) : OnbState
}

/**
 * Экран входа при первом запуске: ввод API-ключа Gemini, проверка запросом к API,
 * сохранение в зашифрованное хранилище. Вход через Google появится после подключения OAuth-клиента.
 */
@Composable
fun OnboardingScreen(keyStore: ApiKeyStore, onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val online by Connectivity.online.collectAsState()

    var key by rememberSaveable { mutableStateOf("") }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var state by remember { mutableStateOf<OnbState>(OnbState.Idle) }

    val entered = remember { MutableTransitionState(false).apply { targetState = true } }
    val shake = remember { Animatable(0f) }

    val pulse by rememberInfiniteTransition(label = "logo-pulse").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = AgAnim.ONBOARD_CHECK_RING_PULSE.spec<Float>(),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(state) {
        if (state is OnbState.Invalid) {
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = AgAnim.ONBOARD_SHAKE_ERROR.durationMs
                    0f at 0
                    -14f at 60
                    14f at 140
                    -8f at 240
                    8f at 320
                    0f at 420
                }
            )
        }
    }

    fun validate() {
        if (state is OnbState.Checking) return
        state = OnbState.Checking
        scope.launch {
            when (val r = GeminiApi.validateKey(key)) {
                is KeyCheck.Valid -> {
                    keyStore.apiKey = key
                    state = OnbState.Valid(r.modelCount)
                    delay(AgAnim.ONBOARD_SUCCESS_EXIT_FADE.durationMs.toLong() + 500L)
                    onAuthenticated()
                }
                is KeyCheck.Invalid -> state = OnbState.Invalid(r.reason)
                is KeyCheck.Failed -> state = OnbState.Failed(r.reason)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(48.dp))

        AnimatedVisibility(
            visibleState = entered,
            enter = fadeIn(AgAnim.SPLASH_LOGO_FADE_IN.spec<Float>()) +
                slideInVertically(AgAnim.SPLASH_LOGO_SCALE_IN.spec<IntOffset>()) { it / 5 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).scale(pulse)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Antigravity 2.0",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Вход и проверка API-ключа",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        AnimatedVisibility(
            visibleState = entered,
            enter = fadeIn(AgAnim.ONBOARD_CARD_ENTER.spec<Float>()) +
                slideInVertically(AgAnim.ONBOARD_CARD_ENTER.spec<IntOffset>()) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shake.value.roundToInt(), 0) }
                    .glass(RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it.trim()
                        if (state !is OnbState.Checking) state = OnbState.Idle
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gemini API-ключ") },
                    singleLine = true,
                    enabled = state !is OnbState.Checking,
                    visualTransformation = if (reveal) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { reveal = !reveal }) {
                            Icon(
                                if (reveal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (reveal) "Скрыть" else "Показать"
                            )
                        }
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        clipboard.getText()?.text?.let { key = it.trim(); state = OnbState.Idle }
                    }) { Text("Вставить") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text("Получить ключ") }
                }

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn((if (targetState is OnbState.Valid) AgAnim.ONBOARD_SUCCESS_CHECK_DRAW else AgAnim.ONBOARD_ERROR_TEXT_SWAP).spec<Float>()) togetherWith
                            fadeOut(AgAnim.ONBOARD_ERROR_TEXT_SWAP.spec<Float>())
                    },
                    label = "status"
                ) { s ->
                    when (s) {
                        OnbState.Idle -> Text(
                            if (online) "Ключ проверяется запросом к Gemini API" else "Нет сети: проверка ключа недоступна",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OnbState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Проверяем ключ…", color = MaterialTheme.colorScheme.onSurface)
                        }
                        is OnbState.Valid -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34A853))
                            Spacer(Modifier.width(8.dp))
                            Text("Ключ работает · моделей: ${s.models}", color = Color(0xFF34A853))
                        }
                        is OnbState.Invalid -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(s.reason, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                        is OnbState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFDBA74))
                            Spacer(Modifier.width(8.dp))
                            Text(s.reason, color = Color(0xFFFDBA74), fontSize = 13.sp)
                        }
                    }
                }

                Button(
                    onClick = { validate() },
                    enabled = key.isNotBlank() && online && state !is OnbState.Checking && state !is OnbState.Valid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Проверить и войти", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти через Google · скоро")
        }
    }
}
