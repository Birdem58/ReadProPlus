package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import com.github.axet.djvulibre.DjvuLibre

class DjVuParser : DocumentParser {

    override fun canHandle(extension: String): Boolean =
        extension.equals("djvu", ignoreCase = true) || extension.equals("djv", ignoreCase = true)

    override fun parse(context: Context, uri: Uri): PdfDocument {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Cannot open DjVu URI: $uri")
        descriptor.use { pfd ->
            val djvu = DjvuLibre(pfd.fileDescriptor)
            return try {
                val pageCount = djvu.getPagesCount()
                require(pageCount > 0) { "DjVu document contains no pages" }

                val fallbackTitle = uri.lastPathSegment?.substringBeforeLast('.')
                    ?.takeIf { it.isNotBlank() } ?: "DjVu Document"
                val title = djvu.getMeta(DjvuLibre.META_TITLE)?.trim().orEmpty()
                    .ifBlank { fallbackTitle }
                val author = djvu.getMeta(DjvuLibre.META_AUTHOR)?.trim()
                    ?.takeIf { it.isNotBlank() }

                val pages = (0 until pageCount).map { pageIndex ->
                    runCatching {
                        djvu.getText(pageIndex, DjvuLibre.ZONE_PAGE)?.text
                            ?.filter { it.isNotBlank() }
                            ?.joinToString(" ")
                            .orEmpty()
                    }.getOrDefault("").ifBlank { "[DjVu page ${pageIndex + 1}]" }
                }
                val bookmarks = runCatching { djvu.getBookmarks().orEmpty().toList() }.getOrDefault(emptyList())
                val toc = if (bookmarks.isNotEmpty()) {
                    bookmarks.map { bookmark ->
                        TocEntry(
                            title = bookmark.title.ifBlank { "Page ${bookmark.page + 1}" },
                            pageNumber = (bookmark.page + 1).coerceIn(1, pageCount),
                            depth = bookmark.level.coerceAtLeast(0),
                        )
                    }
                } else {
                    pages.mapIndexed { index, _ -> TocEntry("Page ${index + 1}", index + 1) }
                }

                PdfDocument(
                    id = uri.toString(),
                    title = title,
                    author = author,
                    totalPages = pageCount,
                    pages = pages,
                    toc = toc,
                    format = FormatType.DJVU,
                    isImageBased = true,
                    sourceUri = uri.toString(),
                )
            } finally {
                djvu.close()
            }
        }
    }
}
