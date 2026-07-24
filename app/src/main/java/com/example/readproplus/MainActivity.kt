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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.ScrollMode
import com.example.readproplus.model.SidebarSection
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.tts.TtsState
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.ui.components.AppSidebar
import com.example.readproplus.ui.components.ModelDownloadDialog
import com.example.readproplus.ui.screens.AuthorsScreen
import com.example.readproplus.ui.screens.BooksAndDocumentsScreen
import com.example.readproplus.ui.screens.CitationsScreen
import com.example.readproplus.ui.screens.CollectionsScreen
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PdfExtractorViewModel = viewModel()
            val ttsViewModel: KokoroTtsViewModel = viewModel()
            val sidebarViewModel: SidebarViewModel = viewModel()

            var readingMode by remember { mutableStateOf(ReadingMode.SEPIA) }
            var scrollMode by remember { mutableStateOf(ScrollMode.PAGED) }
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
            var currentPage by remember { mutableIntStateOf(1) }
            var sidebarSection by remember { mutableStateOf(SidebarSection.BOOKS_AND_DOCUMENTS) }
            val scheme = readerColorScheme(readingMode)
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            val selectedDocument by viewModel.selectedDocument.collectAsState()
            val allHighlights by viewModel.highlights.collectAsState()
            val libraryBooks by viewModel.libraryBooks.collectAsState()
            val ttsState by ttsViewModel.ttsState.collectAsState()
            val downloadProgress by ttsViewModel.downloadProgress.collectAsState()
            val ttsVolume by ttsViewModel.volume.collectAsState()
            val selectedTtsVoice by ttsViewModel.selectedVoice.collectAsState()
            val voiceAvailability by ttsViewModel.voiceAvailability.collectAsState()
            val voiceDownloadProgress by ttsViewModel.voiceDownloadProgress.collectAsState()

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

            ReadProPlusTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            AppSidebar(
                                currentSection = sidebarSection,
                                onSectionSelected = { section ->
                                    sidebarSection = section
                                    currentScreen = Screen.SidebarScreen(section)
                                    scope.launch { drawerState.close() }
                                },
                                scheme = scheme,
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
                            is Screen.Library -> {
                                LibraryScreen(
                                    readingMode = readingMode,
                                    viewModel = viewModel,
                                    onBookClick = { navigateToBook(it) },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                )
                            }
                            is Screen.Reader -> {
                                ReaderScreen(
                                    document = selectedDocument,
                                    readingMode = readingMode,
                                    onReadingModeChange = { readingMode = it },
                                    scrollMode = scrollMode,
                                    onScrollModeChange = { scrollMode = it },
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
                                    ttsVoices = TtsVoice.ALL,
                                    selectedTtsVoice = selectedTtsVoice,
                                    voiceAvailability = voiceAvailability,
                                    voiceDownloadProgress = voiceDownloadProgress,
                                    onTtsStart = { text ->
                                        ttsViewModel.startReadingOrInitialize(text)
                                    },
                                    onTtsDurationStart = { startPageIndex, minutes, mainTextOnly ->
                                        val pages = selectedDocument?.pages ?: emptyList()
                                        ttsViewModel.startDurationReadingOrInitialize(
                                            pages = pages,
                                            startPageIndex = startPageIndex,
                                            targetMinutes = minutes,
                                            mainTextOnly = mainTextOnly,
                                        )
                                    },
                                    onTtsPause = { ttsViewModel.pauseReading() },
                                    onTtsResume = { ttsViewModel.resumeReading() },
                                    onTtsStop = { ttsViewModel.stopReading() },
                                    onTtsSeek = { progress -> ttsViewModel.seekTo(progress) },
                                    onTtsSpeedClick = { },
                                    onTtsDismiss = { ttsViewModel.stopReading() },
                                    onTtsSpeedSelected = { speed -> ttsViewModel.setSpeed(speed) },
                                    onTtsVolumeChanged = { volume -> ttsViewModel.setVolume(volume) },
                                    onTtsVoiceSelected = { voice -> ttsViewModel.selectVoice(voice) },
                                )
                                if (selectedDocument != null) {
                                    androidx.compose.runtime.LaunchedEffect(currentPage) {
                                        val doc = selectedDocument ?: return@LaunchedEffect
                                        sidebarViewModel.saveProgress(doc.id, doc.title, currentPage, doc.totalPages)
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
                                        viewModel.onBookSelected(
                                            PdfDocument(
                                                id = highlight.bookId,
                                                title = highlight.bookTitle,
                                                author = null,
                                                totalPages = 1,
                                                pages = emptyList(),
                                            )
                                        )
                                        currentPage = highlight.pageNumber
                                        currentScreen = Screen.Reader
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
                                onCancel = { ttsViewModel.cancelDownload() },
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen {
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
            )
        }
        SidebarSection.BOOKS_AND_DOCUMENTS -> {
            BooksAndDocumentsScreen(
                books = activeBooks,
                scheme = scheme,
                onBookClick = onBookClick,
            )
        }
        SidebarSection.FAVORITES -> {
            FavoritesScreen(
                books = activeBooks,
                favorites = favorites,
                scheme = scheme,
                onBookClick = onBookClick,
                onToggleFavorite = { sidebarViewModel.toggleFavorite(it) },
            )
        }
        SidebarSection.TO_READ -> {
            ToReadScreen(
                books = activeBooks,
                toRead = toRead,
                scheme = scheme,
                onBookClick = onBookClick,
                onToggleToRead = { sidebarViewModel.toggleToRead(it) },
            )
        }
        SidebarSection.HAVE_READ -> {
            HaveReadScreen(
                books = activeBooks,
                haveRead = haveRead,
                scheme = scheme,
                onBookClick = onBookClick,
            )
        }
        SidebarSection.AUTHORS -> {
            AuthorsScreen(
                authors = sidebarViewModel.getAuthors(activeBooks),
                scheme = scheme,
                onBookClick = onBookClick,
            )
        }
        SidebarSection.SERIES -> {
            SeriesScreen(
                seriesMap = sidebarViewModel.getSeries(activeBooks),
                scheme = scheme,
                onBookClick = onBookClick,
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
            )
        }
        SidebarSection.DOWNLOADS -> {
            DownloadsScreen(
                books = activeBooks,
                scheme = scheme,
                onBookClick = onBookClick,
            )
        }
        SidebarSection.TRASH -> {
            TrashScreen(
                trashBooks = libraryBooks.filter { it.id in trash },
                scheme = scheme,
                onBookClick = onBookClick,
                onRestore = { sidebarViewModel.restoreBook(it) },
                onEmptyTrash = { sidebarViewModel.emptyTrash() },
            )
        }
        SidebarSection.SETTINGS -> {
            SettingsScreen(
                scheme = scheme,
            )
        }
    }
}
