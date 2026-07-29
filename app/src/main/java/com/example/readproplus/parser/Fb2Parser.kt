package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import org.w3c.dom.Element

class Fb2Parser : DocumentParser {

    override fun canHandle(extension: String): Boolean = extension.equals("fb2", ignoreCase = true)

    override fun parse(context: Context, uri: Uri): PdfDocument {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        val xml = stream.use { it.readBytes() }
        return parseXmlDocument(
            xml,
            uri.lastPathSegment?.substringBeforeLast('.') ?: "FB2 Document",
            uri.toString(),
        )
    }

    companion object {
        internal fun parseXmlDocument(bytes: ByteArray, fallbackTitle: String, sourceUri: String): PdfDocument {
            val document = parseXml(bytes)
            val description = firstDescendant(document, "description")
            val title = firstDescendant(description ?: document, "book-title")?.let(::elementText)
                ?.takeIf { it.isNotBlank() } ?: fallbackTitle
            val authorElement = firstDescendant(description ?: document, "author")
            val author = listOf("first-name", "middle-name", "last-name")
                .mapNotNull { name -> firstDescendant(authorElement ?: document, name)?.let(::elementText) }
                .joinToString(" ")
                .ifBlank { "Unknown Author" }

            val body = firstDescendant(document, "body")
            val sections = elementChildren(body ?: document).filter { localName(it) == "section" }
            val pages = mutableListOf<String>()
            val toc = mutableListOf<TocEntry>()
            if (sections.isNotEmpty()) {
                sections.forEach { section ->
                    appendSection(section, 0, pages, toc)
                }
            } else {
                val paragraphs = descendantElements(body ?: document, "p")
                    .map(::xmlText)
                    .filter { it.isNotBlank() }
                paragraphs.chunked(15).forEachIndexed { index, chunk ->
                    pages += chunk.joinToString("\n\n")
                    toc += TocEntry("Page ${index + 1}", index + 1)
                }
            }

            val finalPages = pages.ifEmpty { listOf("No text content found in FB2 file.") }
            return PdfDocument(
                id = sourceUri,
                title = title,
                author = author,
                totalPages = finalPages.size,
                pages = finalPages,
                toc = toc,
                format = FormatType.FB2,
                sourceUri = sourceUri,
            )
        }

        private fun appendSection(
            section: Element,
            depth: Int,
            pages: MutableList<String>,
            toc: MutableList<TocEntry>,
        ) {
            val title = firstDescendant(section, "title")?.let(::xmlText)
                ?.takeIf { it.isNotBlank() } ?: "Section ${pages.size + 1}"
            val paragraphs = descendantElements(section, "p")
                .map(::xmlText)
                .filter { it.isNotBlank() }
            if (paragraphs.isNotEmpty()) {
                pages += "$title\n\n${paragraphs.joinToString("\n\n")}"
                toc += TocEntry(title, pages.size, depth)
            }
            elementChildren(section)
                .filter { localName(it) == "section" }
                .forEach { appendSection(it, depth + 1, pages, toc) }
        }
    }
}
