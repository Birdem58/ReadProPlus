package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry

class EpubParser : DocumentParser {

    override fun canHandle(extension: String): Boolean = extension.equals("epub", ignoreCase = true)

    override fun parse(context: Context, uri: Uri): PdfDocument {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        return parseArchive(
            entries = readZipEntries(input),
            fallbackTitle = uri.lastPathSegment?.substringBeforeLast('.') ?: "EPUB Document",
            sourceUri = uri.toString(),
        )
    }

    companion object {
        private data class ManifestItem(
            val id: String,
            val path: String,
            val mediaType: String,
            val properties: String,
        )

        internal fun parseArchive(
            entries: Map<String, ByteArray>,
            fallbackTitle: String,
            sourceUri: String,
        ): PdfDocument {
            require(entries.isNotEmpty()) { "EPUB archive is empty" }
            val lookup = entries.entries.associateBy { it.key.lowercase() }
            val container = lookup["meta-inf/container.xml"]?.value
            val opfPath = container?.let { bytes ->
                val document = parseXml(bytes)
                firstDescendant(document, "rootfile")?.getAttribute("full-path")
            }?.takeIf { it.isNotBlank() }
                ?: lookup.keys.firstOrNull { it.endsWith(".opf") }
                ?: throw IllegalArgumentException("EPUB package document is missing")

            val opfBytes = lookup[opfPath.lowercase()]?.value
                ?: throw IllegalArgumentException("EPUB package document not found: $opfPath")
            val opf = parseXml(opfBytes)
            val metadata = firstDescendant(opf, "metadata") ?: opf.documentElement
            val title = firstDescendant(metadata, "title")?.let(::elementText)
                ?.takeIf { it.isNotBlank() } ?: fallbackTitle
            val author = firstDescendant(metadata, "creator")?.let(::elementText)
                ?.takeIf { it.isNotBlank() } ?: "Unknown Author"

            val manifest = firstDescendant(opf, "manifest")
                ?.let { manifestElement ->
                    elementChildren(manifestElement)
                        .filter { localName(it) == "item" }
                        .mapNotNull { item ->
                            val id = item.getAttribute("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            ManifestItem(
                                id = id,
                                path = resolveArchivePath(opfPath, item.getAttribute("href")),
                                mediaType = item.getAttribute("media-type"),
                                properties = item.getAttribute("properties"),
                            )
                        }
                }.orEmpty()
            val byId = manifest.associateBy { it.id }
            val spine = firstDescendant(opf, "spine")
                ?.let { spineElement ->
                    elementChildren(spineElement)
                        .filter { localName(it) == "itemref" }
                        .mapNotNull { byId[it.getAttribute("idref")] }
                }.orEmpty()
            val chapterItems = (if (spine.isNotEmpty()) spine else manifest.filter(::isChapterItem))
                .distinctBy { it.path }

            val chapterPages = chapterItems.mapNotNull { item ->
                val html = lookup[item.path.lowercase()]?.value?.toString(Charsets.UTF_8)
                ?: return@mapNotNull null
                cleanHtml(html).takeIf { it.isNotBlank() }?.let { text -> item.path to text }
            }
            val pages = chapterPages.map { it.second }.ifEmpty { listOf("No readable text found in EPUB document.") }

            val pageByPath = chapterPages.mapIndexed { index, item -> item.first to (index + 1) }.toMap()
            val toc = parseToc(lookup, opfPath, manifest, pageByPath).ifEmpty {
                chapterPages.mapIndexed { index, item ->
                    TocEntry(item.first.substringAfterLast('/').substringBeforeLast('.'), index + 1)
                }
            }

            return PdfDocument(
                id = sourceUri,
                title = title,
                author = author,
                totalPages = pages.size,
                pages = pages,
                toc = toc,
                format = FormatType.EPUB,
                isImageBased = false,
                sourceUri = sourceUri,
            )
        }

        private fun isChapterItem(item: ManifestItem): Boolean =
            item.mediaType.contains("html", ignoreCase = true) ||
                item.mediaType.contains("xhtml", ignoreCase = true)

        private fun parseToc(
            lookup: Map<String, Map.Entry<String, ByteArray>>,
            opfPath: String,
            manifest: List<ManifestItem>,
            pageByPath: Map<String, Int>,
        ): List<TocEntry> {
            val result = mutableListOf<TocEntry>()
            val navItem = manifest.firstOrNull { it.properties.split(' ').any { property -> property == "nav" } }
            val navBytes = navItem?.let { lookup[it.path.lowercase()]?.value }
            if (navBytes != null) {
                val navDocument = parseXml(navBytes)
                descendantElements(navDocument, "a").forEach { anchor ->
                    val page = pageByPath[resolveArchivePath(navItem.path, anchor.getAttribute("href"))]
                    val label = elementText(anchor)
                    if (page != null && label.isNotBlank()) result += TocEntry(label, page)
                }
            }
            if (result.isNotEmpty()) return result.distinctBy { it.title to it.pageNumber }

            val ncxItem = manifest.firstOrNull { it.mediaType.contains("ncx", ignoreCase = true) }
            val ncxBytes = ncxItem?.let { lookup[it.path.lowercase()]?.value } ?: return emptyList()
            val ncx = parseXml(ncxBytes)
            descendantElements(ncx, "navpoint").forEach { navPoint ->
                val page = firstDescendant(navPoint, "content")?.getAttribute("src")
                    ?.let { pageByPath[resolveArchivePath(ncxItem.path, it)] }
                val label = firstDescendant(navPoint, "text")?.let(::elementText).orEmpty()
                if (page != null && label.isNotBlank()) result += TocEntry(label, page)
            }
            return result.distinctBy { it.title to it.pageNumber }
        }
    }
}
