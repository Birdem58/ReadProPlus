package com.example.readproplus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object PlaygroundPalette {
    val background = Color(0xFFF5F7F4)
    val surface = Color(0xFFFFFFFF)
    val surfaceMuted = Color(0xFFEEF3F0)
    val ink = Color(0xFF1C332F)
    val muted = Color(0xFF6B7D78)
    val outline = Color(0xFFDDE7E2)
    val primary = Color(0xFF3F766D)
    val primaryDeep = Color(0xFF285B54)
    val primarySoft = Color(0xFFE0EFEB)
    val warm = Color(0xFFF6E9DD)
    val warmStrong = Color(0xFFD37B5F)
    val lavender = Color(0xFFEDEAF5)
    val lavenderStrong = Color(0xFF76679A)
    val chartTrack = Color(0xFFE7EEEB)
}

private data class PlaygroundBook(
    val title: String,
    val author: String,
    val category: String,
    val progress: Float,
    val progressLabel: String,
    val totalPages: Int,
    val currentPage: Int,
    val accent: Color,
    val coverLabel: String,
)

private enum class PlaygroundDestination(val label: String) {
    LIBRARY("Library"),
    READER("Reader"),
    PROGRESS("Your progress"),
}

private val playgroundBooks = listOf(
    PlaygroundBook(
        title = "Atomic Habits",
        author = "James Clear",
        category = "Personal growth",
        progress = 0.72f,
        progressLabel = "214 of 320 pages",
        totalPages = 320,
        currentPage = 214,
        accent = Color(0xFF457B72),
        coverLabel = "ATOMIC\nHABITS",
    ),
    PlaygroundBook(
        title = "The Creative Act",
        author = "Rick Rubin",
        category = "Creativity",
        progress = 0.34f,
        progressLabel = "86 of 248 pages",
        totalPages = 248,
        currentPage = 86,
        accent = Color(0xFFD07B60),
        coverLabel = "THE\nCREATIVE ACT",
    ),
    PlaygroundBook(
        title = "Sapiens",
        author = "Yuval Noah Harari",
        category = "History",
        progress = 1f,
        progressLabel = "Finished",
        totalPages = 412,
        currentPage = 412,
        accent = Color(0xFF6B739C),
        coverLabel = "SAPIENS",
    ),
    PlaygroundBook(
        title = "The Midnight Library",
        author = "Matt Haig",
        category = "Fiction",
        progress = 0.16f,
        progressLabel = "42 of 304 pages",
        totalPages = 304,
        currentPage = 42,
        accent = Color(0xFF8B6B8D),
        coverLabel = "MIDNIGHT\nLIBRARY",
    ),
)

/**
 * Isolated new UI mode for reviewing the next ReadProPlus direction.
 * It uses sample content and does not read or mutate app data.
 */
@Composable
fun DesignPlaygroundScreen(
    onExit: () -> Unit = {},
) {
    var destination by remember { mutableStateOf(PlaygroundDestination.LIBRARY) }
    var selectedBook by remember { mutableStateOf(playgroundBooks.first()) }
    var currentPage by remember { mutableIntStateOf(playgroundBooks.first().currentPage) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isHighlighted by remember { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var readerTone by remember { mutableStateOf("Paper") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All books") }

    fun openBook(book: PlaygroundBook) {
        selectedBook = book
        currentPage = book.currentPage
        isPlaying = false
        isBookmarked = false
        isHighlighted = false
        showReaderSettings = false
        destination = PlaygroundDestination.READER
    }

    BoxWithConstraintsCompat { compact ->
        Scaffold(
            containerColor = PlaygroundPalette.background,
            bottomBar = {
                if (compact && destination != PlaygroundDestination.READER) {
                    PlaygroundBottomBar(
                        selected = destination,
                        onDestinationSelected = { destination = it },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (!compact && destination != PlaygroundDestination.READER) {
                    PlaygroundRail(
                        selected = destination,
                        onDestinationSelected = { destination = it },
                        onExit = onExit,
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (destination != PlaygroundDestination.READER) {
                        PlaygroundTopBar(compact = compact, onExit = onExit)
                    }
                    when (destination) {
                        PlaygroundDestination.LIBRARY -> LibraryPlayground(
                            compact = compact,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it },
                            onBookClick = ::openBook,
                        )
                        PlaygroundDestination.READER -> ReaderPlayground(
                            compact = compact,
                            book = selectedBook,
                            currentPage = currentPage,
                            isPlaying = isPlaying,
                            isBookmarked = isBookmarked,
                            isHighlighted = isHighlighted,
                            showSettings = showReaderSettings,
                            readerTone = readerTone,
                            onBack = { destination = PlaygroundDestination.LIBRARY },
                            onSettingsClick = { showReaderSettings = !showReaderSettings },
                            onPreviousPage = { currentPage = (currentPage - 1).coerceAtLeast(1) },
                            onNextPage = { currentPage = (currentPage + 1).coerceAtMost(selectedBook.totalPages) },
                            onPlayPause = { isPlaying = !isPlaying },
                            onToggleBookmark = { isBookmarked = !isBookmarked },
                            onToggleHighlight = { isHighlighted = !isHighlighted },
                            onReaderToneChange = { readerTone = it },
                        )
                        PlaygroundDestination.PROGRESS -> InsightsPlayground(
                            compact = compact,
                            onBookClick = ::openBook,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsCompat(content: @Composable (compact: Boolean) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        content(maxWidth < 700.dp)
    }
}

@Composable
private fun PlaygroundRail(
    selected: PlaygroundDestination,
    onDestinationSelected: (PlaygroundDestination) -> Unit,
    onExit: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(232.dp),
        color = PlaygroundPalette.surface,
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
        ) {
            BrandLockup()
            Spacer(Modifier.height(38.dp))
            Text(
                text = "YOUR SPACE",
                style = MaterialTheme.typography.labelSmall,
                color = PlaygroundPalette.muted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(10.dp))
            PlaygroundDestination.values().forEach { item ->
                PlaygroundNavigationItem(
                    destination = item,
                    selected = selected == item,
                    onClick = { onDestinationSelected(item) },
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = PlaygroundPalette.primarySoft,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "A calmer way to read",
                        color = PlaygroundPalette.primaryDeep,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Soft contrast, clear hierarchy, zero visual noise.",
                        color = PlaygroundPalette.muted,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
                TextButton(
                    onClick = onExit,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                Text("Classic UI")
            }
        }
    }
}

@Composable
private fun PlaygroundBottomBar(
    selected: PlaygroundDestination,
    onDestinationSelected: (PlaygroundDestination) -> Unit,
) {
    Surface(
        color = PlaygroundPalette.surface,
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PlaygroundDestination.values().forEach { item ->
                PlaygroundBottomItem(
                    destination = item,
                    selected = selected == item,
                    onClick = { onDestinationSelected(item) },
                )
            }
        }
    }
}

@Composable
private fun PlaygroundNavigationItem(
    destination: PlaygroundDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon = when (destination) {
        PlaygroundDestination.LIBRARY -> Icons.AutoMirrored.Filled.MenuBook
        PlaygroundDestination.READER -> Icons.Default.BookmarkBorder
        PlaygroundDestination.PROGRESS -> Icons.Default.Insights
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick),
        color = if (selected) PlaygroundPalette.primarySoft else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) PlaygroundPalette.primaryDeep else PlaygroundPalette.muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = destination.label,
                color = if (selected) PlaygroundPalette.primaryDeep else PlaygroundPalette.ink,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun PlaygroundBottomItem(
    destination: PlaygroundDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon = when (destination) {
        PlaygroundDestination.LIBRARY -> Icons.AutoMirrored.Filled.MenuBook
        PlaygroundDestination.READER -> Icons.Default.BookmarkBorder
        PlaygroundDestination.PROGRESS -> Icons.Default.Insights
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = destination.label,
            tint = if (selected) PlaygroundPalette.primary else PlaygroundPalette.muted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.label,
            color = if (selected) PlaygroundPalette.primaryDeep else PlaygroundPalette.muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PlaygroundTopBar(
    compact: Boolean,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 20.dp else 32.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact) {
            BrandLockup(modifier = Modifier.weight(1f))
        } else {
            Text(
                text = "New UI mode",
                style = MaterialTheme.typography.labelLarge,
                color = PlaygroundPalette.muted,
                modifier = Modifier.weight(1f),
            )
        }
        Surface(
            color = PlaygroundPalette.warm,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = "NEW UI MODE",
                color = PlaygroundPalette.warmStrong,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Return to classic UI",
                tint = PlaygroundPalette.muted,
            )
        }
    }
}

@Composable
private fun BrandLockup(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = PlaygroundPalette.primary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "ReadPro",
            color = PlaygroundPalette.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Text(
            text = "+",
            color = PlaygroundPalette.warmStrong,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
    }
}

@Composable
private fun LibraryPlayground(
    compact: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onBookClick: (PlaygroundBook) -> Unit,
) {
    val visibleBooks = playgroundBooks.filter { book ->
        val matchesSearch = searchQuery.isBlank() ||
            book.title.contains(searchQuery, ignoreCase = true) ||
            book.author.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "In progress" -> book.progress < 1f
            "Finished" -> book.progress >= 1f
            "Favorites" -> book.title == "Atomic Habits" || book.title == "The Creative Act"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (compact) 20.dp else 32.dp)
            .padding(bottom = 28.dp),
    ) {
        Text(
            text = "Your reading space",
            color = PlaygroundPalette.ink,
            fontSize = if (compact) 30.sp else 38.sp,
            lineHeight = if (compact) 36.sp else 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pick up where you left off.",
            color = PlaygroundPalette.muted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(22.dp))
        SearchBarPreview(
            compact = compact,
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
        )
        Spacer(Modifier.height(24.dp))
        ContinueReadingCard(
            compact = compact,
            onResume = { onBookClick(playgroundBooks.first()) },
        )
        Spacer(Modifier.height(28.dp))
        SectionHeading(title = "Your collection", action = "View all")
        Spacer(Modifier.height(12.dp))
        CategoryChips(
            selected = selectedFilter,
            onSelected = onFilterSelected,
        )
        Spacer(Modifier.height(16.dp))
        if (visibleBooks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PlaygroundPalette.surfaceMuted,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = "No books match this view yet.",
                    color = PlaygroundPalette.muted,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else if (compact) {
            visibleBooks.forEach { book ->
                BookRowCard(book = book, onClick = { onBookClick(book) })
                Spacer(Modifier.height(10.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                visibleBooks.take(3).forEach { book ->
                    BookGridCard(book = book, modifier = Modifier.weight(1f), onClick = { onBookClick(book) })
                }
            }
            if (visibleBooks.size > 3) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BookGridCard(book = visibleBooks[3], modifier = Modifier.weight(1f), onClick = { onBookClick(visibleBooks[3]) })
                    CalmEmptyCollectionCard(Modifier.weight(2f))
                }
            }
        }
    }
}

@Composable
private fun SearchBarPreview(
    compact: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PlaygroundPalette.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search books",
                tint = PlaygroundPalette.muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = PlaygroundPalette.ink),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (query.isBlank()) {
                            Text("Search your library", color = PlaygroundPalette.muted)
                        }
                        innerTextField()
                    }
                },
            )
            if (!compact) {
                Surface(
                    color = PlaygroundPalette.surfaceMuted,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "⌘ K",
                        color = PlaygroundPalette.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter library",
                tint = PlaygroundPalette.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ContinueReadingCard(compact: Boolean, onResume: () -> Unit) {
    val book = playgroundBooks.first()
    Card(
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.primaryDeep),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 18.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverTile(
                label = book.coverLabel,
                accent = book.accent,
                width = if (compact) 82.dp else 104.dp,
                height = if (compact) 116.dp else 146.dp,
            )
            Spacer(Modifier.width(if (compact) 16.dp else 22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONTINUE READING",
                    color = Color(0xFFB7D8D0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = book.title,
                    color = Color.White,
                    fontSize = if (compact) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.author,
                    color = Color(0xFFB7D8D0),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = Color(0xFFB7D8D0),
                    trackColor = Color(0xFF416F68),
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.progressLabel,
                        color = Color(0xFFB7D8D0),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onResume,
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Text("Resume", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = PlaygroundPalette.ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {}) {
            Text(action, color = PlaygroundPalette.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryChips(selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("All books", "In progress", "Favorites", "Finished").forEach { label ->
            FilterChip(
                selected = selected == label,
                onClick = { onSelected(label) },
                label = { Text(label) },
                leadingIcon = if (selected == label) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PlaygroundPalette.primarySoft,
                    selectedLabelColor = PlaygroundPalette.primaryDeep,
                    selectedLeadingIconColor = PlaygroundPalette.primary,
                    containerColor = PlaygroundPalette.surface,
                    labelColor = PlaygroundPalette.muted,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == label,
                    borderColor = PlaygroundPalette.outline,
                    selectedBorderColor = PlaygroundPalette.primarySoft,
                ),
            )
        }
    }
}

@Composable
private fun BookRowCard(book: PlaygroundBook, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverTile(book.coverLabel, book.accent, 58.dp, 82.dp)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    color = PlaygroundPalette.ink,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(text = book.author, color = PlaygroundPalette.muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (book.progress >= 1f) PlaygroundPalette.primary else book.accent,
                    trackColor = PlaygroundPalette.chartTrack,
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.MoreHoriz, contentDescription = "More options", tint = PlaygroundPalette.muted)
        }
    }
}

@Composable
private fun BookGridCard(book: PlaygroundBook, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                CoverTile(book.coverLabel, book.accent, 66.dp, 94.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.category.uppercase(),
                        color = PlaygroundPalette.muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = book.title,
                        color = PlaygroundPalette.ink,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = book.author,
                        color = PlaygroundPalette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = if (book.progress >= 1f) PlaygroundPalette.primary else book.accent,
                trackColor = PlaygroundPalette.chartTrack,
            )
            Spacer(Modifier.height(7.dp))
            Text(book.progressLabel, color = PlaygroundPalette.muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CalmEmptyCollectionCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = PlaygroundPalette.surfaceMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Make room for what matters", color = PlaygroundPalette.ink, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Create a collection for the books you want to keep close.",
                color = PlaygroundPalette.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {},
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlaygroundPalette.primary),
                border = BorderStroke(1.dp, PlaygroundPalette.primary.copy(alpha = 0.45f)),
            ) {
                Text("New collection")
            }
        }
    }
}

@Composable
private fun ReaderPlayground(
    compact: Boolean,
    book: PlaygroundBook,
    currentPage: Int,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    isHighlighted: Boolean,
    showSettings: Boolean,
    readerTone: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleHighlight: () -> Unit,
    onReaderToneChange: (String) -> Unit,
) {
    val pageBackground = when (readerTone) {
        "Soft sage" -> PlaygroundPalette.surfaceMuted
        "Night" -> Color(0xFF203B3A)
        else -> Color(0xFFFBF5EA)
    }
    val pageInk = when (readerTone) {
        "Night" -> Color(0xFFE2F0EC)
        "Soft sage" -> PlaygroundPalette.ink
        else -> Color(0xFF3D342C)
    }
    val pageMuted = when (readerTone) {
        "Night" -> Color(0xFFAAC7C0)
        "Soft sage" -> PlaygroundPalette.muted
        else -> Color(0xFF887A6B)
    }
    val pageBorder = if (readerTone == "Night") Color(0xFF365A56) else Color(0xFFF0E6D7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (compact) 12.dp else 32.dp)
            .padding(bottom = if (compact) 12.dp else 28.dp),
    ) {
        Row(
            modifier = Modifier.padding(top = if (compact) 2.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library", tint = PlaygroundPalette.ink)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    color = PlaygroundPalette.ink,
                    fontSize = if (compact) 17.sp else 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Page $currentPage / ${book.totalPages}  ·  ${book.author}",
                    color = PlaygroundPalette.muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Reader settings", tint = PlaygroundPalette.muted)
            }
        }
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = pageBackground,
            shape = RoundedCornerShape(if (compact) 18.dp else 24.dp),
            border = BorderStroke(1.dp, pageBorder),
        ) {
            Column(modifier = Modifier.padding(if (compact) 16.dp else 42.dp)) {
                Text("CHAPTER THREE", color = PlaygroundPalette.warmStrong, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(if (compact) 7.dp else 10.dp))
                Text(
                    if (book.title == "The Creative Act") "The architecture of attention" else "A calmer page",
                    color = pageInk,
                    fontSize = if (compact) 25.sp else 34.sp,
                    lineHeight = if (compact) 30.sp else 40.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${book.title}  ·  ${book.author}",
                    color = pageMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(if (compact) 17.dp else 26.dp))
                ReaderParagraph(
                    "Attention is not something we force into existence. It is a room we prepare, a small clearing where the next thought can arrive without being rushed.",
                    color = pageInk,
                    compact = compact,
                )
                Spacer(Modifier.height(if (compact) 13.dp else 18.dp))
                ReaderParagraph(
                    "The best ideas often appear at the edge of our awareness. Give them enough quiet, and they begin to take shape.",
                    color = pageInk,
                    compact = compact,
                )
                Spacer(Modifier.height(if (compact) 13.dp else 20.dp))
                Surface(
                    modifier = Modifier.clickable(onClick = onToggleHighlight),
                    color = if (isHighlighted) Color(0xFFF5E2B9) else pageBackground.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        if (isHighlighted) "Make space before you ask for more." else "Tap this sentence to highlight it.",
                        color = if (isHighlighted) Color(0xFF654D31) else pageMuted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
                Spacer(Modifier.height(if (compact) 13.dp else 20.dp))
                ReaderParagraph(
                    "When the room is ready, we do not need to fill it immediately. A few quiet minutes can be enough to notice what the day was trying to say.",
                    color = pageInk,
                    compact = compact,
                )
                Spacer(Modifier.height(if (compact) 13.dp else 20.dp))
                ReaderParagraph(
                    "Not every page needs to give us an answer. Some pages simply slow the world down long enough for a better question to appear.",
                    color = pageInk,
                    compact = compact,
                )
                Spacer(Modifier.height(if (compact) 13.dp else 20.dp))
                ReaderParagraph(
                    "That small pause is part of the practice. We return to the next line with a little more attention, and the meaning has room to become our own.",
                    color = pageInk,
                    compact = compact,
                )
            }
        }
        Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
        ReaderControlBar(
            book = book,
            currentPage = currentPage,
            isPlaying = isPlaying,
            isBookmarked = isBookmarked,
            isHighlighted = isHighlighted,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            onPlayPause = onPlayPause,
            onToggleBookmark = onToggleBookmark,
            onToggleHighlight = onToggleHighlight,
        )
        if (showSettings) {
            Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
            ReaderSettingsCard(selectedTone = readerTone, onToneChange = onReaderToneChange)
        }
    }
}

@Composable
private fun ReaderParagraph(text: String, color: Color, compact: Boolean) {
    Text(
        text = text,
        color = color,
        fontSize = if (compact) 16.sp else 17.sp,
        lineHeight = if (compact) 27.sp else 29.sp,
    )
}

@Composable
private fun ReaderControlBar(
    book: PlaygroundBook,
    currentPage: Int,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    isHighlighted: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleHighlight: () -> Unit,
) {
    val pageProgress = currentPage.toFloat() / book.totalPages.toFloat()
    Card(
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = PlaygroundPalette.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isPlaying) "Listening to this page" else "Ready to read",
                    color = PlaygroundPalette.ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("Page $currentPage / ${book.totalPages}", color = PlaygroundPalette.muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { pageProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = PlaygroundPalette.primary,
                trackColor = PlaygroundPalette.chartTrack,
            )
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = currentPage > 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous page", tint = PlaygroundPalette.primary)
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                        tint = PlaygroundPalette.primaryDeep,
                    )
                }
                IconButton(
                    onClick = onToggleHighlight,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        if (isHighlighted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = if (isHighlighted) "Highlighted" else "Highlight",
                        tint = PlaygroundPalette.primaryDeep,
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onPlayPause),
                    color = PlaygroundPalette.primary,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause reading" else "Play reading",
                            tint = Color.White,
                        )
                    }
                }
                IconButton(
                    onClick = onNextPage,
                    enabled = currentPage < book.totalPages,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next page", tint = PlaygroundPalette.primary)
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsCard(selectedTone: String, onToneChange: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Reader settings", color = PlaygroundPalette.ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Choose a page tone that feels comfortable.", color = PlaygroundPalette.muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Paper", "Soft sage", "Night").forEach { tone ->
                    FilterChip(
                        selected = tone == selectedTone,
                        onClick = { onToneChange(tone) },
                        label = { Text(tone) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PlaygroundPalette.primarySoft,
                            selectedLabelColor = PlaygroundPalette.primaryDeep,
                            containerColor = PlaygroundPalette.surfaceMuted,
                            labelColor = PlaygroundPalette.muted,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsPlayground(
    compact: Boolean,
    onBookClick: (PlaygroundBook) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (compact) 20.dp else 32.dp)
            .padding(bottom = 28.dp),
    ) {
        Text(
            "Your progress",
            color = PlaygroundPalette.ink,
            fontSize = if (compact) 30.sp else 38.sp,
            lineHeight = if (compact) 36.sp else 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text("See what you have read, listened to, and finished.", color = PlaygroundPalette.muted, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightStatCard("24m", "Today", Icons.Default.AccessTime, PlaygroundPalette.primarySoft, PlaygroundPalette.primaryDeep, Modifier.weight(1f))
            InsightStatCard("7", "Day streak", Icons.Default.Fireplace, PlaygroundPalette.warm, PlaygroundPalette.warmStrong, Modifier.weight(1f))
            InsightStatCard("18", "Pages read", Icons.Default.BarChart, PlaygroundPalette.lavender, PlaygroundPalette.lavenderStrong, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        ProgressTotalsCard(compact = compact)
        Spacer(Modifier.height(18.dp))
        ReadingHistoryCard(onBookClick = onBookClick)
        Spacer(Modifier.height(18.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, PlaygroundPalette.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(if (compact) 18.dp else 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("This week", color = PlaygroundPalette.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("2h 48m total reading time", color = PlaygroundPalette.muted, fontSize = 13.sp)
                    }
                    Surface(color = PlaygroundPalette.primarySoft, shape = RoundedCornerShape(10.dp)) {
                        Text("+18%", color = PlaygroundPalette.primaryDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                    }
                }
                Spacer(Modifier.height(26.dp))
                WeeklyBars()
            }
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = PlaygroundPalette.primaryDeep, shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(color = Color(0xFF416F68), shape = CircleShape, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = Color(0xFFB7D8D0))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your rhythm is steady", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text("You read most consistently in the morning.", color = Color(0xFFB7D8D0), fontSize = 13.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ProgressTotalsCard(compact: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(if (compact) 18.dp else 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your totals", color = PlaygroundPalette.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Across reading and listening", color = PlaygroundPalette.muted, fontSize = 13.sp)
                }
                Surface(color = PlaygroundPalette.surfaceMuted, shape = RoundedCornerShape(10.dp)) {
                    Text("ALL TIME", color = PlaygroundPalette.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressMetric(value = "1,164", label = "Pages read", accent = PlaygroundPalette.primary, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(42.dp).background(PlaygroundPalette.outline))
                ProgressMetric(value = "86", label = "Pages listened", accent = PlaygroundPalette.warmStrong, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProgressMetric(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, color = PlaygroundPalette.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(6.dp))
            Text(label, color = PlaygroundPalette.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ReadingHistoryCard(onBookClick: (PlaygroundBook) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reading history", color = PlaygroundPalette.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text("Books you have completed or listened to", color = PlaygroundPalette.muted, fontSize = 13.sp)
                }
                TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text("View all", color = PlaygroundPalette.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            ReadingHistoryRow(
                title = "Atomic Habits",
                detail = "Finished Sep 12, 2026",
                amount = "320 pages",
                mode = "Read",
                accent = playgroundBooks[0].accent,
                coverLabel = playgroundBooks[0].coverLabel,
                onClick = { onBookClick(playgroundBooks[0]) },
            )
            ReadingHistoryRow(
                title = "The Creative Act",
                detail = "Last listened today",
                amount = "86 pages",
                mode = "Listened",
                accent = playgroundBooks[1].accent,
                coverLabel = playgroundBooks[1].coverLabel,
                onClick = { onBookClick(playgroundBooks[1]) },
            )
            ReadingHistoryRow(
                title = "Sapiens",
                detail = "Finished Aug 30, 2026",
                amount = "412 pages",
                mode = "Read",
                accent = playgroundBooks[2].accent,
                coverLabel = playgroundBooks[2].coverLabel,
                onClick = { onBookClick(playgroundBooks[2]) },
            )
        }
    }
}

@Composable
private fun ReadingHistoryRow(
    title: String,
    detail: String,
    amount: String,
    mode: String,
    accent: Color,
    coverLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverTile(label = coverLabel, accent = accent, width = 42.dp, height = 58.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PlaygroundPalette.ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(detail, color = PlaygroundPalette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, color = PlaygroundPalette.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(mode, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InsightStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PlaygroundPalette.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PlaygroundPalette.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(color = background, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.height(11.dp))
            Text(value, color = PlaygroundPalette.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = PlaygroundPalette.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WeeklyBars() {
    val values = listOf(0.42f, 0.65f, 0.54f, 0.88f, 0.7f, 0.96f, 0.58f)
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(98.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(98.dp * value)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(if (index == 5) PlaygroundPalette.primary else PlaygroundPalette.primarySoft),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(labels[index], color = PlaygroundPalette.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CoverTile(
    label: String,
    accent: Color,
    width: Dp,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(accent, accent.copy(alpha = 0.72f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.55f)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (width < 70.dp) 8.sp else 10.sp,
                lineHeight = if (width < 70.dp) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(width * 0.45f).height(2.dp).background(Color.White.copy(alpha = 0.65f)))
        }
    }
}
