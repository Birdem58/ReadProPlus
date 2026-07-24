package com.example.readproplus.model.tts

data class TtsConfig(
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val voiceId: String = "af_nicole",
    val maxSentenceLength: Int = 200,
) {
    init {
        require(speed in 0.5f..2.0f) { "Speed must be in 0.5..2.0, got $speed" }
        require(volume in 0.0f..1.0f) { "Volume must be in 0.0..1.0, got $volume" }
    }
}
