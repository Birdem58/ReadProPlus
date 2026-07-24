package com.example.readproplus.pdf

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.readproplus.model.pdf.PdfExtractionProgress
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.coroutineContext

class PdfExtractorTask(
    private val resolver: PdfFileResolver,
) {
    fun extractWithProgress(
        uri: Uri,
        password: String? = null,
    ): Flow<PdfExtractionProgress> = flow {
        val descriptor = resolver.resolve(uri)
        try {
            val doc = loadDocument(descriptor, password)
            try {
                val totalPages = doc.numberOfPages
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    addMoreFormatting = true
                }

                for (i in 0 until totalPages) {
                    coroutineContext.ensureActive()
                    stripper.startPage = i + 1
                    stripper.endPage = i + 1
                    stripper.getText(doc)
                    emit(
                        PdfExtractionProgress(
                            currentPage = i + 1,
                            totalPages = totalPages,
                            percent = (i + 1).toFloat() / totalPages,
                        )
                    )
                }
            } finally {
                doc.close()
            }
        } finally {
            try {
                descriptor.close()
            } catch (_: Exception) {
            }
        }
    }.flowOn(Dispatchers.Default)

    private fun loadDocument(
        descriptor: ParcelFileDescriptor,
        password: String?,
    ): PDDocument {
        val stream = java.io.FileInputStream(descriptor.fileDescriptor)
        return if (password != null) {
            PDDocument.load(stream, password)
        } else {
            PDDocument.load(stream)
        }
    }
}
