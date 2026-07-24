package com.example.readproplus.data

import com.example.readproplus.model.pdf.PdfDocument
import java.util.LinkedHashMap

class BookCache(
    private val maxEntries: Int = 5,
) {
    private val cache = object : LinkedHashMap<String, PdfDocument>(
        /* initialCapacity */ maxEntries + 1,
        /* loadFactor */ 0.75f,
        /* accessOrder */ true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PdfDocument>?): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun put(document: PdfDocument) {
        cache[document.id] = document
    }

    @Synchronized
    fun get(id: String): PdfDocument? = cache[id]

    @Synchronized
    fun remove(id: String): PdfDocument? = cache.remove(id)

    @Synchronized
    fun all(): List<PdfDocument> = ArrayList(cache.values)

    @Synchronized
    fun clear() = cache.clear()
}
