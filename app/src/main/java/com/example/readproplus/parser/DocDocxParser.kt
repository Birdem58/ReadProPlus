package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import org.w3c.dom.Element

class DocDocxParser : DocumentParser {

    override fun canHandle(extension: String): Boolean =
        extension.equals("docx", ignoreCase = true) || extension.equals("doc", ignoreCase = true)

    override fun parse(context: Context, uri: Uri): PdfDocument {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        val extension = UniversalDocumentExtractor.getExtension(context, uri)
        val sourceUri = uri.toString()
        return if (extension == "docx") {
            val entries = readZipEntries(input)
            val documentXml = entries.entries.firstOrNull { it.key.equals("word/document.xml", ignoreCase = true) }?.value
                ?: throw IllegalArgumentException("DOCX word/document.xml is missing")
            parseDocx(documentXml, uri.lastPathSegment?.substringBeforeLast('.') ?: "Word Document", sourceUri)
        } else {
            parseLegacyDoc(input.use { it.readBytes() }, uri.lastPathSegment?.substringBeforeLast('.') ?: "Word Document", sourceUri)
        }
    }

    companion object {
        internal fun parseDocx(bytes: ByteArray, title: String, sourceUri: String): PdfDocument {
            val document = parseXml(bytes)
            val body = firstDescendant(document, "body") ?: document
            val paragraphs = elementChildren(body)
                .filter { localName(it) == "p" }
                .map(::wordParagraphText)
                .filter { it.isNotBlank() }
            val pages = paragraphs.chunked(12).map { it.joinToString("\n\n") }
            return makeDocument(
                pages.ifEmpty { listOf("No readable text found in Word document.") },
                title,
                sourceUri,
                FormatType.DOCX,
            )
        }

        internal fun parseLegacyDoc(bytes: ByteArray, title: String, sourceUri: String): PdfDocument {
            // .doc is an OLE compound binary file. Without a full OLE parser, only
            // contiguous printable runs are accepted; arbitrary binary bytes are never
            // presented as document text.
            val asciiText = buildString {
                var run = StringBuilder()
                fun flush() {
                    if (run.length >= 4) {
                        if (isNotEmpty()) append('\n')
                        append(run)
                    }
                    run = StringBuilder()
                }
                bytes.forEach { byte ->
                    val value = byte.toInt() and 0xff
                    if (value in 0x20..0x7e || value == '\n'.code || value == '\r'.code || value == '\t'.code) {
                        run.append(value.toChar())
                    } else {
                        flush()
                    }
                }
                flush()
            }.lines().map { it.trim() }.filter { it.length >= 4 }.joinToString("\n")
            val unicodeText = extractUtf16Runs(bytes)
            val text = listOf(asciiText, unicodeText)
                .filter { it.isNotBlank() }
                .maxByOrNull { it.length }
                .orEmpty()
            val pages = if (text.isBlank()) listOf("Legacy .doc text could not be decoded on this device.")
            else text.chunked(1500)
            return makeDocument(pages, title, sourceUri, FormatType.DOC)
        }

        private fun extractUtf16Runs(bytes: ByteArray): String {
            val lines = mutableListOf<String>()
            val run = StringBuilder()
            fun flush() {
                if (run.length >= 4) lines += run.toString().trim()
                run.clear()
            }
            var index = 0
            while (index + 1 < bytes.size) {
                val code = (bytes[index].toInt() and 0xff) or ((bytes[index + 1].toInt() and 0xff) shl 8)
                val character = code.toChar()
                if (character == '\n' || character == '\r' || character == '\t' || code in 0x20..0x7E || code >= 0xA0) {
                    run.append(character)
                } else {
                    flush()
                }
                index += 2
            }
            flush()
            return lines.filter { it.length >= 4 }.joinToString("\n")
        }

        private fun wordParagraphText(element: Element): String = buildString {
            fun visit(node: org.w3c.dom.Node) {
                val children = node.childNodes
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    if (child is Element) {
                        when (localName(child)) {
                            "t" -> append(child.textContent)
                            "tab" -> append('\t')
                            "br", "cr" -> append('\n')
                            else -> visit(child)
                        }
                    }
                }
            }
            visit(element)
        }.replace(Regex("[ \\t]+"), " ").replace(Regex(" *\\n *"), "\n").trim()

        private fun makeDocument(
            pages: List<String>,
            title: String,
            sourceUri: String,
            format: FormatType,
        ) = PdfDocument(
            id = sourceUri,
            title = title,
            author = "Word Document",
            totalPages = pages.size,
            pages = pages,
            toc = pages.mapIndexed { index, _ -> TocEntry("Section ${index + 1}", index + 1) },
            format = format,
            sourceUri = sourceUri,
        )
    }
}
