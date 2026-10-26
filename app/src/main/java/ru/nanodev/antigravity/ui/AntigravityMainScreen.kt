package ru.nanodev.antigravity.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.nanodev.antigravity.accounts.AccountSwitcherManager
import ru.nanodev.antigravity.mcp.McpServerEngine
import ru.nanodev.antigravity.offline.OfflineModelEngine
import ru.nanodev.antigravity.remotedesktop.RemoteDesktopService
import ru.nanodev.antigravity.runtime.AntigravityShellEngine
import ru.nanodev.antigravity.scheduler.ScheduleManager
import ru.nanodev.antigravity.sdk.*
import ru.nanodev.antigravity.storage.ChatMessage
import ru.nanodev.antigravity.storage.DownloadsProjectManager
import ru.nanodev.antigravity.storage.ProjectChatSession
import ru.nanodev.antigravity.unlocker.AgyUnlockerService
import ru.nanodev.antigravity.voice.BilingualVoiceEngine
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityMainScreen(
    projectManager: DownloadsProjectManager,
    shellEngine: AntigravityShellEngine,
    voiceEngine: BilingualVoiceEngine,
    mcpEngine: McpServerEngine,
    offlineEngine: OfflineModelEngine,
    scheduleManager: ScheduleManager,
    accountSwitcher: AccountSwitcherManager,
    modelRegistry: DynamicModelRegistry,
    quotaMonitor: QuotaLimitsMonitor,
    orchestrator: AntigravityAgentOrchestrator,
    initialQuickAction: String? = null,
    onRequestManageStorage: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(projectManager.loadGlobalSettings()) }
    var projects by remember { mutableStateOf(projectManager.listProjects()) }
    var activeSession by remember { mutableStateOf(projects.first()) }

    // 0=Agent Chat, 1=CMD Terminal, 2=Remote Desktop, 3=MCP & Offline & /schedule, 4=Settings
    var selectedTab by remember {
        mutableIntStateOf(if (initialQuickAction == "remote_desktop") 2 else 0)
    }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectNameInput by remember { mutableStateOf("") }
    var showLimitsWidgetDialog by remember { mutableStateOf(false) }
    var showAccountSwitcherDialog by remember { mutableStateOf(false) }
    var showModelPickerDialog by remember { mutableStateOf(false) }

    var inputPrompt by remember {
        mutableStateOf(if (initialQuickAction == "voice_goal") "/goal " else "")
    }
    var isAgentRunning by remember { mutableStateOf(false) }

    val isVoiceListening by voiceEngine.isListening.collectAsState()
    val unlockerStatus by AgyUnlockerService.status.collectAsState()
    val availableModels by modelRegistry.models.collectAsState()
    val quotaSnapshot by quotaMonitor.snapshot.collectAsState()
    val isQuotaRefreshing by quotaMonitor.isRefreshing.collectAsState()
    val remoteDesktopState by RemoteDesktopService.state.collectAsState()
    val mcpServers by mcpEngine.externalServers.collectAsState()
    val localOfflineModels by offlineEngine.localModels.collectAsState()
    val scheduledTasks by scheduleManager.tasks.collectAsState()

    // Initial API fetch of latest models
    LaunchedEffect(settings.geminiApiKey, settings.accounts) {
        modelRegistry.refreshModelsFromApi(settings)
    }

    var terminalInput by remember { mutableStateOf("") }
    var terminalLogs by remember {
        mutableStateOf(
            "Antigravity 2.0 (ru.nanodev.antigravity) Shell [v0.1.0 | Android 11+]\n" +
            "Type 'help' for all ${AntigravityShellEngine.SUPPORTED_COMMANDS.size} runtime commands.\n"
        )
    }

    val bgDark = Color(0xFF090C10)
    val surfaceDark = Color(0xFF10151E)
    val cardDark = Color(0xFF161D2A)
    val accentBlue = Color(0xFF3B82F6)
    val accentCyan = Color(0xFF00E5FF)
    val emeraldGreen = Color(0xFF10B981)

    val infiniteTransition = rememberInfiniteTransition(label = "agy_light")
    val lightProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(settings.runningLightSpeed.durationMs.coerceAtLeast(400), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "light_bar"
    )

    val activeAccount = settings.accounts.firstOrNull { it.isActive }

    Scaffold(
        containerColor = bgDark,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(surfaceDark)) {
                // Top AgyUnlocker + Quota Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF062E22))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(emeraldGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AgyUnlocker: ${unlockerStatus.bannerText}",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Interactive Limits Widget Trigger (auto-refreshes every 5 min OR immediately when clicked!)
                    Surface(
                        color = Color(0xFF044E3A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable {
                            showLimitsWidgetDialog = true
                            scope.launch { quotaMonitor.onLimitsWidgetOpened(settings) }
                        }
                    ) {
                        Text(
                            text = "📊 Лимиты: ${quotaSnapshot.aiCreditsRemaining} cr • ⟳ ${quotaSnapshot.nextAutoRefreshSeconds}с",
                            color = Color(0xFF6EE7B7),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Desktop-Grade Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AG", color = bgDark, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Antigravity 2.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = accentBlue.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(5.dp),
                                    modifier = Modifier.clickable {
                                        showModelPickerDialog = true
                                        scope.launch { modelRegistry.refreshModelsFromApi(settings) }
                                    }
                                ) {
                                    Text(
                                        text = "🧠 ${settings.selectedModel} ▾",
                                        color = accentCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "📁 /Download/Antigravity/chats/${activeSession.projectName}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Instant Account Switcher Chip
                        Surface(
                            color = cardDark,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable { showAccountSwitcherDialog = true }
                        ) {
                            Text(
                                text = "👤 ${activeAccount?.email?.substringBefore("@") ?: "Аккаунт"} ▾",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        IconButton(onClick = { showNewProjectDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Новый проект", tint = accentCyan)
                        }
                    }
                }

                if (isAgentRunning && settings.runningLightSpeed != RunningLightSpeed.OFF) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    0f to bgDark,
                                    lightProgress.coerceIn(0.05f, 0.95f) to accentCyan,
                                    1f to accentBlue
                                )
                            )
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = surfaceDark) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Агент 2.0", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    label = { Text("CMD 120+", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                    label = { Text("Remote ПК", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Hub, contentDescription = null) },
                    label = { Text("MCP/Offline", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text("Настройки", fontSize = 10.sp) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> {
                    // Agent Chat Surface
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeSession.messages, key = { it.id }) { msg ->
                                val isUser = msg.role == "user"
                                Surface(
                                    color = if (isUser) Color(0xFF1E293B) else cardDark,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isUser) "Вы" else "🤖 Antigravity 2.0 (${settings.selectedModel})",
                                            color = if (isUser) accentCyan else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (msg.thoughtTrace.isNotEmpty()) {
                                            Text(
                                                text = "💭 ${msg.thoughtTrace}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(msg.content, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputPrompt,
                                onValueChange = { inputPrompt = it },
                                placeholder = { Text("Задача агенту, /usage, /goal или 🎙️ RU+EN...") },
                                modifier = Modifier.weight(1f),
                                maxLines = 3
                            )
                            IconButton(
                                onClick = {
                                    if (isVoiceListening) voiceEngine.stopListening()
                                    else voiceEngine.startLiveTranscription(
                                        enableTechCodeSwitch = settings.voiceAutoTechCodeSwitch,
                                        onPartialResult = { inputPrompt = it },
                                        onFinalResult = { inputPrompt = it },
                                        onError = {
                                            inputPrompt = voiceEngine.normalizeMixedRuEnSpeech(
                                                "я делаю drive приложение и пушу в main ветку", true
                                            )
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Голос RU+EN",
                                    tint = if (isVoiceListening) Color.Red else accentCyan
                                )
                            }
                            Button(
                                onClick = {
                                    val prompt = inputPrompt.trim()
                                    if (prompt.isNotEmpty() && !isAgentRunning) {
                                        inputPrompt = ""
                                        val uMsg = ChatMessage(UUID.randomUUID().toString(), "user", prompt)
                                        activeSession = activeSession.copy(messages = activeSession.messages + uMsg)
                                        projectManager.saveProjectSession(activeSession)
                                        isAgentRunning = true
                                        scope.launch {
                                            orchestrator.runAgentTurn(prompt, activeSession, settings) { reply ->
                                                activeSession = activeSession.copy(messages = activeSession.messages + reply)
                                                projectManager.saveProjectSession(activeSession)
                                            }
                                            isAgentRunning = false
                                        }
                                    }
                                }
                            ) {
                                Text("Пуск")
                            }
                        }
                    }
                }

                1 -> {
                    // Mini-Platform Terminal
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Text("⚡ Мини-Платформа CMD (120+ команд + Termux Bridge)", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF05070B))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(terminalLogs, color = emeraldGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = terminalInput,
                                onValueChange = { terminalInput = it },
                                placeholder = { Text("help, ls -la, mcp-list, offline-models, quota-usage...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(onClick = {
                                val cmd = terminalInput.trim()
                                if (cmd.isNotEmpty()) {
                                    terminalInput = ""
                                    scope.launch {
                                        val out = shellEngine.execute(cmd, File(activeSession.folderPath, "workspace"), settings.preferTermuxIfInstalled)
                                        terminalLogs += "\n$ $cmd\n${out.stdout}\n"
                                    }
                                }
                            }) { Text("Run") }
                        }
                    }
                }

                2 -> {
                    // Full Remote Desktop & Antigravity Remote Control Surface
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🖥️ Remote Desktop & Antigravity 2.0 Remote Control", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Двустороннее управление: подключайтесь к рабочему столу ПК (WebRTC/VNC) или управляйте агентом телефона из браузера (${remoteDesktopState.hostWebUrl})",
                            color = Color(0xFF94A3B8), fontSize = 12.sp
                        )

                        remoteDesktopState.workstations.forEach { ws ->
                            Surface(
                                color = if (ws.isConnected) Color(0xFF142834) else cardDark,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().clickable { RemoteDesktopService.connectToWorkstation(ws.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(ws.nickname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${ws.hostAddress} • ${ws.protocol} • ${ws.latencyMs} мс", color = accentCyan, fontSize = 11.sp)
                                    }
                                    Text(if (ws.isConnected) "ONLINE" else "Подключить", color = emeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Interactive Remote Screen Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF050811))
                                .border(1.dp, accentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        RemoteDesktopService.sendRemoteTouchOrClick(offset.x.toInt(), offset.y.toInt())
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🖥️ Удалённый экран: ${remoteDesktopState.connectedWorkstationId}", color = accentCyan, fontWeight = FontWeight.Bold)
                                Text("Коснитесь экрана для передачи клика • Курсор: (${remoteDesktopState.lastCursorX}, ${remoteDesktopState.lastCursorY})", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Cmd + ,", "Shift + Tab", "Ctrl + C", "Enter", "Esc").forEach { keyCombo ->
                                OutlinedButton(onClick = { RemoteDesktopService.sendRemoteKeyCombo(keyCombo) }) {
                                    Text(keyCombo, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // MCP Server, Offline LiteRT Models & Background Scheduler (/schedule)
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🔌 MCP Серверы (Model Context Protocol)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        mcpServers.forEach { srv ->
                            Surface(color = cardDark, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${srv.name} (${srv.transport})", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(srv.endpointOrCommand, color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Text("🧠 Оффлайн-модели на устройстве (LiteRT / GGUF)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        localOfflineModels.forEach { lm ->
                            Surface(
                                color = cardDark,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    offlineEngine.markModelProvisioned(lm.id)
                                    settings = settings.copy(selectedModel = lm.id, offlineModeEnabled = true)
                                    projectManager.saveGlobalSettings(settings)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(lm.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${lm.format} • ${lm.sizeMb} MB • /Download/Antigravity/models/${lm.fileName}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    Text(if (lm.isActive) "АКТИВНА" else "Выбрать", color = emeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text("⏰ Фоновый планировщик задач (/schedule • WorkManager)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        scheduledTasks.forEach { task ->
                            Surface(color = cardDark, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${task.cronOrInterval} • ${task.promptCommand}", color = accentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("Статус: ${task.lastRunStatus}", color = emeraldGreen, fontSize = 11.sp)
                                    }
                                    Button(onClick = { scheduleManager.runTaskImmediately(task.id) }) {
                                        Text("Запустить", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // Full Settings + Glowing "Разрешить всё!" at the bottom
                    val glowColor by infiniteTransition.animateColor(
                        initialValue = Color(0xFFF59E0B),
                        targetValue = Color(0xFFEF4444),
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        label = "danger_glow"
                    )
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⚙️ Настройки Antigravity 2.0 (ru.nanodev.antigravity)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        Surface(color = cardDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Лимит шагов: ${if (settings.autoDisableStepLimit) "БЕЗ ЛИМИТА (∞)" else settings.maxAgentSteps}", color = Color.White, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = settings.maxAgentSteps.toFloat(),
                                    onValueChange = {
                                        settings = settings.copy(maxAgentSteps = it.toInt())
                                        projectManager.saveGlobalSettings(settings)
                                    },
                                    valueRange = 10f..150f,
                                    enabled = !settings.autoDisableStepLimit
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Автовыключение лимита шагов (/goal)", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = settings.autoDisableStepLimit,
                                        onCheckedChange = {
                                            settings = settings.copy(autoDisableStepLimit = it)
                                            projectManager.saveGlobalSettings(settings)
                                        }
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("📱 Управление телефоном (вкл. в System Prompt)", color = Color(0xFFFDBA74), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Switch(
                                        checked = settings.phoneControlEnabled,
                                        onCheckedChange = {
                                            settings = settings.copy(phoneControlEnabled = it)
                                            projectManager.saveGlobalSettings(settings)
                                        }
                                    )
                                }
                            }
                        }

                        // Highlighted Warning "Разрешить всё!" at the very end of Settings
                        Surface(
                            color = if (settings.allowAllDangerously) Color(0xFF3B0A0A) else Color(0xFF1F1610),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(2.dp, glowColor, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️ Разрешить всё! (Allow All / Turbo)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    Switch(
                                        checked = settings.allowAllDangerously,
                                        onCheckedChange = {
                                            settings = settings.copy(
                                                allowAllDangerously = it,
                                                permissionPreset = if (it) PermissionPreset.TURBO else PermissionPreset.AUTO_PREVIEW,
                                                autoDisableStepLimit = if (it) true else settings.autoDisableStepLimit
                                            )
                                            projectManager.saveGlobalSettings(settings)
                                        }
                                    )
                                }
                                Text(
                                    "ОСТОРОЖНО (ПОДСВЕЧИВАЮЩИЙСЯ РЕЖИМ): Активирует policy.allow_all() — агент выполняет все 120+ команд, MCP и управление устройством без подтверждения!",
                                    color = Color(0xFFFDE68A), fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Quota & Limits Widget (Auto-updates every 5m OR immediately when opened)
    if (showLimitsWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showLimitsWidgetDialog = false },
            containerColor = surfaceDark,
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📊 Виджет Лимитов и Квот (/usage)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (isQuotaRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accentCyan)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Аккаунт: ${quotaSnapshot.accountEmail}", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("AI Credits: ${quotaSnapshot.aiCreditsRemaining} / ${quotaSnapshot.aiCreditsMax} (автообновление через ${quotaSnapshot.nextAutoRefreshSeconds} сек)", color = Color.White, fontSize = 12.sp)
                    quotaSnapshot.modelQuotas.forEach { q ->
                        Surface(color = cardDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(q.modelLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Запросы: ${q.remainingRequests}/${q.maxRequests} • Токены: ${q.remainingTokens / 1000}K • Сброс: ${q.resetInMinutes}m", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { scope.launch { quotaMonitor.refreshLimitsNow(settings, "manual_button") } }) {
                    Text("Обновить сейчас по API")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitsWidgetDialog = false }) { Text("Закрыть") }
            }
        )
    }

    // Dialog 2: Dynamic API Model Selector
    if (showModelPickerDialog) {
        AlertDialog(
            onDismissRequest = { showModelPickerDialog = false },
            containerColor = surfaceDark,
            title = { Text("🧠 Выбор модели (API Live + LiteRT Offline)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableModels.forEach { m ->
                        Surface(
                            color = if (settings.selectedModel == m.id) accentBlue.copy(alpha = 0.25f) else cardDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                settings = settings.copy(selectedModel = m.id, offlineModeEnabled = m.isOffline)
                                projectManager.saveGlobalSettings(settings)
                                showModelPickerDialog = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${m.provider} • Context: ${m.maxInputTokens / 1024}K", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Text(m.badge, color = accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPickerDialog = false }) { Text("Закрыть") }
            }
        )
    }

    // Dialog 3: Multi-Account Switcher
    if (showAccountSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showAccountSwitcherDialog = false },
            containerColor = surfaceDark,
            title = { Text("👤 Переключение аккаунтов Antigravity 2.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.accounts.forEach { acc ->
                        Surface(
                            color = if (acc.isActive) accentBlue.copy(alpha = 0.25f) else cardDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                settings = accountSwitcher.switchActiveAccount(settings, acc.email)
                                scope.launch { quotaMonitor.onLimitsWidgetOpened(settings) }
                                showAccountSwitcherDialog = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(acc.email, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${acc.tier} • ${acc.aiCreditsRemaining} cr", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                if (acc.isActive) Text("ACTIVE", color = emeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountSwitcherDialog = false }) { Text("Готово") }
            }
        )
    }

    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            containerColor = surfaceDark,
            title = { Text("Новый чат проекта в Downloads", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newProjectNameInput,
                    onValueChange = { newProjectNameInput = it },
                    placeholder = { Text("Имя проекта (например: nanodev-agent)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newProjectNameInput.isNotBlank()) {
                        activeSession = projectManager.createOrOpenProjectChat(newProjectNameInput)
                        projects = projectManager.listProjects()
                        newProjectNameInput = ""
                        showNewProjectDialog = false
                    }
                }) { Text("Создать") }
            }
        )
    }
}
