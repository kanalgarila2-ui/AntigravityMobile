package ru.nanodev.antigravity.ui

import ru.nanodev.antigravity.ui.components.AgTextField
import androidx.compose.foundation.layout.BoxScope
import android.content.Intent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.Download
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import ru.nanodev.antigravity.agy.AgyInstaller
import ru.nanodev.antigravity.auth.ApiKeyStore
import ru.nanodev.antigravity.auth.GoogleResult
import ru.nanodev.antigravity.auth.GoogleSignIn
import ru.nanodev.antigravity.mcp.McpServerEngine
import ru.nanodev.antigravity.net.Connectivity
import ru.nanodev.antigravity.linux.ProotRunner
import ru.nanodev.antigravity.linux.RuntimeInstaller
import ru.nanodev.antigravity.net.GeminiApi
import ru.nanodev.antigravity.net.ModelCheck
import ru.nanodev.antigravity.net.ModelProbe
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
import java.util.Locale
import java.util.UUID

/** Экраны приложения. Нижних вкладок нет: навигация через левую панель. */
enum class AgScreen { CHAT, TERMINAL, TOOLS, CLI, REMOTE, SETTINGS }

/** Модель по умолчанию. */
const val DEFAULT_MODEL_ID = "gemini-3.8-flash"

/** gemini-3.8-flash -> Gemini 3.8 Flash */
fun modelTitle(id: String): String =
    id.removePrefix("models/").split("-").joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercase(Locale.ROOT) }
    }

private fun initials(name: String?, email: String?): String {
    val source = name?.takeIf { it.isNotBlank() } ?: email?.substringBefore("@").orEmpty()
    val parts = source.split(" ", ".", "_", "-").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.ROOT)
        parts.size == 1 -> parts[0].take(1).uppercase(Locale.ROOT)
        else -> "?"
    }
}

private fun loadPhoto(url: String): ImageBitmap? = runCatching {
    GeminiApi.client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
        val bytes = resp.body?.bytes() ?: return@use null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
}.getOrNull()

@Composable
private fun Avatar(name: String?, email: String?, photoUrl: String?, size: Dp) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoUrl) {
        value = if (photoUrl.isNullOrBlank()) null else withContext(Dispatchers.IO) { loadPhoto(photoUrl) }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val bm = bitmap
        if (bm != null) {
            Image(bitmap = bm, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                initials(name, email),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.6f).sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityMainScreen(
    projectManager: DownloadsProjectManager,
    keyStore: ApiKeyStore,
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var settings by remember { mutableStateOf(projectManager.loadGlobalSettings()) }
    fun persist(next: AntigravitySettings) {
        settings = next
        scope.launch(Dispatchers.IO) { projectManager.saveGlobalSettings(next) }
    }

    var projects by remember { mutableStateOf(projectManager.listProjects()) }
    var session by remember {
        mutableStateOf(projects.maxByOrNull { it.updatedAt } ?: projectManager.createOrOpenProjectChat("Личный"))
    }
    var screen by rememberSaveable { mutableStateOf(AgScreen.CHAT) }
    var input by rememberSaveable { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var googleEmail by remember { mutableStateOf(keyStore.googleEmail) }
    var googleName by remember { mutableStateOf(keyStore.googleName) }
    var googlePhoto by remember { mutableStateOf(keyStore.googlePhoto) }
    var accountBusy by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var showAccount by remember { mutableStateOf(false) }

    val listening by voiceEngine.isListening.collectAsState()
    val models by modelRegistry.models.collectAsState()
    val quota by quotaMonitor.snapshot.collectAsState()
    val tasks by scheduleManager.tasks.collectAsState()
    val servers by mcpEngine.externalServers.collectAsState()

    fun saveSession(s: ProjectChatSession) {
        scope.launch(Dispatchers.IO) { projectManager.saveProjectSession(s) }
    }

    fun openProject(p: ProjectChatSession) {
        session = p
        screen = AgScreen.CHAT
        scope.launch { drawerState.close() }
    }

    fun newProject(name: String) {
        session = projectManager.createOrOpenProjectChat(name)
        projects = projectManager.listProjects()
        screen = AgScreen.CHAT
        scope.launch { drawerState.close() }
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

    fun changeAccount() {
        if (accountBusy) return
        accountBusy = true
        accountError = null
        scope.launch {
            when (val r = GoogleSignIn.signIn(context)) {
                is GoogleResult.Success -> {
                    keyStore.saveGoogleAccount(r.email, r.name, r.photoUrl)
                    googleEmail = r.email
                    googleName = r.name
                    googlePhoto = r.photoUrl
                }
                is GoogleResult.Failed -> accountError = r.reason
                GoogleResult.Cancelled -> Unit
                GoogleResult.NotConfigured -> accountError = "Google-вход не настроен"
            }
            accountBusy = false
        }
    }

    fun signOutGoogle() {
        keyStore.forgetGoogleAccount()
        googleEmail = null
        googleName = null
        googlePhoto = null
        showAccount = false
    }

    val modelOptions = (listOf(settings.selectedModel) + models.map { it.id })
        .filter { it.isNotBlank() }
        .distinct()
        .map { it to modelTitle(it) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
            ) {
                DrawerContent(
                    projects = projects,
                    activeFolder = session.folderPath,
                    current = screen,
                    onOpenProject = { openProject(it) },
                    onNewProject = { newProject(it) },
                    onScreen = { target ->
                        screen = target
                        scope.launch { drawerState.close() }
                    },
                    onOpenSettings = {
                        screen = AgScreen.SETTINGS
                        scope.launch { drawerState.close() }
                    },
                    googleEmail = googleEmail,
                    googleName = googleName,
                    googlePhoto = googlePhoto,
                    onAccount = { showAccount = true }
                )
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            if (screen == AgScreen.CHAT) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Меню")
                                }
                            } else {
                                IconButton(onClick = { screen = AgScreen.CHAT }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                }
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    when (screen) {
                                        AgScreen.CHAT -> session.projectName
                                        AgScreen.TERMINAL -> "Терминал"
                                        AgScreen.TOOLS -> "Инструменты"
                                        AgScreen.CLI -> "Antigravity CLI"
                                        AgScreen.REMOTE -> "Remote"
                                        AgScreen.SETTINGS -> "Настройки"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (screen == AgScreen.CHAT) {
                                    Text(
                                        if (online) "Онлайн" else "Офлайн: только просмотр",
                                        fontSize = 11.sp,
                                        color = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val inSpec = if (forward) AgAnim.TAB_CONTENT_SLIDE_LEFT else AgAnim.TAB_CONTENT_SLIDE_RIGHT
                        (fadeIn(inSpec.spec<Float>()) +
                            slideInHorizontally(inSpec.spec<IntOffset>()) { if (forward) it / 8 else -it / 8 }) togetherWith
                            fadeOut(AgAnim.TAB_CONTENT_FADE.spec<Float>())
                    },
                    label = "screens"
                ) { current ->
                    when (current) {
                        AgScreen.CHAT -> ChatPane(
                            session = session,
                            online = online,
                            running = running,
                            input = input,
                            error = error,
                            listening = listening,
                            modelId = settings.selectedModel,
                            modelOptions = modelOptions,
                            onPickModel = { id -> persist(settings.copy(selectedModel = id)) },
                            onInput = { input = it },
                            onSend = { send() },
                            onMic = { toggleMic() }
                        )
                        AgScreen.TERMINAL -> TerminalPane(shellEngine, session, settings)
                        AgScreen.TOOLS -> ToolsPane(
                            creditsLeft = quota.aiCreditsRemaining,
                            creditsMax = quota.aiCreditsMax,
                            servers = servers.map { "${it.name} · ${it.transport} · ${if (it.enabled) "вкл" else "выкл"}" },
                            tasks = tasks.map { it.id to "${it.title} · ${it.lastRunStatus}" },
                            onRunTask = { scheduleManager.runTaskImmediately(it) }
                        )
                        AgScreen.CLI -> AgySetupPane()
                        AgScreen.REMOTE -> RemotePane(
                            enabled = settings.remoteControlHostEnabled,
                            onEnable = { on ->
                                persist(settings.copy(remoteControlHostEnabled = on))
                                RemoteDesktopService.setEnabled(context, on)
                            }
                        )
                        AgScreen.SETTINGS -> SettingsPane(
                            settings = settings,
                            models = modelOptions,
                            onChange = { persist(it) },
                            onRefreshModels = { scope.launch { modelRegistry.refreshModelsFromApi(settings) } },
                            onLogout = onLogout
                        )
                    }
                }
            }

            // Оверлей аккаунта: затемнение и карточка снизу
            AnimatedVisibility(
                visible = showAccount,
                enter = fadeIn(AgAnim.DIALOG_SCRIM_FADE.spec<Float>()),
                exit = fadeOut(AgAnim.DIALOG_EXIT.spec<Float>())
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showAccount = false }
                )
            }
            AnimatedVisibility(
                visible = showAccount,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(AgAnim.SHEET_EXPAND.spec<IntOffset>()) { it } +
                    scaleIn(AgAnim.DIALOG_SCALE_IN.spec<Float>()),
                exit = slideOutVertically(AgAnim.DIALOG_EXIT.spec<IntOffset>()) { it } +
                    fadeOut(AgAnim.DIALOG_EXIT.spec<Float>())
            ) {
                AccountCard(
                    name = googleName,
                    email = googleEmail,
                    photo = googlePhoto,
                    busy = accountBusy,
                    error = accountError,
                    onChange = { changeAccount() },
                    onSignOut = { signOutGoogle() },
                    onClose = { showAccount = false }
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    name: String?,
    email: String?,
    photo: String?,
    busy: Boolean,
    error: String?,
    onChange: () -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .glass(RoundedCornerShape(GlassCornerRadius), alpha = 0.16f)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, email, photo, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name ?: "Аккаунт не выбран", fontWeight = FontWeight.SemiBold)
                Text(
                    email ?: "Вход через Google не выполнен",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Check, contentDescription = "Закрыть") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        OutlinedButton(onClick = onChange, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.SwitchAccount, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (busy) "Вход…" else "Сменить аккаунт")
        }
        if (email != null) {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Выйти из аккаунта")
            }
        }
    }
}

@Composable
private fun DrawerContent(
    projects: List<ProjectChatSession>,
    activeFolder: String,
    current: AgScreen,
    onOpenProject: (ProjectChatSession) -> Unit,
    onNewProject: (String) -> Unit,
    onScreen: (AgScreen) -> Unit,
    onOpenSettings: () -> Unit,
    googleEmail: String?,
    googleName: String?,
    googlePhoto: String?,
    onAccount: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    val entered = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = entered,
        enter = slideInHorizontally(AgAnim.DRAWER_SLIDE.spec<IntOffset>()) { -it / 4 } +
            fadeIn(AgAnim.DRAWER_SLIDE.spec<Float>())
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Antigravity", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                }
            }
            Spacer(Modifier.height(8.dp))

            Text(
                "Проекты",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(projects, key = { it.folderPath }) { p ->
                    val isOpen = expanded == p.folderPath
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { expanded = if (isOpen) null else p.folderPath }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                p.projectName,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                fontWeight = if (p.folderPath == activeFolder) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Icon(
                                if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        AnimatedVisibility(
                            visible = isOpen,
                            enter = expandVertically(AgAnim.SETTINGS_SECTION_EXPAND.spec<IntSize>()) +
                                fadeIn(AgAnim.SETTINGS_SECTION_EXPAND.spec<Float>()),
                            exit = shrinkVertically(AgAnim.SETTINGS_SECTION_EXPAND.spec<IntSize>()) +
                                fadeOut(AgAnim.SETTINGS_SECTION_EXPAND.spec<Float>())
                        ) {
                            Column(Modifier.padding(start = 34.dp, bottom = 6.dp)) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenProject(p) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Открыть чат", fontSize = 13.sp)
                                }
                                Text(
                                    "Папка: ${p.folderPath.substringAfterLast('/')}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AgTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Новый проект"
                )
                AnimatedVisibility(
                    visible = newName.isNotBlank(),
                    enter = fadeIn(AgAnim.CHAT_PROJECT_SHEET_SLIDE.spec<Float>()) +
                        slideInHorizontally(AgAnim.CHAT_PROJECT_SHEET_SLIDE.spec<IntOffset>()) { it / 2 }
                ) {
                    IconButton(onClick = {
                        onNewProject(newName.trim())
                        newName = ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Создать проект")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            DrawerItem("Терминал", Icons.Default.Terminal, current == AgScreen.TERMINAL) { onScreen(AgScreen.TERMINAL) }
            DrawerItem("Инструменты", Icons.Default.Build, current == AgScreen.TOOLS) { onScreen(AgScreen.TOOLS) }
            DrawerItem("Antigravity CLI", Icons.Default.Download, current == AgScreen.CLI) { onScreen(AgScreen.CLI) }
            DrawerItem("Remote (Beta)", Icons.Default.DesktopWindows, current == AgScreen.REMOTE) { onScreen(AgScreen.REMOTE) }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAccount() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(googleName, googleEmail, googlePhoto, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(googleName ?: "Войти в аккаунт", maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        googleEmail ?: "Нажмите, чтобы выбрать аккаунт",
                        maxLines = 1,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.SwitchAccount, contentDescription = "Аккаунт", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.97f else 1f, AgAnim.CARD_PRESS_SCALE.spec<Float>(), label = "press")
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        AgAnim.NAV_INDICATOR_MORPH.spec<Color>(),
        label = "indicator"
    )
    val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, AgAnim.NAV_ICON_BOUNCE.spec<Float>(), label = "icon")
    val labelAlpha by animateFloatAsState(if (selected) 1f else 0.75f, AgAnim.NAV_LABEL_FADE.spec<Float>(), label = "label")
    Row(
        Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = source, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp).scale(iconScale))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, modifier = Modifier.alpha(labelAlpha))
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, alpha: Float = 0.10f, content: @Composable () -> Unit) {
    val entered = remember { MutableTransitionState(false).apply { targetState = true } }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(AgAnim.GLASS_BORDER_SHIMMER.spec<Float>(), RepeatMode.Reverse),
        label = "shimmer-alpha"
    )
    AnimatedVisibility(
        visibleState = entered,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn(AgAnim.CARD_ENTER_STAGGER.spec<Float>()) +
            slideInVertically(AgAnim.CARD_ENTER_STAGGER.spec<IntOffset>()) { it / 8 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(GlassCornerRadius), alpha = alpha, shimmer = shimmer)
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
    modelId: String,
    modelOptions: List<Pair<String, String>>,
    onPickModel: (String) -> Unit,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(session.messages.size) {
        if (session.messages.isNotEmpty()) listState.animateScrollToItem(session.messages.size - 1)
    }
    val showScrollDown by remember { derivedStateOf { listState.canScrollForward } }
    val floatY by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(AgAnim.EMPTY_STATE_FLOAT.spec<Float>(), RepeatMode.Reverse),
        label = "float-y"
    )
    val inputSource = remember { MutableInteractionSource() }
    val focused by inputSource.collectIsFocusedAsState()
    val glow by animateFloatAsState(if (focused) 0.2f else 0.1f, AgAnim.CHAT_INPUT_FOCUS_GLOW.spec<Float>(), label = "glow")

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

        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (session.messages.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 48.dp).offset { IntOffset(0, floatY.toInt()) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Новый диалог", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text("Введите запрос. Строка «!команда» запускает shell.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                items(session.messages, key = { it.id }) { msg -> Bubble(msg) }
                if (running) {
                    item { TypingIndicator() }
                }
            }
            ScrollDownButton(
                visible = showScrollDown,
                onClick = { if (session.messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(session.messages.size - 1) } }
            )
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(AgAnim.SNACKBAR_SLIDE.spec<Float>()) +
                slideInVertically(AgAnim.SNACKBAR_SLIDE.spec<IntOffset>()) { it / 2 },
            exit = fadeOut(AgAnim.SNACKBAR_SLIDE.spec<Float>())
        ) {
            Text(
                error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        GlassCard(Modifier.padding(bottom = 8.dp), alpha = glow) {
            ModelPicker(modelId, modelOptions, onPickModel)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.weight(1f),
                    placeholder = if (online) "Сообщение агенту" else "Офлайн: отправка недоступна",
                    enabled = online || input.startsWith("!"),
                    singleLine = false,
                    maxLines = 4,
                    interactionSource = inputSource
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = sendTint)
                }
            }
        }
    }
}

@Composable
private fun ModelPicker(modelId: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(modelTitle(modelId), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Выбрать модель", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    trailingIcon = { if (id == modelId) Icon(Icons.Default.Check, contentDescription = null) },
                    onClick = {
                        onPick(id)
                        open = false
                    }
                )
            }
        }
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
    Text("Агент работает…", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.alpha(alpha))
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    var traceOpen by remember { mutableStateOf(false) }
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
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .animateContentSize(AgAnim.CHAT_BUBBLE_EXPAND.spec<IntSize>())
                    .clickable(enabled = msg.thoughtTrace.isNotEmpty()) { traceOpen = !traceOpen }
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (msg.toolName.isNotEmpty()) {
                        Text(msg.toolName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(msg.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    AnimatedVisibility(
                        visible = traceOpen && msg.thoughtTrace.isNotEmpty(),
                        enter = expandVertically(AgAnim.CHAT_TOOL_CARD_EXPAND.spec<IntSize>()) +
                            fadeIn(AgAnim.CHAT_TOOL_CARD_EXPAND.spec<Float>()),
                        exit = shrinkVertically(AgAnim.CHAT_TOOL_CARD_EXPAND.spec<IntSize>()) +
                            fadeOut(AgAnim.CHAT_TOOL_CARD_EXPAND.spec<Float>())
                    ) {
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
    session: ProjectChatSession,
    settings: AntigravitySettings
) {
    val scope = rememberCoroutineScope()
    var cmd by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var lines by remember { mutableStateOf(listOf("Antigravity shell · введите команду")) }
    val blink by rememberInfiniteTransition(label = "prompt").animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(AgAnim.TERMINAL_PROMPT_BLINK.spec<Float>(), RepeatMode.Reverse),
        label = "prompt-blink"
    )
    val spin by rememberInfiniteTransition(label = "run").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(AgAnim.TOOL_RUN_SPIN.spec<Float>(), RepeatMode.Restart),
        label = "run-spin"
    )

    fun run() {
        val line = cmd.trim()
        if (line.isEmpty() || busy) return
        lines = lines + "$ $line"
        cmd = ""
        busy = true
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
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().glass(RoundedCornerShape(GlassCornerRadius), alpha = 0.10f).padding(12.dp)) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(lines) { l -> FadeInLine(l) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AgTextField(
                value = cmd,
                onValueChange = { cmd = it },
                modifier = Modifier.weight(1f),
                placeholder = "ls -la, git status, python3 --version",
                leading = { Text("$ ", fontFamily = FontFamily.Monospace, modifier = Modifier.alpha(blink)) }
            )
            IconButton(onClick = { run() }, enabled = cmd.isNotBlank() && !busy) {
                Icon(
                    if (busy) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = "Выполнить",
                    modifier = Modifier.rotate(if (busy) spin else 0f)
                )
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
    val smooth by animateFloatAsState(target, AgAnim.PROGRESS_LINEAR_SMOOTH.spec<Float>(), label = "progress")
    val percent by animateFloatAsState(target * 100f, AgAnim.QUOTA_BAR_FILL.spec<Float>(), label = "percent")
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            Text("AI Credits", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            LinearProgressIndicator(progress = { smooth }, modifier = Modifier.fillMaxWidth())
            Text(
                "$creditsLeft / $creditsMax · ${percent.toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        GlassCard {
            Text("MCP-серверы", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (servers.isEmpty()) {
                Text("Внешние серверы не настроены", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            servers.forEach { row ->
                val appear = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(visibleState = appear, enter = fadeIn(AgAnim.TOOL_LIST_ENTER.spec<Float>())) {
                    Text(row, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        GlassCard {
            Text("Расписание", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (tasks.isEmpty()) {
                Text("Задач нет", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            tasks.forEach { (id, label) ->
                val appear = remember { MutableTransitionState(false).apply { targetState = true } }
                val failed = label.contains("ошиб", ignoreCase = true) || label.contains("error", ignoreCase = true)
                val labelColor by animateColorAsState(
                    if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    AgAnim.TASK_STATUS_COLOR.spec<Color>(),
                    label = "task-status"
                )
                AnimatedVisibility(visibleState = appear, enter = fadeIn(AgAnim.TOOL_LIST_ENTER.spec<Float>())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 12.sp, color = labelColor, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRunTask(id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Запустить сейчас")
                        }
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
    val steps by animateFloatAsState(settings.maxAgentSteps.toFloat(), AgAnim.SLIDER_VALUE_TWEEN.spec<Float>(), label = "steps")
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
            OutlinedButton(onClick = onLogout) { Text("Сменить ключ") }
        }

        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Модель", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefreshModels) { Text("Обновить") }
            }
            models.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (id, name) ->
                        AnimChip(
                            selected = settings.selectedModel == id,
                            label = name,
                            anim = AgAnim.MODEL_CHIP_SWAP,
                            onClick = { onChange(settings.copy(selectedModel = id)) }
                        )
                    }
                }
            }
        }

        GlassCard {
            val probeScope = rememberCoroutineScope()
            var probe by remember { mutableStateOf<String?>(null) }
            OutlinedButton(onClick = {
                probe = "Проверяем…"
                probeScope.launch { probe = describeCheck(ModelProbe.check(settings.geminiApiKey, settings.selectedModel)) }
            }) { Text("Проверить ${modelTitle(settings.selectedModel)}") }
            probe?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        GlassCard {
            Text("Разрешения", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionPreset.entries.forEach { p ->
                    AnimChip(
                        selected = settings.permissionPreset == p,
                        label = p.displayName,
                        anim = AgAnim.CHIP_SELECT,
                        onClick = { onChange(settings.copy(permissionPreset = p)) }
                    )
                }
            }
            Text(settings.permissionPreset.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        GlassCard {
            Text(
                "Лимит шагов: ${steps.toInt()}",
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
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = warnAlpha)
                )
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
private fun AnimChip(selected: Boolean, label: String, anim: AgAnim, onClick: () -> Unit) {
    val chipScale by animateFloatAsState(if (selected) 1.04f else 1f, anim.spec<Float>(), label = "chip-scale")
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.scale(chipScale),
        label = { Text(label, fontSize = 12.sp, maxLines = 1) }
    )
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    badge: (@Composable () -> Unit)? = null
) {
    val rowBackground by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
        AgAnim.TOGGLE_CARD_COLOR.spec<Color>(),
        label = "toggle-bg"
    )
    val thumbScale by animateFloatAsState(if (checked) 1f else 0.94f, AgAnim.SWITCH_THUMB_SLIDE.spec<Float>(), label = "thumb")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp),
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
        Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.scale(thumbScale))
    }
}

/** Мастер Antigravity CLI: шаг 1 — скачать и проверить, шаг 2 — вход (его выполняет сам agy). */
@Composable
private fun AgySetupPane() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf<AgyInstaller.Phase>(AgyInstaller.Phase.Idle) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            Text("Шаг 1 из 2. Скачать Antigravity CLI", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Файл берётся с официального релиза Google на GitHub. Приложение считает SHA-256 и сверяет его с опубликованным.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (val p = phase) {
                AgyInstaller.Phase.Idle -> Button(onClick = {
                    phase = AgyInstaller.Phase.Downloading(0, 0)
                    scope.launch { AgyInstaller.install(context) { phase = it } }
                }) { Text("Скачать") }

                is AgyInstaller.Phase.Downloading -> {
                    LinearProgressIndicator(
                        progress = { if (p.total > 0) p.done.toFloat() / p.total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DownloadStats(p.done, p.total, p.bytesPerSec)
                }

                is AgyInstaller.Phase.Done -> {
                    Text("Готово: ${p.path}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("SHA-256: ${p.sha256}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Рабочая папка agy: ${AgyInstaller.workspaceDir(context).absolutePath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (p.verified) "Совпадает с опубликованной суммой" else "Опубликованной суммы нет. Сверьте хэш вручную",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is AgyInstaller.Phase.Failed -> {
                    Text(p.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { phase = AgyInstaller.Phase.Idle }) { Text("Повторить") }
                }
            }
        }

        LinuxRuntimeCard()

        if (phase is AgyInstaller.Phase.Done) {
            GlassCard {
                Text("Шаг 2 из 2. Вход", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Вход выполняет сам agy: он откроет свою страницу Google и сохранит токен у себя. " +
                        "Вход в это приложение в agy не переносится.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString("agy")) }) { Text("Скопировать команду: agy") }
                OutlinedButton(onClick = {
                    context.packageManager.getLaunchIntentForPackage("com.termux")
                        ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) { Text("Открыть Termux") }
            }
        }
    }
}

private fun mebibytes(bytes: Long): String = String.format(java.util.Locale.ROOT, "%.1f", bytes / 1_048_576.0)


/** Кнопка «вниз»: обычная компонента, выравнивание внутри собственного Box. */
@Composable
private fun ScrollDownButton(visible: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.padding(8.dp),
            enter = fadeIn(AgAnim.CHAT_SCROLL_TO_END.spec<Float>()) + scaleIn(AgAnim.CHAT_SCROLL_TO_END.spec<Float>()),
            exit = fadeOut(AgAnim.CHAT_SCROLL_TO_END.spec<Float>())
        ) {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "К последнему сообщению")
            }
        }
    }
}

private fun describeCheck(c: ModelCheck): String = when (c) {
    ModelCheck.Available -> "Модель доступна по вашему ключу"
    ModelCheck.NotFound -> "Этой модели нет в публичном API. Выберите другую"
    is ModelCheck.Invalid -> c.reason
    is ModelCheck.Failed -> "Ошибка: ${c.reason}"
}

/** Шаг 3: среда Linux (манифест вшит), проверка agy и вход в agy внутри среды. */
@Composable
private fun LinuxRuntimeCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf<RuntimeInstaller.Phase>(RuntimeInstaller.Phase.Idle) }
    var result by remember { mutableStateOf<ProotRunner.Result?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loginProcess by remember { mutableStateOf<Process?>(null) }
    var loginLines by remember { mutableStateOf(listOf<String>()) }
    GlassCard {
        Text("Шаг 3. Среда Linux", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "PRoot и Ubuntu приложение скачивает по вшитому манифесту и проверяет SHA-256. В APK они не вшиваются.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (val p = phase) {
            RuntimeInstaller.Phase.Idle -> Unit
            is RuntimeInstaller.Phase.Downloading -> {
                LinearProgressIndicator(
                    progress = { if (p.total > 0) p.done.toFloat() / p.total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                DownloadStats(p.done, p.total, p.bytesPerSec, label = p.component)
            }
            RuntimeInstaller.Phase.Ready -> Text("Среда готова", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            is RuntimeInstaller.Phase.Failed -> Text(p.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                phase = RuntimeInstaller.Phase.Downloading("манифест", 0, 0)
                scope.launch { RuntimeInstaller.install(context) { phase = it } }
            },
            enabled = phase !is RuntimeInstaller.Phase.Downloading
        ) { Text("Установить среду") }
        OutlinedButton(
            onClick = {
                busy = true
                scope.launch {
                    result = runCatching { ProotRunner.runAgy(context, listOf("--version")) }
                        .getOrElse { ProotRunner.Result(-1, it.message ?: "ошибка запуска") }
                    busy = false
                }
            },
            enabled = !busy && phase == RuntimeInstaller.Phase.Ready
        ) { Text("Проверить agy (--version)") }
        OutlinedButton(
            onClick = {
                busy = true
                scope.launch {
                    result = runCatching { ProotRunner.runAgy(context, listOf("--help")) }
                        .getOrElse { ProotRunner.Result(-1, it.message ?: "ошибка запуска") }
                    busy = false
                }
            },
            enabled = !busy && phase == RuntimeInstaller.Phase.Ready
        ) { Text("Справка agy (--help)") }
        result?.let { r ->
            Text(
                "Код выхода: ${r.exitCode}\n${r.output.take(2000)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (phase == RuntimeInstaller.Phase.Ready) {
            Text("Вход в agy", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "agy запускается внутри среды и выводит ссылку для входа. Открой её в браузере. Когда вход завершён, процесс сам завершится.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    loginLines = emptyList()
                    runCatching {
                        ProotRunner.start(context, ProotRunner.LOGIN_ARGS) { line ->
                            scope.launch { loginLines = loginLines + line }
                        }
                    }.onSuccess { proc ->
                        loginProcess = proc
                        scope.launch(Dispatchers.IO) {
                            val code = proc.waitFor()
                            withContext(Dispatchers.Main) {
                                loginLines = loginLines + "[процесс входа завершён, код $code]"
                                loginProcess = null
                            }
                        }
                    }.onFailure { loginLines = listOf("Ошибка запуска: ${it.message}") }
                },
                enabled = loginProcess == null
            ) { Text("Войти в agy") }
            if (loginProcess != null) {
                OutlinedButton(onClick = {
                    loginProcess?.destroy()
                    loginProcess = null
                }) { Text("Остановить вход") }
            }
            loginLines.takeLast(40).forEach { line ->
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}


/** Строка терминала: плавное появление. Отдельная компонента, без лишних получателей. */
@Composable
private fun FadeInLine(text: String) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = visible, enter = fadeIn(AgAnim.TERMINAL_LINE_APPEAR.spec<Float>())) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Панель загрузки: процент, сколько скачано из скольки, скорость. */
@Composable
private fun DownloadStats(done: Long, total: Long, bytesPerSec: Long, label: String = "") {
    val percent = if (total > 0) (done * 100 / total).toInt() else 0
    val speed = if (bytesPerSec > 0) String.format(java.util.Locale.ROOT, "%.1f МБ/с", bytesPerSec / 1_048_576.0) else "— МБ/с"
    val prefix = if (label.isNotEmpty()) "$label · " else ""
    val size = "${mebibytes(done)} из ${if (total > 0) mebibytes(total) else "?"} МБ"
    Text(
        "$prefix$percent% · $size · $speed",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
