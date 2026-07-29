package com.example.readproplus.pdf

import com.tom_roush.pdfbox.pdmodel.common.PDMetadata
import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

data class PdfMetadata(
    val title: String?,
    val author: String?,
    val subject: String?,
    val keywords: String?,
)

object PdfMetadataReader {
    fun read(doc: PDDocument, fallbackTitle: String? = null): PdfMetadata {
        val info = doc.documentInformation
        val xmp = readXmp(doc)
        return PdfMetadata(
            title = firstNonBlank(info.title, xmp?.title, fallbackTitle),
            author = firstNonBlank(info.author, xmp?.author),
            subject = firstNonBlank(info.subject, xmp?.subject),
            keywords = firstNonBlank(info.keywords, xmp?.keywords),
        )
    }

    private fun readXmp(doc: PDDocument): XmpMetadata? = runCatching {
        val metadata = doc.documentCatalog.metadata ?: return@runCatching null
        parseXmp(metadata)
    }.getOrNull()

    private fun parseXmp(metadata: PDMetadata): XmpMetadata {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        val document = metadata.exportXMPMetadata().use { input ->
            factory.newDocumentBuilder().parse(input)
        }
        return XmpMetadata(
            title = firstXmpValue(document, "title"),
            author = firstNonBlank(
                firstXmpValue(document, "Author"),
                firstXmpValue(document, "creator"),
            ),
            subject = firstNonBlank(
                firstXmpValue(document, "Subject"),
                firstXmpValue(document, "description"),
                firstXmpValue(document, "subject"),
            ),
            keywords = firstXmpValue(document, "Keywords"),
        )
    }

    private fun firstXmpValue(document: org.w3c.dom.Document, localName: String): String? {
        val nodes = document.getElementsByTagNameNS("*", localName)
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node !is Element) continue

            val listItems = node.getElementsByTagNameNS("*", "li")
            val value = if (listItems.length > 0) {
                (0 until listItems.length)
                    .mapNotNull { listItems.item(it)?.textContent?.cleanMetadataValue() }
                    .joinToString(", ")
            } else {
                node.textContent.cleanMetadataValue()
            }
            if (value.isNotBlank()) return value
        }
        return null
    }

    private fun firstNonBlank(vararg values: String?): String? = values
        .asSequence()
        .mapNotNull { it?.cleanMetadataValue() }
        .firstOrNull()

    private fun String.cleanMetadataValue(): String = replace('\u0000', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    private data class XmpMetadata(
        val title: String?,
        val author: String?,
        val subject: String?,
        val keywords: String?,
    )
}
