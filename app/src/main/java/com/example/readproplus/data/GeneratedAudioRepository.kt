package com.example.readproplus.data

import android.content.Context
import com.example.readproplus.model.tts.GeneratedAudio
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

class GeneratedAudioRepository(context: Context) {

    private val directory = File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }
    private val indexFile = File(directory, INDEX_FILE_NAME)

    @Synchronized
    fun listByBook(bookId: String): List<GeneratedAudio> = loadAll()
        .filter { it.bookId == bookId && audioFile(it).exists() }
        .sortedByDescending(GeneratedAudio::createdAt)

    @Synchronized
    fun save(
        bookId: String,
        bookTitle: String,
        startPage: Int,
        endPage: Int,
        durationMs: Long,
        samples: ShortArray,
    ): GeneratedAudio {
        require(samples.isNotEmpty()) { "Cannot save an empty audio recording." }

        val id = UUID.randomUUID().toString()
        val fileName = "$id.wav"
        val item = GeneratedAudio(
            id = id,
            bookId = bookId,
            bookTitle = bookTitle,
            startPage = startPage,
            endPage = endPage,
            durationMs = durationMs,
            createdAt = System.currentTimeMillis(),
            fileName = fileName,
        )

        writeWav(audioFile(item), samples)
        persist((loadAll() + item).filter { audioFile(it).exists() })
        return item
    }

    @Synchronized
    fun readSamples(item: GeneratedAudio): ShortArray {
        val file = audioFile(item)
        if (!file.exists()) throw IOException("Generated audio file is missing.")

        DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
            val header = ByteArray(WAV_HEADER_SIZE)
            input.readFully(header)
            require(String(header, 0, 4, Charsets.US_ASCII) == "RIFF") { "Invalid WAV file." }
            require(String(header, 8, 12, Charsets.US_ASCII) == "WAVE") { "Invalid WAV file." }

            val dataSize = littleEndianInt(header, 40)
            require(dataSize > 0 && dataSize % 2 == 0) { "Generated WAV has no PCM samples." }
            return ShortArray(dataSize / 2) { input.readShortLE() }
        }
    }

    @Synchronized
    fun delete(item: GeneratedAudio) {
        audioFile(item).delete()
        persist(loadAll().filter { it.id != item.id && audioFile(it).exists() })
    }

    private fun audioFile(item: GeneratedAudio): File {
        // Metadata is app-generated, but keep the file lookup constrained to the
        // private directory even if an old/corrupt index contains a path separator.
        val safeName = File(item.fileName).name
        return File(directory, safeName)
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * BYTES_PER_SAMPLE
        DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
            output.writeBytes("RIFF")
            output.writeIntLE(WAV_HEADER_SIZE - 8 + dataSize)
            output.writeBytes("WAVE")
            output.writeBytes("fmt ")
            output.writeIntLE(16)
            output.writeShortLE(1)
            output.writeShortLE(CHANNEL_COUNT)
            output.writeIntLE(SAMPLE_RATE)
            output.writeIntLE(SAMPLE_RATE * CHANNEL_COUNT * BYTES_PER_SAMPLE)
            output.writeShortLE(CHANNEL_COUNT * BYTES_PER_SAMPLE)
            output.writeShortLE(BITS_PER_SAMPLE)
            output.writeBytes("data")
            output.writeIntLE(dataSize)
            samples.forEach { sample -> output.writeShortLE(sample.toInt()) }
        }
    }

    private fun loadAll(): List<GeneratedAudio> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(indexFile.readText(Charsets.UTF_8))
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                GeneratedAudio(
                    id = item.getString("id"),
                    bookId = item.getString("bookId"),
                    bookTitle = item.optString("bookTitle", "Untitled"),
                    startPage = item.optInt("startPage", 1),
                    endPage = item.optInt("endPage", 1),
                    durationMs = item.optLong("durationMs", 0L),
                    createdAt = item.optLong("createdAt", 0L),
                    fileName = item.getString("fileName"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(items: List<GeneratedAudio>) {
        JSONArray().apply {
            items.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("bookId", item.bookId)
                    put("bookTitle", item.bookTitle)
                    put("startPage", item.startPage)
                    put("endPage", item.endPage)
                    put("durationMs", item.durationMs)
                    put("createdAt", item.createdAt)
                    put("fileName", item.fileName)
                })
            }
        }.also { indexFile.writeText(it.toString(), Charsets.UTF_8) }
    }

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun DataOutputStream.writeIntLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value ushr 8) and 0xFF)
        writeByte((value ushr 16) and 0xFF)
        writeByte((value ushr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value ushr 8) and 0xFF)
    }

    private fun DataInputStream.readShortLE(): Short {
        val low = readUnsignedByte()
        val high = readUnsignedByte()
        return ((high shl 8) or low).toShort()
    }

    private companion object {
        const val DIRECTORY_NAME = "generated_audio"
        const val INDEX_FILE_NAME = "index.json"
        const val SAMPLE_RATE = 24_000
        const val CHANNEL_COUNT = 1
        const val BITS_PER_SAMPLE = 16
        const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
        const val WAV_HEADER_SIZE = 44
    }
}
