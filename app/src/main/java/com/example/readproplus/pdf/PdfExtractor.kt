package com.example.readproplus.pdf

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.PdfExtractionProgress
import com.example.readproplus.model.pdf.PdfExtractionResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.IOException

class PdfExtractor(
    private val resolver: PdfFileResolver,
) {
    fun extract(
        uri: Uri,
        password: String? = null,
        onProgress: (PdfExtractionProgress) -> Unit = {},
    ): PdfExtractionResult {
        val descriptor = try {
            resolver.resolve(uri)
        } catch (e: PdfFileTooLargeException) {
            return PdfExtractionResult.TooLarge
        } catch (e: Exception) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        }

        try {
            val doc = loadDocument(descriptor, password)
            try {
                if (doc.isEncrypted && password == null) {
                    return PdfExtractionResult.PasswordProtected
                }

                val totalPages = doc.numberOfPages
                onProgress(PdfExtractionProgress(0, totalPages, 0f))

                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    addMoreFormatting = true
                }

                val pages = mutableListOf<String>()
                for (i in 0 until totalPages) {
                    stripper.startPage = i + 1
                    stripper.endPage = i + 1
                    val text = stripper.getText(doc)
                    pages.add(text)
                    onProgress(
                        PdfExtractionProgress(
                            currentPage = i + 1,
                            totalPages = totalPages,
                            percent = (i + 1).toFloat() / totalPages,
                        )
                    )
                }

                val isImageBased = pages.all { it.isBlank() }
                val displayPages = if (isImageBased) {
                    pages.mapIndexed { index, _ -> "[PDF page ${index + 1}]" }
                } else {
                    pages
                }

                val toc = PdfOutlineReader.readOutline(doc)
                val fallbackTitle = resolver.getDisplayName(uri)
                    ?.substringBeforeLast('.', missingDelimiterValue = "")
                    ?.takeIf { it.isNotBlank() }
                    ?: uri.lastPathSegment
                        ?.substringBeforeLast('.', missingDelimiterValue = "")
                        ?.takeIf { it.isNotBlank() }
                val metadata = PdfMetadataReader.read(doc, fallbackTitle)
                val title = metadata.title
                    ?.takeIf { it.isNotBlank() }
                    ?: toc.firstOrNull()?.title?.takeIf { it.isNotBlank() }
                    ?: pages.firstOrNull()?.lineSequence()
                        ?.map { it.trim() }
                        ?.firstOrNull { it.length in 3..160 }
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
                        fileSizeBytes = descriptor.statSize,
                        toc = toc,
                        format = com.example.readproplus.model.pdf.FormatType.PDF,
                        isImageBased = isImageBased,
                        sourceUri = uri.toString(),
                    )
                )
            } finally {
                doc.close()
            }
        } catch (e: IOException) {
            if (e.message?.contains("Invalid password", ignoreCase = true) == true ||
                e.message?.contains("wrong password", ignoreCase = true) == true
            ) {
                return PdfExtractionResult.WrongPassword()
            }
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        } catch (e: Exception) {
            return PdfExtractionResult.Corrupted(e.message ?: "Unknown error")
        } finally {
            try {
                descriptor.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun loadDocument(
        descriptor: ParcelFileDescriptor,
        password: String?,
    ): PDDocument {
        // Use FileInputStream instead of AutoCloseInputStream to avoid
        // prematurely closing the ParcelFileDescriptor. The caller manages
        // the descriptor lifecycle in its own finally block.
        val stream = java.io.FileInputStream(descriptor.fileDescriptor)
        return if (password != null) {
            PDDocument.load(stream, password)
        } else {
            PDDocument.load(stream)
        }
    }
}
