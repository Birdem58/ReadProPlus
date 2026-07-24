package com.example.readproplus.model.pdf

data class PdfDocument(
    val id: String,
    val title: String,
    val author: String?,
    val totalPages: Int,
    val pages: List<String>,
    val fileSizeBytes: Long = 0,
    val toc: List<TocEntry> = emptyList(),
)
