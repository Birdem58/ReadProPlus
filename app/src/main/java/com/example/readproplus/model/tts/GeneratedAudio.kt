package com.example.readproplus.model.tts

data class GeneratedAudio(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val startPage: Int,
    val endPage: Int,
    val durationMs: Long,
    val createdAt: Long,
    val fileName: String,
)
