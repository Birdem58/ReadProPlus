package com.example.readproplus.model

data class ReaderProgress(
    val bookId: String,
    val bookTitle: String,
    val currentPage: Int,
    val totalPages: Int,
    val lastReadAt: Long,
) {
    val progressFraction: Float
        get() = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
}
