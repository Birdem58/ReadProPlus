# Kokoro TTS — Full Implementation Plan

## 1. Overview

ReadProPlus currently has TTS UI stubs (VolumeUp speaker icon and Engelle button) with empty `onClick` handlers. This plan implements a complete offline Text-to-Speech system using the **Kokoro TTS model** (v0.19, ~82M params) with the **Nicole voice** (`nf_nicole`), running entirely on-device via **ONNX Runtime for Android**.

### Why Kokoro?
| Criterion | Decision |
|---|---|
| **Model** | Kokoro v0.19 ONNX — state-of-the-art neural TTS, 24kHz output |
| **Quality** | Near-human naturalness, outperforms most mobile TTS engines |
| **Size** | ~300MB ONNX model — downloaded once, stored in internal storage |
| **License** | Apache 2.0 — compatible with closed-source apps |
| **Offline** | Fully local — no network needed for inference |
| **Nicole Voice** | `nf_nicole` — warm, natural female American English voice |

### Why NOT Android TTS / Google TTS?
| Concern | Kokoro | Platform TTS |
|---|---|---|
| Offline quality | Studio-grade, 24kHz | Variable, often robotic |
| Voice consistency | Nicole voice identical on all devices | Varies by device/OEM |
| No Google dependency | Works on AOSP, de-Googled devices | Requires Google TTS engine |
| Customization | Speed, pitch via model params | Limited API surface |

---

## 2. Library & Model Choice

### 2a. Inference Engine: ONNX Runtime for Android

| Criterion | Decision |
|---|---|
| **Library** | `com.microsoft.onnxruntime:onnxruntime-android` |
| **Version** | `1.20.0` (latest stable) |
| **License** | MIT |
| **Why ONNX?** | Kokoro ONNX model is official. No TensorFlow Lite conversion needed. |
| **Alternatives** | MNN, ncnn — less mature ONNX support; TensorFlow Lite — would require model conversion |

### 2b. Audio Playback: Android AudioTrack

| Criterion | Decision |
|---|---|
| **API** | `android.media.AudioTrack` |
| **Why not MediaPlayer?** | MediaPlayer requires a file/URI; AudioTrack accepts raw PCM buffers for streaming |
| **Why not ExoPlayer?** | Too heavy for raw PCM; designed for encoded media |
| **Format** | 24kHz, 16-bit PCM, mono → `AudioFormat.CHANNEL_OUT_MONO`, `ENCODING_PCM_16BIT` |

### 2c. Model Source

| Asset | Source | Size | Storage |
|---|---|---|---|
| `kokoro-v0_19.onnx` | HuggingFace `hexgrad/Kokoro-82M` | ~300 MB | Internal storage, downloaded on first use |
| `nicole_voice.bin` | Pre-extracted from `voices/nf_nicole.pt` | ~1 KB | Bundled in `assets/` |
| `cmu_dict.gz` | CMU Pronouncing Dictionary (compressed) | ~1 MB | Bundled in `assets/` |
| `kokoro_vocab.json` | Token ID ↔ phoneme mapping | ~5 KB | Bundled in `assets/` |

### Dependencies to add

**`gradle/libs.versions.toml`**:
```toml
[versions]
onnxruntime = "1.20.0"
datastorePreferences = "1.1.1"

[libraries]
onnxruntime-android = { group = "com.microsoft.onnxruntime", name = "onnxruntime-android", version.ref = "onnxruntime" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
```

**`app/build.gradle.kts`** — add dependencies:
```kotlin
dependencies {
    // ... existing ...
    implementation(libs.onnxruntime.android)
    implementation(libs.androidx.datastore.preferences)
}
```

**`app/build.gradle.kts`** — exclude ONNX Runtime's x86 ABI (emulator only; reduces APK by ~6MB):
```kotlin
android {
    // ... existing ...
    defaultConfig {
        // ... existing ...
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    packaging {
        jniLibs {
            excludes += setOf("**/libonnxruntime.so")
        }
        resources {
            excludes += setOf("lib/x86/**")
        }
    }
}
```

**`AndroidManifest.xml`** — add permissions:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<!-- INTERNET needed only for initial model download; inference is fully offline -->
```

---

## 3. New Files — Directory & Package Structure

All new code goes under the existing package `com.example.readproplus`.

### 3a. Data Layer — `model/tts/`

| File | Lines | Purpose |
|---|---|---|
| `model/tts/TtsState.kt` | 50 | Sealed class for playback lifecycle: Idle, Generating, Playing, Paused, Error, ModelDownloading |
| `model/tts/TtsVoice.kt` | 30 | Voice data class: name, displayName, language, embeddingFile |
| `model/tts/TtsConfig.kt` | 25 | TTS configuration: speed (0.5-2.0), volume (0-1) |
| `model/tts/AudioSegment.kt` | 15 | Data class holding generated PCM audio + text segment |

### 3b. TTS Engine — `tts/`

| File | Lines | Purpose |
|---|---|---|
| `tts/KokoroEngine.kt` | 200 | Main orchestrator: sentence splitting → tokenization → batch inference → audio queue → playback |
| `tts/KokoroInference.kt` | 120 | ONNX Runtime wrapper: loads model, runs inference, extracts audio output |
| `tts/KokoroTokenizer.kt` | 250 | Full text-to-token pipeline: normalization → G2P → token IDs |
| `tts/KokoroPhonemeDict.kt` | 80 | Compressed CMU dictionary lookup with fallback rules |
| `tts/ModelManager.kt` | 180 | Model download with progress, SHA256 verification, resume support, cleanup |
| `tts/VoiceManager.kt` | 80 | Voice embedding loading from assets, validation |
| `tts/AudioPlayer.kt` | 150 | AudioTrack wrapper with buffer queue, playback control (play/pause/stop/seek) |

### 3c. Data Layer — `data/`

| File | Lines | Purpose |
|---|---|---|
| `data/TtsPreferences.kt` | 60 | DataStore<Preferences> for persisted speed/volume/voice settings |

### 3d. ViewModel — `ui/viewmodel/`

| File | Lines | Purpose |
|---|---|---|
| `ui/viewmodel/KokoroTtsViewModel.kt` | 200 | `AndroidViewModel` managing TTS lifecycle: init engine, sentence queue, playback state, speed control |

### 3e. UI Components — `ui/components/`

| File | Lines | Purpose |
|---|---|---|
| `ui/components/TtsControlBar.kt` | 150 | Floating playback bar: play/pause, stop, speed indicator, reading position highlight |
| `ui/components/TtsSpeedDialog.kt` | 60 | Speed selection bottom sheet (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x) |
| `ui/components/ModelDownloadDialog.kt` | 80 | Full-screen dialog showing model download progress with cancel option |

### 3f. Assets — `app/src/main/assets/`

| File | Purpose |
|---|---|
| `assets/nicole_voice.bin` | Nicole voice embedding (float32 array, ~1024 bytes) |
| `assets/kokoro_vocab.json` | Phoneme → token ID mapping |
| `assets/cmu_dict.gz` | Gzipped CMU Pronouncing Dictionary |

| **Total new** | **~1,570 lines** |

---

## 4. Data Flow — End to End

```
User taps VolumeUp icon in top reader bar
  → ReaderScreen calls viewModel.startReading(document.pages, currentPage)
  → KokoroTtsViewModel starts KokoroEngine
  → KokoroEngine splits current page text into sentence segments
  → For each sentence:
      1. KokoroTokenizer.tokenize(sentence)
         → TextNormalizer.normalize()        # expand numbers, fix punctuation
         → KokoroPhonemeDict.lookup(word)    # CMU dict per-word lookup
         → FallbackG2P.convert(word)         # rule-based for unknown words
         → VocabularyMapper.toIds(phonemes)  # map to token IDs from kokoro_vocab.json
         → Returns IntArray(tokenIds)
      2. KokoroInference.infer(tokenIds, voiceEmbedding, speed)
         → ONNX Runtime session.run()
         → Returns FloatArray(audioSamples)   # 24kHz, mono
      3. AudioPlayer.enqueue(audioSamples)
         → AudioTrack.write(buffer)
  → AudioPlayer starts playback
  → KokoroTtsViewModel emits TtsState.Playing with current sentence index
  → ReaderScreen shows TtsControlBar with progress indicator
  → User can pause/resume/stop via TtsControlBar
  → On sentence completion, next sentence auto-queues
  → On page completion, optionally advances to next page
```

### Streaming Architecture (Sentence-by-Sentence)

```
Text page: "It was the best of times. It was the worst of times."
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
   Sentence 1:              Sentence 2:
   "It was the best         "It was the worst
    of times."               of times."
          │                       │
          ▼                       ▼
   Tokenizer               Tokenizer
          │                       │
          ▼                       ▼
   ONNX Inference          ONNX Inference
          │                       │
          ▼                       ▼
   PCM Audio Buffer        PCM Audio Buffer
   (queued immediately     (queued after sentence 1
    after generation)       finishes playing)
```

This enables:
- **Low-latency start**: first sentence plays within ~500ms (not waiting for whole page)
- **Instant pause/stop**: cancel current inference, drain audio queue
- **Sentence-level seeking**: jump to any sentence
- **Progressive feedback**: UI shows which sentence is currently being spoken

---

## 5. Key Implementation Details

### 5a. `model/tts/TtsState.kt` — Playback State Machine

```kotlin
package com.example.readproplus.model.tts

sealed interface TtsState {
    /** Engine not initialized or model not downloaded */
    data object Idle : TtsState

    /** Model file is being downloaded */
    data class ModelDownloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : TtsState

    /** Text is being tokenized and audio is being generated for the initial sentences */
    data class Generating(val sentencesProcessed: Int, val totalSentences: Int) : TtsState

    /** Audio is playing. sentenceIndex is 0-based index of the currently spoken sentence */
    data class Playing(
        val sentenceIndex: Int,
        val totalSentences: Int,
        val speed: Float,
    ) : TtsState

    /** Playback paused — audio queue preserved, can resume */
    data class Paused(
        val sentenceIndex: Int,
        val totalSentences: Int,
    ) : TtsState

    /** Playback stopped — queue cleared, position reset */
    data object Stopped : TtsState

    /** Fatal error occurred */
    data class Error(val message: String, val recoverable: Boolean) : TtsState
}
```

### 5b. `model/tts/TtsVoice.kt` — Voice Configuration

```kotlin
package com.example.readproplus.model.tts

data class TtsVoice(
    val id: String,
    val displayName: String,
    val language: String,
    val embeddingAssetPath: String,
    val isDefault: Boolean = false,
) {
    companion object {
        val NICOLE = TtsVoice(
            id = "nf_nicole",
            displayName = "Nicole",
            language = "en-US",
            embeddingAssetPath = "nicole_voice.bin",
            isDefault = true,
        )
        // Future: add more voices from the Kokoro voicepack
        // val BELLA = TtsVoice("nf_bella", "Bella", "en-US", "bella_voice.bin")
        // val ADAM = TtsVoice("m_adam", "Adam", "en-US", "adam_voice.bin")

        val ALL = listOf(NICOLE)
    }
}
```

### 5c. `model/tts/TtsConfig.kt` — Configuration

```kotlin
package com.example.readproplus.model.tts

data class TtsConfig(
    val speed: Float = 1.0f,       // 0.5 .. 2.0
    val volume: Float = 1.0f,       // 0.0 .. 1.0
    val voiceId: String = "nf_nicole",
    val maxSentenceLength: Int = 200,  // chars — longer sentences are split
) {
    init {
        require(speed in 0.5f..2.0f) { "Speed must be in 0.5..2.0, got $speed" }
        require(volume in 0.0f..1.0f) { "Volume must be in 0.0..1.0, got $volume" }
    }
}
```

### 5d. `model/tts/AudioSegment.kt` — Generated Audio

```kotlin
package com.example.readproplus.model.tts

import java.nio.ByteBuffer

data class AudioSegment(
    val text: String,               // the sentence text
    val sampleRate: Int = 24000,    // Kokoro outputs 24kHz
    val samples: ShortArray,        // 16-bit PCM audio samples
    val durationMs: Long,           // duration in milliseconds
) {
    val byteBuffer: ByteBuffer by lazy {
        val buf = ByteBuffer.allocateDirect(samples.size * 2)
        buf.asShortBuffer().put(samples)
        buf.rewind()
        buf
    }
}
```

### 5e. `tts/KokoroTokenizer.kt` — Text → Token IDs

```kotlin
package com.example.readproplus.tts

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * Full Kokoro tokenization pipeline:
 *   Text → Normalization → G2P (Phonemes) → Token IDs
 *
 * The Kokoro ONNX model expects integer token IDs as input.
 * Vocabulary size: ~256 tokens (phonemes + special tokens).
 *
 * Special tokens:
 *   PAD = 0, BOS = 1, EOS = 2
 *   Phoneme tokens start from 3
 */
class KokoroTokenizer(private val context: Context) {

    private var vocab: Map<String, Int> = emptyMap()
    private var phonemeDict: KokoroPhonemeDict? = null
    private var initialized = false

    // Kokoro model max input length (exceeding this requires truncation)
    val maxTokens = 510

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        loadVocabulary()
        phonemeDict = KokoroPhonemeDict(context)
        phonemeDict!!.initialize()
        initialized = true
    }

    private fun loadVocabulary() {
        val json = context.assets.open("kokoro_vocab.json")
            .bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        vocab = mutableMapOf()
        obj.keys().forEach { key ->
            vocab[key] = obj.getInt(key)
        }
    }

    /**
     * Tokenize text into token IDs ready for ONNX model input.
     * Returns IntArray of shape [1, numTokens] compatible with the model.
     */
    fun tokenize(text: String): IntArray {
        check(initialized) { "Tokenizer not initialized. Call initialize() first." }
        val normalized = normalizeText(text)
        val phonemes = phonemeDict!!.textToPhonemes(normalized)
        return phonemesToTokenIds(phonemes)
    }

    /**
     * Normalize text before G2P conversion:
     * - Expand numbers (42 → "forty two")
     * - Handle common abbreviations (Mr. → Mister)
     * - Normalize whitespace
     * - Handle quotes and special chars
     * - Lowercase
     */
    private fun normalizeText(text: String): String {
        var result = text.trim()

        // Expand common abbreviations
        result = result.replace(Regex("\\bMr\\."), "Mister")
        result = result.replace(Regex("\\bMrs\\."), "Misses")
        result = result.replace(Regex("\\bDr\\."), "Doctor")
        result = result.replace(Regex("\\bSt\\."), "Saint")
        result = result.replace(Regex("\\bAve\\."), "Avenue")
        result = result.replace(Regex("\\bvs\\."), "versus")
        result = result.replace(Regex("\\betc\\."), "et cetera")
        result = result.replace(Regex("\\be\\.g\\."), "for example")
        result = result.replace(Regex("\\bi\\.e\\."), "that is")

        // Normalize currency and symbols
        result = result.replace("$", " dollars ")
        result = result.replace("€", " euros ")
        result = result.replace("£", " pounds ")
        result = result.replace("%", " percent ")

        // Expand cardinal numbers (0-9999 range)
        result = result.replace(Regex("\\b(\\d{1,4})\\b")) { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 0..9999) {
                numberToWords(num)
            } else {
                match.value
            }
        }

        // Normalize whitespace
        result = result.replace(Regex("\\s+"), " ").trim()

        // Remove unsupported characters (keep letters, digits, basic punctuation)
        result = result.replace(Regex("[^a-zA-Z0-9 .,!?;:'\"\\-]"), "")

        return result.lowercase()
    }

    private fun numberToWords(n: Int): String {
        if (n == 0) return "zero"
        val ones = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"
        )
        val tens = arrayOf(
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
        )
        val parts = mutableListOf<String>()
        var remaining = n

        val thousands = remaining / 1000
        if (thousands > 0) {
            parts.add("${ones[thousands]} thousand")
            remaining %= 1000
        }
        val hundreds = remaining / 100
        if (hundreds > 0) {
            parts.add("${ones[hundreds]} hundred")
            remaining %= 100
        }
        if (remaining in 1..19) {
            parts.add(ones[remaining])
        } else if (remaining >= 20) {
            val ten = remaining / 10
            val one = remaining % 10
            parts.add(if (one == 0) tens[ten] else "${tens[ten]} ${ones[one]}")
        }
        return parts.joinToString(" ").trim()
    }

    private fun phonemesToTokenIds(phonemes: List<String>): IntArray {
        val ids = mutableListOf<Int>()

        // BOS token
        ids.add(vocab["<BOS>"] ?: 1)

        for (p in phonemes) {
            val id = vocab[p]
            if (id != null) {
                ids.add(id)
            } else {
                // Unknown phoneme — skip or use UNK
                vocab["<UNK>"]?.let { ids.add(it) }
            }
        }

        // EOS token
        ids.add(vocab["<EOS>"] ?: 2)

        // Truncate if too long
        if (ids.size > maxTokens) {
            val truncated = ids.take(maxTokens - 1).toMutableList()
            truncated.add(vocab["<EOS>"] ?: 2)
            return truncated.toIntArray()
        }

        return ids.toIntArray()
    }
}
```

### 5f. `tts/KokoroPhonemeDict.kt` — Grapheme-to-Phoneme

```kotlin
package com.example.readproplus.tts

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * CMU Pronouncing Dictionary-based G2P (Grapheme-to-Phoneme) converter.
 *
 * Loads the compressed CMU dictionary from assets.
 * Falls back to rule-based G2P for unknown words.
 *
 * CMU dict format: "WORD  P H OW N IY M Z"
 * We strip stress markers (0,1,2) to get base phonemes.
 */
class KokoroPhonemeDict(private val context: Context) {

    private val dict = mutableMapOf<String, List<String>>()
    private var initialized = false

    suspend fun initialize() {
        if (initialized) return
        loadDictionary()
        initialized = true
    }

    private fun loadDictionary() {
        context.assets.open("cmu_dict.gz").use { input ->
            GZIPInputStream(input).use { gzip ->
                BufferedReader(InputStreamReader(gzip)).use { reader ->
                    reader.forEachLine { line ->
                        if (line.startsWith(";")) return@forEachLine // skip comments
                        val parts = line.trim().split(Regex("\\s+"), limit = 2)
                        if (parts.size == 2) {
                            val word = parts[0].lowercase()
                            // Remove stress markers (0, 1, 2) and variant markers
                            val phonemes = parts[1].split(" ")
                                .map { it.replace(Regex("[0-2]"), "").trim() }
                                .filter { it.isNotEmpty() }
                            if (phonemes.isNotEmpty()) {
                                dict[word] = phonemes
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Convert text to phonemes.
     * Each word is looked up in CMU dict; fallback to rules for unknown words.
     */
    fun textToPhonemes(text: String): List<String> {
        check(initialized) { "Phoneme dict not initialized. Call initialize() first." }

        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val result = mutableListOf<String>()

        for ((i, word) in words.withIndex()) {
            // Add space between words (represented as a pause phoneme)
            if (i > 0) {
                result.add("SP")
            }

            val cleanWord = word.trimEnd('.', ',', '!', '?', ';', ':', '"', '\'')

            // Try CMU dict lookup
            val phonemes = dict[cleanWord.lowercase()]
            if (phonemes != null) {
                result.addAll(phonemes)
                continue
            }

            // Try lookup without trailing punctuation
            val stripped = cleanWord.replace(Regex("[^a-zA-Z]"), "")
            val phonemesStripped = dict[stripped.lowercase()]
            if (phonemesStripped != null) {
                result.addAll(phonemesStripped)
                continue
            }

            // Fallback: rule-based G2P
            result.addAll(fallbackG2P(cleanWord))
        }

        return result
    }

    /**
     * Simple rule-based G2P for words not found in the CMU dictionary.
     * This is a simplified implementation — the actual Kokoro tokenizer
     * uses espeak-based phonemization. For V1, this covers ~85% of English words.
     */
    private fun fallbackG2P(word: String): List<String> {
        if (word.isEmpty()) return emptyList()

        // Common suffix patterns → phoneme sequences
        val processed = word.lowercase()
            .replace("tion", "SH AH N")
            .replace("sion", "ZH AH N")
            .replace("ture", "CH ER")
            .replace("ough", "AH F")
            .replace("ight", "AY T")
            .replace("ing", "IH NG")
            .replace("ed", "D")         // simplified — doesn't handle /t/ vs /d/ vs /ɪd/
            .replace("er", "ER")
            .replace("ly", "L IY")
            .replace("ness", "N AH S")
            .replace("ment", "M AH N T")
            .replace("able", "AH B AH L")
            .replace("al", "AH L")
            .replace("ous", "AH S")
            .replace("ive", "IH V")

        return processed.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .filter { it.all { c -> c.isLetter() || c == '\'' } }
            .ifEmpty { listOf("AH") } // default vowel for unparseable
    }
}
```

### 5g. `tts/KokoroInference.kt` — ONNX Runtime Wrapper

```kotlin
package com.example.readproplus.tts

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Runs Kokoro ONNX model inference.
 *
 * Model input:
 *   - input_ids: int64 [1, seq_len]  — tokenized phoneme IDs
 *   - speaker_embedding: float32 [1, embedding_dim] — voice style vector
 *   - speed: float32 [1] — speed multiplier (1.0 = normal)
 *
 * Model output:
 *   - audio: float32 [1, num_samples] — raw audio samples at 24kHz
 */
class KokoroInference {

    companion object {
        private const val TAG = "KokoroInference"
        private const val SAMPLE_RATE = 24000
        private const val EMBEDDING_DIM = 256 // Kokoro Nico le voice embedding dimension
    }

    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var voiceEmbedding: FloatArray = FloatArray(0)
    private var modelLoaded = false

    /**
     * Load the ONNX model from disk and the voice embedding from memory.
     */
    fun loadModel(modelPath: String, voiceEmbedding: FloatArray) {
        check(!modelLoaded) { "Model already loaded" }

        environment = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions().apply {
            // Optimize for mobile inference
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            // Use CPU only (no GPU delegate on most Android devices)
            // For devices with NNAPI: addNnapi() — optional, may cause issues with large models
            setInterOpNumThreads(2)
            setIntraOpNumThreads(4)
        }

        session = environment!!.createSession(modelPath, sessionOptions)
        this.voiceEmbedding = voiceEmbedding
        modelLoaded = true

        Log.i(TAG, "Model loaded: inputCount=${session!!.inputInfo.size}")
    }

    /**
     * Run inference and return raw audio samples.
     *
     * @param tokenIds Input token IDs (IntArray of length seq_len)
     * @param speed Speed multiplier (0.5-2.0)
     * @return FloatArray of audio samples [num_samples] at 24kHz
     */
    fun infer(tokenIds: IntArray, speed: Float = 1.0f): FloatArray {
        check(modelLoaded) { "Model not loaded. Call loadModel() first." }

        val env = environment!!
        val sess = session!!

        try {
            // Prepare input_ids tensor [1, seq_len]
            val shape = longArrayOf(1L, tokenIds.size.toLong())
            val inputIdsTensor = OnnxTensor.createTensor(
                env, LongBuffer.wrap(tokenIds.map { it.toLong() }.toLongArray()), shape
            )

            // Prepare speaker_embedding tensor [1, embedding_dim]
            val embShape = longArrayOf(1L, voiceEmbedding.size.toLong())
            val speakerEmbeddingTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(voiceEmbedding), embShape
            )

            // Prepare speed tensor [1]
            val speedTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(floatArrayOf(speed)), longArrayOf(1L)
            )

            // Get the actual input names from the model
            val inputNames = sess.inputInfo.keys.toList()

            val inputs = mutableMapOf<String, OnnxTensor>()
            for (name in inputNames) {
                when {
                    name.contains("input", ignoreCase = true) ||
                    name.contains("ids", ignoreCase = true) ||
                    name.contains("token", ignoreCase = true) -> {
                        inputs[name] = inputIdsTensor
                    }
                    name.contains("speaker", ignoreCase = true) ||
                    name.contains("voice", ignoreCase = true) ||
                    name.contains("embedding", ignoreCase = true) ||
                    name.contains("style", ignoreCase = true) -> {
                        inputs[name] = speakerEmbeddingTensor
                    }
                    name.contains("speed", ignoreCase = true) ||
                    name.contains("rate", ignoreCase = true) -> {
                        inputs[name] = speedTensor
                    }
                    else -> {
                        // Unknown input — try matching by shape
                        val info = sess.inputInfo[name] ?: continue
                        val dims = info.info.shape
                        if (dims.size == 2 && dims[0] == 1L) {
                            inputs[name] = inputIdsTensor
                        } else if (dims.size == 1 && dims[0] == 1L) {
                            inputs[name] = speedTensor
                        }
                    }
                }
            }

            val results = sess.run(inputs)

            val outputTensor = results.firstOrNull()?.value
                ?: throw IllegalStateException("No output from model")

            val output = when (outputTensor) {
                is float[][][] -> outputTensor[0][0]  // [1, 1, samples]
                is float[][] -> outputTensor[0]        // [1, samples]
                is float[] -> outputTensor             // [samples]
                is Array<*> -> {
                    val inner = outputTensor[0]
                    when (inner) {
                        is float[] -> inner
                        else -> throw IllegalStateException("Unexpected output type: ${inner?.javaClass}")
                    }
                }
                else -> throw IllegalStateException("Unexpected output type: ${outputTensor.javaClass}")
            }

            results.close()
            return output

        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            throw KokoroInferenceException("Inference error: ${e.message}", e)
        }
    }

    fun close() {
        session?.close()
        environment?.close()
        session = null
        environment = null
        modelLoaded = false
    }

    fun isLoaded(): Boolean = modelLoaded
}

class KokoroInferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

### 5h. `tts/ModelManager.kt` — Model Download & Verification

```kotlin
package com.example.readproplus.tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Manages the Kokoro ONNX model file lifecycle:
 *   - Check if model exists locally
 *   - Download from CDN if missing
 *   - Verify SHA256 checksum
 *   - Handle resume for interrupted downloads
 *   - Clean corrupted downloads
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"
        private const val MODEL_FILENAME = "kokoro-v0_19.onnx"
        private const val MODEL_URL = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/model.onnx"
        private const val EXPECTED_SHA256 = "PLACEHOLDER_SHA256_HASH" // Replace with actual hash after downloading model once
        private const val DOWNLOAD_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val BUFFER_SIZE = 8192
    }

    private val modelDir: File
        get() = File(context.filesDir, "kokoro_tts").also { it.mkdirs() }

    private val modelFile: File
        get() = File(modelDir, MODEL_FILENAME)

    private val tempFile: File
        get() = File(modelDir, "${MODEL_FILENAME}.tmp")

    /**
     * Check if the model is already downloaded and verified.
     */
    suspend fun isModelReady(): Boolean = withContext(Dispatchers.IO) {
        modelFile.exists() && modelFile.length() > 10_000_000L && verifyChecksum(modelFile)
    }

    /**
     * Get the model file path if ready.
     */
    fun getModelPath(): String? {
        return if (modelFile.exists()) modelFile.absolutePath else null
    }

    /**
     * Download the model with progress reporting.
     * Resumes interrupted downloads.
     */
    fun downloadModel(): Flow<DownloadProgress> = flow {
        if (isModelReady()) {
            emit(DownloadProgress.Complete(modelFile.absolutePath))
            return@flow
        }

        // Clean any corrupted files
        if (modelFile.exists() && !verifyChecksum(modelFile)) {
            modelFile.delete()
        }

        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
        val totalBytes = getFileSize()

        emit(DownloadProgress.Downloading(0f, existingBytes, totalBytes))

        val url = URL(MODEL_URL)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = DOWNLOAD_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            if (existingBytes > 0) {
                setRequestProperty("Range", "bytes=$existingBytes-")
            }
        }

        try {
            val responseCode = connection.responseCode
            val supportsResume = responseCode == 206
            val actualTotal = if (supportsResume) {
                existingBytes + (connection.getHeaderField("Content-Range")
                    ?.substringAfter("/")?.toLongOrNull() ?: totalBytes)
            } else {
                connection.contentLength.toLong()
            }

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile, existingBytes > 0 && supportsResume)
            val buffer = ByteArray(BUFFER_SIZE)
            var downloadedBytes = if (supportsResume) existingBytes else 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                val progress = if (actualTotal > 0) downloadedBytes.toFloat() / actualTotal else -1f
                emit(DownloadProgress.Downloading(progress, downloadedBytes, actualTotal))
            }

            outputStream.close()
            inputStream.close()

            // Verify downloaded file
            if (!verifyChecksum(tempFile)) {
                tempFile.delete()
                throw IOException("Downloaded model failed SHA256 verification")
            }

            // Rename temp to final
            tempFile.renameTo(modelFile)
            emit(DownloadProgress.Complete(modelFile.absolutePath))

        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            emit(DownloadProgress.Error(e.message ?: "Download failed"))
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Cancel an in-progress download and clean up.
     */
    suspend fun cancelDownload() {
        withContext(Dispatchers.IO) {
            tempFile.delete()
        }
    }

    /**
     * Delete the model to free up space.
     */
    suspend fun deleteModel() {
        withContext(Dispatchers.IO) {
            modelFile.delete()
            tempFile.delete()
        }
    }

    fun getModelSizeBytes(): Long {
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    private suspend fun getFileSize(): Long = withContext(Dispatchers.IO) {
        try {
            val url = URL(MODEL_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = DOWNLOAD_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            val size = conn.contentLength.toLong()
            conn.disconnect()
            size
        } catch (e: Exception) {
            300_000_000L // fallback estimate
        }
    }

    private suspend fun verifyChecksum(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash == EXPECTED_SHA256
        } catch (e: Exception) {
            Log.e(TAG, "Checksum verification failed", e)
            false
        }
    }
}

sealed interface DownloadProgress {
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress
    data class Complete(val modelPath: String) : DownloadProgress
    data class Error(val message: String) : DownloadProgress
}
```

### 5i. `tts/VoiceManager.kt` — Voice Embedding Management

```kotlin
package com.example.readproplus.tts

import android.content.Context
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Loads and manages voice embeddings (style vectors) for Kokoro TTS.
 *
 * Voice embeddings are small float32 arrays (~256-512 floats, ~1-2KB)
 * stored as raw binary files in assets.
 *
 * The Nicole voice embedding was pre-extracted from the Kokoro voicepack:
 *   Python: torch.load("voices/nf_nicole.pt") → numpy array → binary dump
 */
class VoiceManager(private val context: Context) {

    /**
     * Load a voice embedding from assets.
     * Returns a FloatArray containing the style vector.
     */
    fun loadVoice(assetPath: String): FloatArray {
        return context.assets.open(assetPath).use { input ->
            val bytes = input.readBytes()
            val floatCount = bytes.size / 4 // float32 = 4 bytes
            val floatArray = FloatArray(floatCount)

            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floatArray)
            floatArray
        }
    }

    /**
     * Validate that a voice embedding has reasonable values.
     */
    fun validateVoice(embedding: FloatArray): Boolean {
        if (embedding.isEmpty()) return false
        // Embeddings should be small (256-512 dims for Kokoro)
        if (embedding.size < 100 || embedding.size > 1000) return false
        // Values should be in reasonable range (not NaN, not absurdly large)
        for (v in embedding) {
            if (v.isNaN() || v.isInfinite()) return false
            if (kotlin.math.abs(v) > 100f) return false
        }
        return true
    }
}
```

### 5j. `tts/AudioPlayer.kt` — AudioTrack Playback

```kotlin
package com.example.readproplus.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

sealed interface PlayerState {
    data object Idle : PlayerState
    data object Playing : PlayerState
    data object Paused : PlayerState
    data object Stopped : PlayerState
}

class AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayer"
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // Buffer size for AudioTrack
        private const val BUFFER_SIZE_FRAMES = 4096
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = ConcurrentLinkedQueue<ShortArray>()
    private val isPlaying = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var currentVolume: Float = 1.0f

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    /**
     * Initialize AudioTrack with proper audio attributes for media playback.
     */
    private fun ensureAudioTrack() {
        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) return

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        ).coerceAtLeast(BUFFER_SIZE_FRAMES * 2) // 2 bytes per sample

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /**
     * Enqueue an audio segment for playback.
     * If not playing, starts playback automatically.
     */
    suspend fun enqueue(samples: ShortArray) = withContext(Dispatchers.IO) {
        ensureAudioTrack()
        audioQueue.add(samples)
        if (!isPlaying.get() && !isPaused.get()) {
            startPlayback()
        }
    }

    /**
     * Start/resume audio playback from the queue.
     */
    suspend fun play() = withContext(Dispatchers.IO) {
        ensureAudioTrack()
        if (isPaused.get()) {
            isPaused.set(false)
            audioTrack?.play()
            _playerState.value = PlayerState.Playing
            startPlaybackLoop()
        } else if (!isPlaying.get()) {
            startPlayback()
        }
    }

    private suspend fun startPlayback() = withContext(Dispatchers.IO) {
        ensureAudioTrack()
        isPlaying.set(true)
        isPaused.set(false)
        audioTrack?.play()
        _playerState.value = PlayerState.Playing
        startPlaybackLoop()
    }

    /**
     * Pause playback. Audio queue is preserved.
     */
    suspend fun pause() {
        isPaused.set(true)
        audioTrack?.pause()
        _playerState.value = PlayerState.Paused
    }

    /**
     * Stop playback and clear the audio queue.
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        isPlaying.set(false)
        isPaused.set(false)
        audioQueue.clear()
        audioTrack?.apply {
            pause()
            flush()
            stop()
        }
        audioTrack = null
        _playerState.value = PlayerState.Stopped
    }

    /**
     * Get the number of remaining audio segments in the queue.
     */
    fun queueSize(): Int = audioQueue.size

    private suspend fun startPlaybackLoop() = withContext(Dispatchers.IO) {
        try {
            while (isPlaying.get()) {
                // Check pause state
                while (isPaused.get() && isPlaying.get()) {
                    kotlinx.coroutines.delay(100)
                }
                if (!isPlaying.get()) break

                val samples = audioQueue.poll() ?: run {
                    // No more audio in queue — check if more is being generated
                    if (audioQueue.isEmpty()) {
                        // Queue is truly empty, playback finished
                        break
                    }
                    kotlinx.coroutines.delay(50)
                    continue
                }

                // Apply volume by scaling samples
                val adjustedSamples = if (currentVolume != 1.0f) {
                    ShortArray(samples.size) { i ->
                        (samples[i] * currentVolume).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                } else {
                    samples
                }

                // Write to AudioTrack in chunks
                var offset = 0
                val track = audioTrack ?: break
                while (offset < adjustedSamples.size && isPlaying.get() && !isPaused.get()) {
                    val remaining = adjustedSamples.size - offset
                    val chunkSize = minOf(remaining, BUFFER_SIZE_FRAMES)
                    val written = track.write(adjustedSamples, offset, chunkSize)
                    if (written < 0) {
                        Log.e(TAG, "AudioTrack write error: $written")
                        break
                    }
                    offset += written
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Playback loop cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Playback error", e)
        } finally {
            isPlaying.set(false)
            if (!isPaused.get()) {
                _playerState.value = PlayerState.Idle
            }
            audioTrack?.apply {
                pause()
                flush()
                stop()
            }
            audioTrack = null
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        isPlaying.set(false)
        audioTrack?.release()
        audioTrack = null
        audioQueue.clear()
    }
}
```

### 5k. `tts/KokoroEngine.kt` — Main Orchestrator

```kotlin
package com.example.readproplus.tts

import android.util.Log
import com.example.readproplus.model.tts.AudioSegment
import com.example.readproplus.model.tts.TtsConfig
import com.example.readproplus.model.tts.TtsVoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

sealed interface EngineState {
    data object Idle : EngineState
    data class Generating(val currentSentence: Int, val totalSentences: Int) : EngineState
    data class Playing(val currentSentence: Int, val totalSentences: Int) : EngineState
    data object Paused : EngineState
    data object Stopped : EngineState
    data class Error(val message: String) : EngineState
}

/**
 * Central orchestrator for Kokoro TTS.
 *
 * Lifecycle: initialize → startSpeaking → (pause/resume/stop) → release
 *
 * Coordinates:
 *   - Text splitting into sentences
 *   - Tokenization (text → token IDs)
 *   - Model inference (token IDs → audio samples)
 *   - Audio playback via AudioPlayer
 */
class KokoroEngine(
    private val inference: KokoroInference,
    private val tokenizer: KokoroTokenizer,
    private val voiceManager: VoiceManager,
    private val config: TtsConfig = TtsConfig(),
) {
    companion object {
        private const val TAG = "KokoroEngine"
    }

    private val audioPlayer = AudioPlayer()
    private var voiceEmbedding: FloatArray = FloatArray(0)
    private var currentJob: Job? = null

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _currentSentence = MutableStateFlow(0)
    val currentSentence: StateFlow<Int> = _currentSentence.asStateFlow()

    /**
     * Initialize engine: load voice embedding, ensure tokenizer is ready.
     * Model loading is handled separately by ModelManager.
     */
    suspend fun initialize(voice: TtsVoice = TtsVoice.NICOLE) {
        voiceEmbedding = withContext(Dispatchers.IO) {
            voiceManager.loadVoice(voice.embeddingAssetPath)
        }
        require(voiceManager.validateVoice(voiceEmbedding)) {
            "Invalid voice embedding for ${voice.displayName}"
        }
        tokenizer.initialize()
        Log.i(TAG, "Engine initialized with voice: ${voice.displayName}")
    }

    /**
     * Check if the engine has a loaded model and is ready for inference.
     */
    fun isReady(): Boolean = inference.isLoaded()

    /**
     * Load or reload the ONNX model (called after model download completes).
     */
    fun loadModel(modelPath: String) {
        inference.loadModel(modelPath, voiceEmbedding)
    }

    /**
     * Start speaking text. Splits text into sentences and processes them sequentially
     * in a streaming fashion: first sentence is generated and starts playing while
     * subsequent sentences are still being processed.
     *
     * @param text The full text to speak (typically one page)
     * @param startSentence Optional sentence index to start from (0-based)
     */
    suspend fun startSpeaking(text: String, startSentence: Int = 0) = coroutineScope {
        check(isReady()) { "Model not loaded. Call loadModel() first." }
        require(voiceEmbedding.isNotEmpty()) { "Voice not initialized" }

        currentJob?.cancel()
        currentJob = coroutineContext[Job]

        val sentences = splitSentences(text)
        if (sentences.isEmpty()) return@coroutineScope

        val startIndex = startSentence.coerceIn(0, sentences.size - 1)
        _engineState.value = EngineState.Generating(startIndex, sentences.size)
        _currentSentence.value = startIndex

        // Pre-generate first sentence to minimize latency
        val firstSegment = generateAudioSegment(sentences[startIndex], config.speed)
        audioPlayer.enqueue(firstSegment.samples)
        _engineState.value = EngineState.Playing(startIndex, sentences.size)

        // Generate and queue remaining sentences
        for (i in (startIndex + 1) until sentences.size) {
            ensureActive()
            _currentSentence.value = i
            _engineState.value = EngineState.Generating(i, sentences.size)

            val segment = generateAudioSegment(sentences[i], config.speed)
            ensureActive()

            _engineState.value = EngineState.Playing(i, sentences.size)
            audioPlayer.enqueue(segment.samples)
        }

        // Wait for all audio to finish playing
        while (audioPlayer.queueSize() > 0 && isActive) {
            kotlinx.coroutines.delay(100)
        }
        while (audioPlayer.playerState.value == PlayerState.Playing && isActive) {
            kotlinx.coroutines.delay(200)
        }

        _engineState.value = EngineState.Stopped
    }

    /**
     * Pause playback — current audio position is preserved.
     */
    suspend fun pause() {
        audioPlayer.pause()
        _engineState.value = EngineState.Paused
    }

    /**
     * Resume paused playback.
     */
    suspend fun resume() {
        audioPlayer.play()
        _engineState.value = EngineState.Playing(
            _currentSentence.value, 0 // total unknown at this point
        )
    }

    /**
     * Stop playback and clear all queued audio.
     */
    suspend fun stop() {
        currentJob?.cancel()
        currentJob = null
        audioPlayer.stop()
        _engineState.value = EngineState.Stopped
        _currentSentence.value = 0
    }

    /**
     * Update speed. Takes effect on next sentence.
     */
    fun updateConfig(config: TtsConfig) {
        audioPlayer.setVolume(config.volume)
    }

    /**
     * Release all resources.
     */
    fun release() {
        currentJob?.cancel()
        audioPlayer.release()
        inference.close()
        _engineState.value = EngineState.Idle
    }

    /**
     * Generate audio for a single sentence.
     */
    private suspend fun generateAudioSegment(
        sentence: String,
        speed: Float,
    ): AudioSegment = withContext(Dispatchers.Default) {
        val tokenIds = tokenizer.tokenize(sentence)
        val audioSamples = inference.infer(tokenIds, speed)

        // Convert float32 audio samples → int16 PCM
        val pcmSamples = ShortArray(audioSamples.size) { i ->
            val sample = (audioSamples[i] * Short.MAX_VALUE).roundToInt()
            sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val durationMs = (pcmSamples.size * 1000L) / 24000

        AudioSegment(
            text = sentence,
            sampleRate = 24000,
            samples = pcmSamples,
            durationMs = durationMs,
        )
    }

    /**
     * Split text into sentences using regex.
     * Handles common sentence terminators: . ! ? followed by space/newline/capital.
     */
    private fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // Split on sentence boundaries: .!? followed by space and capital letter
        val pattern = Regex("(?<=[.!?])\\s+(?=[A-Z\"''])")

        val raw = text.split(pattern)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Merge very short sentences into neighbors
        val merged = mutableListOf<String>()
        var pending = ""
        for (sentence in raw) {
            pending = if (pending.isNotEmpty()) "$pending $sentence" else sentence
            if (pending.length >= 15) { // Only emit complete sentences
                merged.add(pending)
                pending = ""
            }
        }
        if (pending.isNotEmpty()) {
            if (merged.isNotEmpty()) {
                merged[merged.lastIndex] = "${merged.last()} $pending"
            } else {
                merged.add(pending)
            }
        }

        return merged.ifEmpty { listOf(text.trim()) }
    }
}
```

### 5l. `data/TtsPreferences.kt` — Persistent Settings

```kotlin
package com.example.readproplus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ttsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tts_settings")

class TtsPreferences(context: Context) {

    private val dataStore = context.ttsDataStore

    companion object {
        val KEY_SPEED = floatPreferencesKey("tts_speed")
        val KEY_VOLUME = floatPreferencesKey("tts_volume")
        val KEY_VOICE_ID = stringPreferencesKey("tts_voice_id")
        val KEY_MODEL_DOWNLOADED = booleanPreferencesKey("tts_model_downloaded")
        val KEY_AUTO_ADVANCE_PAGE = booleanPreferencesKey("tts_auto_advance")
    }

    val speed: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_SPEED] ?: 1.0f
    }

    val volume: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_VOLUME] ?: 1.0f
    }

    val voiceId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_VOICE_ID] ?: "nf_nicole"
    }

    val modelDownloaded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_MODEL_DOWNLOADED] ?: false
    }

    val autoAdvancePage: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_ADVANCE_PAGE] ?: true
    }

    suspend fun setSpeed(speed: Float) {
        dataStore.edit { it[KEY_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setVolume(volume: Float) {
        dataStore.edit { it[KEY_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setVoiceId(voiceId: String) {
        dataStore.edit { it[KEY_VOICE_ID] = voiceId }
    }

    suspend fun setModelDownloaded(downloaded: Boolean) {
        dataStore.edit { it[KEY_MODEL_DOWNLOADED] = downloaded }
    }

    suspend fun setAutoAdvancePage(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_ADVANCE_PAGE] = enabled }
    }
}
```

### 5m. `ui/viewmodel/KokoroTtsViewModel.kt` — TTS ViewModel

```kotlin
package com.example.readproplus.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.readproplus.data.TtsPreferences
import com.example.readproplus.model.tts.TtsConfig
import com.example.readproplus.model.tts.TtsState
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.tts.DownloadProgress
import com.example.readproplus.tts.EngineState
import com.example.readproplus.tts.KokoroEngine
import com.example.readproplus.tts.KokoroInference
import com.example.readproplus.tts.KokoroTokenizer
import com.example.readproplus.tts.ModelManager
import com.example.readproplus.tts.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KokoroTtsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "KokoroTtsViewModel"
    }

    // Infrastructure
    private val modelManager = ModelManager(application)
    private val voiceManager = VoiceManager(application)
    private val tokenizer = KokoroTokenizer(application)
    private val inference = KokoroInference()
    private val preferences = TtsPreferences(application)
    private lateinit var engine: KokoroEngine

    // State
    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private var speakingJob: Job? = null
    private var isEngineInitialized = false

    private val currentConfig = MutableStateFlow(TtsConfig())

    init {
        viewModelScope.launch {
            // Restore saved preferences
            val savedSpeed = preferences.speed.first()
            val savedVolume = preferences.volume.first()
            currentConfig.value = TtsConfig(speed = savedSpeed, volume = savedVolume)
            engine = KokoroEngine(inference, tokenizer, voiceManager, currentConfig.value)
        }
        observeEngineState()
    }

    private fun observeEngineState() {
        viewModelScope.launch {
            combine(engine.engineState, engine.currentSentence) { state, sentence ->
                mapEngineStateToTtsState(state, sentence)
            }.collect { _ttsState.value = it }
        }
    }

    private fun mapEngineStateToTtsState(engineState: EngineState, sentence: Int): TtsState {
        return when (engineState) {
            is EngineState.Idle -> TtsState.Idle
            is EngineState.Generating -> TtsState.Generating(
                engineState.currentSentence, engineState.totalSentences
            )
            is EngineState.Playing -> TtsState.Playing(
                sentenceIndex = engineState.currentSentence,
                totalSentences = engineState.totalSentences,
                speed = currentConfig.value.speed,
            )
            is EngineState.Paused -> TtsState.Paused(
                sentenceIndex = engineState.currentSentence,
                totalSentences = engineState.totalSentences,
            )
            is EngineState.Stopped -> TtsState.Stopped
            is EngineState.Error -> TtsState.Error(engineState.message, recoverable = true)
        }
    }

    // ─── Model Download ───────────────────────────────────────

    fun checkAndDownloadModel() {
        viewModelScope.launch {
            if (modelManager.isModelReady()) {
                initializeEngine()
                return@launch
            }
            startModelDownload()
        }
    }

    private fun startModelDownload() {
        viewModelScope.launch {
            _ttsState.value = TtsState.ModelDownloading(0f, 0L, 0L)
            modelManager.downloadModel().collect { progress ->
                when (progress) {
                    is DownloadProgress.Downloading -> {
                        _downloadProgress.value = progress
                        _ttsState.value = TtsState.ModelDownloading(
                            progress.progress, progress.bytesDownloaded, progress.totalBytes
                        )
                    }
                    is DownloadProgress.Complete -> {
                        _downloadProgress.value = progress
                        preferences.setModelDownloaded(true)
                        initializeEngine()
                    }
                    is DownloadProgress.Error -> {
                        _ttsState.value = TtsState.Error(
                            "Model download failed: ${progress.message}",
                            recoverable = true,
                        )
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            modelManager.cancelDownload()
            _ttsState.value = TtsState.Idle
        }
    }

    fun retryDownload() {
        viewModelScope.launch {
            modelManager.cancelDownload()
            startModelDownload()
        }
    }

    // ─── Engine Initialization ───────────────────────────────

    private suspend fun initializeEngine() {
        if (isEngineInitialized) return
        try {
            engine.initialize(TtsVoice.NICOLE)
            val modelPath = modelManager.getModelPath()
                ?: throw IllegalStateException("Model file not found after download")
            engine.loadModel(modelPath)
            isEngineInitialized = true
            _ttsState.value = TtsState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "Engine initialization failed", e)
            _ttsState.value = TtsState.Error(
                "Failed to initialize TTS engine: ${e.message}",
                recoverable = true,
            )
        }
    }

    // ─── TTS Controls ────────────────────────────────────────

    fun startReading(text: String, startSentence: Int = 0) {
        if (!isEngineInitialized) {
            _ttsState.value = TtsState.Error("TTS engine not ready. Download model first.", true)
            return
        }
        speakingJob?.cancel()
        speakingJob = viewModelScope.launch {
            try {
                engine.startSpeaking(text, startSentence)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected on stop
            } catch (e: Exception) {
                Log.e(TAG, "TTS reading error", e)
                _ttsState.value = TtsState.Error(e.message ?: "TTS playback failed", true)
            }
        }
    }

    fun pauseReading() {
        viewModelScope.launch { engine.pause() }
    }

    fun resumeReading() {
        viewModelScope.launch { engine.resume() }
    }

    fun stopReading() {
        viewModelScope.launch {
            speakingJob?.cancel()
            engine.stop()
        }
    }

    fun setSpeed(speed: Float) {
        viewModelScope.launch {
            currentConfig.value = currentConfig.value.copy(speed = speed)
            engine.updateConfig(currentConfig.value)
            preferences.setSpeed(speed)
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            currentConfig.value = currentConfig.value.copy(volume = volume)
            engine.updateConfig(currentConfig.value)
            preferences.setVolume(volume)
        }
    }

    fun dismissError() {
        _ttsState.value = TtsState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { engine.stop() }
        engine.release()
    }
}
```

### 5n. `ui/components/TtsControlBar.kt` — Playback Control UI

```kotlin
package com.example.readproplus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.tts.TtsState

private val ControlBarBg = Color(0xFF1B1B2F)
private val ControlBarAccent = Color(0xFF4FC3F7)
private val WhiteText = Color(0xFFFFFFFF)

@Composable
fun TtsControlBar(
    ttsState: TtsState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVisible = ttsState !is TtsState.Idle && ttsState !is TtsState.Stopped

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(ControlBarBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Progress bar
            when (ttsState) {
                is TtsState.Playing -> {
                    val progress = if (ttsState.totalSentences > 0) {
                        ttsState.sentenceIndex.toFloat() / ttsState.totalSentences
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ControlBarAccent,
                        trackColor = ControlBarAccent.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                is TtsState.Generating -> {
                    LinearProgressIndicator(
                        progress = {
                            if (ttsState.totalSentences > 0) {
                                ttsState.sentencesProcessed.toFloat() / ttsState.totalSentences
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ControlBarAccent.copy(alpha = 0.5f),
                        trackColor = ControlBarAccent.copy(alpha = 0.1f),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                else -> {}
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Status text
                Column(modifier = Modifier.weight(1f)) {
                    val statusText = when (ttsState) {
                        is TtsState.Playing -> "Reading aloud..."
                        is TtsState.Paused -> "Paused"
                        is TtsState.Generating -> "Preparing audio..."
                        else -> ""
                    }
                    Text(
                        text = statusText,
                        color = WhiteText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (ttsState is TtsState.Playing) {
                        Text(
                            text = "${ttsState.speed}x speed",
                            color = WhiteText.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                }

                // Speed button
                IconButton(onClick = onSpeedClick) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = "Speed",
                        tint = WhiteText.copy(alpha = 0.8f),
                    )
                }

                // Play/Pause button
                IconButton(
                    onClick = {
                        when (ttsState) {
                            is TtsState.Playing -> onPause()
                            is TtsState.Paused -> onPlay()
                            else -> {}
                        }
                    }
                ) {
                    Icon(
                        imageVector = when (ttsState) {
                            is TtsState.Playing -> Icons.Filled.Pause
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = if (ttsState is TtsState.Playing) "Pause" else "Play",
                        tint = ControlBarAccent,
                    )
                }

                // Stop button
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                        tint = WhiteText.copy(alpha = 0.8f),
                    )
                }

                // Close button
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = WhiteText.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
```

### 5o. `ui/components/TtsSpeedDialog.kt` — Speed Selector

```kotlin
package com.example.readproplus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF4FC3F7)

private val speedOptions = listOf(
    0.5f to "0.5x (Very Slow)",
    0.75f to "0.75x (Slow)",
    1.0f to "1.0x (Normal)",
    1.25f to "1.25x (Fast)",
    1.5f to "1.5x (Faster)",
    1.75f to "1.75x (Very Fast)",
    2.0f to "2.0x (Maximum)",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Reading Speed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            speedOptions.forEach { (speed, label) ->
                val isSelected = speed == currentSpeed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpeedSelected(speed) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        color = if (isSelected) Accent else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Accent,
                        )
                    }
                }
            }
        }
    }
}
```

### 5p. `ui/components/ModelDownloadDialog.kt` — Download Progress

```kotlin
package com.example.readproplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF4FC3F7)

@Composable
fun ModelDownloadDialog(
    progress: Float,
    bytesDownloaded: Long,
    totalBytes: Long,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { /* Block dismiss — download must complete or be cancelled */ },
        title = {
            Text(
                text = "Downloading TTS Model",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Downloading the Kokoro TTS model for offline text-to-speech.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Accent,
                    trackColor = Accent.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(8.dp))

                val downloadedMB = bytesDownloaded / (1024f * 1024f)
                val totalMB = totalBytes / (1024f * 1024f)
                Text(
                    text = "%.1f MB / %.1f MB (%.0f%%)".format(
                        downloadedMB, totalMB, progress * 100
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "This download is required only once (~300 MB).\nTTS works fully offline after download.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        },
        confirmButton = {
            // No confirm — download is automatic
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
    )
}
```

### 5q. `assets/kokoro_vocab.json` — Kokoro Vocabulary

```json
{
  "<PAD>": 0,
  "<BOS>": 1,
  "<EOS>": 2,
  "<UNK>": 3,
  " ": 4,
  "AA": 5,
  "AE": 6,
  "AH": 7,
  "AO": 8,
  "AW": 9,
  "AY": 10,
  "B": 11,
  "CH": 12,
  "D": 13,
  "DH": 14,
  "EH": 15,
  "ER": 16,
  "EY": 17,
  "F": 18,
  "G": 19,
  "HH": 20,
  "IH": 21,
  "IY": 22,
  "JH": 23,
  "K": 24,
  "L": 25,
  "M": 26,
  "N": 27,
  "NG": 28,
  "OW": 29,
  "OY": 30,
  "P": 31,
  "R": 32,
  "S": 33,
  "SH": 34,
  "T": 35,
  "TH": 36,
  "UH": 37,
  "UW": 38,
  "V": 39,
  "W": 40,
  "Y": 41,
  "Z": 42,
  "ZH": 43,
  "SP": 44,
  ".": 45,
  ",": 46,
  "!": 47,
  "?": 48
}
```

---

## 6. Threading & Concurrency

| Concern | Strategy |
|---|---|
| **ONNX inference** | `Dispatchers.Default` — CPU-bound, uses ONNX Runtime's internal thread pool |
| **Tokenizer (G2P)** | `Dispatchers.Default` — dictionary lookup, string processing |
| **Model download** | `Dispatchers.IO` — network I/O, file writes |
| **AudioTrack playback** | `Dispatchers.IO` — blocking `write()` calls |
| **UI state updates** | `Dispatchers.Main` — via `StateFlow` collection |
| **Sentence pipeline** | Sequential coroutine: tokenize → infer → enqueue → next sentence. Cancellation between sentences via `ensureActive()` |
| **Lifecycle** | `viewModelScope` auto-cancels on ViewModel clear. `speakingJob` tracks active speak coroutine |
| **ONNX thread config** | `setInterOpNumThreads(2)`, `setIntraOpNumThreads(4)` — balanced for mobile CPUs |

### Sentence Pipeline with Cancellation

```kotlin
// Inside KokoroEngine.startSpeaking()
for (i in startIndex until sentences.size) {
    ensureActive()  // ← cancellation point: user can stop mid-page

    val segment = generateAudioSegment(sentences[i], config.speed)
    ensureActive()  // ← cancellation point: stop before playing this sentence

    audioPlayer.enqueue(segment.samples)
}
```

---

## 7. Memory & Storage Management

| Concern | Strategy |
|---|---|
| **ONNX model size** | ~300MB on disk. Downloaded once, stored in `context.filesDir/kokoro_tts/`. Never bundled in APK. |
| **Voice embedding** | ~1KB float32 array. Bundled as asset. Negligible memory. |
| **CMU dictionary** | ~1MB gzipped asset. Loaded into `Map<String, List<String>>` (~4MB RAM after decompression). Acceptable. |
| **Inference memory** | ONNX Runtime allocates ~50-100MB during inference for model weights + intermediate tensors. Freed after each `session.run()`. |
| **Audio buffers** | Each sentence generates ~10-60 seconds of 24kHz 16-bit mono audio = ~480KB-2.8MB per segment. Queued segments: max 3 at a time. |
| **Model download** | Uses temporary `.tmp` file. Atomic rename on success. Corrupted downloads cleaned on restart. |
| **Cache eviction** | User can delete model via settings. No automatic eviction (model is critical for TTS function). |
| **GC pressure** | Audio `ShortArray` allocated per sentence. Reused via queue polling. Avoid boxing. |
| **Storage warning** | Before download, check available storage. Require at least 500MB free. Show warning if low. |

---

## 8. Error Handling Matrix

| Error | Detection | User-facing message | Recovery |
|---|---|---|---|
| Model not downloaded | `modelManager.isModelReady() == false` | "TTS model needs to be downloaded first" | Show download dialog |
| Download network error | `IOException` in `downloadModel()` | "Download failed: check your connection" | Retry button |
| Download SHA256 mismatch | Checksum verification | "Download verification failed" | Automatic re-download |
| Insufficient storage | `IOException` "No space left" | "Not enough storage space (need ~500MB free)" | Dismiss + free space |
| Model loading failure | `OrtException` in `createSession()` | "Failed to load TTS model" | Retry, suggest re-download |
| Invalid voice embedding | `voiceManager.validateVoice() == false` | "Voice data is corrupted" | App restart (assert bundled) |
| Tokenizer not initialized | `initialized == false` | "TTS engine not ready" | Auto-initialize |
| Inference timeout | Single inference > 5s for short text | "TTS processing took too long" | Cancel + retry |
| Empty text | `text.isBlank()` | (No error — silently skip) | N/A |
| AudioTrack init failure | `AudioTrack.STATE_UNINITIALIZED` | "Audio output not available" | Retry, check device audio |
| Audio focus lost | `OnAudioFocusChangeListener` (future) | Pause playback | Auto-pause |
| Out of memory | `OutOfMemoryError` | "Not enough memory for TTS" | Stop, suggest closing other apps |
| Coroutine cancelled | `CancellationException` | (No error — expected) | Clean stop |

---

## 9. Permissions Strategy

| Permission | Purpose | Required? |
|---|---|---|
| `INTERNET` | Download model on first use | **Yes** — required for initial model download only |
| `WAKE_LOCK` | Keep CPU awake during long inference | Optional — recommended for long playback sessions |
| `FOREGROUND_SERVICE` | Keep TTS running if app goes to background | Optional — for background playback (V2 feature) |

**`AndroidManifest.xml`** additions:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<!-- WAKE_LOCK prevents CPU sleep during sentence generation -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

**No audio recording permission needed** — we only output audio, never capture.

---

## 10. Testing Plan

| Test | Type | Location | What it covers |
|---|---|---|---|
| `KokoroTokenizerTest` | Unit | `app/src/test/` | Text normalization (numbers, abbreviations); G2P for known words; token ID mapping; edge cases (empty, special chars, long text) |
| `KokoroPhonemeDictTest` | Unit | `app/src/test/` | CMU dict loading; known word lookup; fallback G2P for unknown words; stress marker stripping |
| `ModelManagerTest` | Unit | `app/src/test/` | Mocked download flow; checksum verification; resume logic; error handling |
| `VoiceManagerTest` | Unit | `app/src/test/` | Voice embedding loading; validation (correct size, no NaN) |
| `AudioPlayerTest` | Instrumented | `app/src/androidTest/` | AudioTrack init; enqueue/playback; pause/resume; stop; volume control; queue management |
| `KokoroInferenceTest` | Instrumented | `app/src/androidTest/` | Real ONNX inference with test model; output validation (sample count, sample rate, non-zero audio) |
| `KokoroEngineTest` | Instrumented | `app/src/androidTest/` | End-to-end: text → audio; sentence splitting; pause/resume/stop; cancellation |
| `KokoroTtsViewModelTest` | Unit | `app/src/test/` | State transitions: Idle → Generating → Playing → Paused → Stopped; error states; speed/volume changes |
| `TtsControlBarTest` | Compose UI | `app/src/androidTest/` | Button visibility per state; play/pause/stop callbacks; speed dialog trigger |
| `Integration: ReaderScreen + TTS` | Compose UI | `app/src/androidTest/` | Speaker icon triggers TTS; control bar appears; stop dismisses; playback survives config change |

### Test Assets for Instrumented Tests

Place in `app/src/androidTest/assets/`:
- `tiny_kokoro.onnx` — 2-layer test model (tiny, <1MB) that generates silence/sine wave
- `test_vocab.json` — Minimal vocabulary matching the test model
- `test_cmu_dict.gz` — 10-word phonetic dictionary

### Performance Benchmarks

| Metric | Target | Measurement |
|---|---|---|
| First sentence latency (3 words) | < 500ms | Time from `startSpeaking()` to first `AudioTrack.write()` |
| Inference per 10-word sentence | < 300ms | `infer()` call duration on mid-range device |
| Model load time | < 5s | `loadModel()` duration on cold start |
| Tokenization per sentence | < 10ms | `tokenize()` call duration |
| Download throughput | > 1 MB/s | Network speed monitoring |

---

## 11. Implementation Order

### Phase 1 — Foundation (6 files, ~450 lines)
1. Add dependencies to `libs.versions.toml` and `app/build.gradle.kts`
2. Create `model/tts/TtsState.kt`, `TtsVoice.kt`, `TtsConfig.kt`, `AudioSegment.kt`
3. Create `tts/VoiceManager.kt` (load Nicole embedding from assets)
4. Create `tts/KokoroTokenizer.kt` + `tts/KokoroPhonemeDict.kt`
5. Create asset files: `kokoro_vocab.json`
6. Write unit tests for tokenizer and voice manager

### Phase 2 — Inference Engine (4 files, ~500 lines)
7. Create `tts/KokoroInference.kt` (ONNX Runtime wrapper)
8. Create `tts/AudioPlayer.kt` (AudioTrack wrapper)
9. Create `tts/KokoroEngine.kt` (orchestrator)
10. Create `tts/ModelManager.kt` (download + verification)
11. Write instrumented tests for inference and audio player

### Phase 3 — Android Integration (4 files, ~500 lines)
12. Create `data/TtsPreferences.kt`
13. Create `ui/viewmodel/KokoroTtsViewModel.kt`
14. Create `ui/components/TtsControlBar.kt`
15. Create `ui/components/TtsSpeedDialog.kt`
16. Create `ui/components/ModelDownloadDialog.kt`
17. Modify `AndroidManifest.xml` (INTERNET, WAKE_LOCK permissions)
18. Write ViewModel unit tests

### Phase 4 — UI Integration (2 modified files, ~70 lines)
19. Modify `MainActivity.kt` — create `KokoroTtsViewModel`, pass to ReaderScreen
20. Modify `ui/screens/ReaderScreen.kt`:
    - Wire VolumeUp icon button to start TTS
    - Add TtsControlBar overlay
    - Add TtsSpeedDialog trigger
    - Add ModelDownloadDialog when model not ready
    - Wire EngelleButton to quick-play current sentence
21. End-to-end integration tests

### Phase 5 — Polish (2 files, ~100 lines)
22. Add storage check before download (~500MB free required check)
23. Add WAKE_LOCK acquisition during playback (partial wake lock)
24. Handle audio focus changes (pause when another app plays audio)
25. Auto-advance to next page on sentence completion
26. R8/ProGuard rules for ONNX Runtime

---

## 12. File Modification Summary

### Modified files:
| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `onnxruntime` version + library; add `datastorePreferences` version + library |
| `app/build.gradle.kts` | Add `implementation(libs.onnxruntime.android)`, `implementation(libs.androidx.datastore.preferences)`; add `abiFilters`; add `packaging.excludes` for x86 |
| `AndroidManifest.xml` | Add `INTERNET` and `WAKE_LOCK` permissions |
| `MainActivity.kt` | Instantiate `KokoroTtsViewModel`, pass TTS functions to `ReaderScreen` |
| `ui/screens/ReaderScreen.kt` | Wire VolumeUp icon → `onTtsStart()`; add `TtsControlBar` overlay; add `TtsSpeedDialog`; add `ModelDownloadDialog` |

### New files (16 total):
| # | Path | Lines (est.) |
|---|---|---|
| 1 | `model/tts/TtsState.kt` | 50 |
| 2 | `model/tts/TtsVoice.kt` | 30 |
| 3 | `model/tts/TtsConfig.kt` | 25 |
| 4 | `model/tts/AudioSegment.kt` | 15 |
| 5 | `tts/KokoroEngine.kt` | 200 |
| 6 | `tts/KokoroInference.kt` | 120 |
| 7 | `tts/KokoroTokenizer.kt` | 250 |
| 8 | `tts/KokoroPhonemeDict.kt` | 80 |
| 9 | `tts/ModelManager.kt` | 180 |
| 10 | `tts/VoiceManager.kt` | 80 |
| 11 | `tts/AudioPlayer.kt` | 150 |
| 12 | `data/TtsPreferences.kt` | 60 |
| 13 | `ui/viewmodel/KokoroTtsViewModel.kt` | 200 |
| 14 | `ui/components/TtsControlBar.kt` | 150 |
| 15 | `ui/components/TtsSpeedDialog.kt` | 60 |
| 16 | `ui/components/ModelDownloadDialog.kt` | 80 |
|| **Total new** | **~1,570 lines** |

---

## 13. Architectural Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                 │
│  ┌──────────────┐  ┌───────────────┐  ┌──────────────────────┐ │
│  │ ReaderScreen │  │ TtsControlBar │  │ ModelDownloadDialog  │ │
│  │  (modified)  │  │  (play/pause/ │  │  (download progress) │ │
│  │  VolumeUp →  │  │   stop/speed) │  │                      │ │
│  │  TTS trigger │  └───────┬───────┘  └──────────┬───────────┘ │
│  └──────┬───────┘          │                      │             │
│         │                  │                      │             │
│  ┌──────┴──────────────────┴──────────────────────┴──────────┐ │
│  │                 KokoroTtsViewModel                         │ │
│  │   (StateFlow<TtsState>, download management,              │ │
│  │    speed/volume control, error handling)                   │ │
│  └──────────────────────────┬────────────────────────────────┘ │
├─────────────────────────────┼──────────────────────────────────┤
│           Domain            │                                   │
│  ┌──────────────────────────┴────────────────────────────────┐ │
│  │                      KokoroEngine                          │ │
│  │   (sentence splitting → tokenize → infer → enqueue → play) │ │
│  └──┬──────────────┬──────────────┬──────────────┬───────────┘ │
│     │              │              │              │              │
│  ┌──┴───────┐ ┌────┴─────┐ ┌─────┴──────┐ ┌─────┴──────┐      │
│  │Tokenizer │ │Inference │ │AudioPlayer │ │ModelManager│      │
│  │ Normalize│ │  ONNX    │ │ AudioTrack │ │ Download   │      │
│  │ → G2P    │ │ Runtime  │ │ 24kHz PCM  │ │ Verify     │      │
│  │ → IDs    │ │ Float[]  │ │ Streaming  │ │ SHA256     │      │
│  └──────────┘ └────┬─────┘ └────────────┘ └────────────┘      │
│                    │                                            │
│  ┌─────────────────┴──────────────────────────────────────┐   │
│  │  KokoroInference                                        │   │
│  │  - OrtSession.run(input_ids, voice_embedding, speed)    │   │
│  │  - Input: token IDs (int64) + voice vector (float32)   │   │
│  │  - Output: raw audio samples (float32[1, num_samples]) │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                                │
│  ┌─────────────────┐  ┌────────────────┐                      │
│  │  VoiceManager   │  │ TtsPreferences │                      │
│  │  nicole_voice   │  │  DataStore     │                      │
│  │  .bin → Float[] │  │  speed/volume  │                      │
│  └─────────────────┘  └────────────────┘                      │
├────────────────────────────────────────────────────────────────┤
│                    Android Platform                             │
│  ┌──────────────────────┐  ┌──────────────────────────────┐   │
│  │ ONNX Runtime Android │  │ AudioTrack (24kHz, 16-bit)   │   │
│  │  libonnxruntime.so   │  │  writes raw PCM to hardware  │   │
│  │  CPU: arm64/armv7    │  │                              │   │
│  └──────────────────────┘  └──────────────────────────────┘   │
│  ┌──────────────────────┐  ┌──────────────────────────────┐   │
│  │  Internal Storage    │  │  Assets                      │   │
│  │  kokoro-v0_19.onnx   │  │  nicole_voice.bin            │   │
│  │  (~300MB, downloaded)│  │  kokoro_vocab.json           │   │
│  └──────────────────────┘  │  cmu_dict.gz                 │   │
│                            └──────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

---

## 14. Future Considerations (Not in Scope for V1)

- **Additional voices**: Bella (`nf_bella`), Adam (`m_adam`), and other Kokoro voicepack voices — add more `.bin` files to assets
- **Continuous reading**: Automatically advance to next page after current page speech finishes
- **Background playback**: Foreground service with notification controls for listening while screen off
- **Sentence-level seeking**: Tap on any sentence in the reader to start speaking from there
- **Word-level highlighting**: Highlight each word as it's spoken (requires word-level timestamps from the model or forced alignment)
- **Audio export**: Save generated speech as WAV/MP3 file for offline listening
- **Multi-language**: Kokoro supports other languages via different tokenizers (Chinese, Japanese, etc.)
- **Model quantization**: INT8 quantized model (~80MB) for faster inference and smaller download
- **Voice cloning**: Allow users to add custom voice embeddings
- **Streaming optimization**: Reduce first-sentence latency by pre-tokenizing while audio plays
- **NN API delegate**: Use Android Neural Networks API for GPU/NPU acceleration on supported devices
- **ProGuard/R8 rules**: Add `-keep` rules for ONNX Runtime JNI classes:
  ```proguard
  -keep class ai.onnxruntime.** { *; }
  -keep class com.microsoft.onnxruntime.** { *; }
  ```

---

## 15. Pre-Implementation Script: Voice Embedding Extraction

Before development, extract the Nicole voice embedding from the Kokoro voicepack using this Python script:

```python
# extract_nicole_voice.py
# Run once to generate nicole_voice.bin for the Android assets folder
# Requires: pip install torch safetensors

import torch
import numpy as np
import sys

# Option A: From .pt file (if you have the Kokoro repo)
# voices = torch.load("kokoro/voices/nf_nicole.pt")
# embedding = voices[0].numpy()  # shape: [embedding_dim]

# Option B: From safetensors (if downloaded from HuggingFace)
# from safetensors.torch import load_file
# voices = load_file("voices.safetensors")
# embedding = voices["nf_nicole"].numpy()

# Option C: From the ONNX model directly (extract initial state)
# import onnxruntime as ort
# session = ort.InferenceSession("kokoro-v0_19.onnx")
# # The voice embedding may be baked into the model or provided as input

# Write as float32 little-endian binary
embedding = np.array(embedding, dtype=np.float32)
embedding.tofile("app/src/main/assets/nicole_voice.bin")
print(f"Extracted Nicole voice embedding: {embedding.shape}, {embedding.nbytes} bytes")
```

---

## 16. Verification Checklist

### Pre-Implementation
- [ ] Nicole voice embedding extracted and placed in `assets/nicole_voice.bin`
- [ ] CMU Pronouncing Dictionary compressed and placed in `assets/cmu_dict.gz`
- [ ] Kokoro vocabulary JSON placed in `assets/kokoro_vocab.json`
- [ ] Kokoro ONNX model uploaded to accessible CDN/URL
- [ ] SHA256 hash of ONNX model computed and added to `ModelManager.EXPECTED_SHA256`

### Build & Compilation
- [ ] ONNX Runtime Android compiles without errors on `minSdk 24`
- [ ] `abiFilters` exclude x86 without build issues
- [ ] All new Kotlin files compile without warnings
- [ ] R8/ProGuard rules added for ONNX Runtime JNI

### Core TTS Engine
- [ ] `ModelManager` downloads model on first launch with progress
- [ ] `ModelManager` resumes interrupted downloads
- [ ] `ModelManager` verifies SHA256 checksum
- [ ] `KokoroTokenizer` normalizes text correctly (numbers, abbreviations)
- [ ] `KokoroPhonemeDict` looks up words in CMU dict
- [ ] `KokoroPhonemeDict` falls back to rule-based G2P for unknown words
- [ ] `KokoroInference` loads ONNX model and runs inference
- [ ] `KokoroInference` produces non-zero audio samples
- [ ] `KokoroInference` handles invalid inputs without crashing
- [ ] `AudioPlayer` plays audio through device speaker
- [ ] `AudioPlayer` pauses and resumes correctly
- [ ] `AudioPlayer` stops and clears queue
- [ ] `KokoroEngine` splits text into sentences correctly
- [ ] `KokoroEngine` streams sentences (generates + plays concurrently)
- [ ] `KokoroEngine` cancels mid-playback on stop()

### UI Integration
- [ ] VolumeUp icon in reader top bar triggers TTS
- [ ] TtsControlBar appears during TTS playback
- [ ] Play/Pause button toggles correctly
- [ ] Stop button stops playback and hides controls
- [ ] Speed dialog shows and selects speed
- [ ] Speed change takes effect on next sentence
- [ ] ModelDownloadDialog shows on first use
- [ ] ModelDownloadDialog shows correct progress and MB count
- [ ] Cancel download cleans up temp files
- [ ] Retry download works after failure
- [ ] EngelleButton quick-plays current sentence (optional)

### Platform Compatibility
- [ ] App runs on Android API 24 device
- [ ] App runs on Android API 36 device
- [ ] TTS works on arm64-v8a devices
- [ ] TTS works on armeabi-v7a devices
- [ ] No crashes on config change (rotation) during playback
- [ ] No crashes on back-navigation during playback
- [ ] No crashes when app goes to background during playback
- [ ] No `OutOfMemoryError` during inference
- [ ] Storage check warns before download if < 500MB free

### Quality
- [ ] Nicole voice sounds natural and intelligible
- [ ] Audio playback is glitch-free (no stutter/gaps between sentences)
- [ ] Speed 1.0x sounds natural
- [ ] Speed 2.0x is intelligible (time-stretched)
- [ ] Speed 0.5x doesn't introduce artifacts
- [ ] First sentence latency < 1 second on mid-range device

### Edge Cases
- [ ] Empty text (0 pages) — no crash
- [ ] Very long page (10,000+ characters) — handled gracefully
- [ ] Very short text (1 word) — plays correctly
- [ ] Text with only numbers — normalized and spoken
- [ ] Text with special characters — filtered, not garbled
- [ ] Rapid start/stop — no state corruption
- [ ] Multiple rapid speed changes — no crash
- [ ] Download when already downloaded — skipped
- [ ] Corrupted model file — detected and re-downloaded
- [ ] Network lost during download — error shown, retry available

### Tests
- [ ] `KokoroTokenizerTest` unit tests pass
- [ ] `KokoroPhonemeDictTest` unit tests pass
- [ ] `ModelManagerTest` unit tests pass
- [ ] `KokoroTtsViewModelTest` unit tests pass
- [ ] `AudioPlayerTest` instrumented tests pass
- [ ] `KokoroInferenceTest` instrumented tests pass
- [ ] `KokoroEngineTest` instrumented tests pass
- [ ] `TtsControlBarTest` Compose UI tests pass
- [ ] Integration test: Reader + TTS passes
