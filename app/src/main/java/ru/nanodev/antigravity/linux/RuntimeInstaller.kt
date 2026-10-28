package ru.nanodev.antigravity.linux

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nanodev.antigravity.agy.ArchiveExtractor
import java.io.File

/**
 * Установка среды Linux по манифесту: скачивание, проверка SHA-256, распаковка.
 * Всё лежит в files/linux внутри приложения. Без sha256 компонент не принимается.
 */
object RuntimeInstaller {

    sealed interface Phase {
        data object Idle : Phase
        data class Downloading(
            val component: String,
            val done: Long,
            val total: Long,
            val bytesPerSec: Long = 0L
        ) : Phase
        data object Ready : Phase
        data class Failed(val reason: String) : Phase
    }

    fun root(context: Context): File = File(context.filesDir, "linux").apply { mkdirs() }

    /** Контракт манифеста: компонент target="proot/proot" — исполняемый PRoot. */
    fun prootBinary(context: Context): File = File(root(context), "proot/proot")

    /** Контракт манифеста: компонент target="rootfs" — распакованная glibc-система. */
    fun rootfsDir(context: Context): File = File(root(context), "rootfs")

    suspend fun install(context: Context, onPhase: (Phase) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val components = RuntimeManifest.load()
            require(components.isNotEmpty()) { "В манифесте нет компонентов" }
            for (c in components) {
                val expected = c.sha256
                    ?: error("${c.id}: в манифесте нет sha256, файл не принимается без проверки")
                val part = File(root(context), "${c.id}.part")
                val sha = Downloader.download(c.url, part) { done, total, speed ->
                    onPhase(Phase.Downloading(c.id, done, total, speed))
                }
                if (!expected.equals(sha, ignoreCase = true)) {
                    part.delete()
                    error("${c.id}: контрольная сумма не совпала (ожидалось $expected, получено $sha)")
                }
                val name = c.url.substringBefore('?')
                val dest = File(root(context), c.target)
                if (c.archive) {
                    require(ArchiveExtractor.isArchive(name)) { "${c.id}: ссылка должна вести на .tar.gz или .zip" }
                    dest.deleteRecursively()
                    dest.mkdirs()
                    ArchiveExtractor.extract(part, dest, name)
                    part.delete()
                } else {
                    dest.parentFile?.mkdirs()
                    dest.delete()
                    check(part.renameTo(dest)) { "${c.id}: не удалось сохранить файл" }
                    dest.setExecutable(true, false)
                }
            }
            onPhase(Phase.Ready)
        } catch (e: Exception) {
            onPhase(Phase.Failed(e.message ?: "Неизвестная ошибка"))
        }
    }
}
