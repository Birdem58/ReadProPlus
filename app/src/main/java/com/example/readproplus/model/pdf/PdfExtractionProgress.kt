package com.example.readproplus.model.pdf

data class PdfExtractionProgress(
    val currentPage: Int,
    val totalPages: Int,
    val percent: Float,
)
