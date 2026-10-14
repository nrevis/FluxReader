package io.fluxreader.app.data

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import io.fluxreader.app.reader.ReaderMemoryBudget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Process-local cover preview cache for the library.
 *
 * Cover decoding used to happen synchronously from Compose, which could block the main thread
 * while a large grid was being populated. Decoding now happens on Dispatchers.IO and previews
 * are bounded by a small LRU that scales with the device memory class.
 */
internal object LibraryCoverCache {
    private val lock = Any()
    @Volatile private var cache: LruCache<String, Bitmap>? = null

    suspend fun load(context: Context, path: String, maxSidePx: Int): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.isFile) return null
        val bucket = previewBucket(maxSidePx)
        val key = cacheKey(file, bucket)
        getOrCreateCache(context).get(key)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val decoded = withContext(Dispatchers.IO) { decodeCoverPreview(file, bucket) } ?: return null
        synchronized(lock) {
            getOrCreateCache(context).put(key, decoded)
        }
        return decoded
    }

    fun peek(context: Context, path: String, maxSidePx: Int): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.isFile) return null
        val cached = getOrCreateCache(context).get(cacheKey(file, previewBucket(maxSidePx)))
        return cached?.takeUnless { it.isRecycled }
    }

    fun trimForReader() {
        synchronized(lock) {
            cache?.let { current ->
                current.trimToSize((current.maxSize() / 4).coerceAtLeast(512))
            }
        }
    }

    fun clear() {
        synchronized(lock) { cache?.evictAll() }
    }

    private fun getOrCreateCache(context: Context): LruCache<String, Bitmap> {
        cache?.let { return it }
        synchronized(lock) {
            cache?.let { return it }
            val memoryClassMb = context.applicationContext
                .getSystemService(ActivityManager::class.java)
                ?.memoryClass
                ?: 256
            return object : LruCache<String, Bitmap>(ReaderMemoryBudget.libraryCoverCacheKb(memoryClassMb)) {
                override fun sizeOf(key: String, value: Bitmap): Int =
                    (value.byteCount / 1024).coerceAtLeast(1)
            }.also { cache = it }
        }
    }

    private fun cacheKey(file: File, bucket: Int): String =
        "${file.absolutePath}:${file.length()}:${file.lastModified()}:$bucket"

    private fun previewBucket(maxSidePx: Int): Int {
        val safe = maxSidePx.coerceIn(160, 960)
        return ((safe + 159) / 160) * 160
    }

    private fun decodeCoverPreview(file: File, maxSidePx: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (bounds.outWidth / sample > maxSidePx * 2 || bounds.outHeight / sample > maxSidePx * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }.getOrNull()
}
