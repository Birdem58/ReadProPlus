package com.example.readproplus.model.tts

sealed interface TtsState {
    data object Idle : TtsState

    data class ModelDownloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : TtsState

    data class Generating(
        val generatedMs: Long,
        val targetMs: Long,
        val progress: Float,
    ) : TtsState

    data class Playing(
        val currentMs: Long,
        val totalMs: Long,
        val progress: Float,
        val speed: Float,
    ) : TtsState

    data class Paused(
        val currentMs: Long,
        val totalMs: Long,
        val progress: Float,
        val speed: Float,
    ) : TtsState

    data object Stopped : TtsState

    data class Error(val message: String, val recoverable: Boolean) : TtsState
}
