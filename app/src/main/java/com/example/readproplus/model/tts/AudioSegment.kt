package com.example.readproplus.model.tts

import java.nio.ByteBuffer

data class AudioSegment(
    val text: String,
    val sampleRate: Int = 24000,
    val samples: ShortArray,
    val durationMs: Long,
) {
    val byteBuffer: ByteBuffer by lazy {
        val buf = ByteBuffer.allocateDirect(samples.size * 2)
        buf.asShortBuffer().put(samples)
        buf.rewind()
        buf
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioSegment) return false
        return text == other.text && sampleRate == other.sampleRate &&
            samples.contentEquals(other.samples) && durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + samples.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}
