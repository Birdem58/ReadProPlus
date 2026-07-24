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

@Composable
fun PdfLoadingIndicator(
    currentPage: Int,
    totalPages: Int,
    progress: Float,
    scheme: ReaderColorScheme,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
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
            text = "Page $currentPage of $totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.pageNumberColor,
        )
    }
}
