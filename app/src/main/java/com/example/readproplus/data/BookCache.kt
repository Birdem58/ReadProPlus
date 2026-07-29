package com.example.readproplus.data

import android.content.Context
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.LinkedHashMap

/**
 * Small persistent library index. The extracted pages are kept here so a book
 * selected through SAF is still available after the process is recreated.
 */
class BookCache(
    private val context: Context? = null,
    private val maxEntries: Int = 1_000,
) {
    private val cache = LinkedHashMap<String, PdfDocument>(16, 0.75f, true)

    @Synchronized
    fun put(document: PdfDocument) {
        cache[document.id] = document
        trimIfNeeded()
        persist()
    }

    @Synchronized
    fun get(id: String): PdfDocument? = cache[id]

    @Synchronized
    fun remove(id: String): PdfDocument? {
        val removed = cache.remove(id)
        if (removed != null) persist()
        return removed
    }

    @Synchronized
    fun all(): List<PdfDocument> = ArrayList(cache.values)

    @Synchronized
    fun clear() {
        cache.clear()
        persist()
    }

    private fun trimIfNeeded() {
        while (cache.size > maxEntries) {
            cache.remove(cache.entries.first().key)
        }
    }

    private fun storageFile(): File? = context?.filesDir?.resolve(FILE_NAME)

    /**
     * Loads the persisted index. Callers should run this on a background
     * dispatcher because the index contains the extracted page text.
     */
    @Synchronized
    fun load() {
        val file = storageFile() ?: return
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val books = root.optJSONArray("books") ?: return@runCatching
            for (index in 0 until books.length()) {
                documentFromJson(books.optJSONObject(index) ?: continue)?.let { cache[it.id] = it }
            }
            trimIfNeeded()
        }.onFailure {
            // A corrupt index must not prevent the user from opening the app.
            cache.clear()
        }
    }

    private fun persist() {
        val file = storageFile() ?: return
        runCatching {
            val root = JSONObject().put("books", JSONArray().apply {
                cache.values.forEach { put(documentToJson(it)) }
            })
            file.writeText(root.toString(), Charsets.UTF_8)
        }
    }

    private fun documentToJson(document: PdfDocument): JSONObject = JSONObject().apply {
        put("id", document.id)
        put("title", document.title)
        put("author", document.author)
        put("subject", document.subject)
        put("keywords", document.keywords)
        put("totalPages", document.totalPages)
        put("pages", JSONArray(document.pages))
        put("fileSizeBytes", document.fileSizeBytes)
        put("format", document.format.name)
        put("isImageBased", document.isImageBased)
        put("filePath", document.filePath)
        put("sourceUri", document.sourceUri)
        put("pageImageEntries", JSONArray(document.pageImageEntries))
        put("toc", JSONArray().apply {
            document.toc.forEach {
                put(JSONObject().apply {
                    put("title", it.title)
                    put("pageNumber", it.pageNumber)
                    put("depth", it.depth)
                })
            }
        })
    }

    private fun documentFromJson(json: JSONObject): PdfDocument? = runCatching {
        val pagesJson = json.optJSONArray("pages") ?: JSONArray()
        val pages = List(pagesJson.length()) { pagesJson.optString(it) }
        val entriesJson = json.optJSONArray("pageImageEntries") ?: JSONArray()
        val imageEntries = List(entriesJson.length()) { entriesJson.optString(it) }
        val tocJson = json.optJSONArray("toc") ?: JSONArray()
        val toc = List(tocJson.length()) {
            val item = tocJson.optJSONObject(it) ?: JSONObject()
            TocEntry(
                title = item.optString("title"),
                pageNumber = item.optInt("pageNumber", 1),
                depth = item.optInt("depth", 0),
            )
        }
        PdfDocument(
            id = json.getString("id"),
            title = json.optString("title", "Untitled"),
            author = json.optString("author").takeIf { it.isNotBlank() },
            subject = json.optString("subject").takeIf { it.isNotBlank() },
            keywords = json.optString("keywords").takeIf { it.isNotBlank() },
            totalPages = json.optInt("totalPages", pages.size),
            pages = pages,
            fileSizeBytes = json.optLong("fileSizeBytes", 0L),
            toc = toc,
            format = runCatching { FormatType.valueOf(json.optString("format", FormatType.PDF.name)) }
                .getOrDefault(FormatType.PDF),
            isImageBased = json.optBoolean("isImageBased", false),
            filePath = json.optString("filePath").takeIf { it.isNotBlank() },
            sourceUri = json.optString("sourceUri").takeIf { it.isNotBlank() },
            pageImageEntries = imageEntries,
        )
    }.getOrNull()

    companion object {
        private const val FILE_NAME = "readproplus_library.json"
    }
}
