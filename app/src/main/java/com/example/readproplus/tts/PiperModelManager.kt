package com.example.readproplus.tts

import android.content.Context
import android.util.Log
import com.example.readproplus.model.tts.TtsBackend
import com.example.readproplus.model.tts.TtsVoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

data class PiperModelLocation(
    val modelPath: String,
    val tokensPath: String,
    val dataDirectoryPath: String,
    val usesAssets: Boolean,
)

/** Bundled and on-demand Piper model storage. */
class PiperModelManager(private val context: Context) {

    fun isVoiceAvailable(voice: TtsVoice): Boolean {
        if (voice.backend != TtsBackend.PIPER) return false

        val assetDirectory = voice.piperAssetDirectory
        if (assetDirectory != null && voice.piperModelFileName != null) {
            val model = "$assetDirectory/${voice.piperModelFileName}"
            if (assetFileExists(model) && assetFileExists("$assetDirectory/tokens.txt") &&
                assetDirectoryExists(ASSET_ESPEAK_DIRECTORY)
            ) {
                return true
            }
        }

        val directory = localDirectory(voice)
        return voice.piperModelFileName != null &&
            File(directory, voice.piperModelFileName).isFile &&
            File(directory, TOKENS_FILE_NAME).isFile &&
            File(directory, voice.piperModelFileName).length() > MIN_MODEL_BYTES &&
            File(directory, TOKENS_FILE_NAME).length() > 10L
    }

    suspend fun resolveLocation(voice: TtsVoice): PiperModelLocation = withContextIo {
        require(voice.backend == TtsBackend.PIPER) { "${voice.displayName} is not a Piper voice." }
        require(isVoiceAvailable(voice)) {
            "The ${voice.displayName} Piper model is not downloaded yet."
        }

        val assetDirectory = voice.piperAssetDirectory
        if (assetDirectory != null && voice.piperModelFileName != null &&
            assetFileExists("$assetDirectory/${voice.piperModelFileName}")
        ) {
            // Sherpa can load the model and tokens from APK assets, but the
            // Piper phonemizer expects espeak-ng-data at a real filesystem
            // path. Copy it once before constructing OfflineTts.
            val dataDirectory = ensureLocalEspeakData()
            return@withContextIo PiperModelLocation(
                modelPath = "$assetDirectory/${voice.piperModelFileName}",
                tokensPath = "$assetDirectory/$TOKENS_FILE_NAME",
                dataDirectoryPath = dataDirectory.absolutePath,
                usesAssets = true,
            )
        }

        val dataDirectory = ensureLocalEspeakData()
        PiperModelLocation(
            modelPath = File(localDirectory(voice), requireNotNull(voice.piperModelFileName)).absolutePath,
            tokensPath = File(localDirectory(voice), TOKENS_FILE_NAME).absolutePath,
            dataDirectoryPath = dataDirectory.absolutePath,
            usesAssets = false,
        )
    }

    fun downloadVoice(voice: TtsVoice): Flow<VoiceDownloadProgress> = flow {
        require(voice.backend == TtsBackend.PIPER) { "${voice.displayName} is not a Piper voice." }
        if (isVoiceAvailable(voice)) {
            emit(VoiceDownloadProgress.Complete(voice.id))
            return@flow
        }

        val archiveName = voice.piperArchiveFileName
            ?: throw IOException("No download is configured for ${voice.displayName}.")
        val destinationDirectory = localDirectory(voice)
        val archive = File(destinationDirectory.parentFile, "$archiveName.tmp")
        destinationDirectory.mkdirs()
        archive.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = (URL("$MODEL_BASE_URL$archiveName").openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            val responseCode = connection.responseCode
            if (responseCode !in HttpURLConnection.HTTP_OK..HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("Piper model download failed with HTTP $responseCode")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: UNKNOWN_FILE_SIZE
            var downloadedBytes = 0L
            emit(VoiceDownloadProgress.Downloading(voice.id, 0f, 0L, totalBytes))
            connection.inputStream.use { input ->
                FileOutputStream(archive).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = if (totalBytes > 0L) {
                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        emit(VoiceDownloadProgress.Downloading(voice.id, progress, downloadedBytes, totalBytes))
                    }
                }
            }

            extractModelFiles(archive, voice, destinationDirectory)
            require(isVoiceAvailable(voice)) { "Downloaded ${voice.displayName} model is invalid." }
            emit(VoiceDownloadProgress.Complete(voice.id))
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Log.e(TAG, "Failed to download Piper voice ${voice.id}", error)
            destinationDirectory.deleteRecursively()
            emit(VoiceDownloadProgress.Error(voice.id, error.message ?: "Piper model download failed."))
        } finally {
            archive.delete()
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun extractModelFiles(archive: File, voice: TtsVoice, destinationDirectory: File) {
        val modelName = requireNotNull(voice.piperModelFileName)
        val temporaryDirectory = File(destinationDirectory.parentFile, "${destinationDirectory.name}.tmp")
        temporaryDirectory.deleteRecursively()
        temporaryDirectory.mkdirs()

        try {
            TarArchiveInputStream(
                BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive))),
            ).use { input ->
                while (true) {
                    val entry = input.getNextTarEntry() ?: break
                    if (entry.isDirectory) continue
                    val fileName = File(entry.name.replace('\\', '/')).name
                    if (fileName != modelName && fileName != TOKENS_FILE_NAME) continue
                    val destination = File(temporaryDirectory, fileName)
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                }
            }

            require(File(temporaryDirectory, modelName).length() > MIN_MODEL_BYTES) {
                "The downloaded Piper model file is incomplete."
            }
            require(File(temporaryDirectory, TOKENS_FILE_NAME).length() > 10L) {
                "The downloaded Piper token file is incomplete."
            }
            destinationDirectory.deleteRecursively()
            check(temporaryDirectory.renameTo(destinationDirectory)) {
                "Could not finalize the downloaded Piper model."
            }
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun ensureLocalEspeakData(): File {
        val destination = File(context.filesDir, "piper_tts/espeak-ng-data")
        if (File(destination, "phontab").isFile) return destination
        destination.deleteRecursively()
        copyAssetTree(ASSET_ESPEAK_DIRECTORY, destination)
        require(File(destination, "phontab").isFile) { "Piper phonemizer data is incomplete." }
        return destination
    }

    private fun copyAssetTree(assetPath: String, destination: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            return
        }
        destination.mkdirs()
        children.forEach { child -> copyAssetTree("$assetPath/$child", File(destination, child)) }
    }

    private fun assetFileExists(path: String): Boolean = runCatching {
        context.assets.open(path).use { }
        true
    }.getOrDefault(false)

    private fun assetDirectoryExists(path: String): Boolean =
        context.assets.list(path)?.isNotEmpty() == true

    private fun localDirectory(voice: TtsVoice): File =
        File(context.filesDir, "piper_tts/voices/${voice.id}")

    private suspend fun <T> withContextIo(block: () -> T): T =
        kotlinx.coroutines.withContext(Dispatchers.IO) { block() }

    private companion object {
        const val TAG = "PiperModelManager"
        const val MODEL_BASE_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"
        const val ASSET_ESPEAK_DIRECTORY = "piper/espeak-ng-data"
        const val TOKENS_FILE_NAME = "tokens.txt"
        const val MIN_MODEL_BYTES = 10_000_000L
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 120_000
        const val BUFFER_SIZE = 32_768
        const val UNKNOWN_FILE_SIZE = 67_000_000L
    }
}
