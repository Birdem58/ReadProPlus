package com.example.readproplus.model.audiobook

enum class AudiobookStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    PAUSED,
    ERROR
}

data class AudiobookPageMarker(
    val pageIndex: Int, // 1-based page number (1..totalPages)
    val startMs: Long,
    val endMs: Long,
    val sampleOffset: Long,
    val sampleCount: Long,
)

data class AudiobookManifest(
    val bookId: String,
    val lastProcessedPage: Int, // 1-based page number
    val totalDurationMs: Long,
    val totalSampleCount: Long,
    val markers: List<AudiobookPageMarker> = emptyList(),
)

data class Audiobook(
    val id: String,
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverPath: String? = null,
    val sourceUri: String? = null,
    val filePath: String? = null,
    val mainTextOnly: Boolean = true,
    val voiceId: String,
    val voiceName: String,
    val engineType: String = "PIPER",
    val language: String = "tr",
    val startPage: Int = 1, // 1-based
    val endPage: Int = 1,   // 1-based
    val totalPages: Int = 1,
    val processedPages: Int = 0,
    val checkpointProgress: Float = 0f,
    val checkpointDurationMs: Long = 0L,
    val totalEstimatedDurationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val currentPageIndex: Int = 1, // 1-based page number (1..totalPages)
    val status: AudiobookStatus = AudiobookStatus.QUEUED,
    val errorMessage: String? = null,
    val isPlayable: Boolean = false,
    val audioFileName: String = "audiobook_${id}.wav",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val currentPage: Int
        get() = currentPageIndex.coerceIn(1, totalPages.coerceAtLeast(1))
}
