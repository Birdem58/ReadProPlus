package com.example.readproplus.model.pdf

enum class FormatType(val displayName: String, val extension: String) {
    PDF("PDF", "pdf"),
    EPUB("EPUB", "epub"),
    MOBI("MOBI", "mobi"),
    AZW3("AZW3", "azw3"),
    FB2("FB2", "fb2"),
    ODT("ODT", "odt"),
    DOC("DOC", "doc"),
    DOCX("DOCX", "docx"),
    DJVU("DjVu", "djvu"),
    CBZ("CBZ", "cbz"),
    CBR("CBR", "cbr");

    companion object {
        fun fromExtension(ext: String): FormatType {
            val clean = ext.lowercase().trimStart('.')
            return entries.firstOrNull { it.extension == clean } ?: PDF
        }

        val ALL_EXTENSIONS = entries.map { it.extension }.toSet()
    }
}
