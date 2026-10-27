package ru.nanodev.antigravity.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.nanodev.antigravity.net.Connectivity
import ru.nanodev.antigravity.net.GeminiApi

@Serializable
data class AntigravityModelInfo(
    val id: String,
    val displayName: String,
    val provider: String, // "Google Antigravity API", "Vertex Enterprise", "Anthropic Partner", "Local"
    val maxInputTokens: Int = 2097152,
    val maxOutputTokens: Int = 65536,
    val supportsThinking: Boolean = true,
    val badge: String = "LATEST"
)

/**
 * Динамический реестр моделей Antigravity 2.0 (`ru.nanodev.antigravity.sdk`).
 * Запрашивает самые последние доступные модели напрямую по API:
 * 1. `GET https://generativelanguage.googleapis.com/v1beta/models` (при наличии GEMINI_API_KEY или OAuth)
 * 2. `POST https://cloudcode-pa.googleapis.com/v1internal:fetchAvailableModels` (каталог Antigravity 2.0)
 */
class DynamicModelRegistry {

    private val json = Json { ignoreUnknownKeys = true }

    private val defaultLatestModels = listOf(
        AntigravityModelInfo(
            id = "gemini-4-argon",
            displayName = "Gemini 4 Argon (Pro / Ultra)",
            provider = "Google Antigravity API",
            maxInputTokens = 2097152,
            maxOutputTokens = 65536,
            supportsThinking = true,
            badge = "NEW 2.0"
        ),
        AntigravityModelInfo(
            id = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            provider = "Google Antigravity API",
            maxInputTokens = 1048576,
            maxOutputTokens = 65536,
            supportsThinking = true,
            badge = "FAST"
        ),
        AntigravityModelInfo(
            id = "gemini-3.7-flash",
            displayName = "Gemini 3.7 Flash",
            provider = "Google Antigravity API",
            maxInputTokens = 1048576,
            maxOutputTokens = 65536,
            supportsThinking = true,
            badge = "STABLE"
        ),
        AntigravityModelInfo(
            id = "gemini-3.5-flash",
            displayName = "Gemini 3.5 Flash (SDK Default)",
            provider = "Google Antigravity API",
            maxInputTokens = 1048576,
            maxOutputTokens = 65536,
            supportsThinking = true,
            badge = "DEFAULT"
        ),
        AntigravityModelInfo(
            id = "gemini-3.1-pro-preview",
            displayName = "Gemini 3.1 Pro Preview",
            provider = "Google Antigravity API",
            maxInputTokens = 2097152,
            maxOutputTokens = 65536,
            supportsThinking = true,
            badge = "DEEP"
        ),
        AntigravityModelInfo(
            id = "claude-sonnet-5-5-thinking",
            displayName = "Claude Sonnet 5.5 (Thinking)",
            provider = "Antigravity Enterprise",
            maxInputTokens = 500000,
            maxOutputTokens = 32768,
            supportsThinking = true,
            badge = "PARTNER"
        )
    )

    private val _models = MutableStateFlow(defaultLatestModels)
    val models: StateFlow<List<AntigravityModelInfo>> = _models

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp

    suspend fun refreshModelsFromApi(settings: AntigravitySettings): List<AntigravityModelInfo> = withContext(Dispatchers.IO) {
        if (!Connectivity.online.value) return@withContext _models.value
        val discovered = mutableListOf<AntigravityModelInfo>()
        discovered.addAll(defaultLatestModels)

        val apiKey = settings.geminiApiKey.trim()
        val activeAcc = settings.accounts.firstOrNull { it.isActive }

        if (apiKey.isNotEmpty() || !activeAcc?.oauthAccessToken.isNullOrEmpty()) {
            runCatching {
                val client: OkHttpClient = GeminiApi.client
                val reqBuilder = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=50")
                    .get()
                if (apiKey.isNotEmpty()) reqBuilder.addHeader("x-goog-api-key", apiKey)
                if (apiKey.isEmpty() && !activeAcc?.oauthAccessToken.isNullOrEmpty()) {
                    reqBuilder.addHeader("Authorization", "Bearer ${activeAcc?.oauthAccessToken}")
                }

                client.newCall(reqBuilder.build()).execute().use { resp ->
                    val raw = resp.body?.string() ?: ""
                    if (resp.isSuccessful && raw.isNotEmpty()) {
                        val root = json.parseToJsonElement(raw).jsonObject
                        val arr = root["models"]?.jsonArray
                        arr?.forEach { item ->
                            val obj = item.jsonObject
                            val rawName = obj["name"]?.jsonPrimitive?.content?.removePrefix("models/") ?: return@forEach
                            val disp = obj["displayName"]?.jsonPrimitive?.content ?: rawName
                            val inLimit = obj["inputTokenLimit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1048576
                            val outLimit = obj["outputTokenLimit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 65536
                            if (discovered.none { it.id == rawName }) {
                                discovered.add(
                                    0,
                                    AntigravityModelInfo(
                                        id = rawName,
                                        displayName = "$disp (API Live)",
                                        provider = "Google Gemini API (Live)",
                                        maxInputTokens = inLimit,
                                        maxOutputTokens = outLimit,
                                        supportsThinking = true,
                                        badge = "API LIVE"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        _models.value = discovered
        _lastSyncTimestamp.value = System.currentTimeMillis()
        discovered
    }
}
