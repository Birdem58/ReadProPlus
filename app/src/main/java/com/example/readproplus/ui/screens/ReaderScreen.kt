package com.example.readproplus.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.withStyle
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.ScrollMode
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.tts.TtsState
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.ui.components.PdfErrorBanner
import com.example.readproplus.ui.components.TableOfContentsSheet
import com.example.readproplus.ui.components.TtsControlBar
import com.example.readproplus.ui.components.TtsDurationDialog
import com.example.readproplus.ui.components.TtsSpeedDialog
import com.example.readproplus.ui.components.VoicePickerSheet
import com.example.readproplus.ui.theme.ReaderColorScheme
import com.example.readproplus.ui.theme.readerColorScheme
import com.example.readproplus.tts.VoiceDownloadProgress

private val DarkTeal = Color(0xFF006064)
private val BlueHandle = Color(0xFF4FC3F7)
private val WhiteText = Color(0xFFFFFFFF)

private val fontSizeOptions = listOf(14, 16, 18, 20, 22, 24, 28)

@Composable
fun ReaderScreen(
    document: PdfDocument?,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    scrollMode: ScrollMode,
    onScrollModeChange: (ScrollMode) -> Unit,
    onBackClick: () -> Unit,
    onHighlightToggle: (bookId: String, bookTitle: String, pageNumber: Int, text: String, color: Long) -> Unit = { _, _, _, _, _ -> },
    onCitationsClick: () -> Unit = {},
    pageHighlights: Set<String> = emptySet(),
    ttsState: TtsState = TtsState.Idle,
    ttsVolume: Float = 1f,
    ttsVoices: List<TtsVoice> = TtsVoice.ALL,
    selectedTtsVoice: TtsVoice = TtsVoice.NICOLE,
    voiceAvailability: Map<String, Boolean> = emptyMap(),
    voiceDownloadProgress: VoiceDownloadProgress? = null,
    onTtsStart: (String) -> Unit = {},
    onTtsDurationStart: (startPageIndex: Int, minutes: Int, mainTextOnly: Boolean) -> Unit = { _, _, _ -> },
    onTtsPause: () -> Unit = {},
    onTtsResume: () -> Unit = {},
    onTtsStop: () -> Unit = {},
    onTtsSeek: (Float) -> Unit = {},
    onTtsSpeedClick: () -> Unit = {},
    onTtsDismiss: () -> Unit = {},
    onTtsSpeedSelected: (Float) -> Unit = {},
    onTtsVolumeChanged: (Float) -> Unit = {},
    onTtsVoiceSelected: (TtsVoice) -> Unit = {},
) {
    val scheme = readerColorScheme(readingMode)
    var currentPage by remember { mutableIntStateOf(1) }
    var fontSizeIndex by remember { mutableIntStateOf(2) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var progress by remember { mutableFloatStateOf(0f) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showTtsDurationDialog by remember { mutableStateOf(false) }
    var showTtsSpeedDialog by remember { mutableStateOf(false) }
    var showVoicePicker by remember { mutableStateOf(false) }
    var mainTextOnly by rememberSaveable { mutableStateOf(true) }
    var ttsCurrentSpeed by remember { mutableFloatStateOf(1.0f) }
    val searchFocusRequester = remember { FocusRequester() }
    val searchFocusManager = LocalFocusManager.current

    val currentFontSize = fontSizeOptions[fontSizeIndex]
    val pages = document?.pages ?: emptyList()
    val totalPages = pages.size
    val bookTitle = document?.title ?: "Unknown"

    val searchMatchPages = remember(searchQuery, pages) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            pages.mapIndexedNotNull { index, content ->
                if (content.contains(searchQuery, ignoreCase = true)) index else null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSearchActive) {
                SearchTopBar(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClose = {
                        isSearchActive = false
                        searchQuery = ""
                        currentMatchIndex = 0
                        searchFocusManager.clearFocus()
                    },
                    matchCount = searchMatchPages.size,
                    currentMatch = currentMatchIndex,
                    onPrevMatch = {
                        if (searchMatchPages.isNotEmpty()) {
                            currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else searchMatchPages.lastIndex
                            currentPage = searchMatchPages[currentMatchIndex] + 1
                        }
                    },
                    onNextMatch = {
                        if (searchMatchPages.isNotEmpty()) {
                            currentMatchIndex = if (currentMatchIndex < searchMatchPages.lastIndex) currentMatchIndex + 1 else 0
                            currentPage = searchMatchPages[currentMatchIndex] + 1
                        }
                    },
                    focusRequester = searchFocusRequester,
                )
                LaunchedEffect(isSearchActive) {
                    if (isSearchActive) searchFocusRequester.requestFocus()
                }
            } else {
                TopReaderBar(
                    bookTitle = bookTitle,
                    readingMode = readingMode,
                    onReadingModeChange = onReadingModeChange,
                    scrollMode = scrollMode,
                    onScrollModeChange = onScrollModeChange,
                    onBackClick = onBackClick,
                    onSearchClick = { isSearchActive = true },
                    onTocClick = { showTocSheet = true },
                    onCitationsClick = onCitationsClick,
                    onTtsStartClick = {
                        showTtsDurationDialog = true
                    },
                    onVoiceClick = {
                        showVoicePicker = true
                    },
                )
            }

            BrightnessSubHeader(
                brightness = brightness,
                scheme = scheme,
                onBrightnessChange = { brightness = it },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(scheme.background),
            ) {
                when (scrollMode) {
                    ScrollMode.PAGED -> PagedContent(
                        pages = pages,
                        currentPage = currentPage,
                        currentFontSize = currentFontSize,
                        scheme = scheme,
                        searchQuery = searchQuery,
                        highlightColor = scheme.highlightColor,
                        onPrevPage = { if (currentPage > 1) currentPage-- },
                        onNextPage = { if (currentPage < totalPages) currentPage++ },
                        onHighlightToggle = onHighlightToggle,
                        bookId = document?.id ?: "",
                        bookTitle = bookTitle,
                        pageHighlights = pageHighlights,
                    )
                    ScrollMode.VERTICAL -> VerticalContent(
                        pages = pages,
                        currentFontSize = currentFontSize,
                        scheme = scheme,
                        searchQuery = searchQuery,
                        highlightColor = scheme.highlightColor,
                        onHighlightToggle = onHighlightToggle,
                        bookId = document?.id ?: "",
                        bookTitle = bookTitle,
                        pageHighlights = pageHighlights,
                    )
                }

                EngelleButton(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onClick = {
                        showTtsDurationDialog = true
                    },
                )
            }

            if (ttsState is TtsState.Error) {
                PdfErrorBanner(
                    message = ttsState.message,
                    visible = true,
                    onDismiss = onTtsDismiss,
                )
            }

            TtsControlBar(
                ttsState = ttsState,
                volume = ttsVolume,
                onPlay = onTtsResume,
                onPause = onTtsPause,
                onStop = onTtsStop,
                onSeek = onTtsSeek,
                onSpeedClick = {
                    ttsCurrentSpeed = when (ttsState) {
                        is TtsState.Playing -> ttsState.speed
                        is TtsState.Paused -> ttsState.speed
                        else -> 1.0f
                    }
                    showTtsSpeedDialog = true
                },
                onVolumeChange = onTtsVolumeChanged,
                onDismiss = onTtsDismiss,
            )

            BottomReaderNavBar(
                currentPage = currentPage,
                totalPages = totalPages,
                progress = progress,
                onProgressChange = {
                    progress = it
                    currentPage = (it * (totalPages - 1)).toInt() + 1
                },
            )
        }

        if (showTocSheet && document != null && document.toc.isNotEmpty()) {
            TableOfContentsSheet(
                tocEntries = document.toc,
                onItemClick = { entry ->
                    currentPage = entry.pageNumber.coerceIn(1, totalPages)
                    showTocSheet = false
                },
                onDismiss = { showTocSheet = false },
            )
        }

        if (showTtsDurationDialog) {
            TtsDurationDialog(
                mainTextOnly = mainTextOnly,
                onMainTextOnlyChange = { mainTextOnly = it },
                onGenerate = { minutes ->
                    showTtsDurationDialog = false
                    onTtsDurationStart(currentPage - 1, minutes, mainTextOnly)
                },
                onDismiss = { showTtsDurationDialog = false },
            )
        }

        if (brightness < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1f - brightness)),
            )
        }

        if (showTtsSpeedDialog) {
            TtsSpeedDialog(
                currentSpeed = ttsCurrentSpeed,
                onSpeedSelected = { speed ->
                    ttsCurrentSpeed = speed
                    onTtsSpeedSelected(speed)
                    showTtsSpeedDialog = false
                },
                onDismiss = { showTtsSpeedDialog = false },
            )
        }

        if (showVoicePicker) {
            VoicePickerSheet(
                voices = ttsVoices,
                selectedVoice = selectedTtsVoice,
                voiceAvailability = voiceAvailability,
                downloadProgress = voiceDownloadProgress,
                onVoiceSelected = onTtsVoiceSelected,
                onDismiss = { showVoicePicker = false },
            )
        }
    }
}

@Composable
private fun TopReaderBar(
    bookTitle: String,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    scrollMode: ScrollMode,
    onScrollModeChange: (ScrollMode) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onTocClick: () -> Unit = {},
    onCitationsClick: () -> Unit = {},
    onTtsStartClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
) {
    var showSettingsMenu by remember { mutableStateOf(false) }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkTeal)
            .padding(top = statusBarPadding.calculateTopPadding()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WhiteText,
                )
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onTtsStartClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Text to Speech",
                    tint = WhiteText,
                )
            }

            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = WhiteText,
                )
            }

            IconButton(onClick = onTocClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = "Table of Contents",
                    tint = WhiteText,
                )
            }

            IconButton(onClick = onCitationsClick) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Highlights",
                    tint = WhiteText,
                )
            }

            Box {
                IconButton(onClick = { showSettingsMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = WhiteText,
                    )
                }
                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false },
                ) {
                    Text(
                        text = "View Navigation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    DropdownMenuItem(
                        text = { Text("Horizontal (Paged)") },
                        onClick = {
                            showSettingsMenu = false
                            onScrollModeChange(ScrollMode.PAGED)
                        },
                        trailingIcon = {
                            if (scrollMode == ScrollMode.PAGED) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Vertical (Scroll)") },
                        onClick = {
                            showSettingsMenu = false
                            onScrollModeChange(ScrollMode.VERTICAL)
                        },
                        trailingIcon = {
                            if (scrollMode == ScrollMode.VERTICAL) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    ReadingMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                showSettingsMenu = false
                                onReadingModeChange(mode)
                            },
                            trailingIcon = {
                                if (readingMode == mode) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Kokoro voices") },
                        onClick = {
                            showSettingsMenu = false
                            onVoiceClick()
                        },
                    )
                }
            }

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bookTitle,
                fontWeight = FontWeight.Medium,
                color = WhiteText,
                fontSize = 16.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BrightnessSubHeader(
    brightness: Float,
    scheme: ReaderColorScheme,
    onBrightnessChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "☀",
            fontSize = 14.sp,
            color = scheme.textColor,
        )
        Spacer(Modifier.width(8.dp))
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.1f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = scheme.accentColor,
                activeTrackColor = scheme.accentColor,
                inactiveTrackColor = scheme.dividerColor,
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedContent(
    pages: List<String>,
    currentPage: Int,
    currentFontSize: Int,
    scheme: ReaderColorScheme,
    searchQuery: String = "",
    highlightColor: Color = Color.Transparent,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onHighlightToggle: (bookId: String, bookTitle: String, pageNumber: Int, text: String, color: Long) -> Unit = { _, _, _, _, _ -> },
    bookId: String = "",
    bookTitle: String = "",
    pageHighlights: Set<String> = emptySet(),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                    bottom = 16.dp,
                ),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
        ) {
            val pageIndex = currentPage - 1
            if (pageIndex in pages.indices) {
                val lines = pages[pageIndex].lines()
                itemsIndexed(lines) { _, line ->
                    if (line.isBlank()) {
                        Spacer(Modifier.height((currentFontSize / 3).dp))
                    } else {
                        val trimmed = line.trim()
                        val isHighlighted = trimmed in pageHighlights
                        Text(
                            text = buildTextWithHighlights(
                                text = line,
                                query = searchQuery,
                                searchHighlightColor = highlightColor,
                                isSavedHighlight = isHighlighted,
                                savedHighlightColor = highlightColor,
                            ),
                            fontSize = currentFontSize.sp,
                            lineHeight = (currentFontSize + 8).sp,
                            color = scheme.textColor,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (bookId.isNotBlank() && trimmed.isNotBlank()) {
                                        onHighlightToggle(bookId, bookTitle, currentPage, trimmed, highlightColor.value.toLong())
                                    }
                                },
                            ),
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
                    .clickable { onPrevPage() },
            )
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
            )
            Box(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
                    .clickable { onNextPage() },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalContent(
    pages: List<String>,
    currentFontSize: Int,
    scheme: ReaderColorScheme,
    searchQuery: String = "",
    highlightColor: Color = Color.Transparent,
    onHighlightToggle: (bookId: String, bookTitle: String, pageNumber: Int, text: String, color: Long) -> Unit = { _, _, _, _, _ -> },
    bookId: String = "",
    bookTitle: String = "",
    pageHighlights: Set<String> = emptySet(),
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        state = rememberLazyListState(),
        userScrollEnabled = true,
    ) {
        itemsIndexed(pages) { pageIndex, pageContent ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                val pageNumber = pageIndex + 1
                val lines = pageContent.lines()
                lines.forEach { line ->
                    if (line.isBlank()) {
                        Spacer(Modifier.height((currentFontSize / 3).dp))
                    } else {
                        val trimmed = line.trim()
                        val isHighlighted = trimmed in pageHighlights
                        Text(
                            text = buildTextWithHighlights(
                                text = line,
                                query = searchQuery,
                                searchHighlightColor = highlightColor,
                                isSavedHighlight = isHighlighted,
                                savedHighlightColor = highlightColor,
                            ),
                            fontSize = currentFontSize.sp,
                            lineHeight = (currentFontSize + 8).sp,
                            color = scheme.textColor,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (bookId.isNotBlank() && trimmed.isNotBlank()) {
                                        onHighlightToggle(bookId, bookTitle, pageNumber, trimmed, highlightColor.value.toLong())
                                    }
                                },
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 48.dp),
                    color = scheme.dividerColor.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun buildTextWithHighlights(
    text: String,
    query: String,
    searchHighlightColor: Color,
    isSavedHighlight: Boolean,
    savedHighlightColor: Color,
): AnnotatedString {
    if (query.isBlank() && !isSavedHighlight) return AnnotatedString(text)

    return buildAnnotatedString {
        if (isSavedHighlight && query.isBlank()) {
            withStyle(SpanStyle(background = savedHighlightColor.copy(alpha = 0.4f))) {
                append(text)
            }
        } else if (isSavedHighlight) {
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var lastIndex = 0
            while (true) {
                val index = lowerText.indexOf(lowerQuery, lastIndex)
                if (index == -1) break
                withStyle(SpanStyle(background = savedHighlightColor.copy(alpha = 0.4f))) {
                    append(text.substring(lastIndex, index))
                }
                withStyle(SpanStyle(background = searchHighlightColor)) {
                    append(text.substring(index, index + query.length))
                }
                lastIndex = index + query.length
            }
            withStyle(SpanStyle(background = savedHighlightColor.copy(alpha = 0.4f))) {
                append(text.substring(lastIndex))
            }
        } else {
            var lastIndex = 0
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            while (true) {
                val index = lowerText.indexOf(lowerQuery, lastIndex)
                if (index == -1) break
                append(text.substring(lastIndex, index))
                withStyle(SpanStyle(background = searchHighlightColor)) {
                    append(text.substring(index, index + query.length))
                }
                lastIndex = index + query.length
            }
            append(text.substring(lastIndex))
        }
    }
}

@Composable
private fun EngelleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .padding(end = 16.dp, bottom = 16.dp)
            .background(
                color = DarkTeal,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Engelle",
                color = WhiteText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "|\u2194|",
                color = WhiteText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomReaderNavBar(
    currentPage: Int,
    totalPages: Int,
    progress: Float,
    onProgressChange: (Float) -> Unit,
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkTeal)
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 6.dp + navBarPadding.calculateBottomPadding(),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = "$currentPage/$totalPages",
                color = WhiteText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center),
            )

            IconButton(
                onClick = { },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Rotate",
                    tint = WhiteText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Slider(
            value = if (totalPages > 1) (currentPage - 1).toFloat() / (totalPages - 1).toFloat() else 0f,
            onValueChange = {
                val page = (it * (totalPages - 1)).toInt() + 1
                onProgressChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF80DEEA),
                activeTrackColor = BlueHandle,
                inactiveTrackColor = Color(0xFF4A9A9B),
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF80DEEA)),
                )
            },
        )
    }
}

@Composable
private fun SearchTopBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    matchCount: Int,
    currentMatch: Int,
    onPrevMatch: () -> Unit,
    onNextMatch: () -> Unit,
    focusRequester: FocusRequester,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkTeal)
            .padding(top = statusBarPadding.calculateTopPadding()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close search",
                    tint = WhiteText,
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Search in document...",
                        color = WhiteText.copy(alpha = 0.6f),
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = WhiteText,
                    cursorColor = BlueHandle,
                    focusedBorderColor = BlueHandle,
                    unfocusedBorderColor = WhiteText.copy(alpha = 0.5f),
                    focusedContainerColor = DarkTeal,
                    unfocusedContainerColor = DarkTeal,
                ),
            )

            if (matchCount > 0) {
                Text(
                    text = "${currentMatch + 1}/$matchCount",
                    color = WhiteText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(onClick = onPrevMatch, enabled = matchCount > 0) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous match",
                        tint = WhiteText,
                    )
                }
                IconButton(onClick = onNextMatch, enabled = matchCount > 0) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next match",
                        tint = WhiteText,
                    )
                }
            }
        }
    }
}
