package ru.nanodev.antigravity.linux

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Загрузка файлов: SHA-256 на лету, прогресс и средняя скорость. */
object Downloader {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Качает [url] в [target]. onProgress(done, total, bytesPerSec).
     * Возвращает SHA-256 в нижнем регистре.
     */
    fun download(url: String, target: File, onProgress: (done: Long, total: Long, bytesPerSec: Long) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("Скачивание: HTTP ${resp.code}")
            val body = resp.body ?: error("Пустой ответ при скачивании")
            val total = body.contentLength().coerceAtLeast(0L)
            var done = 0L
            val started = System.nanoTime()
            target.parentFile?.mkdirs()
            target.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        done += n
                        val seconds = (System.nanoTime() - started) / 1_000_000_000.0
                        val speed = if (seconds > 0) (done / seconds).toLong() else 0L
                        onProgress(done, total, speed)
                    }
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
