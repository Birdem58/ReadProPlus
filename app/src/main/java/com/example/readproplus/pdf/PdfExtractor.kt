package com.example.readproplus.pdf

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.PdfExtractionProgress
import com.example.readproplus.model.pdf.PdfExtractionResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import android.util.Log
import java.io.IOException
import java.util.concurrent.CancellationException

class PdfExtractor(
    private val resolver: PdfFileResolver,
) {
    fun extract(
        uri: Uri,
        password: String? = null,
        onProgress: (PdfExtractionProgress) -> Unit = {},
        checkCancellation: () -> Unit = {},
    ): PdfExtractionResult {
        val descriptor = try {
            resolver.resolve(uri)
        } catch (e: PdfFileTooLargeException) {
            return PdfExtractionResult.TooLarge
        } catch (e: Exception) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        }

        try {
            val fileSizeBytes = descriptor.statSize
            val doc = loadDocument(descriptor, password)
            try {
                val totalPages = doc.numberOfPages
                onProgress(PdfExtractionProgress(0, totalPages, 0f))

                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    addMoreFormatting = true
                }

                val pages = mutableListOf<String>()
                for (i in 0 until totalPages) {
                    checkCancellation()
                    stripper.startPage = i + 1
                    stripper.endPage = i + 1
                    val text = PdfTextNormalizer.normalizePage(stripper.getText(doc))
                    pages.add(text)
                    checkCancellation()
                    onProgress(
                        PdfExtractionProgress(
                            currentPage = i + 1,
                            totalPages = totalPages,
                            percent = (i + 1).toFloat() / totalPages,
                        )
                    )
                }

                val isImageBased = pages.isNotEmpty() && pages.all { it.isBlank() }
                val displayPages = if (isImageBased) {
                    pages.mapIndexed { index, _ -> "[PDF page ${index + 1}]" }
                } else {
                    pages
                }

                val toc = PdfOutlineReader.readOutline(doc)
                val fallbackTitle = PdfTitleResolver.fromDisplayName(resolver.getDisplayName(uri))
                    ?: PdfTitleResolver.fromUri(uri)
                val metadata = PdfMetadataReader.read(doc, fallbackTitle)
                val title = PdfTitleResolver.usableTitle(metadata.title)
                    ?: toc.firstOrNull()?.title?.takeIf { it.isNotBlank() }
                    ?: pages.firstOrNull()?.lineSequence()
                        ?.map { it.trim() }
                        ?.firstOrNull { it.length in 3..160 }
                    ?: fallbackTitle
                    ?: "Untitled"

                return PdfExtractionResult.Success(
                    PdfDocument(
                        id = uri.toString(),
                        title = title,
                        author = metadata.author,
                        subject = metadata.subject,
                        keywords = metadata.keywords,
                        totalPages = pages.size,
                        pages = displayPages,
                        fileSizeBytes = fileSizeBytes,
                        toc = toc,
                        format = com.example.readproplus.model.pdf.FormatType.PDF,
                        isImageBased = isImageBased,
                        sourceUri = uri.toString(),
                    )
                )
            } finally {
                doc.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: InvalidPasswordException) {
            return if (password.isNullOrEmpty()) {
                PdfExtractionResult.PasswordProtected
            } else {
                PdfExtractionResult.WrongPassword()
            }
        } catch (e: IOException) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        } catch (e: Exception) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        }
    }

    fun openFast(
        uri: Uri,
        password: String? = null,
    ): PdfExtractionResult {
        val descriptor = try {
            resolver.resolve(uri)
        } catch (e: PdfFileTooLargeException) {
            return PdfExtractionResult.TooLarge
        } catch (e: Exception) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        }

        try {
            val fileSizeBytes = descriptor.statSize
            val doc = loadDocument(descriptor, password)
            try {
                val totalPages = doc.numberOfPages
                val toc = PdfOutlineReader.readOutline(doc)
                val fallbackTitle = PdfTitleResolver.fromDisplayName(resolver.getDisplayName(uri))
                    ?: PdfTitleResolver.fromUri(uri)
                val metadata = PdfMetadataReader.read(doc, fallbackTitle)
                val title = PdfTitleResolver.usableTitle(metadata.title)
                    ?: toc.firstOrNull()?.title?.takeIf { it.isNotBlank() }
                    ?: fallbackTitle
                    ?: "Untitled"

                val placeholderPages = List(totalPages) { index -> "" }

                return PdfExtractionResult.Success(
                    PdfDocument(
                        id = uri.toString(),
                        title = title,
                        author = metadata.author,
                        subject = metadata.subject,
                        keywords = metadata.keywords,
                        totalPages = totalPages,
                        pages = placeholderPages,
                        fileSizeBytes = fileSizeBytes,
                        toc = toc,
                        format = com.example.readproplus.model.pdf.FormatType.PDF,
                        isImageBased = false,
                        sourceUri = uri.toString(),
                    )
                )
            } finally {
                doc.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: InvalidPasswordException) {
            return if (password.isNullOrEmpty()) {
                PdfExtractionResult.PasswordProtected
            } else {
                PdfExtractionResult.WrongPassword()
            }
        } catch (e: IOException) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        } catch (e: Exception) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        }
    }

    fun extractPageText(
        uri: Uri,
        password: String? = null,
        pageIndex: Int,
    ): String {
        return try {
            val descriptor = resolver.resolve(uri)
            val doc = loadDocument(descriptor, password)
            try {
                if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return ""
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    addMoreFormatting = true
                    startPage = pageIndex + 1
                    endPage = pageIndex + 1
                }
                PdfTextNormalizer.normalizePage(stripper.getText(doc))
            } finally {
                doc.close()
            }
        } catch (e: Exception) {
            Log.e("PdfExtractor", "Failed to extract text for page $pageIndex from $uri", e)
            ""
        }
    }

    /**
     * Extracts text for a range of pages (0-based inclusive) in a single document load.
     * Prevents repetitive file opening and parsing overhead.
     */
    fun extractPageRangeText(
        uri: Uri,
        password: String? = null,
        startPageIndex: Int,
        endPageIndex: Int,
    ): Map<Int, String> {
        return try {
            val descriptor = resolver.resolve(uri)
            val doc = loadDocument(descriptor, password)
            try {
                val totalPages = doc.numberOfPages
                if (totalPages <= 0) return emptyMap()
                val validStart = startPageIndex.coerceIn(0, totalPages - 1)
                val validEnd = endPageIndex.coerceIn(validStart, totalPages - 1)
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    addMoreFormatting = true
                }
                val result = mutableMapOf<Int, String>()
                for (pageIdx in validStart..validEnd) {
                    stripper.startPage = pageIdx + 1
                    stripper.endPage = pageIdx + 1
                    val text = PdfTextNormalizer.normalizePage(stripper.getText(doc))
                    result[pageIdx] = text
                }
                result
            } finally {
                doc.close()
            }
        } catch (e: Exception) {
            Log.e("PdfExtractor", "Failed to extract page range $startPageIndex..$endPageIndex from $uri", e)
            emptyMap()
        }
    }

    private fun loadDocument(
        descriptor: ParcelFileDescriptor,
        password: String?,
    ): PDDocument {
        // PDFBox copies the input into its scratch storage while loading, so
        // the input stream can be closed as soon as load() returns. The
        // AutoCloseInputStream also closes the descriptor on every exit path.
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            return if (password != null) {
                PDDocument.load(stream, password)
            } else {
                PDDocument.load(stream)
            }
        }
    }
}
