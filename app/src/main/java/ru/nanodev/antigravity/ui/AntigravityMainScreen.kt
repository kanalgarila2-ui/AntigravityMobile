package ru.nanodev.antigravity.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nanodev.antigravity.mcp.McpServerEngine
import ru.nanodev.antigravity.net.Connectivity
import ru.nanodev.antigravity.remotedesktop.RemoteDesktopService
import ru.nanodev.antigravity.runtime.AntigravityShellEngine
import ru.nanodev.antigravity.scheduler.ScheduleManager
import ru.nanodev.antigravity.sdk.AntigravityAgentOrchestrator
import ru.nanodev.antigravity.sdk.AntigravitySettings
import ru.nanodev.antigravity.sdk.DynamicModelRegistry
import ru.nanodev.antigravity.sdk.PermissionPreset
import ru.nanodev.antigravity.sdk.QuotaLimitsMonitor
import ru.nanodev.antigravity.storage.ChatMessage
import ru.nanodev.antigravity.storage.DownloadsProjectManager
import ru.nanodev.antigravity.storage.ProjectChatSession
import ru.nanodev.antigravity.ui.motion.AgAnim
import ru.nanodev.antigravity.ui.theme.GlassCornerRadius
import ru.nanodev.antigravity.ui.theme.glass
import ru.nanodev.antigravity.voice.BilingualVoiceEngine
import java.io.File
import java.util.UUID

private enum class Tab(val label: String) {
    CHAT("Чат"), TERMINAL("Терминал"), REMOTE("Remote"), TOOLS("Инструменты"), SETTINGS("Настройки")
}

/**
 * Главный экран после входа. Чаты и история читаются с диска и работают без сети.
 * Без сети блокируется только отправка запроса к модели.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityMainScreen(
    projectManager: DownloadsProjectManager,
    shellEngine: AntigravityShellEngine,
    voiceEngine: BilingualVoiceEngine,
    mcpEngine: McpServerEngine,
    scheduleManager: ScheduleManager,
    modelRegistry: DynamicModelRegistry,
    quotaMonitor: QuotaLimitsMonitor,
    orchestrator: AntigravityAgentOrchestrator,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val online by Connectivity.online.collectAsState()

    var settings by remember { mutableStateOf(projectManager.loadGlobalSettings()) }
    fun persist(next: AntigravitySettings) {
        settings = next
        scope.launch(Dispatchers.IO) { projectManager.saveGlobalSettings(next) }
    }

    var session by remember {
        mutableStateOf(
            projectManager.listProjects().maxByOrNull { it.updatedAt }
                ?: projectManager.createOrOpenProjectChat("Личный")
        )
    }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = Tab.entries[tabIndex]

    var input by rememberSaveable { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showProjects by remember { mutableStateOf(false) }

    val listening by voiceEngine.isListening.collectAsState()
    val models by modelRegistry.models.collectAsState()
    val quota by quotaMonitor.snapshot.collectAsState()
    val tasks by scheduleManager.tasks.collectAsState()
    val servers by mcpEngine.externalServers.collectAsState()

    fun saveSession(s: ProjectChatSession) {
        scope.launch(Dispatchers.IO) { projectManager.saveProjectSession(s) }
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || running) return
        if (!online && !text.startsWith("!")) {
            error = "Нет подключения к интернету. Отправка недоступна, историю можно читать."
            return
        }
        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), role = "user", content = text)
        session = session.copy(messages = session.messages + userMsg, updatedAt = System.currentTimeMillis())
        saveSession(session)
        input = ""
        error = null
        running = true
        val turnSettings = settings
        scope.launch {
            orchestrator.runAgentTurn(text, session, turnSettings) { reply ->
                scope.launch {
                    session = session.copy(messages = session.messages + reply, updatedAt = System.currentTimeMillis())
                    saveSession(session)
                }
            }
            running = false
        }
    }

    fun toggleMic() {
        if (listening) {
            voiceEngine.stopListening()
        } else {
            voiceEngine.startLiveTranscription(
                enableTechCodeSwitch = settings.voiceAutoTechCodeSwitch,
                onPartialResult = { input = it },
                onFinalResult = { input = it },
                onError = { error = it }
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(
                            if (tab == Tab.CHAT) session.projectName else tab.label,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (tab == Tab.CHAT) {
                            Text(
                                if (online) "Онлайн" else "Офлайн: только просмотр",
                                fontSize = 11.sp,
                                color = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                actions = {
                    if (tab == Tab.CHAT) {
                        IconButton(onClick = { showProjects = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Проекты")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                NavItem(Tab.CHAT, tabIndex, Icons.Default.Chat) { tabIndex = it }
                NavItem(Tab.TERMINAL, tabIndex, Icons.Default.Terminal) { tabIndex = it }
                NavItem(Tab.REMOTE, tabIndex, Icons.Default.DesktopWindows) { tabIndex = it }
                NavItem(Tab.TOOLS, tabIndex, Icons.Default.Build) { tabIndex = it }
                NavItem(Tab.SETTINGS, tabIndex, Icons.Default.Settings) { tabIndex = it }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (fadeIn(AgAnim.TAB_CONTENT_SLIDE_LEFT.spec<Float>()) +
                    slideInVertically((if (dir > 0) AgAnim.TAB_CONTENT_SLIDE_LEFT else AgAnim.TAB_CONTENT_SLIDE_RIGHT).spec<IntOffset>()) { it / 12 * dir }) togetherWith
                    fadeOut(AgAnim.TAB_CONTENT_FADE.spec<Float>())
            },
            label = "tabs"
        ) { current ->
            when (current) {
                Tab.CHAT -> ChatPane(
                    session = session,
                    online = online,
                    running = running,
                    input = input,
                    error = error,
                    listening = listening,
                    onInput = { input = it },
                    onSend = { send() },
                    onMic = { toggleMic() }
                )
                Tab.TERMINAL -> TerminalPane(shellEngine, projectManager, session, settings)
                Tab.REMOTE -> RemotePane(
                    enabled = settings.remoteControlHostEnabled,
                    onEnable = { on ->
                        persist(settings.copy(remoteControlHostEnabled = on))
                        RemoteDesktopService.setEnabled(context, on)
                    }
                )
                Tab.TOOLS -> ToolsPane(
                    creditsLeft = quota.aiCreditsRemaining,
                    creditsMax = quota.aiCreditsMax,
                    servers = servers.map { "${it.name} · ${it.transport} · ${if (it.enabled) "вкл" else "выкл"}" },
                    tasks = tasks.map { it.id to "${it.title} · ${it.lastRunStatus}" },
                    onRunTask = { scheduleManager.runTaskImmediately(it) }
                )
                Tab.SETTINGS -> SettingsPane(
                    settings = settings,
                    models = models.map { it.id to it.displayName },
                    onChange = { persist(it) },
                    onRefreshModels = {
                        scope.launch { modelRegistry.refreshModelsFromApi(settings) }
                    },
                    onLogout = onLogout
                )
            }
        }
    }

    if (showProjects) {
        ProjectsSheet(
            projects = projectManager.listProjects(),
            onPick = { picked ->
                session = picked
                showProjects = false
            },
            onCreate = { name ->
                session = projectManager.createOrOpenProjectChat(name)
                showProjects = false
            },
            onDismiss = { showProjects = false }
        )
    }
}

@Composable
private fun RowScope.NavItem(
    item: Tab,
    selectedIndex: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: (Int) -> Unit
) {
    val selected = selectedIndex == item.ordinal
    val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, AgAnim.NAV_ICON_BOUNCE.spec<Float>(), label = "navscale")
    val labelAlpha by animateFloatAsState(if (selected) 1f else 0.7f, AgAnim.NAV_LABEL_FADE.spec<Float>(), label = "navalpha")
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(item.ordinal) },
        icon = { Icon(icon, contentDescription = item.label, modifier = Modifier.scale(iconScale)) },
        label = { Text(item.label, fontSize = 10.sp, modifier = Modifier.alpha(labelAlpha)) },
        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
    )
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val entered = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = entered,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn(AgAnim.CARD_ENTER_STAGGER.spec<Float>()) +
            slideInVertically(AgAnim.CARD_ENTER_STAGGER.spec<IntOffset>()) { it / 8 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(GlassCornerRadius))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { content() }
    }
}

@Composable
private fun ChatPane(
    session: ProjectChatSession,
    online: Boolean,
    running: Boolean,
    input: String,
    error: String?,
    listening: Boolean,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffectScroll(listState, session.messages.size)
    val floatY by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(AgAnim.EMPTY_STATE_FLOAT.spec<Float>(), RepeatMode.Reverse),
        label = "float-y"
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        AnimatedVisibility(
            visible = !online,
            enter = fadeIn(AgAnim.ERROR_BANNER_SLIDE.spec<Float>()) +
                slideInVertically(AgAnim.ERROR_BANNER_SLIDE.spec<IntOffset>()) { -it },
            exit = fadeOut(AgAnim.ERROR_BANNER_SLIDE.spec<Float>())
        ) {
            GlassCard(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Нет подключения. Чаты открываются без сети, отправка вернётся с интернетом.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            if (session.messages.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 48.dp).offset { IntOffset(0, floatY.roundToInt()) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Новый диалог", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text("Введите запрос, !команда запускает shell", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            items(session.messages, key = { it.id }) { msg -> Bubble(msg) }
            if (running) {
                item { TypingIndicator() }
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        }

        GlassCard(Modifier.padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (online) "Сообщение агенту" else "Офлайн: отправка недоступна") },
                    enabled = online || input.startsWith("!"),
                    maxLines = 4
                )
                val canSend = input.isNotBlank() && !running && (online || input.startsWith("!"))
                val micTint by animateColorAsState(
                    if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    AgAnim.CHAT_MIC_PULSE.spec<Color>(),
                    label = "mic"
                )
                val sendTint by animateColorAsState(
                    if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    AgAnim.CHAT_SEND_PULSE.spec<Color>(),
                    label = "send"
                )
                IconButton(onClick = onMic) {
                    Icon(if (listening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Голос", tint = micTint)
                }
                IconButton(onClick = onSend, enabled = canSend) {
                    Icon(Icons.Default.Send, contentDescription = "Отправить", tint = sendTint)
                }
            }
        }
    }
}

@Composable
private fun LaunchedEffectScroll(state: androidx.compose.foundation.lazy.LazyListState, count: Int) {
    androidx.compose.runtime.LaunchedEffect(count) {
        if (count > 0) state.animateScrollToItem(count - 1)
    }
}

@Composable
private fun TypingIndicator() {
    val alpha by rememberInfiniteTransition(label = "typing").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(AgAnim.CHAT_TYPING_DOT.spec<Float>(), RepeatMode.Reverse),
        label = "typing-alpha"
    )
    Text(
        "Агент работает…",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(AgAnim.CHAT_BUBBLE_ENTER.spec<Float>()) +
            slideInVertically(AgAnim.CHAT_BUBBLE_ENTER.spec<IntOffset>()) { it / 4 }
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (msg.toolName.isNotEmpty()) {
                        Text(msg.toolName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(msg.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (msg.thoughtTrace.isNotEmpty()) {
                        Text(msg.thoughtTrace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalPane(
    shellEngine: AntigravityShellEngine,
    projectManager: DownloadsProjectManager,
    session: ProjectChatSession,
    settings: AntigravitySettings
) {
    val scope = rememberCoroutineScope()
    var cmd by rememberSaveable { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf("Antigravity shell · введите команду")) }

    fun run() {
        val line = cmd.trim()
        if (line.isEmpty()) return
        lines = lines + "$ $line"
        cmd = ""
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                shellEngine.execute(
                    line,
                    File(session.folderPath, "workspace").apply { mkdirs() },
                    settings.preferTermuxIfInstalled
                )
            }
            val out = res.stdout.lines().filter { it.isNotBlank() }
            val err = res.stderr.lines().filter { it.isNotBlank() }.map { "[err] $it" }
            lines = lines + out + err
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        GlassCard(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(lines) { l ->
                    Text(l, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cmd,
                onValueChange = { cmd = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("ls -la, git status, python3 --version") }
            )
            IconButton(onClick = { run() }, enabled = cmd.isNotBlank()) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Выполнить")
            }
        }
    }
}

@Composable
private fun RemotePane(enabled: Boolean, onEnable: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Remote Control", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                BetaBadge()
            }
            Text(
                if (enabled) {
                    "Включено. Мост к antigravity.google.com в этой Beta-сборке ещё не подключён: " +
                        "сессии на этом телефоне в веб-дашборде пока не видны."
                } else {
                    "Выключено по умолчанию. Функция для просмотра и ведения диалогов с других устройств через " +
                        "antigravity.google.com. В этой Beta-сборке мост ещё не подключён."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (enabled) {
                OutlinedButton(onClick = { onEnable(false) }) { Text("Выключить") }
            } else {
                OutlinedButton(onClick = { onEnable(true) }) { Text("Включить Beta") }
            }
        }
    }
}

@Composable
private fun ToolsPane(
    creditsLeft: Int,
    creditsMax: Int,
    servers: List<String>,
    tasks: List<Pair<String, String>>,
    onRunTask: (String) -> Unit
) {
    val target = if (creditsMax > 0) creditsLeft.toFloat() / creditsMax.toFloat() else 0f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = AgAnim.QUOTA_BAR_FILL.spec<Float>(),
        label = "quota"
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            Text("AI Credits", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
            Text("$creditsLeft / $creditsMax", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        GlassCard {
            Text("MCP-серверы", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (servers.isEmpty()) {
                Text("Внешние серверы не настроены", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            servers.forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) }
        }
        GlassCard {
            Text("Расписание", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (tasks.isEmpty()) {
                Text("Задач нет", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            tasks.forEach { (id, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRunTask(id) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Запустить сейчас")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPane(
    settings: AntigravitySettings,
    models: List<Pair<String, String>>,
    onChange: (AntigravitySettings) -> Unit,
    onRefreshModels: () -> Unit,
    onLogout: () -> Unit
) {
    val ctx = LocalContext.current
    val warnAlpha by rememberInfiniteTransition(label = "warn").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(AgAnim.WARNING_BORDER_BLINK.spec<Float>(), RepeatMode.Reverse),
        label = "warn-alpha"
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            Text("Ключ API", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Ключ хранится зашифрованно на устройстве и не попадает в файлы настроек.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onLogout) { Text("Сменить ключ / выйти") }
        }

        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Модель", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefreshModels) { Text("Обновить") }
            }
            models.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (id, name) ->
                        FilterChip(
                            selected = settings.selectedModel == id,
                            onClick = { onChange(settings.copy(selectedModel = id)) },
                            label = { Text(name, fontSize = 12.sp, maxLines = 1) }
                        )
                    }
                }
            }
        }

        GlassCard {
            Text("Разрешения", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Row(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionPreset.entries.forEach { p ->
                    FilterChip(
                        selected = settings.permissionPreset == p,
                        onClick = { onChange(settings.copy(permissionPreset = p)) },
                        label = { Text(p.displayName, fontSize = 12.sp) }
                    )
                }
            }
            Text(
                settings.permissionPreset.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlassCard {
            Text(
                "Лимит шагов: ${settings.maxAgentSteps}",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = settings.maxAgentSteps.toFloat(),
                onValueChange = { onChange(settings.copy(maxAgentSteps = it.toInt())) },
                valueRange = 1f..100f
            )
            SwitchRow(
                title = "Автовыключение лимита шагов",
                checked = settings.autoDisableStepLimit,
                onChecked = { onChange(settings.copy(autoDisableStepLimit = it)) }
            )
        }

        GlassCard {
            SwitchRow(
                title = "Голос: авто-переключение терминов на английский",
                checked = settings.voiceAutoTechCodeSwitch,
                onChecked = { onChange(settings.copy(voiceAutoTechCodeSwitch = it)) }
            )
            SwitchRow(
                title = "Remote Control (Beta)",
                badge = { BetaBadge() },
                checked = settings.remoteControlHostEnabled,
                onChecked = { on ->
                    onChange(settings.copy(remoteControlHostEnabled = on))
                    RemoteDesktopService.setEnabled(ctx, on)
                }
            )
        }

        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = warnAlpha))
                Spacer(Modifier.width(8.dp))
                Text("Опасная зона", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            }
            SwitchRow(
                title = "Разрешить всё (без подтверждений и песочницы)",
                checked = settings.allowAllDangerously,
                onChecked = { onChange(settings.copy(allowAllDangerously = it)) }
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    badge: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            if (badge != null) {
                Spacer(Modifier.width(6.dp))
                badge()
            }
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsSheet(
    projects: List<ProjectChatSession>,
    onPick: (ProjectChatSession) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Проекты", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            LazyColumn(Modifier.fillMaxWidth().height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(projects, key = { it.folderPath }) { p ->
                    AssistChip(
                        onClick = { onPick(p) },
                        label = { Text(p.projectName, maxLines = 1) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Новый проект") }
                )
                IconButton(
                    onClick = { if (newName.isNotBlank()) onCreate(newName.trim()) },
                    enabled = newName.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
