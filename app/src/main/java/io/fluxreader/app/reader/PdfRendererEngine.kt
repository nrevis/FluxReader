package io.fluxreader.app.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.sqrt

object PdfRendererEngine {
    data class RenderedPage(val pageIndex: Int, val pageCount: Int, val bitmap: Bitmap)

    fun pageCount(file: File): Int = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { it.pageCount }
    }

    fun render(
        file: File,
        pageIndex: Int,
        requestedWidthPx: Int,
        maxBitmapPixels: Long = ReaderMemoryBudget.PDF_MAX_BITMAP_PIXELS,
    ): RenderedPage {
        val requested = requestedWidthPx.coerceIn(320, 2800)
        val pixelBudget = maxBitmapPixels.coerceIn(1_000_000L, ReaderMemoryBudget.PDF_MAX_BITMAP_PIXELS)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                require(renderer.pageCount > 0) { "PDF не содержит страниц" }
                val index = pageIndex.coerceIn(0, renderer.pageCount - 1)
                renderer.openPage(index).use { page ->
                    val ratio = page.height.toDouble() / page.width.toDouble().coerceAtLeast(1.0)
                    var bitmapWidth = requested
                    var bitmapHeight = (bitmapWidth * ratio).toInt().coerceAtLeast(1)
                    val pixels = bitmapWidth.toLong() * bitmapHeight.toLong()
                    if (pixels > pixelBudget) {
                        val factor = sqrt(pixelBudget.toDouble() / pixels.toDouble())
                        bitmapWidth = (bitmapWidth * factor).toInt().coerceAtLeast(320)
                        bitmapHeight = (bitmapHeight * factor).toInt().coerceAtLeast(320)
                    }

                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    val scale = bitmapWidth.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                    val matrix = Matrix().apply { postScale(scale, scale) }
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    RenderedPage(index, renderer.pageCount, bitmap)
                }
            }
        }
    }
}
