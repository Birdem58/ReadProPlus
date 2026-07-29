package com.example.readproplus.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.ReadingStats
import com.example.readproplus.ui.theme.ReaderColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingStatsScreen(
    stats: ReadingStats,
    scheme: ReaderColorScheme,
    onMenuClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = scheme.navigationContent)
                    }
                },
                title = {
                    Text("Reading Statistics", fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                },
                actions = {
                    IconButton(onClick = onExportClick) {
                        Icon(Icons.Default.Share, contentDescription = "Export Data", tint = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = "Your Reading Insights",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.pageNumberColor,
            )

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    StatCard(
                        title = "Reading Time",
                        value = "${stats.totalReadingTimeSeconds / 60} mins",
                        icon = Icons.Default.Timer,
                        iconColor = Color(0xFF42A5F5),
                        scheme = scheme,
                    )
                }
                item {
                    StatCard(
                        title = "Pages Read",
                        value = "${stats.totalPagesRead}",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconColor = Color(0xFF66BB6A),
                        scheme = scheme,
                    )
                }
                item {
                    StatCard(
                        title = "Books Finished",
                        value = "${stats.booksFinished}",
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFFAB47BC),
                        scheme = scheme,
                    )
                }
                item {
                    StatCard(
                        title = "Reading Streak",
                        value = "${stats.currentStreakDays} Days",
                        icon = Icons.Default.LocalFireDepartment,
                        iconColor = Color(0xFFFF7043),
                        scheme = scheme,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Export Highlights & Reading Data Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Export Highlights & Reading Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.textColor,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Export your highlights, quotes, written notes, bookmarks, and reading metrics to Markdown or share with other apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.pageNumberColor,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.accentColor),
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = scheme.background)
                        Spacer(Modifier.width(6.dp))
                        Text("Export & Share", color = scheme.background)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    scheme: ReaderColorScheme,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.textColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = scheme.pageNumberColor,
            )
        }
    }
}
