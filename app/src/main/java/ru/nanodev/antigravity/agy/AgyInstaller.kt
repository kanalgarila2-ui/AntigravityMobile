package ru.nanodev.antigravity.agy

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Скачивание официального Antigravity CLI (agy) с GitHub-релиза Google.
 * Файл не вшивается в приложение: пользователь скачивает его сам, а приложение проверяет SHA-256.
 * Если в релизе опубликован digest, файл принимается только при совпадении.
 */
object AgyInstaller {

    private const val RELEASES_API =
        "https://api.github.com/repos/google-antigravity/antigravity-cli/releases/latest"

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed interface Phase {
        data object Idle : Phase
        data class Downloading(val done: Long, val total: Long) : Phase
        data class Done(val path: String, val sha256: String, val verified: Boolean) : Phase
        data class Failed(val reason: String) : Phase
    }

    fun binaryFile(context: Context): File = File(context.filesDir, "agy/agy")

    /** Скачивает файл, считает SHA-256 на лету и сообщает прогресс через [onPhase]. */
    suspend fun install(context: Context, onPhase: (Phase) -> Unit) = withContext(Dispatchers.IO) {
        try {
            onPhase(Phase.Downloading(0, 0))
            val release = http.newCall(
                Request.Builder().url(RELEASES_API).header("Accept", "application/vnd.github+json").build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) error("GitHub ответил HTTP ${resp.code}")
                JSONObject(resp.body?.string().orEmpty())
            }

            val assets = release.getJSONArray("assets")
            val names = (0 until assets.length()).map { assets.getJSONObject(it).getString("name") }
            val asset = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull {
                    val n = it.getString("name").lowercase()
                    n.contains("linux") && n.contains("arm64") && !n.endsWith(".sha256")
                }
                ?: error("В релизе нет файла для linux arm64. Доступно: ${names.joinToString()}")

            val name = asset.getString("name")
            if (name.endsWith(".tar.gz") || name.endsWith(".zip")) {
                error("Файл $name в архиве. Распаковка в приложении пока не поддержана")
            }

            val expected = asset.optString("digest", "").removePrefix("sha256:").takeIf { it.isNotBlank() }
            val total = asset.optLong("size", 0L)
            val target = binaryFile(context).apply { parentFile?.mkdirs() }
            val tmp = File(target.parentFile, "$name.part")

            val digest = MessageDigest.getInstance("SHA-256")
            http.newCall(Request.Builder().url(asset.getString("browser_download_url")).build()).execute().use { resp ->
                if (!resp.isSuccessful) error("Скачивание: HTTP ${resp.code}")
                val body = resp.body ?: error("Пустой ответ при скачивании")
                val size = if (total > 0) total else body.contentLength()
                var done = 0L
                tmp.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            done += n
                            onPhase(Phase.Downloading(done, size))
                        }
                    }
                }
            }

            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            if (expected != null && !expected.equals(sha, ignoreCase = true)) {
                tmp.delete()
                error("Контрольная сумма не совпала: ожидалось $expected, получено $sha. Файл удалён")
            }

            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) error("Не удалось сохранить файл")
            target.setExecutable(true, false)
            onPhase(Phase.Done(target.absolutePath, sha, verified = expected != null))
        } catch (e: Exception) {
            onPhase(Phase.Failed(e.message ?: "Неизвестная ошибка"))
        }
    }
}
