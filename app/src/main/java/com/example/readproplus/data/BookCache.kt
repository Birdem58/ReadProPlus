package com.example.readproplus.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import com.example.readproplus.pdf.PdfTextNormalizer
import com.example.readproplus.pdf.PdfTitleResolver
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
        // A reload must reflect the persisted file exactly. Without clearing
        // first, documents removed from disk can remain visible in memory.
        cache.clear()
        val file = storageFile() ?: return
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val books = root.optJSONArray("books") ?: return@runCatching
            var migrated = false
            for (index in 0 until books.length()) {
                documentFromJson(books.optJSONObject(index) ?: continue)?.let { document ->
                    val restored = restoreMissingTitle(document)
                    migrated = migrated || restored != document
                    cache[restored.id] = restored
                }
            }
            trimIfNeeded()
            if (migrated) persist()
        }.onFailure {
            // A corrupt index must not prevent the user from opening the app.
            cache.clear()
        }
    }

    private fun restoreMissingTitle(document: PdfDocument): PdfDocument {
        if (PdfTitleResolver.usableTitle(document.title) != null) return document

        val sourceUri = document.sourceUri?.takeIf { it.isNotBlank() } ?: document.id
        val uri = runCatching { Uri.parse(sourceUri) }.getOrNull() ?: return document
        val displayName = context?.let { appContext ->
            runCatching {
                appContext.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
        }
        val title = PdfTitleResolver.fromDisplayName(displayName)
            ?: PdfTitleResolver.fromUri(uri)
            ?: return document

        return document.copy(
            title = title,
            sourceUri = document.sourceUri ?: sourceUri,
        )
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
        val format = runCatching { FormatType.valueOf(json.optString("format", FormatType.PDF.name)) }
            .getOrDefault(FormatType.PDF)
        val normalizedPages = if (format == FormatType.PDF) {
            pages.map(PdfTextNormalizer::normalizePage)
        } else {
            pages
        }
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
            pages = normalizedPages,
            fileSizeBytes = json.optLong("fileSizeBytes", 0L),
            toc = toc,
            format = format,
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
