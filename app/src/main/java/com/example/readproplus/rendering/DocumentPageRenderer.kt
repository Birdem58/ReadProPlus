package com.example.readproplus.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.parser.readZipEntries
import com.github.axet.djvulibre.DjvuLibre
import com.github.junrar.Archive
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/** Renders a page from the original source, including content:// SAF URIs. */
object DocumentPageRenderer {

    fun render(context: Context, document: PdfDocument, pageIndex: Int, maxWidthPx: Int = 2048): Bitmap? {
        if (pageIndex < 0 || pageIndex >= document.totalPages) return null
        return runCatching {
            when (document.format) {
                FormatType.PDF -> renderPdf(context, document, pageIndex, maxWidthPx)
                FormatType.CBZ -> renderZipImage(context, document, pageIndex)
                FormatType.CBR -> renderRarImage(context, document, pageIndex)
                FormatType.DJVU -> renderDjVu(context, document, pageIndex, maxWidthPx)
                else -> null
            }
        }.getOrNull()
    }

    private fun renderPdf(context: Context, document: PdfDocument, pageIndex: Int, maxWidthPx: Int): Bitmap? {
        val descriptor = openDescriptor(context, document) ?: return null
        descriptor.use { pfd ->
            val renderer = PdfRenderer(pfd)
            return try {
                if (pageIndex !in 0 until renderer.pageCount) return null
                val page = renderer.openPage(pageIndex)
                try {
                    val width = page.width.coerceAtMost(maxWidthPx.coerceAtLeast(256))
                    val height = (page.height.toFloat() * width / page.width).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                } finally {
                    page.close()
                }
            } finally {
                renderer.close()
            }
        }
    }

    private fun renderZipImage(context: Context, document: PdfDocument, pageIndex: Int): Bitmap? {
        val entryName = document.pageImageEntries.getOrNull(pageIndex) ?: return null
        val input = openSource(context, document) ?: return null
        return input.use {
            val bytes = readZipEntries(it)[entryName]
            decode(bytes)
        }
    }

    private fun renderRarImage(context: Context, document: PdfDocument, pageIndex: Int): Bitmap? {
        val entryName = document.pageImageEntries.getOrNull(pageIndex) ?: return null
        val input = openSource(context, document) ?: return null
        val bytes = input.use(InputStream::readBytes)
        val archive = Archive(ByteArrayInputStream(bytes))
        return try {
            val header = archive.fileHeaders.firstOrNull {
                !it.isDirectory && it.fileName.equals(entryName, ignoreCase = true)
            } ?: return null
            archive.getInputStream(header).use { decode(it.readBytes()) }
        } finally {
            archive.close()
        }
    }

    private fun renderDjVu(context: Context, document: PdfDocument, pageIndex: Int, maxWidthPx: Int): Bitmap? {
        val descriptor = openDescriptor(context, document) ?: return null
        descriptor.use { pfd ->
            val djvu = DjvuLibre(pfd.fileDescriptor)
            return try {
                if (pageIndex !in 0 until djvu.getPagesCount()) return null
                val info = djvu.getPageInfo(pageIndex)
                val width = info.width.coerceAtMost(maxWidthPx.coerceAtLeast(256))
                val height = (info.height.toFloat() * width / info.width).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    djvu.renderPage(bitmap, pageIndex, 0, 0, info.width, info.height, 0, 0, width, height)
                }
            } finally {
                djvu.close()
            }
        }
    }

    private fun decode(bytes: ByteArray?): Bitmap? = bytes?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }

    private fun openSource(context: Context, document: PdfDocument): InputStream? {
        val source = document.sourceUri ?: document.filePath ?: return null
        return if (source.startsWith("content:") || source.startsWith("file:")) {
            context.contentResolver.openInputStream(Uri.parse(source))
        } else {
            File(source).inputStream()
        }
    }

    private fun openDescriptor(context: Context, document: PdfDocument): ParcelFileDescriptor? {
        val source = document.sourceUri ?: document.filePath ?: return null
        return if (source.startsWith("content:") || source.startsWith("file:")) {
            context.contentResolver.openFileDescriptor(Uri.parse(source), "r")
        } else {
            ParcelFileDescriptor.open(File(source), ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}
