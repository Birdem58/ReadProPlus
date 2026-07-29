package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import org.w3c.dom.Element

class OdtParser : DocumentParser {

    override fun canHandle(extension: String): Boolean = extension.equals("odt", ignoreCase = true)

    override fun parse(context: Context, uri: Uri): PdfDocument {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        val entries = readZipEntries(input)
        val content = entries.entries.firstOrNull { it.key.equals("content.xml", ignoreCase = true) }?.value
            ?: throw IllegalArgumentException("ODT content.xml is missing")
        val title = entries.entries.firstOrNull { it.key.equals("meta.xml", ignoreCase = true) }
            ?.value?.let { bytes ->
                runCatching { firstDescendant(parseXml(bytes), "title")?.let(::elementText) }.getOrNull()
            }?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringBeforeLast('.') ?: "OpenDocument Text"
        return parseContent(content, title, uri.toString())
    }

    companion object {
        internal fun parseContent(bytes: ByteArray, title: String, sourceUri: String): PdfDocument {
            val document = parseXml(bytes)
            val textRoot = firstDescendant(document, "text") ?: document
            val paragraphs = descendantElements(textRoot)
                .filter { localName(it) == "p" || localName(it) == "h" }
                .map(::odtText)
                .filter { it.isNotBlank() }
            val pages = paragraphs.chunked(12).map { it.joinToString("\n\n") }
            val finalPages = pages.ifEmpty { listOf("No text content found in ODT document.") }
            val toc = finalPages.mapIndexed { index, page ->
                TocEntry(page.lineSequence().firstOrNull().orEmpty().take(80).ifBlank { "Section ${index + 1}" }, index + 1)
            }
            return PdfDocument(
                id = sourceUri,
                title = title,
                author = "OpenDocument",
                totalPages = finalPages.size,
                pages = finalPages,
                toc = toc,
                format = FormatType.ODT,
                sourceUri = sourceUri,
            )
        }

        private fun odtText(element: Element): String = buildString {
            fun visit(node: org.w3c.dom.Node) {
                val children = node.childNodes
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    if (child.nodeType == org.w3c.dom.Node.TEXT_NODE || child.nodeType == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                        append(child.nodeValue)
                    } else if (child is Element) {
                        when (localName(child)) {
                            "s" -> repeat(child.getAttribute("c").toIntOrNull()?.coerceAtLeast(1) ?: 1) { append(' ') }
                            "tab" -> append('\t')
                            "line-break" -> append('\n')
                            else -> visit(child)
                        }
                    }
                }
            }
            visit(element)
        }.replace(Regex("[ \\t]+"), " ").replace(Regex(" *\\n *"), "\n").trim()
    }
}
