package com.example.readproplus.data

import android.content.Context
import android.content.SharedPreferences
import com.example.readproplus.model.BookCollection
import com.example.readproplus.model.BookFolder
import com.example.readproplus.model.ReaderProgress
import com.example.readproplus.model.Series
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SidebarRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getReadingProgress(): List<ReaderProgress> =
        decodeProgressList(prefs.getString(KEY_READING_PROGRESS, "[]") ?: "[]")

    fun saveReadingProgress(bookId: String, bookTitle: String, currentPage: Int, totalPages: Int) {
        val list = getReadingProgress().toMutableList()
        val existing = list.indexOfFirst { it.bookId == bookId }
        val entry = ReaderProgress(bookId, bookTitle, currentPage, totalPages, System.currentTimeMillis())
        if (existing >= 0) list[existing] = entry else list.add(entry)
        saveProgressList(list)
    }

    fun removeReadingProgress(bookId: String) {
        val list = getReadingProgress().filter { it.bookId != bookId }
        saveProgressList(list)
    }

    fun getFavorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    fun toggleFavorite(bookId: String) {
        val set = getFavorites().toMutableSet()
        if (set.contains(bookId)) set.remove(bookId) else set.add(bookId)
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply()
    }

    fun isFavorite(bookId: String): Boolean = getFavorites().contains(bookId)

    fun getToRead(): Set<String> =
        prefs.getStringSet(KEY_TO_READ, emptySet()) ?: emptySet()

    fun toggleToRead(bookId: String) {
        val set = getToRead().toMutableSet()
        if (set.contains(bookId)) {
            set.remove(bookId)
            prefs.edit().putStringSet(KEY_TO_READ, set).apply()
        } else {
            set.add(bookId)
            prefs.edit()
                .putStringSet(KEY_TO_READ, set)
                .putStringSet(KEY_HAVE_READ, getHaveRead().toMutableSet().apply { remove(bookId) })
                .apply()
        }
    }

    fun getHaveRead(): Set<String> =
        prefs.getStringSet(KEY_HAVE_READ, emptySet()) ?: emptySet()

    fun markAsRead(bookId: String) {
        val set = getHaveRead().toMutableSet()
        set.add(bookId)
        prefs.edit().putStringSet(KEY_HAVE_READ, set).apply()
        val toRead = getToRead().toMutableSet()
        toRead.remove(bookId)
        prefs.edit().putStringSet(KEY_TO_READ, toRead).apply()
    }

    fun toggleHaveRead(bookId: String) {
        if (bookId in getHaveRead()) {
            prefs.edit()
                .putStringSet(KEY_HAVE_READ, getHaveRead().toMutableSet().apply { remove(bookId) })
                .apply()
        } else {
            markAsRead(bookId)
        }
    }

    fun getCollections(): List<BookCollection> =
        decodeCollectionList(prefs.getString(KEY_COLLECTIONS, "[]") ?: "[]")

    fun saveCollection(collection: BookCollection) {
        val list = getCollections().toMutableList()
        val existing = list.indexOfFirst { it.id == collection.id }
        if (existing >= 0) list[existing] = collection else list.add(collection)
        saveCollectionList(list)
    }

    fun deleteCollection(id: String) {
        val list = getCollections().filter { it.id != id }
        saveCollectionList(list)
    }

    fun addBookToCollection(collectionId: String, bookId: String) {
        val list = getCollections().toMutableList()
        val idx = list.indexOfFirst { it.id == collectionId }
        if (idx >= 0) {
            val c = list[idx]
            if (bookId !in c.bookIds) {
                list[idx] = c.copy(bookIds = c.bookIds + bookId)
                saveCollectionList(list)
            }
        }
    }

    fun getFolders(): List<BookFolder> =
        decodeFolderList(prefs.getString(KEY_FOLDERS, "[]") ?: "[]")

    fun saveFolder(folder: BookFolder) {
        val list = getFolders().toMutableList()
        val existing = list.indexOfFirst { it.id == folder.id }
        if (existing >= 0) list[existing] = folder else list.add(folder)
        saveFolderList(list)
    }

    fun deleteFolder(id: String) {
        val list = getFolders().filter { it.id != id }
        saveFolderList(list)
    }

    fun getTrash(): Set<String> =
        prefs.getStringSet(KEY_TRASH, emptySet()) ?: emptySet()

    fun trashBook(bookId: String) {
        val set = getTrash().toMutableSet()
        set.add(bookId)
        prefs.edit().putStringSet(KEY_TRASH, set).apply()
    }

    fun restoreBook(bookId: String) {
        val set = getTrash().toMutableSet()
        set.remove(bookId)
        prefs.edit().putStringSet(KEY_TRASH, set).apply()
    }

    fun emptyTrash() {
        prefs.edit().putStringSet(KEY_TRASH, emptySet()).apply()
    }

    private fun saveProgressList(list: List<ReaderProgress>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(JSONObject().apply {
                put("bookId", p.bookId)
                put("bookTitle", p.bookTitle)
                put("currentPage", p.currentPage)
                put("totalPages", p.totalPages)
                put("lastReadAt", p.lastReadAt)
            })
        }
        prefs.edit().putString(KEY_READING_PROGRESS, arr.toString()).apply()
    }

    private fun decodeProgressList(json: String): List<ReaderProgress> {
        val arr = JSONArray(json)
        val list = mutableListOf<ReaderProgress>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(ReaderProgress(
                bookId = obj.getString("bookId"),
                bookTitle = obj.getString("bookTitle"),
                currentPage = obj.getInt("currentPage"),
                totalPages = obj.getInt("totalPages"),
                lastReadAt = obj.getLong("lastReadAt"),
            ))
        }
        return list.sortedByDescending { it.lastReadAt }
    }

    private fun saveCollectionList(list: List<BookCollection>) {
        val arr = JSONArray()
        for (c in list) {
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("bookIds", JSONArray(c.bookIds))
            })
        }
        prefs.edit().putString(KEY_COLLECTIONS, arr.toString()).apply()
    }

    private fun decodeCollectionList(json: String): List<BookCollection> {
        val arr = JSONArray(json)
        val list = mutableListOf<BookCollection>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val ids = mutableListOf<String>()
            val idsArr = obj.getJSONArray("bookIds")
            for (j in 0 until idsArr.length()) ids.add(idsArr.getString(j))
            list.add(BookCollection(
                id = obj.getString("id"),
                name = obj.getString("name"),
                bookIds = ids,
            ))
        }
        return list
    }

    private fun saveFolderList(list: List<BookFolder>) {
        val arr = JSONArray()
        for (f in list) {
            arr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("bookIds", JSONArray(f.bookIds))
            })
        }
        prefs.edit().putString(KEY_FOLDERS, arr.toString()).apply()
    }

    private fun decodeFolderList(json: String): List<BookFolder> {
        val arr = JSONArray(json)
        val list = mutableListOf<BookFolder>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val ids = mutableListOf<String>()
            val idsArr = obj.getJSONArray("bookIds")
            for (j in 0 until idsArr.length()) ids.add(idsArr.getString(j))
            list.add(BookFolder(
                id = obj.getString("id"),
                name = obj.getString("name"),
                bookIds = ids,
            ))
        }
        return list
    }

    companion object {
        private const val PREFS_NAME = "readproplus_sidebar"
        private const val KEY_READING_PROGRESS = "reading_progress"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_TO_READ = "to_read"
        private const val KEY_HAVE_READ = "have_read"
        private const val KEY_COLLECTIONS = "collections"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_TRASH = "trash"
    }
}
