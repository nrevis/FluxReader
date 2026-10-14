package io.fluxreader.app.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.fluxreader.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

private const val REPO = "nrevis/FluxReader"
private const val RELEASES_API = "https://api.github.com/repos/$REPO/releases?per_page=30"
const val RELEASES_WEB_URL = "https://github.com/$REPO/releases"
private const val MAX_APK_BYTES = 300L * 1024 * 1024
private const val AUTO_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

enum class UpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, READY, ERROR }

data class UpdateRelease(
    val tag: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkName: String,
    val assetSize: Long,
    val sha256: String?,
    val checksumUrl: String?,
    val checksumName: String?,
)

data class GitHubUpdateState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val autoCheck: Boolean = true,
    val includePreReleases: Boolean = true,
    val release: UpdateRelease? = null,
    val progressPercent: Int = 0,
    val message: String = "Проверка обновлений ещё не выполнялась",
    val lastCheckedAt: Long = 0,
)

/** GitHub Releases updater for manually sideloaded builds; no silent APK installation. */
class GitHubUpdateManager(context: Context, private val scope: CoroutineScope) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("github_updates_v1", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        GitHubUpdateState(
            autoCheck = prefs.getBoolean("auto_check", true),
            includePreReleases = prefs.getBoolean("prerelease", true),
            lastCheckedAt = prefs.getLong("last_checked", 0L),
        )
    )
    val state: StateFlow<GitHubUpdateState> = _state.asStateFlow()
    private var operation: Job? = null
    private var verifiedApk: File? = null

    init {
        if (_state.value.autoCheck) check(force = false)
    }

    fun setAutoCheck(enabled: Boolean) {
        prefs.edit().putBoolean("auto_check", enabled).apply()
        _state.value = _state.value.copy(autoCheck = enabled)
        if (enabled) check(force = false)
    }

    fun setIncludePreReleases(enabled: Boolean) {
        prefs.edit().putBoolean("prerelease", enabled).apply()
        verifiedApk = null
        _state.value = _state.value.copy(includePreReleases = enabled, release = null, phase = UpdatePhase.IDLE,
            message = "Канал обновлений изменён. Проверьте новую версию.")
    }

    fun check(force: Boolean = true) {
        if (operation?.isActive == true) return
        val current = _state.value
        val now = System.currentTimeMillis()
        if (!force && now - current.lastCheckedAt in 0 until AUTO_CHECK_INTERVAL_MS) return
        _state.value = current.copy(phase = UpdatePhase.CHECKING, progressPercent = 0, message = "Проверяем GitHub Releases…")
        operation = scope.launch(Dispatchers.IO) {
            try {
                val includeAlpha = _state.value.includePreReleases
                val releases = JSONArray(readText(RELEASES_API, 1024 * 1024))
                var chosen: UpdateRelease? = null
                for (i in 0 until releases.length()) {
                    val item = releases.optJSONObject(i) ?: continue
                    if (item.optBoolean("draft")) continue
                    val tag = item.optString("tag_name")
                    if (!UpdateVersionPolicy.newerThanInstalled(tag, BuildConfig.VERSION_NAME, includeAlpha)) continue
                    if (!includeAlpha && item.optBoolean("prerelease")) continue
                    val candidate = releaseFromJson(item) ?: continue
                    if (chosen == null || ReleaseVersion.parse(tag)!! > ReleaseVersion.parse(chosen.tag)!!) {
                        chosen = candidate
                    }
                }
                val timestamp = System.currentTimeMillis()
                prefs.edit().putLong("last_checked", timestamp).apply()
                verifiedApk = null
                _state.value = _state.value.copy(
                    phase = if (chosen == null) UpdatePhase.CURRENT else UpdatePhase.AVAILABLE,
                    release = chosen,
                    lastCheckedAt = timestamp,
                    message = if (chosen == null) "Новых выпусков с APK нет" else "Доступна версия ${chosen.tag}",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(phase = UpdatePhase.ERROR,
                    message = "Не удалось проверить GitHub: ${friendlyError(e)}")
            }
        }
    }

    fun download() {
        if (operation?.isActive == true) return
        val release = _state.value.release ?: return
        _state.value = _state.value.copy(phase = UpdatePhase.DOWNLOADING, progressPercent = 0,
            message = "Подготавливаем защищённое скачивание…")
        operation = scope.launch(Dispatchers.IO) {
            val dir = File(app.cacheDir, "updates").apply { mkdirs() }
            val partial = File(dir, "fluxreader-update.partial.apk")
            val target = File(dir, "fluxreader-update.apk")
            try {
                val checksum = release.sha256 ?: resolveChecksum(release)
                    ?: throw IOException("У релиза нет SHA-256. Добавьте checksum к APK в GitHub Releases.")
                if (release.assetSize !in 1..MAX_APK_BYTES) throw IOException("Недопустимый размер APK")
                partial.delete()
                downloadFile(release, partial)
                if (!sha256(partial).equals(checksum, ignoreCase = true)) {
                    throw IOException("SHA-256 не совпадает. Файл не будет установлен.")
                }
                validateApk(partial)
                target.delete()
                if (!partial.renameTo(target)) throw IOException("Не удалось подготовить APK для установки")
                verifiedApk = target
                _state.value = _state.value.copy(phase = UpdatePhase.READY, progressPercent = 100,
                    message = "APK проверен. Для установки требуется подтверждение Android.")
            } catch (e: Exception) {
                verifiedApk = null
                partial.delete()
                _state.value = _state.value.copy(phase = UpdatePhase.ERROR,
                    message = "Не удалось скачать обновление: ${friendlyError(e)}")
            }
        }
    }

    /** Called only by a direct user click. Android's installer handles the confirmation. */
    fun requestInstall() {
        if (_state.value.phase != UpdatePhase.READY) return
        val apk = verifiedApk?.takeIf { it.isFile } ?: run {
            _state.value = _state.value.copy(phase = UpdatePhase.ERROR, message = "APK больше не найден. Скачайте его заново.")
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
                val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(permissionIntent)
                _state.value = _state.value.copy(message = "Разрешите установку из Flux Reader, вернитесь и нажмите «Установить» ещё раз.")
                return
            }
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates.fileprovider", apk)
            app.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: ActivityNotFoundException) {
            _state.value = _state.value.copy(message = "В системе не найден установщик APK")
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "Не удалось открыть установщик: ${friendlyError(e)}")
        }
    }

    private fun releaseFromJson(json: JSONObject): UpdateRelease? {
        val assets = json.optJSONArray("assets") ?: return null
        val found = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
        val apk = found.firstOrNull { asset ->
            val name = asset.optString("name")
            name.endsWith(".apk", true) && name.contains("fluxreader", true) &&
                !name.contains("debug", true) && asset.optLong("size") in 1..MAX_APK_BYTES &&
                isRepositoryAssetUrl(asset.optString("browser_download_url"))
        } ?: return null
        val name = apk.optString("name")
        val checksum = found.firstOrNull { asset ->
            val checkName = asset.optString("name")
            (checkName.equals("$name.sha256", true) || checkName.equals("SHA256SUMS", true)) &&
                isRepositoryAssetUrl(asset.optString("browser_download_url"))
        }
        val digest = apk.optString("digest").removePrefix("sha256:")
            .takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
        return UpdateRelease(
            tag = json.optString("tag_name"),
            title = json.optString("name").takeIf { it.isNotBlank() } ?: json.optString("tag_name"),
            notes = json.optString("body").take(2500),
            pageUrl = json.optString("html_url").takeIf { isReleasePageUrl(it) } ?: RELEASES_WEB_URL,
            apkUrl = apk.optString("browser_download_url"),
            apkName = name,
            assetSize = apk.optLong("size"),
            sha256 = digest,
            checksumUrl = checksum?.optString("browser_download_url"),
            checksumName = checksum?.optString("name"),
        )
    }

    private fun resolveChecksum(release: UpdateRelease): String? {
        val url = release.checksumUrl ?: return null
        val raw = readText(url, 8192).trim()
        val plain = raw.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
        if (plain != null && release.checksumName.equals("${release.apkName}.sha256", true)) return plain
        return raw.lineSequence().mapNotNull { line ->
            val parts = Regex("^([a-fA-F0-9]{64})\\s+\\*?(.+)$").matchEntire(line.trim())
            if (parts?.groupValues?.get(2) == release.apkName) parts.groupValues[1] else null
        }.firstOrNull()
    }

    private fun downloadFile(release: UpdateRelease, destination: File) {
        val connection = openConnection(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val serverLength = connection.contentLengthLong
            if (serverLength > MAX_APK_BYTES) throw IOException("APK слишком велик")
            var count = 0L
            var lastReported = -1
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        count += read
                        if (count > MAX_APK_BYTES || count > release.assetSize) throw IOException("Размер APK отличается от релиза")
                        output.write(buffer, 0, read)
                        val percent = (count * 100 / release.assetSize).toInt().coerceIn(0, 99)
                        if (percent != lastReported) {
                            lastReported = percent
                            _state.value = _state.value.copy(progressPercent = percent,
                                message = "Скачивание APK: $percent%")
                        }
                    }
                }
            }
            if (count != release.assetSize) throw IOException("APK загружен не полностью")
        } finally {
            connection.disconnect()
        }
    }

    /** Refuse cross-scheme redirects and unexpected hosts before streaming APK bytes. */
    private fun openConnection(rawUrl: String): HttpURLConnection {
        var url = URL(rawUrl)
        repeat(6) {
            val host = url.host.lowercase(Locale.ROOT)
            if (url.protocol != "https" || (host != "api.github.com" && host != "github.com" &&
                    host != "objects.githubusercontent.com" && host != "release-assets.githubusercontent.com")) {
                throw IOException("Небезопасный адрес загрузки")
            }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 12000
                readTimeout = 20000
                setRequestProperty("User-Agent", "FluxReader-Android-Updater")
                setRequestProperty("Accept", if (host == "api.github.com") "application/vnd.github+json" else "application/octet-stream")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val target = connection.getHeaderField("Location") ?: throw IOException("Редирект без адреса")
                val next = URI(url.toString()).resolve(target).toURL()
                connection.disconnect()
                url = next
            } else return connection
        }
        throw IOException("Слишком много переадресаций")
    }

    private fun readText(url: String, limitBytes: Int): String {
        val connection = openConnection(url)
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> throw IOException("Репозиторий или файл недоступен (404)")
                403, 429 -> throw IOException("GitHub ограничил частоту запросов (HTTP $code)")
                else -> throw IOException("HTTP $code")
            }
            connection.inputStream.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    if (output.size() + n > limitBytes) throw IOException("Ответ GitHub слишком велик")
                    output.write(buffer, 0, n)
                }
                return output.toString("UTF-8")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun validateApk(file: File) {
        val pm = app.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("Файл не распознаётся как APK")
        val installed = pm.getPackageInfo(app.packageName, flags)
        if (archive.packageName != app.packageName) throw IOException("APK принадлежит другому приложению")
        if (archive.longVersionCode <= installed.longVersionCode) throw IOException("В APK номер сборки не выше установленного")
        val newSigners = archive.signingInfo?.apkContentsSigners?.map { sha256Bytes(it.toByteArray()) }?.toSet()
        val oldSigners = installed.signingInfo?.apkContentsSigners?.map { sha256Bytes(it.toByteArray()) }?.toSet()
        if (newSigners.isNullOrEmpty() || oldSigners.isNullOrEmpty() || newSigners != oldSigners) {
            throw IOException("Подпись APK отличается от установленной версии. Обновление поверх неё невозможно.")
        }
    }

    private fun sha256Bytes(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun isRepositoryAssetUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host.equals("github.com", true) &&
            uri.path.startsWith("/$REPO/releases/download/")
    }.getOrDefault(false)

    private fun isReleasePageUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host.equals("github.com", true) &&
            uri.path.startsWith("/$REPO/releases/")
    }.getOrDefault(false)

    private fun friendlyError(error: Exception) =
        error.message?.take(180).orEmpty().ifBlank { "ошибка соединения" }
}
