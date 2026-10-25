package io.fluxreader.app.reader

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** No arbitrary archives/URLs: the catalog pins one package and its complete digest. */
internal object VoicePackageInstaller {
    fun verify(file: File, bytes: Long, sha256: String, check: () -> Unit = {}) {
        require(file.length() == bytes) { "Неполная загрузка голоса" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                check()
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) } == sha256) {
            "Проверка загрузки не пройдена. Скачайте голос заново"
        }
    }

    fun extract(input: InputStream, destination: File, root: String,
                expected: Map<String, Long>, check: () -> Unit = {}) {
        require(!destination.exists())
        kotlin.check(destination.mkdirs())
        val seen = mutableSetOf<String>()
        var expanded = 0L
        var count = 0
        try {
            TarArchiveInputStream(BZip2CompressorInputStream(input.buffered())).use { tar ->
                while (true) {
                    check()
                    val entry = tar.nextEntry ?: break
                    require(++count <= 64) { "Слишком много файлов в пакете" }
                    val path = entry.name.removeSuffix("/")
                    require(!path.startsWith("/") && '\\' !in path && path.split('/').none { it == ".." })
                    require(path == root || path.startsWith("$root/"))
                    require(entry.isDirectory || entry.isFile) { "Ссылки в пакете недопустимы" }
                    if (entry.isDirectory) continue
                    expanded += entry.size
                    require(entry.size >= 0 && expanded <= 180L * 1024 * 1024) { "Пакет слишком большой" }
                    val name = path.removePrefix("$root/")
                    val size = expected[name] ?: continue
                    require(seen.add(name) && entry.size == size) { "Неверный файл модели: $name" }
                    File(destination, name).outputStream().buffered().use { out ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            check()
                            val n = tar.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                        }
                    }
                    require(File(destination, name).length() == size)
                }
            }
            require(seen == expected.keys) { "В пакете не хватает файлов голоса" }
        } catch (e: Exception) {
            destination.deleteRecursively()
            throw e
        }
    }

    fun installed(directory: File): Boolean =
        File(directory, "verified.sha256").let { it.isFile && runCatching { it.readText() }.getOrNull() == BuiltInVoiceCatalog.SHA256 } &&
            BuiltInVoiceCatalog.files.all { (name, size) -> File(directory, name).length() == size }
}
