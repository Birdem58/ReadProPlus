package com.example.readproplus.ui.viewmodel

import android.app.Application
import android.content.Intent
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
import com.example.readproplus.pdf.PdfFileResolver
import com.example.readproplus.pdf.PdfPasswordCache
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PdfExtractorUiState {
    data object Idle : PdfExtractorUiState
    data class Loading(
        val progress: Float,
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val currentDocument: Int = 1,
        val totalDocuments: Int = 1,
    ) : PdfExtractorUiState
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
    private val cache = BookCache(application)
    private val passwordCache = PdfPasswordCache()
    private val repository = PdfRepository(application, extractor, cache)
    private val highlightRepository = HighlightRepository(application)

    private val _state = MutableStateFlow<PdfExtractorUiState>(PdfExtractorUiState.Idle)
    val state: StateFlow<PdfExtractorUiState> = _state.asStateFlow()

    private val _libraryBooks = MutableStateFlow<List<PdfDocument>>(emptyList())
    val libraryBooks: StateFlow<List<PdfDocument>> = _libraryBooks.asStateFlow()

    private val _selectedDocument = MutableStateFlow<PdfDocument?>(null)
    val selectedDocument: StateFlow<PdfDocument?> = _selectedDocument.asStateFlow()

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights.asStateFlow()

    private val cacheReady = CompletableDeferred<Unit>()
    private var extractionJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    cache.load()
                }
            } finally {
                // Imports must not race the background restore if the user
                // immediately selects a file from the library screen.
                cacheReady.complete(Unit)
            }
            refreshLibrary()
            refreshHighlights()
        }
    }

    fun onPdfPicked(uri: Uri) {
        persistReadPermission(uri)
        val displayName = getPdfDisplayName(uri)
        val cachedPassword = passwordCache.get(uri.toString())
        startExtraction(uri, displayName, cachedPassword)
    }

    fun onDocumentsPicked(uris: List<Uri>) {
        val candidateUris = uris.distinctBy { it.toString() }
        if (candidateUris.isEmpty()) return
        candidateUris.forEach(::persistReadPermission)
        extractionJob?.cancel()
        _state.value = PdfExtractorUiState.Loading(
            progress = 0f,
            currentDocument = 1,
            totalDocuments = candidateUris.size,
        )
        extractionJob = viewModelScope.launch {
            cacheReady.await()
            val distinctUris = candidateUris.filterNot { cache.get(it.toString()) != null }
            if (distinctUris.isEmpty()) {
                _state.value = PdfExtractorUiState.Idle
                return@launch
            }

            distinctUris.forEachIndexed { index, uri ->
                val displayName = getPdfDisplayName(uri)
                val password = passwordCache.get(uri.toString())
                repository.addBook(uri, password).collect { asyncState ->
                    when (asyncState) {
                        is AsyncState.Loading -> {
                            val batchProgress = (index + asyncState.progress) / distinctUris.size
                            _state.value = PdfExtractorUiState.Loading(
                                progress = batchProgress,
                                currentPage = asyncState.currentPage,
                                totalPages = asyncState.totalPages,
                                currentDocument = index + 1,
                                totalDocuments = distinctUris.size,
                            )
                        }
                        is AsyncState.Success -> {
                            password?.let { passwordCache.put(uri.toString(), it) }
                            _state.value = PdfExtractorUiState.Success(asyncState.data)
                            refreshLibrary()
                        }
                        is AsyncState.PasswordProtected -> {
                            _state.value = PdfExtractorUiState.Error(
                                "${displayName ?: "A book"} is password protected; open it separately to enter the password."
                            )
                        }
                        is AsyncState.Error -> {
                            _state.value = PdfExtractorUiState.Error(
                                "${displayName ?: uri.lastPathSegment}: ${asyncState.message}"
                            )
                        }
                        is AsyncState.Idle -> Unit
                    }
                }
            }
            refreshLibrary()
            _state.value = PdfExtractorUiState.Idle
        }
    }

    private fun startExtraction(
        uri: Uri,
        displayName: String?,
        password: String?,
    ) {
        extractionJob?.cancel()
        _state.value = PdfExtractorUiState.Loading(progress = 0f)
        extractionJob = viewModelScope.launch {
            cacheReady.await()
            repository.addBook(uri, password).collect { asyncState ->
                when (asyncState) {
                    is AsyncState.Loading -> {
                        _state.value = PdfExtractorUiState.Loading(
                            progress = asyncState.progress,
                            currentPage = asyncState.currentPage,
                            totalPages = asyncState.totalPages,
                        )
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
        // Providers can reject metadata queries even when the file itself is
        // readable. Reuse the guarded resolver so picking such a URI cannot
        // crash the activity before extraction reports its actual error.
        return resolver.getDisplayName(uri)
    }

    private fun persistReadPermission(uri: Uri) {
        if (uri.scheme != "content") return
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
