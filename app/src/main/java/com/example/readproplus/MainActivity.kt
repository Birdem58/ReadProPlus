package com.example.readproplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.ScrollMode
import com.example.readproplus.model.SidebarSection
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.data.ReaderSettingsRepository
import com.example.readproplus.data.ReadingStatsRepository
import com.example.readproplus.model.ReaderSettings
import com.example.readproplus.model.tts.TtsState
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.ui.components.AppSidebar
import com.example.readproplus.ui.components.ModelDownloadDialog
import com.example.readproplus.ui.screens.AuthorsScreen
import com.example.readproplus.ui.screens.BooksAndDocumentsScreen
import com.example.readproplus.ui.screens.CitationsScreen
import com.example.readproplus.ui.screens.CollectionsScreen
import com.example.readproplus.ui.screens.DesignPlaygroundScreen
import com.example.readproplus.ui.screens.DownloadsScreen
import com.example.readproplus.ui.screens.FavoritesScreen
import com.example.readproplus.ui.screens.FoldersScreen
import com.example.readproplus.ui.screens.HaveReadScreen
import com.example.readproplus.ui.screens.LibraryScreen
import com.example.readproplus.ui.screens.ReadingNowScreen
import com.example.readproplus.ui.screens.ReaderScreen
import com.example.readproplus.ui.screens.SeriesScreen
import com.example.readproplus.ui.screens.SettingsScreen
import com.example.readproplus.ui.screens.ToReadScreen
import com.example.readproplus.ui.screens.TrashScreen
import com.example.readproplus.ui.theme.ReadProPlusTheme
import com.example.readproplus.ui.theme.readerColorScheme
import com.example.readproplus.ui.viewmodel.KokoroTtsViewModel
import com.example.readproplus.ui.viewmodel.PdfExtractorViewModel
import com.example.readproplus.ui.viewmodel.SidebarViewModel
import com.example.readproplus.tts.VoiceDownloadProgress
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showMainContent by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                // Commit a lightweight visible frame before constructing the
                // library/reader tree and its optional native dependencies.
                withFrameNanos { }
                showMainContent = true
            }

            if (!showMainContent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFBF0D9)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text(
                        text = "Loading library...",
                        color = Color(0xFF3E2C1A),
                    )
                }
            } else {
            val viewModel: PdfExtractorViewModel = viewModel()
            val sidebarViewModel: SidebarViewModel = viewModel()
            val context = LocalContext.current
            val readerSettingsRepository = remember { ReaderSettingsRepository(context) }
            val readingStatsRepository = remember { ReadingStatsRepository(context) }

            val storedReaderSettings = remember { readerSettingsRepository.getSettings() }
            var readerSettings by remember { mutableStateOf(storedReaderSettings) }
            var readingMode by remember {
                mutableStateOf(
                    ReadingMode.values().firstOrNull { it.name == storedReaderSettings.readingMode }
                        ?: ReadingMode.SEPIA,
                )
            }
            var scrollMode by remember {
                mutableStateOf(
                    ScrollMode.values().firstOrNull { it.name == storedReaderSettings.scrollMode }
                        ?: ScrollMode.PAGED,
                )
            }
            // Keep the classic UI as the default. The new UI is an explicit mode
            // so the existing navigation and reader never disappear.
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
            var currentPage by remember { mutableIntStateOf(1) }
            var sidebarSection by remember { mutableStateOf(SidebarSection.BOOKS_AND_DOCUMENTS) }
            // TTS initializes optional ONNX/audio dependencies. Do not create
            // it while the library is opening; the library must get its first
            // frame without waiting for the reader backend.
            val ttsRequested = currentScreen is Screen.Reader ||
                (currentScreen as? Screen.SidebarScreen)?.section == SidebarSection.SETTINGS
            val ttsViewModel: KokoroTtsViewModel? = if (ttsRequested) {
                viewModel()
            } else {
                null
            }
            val scheme = readerColorScheme(readingMode)
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            val selectedDocument by viewModel.selectedDocument.collectAsState()
            val allHighlights by viewModel.highlights.collectAsState()
            val libraryBooks by viewModel.libraryBooks.collectAsState()
            val ttsState = ttsViewModel?.ttsState?.collectAsState()?.value ?: TtsState.Idle
            val ttsVolume = ttsViewModel?.volume?.collectAsState()?.value ?: 1f
            val generatedAudios = ttsViewModel?.generatedAudios?.collectAsState()?.value ?: emptyList()
            val selectedTtsVoice = ttsViewModel?.selectedVoice?.collectAsState()?.value ?: TtsVoice.NICOLE
            val voiceAvailability = ttsViewModel?.voiceAvailability?.collectAsState()?.value ?: emptyMap()
            val voiceDownloadProgress = ttsViewModel?.voiceDownloadProgress?.collectAsState()?.value

            LaunchedEffect(selectedDocument?.id, ttsViewModel) {
                ttsViewModel?.loadGeneratedAudios(selectedDocument?.id)
            }

            val readingProgress by sidebarViewModel.readingProgress.collectAsState()
            val favorites by sidebarViewModel.favorites.collectAsState()
            val toRead by sidebarViewModel.toRead.collectAsState()
            val haveRead by sidebarViewModel.haveRead.collectAsState()
            val collections by sidebarViewModel.collections.collectAsState()
            val folders by sidebarViewModel.folders.collectAsState()
            val trash by sidebarViewModel.trash.collectAsState()

            val activeBooks = libraryBooks.filter { it.id !in trash }

            val pageHighlights by remember {
                derivedStateOf {
                    val doc = selectedDocument ?: return@derivedStateOf emptySet<String>()
                    allHighlights
                        .filter { it.bookId == doc.id && it.pageNumber == currentPage }
                        .map { it.text.trim() }
                        .toSet()
                }
            }

            fun navigateToBook(document: PdfDocument) {
                viewModel.onBookSelected(document)
                currentPage = 1
                currentScreen = Screen.Reader
            }

            fun returnToPreviousScreen() {
                viewModel.onReturnToLibrary()
                currentScreen = Screen.Library
                sidebarSection = SidebarSection.BOOKS_AND_DOCUMENTS
            }

            DisposableEffect(selectedDocument?.id) {
                selectedDocument?.let { readingStatsRepository.startSession(it.id) }
                onDispose { readingStatsRepository.finishSession() }
            }

            ReadProPlusTheme(
                darkTheme = readingMode in setOf(ReadingMode.DARK, ReadingMode.OLED_DARK, ReadingMode.NIGHT_BLUE) ||
                    isSystemInDarkTheme(),
            ) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            AppSidebar(
                                currentSection = sidebarSection,
                                onSectionSelected = { section ->
                                    sidebarSection = section
                                    currentScreen = if (section == SidebarSection.BOOKS_AND_DOCUMENTS) {
                                        // Books and Documents is the library entry point. Keep it on the
                                        // same screen as the home page so the layout and view modes cannot
                                        // drift apart between the two navigation paths.
                                        Screen.Library
                                    } else {
                                        Screen.SidebarScreen(section)
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                scheme = scheme,
                                onNewUiModeClick = {
                                    currentScreen = Screen.NewUiMode
                                    scope.launch { drawerState.close() }
                                },
                            )
                        }
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.background),
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.NewUiMode -> {
                                DesignPlaygroundScreen(
                                    onExit = {
                                        currentScreen = Screen.Library
                                        sidebarSection = SidebarSection.BOOKS_AND_DOCUMENTS
                                    },
                                )
                            }
                            is Screen.Library -> {
                                LibraryScreen(
                                    readingMode = readingMode,
                                    viewModel = viewModel,
                                    onBookClick = { navigateToBook(it) },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onNewUiModeClick = { currentScreen = Screen.NewUiMode },
                                    favorites = favorites,
                                    toRead = toRead,
                                    haveRead = haveRead,
                                    onToggleFavorite = { sidebarViewModel.toggleFavorite(it) },
                                    onToggleToRead = { sidebarViewModel.toggleToRead(it) },
                                    onToggleHaveRead = { sidebarViewModel.toggleHaveRead(it) },
                                )
                            }
                            is Screen.Reader -> {
                                ReaderScreen(
                                    document = selectedDocument,
                                    readingMode = readingMode,
                                    initialReaderSettings = readerSettings,
                                    onReaderSettingsChanged = {
                                        readerSettings = it
                                        readerSettingsRepository.saveSettings(it)
                                    },
                                    scrollMode = scrollMode,
                                    onScrollModeChange = { mode ->
                                        scrollMode = mode
                                        val updated = readerSettings.copy(scrollMode = mode.name)
                                        readerSettings = updated
                                        readerSettingsRepository.saveSettings(updated)
                                    },
                                    onReadingModeChange = { mode ->
                                        readingMode = mode
                                        val updated = readerSettings.copy(readingMode = mode.name)
                                        readerSettings = updated
                                        readerSettingsRepository.saveSettings(updated)
                                    },
                                    onBackClick = { returnToPreviousScreen() },
                                    onHighlightToggle = { bookId, bookTitle, pageNumber, text, color ->
                                        viewModel.toggleHighlight(bookId, bookTitle, pageNumber, text, color)
                                        sidebarViewModel.saveProgress(bookId, bookTitle, currentPage, selectedDocument?.totalPages ?: 1)
                                    },
                                    onCitationsClick = {
                                        currentScreen = Screen.Citations
                                    },
                                     pageHighlights = pageHighlights,
                                     ttsState = ttsState,
                                     ttsVolume = ttsVolume,
                                     generatedAudios = generatedAudios,
                                    ttsVoices = TtsVoice.ALL,
                                    selectedTtsVoice = selectedTtsVoice,
                                    voiceAvailability = voiceAvailability,
                                     voiceDownloadProgress = voiceDownloadProgress,
                                     onTtsStart = { text ->
                                         ttsViewModel?.startReadingOrInitialize(text)
                                     },
                                     onTtsPageRangeStart = { startPage, endPage, mainTextOnly ->
                                         selectedDocument?.let { document ->
                                              ttsViewModel?.startPageRangeReading(
                                                 bookId = document.id,
                                                 bookTitle = document.title,
                                                 pages = document.pages,
                                                 startPage = startPage,
                                                 endPage = endPage,
                                                 mainTextOnly = mainTextOnly,
                                             )
                                         }
                                     },
                                      onGeneratedAudioPlay = { audio -> ttsViewModel?.playGeneratedAudio(audio) },
                                      onGeneratedAudioDelete = { audio -> ttsViewModel?.deleteGeneratedAudio(audio) },
                                     onTtsPause = { ttsViewModel?.pauseReading() },
                                     onTtsResume = { ttsViewModel?.resumeReading() },
                                     onTtsStop = { ttsViewModel?.stopReading() },
                                     onTtsSeek = { progress -> ttsViewModel?.seekTo(progress) },
                                     onTtsSpeedClick = { },
                                     onTtsDismiss = { ttsViewModel?.stopReading() },
                                     onTtsSpeedSelected = { speed -> ttsViewModel?.setSpeed(speed) },
                                     onTtsVolumeChanged = { volume -> ttsViewModel?.setVolume(volume) },
                                     onTtsVoiceSelected = { voice -> ttsViewModel?.selectVoice(voice) },
                                )
                                if (selectedDocument != null) {
                                    androidx.compose.runtime.LaunchedEffect(currentPage, selectedDocument?.id) {
                                        val doc = selectedDocument ?: return@LaunchedEffect
                                        sidebarViewModel.saveProgress(doc.id, doc.title, currentPage, doc.totalPages)
                                        readingStatsRepository.recordPageRead(doc.id, currentPage, doc.totalPages)
                                    }
                                }
                            }
                            is Screen.Citations -> {
                                CitationsScreen(
                                    highlights = allHighlights,
                                    readingMode = readingMode,
                                    onBackClick = {
                                        currentScreen = Screen.Reader
                                    },
                                     onHighlightClick = { highlight ->
                                         // Reopen the cached source document so a
                                         // citation jump keeps its real pages and
                                         // source URI instead of creating an empty
                                         // placeholder reader.
                                         libraryBooks.firstOrNull { it.id == highlight.bookId }?.let { document ->
                                             viewModel.onBookSelected(document)
                                             currentPage = highlight.pageNumber.coerceIn(
                                                 1,
                                                 document.pages.size.coerceAtLeast(1),
                                             )
                                             currentScreen = Screen.Reader
                                         }
                                     },
                                    onDeleteHighlight = { id ->
                                        viewModel.removeHighlight(id)
                                    },
                                )
                            }
                            is Screen.SidebarScreen -> {
                                val section = screen.section
                                SidebarSectionContent(
                                    section = section,
                                    libraryBooks = libraryBooks,
                                    activeBooks = activeBooks,
                                    readingProgress = readingProgress,
                                    favorites = favorites,
                                    toRead = toRead,
                                    haveRead = haveRead,
                                    collections = collections,
                                    folders = folders,
                                    trash = trash,
                                    scheme = scheme,
                                    sidebarViewModel = sidebarViewModel,
                                    readingMode = readingMode,
                                    onReadingModeChange = { mode ->
                                        readingMode = mode
                                        val updated = readerSettings.copy(readingMode = mode.name)
                                        readerSettings = updated
                                        readerSettingsRepository.saveSettings(updated)
                                    },
                                    scrollMode = scrollMode,
                                    onScrollModeChange = { mode ->
                                        scrollMode = mode
                                        val updated = readerSettings.copy(scrollMode = mode.name)
                                        readerSettings = updated
                                        readerSettingsRepository.saveSettings(updated)
                                    },
                                    ttsVoices = TtsVoice.ALL,
                                    selectedTtsVoice = selectedTtsVoice,
                                    voiceAvailability = voiceAvailability,
                                    voiceDownloadProgress = voiceDownloadProgress,
                                    onTtsVoiceSelected = { voice -> ttsViewModel?.selectVoice(voice) },
                                    onBookClick = { navigateToBook(it) },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                )
                            }
                        }

                        if (ttsState is TtsState.ModelDownloading) {
                            val ds = ttsState as TtsState.ModelDownloading
                            ModelDownloadDialog(
                                progress = ds.progress,
                                bytesDownloaded = ds.bytesDownloaded,
                                totalBytes = ds.totalBytes,
                                 onCancel = { ttsViewModel?.cancelDownload() },
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

sealed class Screen {
    data object NewUiMode : Screen()
    data object Library : Screen()
    data object Reader : Screen()
    data object Citations : Screen()
    data class SidebarScreen(val section: SidebarSection) : Screen()
}

@Composable
private fun SidebarSectionContent(
    section: SidebarSection,
    libraryBooks: List<PdfDocument>,
    activeBooks: List<PdfDocument>,
    readingProgress: List<com.example.readproplus.model.ReaderProgress>,
    favorites: Set<String>,
    toRead: Set<String>,
    haveRead: Set<String>,
    collections: List<com.example.readproplus.model.BookCollection>,
    folders: List<com.example.readproplus.model.BookFolder>,
    trash: Set<String>,
    scheme: com.example.readproplus.ui.theme.ReaderColorScheme,
    sidebarViewModel: SidebarViewModel,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    scrollMode: ScrollMode,
    onScrollModeChange: (ScrollMode) -> Unit,
    ttsVoices: List<TtsVoice>,
    selectedTtsVoice: TtsVoice,
    voiceAvailability: Map<String, Boolean>,
    voiceDownloadProgress: VoiceDownloadProgress?,
    onTtsVoiceSelected: (TtsVoice) -> Unit,
    onBookClick: (PdfDocument) -> Unit,
    onMenuClick: () -> Unit,
) {
    when (section) {
        SidebarSection.READING_NOW -> {
            ReadingNowScreen(
                readingProgress = readingProgress,
                allBooks = libraryBooks,
                scheme = scheme,
                onBookClick = onBookClick,
                onRemoveProgress = { sidebarViewModel.removeProgress(it) },
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.BOOKS_AND_DOCUMENTS -> {
            BooksAndDocumentsScreen(
                books = activeBooks,
                scheme = scheme,
                onBookClick = onBookClick,
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.FAVORITES -> {
            FavoritesScreen(
                books = activeBooks,
                favorites = favorites,
                scheme = scheme,
                onBookClick = onBookClick,
                onToggleFavorite = { sidebarViewModel.toggleFavorite(it) },
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.TO_READ -> {
            ToReadScreen(
                books = activeBooks,
                toRead = toRead,
                scheme = scheme,
                onBookClick = onBookClick,
                onToggleToRead = { sidebarViewModel.toggleToRead(it) },
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.HAVE_READ -> {
            HaveReadScreen(
                books = activeBooks,
                haveRead = haveRead,
                scheme = scheme,
                onBookClick = onBookClick,
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.AUTHORS -> {
            AuthorsScreen(
                authors = sidebarViewModel.getAuthors(activeBooks),
                scheme = scheme,
                onBookClick = onBookClick,
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.SERIES -> {
            SeriesScreen(
                seriesMap = sidebarViewModel.getSeries(activeBooks),
                scheme = scheme,
                onBookClick = onBookClick,
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.COLLECTIONS -> {
            CollectionsScreen(
                collections = collections,
                books = activeBooks,
                scheme = scheme,
                onBookClick = onBookClick,
                onCreateCollection = { sidebarViewModel.createCollection(it) },
                onDeleteCollection = { sidebarViewModel.deleteCollection(it) },
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.FOLDERS -> {
            FoldersScreen(
                folders = folders,
                books = activeBooks,
                scheme = scheme,
                onBookClick = onBookClick,
                onCreateFolder = { sidebarViewModel.createFolder(it) },
                onDeleteFolder = { sidebarViewModel.deleteFolder(it) },
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.DOWNLOADS -> {
            DownloadsScreen(
                books = activeBooks,
                scheme = scheme,
                onBookClick = onBookClick,
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.READING_STATS -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val statsRepo = remember { com.example.readproplus.data.ReadingStatsRepository(context) }
            val annotationRepo = remember { com.example.readproplus.data.AnnotationRepository(context) }
            val highlightRepo = remember { com.example.readproplus.data.HighlightRepository(context) }
            val exporter = remember { com.example.readproplus.data.DataExporter(context) }

            com.example.readproplus.ui.screens.ReadingStatsScreen(
                stats = statsRepo.getStats(),
                scheme = scheme,
                onMenuClick = onMenuClick,
                onExportClick = {
                    val markdown = exporter.exportToMarkdown(
                        highlights = highlightRepo.getAll(),
                        notes = annotationRepo.getNotes(),
                        bookmarks = annotationRepo.getBookmarks(),
                        stats = statsRepo.getStats(),
                    )
                    exporter.shareExportData(markdown)
                },
            )
        }
        SidebarSection.TRASH -> {
            TrashScreen(
                trashBooks = libraryBooks.filter { it.id in trash },
                scheme = scheme,
                onBookClick = onBookClick,
                onRestore = { sidebarViewModel.restoreBook(it) },
                onEmptyTrash = { sidebarViewModel.emptyTrash() },
                onMenuClick = onMenuClick,
            )
        }
        SidebarSection.SETTINGS -> {
            SettingsScreen(
                scheme = scheme,
                readingMode = readingMode,
                onReadingModeChange = onReadingModeChange,
                scrollMode = scrollMode,
                onScrollModeChange = onScrollModeChange,
                ttsVoices = ttsVoices,
                selectedTtsVoice = selectedTtsVoice,
                voiceAvailability = voiceAvailability,
                voiceDownloadProgress = voiceDownloadProgress,
                onTtsVoiceSelected = onTtsVoiceSelected,
                onMenuClick = onMenuClick,
            )
        }
    }
}
