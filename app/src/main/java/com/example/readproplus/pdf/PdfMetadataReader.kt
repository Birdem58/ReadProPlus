package com.example.readproplus.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument

data class PdfMetadata(
    val title: String?,
    val author: String?,
    val subject: String?,
    val keywords: String?,
)

object PdfMetadataReader {
    fun read(doc: PDDocument): PdfMetadata {
        val info = doc.documentInformation
        return PdfMetadata(
            title = info.title,
            author = info.author,
            subject = info.subject,
            keywords = info.keywords,
        )
    }
}
