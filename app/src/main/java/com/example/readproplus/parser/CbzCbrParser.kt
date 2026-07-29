package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import java.io.ByteArrayInputStream

class CbzCbrParser : DocumentParser {

    override fun canHandle(extension: String): Boolean =
        extension.equals("cbz", ignoreCase = true) || extension.equals("cbr", ignoreCase = true)

    override fun parse(context: Context, uri: Uri): PdfDocument {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        val bytes = input.use { it.readBytes() }
        val extension = UniversalDocumentExtractor.getExtension(context, uri)
        val imageFileNames = if (extension == "cbr") {
            listRarImages(bytes)
        } else {
            readZipEntries(ByteArrayInputStream(bytes)).keys.filter(::isImageEntry)
        }.sortedWith(naturalPathComparator)
        if (imageFileNames.isEmpty()) throw IllegalArgumentException("Comic archive contains no supported page images")

        val pages = imageFileNames.mapIndexed { index, fileName ->
            "[Comic Page ${index + 1}: ${fileName.substringAfterLast('/')}]"
        }
        return PdfDocument(
            id = uri.toString(),
            title = uri.lastPathSegment?.substringBeforeLast('.') ?: "Comic Book",
            author = "Comic Archive",
            totalPages = pages.size,
            pages = pages,
            toc = pages.mapIndexed { index, _ -> TocEntry("Page ${index + 1}", index + 1) },
            format = if (extension == "cbr") FormatType.CBR else FormatType.CBZ,
            isImageBased = true,
            sourceUri = uri.toString(),
            pageImageEntries = imageFileNames,
        )
    }

    companion object {
        internal fun isImageEntry(name: String): Boolean {
            val lower = name.lowercase()
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
        }

        internal fun listRarImages(bytes: ByteArray): List<String> =
            Archive(ByteArrayInputStream(bytes)).use { archive ->
                archive.fileHeaders
                    .filter { header: FileHeader -> !header.isDirectory && isImageEntry(header.fileName) }
                    .map { it.fileName }
            }
    }
}
