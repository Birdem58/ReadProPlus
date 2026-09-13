package com.example.readproplus.rendering

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.readproplus.model.ReadingMode

/**
 * ReadEra-style color filters for PDF and document rendering.
 * Provides eye-friendly dark mode inversion, sepia paper tint,
 * OLED black, and day reading modes.
 */
object ReaderPageColorFilter {

    private val darkMatrix = ColorMatrix(
        floatArrayOf(
            -0.85f, 0f, 0f, 0f, 220f,
            0f, -0.85f, 0f, 0f, 220f,
            0f, 0f, -0.85f, 0f, 220f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private val oledMatrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private val sepiaMatrix = ColorMatrix(
        floatArrayOf(
            0.90f, 0.05f, 0.05f, 0f, 25f,
            0.05f, 0.85f, 0.05f, 0f, 15f,
            0.02f, 0.05f, 0.70f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private val nightBlueMatrix = ColorMatrix(
        floatArrayOf(
            -0.80f, 0f, 0f, 0f, 200f,
            0f, -0.82f, 0f, 0f, 210f,
            0f, 0f, -0.75f, 0f, 235f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private val greenMatrix = ColorMatrix(
        floatArrayOf(
            0.82f, 0.05f, 0.05f, 0f, 10f,
            0.05f, 0.95f, 0.05f, 0f, 25f,
            0.05f, 0.05f, 0.82f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private val grayMatrix = ColorMatrix().apply {
        setToSaturation(0f)
    }

    fun getColorFilter(mode: ReadingMode): ColorFilter? {
        return when (mode) {
            ReadingMode.LIGHT -> null
            ReadingMode.DARK -> ColorFilter.colorMatrix(darkMatrix)
            ReadingMode.OLED_DARK -> ColorFilter.colorMatrix(oledMatrix)
            ReadingMode.SEPIA -> ColorFilter.colorMatrix(sepiaMatrix)
            ReadingMode.NIGHT_BLUE -> ColorFilter.colorMatrix(nightBlueMatrix)
            ReadingMode.GREEN -> ColorFilter.colorMatrix(greenMatrix)
            ReadingMode.GRAY -> ColorFilter.colorMatrix(grayMatrix)
        }
    }
}
