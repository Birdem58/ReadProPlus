package com.example.readproplus.data

import android.content.Context
import android.content.SharedPreferences
import com.example.readproplus.model.ReaderSettings

class ReaderSettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): ReaderSettings {
        return ReaderSettings(
            horizontalMarginDp = prefs.getInt(KEY_MARGIN, 24),
            lineSpacingMultiplier = prefs.getFloat(KEY_LINE_SPACING, 1.5f),
            fontFamily = prefs.getString(KEY_FONT, "Sans-Serif") ?: "Sans-Serif",
            alignment = prefs.getString(KEY_ALIGNMENT, "Justify") ?: "Justify",
            renderMode = prefs.getString(KEY_RENDER_MODE, "TEXT_REFLOW") ?: "TEXT_REFLOW",
            scrollMode = prefs.getString(KEY_SCROLL_MODE, "PAGED") ?: "PAGED",
            readingMode = prefs.getString(KEY_READING_MODE, "SEPIA") ?: "SEPIA",
        )
    }

    fun saveSettings(settings: ReaderSettings) {
        prefs.edit()
            .putInt(KEY_MARGIN, settings.horizontalMarginDp)
            .putFloat(KEY_LINE_SPACING, settings.lineSpacingMultiplier)
            .putString(KEY_FONT, settings.fontFamily)
            .putString(KEY_ALIGNMENT, settings.alignment)
            .putString(KEY_RENDER_MODE, settings.renderMode)
            .putString(KEY_SCROLL_MODE, settings.scrollMode)
            .putString(KEY_READING_MODE, settings.readingMode)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "readproplus_reader_settings"
        private const val KEY_MARGIN = "margin"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_FONT = "font"
        private const val KEY_ALIGNMENT = "alignment"
        private const val KEY_RENDER_MODE = "render_mode"
        private const val KEY_SCROLL_MODE = "scroll_mode"
        private const val KEY_READING_MODE = "reading_mode"
    }
}
