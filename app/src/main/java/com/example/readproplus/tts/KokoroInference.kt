package com.example.readproplus.tts

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.util.Log
import java.nio.FloatBuffer
import java.nio.LongBuffer

/** Thin, model-aware ONNX Runtime wrapper for Kokoro v1.0. */
class KokoroInference {

    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var voiceEmbeddings = FloatArray(0)
    private var modelLoaded = false

    @Synchronized
    fun loadModel(modelPath: String, voiceEmbedding: FloatArray) {
        if (modelLoaded) return

        try {
            require(voiceEmbedding.isNotEmpty()) { "A Kokoro voice must be loaded before the model." }
            environment = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setInterOpNumThreads(2)
                setIntraOpNumThreads(4)
            }
            session = environment!!.createSession(modelPath, sessionOptions)
            voiceEmbeddings = voiceEmbedding
            modelLoaded = true
            Log.i(TAG, "Kokoro model loaded: ${session!!.inputInfo.keys}")
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to load ONNX model", error)
            close()
            throw KokoroInferenceException("Failed to load ONNX model: ${error.message}", error)
        }
    }

    @Synchronized
    fun infer(tokenIds: IntArray, speed: Float = 1.0f): FloatArray {
        check(modelLoaded) { "Model not loaded. Call loadModel() first." }

        val env = environment ?: throw IllegalStateException("OrtEnvironment is null")
        val activeSession = session ?: throw IllegalStateException("OrtSession is null")
        val paddedTokenIds = if (tokenIds.isEmpty()) intArrayOf(0, 83, 0) else tokenIds
        val inputNames = activeSession.inputInfo.keys.toList()
        val styleDimension = findStyleDimension(activeSession, inputNames)

        var inputIdsTensor: OnnxTensor? = null
        var styleTensor: OnnxTensor? = null
        var speedTensor: OnnxTensor? = null
        var results: OrtSession.Result? = null

        try {
            inputIdsTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(paddedTokenIds.map(Int::toLong).toLongArray()),
                longArrayOf(1L, paddedTokenIds.size.toLong()),
            )
            val styleEmbedding = selectStyleEmbedding(
                targetSize = styleDimension,
                contentTokenCount = (paddedTokenIds.size - 2).coerceAtLeast(1),
            )
            styleTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(styleEmbedding),
                longArrayOf(1L, styleEmbedding.size.toLong()),
            )
            speedTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(floatArrayOf(speed.coerceIn(MIN_SPEED, MAX_SPEED))),
                longArrayOf(1L),
            )

            val inputs = buildInputs(
                inputNames = inputNames,
                inputIds = inputIdsTensor,
                style = styleTensor,
                speed = speedTensor,
            )
            results = activeSession.run(inputs)
            val outputTensor = results.firstOrNull()?.value as? OnnxTensor
                ?: throw IllegalStateException("Kokoro returned no waveform tensor.")

            return extractWaveform(outputTensor.value)
        } catch (error: Throwable) {
            Log.e(TAG, "Inference execution failed", error)
            throw KokoroInferenceException("Inference error: ${error.message}", error)
        } finally {
            try {
                results?.close()
            } catch (_: Throwable) {
            }
            try {
                inputIdsTensor?.close()
            } catch (_: Throwable) {
            }
            try {
                styleTensor?.close()
            } catch (_: Throwable) {
            }
            try {
                speedTensor?.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun findStyleDimension(session: OrtSession, inputNames: List<String>): Int {
        val styleInput = inputNames.firstOrNull { name ->
            name.contains("speaker", ignoreCase = true) ||
                name.contains("voice", ignoreCase = true) ||
                name.contains("embedding", ignoreCase = true) ||
                name.contains("style", ignoreCase = true)
        } ?: return DEFAULT_STYLE_DIMENSION
        val info = session.inputInfo[styleInput]?.info as? TensorInfo
        return info?.shape?.lastOrNull()?.takeIf { it > 0 }?.toInt() ?: DEFAULT_STYLE_DIMENSION
    }

    private fun buildInputs(
        inputNames: List<String>,
        inputIds: OnnxTensor,
        style: OnnxTensor,
        speed: OnnxTensor,
    ): Map<String, OnnxTensor> = buildMap {
        inputNames.forEach { name ->
            when {
                name.contains("input", ignoreCase = true) ||
                    name.contains("ids", ignoreCase = true) ||
                    name.contains("token", ignoreCase = true) -> put(name, inputIds)

                name.contains("speaker", ignoreCase = true) ||
                    name.contains("voice", ignoreCase = true) ||
                    name.contains("embedding", ignoreCase = true) ||
                    name.contains("style", ignoreCase = true) -> put(name, style)

                name.contains("speed", ignoreCase = true) ||
                    name.contains("rate", ignoreCase = true) -> put(name, speed)
            }
        }
    }

    /**
     * Kokoro voice assets contain one 256-float style vector per supported token
     * length. Selecting by sentence length is required for natural prosody.
     */
    private fun selectStyleEmbedding(targetSize: Int, contentTokenCount: Int): FloatArray {
        require(voiceEmbeddings.isNotEmpty()) { "Voice embeddings are not loaded." }
        val styleCount = voiceEmbeddings.size / targetSize
        if (styleCount > 1 && voiceEmbeddings.size % targetSize == 0) {
            val index = contentTokenCount.coerceIn(0, styleCount - 1)
            val offset = index * targetSize
            return voiceEmbeddings.copyOfRange(offset, offset + targetSize)
        }

        return FloatArray(targetSize).also { output ->
            System.arraycopy(voiceEmbeddings, 0, output, 0, minOf(voiceEmbeddings.size, output.size))
        }
    }

    private fun extractWaveform(output: Any): FloatArray = when (output) {
        is FloatArray -> output
        is Array<*> -> when (val first = output.firstOrNull()) {
            is FloatArray -> first
            is Array<*> -> first.firstOrNull() as? FloatArray
                ?: throw IllegalStateException("Unexpected nested waveform output.")

            else -> throw IllegalStateException("Unexpected waveform output: ${first?.javaClass}")
        }

        else -> throw IllegalStateException("Unexpected waveform output: ${output.javaClass}")
    }

    @Synchronized
    fun close() {
        try {
            session?.close()
        } catch (_: Throwable) {
        }
        try {
            environment?.close()
        } catch (_: Throwable) {
        }
        session = null
        environment = null
        voiceEmbeddings = FloatArray(0)
        modelLoaded = false
    }

    fun isLoaded(): Boolean = modelLoaded

    private companion object {
        const val TAG = "KokoroInference"
        const val DEFAULT_STYLE_DIMENSION = 256
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2.0f
    }
}

class KokoroInferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)
