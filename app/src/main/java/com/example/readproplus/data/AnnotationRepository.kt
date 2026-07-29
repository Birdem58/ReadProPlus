package com.example.readproplus.data

import android.content.Context
import android.content.SharedPreferences
import com.example.readproplus.model.BookNote
import com.example.readproplus.model.Bookmark
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AnnotationRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Bookmarks
    fun getBookmarks(): List<Bookmark> = decodeBookmarks(prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]")

    fun getBookmarksForBook(bookId: String): List<Bookmark> = getBookmarks().filter { it.bookId == bookId }

    fun isBookmarked(bookId: String, pageNumber: Int): Boolean =
        getBookmarks().any { it.bookId == bookId && it.pageNumber == pageNumber }

    fun toggleBookmark(bookId: String, bookTitle: String, pageNumber: Int): Boolean {
        val list = getBookmarks().toMutableList()
        val existing = list.firstOrNull { it.bookId == bookId && it.pageNumber == pageNumber }
        return if (existing != null) {
            list.remove(existing)
            saveBookmarks(list)
            false
        } else {
            val bookmark = Bookmark(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                bookTitle = bookTitle,
                pageNumber = pageNumber,
                createdAt = System.currentTimeMillis(),
            )
            list.add(0, bookmark)
            saveBookmarks(list)
            true
        }
    }

    // Written Notes
    fun getNotes(): List<BookNote> = decodeNotes(prefs.getString(KEY_NOTES, "[]") ?: "[]")

    fun getNotesForBook(bookId: String): List<BookNote> = getNotes().filter { it.bookId == bookId }

    fun addNote(bookId: String, bookTitle: String, pageNumber: Int, noteText: String, snippet: String? = null): BookNote {
        val list = getNotes().toMutableList()
        val note = BookNote(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            bookTitle = bookTitle,
            pageNumber = pageNumber,
            noteText = noteText,
            textSnippet = snippet,
            createdAt = System.currentTimeMillis(),
        )
        list.add(0, note)
        saveNotes(list)
        return note
    }

    fun removeNote(id: String) {
        val list = getNotes().filter { it.id != id }
        saveNotes(list)
    }

    private fun saveBookmarks(list: List<Bookmark>) {
        val arr = JSONArray()
        for (b in list) {
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("bookId", b.bookId)
                put("bookTitle", b.bookTitle)
                put("pageNumber", b.pageNumber)
                put("createdAt", b.createdAt)
            })
        }
        prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    private fun decodeBookmarks(json: String): List<Bookmark> {
        val arr = JSONArray(json)
        val list = mutableListOf<Bookmark>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Bookmark(
                    id = obj.getString("id"),
                    bookId = obj.getString("bookId"),
                    bookTitle = obj.getString("bookTitle"),
                    pageNumber = obj.getInt("pageNumber"),
                    createdAt = obj.getLong("createdAt"),
                )
            )
        }
        return list
    }

    private fun saveNotes(list: List<BookNote>) {
        val arr = JSONArray()
        for (n in list) {
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("bookId", n.bookId)
                put("bookTitle", n.bookTitle)
                put("pageNumber", n.pageNumber)
                put("noteText", n.noteText)
                put("textSnippet", n.textSnippet ?: "")
                put("createdAt", n.createdAt)
            })
        }
        prefs.edit().putString(KEY_NOTES, arr.toString()).apply()
    }

    private fun decodeNotes(json: String): List<BookNote> {
        val arr = JSONArray(json)
        val list = mutableListOf<BookNote>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                BookNote(
                    id = obj.getString("id"),
                    bookId = obj.getString("bookId"),
                    bookTitle = obj.getString("bookTitle"),
                    pageNumber = obj.getInt("pageNumber"),
                    noteText = obj.getString("noteText"),
                    textSnippet = obj.optString("textSnippet").ifBlank { null },
                    createdAt = obj.getLong("createdAt"),
                )
            )
        }
        return list
    }

    companion object {
        private const val PREFS_NAME = "readproplus_annotations"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_NOTES = "notes"
    }
}
