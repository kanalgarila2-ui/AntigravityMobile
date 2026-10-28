package ru.nanodev.antigravity.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

/** Результат проверки модели по ключу. */
sealed interface ModelCheck {
    data object Available : ModelCheck
    data object NotFound : ModelCheck
    data class Invalid(val reason: String) : ModelCheck
    data class Failed(val reason: String) : ModelCheck
}

/** Проверяет, отдаёт ли публичный API модель по этому ключу (GET /models/{id}). */
object ModelProbe {
    suspend fun check(apiKey: String, modelId: String): ModelCheck = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) return@withContext ModelCheck.Invalid("Ключ не задан")
        val id = modelId.removePrefix("models/")
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$id")
            .header("x-goog-api-key", key)
            .get()
            .build()
        try {
            GeminiApi.client.newCall(request).execute().use { resp ->
                when (resp.code) {
                    in 200..299 -> ModelCheck.Available
                    404 -> ModelCheck.NotFound
                    401, 403 -> ModelCheck.Invalid("Ключ отклонён (HTTP ${resp.code})")
                    else -> ModelCheck.Failed("HTTP ${resp.code}")
                }
            }
        } catch (e: IOException) {
            ModelCheck.Failed("Нет связи: ${e.message ?: "неизвестная ошибка"}")
        }
    }
}
