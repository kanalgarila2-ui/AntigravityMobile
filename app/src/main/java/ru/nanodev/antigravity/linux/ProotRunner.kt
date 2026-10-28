package ru.nanodev.antigravity.linux

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nanodev.antigravity.agy.AgyInstaller
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Запуск agy внутри Linux-окружения через PRoot.
 * Рабочая папка agy монтируется как /workspace, сам agy как /usr/local/bin/agy.
 */
object ProotRunner {

    data class Result(val exitCode: Int, val output: String)

    /** Аргументы входа в agy. Точную команду покажет «Справка agy (--help)». */
    val LOGIN_ARGS: List<String> = listOf("login")

    /** Готовит ProcessBuilder: проверяет файлы и подставляет переменные окружения. */
    private fun prepare(context: Context, args: List<String>): ProcessBuilder {
        val proot = RuntimeInstaller.prootBinary(context)
        val loader = File(proot.parentFile, "loader")
        val rootfs = RuntimeInstaller.rootfsDir(context)
        val agy = AgyInstaller.binaryFile(context)
        val work = AgyInstaller.workspaceDir(context)
        check(proot.exists()) { "PRoot не установлен: сначала установите среду Linux" }
        check(rootfs.isDirectory) { "Rootfs не распакован" }
        check(agy.exists()) { "agy не скачан" }
        proot.setExecutable(true, false)
        if (loader.exists()) loader.setExecutable(true, false)

        val cmd = listOf(
            proot.absolutePath,
            "-r", rootfs.absolutePath,
            "-b", "${work.absolutePath}:/workspace",
            "-b", "${agy.absolutePath}:/usr/local/bin/agy",
            "-w", "/workspace",
            "/usr/bin/env", "-i", "HOME=/root", "TERM=dumb", "PATH=/usr/local/bin:/usr/bin:/bin",
            "agy"
        ) + args

        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        if (loader.exists()) pb.environment()["PROOT_LOADER"] = loader.absolutePath
        pb.environment()["PROOT_TMP_DIR"] = File(context.cacheDir, "proot").apply { mkdirs() }.absolutePath
        return pb
    }

    /** Запуск с ожиданием результата (для --version и --help). */
    suspend fun runAgy(context: Context, args: List<String>, timeoutSec: Long = 120): Result =
        withContext(Dispatchers.IO) {
            val process = prepare(context, args).start()
            // Сначала ждём с таймаутом, потом читаем вывод: зависший процесс не блокирует приложение
            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            val text = process.inputStream.bufferedReader().use { it.readText() }
            if (!finished) Result(-1, text + "\n[таймаут ${timeoutSec} с]")
            else Result(process.exitValue(), text)
        }

    /** Потоковый запуск (для входа): каждая строка вывода уходит в onLine. Процесс возвращается наружу. */
    fun start(context: Context, args: List<String>, onLine: (String) -> Unit): Process {
        val process = prepare(context, args).start()
        Thread {
            process.inputStream.bufferedReader().forEachLine(onLine)
        }.apply { isDaemon = true; start() }
        return process
    }
}
