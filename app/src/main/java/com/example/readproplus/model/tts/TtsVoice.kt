package com.example.readproplus.model.tts

enum class TtsBackend {
    KOKORO,
    PIPER,
}

/** A selectable voice pack for either the bundled Kokoro or Piper runtime. */
data class TtsVoice(
    val id: String,
    val displayName: String,
    val locale: String,
    val accent: String,
    val gender: String,
    val bundledAssetPath: String? = null,
    val isDefault: Boolean = false,
    val backend: TtsBackend = TtsBackend.KOKORO,
    val piperAssetDirectory: String? = null,
    val piperModelFileName: String? = null,
    val piperArchiveFileName: String? = null,
) {
    val remoteFileName: String
        get() = piperArchiveFileName ?: "$id.bin"

    val details: String
        get() = "$accent • $gender"

    val engineLabel: String
        get() = backend.name.lowercase().replaceFirstChar(Char::uppercase)

    companion object {
        val NICOLE = TtsVoice(
            id = "af_nicole",
            displayName = "Nicole",
            locale = "en-US",
            accent = "American",
            gender = "Female",
            bundledAssetPath = "nicole_voice.bin",
            isDefault = true,
        )

        // DFKI is the female Turkish voice. Fahrettin is the bundled male
        // voice; Fettah remains available through the optional download row.
        val PIPER_DFKI = TtsVoice(
            id = "piper_tr_TR_dfki_medium",
            displayName = "DFKI",
            locale = "tr-TR",
            accent = "Turkish",
            gender = "Female",
            backend = TtsBackend.PIPER,
            piperAssetDirectory = "piper/vits-piper-tr_TR-dfki-medium",
            piperModelFileName = "tr_TR-dfki-medium.onnx",
            piperArchiveFileName = "vits-piper-tr_TR-dfki-medium.tar.bz2",
        )

        val PIPER_FAHRRETTIN = TtsVoice(
            id = "piper_tr_TR_fahrettin_medium",
            displayName = "Fahrettin",
            locale = "tr-TR",
            accent = "Turkish",
            gender = "Male",
            backend = TtsBackend.PIPER,
            piperAssetDirectory = "piper/vits-piper-tr_TR-fahrettin-medium",
            piperModelFileName = "tr_TR-fahrettin-medium.onnx",
            piperArchiveFileName = "vits-piper-tr_TR-fahrettin-medium.tar.bz2",
        )

        val PIPER_FETTAH = TtsVoice(
            id = "piper_tr_TR_fettah_medium",
            displayName = "Fettah",
            locale = "tr-TR",
            accent = "Turkish",
            gender = "Male",
            backend = TtsBackend.PIPER,
            piperModelFileName = "tr_TR-fettah-medium.onnx",
            piperArchiveFileName = "vits-piper-tr_TR-fettah-medium.tar.bz2",
        )

        private val KOKORO = listOf(
            TtsVoice("af_heart", "Heart", "en-US", "American", "Female"),
            TtsVoice("af_alloy", "Alloy", "en-US", "American", "Female"),
            TtsVoice("af_aoede", "Aoede", "en-US", "American", "Female"),
            TtsVoice("af_bella", "Bella", "en-US", "American", "Female"),
            TtsVoice("af_jessica", "Jessica", "en-US", "American", "Female"),
            TtsVoice("af_kore", "Kore", "en-US", "American", "Female"),
            NICOLE,
            TtsVoice("af_nova", "Nova", "en-US", "American", "Female"),
            TtsVoice("af_river", "River", "en-US", "American", "Female"),
            TtsVoice("af_sarah", "Sarah", "en-US", "American", "Female"),
            TtsVoice("af_sky", "Sky", "en-US", "American", "Female"),
            TtsVoice("am_adam", "Adam", "en-US", "American", "Male"),
            TtsVoice("am_echo", "Echo", "en-US", "American", "Male"),
            TtsVoice("am_eric", "Eric", "en-US", "American", "Male"),
            TtsVoice("am_fenrir", "Fenrir", "en-US", "American", "Male"),
            TtsVoice("am_liam", "Liam", "en-US", "American", "Male"),
            TtsVoice("am_michael", "Michael", "en-US", "American", "Male"),
            TtsVoice("am_onyx", "Onyx", "en-US", "American", "Male"),
            TtsVoice("am_puck", "Puck", "en-US", "American", "Male"),
            TtsVoice("am_santa", "Santa", "en-US", "American", "Male"),
            TtsVoice("bf_alice", "Alice", "en-GB", "British", "Female"),
            TtsVoice("bf_emma", "Emma", "en-GB", "British", "Female"),
            TtsVoice("bf_isabella", "Isabella", "en-GB", "British", "Female"),
            TtsVoice("bf_lily", "Lily", "en-GB", "British", "Female"),
            TtsVoice("bm_daniel", "Daniel", "en-GB", "British", "Male"),
            TtsVoice("bm_fable", "Fable", "en-GB", "British", "Male"),
            TtsVoice("bm_george", "George", "en-GB", "British", "Male"),
            TtsVoice("bm_lewis", "Lewis", "en-GB", "British", "Male"),
        )

        val PIPER = listOf(PIPER_DFKI, PIPER_FAHRRETTIN, PIPER_FETTAH)

        val ALL = KOKORO + PIPER

        fun fromId(id: String?): TtsVoice = ALL.firstOrNull { it.id == id } ?: NICOLE
    }
}
