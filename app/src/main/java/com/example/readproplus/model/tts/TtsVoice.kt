package com.example.readproplus.model.tts

/** A Kokoro voice pack. Nicole is bundled; every other pack is downloaded on demand. */
data class TtsVoice(
    val id: String,
    val displayName: String,
    val locale: String,
    val accent: String,
    val gender: String,
    val bundledAssetPath: String? = null,
    val isDefault: Boolean = false,
) {
    val remoteFileName: String
        get() = "$id.bin"

    val details: String
        get() = "$accent • $gender"

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

        val ALL = listOf(
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

        fun fromId(id: String?): TtsVoice = ALL.firstOrNull { it.id == id } ?: NICOLE
    }
}
