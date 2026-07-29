package com.example.readproplus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.readproplus.ui.theme.ReaderColorScheme
import kotlin.math.roundToInt

@Composable
fun PdfLoadingIndicator(
    currentPage: Int,
    totalPages: Int,
    progress: Float,
    scheme: ReaderColorScheme,
    currentDocument: Int = 1,
    totalDocuments: Int = 1,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "progress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = "Extracting PDF...",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.textColor,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth(),
            color = scheme.accentColor,
            trackColor = scheme.surfaceColor,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                totalPages > 0 && currentPage > 0 ->
                    "Page $currentPage of $totalPages  •  ${(animatedProgress * 100).roundToInt()}%"
                totalPages > 0 ->
                    "Preparing PDF  •  $totalPages pages"
                else ->
                    "Preparing PDF...  •  ${(animatedProgress * 100).roundToInt()}%"
            },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.pageNumberColor,
        )
        if (totalDocuments > 1) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Document $currentDocument of $totalDocuments",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.pageNumberColor,
            )
        }
    }
}
