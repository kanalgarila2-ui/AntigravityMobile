package ru.nanodev.antigravity.crash

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Записывает последнюю необработанную ошибку в файл приложения.
 * Файл лежит в Android/data/ru.nanodev.antigravity/files/crash.txt и не требует ADB.
 * Следующий запуск покажет отчёт, который можно скопировать.
 */
object CrashLogger {
    private const val FILE_NAME = "crash.txt"

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun file(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, FILE_NAME)

    fun read(context: Context): String? = runCatching {
        file(context).takeIf { it.exists() }?.readText()
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            appendLine("Время: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("Устройство: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Поток: ${thread.name}")
            appendLine()
            append(stack)
        }
        file(context).writeText(text)
        // Пробуем сразу положить в буфер; Android может не разрешить из фона, тогда экран скопирует при запуске
        runCatching {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText("Antigravity crash", text))
        }
    }
}
