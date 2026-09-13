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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.readproplus.data.AnnotationRepository
import com.example.readproplus.data.ReaderSettingsRepository
import com.example.readproplus.model.BookNote
import com.example.readproplus.model.Bookmark
import com.example.readproplus.model.ReaderSettings
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.ScrollMode
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.tts.GeneratedAudio
import com.example.readproplus.model.tts.TtsState
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.tts.VoiceDownloadProgress
import com.example.readproplus.ui.components.KokoroAudioDialog
import com.example.readproplus.ui.components.NotesBookmarksSheet
import com.example.readproplus.ui.components.PdfErrorBanner
import com.example.readproplus.ui.components.ReaderSettingsSheet
import com.example.readproplus.ui.components.SmartZoomableCanvas
import com.example.readproplus.ui.components.TableOfContentsSheet
import com.example.readproplus.ui.components.TtsControlBar
import com.example.readproplus.ui.components.TtsSpeedDialog
import com.example.readproplus.ui.components.VoicePickerSheet
import com.example.readproplus.ui.theme.ReaderColorScheme
import com.example.readproplus.ui.theme.readerColorScheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val DarkTeal = Color(0xFF006064)
private val BlueHandle = Color(0xFF4FC3F7)
private val ReaderProgressThumbColor = Color(0xFF9DEBF2)
private val ReaderProgressTrackColor = Color(0xFF9DEBF2).copy(alpha = 0.28f)
private val ReaderProgressInactiveTrackColor = Color.White.copy(alpha = 0.22f)
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
    generatedAudios: List<GeneratedAudio> = emptyList(),
    ttsVoices: List<TtsVoice> = TtsVoice.ALL,
    selectedTtsVoice: TtsVoice = TtsVoice.NICOLE,
    voiceAvailability: Map<String, Boolean> = emptyMap(),
    voiceDownloadProgress: VoiceDownloadProgress? = null,
    onTtsStart: (String) -> Unit = {},
    onTtsPageRangeStart: (startPage: Int, endPage: Int, mainTextOnly: Boolean) -> Unit = { _, _, _ -> },
    onGeneratedAudioPlay: (GeneratedAudio) -> Unit = {},
    onGeneratedAudioDelete: (GeneratedAudio) -> Unit = {},
    onTtsPause: () -> Unit = {},
    onTtsResume: () -> Unit = {},
    onTtsStop: () -> Unit = {},
    onTtsSeek: (Float) -> Unit = {},
    onTtsSpeedClick: () -> Unit = {},
    onTtsDismiss: () -> Unit = {},
    onTtsSpeedSelected: (Float) -> Unit = {},
    onTtsVolumeChanged: (Float) -> Unit = {},
    onTtsVoiceSelected: (TtsVoice) -> Unit = {},
    initialReaderSettings: ReaderSettings? = null,
    onReaderSettingsChanged: (ReaderSettings) -> Unit = {},
) {
    val scheme = readerColorScheme(readingMode)
    val context = LocalContext.current
    val annotationRepo = remember { AnnotationRepository(context) }

    var currentPage by remember { mutableIntStateOf(1) }
    var fontSizeIndex by remember { mutableIntStateOf(2) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var sliderJumpTargetPage by remember { mutableIntStateOf(0) }
    var selectedHighlightColor by remember { mutableStateOf(Color(0xFFFFD54F)) }

    val readerSettingsRepository = remember { ReaderSettingsRepository(context) }
    var readerSettings by remember(document?.id, initialReaderSettings) {
        mutableStateOf(initialReaderSettings ?: readerSettingsRepository.getSettings())
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showKokoroAudioDialog by remember { mutableStateOf(false) }
    var showTtsSpeedDialog by remember { mutableStateOf(false) }
    var showVoicePicker by remember { mutableStateOf(false) }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var isReaderChromeVisible by rememberSaveable(document?.id) { mutableStateOf(true) }

    var bookmarks by remember { mutableStateOf(emptyList<Bookmark>()) }
    var notes by remember { mutableStateOf(emptyList<BookNote>()) }

    var mainTextOnly by rememberSaveable { mutableStateOf(true) }
    var ttsCurrentSpeed by remember { mutableFloatStateOf(1.0f) }
    val searchFocusRequester = remember { FocusRequester() }
    val searchFocusManager = LocalFocusManager.current
    val verticalListState = remember(document?.id) { LazyListState() }
    val readerScope = rememberCoroutineScope()

    val currentFontSize = fontSizeOptions[fontSizeIndex]
    val pages = document?.pages ?: emptyList()
    val totalPages = pages.size
    val bookTitle = document?.title ?: "Unknown"
    val isPageImageMode = readerSettings.renderMode == "PAGE_IMAGE" || document?.isImageBased == true

    fun jumpToPageImmediately(page: Int) {
        val newPage = page.coerceIn(1, totalPages.coerceAtLeast(1))
        currentPage = newPage
        if (scrollMode == ScrollMode.VERTICAL && !isPageImageMode && totalPages > 0) {
            // Keep the scroll observer from publishing the old visible page
            // while this non-animated jump is being applied.
            sliderJumpTargetPage = newPage
            readerScope.launch {
                verticalListState.scrollToItem(newPage - 1)
            }
        } else {
            sliderJumpTargetPage = 0
        }
    }

    fun refreshAnnotations() {
        document?.let { doc ->
            bookmarks = annotationRepo.getBookmarksForBook(doc.id)
            notes = annotationRepo.getNotesForBook(doc.id)
        }
    }

    LaunchedEffect(document?.id) {
        refreshAnnotations()
    }

    LaunchedEffect(currentPage, totalPages, scrollMode) {
        if (scrollMode != ScrollMode.VERTICAL) {
            sliderPosition = pageToSliderPosition(currentPage, totalPages)
        }
    }

    LaunchedEffect(scrollMode, isPageImageMode, totalPages, verticalListState) {
        if (scrollMode != ScrollMode.VERTICAL || isPageImageMode || totalPages == 0) return@LaunchedEffect

        snapshotFlow {
            val firstVisibleItem = verticalListState.layoutInfo.visibleItemsInfo.firstOrNull()
            val itemSize = firstVisibleItem?.size ?: 0
            val withinPageProgress = if (itemSize > 0) {
                verticalListState.firstVisibleItemScrollOffset.toFloat() / itemSize.toFloat()
            } else {
                0f
            }
            verticalListState.firstVisibleItemIndex + withinPageProgress to sliderJumpTargetPage
        }.collectLatest { (position, requestedPage) ->
            val visiblePage = (position.toInt() + 1).coerceIn(1, totalPages)
            if (requestedPage > 0) {
                if (visiblePage == requestedPage) sliderJumpTargetPage = 0
                return@collectLatest
            }
            currentPage = visiblePage
            sliderPosition = pageToSliderPosition(position.toInt() + 1, totalPages)
        }
    }

    val searchMatchPages = remember(searchQuery, pages) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            pages.mapIndexedNotNull { index, content ->
                if (content.contains(searchQuery, ignoreCase = true)) index else null
            }
        }
    }

    val isCurrentPageBookmarked = remember(currentPage, bookmarks) {
        bookmarks.any { it.pageNumber == currentPage }
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
            } else if (isReaderChromeVisible) {
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
                    onTtsStartClick = { showKokoroAudioDialog = true },
                    onVoiceClick = { showVoicePicker = true },
                    onNotesClick = { showNotesSheet = true },
                    onSettingsClick = { showReaderSettingsSheet = true },
                    isBookmarked = isCurrentPageBookmarked,
                    onBookmarkToggle = {
                        document?.let { doc ->
                            annotationRepo.toggleBookmark(doc.id, doc.title, currentPage)
                            refreshAnnotations()
                        }
                    },
                )
            }

            if (isReaderChromeVisible) {
                BrightnessSubHeader(
                    brightness = brightness,
                    scheme = scheme,
                    onBrightnessChange = { brightness = it },
                    renderMode = readerSettings.renderMode,
                    onRenderModeToggle = {
                        val updated = readerSettings.copy(
                            renderMode = if (readerSettings.renderMode == "TEXT_REFLOW") "PAGE_IMAGE" else "TEXT_REFLOW",
                        )
                        readerSettings = updated
                        readerSettingsRepository.saveSettings(updated)
                        onReaderSettingsChanged(updated)
                    },
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(scheme.background),
            ) {
                if (isPageImageMode) {
                    SmartZoomableCanvas(
                        pageIndex = currentPage - 1,
                        document = document,
                        scheme = scheme,
                        showControls = isReaderChromeVisible,
                        onSingleTap = { isReaderChromeVisible = !isReaderChromeVisible },
                        onPreviousPage = { if (currentPage > 1) currentPage-- },
                        onNextPage = { if (currentPage < totalPages) currentPage++ },
                    )
                } else {
                    when (scrollMode) {
                        ScrollMode.PAGED -> PagedContent(
                            pages = pages,
                            currentPage = currentPage,
                            currentFontSize = currentFontSize,
                            scheme = scheme,
                            marginDp = readerSettings.horizontalMarginDp,
                            lineSpacingMultiplier = readerSettings.lineSpacingMultiplier,
                            fontFamily = readerSettings.fontFamily,
                            textAlignment = readerSettings.alignment,
                            searchQuery = searchQuery,
                            highlightColor = selectedHighlightColor,
                            onSingleTap = { isReaderChromeVisible = !isReaderChromeVisible },
                            onPrevPage = { if (currentPage > 1) currentPage-- },
                            onNextPage = { if (currentPage < totalPages) currentPage++ },
                            onHighlightToggle = { bId, bTitle, pNum, text, _ ->
                                onHighlightToggle(bId, bTitle, pNum, text, selectedHighlightColor.value.toLong())
                            },
                            bookId = document?.id ?: "",
                            bookTitle = bookTitle,
                            pageHighlights = pageHighlights,
                        )
                        ScrollMode.VERTICAL -> VerticalContent(
                            pages = pages,
                            state = verticalListState,
                            currentFontSize = currentFontSize,
                            scheme = scheme,
                            marginDp = readerSettings.horizontalMarginDp,
                            lineSpacingMultiplier = readerSettings.lineSpacingMultiplier,
                            fontFamily = readerSettings.fontFamily,
                            textAlignment = readerSettings.alignment,
                            searchQuery = searchQuery,
                            highlightColor = selectedHighlightColor,
                            onSingleTap = { isReaderChromeVisible = !isReaderChromeVisible },
                            onHighlightToggle = { bId, bTitle, pNum, text, _ ->
                                onHighlightToggle(bId, bTitle, pNum, text, selectedHighlightColor.value.toLong())
                            },
                            bookId = document?.id ?: "",
                            bookTitle = bookTitle,
                            pageHighlights = pageHighlights,
                        )
                    }
                }

                if (isReaderChromeVisible) {
                    EngelleButton(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        onClick = { showKokoroAudioDialog = true },
                    )
                }
            }

            if (isReaderChromeVisible && ttsState is TtsState.Error) {
                PdfErrorBanner(
                    message = ttsState.message,
                    visible = true,
                    onDismiss = onTtsDismiss,
                )
            }

            if (isReaderChromeVisible) {
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
            }

            if (isReaderChromeVisible) {
                BottomReaderNavBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    sliderPosition = sliderPosition,
                    onSliderDrag = { position ->
                        sliderPosition = position
                        jumpToPageImmediately(sliderToPage(position, totalPages))
                    },
                    onSliderDragFinished = {
                        jumpToPageImmediately(sliderToPage(sliderPosition, totalPages))
                    },
                )
            }
        }

        if (showTocSheet && document != null && document.toc.isNotEmpty()) {
            TableOfContentsSheet(
                tocEntries = document.toc,
                onItemClick = { entry ->
                    currentPage = entry.pageNumber.coerceIn(1, totalPages)
                    if (scrollMode == ScrollMode.VERTICAL && !isPageImageMode) {
                        readerScope.launch {
                            verticalListState.animateScrollToItem(currentPage - 1)
                        }
                    }
                    showTocSheet = false
                },
                onDismiss = { showTocSheet = false },
            )
        }

        if (showKokoroAudioDialog) {
            KokoroAudioDialog(
                totalPages = totalPages,
                currentPage = currentPage,
                mainTextOnly = mainTextOnly,
                generatedAudios = generatedAudios,
                onMainTextOnlyChange = { mainTextOnly = it },
                onGenerate = { startPage, endPage, mainTextOnlyValue ->
                    showKokoroAudioDialog = false
                    onTtsPageRangeStart(startPage, endPage, mainTextOnlyValue)
                },
                onPlay = onGeneratedAudioPlay,
                onDelete = onGeneratedAudioDelete,
                onDismiss = { showKokoroAudioDialog = false },
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

        if (showReaderSettingsSheet) {
            ReaderSettingsSheet(
                settings = readerSettings,
                scheme = scheme,
                scrollMode = scrollMode,
                onScrollModeChange = onScrollModeChange,
                readingMode = readingMode,
                onReadingModeChange = onReadingModeChange,
                selectedTtsVoice = selectedTtsVoice,
                onVoiceClick = { showVoicePicker = true },
                onSettingsChanged = {
                    readerSettings = it
                    readerSettingsRepository.saveSettings(it)
                    onReaderSettingsChanged(it)
                },
                onDismiss = { showReaderSettingsSheet = false },
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

        if (showNotesSheet && document != null) {
            NotesBookmarksSheet(
                bookTitle = bookTitle,
                currentPage = currentPage,
                bookmarks = bookmarks,
                notes = notes,
                scheme = scheme,
                onAddNote = { noteText ->
                    annotationRepo.addNote(document.id, bookTitle, currentPage, noteText)
                    refreshAnnotations()
                },
                onDeleteNote = { noteId ->
                    annotationRepo.removeNote(noteId)
                    refreshAnnotations()
                },
                onToggleBookmark = {
                    annotationRepo.toggleBookmark(document.id, bookTitle, currentPage)
                    refreshAnnotations()
                },
                onToggleBookmarkAtPage = { pageNumber ->
                    annotationRepo.toggleBookmark(document.id, bookTitle, pageNumber)
                    refreshAnnotations()
                },
                isCurrentPageBookmarked = isCurrentPageBookmarked,
                onDismiss = { showNotesSheet = false },
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
    onNotesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onBookmarkToggle) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) Color(0xFFFFD54F) else WhiteText,
                )
            }

            IconButton(onClick = onNotesClick) {
                Icon(Icons.Default.PushPin, contentDescription = "Notes", tint = WhiteText)
            }

            IconButton(onClick = onTtsStartClick) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Text to Speech", tint = WhiteText)
            }

            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = WhiteText)
            }

            IconButton(onClick = onTocClick) {
                Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "Table of Contents", tint = WhiteText)
            }

            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Layout Settings", tint = WhiteText)
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
    renderMode: String,
    onRenderModeToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("☀", fontSize = 14.sp, color = scheme.textColor)
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
    marginDp: Int = 24,
    lineSpacingMultiplier: Float = 1.5f,
    fontFamily: String = "Sans-Serif",
    textAlignment: String = "Justify",
    searchQuery: String = "",
    highlightColor: Color = Color.Transparent,
    onSingleTap: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onHighlightToggle: (bookId: String, bookTitle: String, pageNumber: Int, text: String, color: Long) -> Unit = { _, _, _, _, _ -> },
    bookId: String = "",
    bookTitle: String = "",
    pageHighlights: Set<String> = emptySet(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onSingleTap),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = marginDp.dp,
                    end = marginDp.dp,
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
                            lineHeight = (currentFontSize * lineSpacingMultiplier).sp,
                            fontFamily = readerFontFamily(fontFamily),
                            color = scheme.textColor,
                            textAlign = readerTextAlign(textAlignment),
                            modifier = Modifier.combinedClickable(
                                onClick = onSingleTap,
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
            Box(modifier = Modifier.weight(0.25f).fillMaxHeight().clickable { onPrevPage() })
            Box(modifier = Modifier.weight(0.5f).fillMaxHeight())
            Box(modifier = Modifier.weight(0.25f).fillMaxHeight().clickable { onNextPage() })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalContent(
    pages: List<String>,
    state: LazyListState,
    currentFontSize: Int,
    scheme: ReaderColorScheme,
    marginDp: Int = 24,
    lineSpacingMultiplier: Float = 1.5f,
    fontFamily: String = "Sans-Serif",
    textAlignment: String = "Justify",
    searchQuery: String = "",
    highlightColor: Color = Color.Transparent,
    onSingleTap: () -> Unit,
    onHighlightToggle: (bookId: String, bookTitle: String, pageNumber: Int, text: String, color: Long) -> Unit = { _, _, _, _, _ -> },
    bookId: String = "",
    bookTitle: String = "",
    pageHighlights: Set<String> = emptySet(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onSingleTap),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = marginDp.dp, end = marginDp.dp, top = 16.dp, bottom = 16.dp),
            state = state,
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
                            lineHeight = (currentFontSize * lineSpacingMultiplier).sp,
                            fontFamily = readerFontFamily(fontFamily),
                            color = scheme.textColor,
                            textAlign = readerTextAlign(textAlignment),
                            modifier = Modifier.combinedClickable(
                                onClick = onSingleTap,
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
}

private fun readerFontFamily(name: String): FontFamily = when (name.lowercase()) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

private fun readerTextAlign(name: String): TextAlign = when (name.lowercase()) {
    "left" -> TextAlign.Start
    "center" -> TextAlign.Center
    "right" -> TextAlign.End
    else -> TextAlign.Justify
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
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomReaderNavBar(
    currentPage: Int,
    totalPages: Int,
    sliderPosition: Float,
    onSliderDrag: (Float) -> Unit,
    onSliderDragFinished: () -> Unit,
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val safeTotalPages = totalPages.coerceAtLeast(1)
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
                text = "Page ${currentPage.coerceIn(1, safeTotalPages)} of $safeTotalPages",
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "1",
                color = WhiteText.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
            Slider(
                value = sliderPosition.coerceIn(0f, 1f),
                onValueChange = { onSliderDrag(it.coerceIn(0f, 1f)) },
                onValueChangeFinished = onSliderDragFinished,
                enabled = safeTotalPages > 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .semantics {
                        contentDescription = "Page ${currentPage.coerceIn(1, safeTotalPages)} of $safeTotalPages"
                    },
                colors = SliderDefaults.colors(
                    thumbColor = ReaderProgressThumbColor,
                    activeTrackColor = ReaderProgressTrackColor,
                    inactiveTrackColor = ReaderProgressInactiveTrackColor,
                    disabledThumbColor = ReaderProgressThumbColor.copy(alpha = 0.5f),
                    disabledActiveTrackColor = ReaderProgressTrackColor.copy(alpha = 0.5f),
                    disabledInactiveTrackColor = ReaderProgressInactiveTrackColor.copy(alpha = 0.5f),
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(ReaderProgressThumbColor),
                    )
                },
            )
            Text(
                text = safeTotalPages.toString(),
                color = WhiteText.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }
    }
}

internal fun pageToSliderPosition(currentPage: Int, totalPages: Int): Float {
    if (totalPages <= 1) return 0f
    return ((currentPage.coerceIn(1, totalPages) - 1).toFloat() / (totalPages - 1).toFloat())
        .coerceIn(0f, 1f)
}

internal fun sliderToPage(sliderPosition: Float, totalPages: Int): Int {
    if (totalPages <= 1) return 1
    return (sliderPosition.coerceIn(0f, 1f) * (totalPages - 1))
        .toInt()
        .plus(1)
        .coerceIn(1, totalPages)
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
