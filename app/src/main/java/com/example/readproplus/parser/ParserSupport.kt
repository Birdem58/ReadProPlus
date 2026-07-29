package com.example.readproplus.parser

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Comparator
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

internal fun readZipEntries(input: InputStream): LinkedHashMap<String, ByteArray> {
    val result = LinkedHashMap<String, ByteArray>()
    ZipInputStream(input).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                result[entry.name] = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return result
}

internal fun parseXml(bytes: ByteArray): Document {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        try {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        } catch (_: Exception) {
            // Android's parser implementations expose slightly different feature sets.
        }
    }
    return factory.newDocumentBuilder().parse(InputSource(ByteArrayInputStream(bytes)))
}

internal fun localName(node: Node): String =
    (node.localName ?: node.nodeName.substringAfter(':')).lowercase()

internal fun elementChildren(node: Node): List<Element> = buildList {
    val children = node.childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child is Element) add(child)
    }
}

internal fun descendantElements(node: Node, name: String? = null): List<Element> {
    val result = mutableListOf<Element>()
    fun visit(current: Node) {
        val children = current.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element) {
                if (name == null || localName(child) == name.lowercase()) result += child
                visit(child)
            }
        }
    }
    visit(node)
    return result
}

internal fun firstDescendant(node: Node, name: String): Element? =
    descendantElements(node, name).firstOrNull()

internal fun elementText(element: Element?): String =
    element?.textContent?.replace(Regex("\\s+"), " ")?.trim().orEmpty()

internal fun xmlText(element: Element?): String = buildString {
    fun visit(node: Node) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            when {
                child.nodeType == Node.TEXT_NODE || child.nodeType == Node.CDATA_SECTION_NODE -> append(child.nodeValue)
                child is Element && localName(child) in setOf("br", "line-break") -> append('\n')
                child is Element -> visit(child)
            }
        }
    }
    if (element != null) visit(element)
}.replace(Regex("[ \\t]+"), " ")
    .replace(Regex(" *\\n *"), "\n")
    .trim()

internal fun decodeEntities(text: String): String = text
    .replace("&nbsp;", " ", ignoreCase = true)
    .replace("&amp;", "&", ignoreCase = true)
    .replace("&lt;", "<", ignoreCase = true)
    .replace("&gt;", ">", ignoreCase = true)
    .replace("&quot;", "\"", ignoreCase = true)
    .replace("&#39;", "'", ignoreCase = true)
    .replace(Regex("&#(x[0-9a-fA-F]+|[0-9]+);") ) { match ->
        val raw = match.groupValues[1]
        val codePoint = if (raw.startsWith("x", ignoreCase = true)) {
            raw.substring(1).toIntOrNull(16)
        } else {
            raw.toIntOrNull()
        }
        codePoint?.let { String(Character.toChars(it)) } ?: match.value
    }

internal fun cleanHtml(html: String): String {
    return html
        .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<(br|hr)\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(p|div|section|h[1-6]|li|tr)>\\s*", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), "")
        .let(::decodeEntities)
        .lines()
        .joinToString("\n") { it.trim() }
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

internal fun resolveArchivePath(basePath: String, href: String): String {
    val cleanHref = try {
        URLDecoder.decode(href.substringBefore('#'), StandardCharsets.UTF_8.name())
    } catch (_: Exception) {
        href.substringBefore('#')
    }
    val base = basePath.substringBeforeLast('/', "")
    val pieces = (if (base.isBlank()) cleanHref else "$base/$cleanHref")
        .replace('\\', '/')
        .split('/')
    val normalized = ArrayDeque<String>()
    for (piece in pieces) {
        when (piece) {
            "", "." -> Unit
            ".." -> if (normalized.isNotEmpty()) normalized.removeLast()
            else -> normalized.addLast(piece)
        }
    }
    return normalized.joinToString("/")
}

internal val naturalPathComparator: Comparator<String> = Comparator { left, right ->
    val leftParts = Regex("\\d+|\\D+").findAll(left.lowercase()).map { it.value }.toList()
    val rightParts = Regex("\\d+|\\D+").findAll(right.lowercase()).map { it.value }.toList()
    for (index in 0 until minOf(leftParts.size, rightParts.size)) {
        val a = leftParts[index]
        val b = rightParts[index]
        val comparison = if (a.first().isDigit() && b.first().isDigit()) {
            a.toLongOrNull().orEmpty().compareTo(b.toLongOrNull().orEmpty())
        } else {
            a.compareTo(b)
        }
        if (comparison != 0) return@Comparator comparison
    }
    left.compareTo(right, ignoreCase = true)
}

private fun Long?.orEmpty(): Long = this ?: 0L
