package ru.nanodev.antigravity.sdk

import kotlinx.serialization.Serializable

@Serializable
enum class PermissionPreset(val id: String, val displayName: String, val description: String) {
    REQUEST_REVIEW("request-review", "Request Review", "Каждая команда и запись требует подтверждения"),
    DEFAULT("default", "Default", "Команды внутри песочницы проекта выполняются автоматически"),
    AUTO_PREVIEW("auto", "Auto (Preview)", "Авто-одобрение правок и безопасных команд (v2.23.0+)"),
    TURBO("turbo", "Turbo", "Выполнение всех команд без ограничений и песочницы")
}

@Serializable
enum class AgentExecutionMode(val id: String, val label: String) {
    DEFAULT("default", "Default (Diff Review)"),
    ACCEPT_EDITS("accept-edits", "Accept Edits"),
    AUTO("auto", "Auto Mode"),
    PLAN("plan", "Plan First (/plan)")
}

@Serializable
enum class ColorSchemeTheme(val id: String, val label: String) {
    ANTIGRAVITY_DARK("dark", "Antigravity 2.0 Desktop Dark"),
    TOKYO_NIGHT("tokyo night", "Tokyo Night"),
    SOLARIZED_DARK("solarized dark", "Solarized Dark"),
    LIGHT("light", "Antigravity Light")
}

@Serializable
enum class RunningLightSpeed(val id: String, val durationMs: Int) {
    FAST("fast", 600),
    MEDIUM("medium", 1200),
    SLOW("slow", 2200),
    OFF("off", 0)
}

@Serializable
data class GoogleAccountProfile(
    val email: String,
    val displayName: String,
    val avatarUrl: String = "",
    val oauthAccessToken: String = "",
    val refreshToken: String = "",
    val customApiKey: String = "",
    val tier: String = "Google AI Pro • Antigravity 2.0",
    val aiCreditsRemaining: Int = 485,
    val isActive: Boolean = false
)

@Serializable
data class AntigravitySettings(
    val authMode: String = "account_or_key",
    val geminiApiKey: String = "",
    val accounts: List<GoogleAccountProfile> = listOf(
        GoogleAccountProfile(
            email = "nanodev.lead@gmail.com",
            displayName = "NanoDev Lead (Pro)",
            tier = "Google AI Ultra • Gemini 4 Argon",
            aiCreditsRemaining = 485,
            isActive = true
        ),
        GoogleAccountProfile(
            email = "team.agent@nanodev.ru",
            displayName = "NanoDev Enterprise",
            tier = "Gemini Enterprise • Vertex AI",
            aiCreditsRemaining = 1200,
            isActive = false
        ),
        GoogleAccountProfile(
            email = "backup.dev@gmail.com",
            displayName = "Backup Dev Account",
            tier = "Google AI Pro • Gemini 3.8 Flash",
            aiCreditsRemaining = 300,
            isActive = false
        )
    ),
    val autoRotateAccountOnQuotaLimit: Boolean = true,
    val useVertexAi: Boolean = false,
    val gcpProjectId: String = "",
    val gcpLocation: String = "us-central1",

    // Модели (подгружаются по API)
    val selectedModel: String = "gemini-3.8-flash",
    val thinkingLevel: String = "HIGH",

    // MCP & Remote Desktop
    val mcpLocalServerEnabled: Boolean = true,
    // Remote Control — BETA, по умолчанию выключено
    val remoteControlHostEnabled: Boolean = false,
    // Версия миграций настроек (см. DownloadsProjectManager.loadGlobalSettings)
    val settingsSchemaVersion: Int = 0,

    // Safety & Permissions
    val permissionPreset: PermissionPreset = PermissionPreset.AUTO_PREVIEW,
    val agentMode: AgentExecutionMode = AgentExecutionMode.AUTO,
    val toolPermission: String = "proceed-in-sandbox",
    val artifactReviewPolicy: String = "asks-for-review",
    val enableTerminalSandbox: Boolean = true,
    val allowNonWorkspaceAccess: Boolean = false,

    // Лимит шагов (до 100+ команд) + Автовыключение лимита
    val maxAgentSteps: Int = 100,
    val autoDisableStepLimit: Boolean = false,

    // Управление телефоном (Включается в системный промпт ТОЛЬКО когда включён этот индикатор)
    val phoneControlEnabled: Boolean = false,

    // Голосовой ввод (RU + EN смешанный билингвальный распознаватель)
    val voiceBilingualRuEn: Boolean = true,
    val voiceLiveTranscription: Boolean = true,
    val voiceAutoTechCodeSwitch: Boolean = true,

    // Терминал и Termux Bridge
    val preferTermuxIfInstalled: Boolean = true,
    val enableTermuxApiHardware: Boolean = true,


    // Display, Quotas & Animations
    val colorScheme: ColorSchemeTheme = ColorSchemeTheme.ANTIGRAVITY_DARK,
    val runningLightSpeed: RunningLightSpeed = RunningLightSpeed.FAST,
    val verbosity: String = "high",
    val showActiveTasks: Boolean = true,
    val copyOnSelect: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val queuedMessagesMode: String = "queue",
    val useAiCredits: Boolean = true,
    val enableTelemetry: Boolean = false,
    val showTips: Boolean = true,

    // В самом конце настроек: "Разрешить всё! (С пометкой осторожности подсвечивающейся)"
    val allowAllDangerously: Boolean = false
)
