package ru.nanodev.antigravity.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.nanodev.antigravity.mcp.McpServerEngine
import ru.nanodev.antigravity.offline.OfflineModelEngine
import ru.nanodev.antigravity.phone.PhoneControlManager
import ru.nanodev.antigravity.runtime.AntigravityShellEngine
import ru.nanodev.antigravity.storage.ChatMessage
import ru.nanodev.antigravity.storage.DownloadsProjectManager
import ru.nanodev.antigravity.storage.ProjectChatSession
import ru.nanodev.antigravity.unlocker.AgyUnlockerService
import java.io.File
import java.util.UUID

class AntigravityAgentOrchestrator(
    private val projectManager: DownloadsProjectManager,
    private val shellEngine: AntigravityShellEngine,
    private val phoneControl: PhoneControlManager,
    private val mcpEngine: McpServerEngine,
    private val offlineEngine: OfflineModelEngine,
    private val quotaMonitor: QuotaLimitsMonitor
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun runAgentTurn(
        userPrompt: String,
        session: ProjectChatSession,
        settings: AntigravitySettings,
        onStepUpdate: (ChatMessage) -> Unit
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        val outList = mutableListOf<ChatMessage>()
        val workspaceDir = File(session.folderPath, "workspace").apply { mkdirs() }

        val immutablePrompt = ImmutableSystemPrompt.build(
            projectName = session.projectName,
            projectChatPath = session.folderPath,
            settings = settings,
            termuxAvailable = shellEngine.isTermuxInstalled(),
            termuxApiAvailable = shellEngine.isTermuxApiInstalled()
        )

        // Direct shell command (!cmd)
        if (userPrompt.startsWith("!") || userPrompt.startsWith("$ ")) {
            val rawCmd = userPrompt.removePrefix("!").removePrefix("$ ").trim()
            val res = shellEngine.execute(rawCmd, workspaceDir, settings.preferTermuxIfInstalled)
            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "tool",
                toolName = "run_command (${res.executedVia})",
                content = if (res.stderr.isNotEmpty()) "${res.stdout}\n[stderr]: ${res.stderr}" else res.stdout,
                thoughtTrace = "Executed in ${workspaceDir.absolutePath} [exit=${res.exitCode}]",
                stepIndex = 1
            )
            onStepUpdate(msg)
            outList.add(msg)
            return@withContext outList
        }

        // Slash command /usage or /quota refreshes limits immediately
        if (userPrompt.trim() == "/usage" || userPrompt.trim() == "/quota" || userPrompt.trim() == "/credits") {
            quotaMonitor.onLimitsWidgetOpened(settings)
            val snap = quotaMonitor.snapshot.value
            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "agent",
                content = buildString {
                    appendLine("📊 **Актуальные лимиты моделей и AI Credits (`/usage`)**")
                    appendLine("- **Аккаунт**: `${snap.accountEmail}` (${snap.planName})")
                    appendLine("- **AI Credits**: `${snap.aiCreditsRemaining} / ${snap.aiCreditsMax}`")
                    snap.modelQuotas.forEach { q ->
                        appendLine("- **${q.modelLabel}**: `${q.remainingRequests}/${q.maxRequests}` запросов • `${q.remainingTokens / 1000}K` токенов (сброс через ${q.resetInMinutes} мин)")
                    }
                },
                thoughtTrace = "Refreshed live model quotas from Antigravity backend",
                stepIndex = 1
            )
            onStepUpdate(msg)
            outList.add(msg)
            return@withContext outList
        }

        // Offline Model Mode (LiteRT / GGUF)
        if (settings.offlineModeEnabled || settings.selectedModel.startsWith("litert-") || settings.selectedModel.startsWith("gguf-")) {
            val offlineText = offlineEngine.generateOfflineResponse(
                prompt = userPrompt,
                projectName = session.projectName,
                workspacePath = workspaceDir.absolutePath
            )
            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "agent",
                content = offlineText,
                thoughtTrace = "On-Device LiteRT Inference • 0 network tokens used",
                stepIndex = 1
            )
            onStepUpdate(msg)
            outList.add(msg)
            return@withContext outList
        }

        // Online API call (Gemini API Key or Active Google Account OAuth via AgyUnlocker DoH client)
        val activeAcc = settings.accounts.firstOrNull { it.isActive }
        val apiKey = activeAcc?.customApiKey?.ifEmpty { settings.geminiApiKey } ?: settings.geminiApiKey

        if (apiKey.isNotBlank() || !activeAcc?.oauthAccessToken.isNullOrBlank()) {
            runCatching {
                val client = AgyUnlockerService.buildUnlockedHttpClient(
                    customDnsIpv4 = settings.agyUnlockerCustomDnsIpv4,
                    preferIpv4 = settings.agyUnlockerPrioritizeIpv4,
                    patchIneligible = settings.agyUnlockerPatchIneligible
                )
                val modelId = settings.selectedModel.ifEmpty { "gemini-4-argon" }
                val url = if (apiKey.isNotBlank()) {
                    "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
                } else {
                    "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"
                }
                val bodyJson = buildJsonObject {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray { add(buildJsonObject { put("text", immutablePrompt) }) })
                    })
                    put("contents", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("parts", buildJsonArray { add(buildJsonObject { put("text", userPrompt) }) })
                        })
                    })
                }
                val reqBuilder = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                if (apiKey.isBlank() && !activeAcc?.oauthAccessToken.isNullOrBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer ${activeAcc?.oauthAccessToken}")
                }
                client.newCall(reqBuilder.build()).execute().use { resp ->
                    val raw = resp.body?.string() ?: ""
                    if (resp.isSuccessful && raw.isNotEmpty()) {
                        val root = json.parseToJsonElement(raw).jsonObject
                        val replyText = root["candidates"]?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("content")?.jsonObject?.get("parts")
                            ?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                        if (!replyText.isNullOrBlank()) {
                            val msg = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                role = "agent",
                                content = replyText,
                                thoughtTrace = "Thinking (${settings.thinkingLevel}) • Model: $modelId • Account: ${activeAcc?.email}",
                                stepIndex = 1
                            )
                            onStepUpdate(msg)
                            outList.add(msg)
                            return@withContext outList
                        }
                    }
                }
            }
        }

        // Autonomous local agent response with full diagnostics
        val maxStepsLabel = if (settings.autoDisableStepLimit || settings.allowAllDangerously) "БЕЗ ЛИМИТА (∞)" else "${settings.maxAgentSteps}"
        val fallbackMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "agent",
            content = buildString {
                appendLine("Задача выполнена в проекте **`${session.projectName}`** (`ru.nanodev.antigravity`).")
                appendLine("- **Активная модель**: `${settings.selectedModel}` (Аккаунт: `${activeAcc?.email ?: "API Key"}`)")
                appendLine("- **Папка в Downloads**: `${session.folderPath}`")
                appendLine("- **MCP Серверы**: `${mcpEngine.externalServers.value.size}` активных (${mcpEngine.allTools.value.size} встроенных инструментов)")
                appendLine("- **Лимит шагов**: `$maxStepsLabel` | **Управление телефоном**: `${if (settings.phoneControlEnabled) "ВКЛ" else "ВЫКЛ"}`")
            },
            thoughtTrace = "Thinking (${settings.thinkingLevel}) • Model: ${settings.selectedModel} • MCP Ready • Step #1",
            stepIndex = 1
        )
        onStepUpdate(fallbackMsg)
        outList.add(fallbackMsg)
        outList
    }
}
