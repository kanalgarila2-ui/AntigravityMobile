package ru.nanodev.antigravity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.nanodev.antigravity.auth.ApiKeyStore
import ru.nanodev.antigravity.crash.CrashLogger
import ru.nanodev.antigravity.ui.crash.CrashReportScreen
import ru.nanodev.antigravity.mcp.McpServerEngine
import ru.nanodev.antigravity.net.Connectivity
import ru.nanodev.antigravity.phone.PhoneControlManager
import ru.nanodev.antigravity.remotedesktop.RemoteDesktopService
import ru.nanodev.antigravity.runtime.AntigravityShellEngine
import ru.nanodev.antigravity.scheduler.ScheduleManager
import ru.nanodev.antigravity.sdk.AntigravityAgentOrchestrator
import ru.nanodev.antigravity.sdk.DynamicModelRegistry
import ru.nanodev.antigravity.sdk.QuotaLimitsMonitor
import ru.nanodev.antigravity.storage.DownloadsProjectManager
import ru.nanodev.antigravity.ui.AntigravityMainScreen
import ru.nanodev.antigravity.ui.motion.AgAnim
import ru.nanodev.antigravity.ui.onboarding.OnboardingScreen
import ru.nanodev.antigravity.ui.theme.AgBackdrop
import ru.nanodev.antigravity.ui.theme.AgTheme
import ru.nanodev.antigravity.voice.BilingualVoiceEngine

class MainActivity : ComponentActivity() {

    private lateinit var keyStore: ApiKeyStore
    private lateinit var projectManager: DownloadsProjectManager
    private lateinit var shellEngine: AntigravityShellEngine
    private lateinit var voiceEngine: BilingualVoiceEngine
    private lateinit var mcpEngine: McpServerEngine
    private lateinit var scheduleManager: ScheduleManager
    private lateinit var modelRegistry: DynamicModelRegistry
    private lateinit var quotaMonitor: QuotaLimitsMonitor
    private lateinit var orchestrator: AntigravityAgentOrchestrator

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Сеть и хранилище поднимаем первыми: остальное на них опирается
        Connectivity.start(this)
        keyStore = ApiKeyStore(this)

        projectManager = DownloadsProjectManager(this)
        val rootDir = projectManager.getAntigravityRootDir()

        shellEngine = AntigravityShellEngine(this)
        voiceEngine = BilingualVoiceEngine(this)
        mcpEngine = McpServerEngine(this, rootDir)
        scheduleManager = ScheduleManager(this, rootDir)
        modelRegistry = DynamicModelRegistry()
        quotaMonitor = QuotaLimitsMonitor()

        orchestrator = AntigravityAgentOrchestrator(
            projectManager = projectManager,
            shellEngine = shellEngine,
            phoneControl = PhoneControlManager(this),
            mcpEngine = mcpEngine,
            quotaMonitor = quotaMonitor
        )

        requestNecessaryPermissions()

        // Remote Control (Beta) стартует только если включён в настройках
        if (projectManager.loadGlobalSettings().remoteControlHostEnabled) {
            RemoteDesktopService.setEnabled(this, true)
        }

        // Лимиты обновляются в фоне только при наличии сети (см. QuotaLimitsMonitor)
        quotaMonitor.startAutoRefreshLoop { projectManager.loadGlobalSettings() }

        setContent {
            AgTheme {
                AgBackdrop {
                    // Если прошлый запуск упал, сначала показываем отчёт
                    val crash = remember { mutableStateOf(CrashLogger.read(this@MainActivity)) }
                    val report = crash.value
                    if (report != null) {
                        CrashReportScreen(
                            report = report,
                            onDismiss = {
                                CrashLogger.clear(this@MainActivity)
                                crash.value = null
                            }
                        )
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }

    @Composable
    private fun AppRoot() {
        var authed by remember { mutableStateOf(keyStore.hasKey) }

        AnimatedContent(
            targetState = authed,
            transitionSpec = {
                fadeIn(AgAnim.TAB_CONTENT_FADE.spec<Float>()) togetherWith
                    fadeOut(AgAnim.TAB_CONTENT_FADE.spec<Float>())
            },
            label = "root"
        ) { isAuthed ->
            if (isAuthed) {
                AntigravityMainScreen(
                    projectManager = projectManager,
                    shellEngine = shellEngine,
                    voiceEngine = voiceEngine,
                    mcpEngine = mcpEngine,
                    scheduleManager = scheduleManager,
                    modelRegistry = modelRegistry,
                    quotaMonitor = quotaMonitor,
                    orchestrator = orchestrator,
                    onLogout = {
                        keyStore.clear()
                        authed = false
                    }
                )
            } else {
                OnboardingScreen(
                    keyStore = keyStore,
                    onAuthenticated = { authed = true }
                )
            }
        }
    }

    private fun requestNecessaryPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun requestManageAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }
}
