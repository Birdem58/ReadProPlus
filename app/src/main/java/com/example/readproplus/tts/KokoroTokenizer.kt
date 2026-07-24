package com.example.readproplus.tts

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Converts reader text into the padded token sequence required by Kokoro v1.0 ONNX. */
class KokoroTokenizer(private val context: Context) {

    private var vocabulary: Map<String, Int> = emptyMap()
    private var phonemeDict: KokoroPhonemeDict? = null
    private var initialized = false

    /** Kokoro permits 510 content symbols plus a pad token at each edge. */
    val maxTokens = MAX_SEQUENCE_LENGTH

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        vocabulary = loadVocabulary()
        phonemeDict = KokoroPhonemeDict(context).also { it.initialize() }
        initialized = true
    }

    fun hasCompatibleVocabulary(): Boolean = runCatching {
        loadVocabulary().containsKey(PAD_TOKEN)
    }.getOrDefault(false)

    fun tokenize(text: String): IntArray {
        check(initialized) { "Tokenizer not initialized. Call initialize() first." }
        val normalized = normalizeText(text)
        val phonemes = phonemeDict!!.textToPhonemes(normalized)
        return phonemesToTokenIds(phonemes)
    }

    private fun loadVocabulary(): Map<String, Int> {
        val json = context.assets.open(VOCABULARY_ASSET)
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(json)
        val entries = if (root.has("model")) {
            root.getJSONObject("model").getJSONObject("vocab")
        } else {
            root
        }

        return buildMap {
            entries.keys().forEach { token -> put(token, entries.getInt(token)) }
        }.also {
            require(it[PAD_TOKEN] == PAD_ID) {
                "The bundled Kokoro tokenizer is incompatible with the ONNX model."
            }
        }
    }

    private fun normalizeText(text: String): String {
        var result = text.trim()
        result = result.replace(Regex("\\bMr\\."), "Mister")
        result = result.replace(Regex("\\bMrs\\."), "Misses")
        result = result.replace(Regex("\\bDr\\."), "Doctor")
        result = result.replace(Regex("\\bSt\\."), "Saint")
        result = result.replace(Regex("\\bAve\\."), "Avenue")
        result = result.replace(Regex("\\bvs\\."), "versus")
        result = result.replace(Regex("\\betc\\."), "et cetera")
        result = result.replace(Regex("\\be\\.g\\."), "for example")
        result = result.replace(Regex("\\bi\\.e\\."), "that is")

        result = result.replace("$", " dollars ")
        result = result.replace("\u20ac", " euros ")
        result = result.replace("\u00a3", " pounds ")
        result = result.replace("%", " percent ")
        result = result.replace(Regex("\\b(\\d{1,4})\\b")) { match ->
            match.groupValues[1].toIntOrNull()?.takeIf { it in 0..9999 }
                ?.let(::numberToWords)
                ?: match.value
        }

        return result
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9 .,!?;:'\"()\u2014\u2026-]"), "")
            .trim()
    }

    private fun numberToWords(number: Int): String {
        if (number == 0) return "zero"
        val ones = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
            "eighteen", "nineteen",
        )
        val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
        var remaining = number
        val parts = mutableListOf<String>()
        if (remaining >= 1000) {
            parts += "${ones[remaining / 1000]} thousand"
            remaining %= 1000
        }
        if (remaining >= 100) {
            parts += "${ones[remaining / 100]} hundred"
            remaining %= 100
        }
        if (remaining in 1..19) {
            parts += ones[remaining]
        } else if (remaining >= 20) {
            val unit = remaining % 10
            parts += if (unit == 0) tens[remaining / 10] else "${tens[remaining / 10]} ${ones[unit]}"
        }
        return parts.joinToString(" ")
    }

    private fun phonemesToTokenIds(phonemes: String): IntArray {
        val padId = vocabulary.getValue(PAD_TOKEN)
        val tokenIds = ArrayList<Int>(phonemes.length + 2)
        tokenIds += padId
        phonemes.forEach { symbol -> vocabulary[symbol.toString()]?.let(tokenIds::add) }
        tokenIds += padId

        if (tokenIds.size == 2) {
            vocabulary["\u0259"]?.let(tokenIds::add)
            tokenIds.add(padId)
        }

        if (tokenIds.size <= maxTokens) return tokenIds.toIntArray()

        return tokenIds.take(maxTokens - 1)
            .plus(padId)
            .toIntArray()
    }

    private companion object {
        const val VOCABULARY_ASSET = "kokoro_vocab.json"
        const val PAD_TOKEN = "$"
        const val PAD_ID = 0
        const val MAX_SEQUENCE_LENGTH = 512
    }
}
