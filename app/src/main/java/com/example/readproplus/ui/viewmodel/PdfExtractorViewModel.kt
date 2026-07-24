package com.example.readproplus.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.readproplus.data.AsyncState
import com.example.readproplus.data.BookCache
import com.example.readproplus.data.HighlightRepository
import com.example.readproplus.data.PdfRepository
import com.example.readproplus.model.Highlight
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.pdf.PdfExtractor
import com.example.readproplus.pdf.PdfExtractorTask
import com.example.readproplus.pdf.PdfFileResolver
import com.example.readproplus.pdf.PdfPasswordCache
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PdfExtractorUiState {
    data object Idle : PdfExtractorUiState
    data class Loading(val progress: Float) : PdfExtractorUiState
    data class NeedsPassword(val uri: Uri, val displayName: String?) : PdfExtractorUiState
    data class Error(val message: String) : PdfExtractorUiState
    data class Success(val document: PdfDocument) : PdfExtractorUiState
}

class PdfExtractorViewModel(application: Application) : AndroidViewModel(application) {

    init {
        // PDFBox-Android requires this before any PDF operations.
        // Without it, PDDocument.load() crashes with NullPointerException
        // when trying to load internal font/encoding resources.
        PDFBoxResourceLoader.init(application)
    }

    private val resolver = PdfFileResolver(application)
    private val extractor = PdfExtractor(resolver)
    private val extractorTask = PdfExtractorTask(resolver)
    private val cache = BookCache()
    private val passwordCache = PdfPasswordCache()
    private val repository = PdfRepository(extractor, extractorTask, cache)
    private val highlightRepository = HighlightRepository(application)

    private val _state = MutableStateFlow<PdfExtractorUiState>(PdfExtractorUiState.Idle)
    val state: StateFlow<PdfExtractorUiState> = _state.asStateFlow()

    private val _libraryBooks = MutableStateFlow<List<PdfDocument>>(emptyList())
    val libraryBooks: StateFlow<List<PdfDocument>> = _libraryBooks.asStateFlow()

    private val _selectedDocument = MutableStateFlow<PdfDocument?>(null)
    val selectedDocument: StateFlow<PdfDocument?> = _selectedDocument.asStateFlow()

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights.asStateFlow()

    private var extractionJob: Job? = null

    init {
        refreshLibrary()
        refreshHighlights()
    }

    fun onPdfPicked(uri: Uri) {
        val displayName = getPdfDisplayName(uri)
        val cachedPassword = passwordCache.get(uri.toString())
        startExtraction(uri, displayName, cachedPassword)
    }

    private fun startExtraction(
        uri: Uri,
        displayName: String?,
        password: String?,
    ) {
        extractionJob?.cancel()
        _state.value = PdfExtractorUiState.Loading(0f)
        extractionJob = viewModelScope.launch {
            repository.addBook(uri, password).collect { asyncState ->
                when (asyncState) {
                    is AsyncState.Loading -> {
                        _state.value = PdfExtractorUiState.Loading(asyncState.progress)
                    }
                    is AsyncState.Success -> {
                        password?.let { passwordCache.put(uri.toString(), it) }
                        _state.value = PdfExtractorUiState.Success(asyncState.data)
                        refreshLibrary()
                    }
                    is AsyncState.PasswordProtected -> {
                        _state.value = PdfExtractorUiState.NeedsPassword(uri, displayName)
                    }
                    is AsyncState.Error -> {
                        _state.value = PdfExtractorUiState.Error(asyncState.message)
                    }
                    is AsyncState.Idle -> {}
                }
            }
        }
    }

    fun onPasswordEntered(password: String) {
        val currentState = _state.value
        if (currentState is PdfExtractorUiState.NeedsPassword) {
            startExtraction(currentState.uri, currentState.displayName, password)
        }
    }

    fun onPasswordCancelled() {
        _state.value = PdfExtractorUiState.Idle
    }

    fun onBookSelected(document: PdfDocument) {
        _selectedDocument.value = document
    }

    fun onReturnToLibrary() {
        _selectedDocument.value = null
    }

    fun removeBook(id: String) {
        repository.removeBook(id)
        passwordCache.remove(id)
        refreshLibrary()
    }

    fun dismissError() {
        _state.value = PdfExtractorUiState.Idle
    }

    fun toggleHighlight(bookId: String, bookTitle: String, pageNumber: Int, text: String, color: Long) {
        highlightRepository.toggle(bookId, bookTitle, pageNumber, text, color)
        refreshHighlights()
    }

    fun removeHighlight(id: String) {
        highlightRepository.remove(id)
        refreshHighlights()
    }

    fun getHighlightsForBook(bookId: String): List<Highlight> = highlightRepository.getByBook(bookId)

    fun getHighlightsForPage(bookId: String, pageNumber: Int): Set<String> {
        return highlightRepository.getByBookAndPage(bookId, pageNumber).map { it.text.trim() }.toSet()
    }

    private fun refreshHighlights() {
        _highlights.value = highlightRepository.getAll()
    }

    private fun refreshLibrary() {
        _libraryBooks.value = repository.listBooks()
    }

    private fun getPdfDisplayName(uri: Uri): String? {
        val context = getApplication<Application>()
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        return name
    }
}
