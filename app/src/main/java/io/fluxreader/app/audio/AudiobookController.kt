package io.fluxreader.app.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** SAF-backed audiobooks: no unbounded copy of large M4B/MP3 files into app storage. */
data class AudiobookEntry(
    val id: String,
    val uri: String,
    val name: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val folderPath: String = "",
)

data class AudiobookState(
    val entries: List<AudiobookEntry> = emptyList(),
    val activeId: String? = null,
    val loading: Boolean = false,
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
)

class AudiobookController(private val context: Context) {
    private val prefs = context.getSharedPreferences("audiobook_library_v1", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(AudiobookState(entries = load()))
    val state: StateFlow<AudiobookState> = _state.asStateFlow()
    private var player: MediaPlayer? = null
    private var lastSavedAt = 0L

    private val progressPoll = object : Runnable {
        override fun run() {
            val current = _state.value
            if (!current.playing) return
            val pos = runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
            _state.value = current.copy(positionMs = pos)
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastSavedAt >= 5_000L) {
                savePosition(pos)
                lastSavedAt = now
            }
            handler.postDelayed(this, 500L)
        }
    }

    /** Files remain where the document picker placed them; retain SAF read grants across restarts. */
    fun import(uris: List<Uri>, folderPath: String = ""): Int {
        var added = 0
        val updated = _state.value.entries.toMutableList()
        uris.forEach { uri ->
            if (updated.any { it.uri == uri.toString() }) return@forEach
            val title = displayName(uri) ?: uri.lastPathSegment ?: "Аудиокнига"
            if (!AudiobookFormatPolicy.isAccepted(title)) return@forEach
            val granted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                true
            }.getOrDefault(false)
            if (!granted) return@forEach
            updated += AudiobookEntry(UUID.randomUUID().toString(), uri.toString(), title, folderPath = AudiobookFolderPolicy.normalize(folderPath))
            added++
        }
        _state.value = _state.value.copy(entries = updated, error = if (added == 0) "Поддерживаемые аудиофайлы не добавлены. Выберите MP3, M4A, M4B, AAC, OGG, FLAC или WAV." else null)
        persist()
        return added
    }

    fun play(id: String) {
        if (id == _state.value.activeId && player != null) { resume(); return }
        val entry = _state.value.entries.firstOrNull { it.id == id } ?: return
        pause()
        stopPlayer()
        _state.value = _state.value.copy(activeId = id, loading = true, playing = false,
            positionMs = entry.positionMs, durationMs = entry.durationMs, error = null)
        val preparedPlayer = MediaPlayer()
        player = preparedPlayer
        try {
            preparedPlayer.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            preparedPlayer.setDataSource(context, Uri.parse(entry.uri))
            preparedPlayer.setOnPreparedListener { ready ->
                if (player !== ready) return@setOnPreparedListener
                val length = ready.duration.coerceAtLeast(0).toLong()
                val desired = entry.positionMs.coerceIn(0L, (length - 1_000L).coerceAtLeast(0L))
                if (desired > 0) ready.seekTo(desired.toInt())
                _state.value = _state.value.copy(loading = false, durationMs = length, positionMs = desired)
                updateEntry(entry.id, desired, length)
                resume()
            }
            preparedPlayer.setOnCompletionListener { completed ->
                if (player !== completed) return@setOnCompletionListener
                handler.removeCallbacks(progressPoll)
                runCatching { completed.seekTo(0) }
                _state.value = _state.value.copy(playing = false, positionMs = 0L)
                savePosition(0L)
            }
            preparedPlayer.setOnErrorListener { failed, what, extra ->
                if (player === failed) {
                    _state.value = _state.value.copy(loading = false, playing = false,
                        error = "Android не смог воспроизвести этот файл (код $what/$extra)")
                    stopPlayer()
                }
                true
            }
            preparedPlayer.prepareAsync()
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, playing = false,
                error = e.localizedMessage ?: "Файл недоступен или формат не поддерживается")
            stopPlayer()
        }
    }

    fun resume() {
        val p = player ?: return
        if (_state.value.loading) return
        try {
            if (!p.isPlaying) p.start()
            _state.value = _state.value.copy(playing = true, error = null)
            handler.removeCallbacks(progressPoll)
            handler.post(progressPoll)
        } catch (e: Exception) {
            _state.value = _state.value.copy(playing = false, error = e.localizedMessage ?: "Ошибка воспроизведения")
        }
    }

    fun pause() {
        if (_state.value.loading) {
            // Navigating away while prepareAsync is pending must not auto-start an invisible player.
            stopPlayer()
            _state.value = _state.value.copy(loading = false, playing = false)
            return
        }
        val p = player ?: return
        if (!(_state.value.playing)) return
        runCatching { p.pause() }
        handler.removeCallbacks(progressPoll)
        val pos = runCatching { p.currentPosition.toLong() }.getOrDefault(_state.value.positionMs)
        _state.value = _state.value.copy(playing = false, positionMs = pos)
        savePosition(pos)
    }

    fun seek(ms: Long) {
        val p = player ?: return
        if (_state.value.loading) return
        val limit = _state.value.durationMs
        val target = AudiobookFormatPolicy.seekTarget(ms, limit)
        runCatching { p.seekTo(target.toInt()) }
        _state.value = _state.value.copy(positionMs = target)
        savePosition(target)
    }

    fun skip(deltaMs: Long) = seek(_state.value.positionMs + deltaMs)

    /** Rebase audiobook folder memberships together with library folder renames/moves. */
    fun rebaseFolder(oldPath: String, newPath: String) {
        var changed = false
        val next = _state.value.entries.map { entry ->
            val destination = AudiobookFolderPolicy.rebase(entry.folderPath, oldPath, newPath)
            if (destination == null) entry else {
                changed = true
                entry.copy(folderPath = destination)
            }
        }
        if (changed) { _state.value = _state.value.copy(entries = next); persist() }
    }

    fun move(id: String, folderPath: String) {
        _state.value = _state.value.copy(entries = _state.value.entries.map { entry ->
            if (entry.id == id) entry.copy(folderPath = AudiobookFolderPolicy.normalize(folderPath)) else entry
        })
        persist()
    }

    fun remove(id: String) {
        if (_state.value.activeId == id) {
            pause()
            stopPlayer()
            _state.value = _state.value.copy(activeId = null, positionMs = 0L, durationMs = 0L, playing = false, loading = false)
        }
        val entry = _state.value.entries.firstOrNull { it.id == id } ?: return
        _state.value = _state.value.copy(entries = _state.value.entries.filterNot { it.id == id })
        persist()
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(entry.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun close() { pause(); stopPlayer(); handler.removeCallbacks(progressPoll) }

    private fun stopPlayer() {
        handler.removeCallbacks(progressPoll)
        val previous = player
        player = null
        runCatching { previous?.reset() }
        runCatching { previous?.release() }
    }

    private fun savePosition(ms: Long) {
        val id = _state.value.activeId ?: return
        updateEntry(id, ms, _state.value.durationMs)
    }

    private fun updateEntry(id: String, pos: Long, length: Long) {
        _state.value = _state.value.copy(entries = _state.value.entries.map {
            if (it.id == id) it.copy(positionMs = pos, durationMs = length) else it
        })
        persist()
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun persist() {
        val items = JSONArray()
        _state.value.entries.forEach { e ->
            items.put(JSONObject().put("id", e.id).put("uri", e.uri).put("name", e.name)
                .put("positionMs", e.positionMs).put("durationMs", e.durationMs)
                .put("folderPath", e.folderPath))
        }
        prefs.edit().putString("books", items.toString()).apply()
    }

    private fun load(): List<AudiobookEntry> = runCatching {
        val arr = JSONArray(prefs.getString("books", "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            val id = obj.optString("id")
            val uri = obj.optString("uri")
            if (id.isBlank() || uri.isBlank()) null else AudiobookEntry(
                id, uri, obj.optString("name", "Аудиокнига"),
                obj.optLong("positionMs").coerceAtLeast(0L), obj.optLong("durationMs").coerceAtLeast(0L),
                obj.optString("folderPath", ""),
            )
        }
    }.getOrDefault(emptyList())
}
