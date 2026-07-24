package com.example.readproplus.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.readproplus.model.ReadingMode

data class ReaderColorScheme(
    val background: Color,
    val textColor: Color,
    val accentColor: Color,
    val surfaceColor: Color,
    val surfaceVariant: Color,
    val navigationBar: Color,
    val navigationContent: Color,
    val dividerColor: Color,
    val pageNumberColor: Color,
    val modeSelectorBackground: Color,
    val modeSelectorActive: Color,
    val modeSelectorInactive: Color,
    val scrollbarColor: Color,
    val shadowColor: Color,
    val highlightColor: Color,
)

private val Light = ReaderColorScheme(
    background = Color(0xFFF5F0EB),
    textColor = Color(0xFF1C1B1F),
    accentColor = Color(0xFF6750A4),
    surfaceColor = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    navigationBar = Color(0xFFFFFBFE),
    navigationContent = Color(0xFF1C1B1F),
    dividerColor = Color(0xFFCAC4D0),
    pageNumberColor = Color(0xFF49454F),
    modeSelectorBackground = Color(0xFFE7E0EC),
    modeSelectorActive = Color(0xFF6750A4),
    modeSelectorInactive = Color(0xFF938F99),
    scrollbarColor = Color(0xFFCAC4D0),
    shadowColor = Color(0x1A000000),
    highlightColor = Color(0xFFFFD54F),
)

private val Dark = ReaderColorScheme(
    background = Color(0xFF1C1B1F),
    textColor = Color(0xFFE6E1E5),
    accentColor = Color(0xFFD0BCFF),
    surfaceColor = Color(0xFF2B2930),
    surfaceVariant = Color(0xFF49454F),
    navigationBar = Color(0xFF2B2930),
    navigationContent = Color(0xFFE6E1E5),
    dividerColor = Color(0xFF938F99),
    pageNumberColor = Color(0xFFCAC4D0),
    modeSelectorBackground = Color(0xFF49454F),
    modeSelectorActive = Color(0xFFD0BCFF),
    modeSelectorInactive = Color(0xFF938F99),
    scrollbarColor = Color(0xFF49454F),
    shadowColor = Color(0x33000000),
    highlightColor = Color(0xFF80CBC4),
)

private val OledDark = ReaderColorScheme(
    background = Color(0xFF000000),
    textColor = Color(0xFFE6E1E5),
    accentColor = Color(0xFFBB86FC),
    surfaceColor = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    navigationBar = Color(0xFF000000),
    navigationContent = Color(0xFFE6E1E5),
    dividerColor = Color(0xFF2C2C2C),
    pageNumberColor = Color(0xFF9E9E9E),
    modeSelectorBackground = Color(0xFF1E1E1E),
    modeSelectorActive = Color(0xFFBB86FC),
    modeSelectorInactive = Color(0xFF757575),
    scrollbarColor = Color(0xFF2C2C2C),
    shadowColor = Color(0x44000000),
    highlightColor = Color(0xFFCE93D8),
)

private val Sepia = ReaderColorScheme(
    background = Color(0xFFFBF0D9),
    textColor = Color(0xFF3E2C1A),
    accentColor = Color(0xFF8B5E3C),
    surfaceColor = Color(0xFFFFF8F0),
    surfaceVariant = Color(0xFFEDE0D4),
    navigationBar = Color(0xFFFFF8F0),
    navigationContent = Color(0xFF3E2C1A),
    dividerColor = Color(0xFFD4C5B2),
    pageNumberColor = Color(0xFF7A6B5D),
    modeSelectorBackground = Color(0xFFEDE0D4),
    modeSelectorActive = Color(0xFF8B5E3C),
    modeSelectorInactive = Color(0xFFB8A99A),
    scrollbarColor = Color(0xFFD4C5B2),
    shadowColor = Color(0x1A3E2C1A),
    highlightColor = Color(0xFFFFAB40),
)

private val Green = ReaderColorScheme(
    background = Color(0xFFC8E6C9),
    textColor = Color(0xFF1B3B1B),
    accentColor = Color(0xFF2E7D32),
    surfaceColor = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFFC8E6C9),
    navigationBar = Color(0xFFE8F5E9),
    navigationContent = Color(0xFF1B3B1B),
    dividerColor = Color(0xFFA5D6A7),
    pageNumberColor = Color(0xFF336633),
    modeSelectorBackground = Color(0xFFA5D6A7),
    modeSelectorActive = Color(0xFF1B5E20),
    modeSelectorInactive = Color(0xFF66BB6A),
    scrollbarColor = Color(0xFFA5D6A7),
    shadowColor = Color(0x1A1B3B1B),
    highlightColor = Color(0xFFAED581),
)

private val Gray = ReaderColorScheme(
    background = Color(0xFFF0F0F0),
    textColor = Color(0xFF2E2E2E),
    accentColor = Color(0xFF757575),
    surfaceColor = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFE0E0E0),
    navigationBar = Color(0xFFFAFAFA),
    navigationContent = Color(0xFF2E2E2E),
    dividerColor = Color(0xFFBDBDBD),
    pageNumberColor = Color(0xFF616161),
    modeSelectorBackground = Color(0xFFE0E0E0),
    modeSelectorActive = Color(0xFF616161),
    modeSelectorInactive = Color(0xFF9E9E9E),
    scrollbarColor = Color(0xFFBDBDBD),
    shadowColor = Color(0x1A2E2E2E),
    highlightColor = Color(0xFF90CAF9),
)

private val NightBlue = ReaderColorScheme(
    background = Color(0xFF0A1628),
    textColor = Color(0xFFB3D4FF),
    accentColor = Color(0xFF5C9EFF),
    surfaceColor = Color(0xFF0F1F3D),
    surfaceVariant = Color(0xFF1A2D4E),
    navigationBar = Color(0xFF0A1628),
    navigationContent = Color(0xFFB3D4FF),
    dividerColor = Color(0xFF233B5E),
    pageNumberColor = Color(0xFF6EB0FF),
    modeSelectorBackground = Color(0xFF1A2D4E),
    modeSelectorActive = Color(0xFF5C9EFF),
    modeSelectorInactive = Color(0xFF3A5A8C),
    scrollbarColor = Color(0xFF233B5E),
    shadowColor = Color(0x44000000),
    highlightColor = Color(0xFF4DD0E1),
)

private val readerSchemes = mapOf(
    ReadingMode.LIGHT to Light,
    ReadingMode.DARK to Dark,
    ReadingMode.OLED_DARK to OledDark,
    ReadingMode.SEPIA to Sepia,
    ReadingMode.GREEN to Green,
    ReadingMode.GRAY to Gray,
    ReadingMode.NIGHT_BLUE to NightBlue,
)

fun readerColorScheme(mode: ReadingMode): ReaderColorScheme = readerSchemes[mode] ?: Light
