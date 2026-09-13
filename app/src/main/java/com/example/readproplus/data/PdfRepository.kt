package com.example.readproplus.data

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.PdfExtractionResult
import com.example.readproplus.parser.UniversalDocumentExtractor
import com.example.readproplus.pdf.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import java.util.concurrent.CancellationException

sealed class AsyncState<out T> {
    data object Idle : AsyncState<Nothing>()
    data class Loading(
        val progress: Float,
        val currentPage: Int = 0,
        val totalPages: Int = 0,
    ) : AsyncState<Nothing>()
    data class Success<T>(val data: T) : AsyncState<T>()
    data class Error(val message: String) : AsyncState<Nothing>()
    data object PasswordProtected : AsyncState<Nothing>()
}

class PdfRepository(
    private val context: Context,
    private val extractor: PdfExtractor,
    private val cache: BookCache,
) {
    private val universalExtractor = UniversalDocumentExtractor(extractor)

    fun addBook(
        uri: Uri,
        password: String? = null,
    ): Flow<AsyncState<PdfDocument>> = channelFlow {
        send(AsyncState.Loading(0f))
        try {
            // Fast open first: allows opening books in <50ms without waiting for full page-by-page text stripper
            val fastResult = universalExtractor.openFast(context, uri, password)
            when (fastResult) {
                is PdfExtractionResult.Success -> {
                    cache.put(fastResult.document)
                    send(AsyncState.Success(fastResult.document))
                    // Background text enrichment for search & reflow
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            val fullResult = universalExtractor.extract(
                                context = context,
                                uri = uri,
                                password = password,
                            )
                            if (fullResult is PdfExtractionResult.Success) {
                                cache.put(fullResult.document)
                            }
                        }
                    }
                    return@channelFlow
                }
                is PdfExtractionResult.PasswordProtected -> {
                    send(AsyncState.PasswordProtected)
                    return@channelFlow
                }
                is PdfExtractionResult.WrongPassword -> {
                    send(AsyncState.Error("Incorrect password"))
                    return@channelFlow
                }
                is PdfExtractionResult.TooLarge -> {
                    send(AsyncState.Error("File too large (max 50 MB)"))
                    return@channelFlow
                }
                else -> {
                    // Fall back to full extract below
                }
            }

            val extractionContext = coroutineContext
            val result = universalExtractor.extract(
                context = context,
                uri = uri,
                password = password,
                onPdfProgress = { progress ->
                    extractionContext.ensureActive()
                    trySend(
                        AsyncState.Loading(
                            progress = progress.percent,
                            currentPage = progress.currentPage,
                            totalPages = progress.totalPages,
                        )
                    )
                },
                checkCancellation = { extractionContext.ensureActive() },
            )
            when (result) {
                is PdfExtractionResult.Success -> {
                    cache.put(result.document)
                    send(AsyncState.Success(result.document))
                }
                is PdfExtractionResult.PasswordProtected -> {
                    send(AsyncState.PasswordProtected)
                }
                is PdfExtractionResult.WrongPassword -> {
                    send(AsyncState.Error("Incorrect password"))
                }
                is PdfExtractionResult.Corrupted -> {
                    send(AsyncState.Error(result.error))
                }
                is PdfExtractionResult.TooLarge -> {
                    send(AsyncState.Error("File too large (max 50 MB)"))
                }
                is PdfExtractionResult.NoText -> {
                    send(AsyncState.Error("This document has no selectable text"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            send(AsyncState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    fun extractPageText(document: PdfDocument, pageIndex: Int, password: String? = null): String {
        val cachedText = document.pages.getOrNull(pageIndex)
        if (!cachedText.isNullOrBlank()) {
            return cachedText
        }
        val uri = document.sourceUri?.let { Uri.parse(it) } ?: Uri.parse(document.id)
        return extractor.extractPageText(uri, password, pageIndex)
    }

    fun extractPageRangeText(
        document: PdfDocument,
        startPageIndex: Int,
        endPageIndex: Int,
        password: String? = null,
    ): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val missingPages = mutableListOf<Int>()
        for (page in startPageIndex..endPageIndex) {
            val cached = document.pages.getOrNull(page)
            if (!cached.isNullOrBlank()) {
                result[page] = cached
            } else {
                missingPages.add(page)
            }
        }
        if (missingPages.isNotEmpty()) {
            val uriStr = document.sourceUri?.takeIf { it.isNotBlank() } ?: document.id
            val uri = Uri.parse(uriStr)
            val extracted = extractor.extractPageRangeText(
                uri = uri,
                password = password,
                startPageIndex = missingPages.minOrNull() ?: startPageIndex,
                endPageIndex = missingPages.maxOrNull() ?: endPageIndex,
            )
            extracted.forEach { (page, text) ->
                result[page] = text
            }
        }
        return result
    }


    fun getBook(id: String): PdfDocument? = cache.get(id)

    fun listBooks(): List<PdfDocument> = cache.all()

    fun removeBook(id: String) {
        cache.remove(id)
    }
}
