package ru.nanodev.antigravity.net

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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Результат проверки API-ключа. */
sealed class KeyCheck {
    data class Valid(val modelCount: Int) : KeyCheck()
    data class Invalid(val reason: String) : KeyCheck()
    data class Failed(val reason: String) : KeyCheck()
}

/** Прямой клиент Gemini API. Ключ передаётся заголовком x-goog-api-key, а не в URL. */
object GeminiApi {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Проверка ключа запросом списка моделей. */
    suspend fun validateKey(apiKey: String): KeyCheck = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) return@withContext KeyCheck.Invalid("Ключ пустой")
        val request = Request.Builder()
            .url("$BASE/models?pageSize=50")
            .header("x-goog-api-key", key)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                when {
                    resp.isSuccessful -> {
                        val count = runCatching {
                            json.parseToJsonElement(raw).jsonObject["models"]?.jsonArray?.size ?: 0
                        }.getOrDefault(0)
                        KeyCheck.Valid(count)
                    }
                    resp.code == 400 || resp.code == 401 || resp.code == 403 ->
                        KeyCheck.Invalid(extractMessage(raw) ?: "Ключ отклонён (HTTP ${resp.code})")
                    resp.code == 429 ->
                        KeyCheck.Failed("Слишком много запросов (HTTP 429), попробуйте позже")
                    else ->
                        KeyCheck.Failed("Сервер ответил HTTP ${resp.code}")
                }
            }
        } catch (e: IOException) {
            KeyCheck.Failed("Нет связи с сервером: ${e.message ?: "неизвестная ошибка"}")
        }
    }

    /** Один запрос generateContent. Возвращает текст ответа или бросает IllegalStateException с понятным сообщением. */
    suspend fun generate(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userText: String,
        bearerToken: String? = null
    ): String = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray { add(buildJsonObject { put("text", systemPrompt) }) })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", userText) }) })
                })
            })
        }
        val builder = Request.Builder()
            .url("$BASE/models/$model:generateContent")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        when {
            apiKey.isNotBlank() -> builder.header("x-goog-api-key", apiKey.trim())
            !bearerToken.isNullOrBlank() -> builder.header("Authorization", "Bearer $bearerToken")
            else -> throw IllegalStateException("Не задан ключ API")
        }
        client.newCall(builder.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(extractMessage(raw) ?: "HTTP ${resp.code}")
            }
            json.parseToJsonElement(raw).jsonObject["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: throw IllegalStateException("Модель вернула пустой ответ")
        }
    }

    private fun extractMessage(raw: String): String? = runCatching {
        json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject
            ?.get("message")?.jsonPrimitive?.content
    }.getOrNull()
}
