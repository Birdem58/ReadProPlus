package com.example.readproplus.tts

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * Converts English text to the IPA-like symbols expected by the Kokoro tokenizer.
 *
 * The Kokoro ONNX model does not accept ARPABET tokens directly.  CMUdict provides
 * reliable ARPABET pronunciations, which are converted here before tokenization.
 */
class KokoroPhonemeDict(private val context: Context) {

    private val dictionary = mutableMapOf<String, List<String>>()
    private var initialized = false

    suspend fun initialize() {
        if (initialized) return
        loadDictionary()
        initialized = true
    }

    fun textToPhonemes(text: String): String {
        check(initialized) { "Phoneme dictionary is not initialized." }

        val result = StringBuilder()
        text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .forEach { token ->
                val (word, punctuation) = splitWordAndPunctuation(token)
                if (word.isNotBlank()) {
                    if (result.isNotEmpty() && result.last() !in sentencePunctuation) {
                        result.append(' ')
                    }
                    val pronunciation = dictionary[word.lowercase()]
                    result.append(
                        if (pronunciation != null) {
                            arpabetToIpa(pronunciation)
                        } else {
                            fallbackG2P(word)
                        },
                    )
                }
                result.append(punctuation.filter { it in supportedPunctuation })
            }

        return result.toString().trim()
    }

    private fun loadDictionary() {
        val assetNames = context.assets.list("")?.toSet().orEmpty()
        if (FULL_DICTIONARY_ASSET in assetNames) {
            context.assets.open(FULL_DICTIONARY_ASSET).bufferedReader().use(::readDictionary)
            return
        }

        context.assets.open(LEGACY_DICTIONARY_ASSET).use { input ->
            GZIPInputStream(input).use { gzip ->
                BufferedReader(InputStreamReader(gzip)).use(::readDictionary)
            }
        }
    }

    private fun readDictionary(reader: BufferedReader) {
        reader.forEachLine { line ->
            if (line.isBlank() || line.startsWith(";;;")) return@forEachLine

            val parts = line.trim().split(Regex("\\s+"), limit = 2)
            if (parts.size != 2) return@forEachLine

            val word = parts[0]
                .replace(Regex("\\(\\d+\\)$"), "")
                .lowercase()
            val phonemes = parts[1]
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            if (phonemes.isNotEmpty()) {
                // Keep the first pronunciation listed by CMUdict as its canonical one.
                dictionary.putIfAbsent(word, phonemes)
            }
        }
    }

    private fun splitWordAndPunctuation(token: String): Pair<String, String> {
        val trimmed = token.trim()
        val trailing = trimmed.takeLastWhile { it in supportedPunctuation || it == '"' || it == ')' }
        val word = trimmed
            .dropLast(trailing.length)
            .trimStart('"', '(', '\u201c')
            .trimEnd('"', ')')
        return word to trailing
    }

    private fun arpabetToIpa(phonemes: List<String>): String = buildString {
        phonemes.forEach { phoneme ->
            val stress = phoneme.lastOrNull()?.digitToIntOrNull()
            val base = if (stress != null) phoneme.dropLast(1) else phoneme
            if (stress == 1) append(PRIMARY_STRESS)
            if (stress == 2) append(SECONDARY_STRESS)

            append(
                when (base) {
                    "AA" -> "\u0251"
                    "AE" -> "\u00e6"
                    "AH" -> if (stress == 0) "\u0259" else "\u028c"
                    "AO" -> "\u0254"
                    "AW" -> "a\u028a"
                    "AY" -> "a\u026a"
                    "EH" -> "\u025b"
                    "ER" -> if (stress == 0) "\u025a" else "\u025c\u0279"
                    "EY" -> "e\u026a"
                    "IH" -> "\u026a"
                    "IY" -> "i"
                    "OW" -> "o\u028a"
                    "OY" -> "\u0254\u026a"
                    "UH" -> "\u028a"
                    "UW" -> "u"
                    "AX" -> "\u0259"
                    "AXR" -> "\u025a"
                    "IX" -> "\u0268"
                    "EL" -> "\u0259l"
                    "EM" -> "\u0259m"
                    "EN" -> "\u0259n"
                    "NX", "DX" -> "\u027e"
                    "Q" -> "\u0294"
                    "B" -> "b"
                    "CH" -> "t\u0283"
                    "D" -> "d"
                    "DH" -> "\u00f0"
                    "F" -> "f"
                    "G" -> "\u0261"
                    "HH" -> "h"
                    "JH" -> "d\u0292"
                    "K" -> "k"
                    "L" -> "l"
                    "M" -> "m"
                    "N" -> "n"
                    "NG" -> "\u014b"
                    "P" -> "p"
                    "R" -> "\u0279"
                    "S" -> "s"
                    "SH" -> "\u0283"
                    "T" -> "t"
                    "TH" -> "\u03b8"
                    "V" -> "v"
                    "W" -> "w"
                    "Y" -> "j"
                    "Z" -> "z"
                    "ZH" -> "\u0292"
                    else -> ""
                },
            )
        }
    }

    /** A small pronunciation fallback for names and uncommon words absent from CMUdict. */
    private fun fallbackG2P(word: String): String {
        var value = word.lowercase()
            .replace("tion", "\u0283\u0259n")
            .replace("sion", "\u0292\u0259n")
            .replace("tch", "t\u0283")
            .replace("ch", "t\u0283")
            .replace("sh", "\u0283")
            .replace("th", "\u03b8")
            .replace("ng", "\u014b")
            .replace("ph", "f")
            .replace("qu", "kw")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("ai", "e\u026a")
            .replace("ay", "e\u026a")
            .replace("ow", "o\u028a")

        val letterMap = mapOf(
            'a' to "\u00e6", 'b' to "b", 'c' to "k", 'd' to "d", 'e' to "\u025b",
            'f' to "f", 'g' to "\u0261", 'h' to "h", 'i' to "\u026a", 'j' to "d\u0292",
            'k' to "k", 'l' to "l", 'm' to "m", 'n' to "n", 'o' to "o",
            'p' to "p", 'r' to "\u0279", 's' to "s", 't' to "t", 'u' to "\u028c",
            'v' to "v", 'w' to "w", 'x' to "ks", 'y' to "j", 'z' to "z",
        )
        return buildString {
            value.forEach { character -> letterMap[character]?.let(::append) }
        }
    }

    private companion object {
        const val FULL_DICTIONARY_ASSET = "cmudict.dict"
        const val LEGACY_DICTIONARY_ASSET = "cmu_dict.gz"
        const val PRIMARY_STRESS = '\u02c8'
        const val SECONDARY_STRESS = '\u02cc'
        const val supportedPunctuation = ".,!?;:\u2014\u2026"
        const val sentencePunctuation = ".,!?;:\u2014\u2026"
    }
}
