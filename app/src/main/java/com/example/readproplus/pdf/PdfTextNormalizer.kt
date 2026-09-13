package com.example.readproplus.pdf

/**
 * Applies loss-minimizing cleanup to text returned by PDFBox.
 *
 * PDF text often contains platform-specific line endings, non-breaking
 * spaces, soft hyphens, or typographic ligatures. Keeping this cleanup at the
 * extraction boundary makes the reader, search, and speech paths see the
 * same usable text without flattening meaningful page line breaks.
 */
internal object PdfTextNormalizer {

    private val repeatedWhitespace = Regex("[ \\t]+")
    private val repeatedBlankLines = Regex("\\n{3,}")

    fun normalizePage(text: String): String {
        if (text.isBlank()) return ""

        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u0000', ' ')
            .replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace('\u00AD'.toString(), "")
            .replaceLigatures()
            .lineSequence()
            .map { it.trim() }
            .joinToString("\n")
            .replace(repeatedWhitespace, " ")
            .replace(repeatedBlankLines, "\n\n")
            .trim()
    }

    private fun String.replaceLigatures(): String = buildString(length) {
        for (character in this@replaceLigatures) {
            append(
                when (character) {
                    '\uFB00' -> "ff"
                    '\uFB01' -> "fi"
                    '\uFB02' -> "fl"
                    '\uFB03' -> "ffi"
                    '\uFB04' -> "ffl"
                    '\uFB05', '\uFB06' -> "st"
                    else -> character
                },
            )
        }
    }
}
