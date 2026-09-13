package com.example.readproplus.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.readproplus.model.tts.GeneratedAudio
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratedAudioRepositoryInstrumentedTest {

    @Test
    fun generatedWavRoundTripsAndLegacyRawPcmIsMigrated() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = GeneratedAudioRepository(context)
        val samples = shortArrayOf(-32768, -1024, 0, 1024, 32767)
        val testId = "wav-test-${System.currentTimeMillis()}"

        val generated = repository.save(
            bookId = testId,
            bookTitle = "WAV test",
            startPage = 1,
            endPage = 1,
            durationMs = 1L,
            samples = samples,
        )
        try {
            assertArrayEquals(samples, repository.readSamples(generated))

            val legacyFile = File(context.filesDir, "generated_audio/$testId-legacy.wav")
            writeRawPcm(legacyFile, samples)
            val legacy = GeneratedAudio(
                id = "$testId-legacy",
                bookId = testId,
                bookTitle = "WAV test",
                startPage = 1,
                endPage = 1,
                durationMs = 1L,
                createdAt = 0L,
                fileName = legacyFile.name,
            )

            assertArrayEquals(samples, repository.readSamples(legacy))
            assertEquals("RIFF", String(legacyFile.readBytes(), 0, 4, StandardCharsets.US_ASCII))
            legacyFile.delete()
        } finally {
            repository.delete(generated)
        }
    }

    private fun writeRawPcm(file: File, samples: ShortArray) {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            samples.forEach { sample ->
                val value = sample.toInt()
                output.write(value and 0xFF)
                output.write((value ushr 8) and 0xFF)
            }
        }
    }
}
