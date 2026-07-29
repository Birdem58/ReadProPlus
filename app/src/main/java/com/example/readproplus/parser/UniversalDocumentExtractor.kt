package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.PdfExtractionProgress
import com.example.readproplus.model.pdf.PdfExtractionResult
import com.example.readproplus.pdf.PdfExtractor

class UniversalDocumentExtractor(
    private val pdfExtractor: PdfExtractor,
) {
    private val parsers: List<DocumentParser> = listOf(
        EpubParser(),
        Fb2Parser(),
        CbzCbrParser(),
        DocDocxParser(),
        OdtParser(),
        MobiAzw3Parser(),
        DjVuParser(),
    )

    fun extract(
        context: Context,
        uri: Uri,
        password: String? = null,
        onPdfProgress: (PdfExtractionProgress) -> Unit = {},
    ): PdfExtractionResult {
        val extension = getExtension(context, uri)

        if (extension.equals("pdf", ignoreCase = true)) {
            return pdfExtractor.extract(uri, password, onPdfProgress)
        }

        val parser = parsers.firstOrNull { it.canHandle(extension) }
        if (parser != null) {
            return try {
                val doc = parser.parse(context, uri)
                PdfExtractionResult.Success(doc)
            } catch (e: Exception) {
                PdfExtractionResult.Corrupted(e.message ?: "Failed to parse ${extension.uppercase()} document")
            }
        }

        // Fallback to PDF extractor if format unknown
        return pdfExtractor.extract(uri, password, onPdfProgress)
    }

    companion object {
        fun getExtension(context: Context, uri: Uri): String {
            val scheme = uri.scheme
            if (scheme == "content") {
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType != null) {
                    when {
                        mimeType.contains("pdf") -> return "pdf"
                        mimeType.contains("epub") -> return "epub"
                        mimeType.contains("mobi") || mimeType.contains("x-mobipocket") -> return "mobi"
                        mimeType.contains("amazon.ebook") || mimeType.contains("azw") -> return "azw3"
                        mimeType.contains("fb2") || mimeType.contains("fictionbook") -> return "fb2"
                        mimeType.contains("opendocument.text") || mimeType.contains("odt") -> return "odt"
                        mimeType.contains("wordprocessingml") || mimeType.contains("docx") -> return "docx"
                        mimeType.contains("msword") || mimeType.contains("doc") -> return "doc"
                        mimeType.contains("djvu") -> return "djvu"
                        mimeType.contains("cbr") || mimeType.contains("rar") -> return "cbr"
                        mimeType.contains("cbz") || mimeType.contains("zip") -> return "cbz"
                    }
                }
                val displayName = runCatching {
                    context.contentResolver.query(
                        uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                }.getOrNull().orEmpty()
                val displayExtension = displayName.substringAfterLast('.', "")
                if (displayExtension.isNotBlank()) return displayExtension.lowercase()
            }

            val path = uri.path ?: ""
            val lastSegment = uri.lastPathSegment ?: ""
            val extFromPath = (if (lastSegment.contains('.')) lastSegment else path).substringAfterLast('.', "")
            return if (extFromPath.isNotBlank()) extFromPath.lowercase() else "pdf"
        }
    }
}
