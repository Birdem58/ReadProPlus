package com.example.readproplus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.ReaderProgress
import com.example.readproplus.model.ReadingStats
import com.example.readproplus.ui.theme.ReaderColorScheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ProgressColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val ink: Color,
    val muted: Color,
    val outline: Color,
    val primary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,
    val warm: Color,
    val warmStrong: Color,
    val lavender: Color,
    val lavenderStrong: Color,
)

private fun progressColors(scheme: ReaderColorScheme): ProgressColors = ProgressColors(
    background = scheme.background,
    surface = scheme.surfaceColor,
    surfaceMuted = scheme.surfaceVariant,
    ink = scheme.textColor,
    muted = scheme.pageNumberColor,
    outline = scheme.dividerColor,
    primary = scheme.accentColor,
    primaryDeep = scheme.accentColor,
    primarySoft = scheme.modeSelectorBackground,
    warm = scheme.highlightColor.copy(alpha = 0.22f),
    warmStrong = scheme.accentColor,
    lavender = scheme.modeSelectorBackground.copy(alpha = 0.78f),
    lavenderStrong = scheme.pageNumberColor,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingStatsScreen(
    stats: ReadingStats,
    readingProgress: List<ReaderProgress> = emptyList(),
    scheme: ReaderColorScheme,
    onMenuClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
) {
    val colors = progressColors(scheme)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.ink)
                    }
                },
                title = {
                    Text("ReadProPlus", fontWeight = FontWeight.SemiBold, color = colors.ink)
                },
                actions = {
                    IconButton(onClick = onExportClick) {
                        Icon(Icons.Default.Share, contentDescription = "Export Data", tint = colors.primaryDeep)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Your progress",
                color = colors.ink,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "See what you have read, listened to, and finished.",
                color = colors.muted,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressStatCard(
                    value = formatReadingTime(stats.totalReadingTimeSeconds),
                    label = "Read time",
                    icon = Icons.Default.AccessTime,
                    iconBackground = colors.primarySoft,
                    iconColor = colors.primaryDeep,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
                ProgressStatCard(
                    value = stats.currentStreakDays.toString(),
                    label = "Day streak",
                    icon = Icons.Default.LocalFireDepartment,
                    iconBackground = colors.warm,
                    iconColor = colors.warmStrong,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
                ProgressStatCard(
                    value = formatCount(stats.totalPagesRead),
                    label = "Pages read",
                    icon = Icons.Default.BarChart,
                    iconBackground = colors.lavender,
                    iconColor = colors.lavenderStrong,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))
            ProgressTotalsCard(stats = stats, colors = colors)
            Spacer(Modifier.height(18.dp))
            ReadingHistoryCard(readingProgress = readingProgress, colors = colors)
        }
    }
}

@Composable
private fun ProgressStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBackground: Color,
    iconColor: Color,
    colors: ProgressColors,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(124.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                color = iconBackground,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                color = colors.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                color = colors.muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressTotalsCard(
    stats: ReadingStats,
    colors: ProgressColors,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, colors.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your totals", color = colors.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Across your reading activity", color = colors.muted, fontSize = 13.sp)
                }
                Surface(color = colors.surfaceMuted, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        "ALL TIME",
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressMetric(
                    value = formatCount(stats.totalPagesRead),
                    label = "Pages read",
                    accent = colors.primary,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(colors.outline),
                )
                ProgressMetric(
                    value = formatCount(stats.booksFinished),
                    label = "Books finished",
                    accent = colors.warmStrong,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    value: String,
    label: String,
    accent: Color,
    colors: ProgressColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(value, color = colors.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(6.dp))
            Text(label, color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReadingHistoryCard(
    readingProgress: List<ReaderProgress>,
    colors: ProgressColors,
) {
    val history = readingProgress.sortedByDescending { it.lastReadAt }
    val accents = listOf(colors.primary, colors.warmStrong, colors.lavenderStrong)

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, colors.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("Reading history", color = colors.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text("Books you have been reading", color = colors.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))

            if (history.isEmpty()) {
                Surface(color = colors.surfaceMuted, shape = RoundedCornerShape(14.dp)) {
                    Text(
                        "Your reading history will appear here.",
                        color = colors.muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            } else {
                history.take(6).forEachIndexed { index, progress ->
                    ReadingHistoryRow(
                        progress = progress,
                        accent = accents[index % accents.size],
                        colors = colors,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingHistoryRow(
    progress: ReaderProgress,
    accent: Color,
    colors: ProgressColors,
) {
    val isFinished = progress.totalPages > 0 && progress.currentPage >= progress.totalPages
    val title = progress.bookTitle.ifBlank { "Untitled book" }
    val date = formatHistoryDate(progress.lastReadAt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressCover(title = title, accent = accent)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (isFinished) "Finished $date" else "Last opened $date",
                color = colors.muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${progress.currentPage.coerceAtLeast(0)} / ${progress.totalPages.coerceAtLeast(0)}",
                color = colors.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (isFinished) "Finished" else "Reading",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProgressCover(
    title: String,
    accent: Color,
    width: Dp = 42.dp,
    height: Dp = 58.dp,
) {
    Surface(
        color = accent,
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.size(width = width, height = height),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                title
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("\n") { it.take(5).uppercase(Locale.getDefault()) },
                color = Color.White,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
        }
    }
}

private fun formatReadingTime(totalSeconds: Long): String {
    val totalMinutes = (totalSeconds / 60L).coerceAtLeast(0L)
    return if (totalMinutes >= 60L) {
        "${totalMinutes / 60L}h"
    } else {
        "${totalMinutes}m"
    }
}

private fun formatCount(value: Int): String = String.format(Locale.US, "%,d", value.coerceAtLeast(0))

private fun formatHistoryDate(timestamp: Long): String {
    if (timestamp <= 0L) return "not started"
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))
}
