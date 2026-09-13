package com.example.readproplus.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance PDF renderer manager with persistent session,
 * aspect ratio pre-caching, and LruCache to prevent OOM and stutter.
 */
object PdfPageRendererManager {

    private const val MAX_CACHE_SIZE_PAGES = 16

    private val lruCache = object : LruCache<Int, Bitmap>(MAX_CACHE_SIZE_PAGES) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Let GC collect or recycle if evicted
        }
    }

    private val dimensionsCache = mutableMapOf<Int, Pair<Int, Int>>()
    private val mutex = Mutex()

    private var currentDocumentId: String? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var currentRenderer: PdfRenderer? = null

    /**
     * Retrieves cached page bitmap synchronously if available.
     */
    fun getCachedBitmap(pageIndex: Int): Bitmap? {
        val bitmap = lruCache.get(pageIndex)
        return if (bitmap != null && !bitmap.isRecycled) bitmap else null
    }

    /**
     * Gets aspect ratio (width / height) of the page.
     * Defaults to standard A4 (0.707f) if not yet known.
     */
    fun getPageAspectRatio(pageIndex: Int): Float {
        val dims = dimensionsCache[pageIndex]
        return if (dims != null && dims.second > 0) {
            dims.first.toFloat() / dims.second.toFloat()
        } else {
            0.7071f
        }
    }

    /**
     * Renders a page asynchronously on Dispatchers.IO.
     * Uses LruCache first, then renders and caches.
     */
    suspend fun renderPage(
        context: Context,
        document: PdfDocument,
        pageIndex: Int,
        maxWidthPx: Int = 1440,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= document.totalPages) return@withContext null

        // Check in-memory LRU cache first
        getCachedBitmap(pageIndex)?.let { return@withContext it }

        // Non-PDF formats fallback to DocumentPageRenderer
        if (document.format != FormatType.PDF) {
            val bitmap = DocumentPageRenderer.render(context, document, pageIndex, maxWidthPx)
            if (bitmap != null) {
                dimensionsCache[pageIndex] = Pair(bitmap.width, bitmap.height)
                lruCache.put(pageIndex, bitmap)
            }
            return@withContext bitmap
        }

        mutex.withLock {
            // Double-check cache inside lock
            getCachedBitmap(pageIndex)?.let { return@withContext it }

            try {
                ensureRendererOpen(context, document)
                val renderer = currentRenderer ?: return@withContext null
                if (pageIndex !in 0 until renderer.pageCount) return@withContext null

                val page = renderer.openPage(pageIndex)
                try {
                    val originalWidth = page.width
                    val originalHeight = page.height
                    dimensionsCache[pageIndex] = Pair(originalWidth, originalHeight)

                    val targetWidth = originalWidth.coerceAtMost(maxWidthPx.coerceAtLeast(480))
                    val targetHeight = (originalHeight.toFloat() * targetWidth / originalWidth).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    lruCache.put(pageIndex, bitmap)
                    return@withContext bitmap
                } finally {
                    page.close()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun ensureRendererOpen(context: Context, document: PdfDocument) {
        if (currentDocumentId == document.id && currentRenderer != null) {
            return
        }
        closeSession()

        val pfd = openDescriptor(context, document) ?: return
        try {
            val renderer = PdfRenderer(pfd)
            currentDocumentId = document.id
            currentPfd = pfd
            currentRenderer = renderer
        } catch (e: Exception) {
            pfd.close()
            currentPfd = null
            currentRenderer = null
        }
    }

    private fun openDescriptor(context: Context, document: PdfDocument): ParcelFileDescriptor? {
        val source = document.sourceUri ?: document.filePath ?: return null
        return try {
            if (source.startsWith("content:") || source.startsWith("file:")) {
                context.contentResolver.openFileDescriptor(Uri.parse(source), "r")
            } else {
                ParcelFileDescriptor.open(File(source), ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun closeSession() {
        try {
            currentRenderer?.close()
        } catch (_: Exception) {}
        try {
            currentPfd?.close()
        } catch (_: Exception) {}

        currentRenderer = null
        currentPfd = null
        currentDocumentId = null
        lruCache.evictAll()
        dimensionsCache.clear()
    }
}
