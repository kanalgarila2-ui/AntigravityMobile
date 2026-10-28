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
 * Архив проверяется по SHA-256 (если digest опубликован), затем распаковывается,
 * и исполняемый файл agy кладётся в папку приложения. Никакого вшивания в APK.
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
        data class Downloading(val done: Long, val total: Long, val bytesPerSec: Long = 0L) : Phase
        data class Done(val path: String, val sha256: String, val verified: Boolean) : Phase
        data class Failed(val reason: String) : Phase
    }

    private fun agyDir(context: Context): File = File(context.filesDir, "agy")

    /** Исполняемый файл agy внутри приложения. */
    fun binaryFile(context: Context): File = File(agyDir(context), "agy")

    /** Рабочая папка agy: всё, что agy создаёт, остаётся внутри приложения. */
    fun workspaceDir(context: Context): File = File(agyDir(context), "workspace").apply { mkdirs() }

    suspend fun install(context: Context, onPhase: (Phase) -> Unit) = withContext(Dispatchers.IO) {
        try {
            onPhase(Phase.Downloading(0, 0))
            val asset = pickAsset(fetchRelease())
            val name = asset.getString("name")
            val expected = asset.optString("digest", "").removePrefix("sha256:").takeIf { it.isNotBlank() }
            val total = asset.optLong("size", 0L)

            val dir = agyDir(context).apply { mkdirs() }
            val part = File(dir, "download.part")
            val sha = ru.nanodev.antigravity.linux.Downloader.download(asset.getString("browser_download_url"), part) { done, tot, speed ->
                onPhase(Phase.Downloading(done, if (tot > 0) tot else total, speed))
            }

            if (expected != null && !expected.equals(sha, ignoreCase = true)) {
                part.delete()
                error("Контрольная сумма не совпала: ожидалось $expected, получено $sha. Файл удалён")
            }

            val target = binaryFile(context)
            target.delete()
            if (ArchiveExtractor.isArchive(name)) {
                val extractDir = File(dir, "extract").apply { deleteRecursively(); mkdirs() }
                ArchiveExtractor.extract(part, extractDir, name)
                part.delete()
                ArchiveExtractor.findBinary(extractDir).copyTo(target, overwrite = true)
                extractDir.deleteRecursively()
            } else {
                if (!part.renameTo(target)) error("Не удалось сохранить файл")
            }
            target.setExecutable(true, false)
            onPhase(Phase.Done(target.absolutePath, sha, verified = expected != null))
        } catch (e: Exception) {
            onPhase(Phase.Failed(e.message ?: "Неизвестная ошибка"))
        }
    }

    private fun fetchRelease(): JSONObject =
        http.newCall(
            Request.Builder().url(RELEASES_API).header("Accept", "application/vnd.github+json").build()
        ).execute().use { resp ->
            if (!resp.isSuccessful) error("GitHub ответил HTTP ${resp.code}")
            JSONObject(resp.body?.string().orEmpty())
        }

    private fun pickAsset(release: JSONObject): JSONObject {
        val assets = release.getJSONArray("assets")
        val all = (0 until assets.length()).map { assets.getJSONObject(it) }
        return all.firstOrNull {
            val n = it.getString("name").lowercase()
            n.contains("linux") && n.contains("arm64") && !n.endsWith(".sha256")
        } ?: error("В релизе нет файла для linux arm64. Доступно: ${all.joinToString { it.getString("name") }}")
    }

    /** Качает файл, считает SHA-256 на лету и сообщает прогресс. */
    private fun downloadWithDigest(url: String, total: Long, target: File, onPhase: (Phase) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("Скачивание: HTTP ${resp.code}")
            val body = resp.body ?: error("Пустой ответ при скачивании")
            val size = if (total > 0) total else body.contentLength()
            var done = 0L
            target.outputStream().use { out ->
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
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
