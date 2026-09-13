package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.ReaderSettings
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.ScrollMode
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.ui.theme.ReaderColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    scheme: ReaderColorScheme,
    scrollMode: ScrollMode,
    onScrollModeChange: (ScrollMode) -> Unit,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit,
    selectedTtsVoice: TtsVoice = TtsVoice.NICOLE,
    onVoiceClick: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Reader Settings & Layout",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.textColor,
            )

            Spacer(Modifier.height(16.dp))

            // PDF page view vs extracted text view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PDF page view",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.textColor,
                    )
                    Text(
                        text = if (settings.renderMode == "PAGE_IMAGE") {
                            "Show the original PDF page"
                        } else {
                            "Show extracted, reflowable text"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.pageNumberColor,
                    )
                }
                Switch(
                    checked = settings.renderMode == "PAGE_IMAGE",
                    onCheckedChange = { showPdfPage ->
                        onSettingsChanged(
                            settings.copy(
                                renderMode = if (showPdfPage) "PAGE_IMAGE" else "TEXT_REFLOW",
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = scheme.accentColor),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.dividerColor)

            Text(
                text = "Scroll Mode",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.textColor,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScrollMode.values().forEach { mode ->
                    FilterChip(
                        selected = scrollMode == mode,
                        onClick = { onScrollModeChange(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.accentColor,
                            selectedLabelColor = scheme.background,
                        ),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.dividerColor)

            Text(
                text = "Reading Theme",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.textColor,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReadingMode.values().forEach { mode ->
                    FilterChip(
                        selected = readingMode == mode,
                        onClick = { onReadingModeChange(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.accentColor,
                            selectedLabelColor = scheme.background,
                        ),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.dividerColor)

            Text(
                text = "Speech Voice",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.textColor,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceVariant)
                    .clickable(onClick = onVoiceClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedTtsVoice.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.textColor,
                    )
                    Text(
                        text = "Used when generating audio - ${selectedTtsVoice.details}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.pageNumberColor,
                    )
                }
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.accentColor,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.dividerColor)

            // Horizontal Margins
            Text(
                text = "Horizontal Margins: ${settings.horizontalMarginDp} dp",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.textColor,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(8, 16, 24, 36).forEach { margin ->
                    FilterChip(
                        selected = settings.horizontalMarginDp == margin,
                        onClick = { onSettingsChanged(settings.copy(horizontalMarginDp = margin)) },
                        label = { Text("${margin}dp") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.accentColor,
                            selectedLabelColor = scheme.background,
                        ),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.dividerColor)

            // Line Spacing
            Text(
                text = "Line Spacing: ${String.format("%.1fx", settings.lineSpacingMultiplier)}",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.textColor,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1.2f, 1.5f, 1.8f, 2.0f).forEach { spacing ->
                    FilterChip(
                        selected = settings.lineSpacingMultiplier == spacing,
                        onClick = { onSettingsChanged(settings.copy(lineSpacingMultiplier = spacing)) },
                        label = { Text("${spacing}x") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.accentColor,
                            selectedLabelColor = scheme.background,
                        ),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.dividerColor)

            // Font Family
            Text(
                text = "Font Family",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.textColor,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Sans-Serif", "Serif", "Monospace").forEach { font ->
                    FilterChip(
                        selected = settings.fontFamily == font,
                        onClick = { onSettingsChanged(settings.copy(fontFamily = font)) },
                        label = { Text(font) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.accentColor,
                            selectedLabelColor = scheme.background,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
