package com.example.readproplus.pdf

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.readproplus.model.pdf.PdfDocument
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

            if (doc.isEncrypted && password == null) {
                doc.close()
                return PdfExtractionResult.PasswordProtected
            }

            val stripper = PDFTextStripper().apply {
                sortByPosition = true
                addMoreFormatting = true
            }

            val pages = mutableListOf<String>()
            for (i in 0 until doc.numberOfPages) {
                stripper.startPage = i + 1
                stripper.endPage = i + 1
                val text = stripper.getText(doc)
                pages.add(text)
            }

            if (pages.all { it.isBlank() }) {
                doc.close()
                return PdfExtractionResult.NoText
            }

            val metadata = PdfMetadataReader.read(doc)
            val toc = PdfOutlineReader.readOutline(doc)
            doc.close()

            return PdfExtractionResult.Success(
                PdfDocument(
                    id = uri.toString(),
                    title = metadata.title ?: "Untitled",
                    author = metadata.author,
                    totalPages = pages.size,
                    pages = pages,
                    fileSizeBytes = descriptor.statSize,
                    toc = toc,
                )
            )
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
