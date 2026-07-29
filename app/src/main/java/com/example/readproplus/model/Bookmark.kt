package com.example.readproplus.model

data class Bookmark(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val pageNumber: Int,
    val createdAt: Long,
)

data class BookNote(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val pageNumber: Int,
    val noteText: String,
    val textSnippet: String?,
    val createdAt: Long,
)
