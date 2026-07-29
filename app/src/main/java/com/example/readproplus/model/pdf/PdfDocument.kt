package com.example.readproplus.model.pdf

data class PdfDocument(
    val id: String,
    val title: String,
    val author: String?,
    val subject: String? = null,
    val keywords: String? = null,
    val totalPages: Int,
    val pages: List<String>,
    val fileSizeBytes: Long = 0,
    val toc: List<TocEntry> = emptyList(),
    val format: FormatType = FormatType.PDF,
    val isImageBased: Boolean = false,
    val filePath: String? = null,
    val sourceUri: String? = null,
    val pageImageEntries: List<String> = emptyList(),
)
