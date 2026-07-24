package com.example.readproplus.model

data class Highlight(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val pageNumber: Int,
    val text: String,
    val color: Long,
    val createdAt: Long,
)
