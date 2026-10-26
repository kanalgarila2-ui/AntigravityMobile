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
import androidx.activity.result.contract.ActivityResultContracts
import ru.nanodev.antigravity.accounts.AccountSwitcherManager
import ru.nanodev.antigravity.mcp.McpServerEngine
import ru.nanodev.antigravity.offline.OfflineModelEngine
import ru.nanodev.antigravity.phone.PhoneControlManager
import ru.nanodev.antigravity.remotedesktop.RemoteDesktopService
import ru.nanodev.antigravity.runtime.AntigravityShellEngine
import ru.nanodev.antigravity.scheduler.ScheduleManager
import ru.nanodev.antigravity.sdk.AntigravityAgentOrchestrator
import ru.nanodev.antigravity.sdk.DynamicModelRegistry
import ru.nanodev.antigravity.sdk.QuotaLimitsMonitor
import ru.nanodev.antigravity.storage.DownloadsProjectManager
import ru.nanodev.antigravity.ui.AntigravityMainScreen
import ru.nanodev.antigravity.unlocker.AgyUnlockerService
import ru.nanodev.antigravity.voice.BilingualVoiceEngine

class MainActivity : ComponentActivity() {

    private lateinit var projectManager: DownloadsProjectManager
    private lateinit var shellEngine: AntigravityShellEngine
    private lateinit var phoneControl: PhoneControlManager
    private lateinit var voiceEngine: BilingualVoiceEngine
    private lateinit var mcpEngine: McpServerEngine
    private lateinit var offlineEngine: OfflineModelEngine
    private lateinit var scheduleManager: ScheduleManager
    private lateinit var accountSwitcher: AccountSwitcherManager
    private lateinit var modelRegistry: DynamicModelRegistry
    private lateinit var quotaMonitor: QuotaLimitsMonitor
    private lateinit var orchestrator: AntigravityAgentOrchestrator

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectManager = DownloadsProjectManager(this)
        val rootDir = projectManager.getAntigravityRootDir()

        shellEngine = AntigravityShellEngine(this)
        phoneControl = PhoneControlManager(this)
        voiceEngine = BilingualVoiceEngine(this)
        mcpEngine = McpServerEngine(this, rootDir)
        offlineEngine = OfflineModelEngine(this, rootDir)
        scheduleManager = ScheduleManager(this, rootDir)
        accountSwitcher = AccountSwitcherManager(projectManager)
        modelRegistry = DynamicModelRegistry()
        quotaMonitor = QuotaLimitsMonitor()

        orchestrator = AntigravityAgentOrchestrator(
            projectManager = projectManager,
            shellEngine = shellEngine,
            phoneControl = phoneControl,
            mcpEngine = mcpEngine,
            offlineEngine = offlineEngine,
            quotaMonitor = quotaMonitor
        )

        requestNecessaryPermissions()
        startService(Intent(this, AgyUnlockerService::class.java))
        startService(Intent(this, RemoteDesktopService::class.java))

        // Запускаем 5-минутный цикл автообновления лимитов (/usage)
        quotaMonitor.startAutoRefreshLoop { projectManager.loadGlobalSettings() }

        val quickAction = intent?.getStringExtra("quick_action")

        setContent {
            AntigravityMainScreen(
                projectManager = projectManager,
                shellEngine = shellEngine,
                voiceEngine = voiceEngine,
                mcpEngine = mcpEngine,
                offlineEngine = offlineEngine,
                scheduleManager = scheduleManager,
                accountSwitcher = accountSwitcher,
                modelRegistry = modelRegistry,
                quotaMonitor = quotaMonitor,
                orchestrator = orchestrator,
                initialQuickAction = quickAction,
                onRequestManageStorage = { requestManageAllFilesAccess() }
            )
        }
    }

    private fun requestNecessaryPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)
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

    override fun onDestroy() {
        voiceEngine.stopListening()
        super.onDestroy()
    }
}
