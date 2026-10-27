package ru.nanodev.antigravity.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nanodev.antigravity.mcp.McpServerEngine
import ru.nanodev.antigravity.net.Connectivity
import ru.nanodev.antigravity.net.GeminiApi
import ru.nanodev.antigravity.phone.PhoneControlManager
import ru.nanodev.antigravity.runtime.AntigravityShellEngine
import ru.nanodev.antigravity.storage.ChatMessage
import ru.nanodev.antigravity.storage.DownloadsProjectManager
import ru.nanodev.antigravity.storage.ProjectChatSession
import java.io.File
import java.util.UUID

/**
 * Один ход диалога: команда shell, /usage, офлайн-модель или запрос к Gemini API.
 * Цикла с инструментами (агент сам вызывает команды) в этой версии нет.
 */
class AntigravityAgentOrchestrator(
    private val projectManager: DownloadsProjectManager,
    private val shellEngine: AntigravityShellEngine,
    private val phoneControl: PhoneControlManager,
    private val mcpEngine: McpServerEngine,
    private val quotaMonitor: QuotaLimitsMonitor
) {

    suspend fun runAgentTurn(
        userPrompt: String,
        session: ProjectChatSession,
        settings: AntigravitySettings,
        onStepUpdate: (ChatMessage) -> Unit
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        val out = mutableListOf<ChatMessage>()
        fun emit(msg: ChatMessage) {
            onStepUpdate(msg)
            out.add(msg)
        }

        val workspaceDir = File(session.folderPath, "workspace").apply { mkdirs() }
        val trimmed = userPrompt.trim()

        // Прямая команда shell: !cmd или "$ cmd"
        if (userPrompt.startsWith("!") || userPrompt.startsWith("$ ")) {
            val rawCmd = userPrompt.removePrefix("!").removePrefix("$ ").trim()
            val res = shellEngine.execute(rawCmd, workspaceDir, settings.preferTermuxIfInstalled)
            emit(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "tool",
                    toolName = "run_command (${res.executedVia})",
                    content = if (res.stderr.isNotEmpty()) "${res.stdout}\n[stderr]: ${res.stderr}" else res.stdout,
                    thoughtTrace = "Выполнено в ${workspaceDir.absolutePath}, код выхода ${res.exitCode}",
                    stepIndex = 1
                )
            )
            return@withContext out
        }

        // Нет сети: локальные команды работают, запросы к модели — нет
        if (!Connectivity.online.value) {
            emit(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "agent",
                    content = "Нет подключения к интернету. Ответ модели сейчас недоступен. " +
                        "Чаты открываются без сети, отправка вернётся после восстановления связи.",
                    stepIndex = 1
                )
            )
            return@withContext out
        }

        // /usage, /quota, /credits — актуальные лимиты
        if (trimmed == "/usage" || trimmed == "/quota" || trimmed == "/credits") {
            quotaMonitor.refreshLimitsNow(settings, reason = "slash-command")
            val snap = quotaMonitor.snapshot.value
            emit(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "agent",
                    content = buildString {
                        appendLine("Лимиты и AI Credits")
                        appendLine("Аккаунт: ${snap.accountEmail} (${snap.planName})")
                        appendLine("AI Credits: ${snap.aiCreditsRemaining} / ${snap.aiCreditsMax}")
                        snap.modelQuotas.forEach { q ->
                            appendLine("${q.modelLabel}: ${q.remainingRequests}/${q.maxRequests} запросов, сброс через ${q.resetInMinutes} мин")
                        }
                    },
                    thoughtTrace = "Данные из QuotaLimitsMonitor",
                    stepIndex = 1
                )
            )
            return@withContext out
        }

        // Онлайн: Gemini API
        val activeAcc = settings.accounts.firstOrNull { it.isActive }
        val apiKey = activeAcc?.customApiKey?.takeIf { it.isNotBlank() } ?: settings.geminiApiKey
        val bearer = activeAcc?.oauthAccessToken

        if (apiKey.isBlank() && bearer.isNullOrBlank()) {
            emit(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "agent",
                    content = "Ключ API не задан. Откройте Настройки и введите ключ Gemini API.",
                    stepIndex = 1
                )
            )
            return@withContext out
        }

        val modelId = settings.selectedModel.ifEmpty { "gemini-4-argon" }
        val systemPrompt = ImmutableSystemPrompt.build(
            projectName = session.projectName,
            projectChatPath = session.folderPath,
            settings = settings,
            termuxAvailable = shellEngine.isTermuxInstalled(),
            termuxApiAvailable = shellEngine.isTermuxApiInstalled()
        )

        try {
            val reply = GeminiApi.generate(
                apiKey = apiKey,
                model = modelId,
                systemPrompt = systemPrompt,
                userText = userPrompt,
                bearerToken = bearer
            )
            emit(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "agent",
                    content = reply,
                    thoughtTrace = "Модель: $modelId",
                    stepIndex = 1
                )
            )
        } catch (e: Exception) {
            emit(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "agent",
                    content = "Модель не ответила: ${e.message ?: "неизвестная ошибка"}",
                    thoughtTrace = "Ошибка запроса к $modelId",
                    stepIndex = 1
                )
            )
        }
        out
    }
}
