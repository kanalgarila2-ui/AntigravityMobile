package ru.nanodev.antigravity.agy

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/**
 * Распаковка .tar.gz и .zip без внешних библиотек.
 * Каждый путь проверяется: файлы не могут выйти за пределы папки назначения.
 */
object ArchiveExtractor {

    fun isArchive(name: String): Boolean =
        name.endsWith(".tar.gz") || name.endsWith(".tgz") || name.endsWith(".zip")

    fun extract(archive: File, dest: File, name: String) {
        dest.mkdirs()
        when {
            name.endsWith(".tar.gz") || name.endsWith(".tgz") ->
                FileInputStream(archive).use { fis -> GZIPInputStream(fis).use { extractTar(it, dest) } }
            name.endsWith(".zip") ->
                ZipInputStream(FileInputStream(archive)).use { extractZip(it, dest) }
            else -> error("Неизвестный формат архива: $name")
        }
    }

    /** Ищет исполняемый файл agy в распакованной папке. */
    fun findBinary(dir: File): File {
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        return files.firstOrNull { it.name == "agy" }
            ?: error("В архиве нет файла agy. Найдено: ${files.take(10).joinToString { it.name }}")
    }

    private fun safeTarget(root: File, entry: String): File {
        val clean = entry.trimStart('/').replace('\\', '/')
        require(clean.split('/').none { it == ".." }) { "Небезопасный путь в архиве: $entry" }
        val rootPath = root.canonicalPath
        val target = File(root, clean).canonicalFile
        require(target.path == rootPath || target.path.startsWith(rootPath + File.separator)) {
            "Путь выходит за папку распаковки: $entry"
        }
        return target
    }

    private fun extractZip(input: ZipInputStream, dest: File) {
        var entry = input.nextEntry
        while (entry != null) {
            val target = safeTarget(dest, entry.name)
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                FileOutputStream(target).use { input.copyTo(it) }
            }
            input.closeEntry()
            entry = input.nextEntry
        }
    }

    /** Разбор tar (ustar, GNU long names). Ссылки и устройства пропускаются. */
    private fun extractTar(input: InputStream, dest: File) {
        val header = ByteArray(512)
        var pendingName: String? = null
        while (true) {
            if (readFully(input, header) < 512) break
            if (header.all { it == 0.toByte() }) break

            val size = octal(header, 124, 12)
            val type = header[156].toInt().toChar()
            val base = cString(header, 0, 100)
            val prefix = cString(header, 345, 155)
            val entryName = pendingName ?: if (prefix.isNotEmpty()) "$prefix/$base" else base
            pendingName = null

            when (type) {
                'L' -> {
                    val data = ByteArray(size.toInt())
                    readFully(input, data)
                    skipPadding(input, size)
                    pendingName = cString(data, 0, data.size)
                }
                '0', '\u0000', '7' -> {
                    val target = safeTarget(dest, entryName)
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { copyN(input, it, size) }
                    skipPadding(input, size)
                    val mode = octal(header, 100, 8)
                    target.setExecutable(mode and 0x49L != 0L, false)
                }
                '1' -> {
                    // Жёсткая ссылка: копия уже распакованного файла (имя внутри архива, от корня)
                    val link = safeTarget(dest, entryName)
                    val source = safeTarget(dest, cString(header, 157, 100))
                    require(source.exists()) { "Жёсткая ссылка на отсутствующий файл: $entryName" }
                    link.parentFile?.mkdirs()
                    source.copyTo(link, overwrite = true)
                    skipPadding(input, size)
                }
                '2' -> {
                    val link = safeTarget(dest, entryName)
                    link.parentFile?.mkdirs()
                    val linkTarget = cString(header, 157, 100)
                    require(linkTarget.isNotEmpty()) { "Пустая символическая ссылка: $entryName" }
                    if (link.exists() || java.nio.file.Files.isSymbolicLink(link.toPath())) link.delete()
                    java.nio.file.Files.createSymbolicLink(link.toPath(), java.nio.file.Paths.get(linkTarget))
                    skipPadding(input, size)
                }
                '5' -> {
                    safeTarget(dest, entryName).mkdirs()
                    skipPadding(input, size)
                }
                else -> {
                    copyN(input, OutputStream.nullOutputStream(), size)
                    skipPadding(input, size)
                }
            }
        }
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val n = input.read(buf, total, buf.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    private fun copyN(input: InputStream, out: OutputStream, count: Long) {
        val buf = ByteArray(64 * 1024)
        var left = count
        while (left > 0) {
            val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
            if (n < 0) break
            out.write(buf, 0, n)
            left -= n
        }
    }

    private fun skipPadding(input: InputStream, size: Long) {
        val pad = ((512 - size % 512) % 512).toInt()
        if (pad > 0) readFully(input, ByteArray(pad))
    }

    private fun cString(buf: ByteArray, off: Int, len: Int): String =
        String(buf, off, len, Charsets.UTF_8).substringBefore('\u0000')

    private fun octal(buf: ByteArray, off: Int, len: Int): Long {
        val s = String(buf, off, len, Charsets.US_ASCII).trim { it == ' ' || it == '\u0000' }
        return if (s.isEmpty()) 0L else s.toLong(8)
    }
}
