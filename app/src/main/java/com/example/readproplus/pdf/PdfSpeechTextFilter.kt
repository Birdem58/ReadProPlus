package com.example.readproplus.pdf

import java.util.Locale

/**
 * Produces a speech-friendly copy of PDF text without changing the text shown
 * in the reader. The rules are deliberately conservative so normal body text
 * remains intact while common PDF chrome is removed.
 */
object PdfSpeechTextFilter {

    private const val EDGE_LINE_COUNT = 3

    private val whitespace = Regex("\\s+")
    private val standaloneNumber = Regex("^[\\[\\(\\{\\-–—\\s]*\\d{1,6}[\\]\\)\\}\\-–—\\s]*$")
    private val pageLabel = Regex(
        "^page\\s+(?:\\d{1,6}|[ivxlcdm]+)(?:\\s*(?:/|of)\\s*(?:\\d{1,6}|[ivxlcdm]+))?$",
        RegexOption.IGNORE_CASE,
    )
    private val pageNumberWithTotal = Regex(
        "^\\d{1,6}\\s*(?:/|of)\\s*\\d{1,6}$",
        RegexOption.IGNORE_CASE,
    )
    private val imagePagePlaceholder = Regex("^\\[pdf page \\d{1,6}]$", RegexOption.IGNORE_CASE)
    private val caption = Regex(
        "^(?:fig(?:ure)?|table|chart|diagram|image|photo|illustration)\\s*" +
            "(?:[a-z]?\\d+[a-z]?|[ivxlcdm]+)\\b(?:\\s*[:.\\-–—]\\s*|\\s+).+$",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Removes page numbers, repeated header/footer lines, figure or table
     * captions, and standalone web/DOI lines from every page.
     */
    fun mainTextPages(pages: List<String>): List<String> {
        if (pages.isEmpty()) return emptyList()

        val linesByPage = pages.map(::readableLines)
        val repeatedEdgeLines = findRepeatedEdgeLines(linesByPage)

        return linesByPage.map { lines ->
            lines.mapIndexedNotNull { index, line ->
                if (shouldSkip(line, index, lines.size, repeatedEdgeLines)) null else line
            }
                .joinToString("\n")
                .replace(Regex("(?<=[A-Za-z])-\\s*\\n\\s*(?=[a-z])"), "")
                .trim()
        }
    }

    private fun readableLines(page: String): List<String> = page.lineSequence()
        .map { it.replace('\u00A0', ' ').trim() }
        .filter(String::isNotBlank)
        .toList()

    private fun findRepeatedEdgeLines(linesByPage: List<List<String>>): Set<String> {
        if (linesByPage.size < 2) return emptySet()

        val pagesByLine = mutableMapOf<String, MutableSet<Int>>()
        linesByPage.forEachIndexed { pageIndex, lines ->
            (lines.take(EDGE_LINE_COUNT) + lines.takeLast(EDGE_LINE_COUNT)).forEach { line ->
                val key = comparisonKey(line)
                if (isHeaderFooterCandidate(key)) {
                    pagesByLine.getOrPut(key) { mutableSetOf() }.add(pageIndex)
                }
            }
        }

        return pagesByLine
            .filterValues { pageIndexes -> pageIndexes.size >= 2 }
            .keys
    }

    private fun shouldSkip(
        line: String,
        index: Int,
        lineCount: Int,
        repeatedEdgeLines: Set<String>,
    ): Boolean {
        if (isPageMarker(line) || imagePagePlaceholder.matches(line) ||
            isCaption(line) || isStandaloneLink(line)
        ) return true

        val isPageEdge = index < EDGE_LINE_COUNT || index >= lineCount - EDGE_LINE_COUNT
        return isPageEdge && comparisonKey(line) in repeatedEdgeLines
    }

    private fun comparisonKey(line: String): String = line
        .replace('\u00A0', ' ')
        .lowercase(Locale.ROOT)
        .replace(whitespace, " ")
        .trim()

    private fun isHeaderFooterCandidate(key: String): Boolean =
        key.length in 3..120 && key.any(Char::isLetter)

    private fun isPageMarker(line: String): Boolean {
        val compact = line.trim().replace(whitespace, " ")
        return standaloneNumber.matches(compact) ||
            pageLabel.matches(compact) ||
            pageNumberWithTotal.matches(compact)
    }

    private fun isCaption(line: String): Boolean =
        line.length <= 240 && caption.matches(line.trim())

    private fun isStandaloneLink(line: String): Boolean {
        val compact = line.trim().lowercase(Locale.ROOT)
        return compact.startsWith("http://") ||
            compact.startsWith("https://") ||
            compact.startsWith("www.") ||
            compact.startsWith("doi:")
    }
}
