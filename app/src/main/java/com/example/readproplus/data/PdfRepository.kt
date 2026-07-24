package com.example.readproplus.data

import android.net.Uri
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.PdfExtractionResult
import com.example.readproplus.pdf.PdfExtractor
import com.example.readproplus.pdf.PdfExtractorTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed class AsyncState<out T> {
    data object Idle : AsyncState<Nothing>()
    data class Loading(val progress: Float) : AsyncState<Nothing>()
    data class Success<T>(val data: T) : AsyncState<T>()
    data class Error(val message: String) : AsyncState<Nothing>()
    data object PasswordProtected : AsyncState<Nothing>()
}

class PdfRepository(
    private val extractor: PdfExtractor,
    private val extractorTask: PdfExtractorTask,
    private val cache: BookCache,
) {
    fun addBook(
        uri: Uri,
        password: String? = null,
    ): Flow<AsyncState<PdfDocument>> = flow {
        emit(AsyncState.Loading(0f))
        try {
            // Single extraction pass — extract() both parses the PDF and
            // produces the final result. No need to read the file twice.
            val result = extractor.extract(uri, password)
            when (result) {
                is PdfExtractionResult.Success -> {
                    cache.put(result.document)
                    emit(AsyncState.Success(result.document))
                }
                is PdfExtractionResult.PasswordProtected -> {
                    emit(AsyncState.PasswordProtected)
                }
                is PdfExtractionResult.WrongPassword -> {
                    emit(AsyncState.Error("Incorrect password"))
                }
                is PdfExtractionResult.Corrupted -> {
                    emit(AsyncState.Error(result.error))
                }
                is PdfExtractionResult.TooLarge -> {
                    emit(AsyncState.Error("File too large (max 50 MB)"))
                }
                is PdfExtractionResult.NoText -> {
                    emit(AsyncState.Error("This PDF has no selectable text"))
                }
            }
        } catch (e: Exception) {
            emit(AsyncState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getBook(id: String): PdfDocument? = cache.get(id)

    fun listBooks(): List<PdfDocument> = cache.all()

    fun removeBook(id: String) {
        cache.remove(id)
    }
}
