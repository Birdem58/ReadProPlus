package com.example.readproplus.data

import android.content.Context
import android.content.SharedPreferences
import com.example.readproplus.model.Highlight
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class HighlightRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<Highlight> = decode(prefs.getString(KEY_HIGHLIGHTS, "[]") ?: "[]")

    fun getByBook(bookId: String): List<Highlight> = getAll().filter { it.bookId == bookId }

    fun getByBookAndPage(bookId: String, pageNumber: Int): List<Highlight> =
        getAll().filter { it.bookId == bookId && it.pageNumber == pageNumber }

    fun add(highlight: Highlight) {
        val list = getAll().toMutableList()
        list.add(0, highlight)
        save(list)
    }

    fun remove(id: String) {
        val list = getAll().filter { it.id != id }
        save(list)
    }

    fun toggle(
        bookId: String,
        bookTitle: String,
        pageNumber: Int,
        text: String,
        color: Long,
    ): Highlight? {
        val existing = getAll().firstOrNull {
            it.bookId == bookId && it.pageNumber == pageNumber && it.text == text
        }
        if (existing != null) {
            remove(existing.id)
            return null
        }
        val highlight = Highlight(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            bookTitle = bookTitle,
            pageNumber = pageNumber,
            text = text,
            color = color,
            createdAt = System.currentTimeMillis(),
        )
        add(highlight)
        return highlight
    }

    fun clear() {
        save(emptyList())
    }

    private fun save(list: List<Highlight>) {
        prefs.edit().putString(KEY_HIGHLIGHTS, encode(list)).apply()
    }

    private fun encode(list: List<Highlight>): String {
        val arr = JSONArray()
        for (h in list) {
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("bookId", h.bookId)
                put("bookTitle", h.bookTitle)
                put("pageNumber", h.pageNumber)
                put("text", h.text)
                put("color", h.color)
                put("createdAt", h.createdAt)
            })
        }
        return arr.toString()
    }

    private fun decode(json: String): List<Highlight> {
        val arr = JSONArray(json)
        val list = mutableListOf<Highlight>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Highlight(
                    id = obj.getString("id"),
                    bookId = obj.getString("bookId"),
                    bookTitle = obj.getString("bookTitle"),
                    pageNumber = obj.getInt("pageNumber"),
                    text = obj.getString("text"),
                    color = obj.getLong("color"),
                    createdAt = obj.getLong("createdAt"),
                )
            )
        }
        return list
    }

    companion object {
        private const val PREFS_NAME = "readproplus_highlights"
        private const val KEY_HIGHLIGHTS = "highlights"
    }
}
