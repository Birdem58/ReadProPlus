package com.example.readproplus.model

data class ReaderSettings(
    val horizontalMarginDp: Int = 24,
    val lineSpacingMultiplier: Float = 1.5f,
    val fontFamily: String = "Sans-Serif",
    val alignment: String = "Justify",
    val renderMode: String = "PAGE_IMAGE",
    val scrollMode: String = "VERTICAL",
    val readingMode: String = "SEPIA",
)
