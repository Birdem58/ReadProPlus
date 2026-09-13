package com.example.readproplus.pdf

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.PdfExtractionProgress
import com.example.readproplus.model.pdf.PdfExtractionResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
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
