package com.example.readproplus.data

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.PdfExtractionResult
import com.example.readproplus.parser.UniversalDocumentExtractor
import com.example.readproplus.pdf.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

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
            val result = universalExtractor.extract(context, uri, password) { progress ->
                trySend(
                    AsyncState.Loading(
                        progress = progress.percent,
                        currentPage = progress.currentPage,
                        totalPages = progress.totalPages,
                    )
                )
            }
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
        } catch (e: Exception) {
            send(AsyncState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)


    fun getBook(id: String): PdfDocument? = cache.get(id)

    fun listBooks(): List<PdfDocument> = cache.all()

    fun removeBook(id: String) {
        cache.remove(id)
    }
}
