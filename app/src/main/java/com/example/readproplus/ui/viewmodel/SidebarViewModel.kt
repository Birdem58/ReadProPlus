package com.example.readproplus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.readproplus.data.SidebarRepository
import com.example.readproplus.model.BookCollection
import com.example.readproplus.model.BookFolder
import com.example.readproplus.model.ReaderProgress
import com.example.readproplus.model.pdf.PdfDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SidebarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SidebarRepository(application)

    private val _readingProgress = MutableStateFlow<List<ReaderProgress>>(emptyList())
    val readingProgress: StateFlow<List<ReaderProgress>> = _readingProgress.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _toRead = MutableStateFlow<Set<String>>(emptySet())
    val toRead: StateFlow<Set<String>> = _toRead.asStateFlow()

    private val _haveRead = MutableStateFlow<Set<String>>(emptySet())
    val haveRead: StateFlow<Set<String>> = _haveRead.asStateFlow()

    private val _collections = MutableStateFlow<List<BookCollection>>(emptyList())
    val collections: StateFlow<List<BookCollection>> = _collections.asStateFlow()

    private val _folders = MutableStateFlow<List<BookFolder>>(emptyList())
    val folders: StateFlow<List<BookFolder>> = _folders.asStateFlow()

    private val _trash = MutableStateFlow<Set<String>>(emptySet())
    val trash: StateFlow<Set<String>> = _trash.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _readingProgress.value = repository.getReadingProgress()
            _favorites.value = repository.getFavorites()
            _toRead.value = repository.getToRead()
            _haveRead.value = repository.getHaveRead()
            _collections.value = repository.getCollections()
            _folders.value = repository.getFolders()
            _trash.value = repository.getTrash()
        }
    }

    fun saveProgress(bookId: String, bookTitle: String, currentPage: Int, totalPages: Int) {
        repository.saveReadingProgress(bookId, bookTitle, currentPage, totalPages)
        refresh()
    }

    fun removeProgress(bookId: String) {
        repository.removeReadingProgress(bookId)
        refresh()
    }

    fun toggleFavorite(bookId: String) {
        repository.toggleFavorite(bookId)
        refresh()
    }

    fun isFavorite(bookId: String): Boolean = repository.isFavorite(bookId)

    fun toggleToRead(bookId: String) {
        repository.toggleToRead(bookId)
        refresh()
    }

    fun markAsRead(bookId: String) {
        repository.markAsRead(bookId)
        refresh()
    }

    fun toggleHaveRead(bookId: String) {
        repository.toggleHaveRead(bookId)
        refresh()
    }

    fun createCollection(name: String) {
        val collection = BookCollection(
            id = UUID.randomUUID().toString(),
            name = name,
        )
        repository.saveCollection(collection)
        refresh()
    }

    fun deleteCollection(id: String) {
        repository.deleteCollection(id)
        refresh()
    }

    fun addBookToCollection(collectionId: String, bookId: String) {
        repository.addBookToCollection(collectionId, bookId)
        refresh()
    }

    fun createFolder(name: String) {
        val folder = BookFolder(
            id = UUID.randomUUID().toString(),
            name = name,
        )
        repository.saveFolder(folder)
        refresh()
    }

    fun deleteFolder(id: String) {
        repository.deleteFolder(id)
        refresh()
    }

    fun trashBook(bookId: String) {
        repository.trashBook(bookId)
        refresh()
    }

    fun restoreBook(bookId: String) {
        repository.restoreBook(bookId)
        refresh()
    }

    fun emptyTrash() {
        repository.emptyTrash()
        refresh()
    }

    fun getAuthors(books: List<PdfDocument>): Map<String, List<PdfDocument>> {
        return books
            .filter { it.id !in _trash.value }
            .filter { it.author != null }
            .groupBy { it.author!! }
            .toSortedMap()
    }

    fun getSeries(books: List<PdfDocument>): Map<String, List<PdfDocument>> {
        return books
            .filter { it.id !in _trash.value }
            .groupBy { it.title.firstOrNull()?.toString()?.let { "Series $it" } ?: "Other" }
    }

    fun getFavoritedBooks(books: List<PdfDocument>): List<PdfDocument> {
        return books.filter { it.id in _favorites.value }
    }

    fun getToReadBooks(books: List<PdfDocument>): List<PdfDocument> {
        return books.filter { it.id in _toRead.value }
    }

    fun getHaveReadBooks(books: List<PdfDocument>): List<PdfDocument> {
        return books.filter { it.id in _haveRead.value }
    }

    fun getTrashBooks(books: List<PdfDocument>): List<PdfDocument> {
        return books.filter { it.id in _trash.value }
    }

    fun getBooksNotInTrash(books: List<PdfDocument>): List<PdfDocument> {
        return books.filter { it.id !in _trash.value }
    }
}
