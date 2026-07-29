package com.example.readproplus.model

data class ReadingStats(
    val totalReadingTimeSeconds: Long = 0,
    val totalPagesRead: Int = 0,
    val booksFinished: Int = 0,
    val currentStreakDays: Int = 0,
    val lastReadTimestamp: Long = 0,
)
