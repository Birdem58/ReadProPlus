# PDF Extractor — Full Implementation Plan

## 1. Overview

ReadProPlus currently displays hardcoded sample text in its reader. The PDF extractor will load real `.pdf` files from the device, extract their text content, and feed it into the existing `ReaderScreen` so users can read actual books.

---

## 2. Library Choice: `com.tom-roush:pdfbox-android`

| Criterion | Decision |
|---|---|
| **Library** | `com.tom-roush:pdfbox-android` (Android port of Apache PDFBox) |
| **License** | Apache 2.0 — compatible with closed-source apps |
| **Version** | `2.0.27.0` |
| **Why not iText?** | iText is AGPL — requires commercial license for closed-source distribution |
| **Why not MuPDF?** | Also AGPL; optimized for rendering, text extraction is secondary |
| **Why not platform PdfRenderer?** | API 21+ but only renders pages to bitmaps; no text extraction API |
| **Features needed** | Text extraction (`PDFTextStripper`), metadata reading, password support |

### Dependencies to add

**`gradle/libs.versions.toml`** — add version:

```toml
[versions]
pdfboxAndroid = "2.0.27.0"

[libraries]
pdfbox-android = { group = "com.tom-roush", name = "pdfbox-android", version.ref = "pdfboxAndroid" }
```

**`app/build.gradle.kts`** — add dependency:

```kotlin
dependencies {
    // ...
    implementation(libs.pdfbox.android)
}
```

---

## 3. New Files — Directory & Package Structure

All new code goes under the existing package `com.example.readproplus`.

### 3a. Data Layer — `model/pdf/`

| File | Purpose |
|---|---|
| `model/pdf/PdfDocument.kt` | Domain model for an opened PDF: title, author, total pages, extracted page texts |
| `model/pdf/PdfExtractionResult.kt` | Sealed class for extraction outcomes: `Success(text)`, `PasswordProtected`, `Corrupted(e)`, `TooLarge` |
| `model/pdf/PdfExtractionProgress.kt` | Data class tracking progress: `currentPage`, `totalPages`, `percent` |

### 3b. Extraction Engine — `pdf/`

| File | Purpose |
|---|---|
| `pdf/PdfExtractor.kt` | Core class that wraps `PDFBox` and extracts text synchronously per page |
| `pdf/PdfExtractorTask.kt` | Coroutine-based async wrapper that runs extraction on `Dispatchers.Default`, emits progress, handles cancellation |
| `pdf/PdfPasswordCache.kt` | Simple in-memory cache for PDF passwords per URI (cleared on app restart) |

### 3c. File Access — `pdf/`

| File | Purpose |
|---|---|
| `pdf/PdfFilePicker.kt` | Helper that launches `ACTION_OPEN_DOCUMENT` via `ActivityResultContracts.OpenDocument` to pick a PDF |
| `pdf/PdfFileResolver.kt` | Resolves a `Uri` to a `ParcelFileDescriptor` for the extractor, handles content URIs vs file URIs |

### 3d. Metadata — `pdf/`

| File | Purpose |
|---|---|
| `pdf/PdfMetadataReader.kt` | Reads `PDDocument.getDocumentInformation()` to extract title, author, subject, keywords |

### 3e. Repository — `data/`

| File | Purpose |
|---|---|
| `data/PdfRepository.kt` | Single source of truth for PDF operations. Coordinates file picker → resolver → extractor → caching. Exposes `Flow<AsyncState<PdfDocument>>` |

### 3f. Caching — `data/`

| File | Purpose |
|---|---|
| `data/BookCache.kt` | In-memory LRU cache mapping book ID → `PdfDocument`. Cache limit: 5 books (~50 MB budget). Evicts when used app goes to background via `LifecycleObserver`. |

### 3g. ViewModel — `ui/viewmodel/`

| File | Purpose |
|---|---|
| `ui/viewmodel/PdfExtractorViewModel.kt` | ViewModel for the library/reader flow. Holds `state: MutableStateFlow<PdfExtractorUiState>`. Handles pick → extract → load → navigate. |

### 3h. UI — `ui/screens/` and `ui/components/`

| File | Purpose |
|---|---|
| `ui/components/PdfLoadingIndicator.kt` | Composable showing extraction progress (page X of Y, animated linear progress bar) |
| `ui/components/PdfPasswordDialog.kt` | AlertDialog asking for PDF password, emits `onPasswordEntered(String)` |
| `ui/components/PdfErrorBanner.kt` | Snackbar-style banner for extraction errors (corrupted, too large, etc.) |
| `ui/screens/LibraryScreen.kt` | **Modified** — replace hardcoded `sampleBooks` with real books from repository; add "Add PDF" FAB that triggers file picker; each book card reads from cache or shows metadata |
| `ui/screens/ReaderScreen.kt` | **Modified** — replace `samplePages` with pages from the extracted `PdfDocument`; show page count and current page from actual PDF |

### 3i. Manifest

| File | Change |
|---|---|
| `AndroidManifest.xml` | Add `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />` (API 33+ uses granular media permissions; the SAF picker handles it anyway) |

---

## 4. Data Flow — End to End

```
User taps "Add PDF" FAB
  → PdfFilePicker launches SAF document picker (filter: application/pdf)
  → User selects a PDF file, returns content:// URI
  → PdfExtractorViewModel receives URI
  → ViewModel sets state = Loading(progress = 0%)
  → PdfExtractorTask.launch(uri) starts on Dispatchers.Default
      → PdfFileResolver opens ParcelFileDescriptor from URI
      → PdfExtractor opens PDDocument via PDDocument.load(descriptor)
      → PdfMetadataReader reads title, author from document info
      → For each page:
          - PDFTextStripper.getText(page) extracts text
          - Emits progress via _progress Flow
          - On cancellation (user backs out), calls document.close()
      → Returns PdfDocument(pages = List<String>, metadata = ...)
  → Repository caches the PdfDocument
  → ViewModel sets state = Loaded(document)
  → LibraryScreen shows book card for the new PDF
  → User taps the book card
  → ViewModel sets selectedDocument = document
  → Navigation switches to ReaderScreen
  → ReaderScreen displays pages from document.pages
```

---

## 5. Key Implementation Details

### 5a. `PdfExtractor.kt` — Core Text Extraction

```kotlin
class PdfExtractor(private val resolver: PdfFileResolver) {

    fun extract(uri: Uri, password: String? = null): PdfExtractionResult {
        val descriptor: ParcelFileDescriptor = resolver.resolve(uri)
        val doc: PDDocument = PDDocument.load(descriptor.fileDescriptor, password ?: "")
        if (doc.isEncrypted) return PdfExtractionResult.PasswordProtected

        val stripper = PDFTextStripper().apply {
            sortByPosition = true
            addMoreFormatting = true
            lineEnding = "\n"
        }

        val pages = mutableListOf<String>()
        for (i in 0 until doc.numberOfPages) {
            stripper.startPage = i + 1
            stripper.endPage = i + 1
            pages.add(stripper.getText(doc))
        }

        val metadata = PdfMetadataReader.read(doc)
        doc.close()
        return PdfExtractionResult.Success(
            PdfDocument(
                id = uri.toString(),
                title = metadata.title ?: "Untitled",
                author = metadata.author,
                totalPages = pages.size,
                pages = pages
            )
        )
    }
}
```

**Key behaviors:**
- `sortByPosition = true` — maintains reading order even on multi-column PDFs
- `addMoreFormatting = true` — inserts line breaks between paragraphs for readability
- If `isEncrypted` and no password supplied, return `PasswordProtected` to trigger the password dialog
- If password is wrong, `PDDocument.load()` throws `IOException` with "Invalid password" message
- Always close `PDDocument` in a `finally` block

### 5b. `PdfExtractorTask.kt` — Async with Progress

```kotlin
class PdfExtractorTask(
    private val extractor: PdfExtractor,
) {
    suspend fun launch(
        uri: Uri,
        password: String? = null,
    ): Flow<PdfExtractionProgress> = flow {
        // Use a sequential approach: extract one page at a time
        // and emit progress after each page
        val descriptor = resolver.resolve(uri)
        val doc = PDDocument.load(descriptor.fileDescriptor, password ?: "")
        val totalPages = doc.numberOfPages
        val stripper = PDFTextStripper().apply { ... }

        val extracted = mutableListOf<String>()
        for (i in 0 until totalPages) {
            ensureActive()  // support cancellation
            stripper.startPage = i + 1
            stripper.endPage = i + 1
            extracted.add(stripper.getText(doc))
            emit(PdfExtractionProgress(i + 1, totalPages, (i + 1).toFloat() / totalPages))
        }
        doc.close()
    }.flowOn(Dispatchers.Default)
}
```

### 5c. `PdfRepository.kt` — Central Coordination

```kotlin
class PdfRepository(
    private val extractor: PdfExtractor,
    private val task: PdfExtractorTask,
    private val cache: BookCache,
) {
    fun addBook(uri: Uri, password: String? = null): Flow<AsyncState<PdfDocument>> = flow {
        emit(AsyncState.Loading(0f))
        task.launch(uri, password).collect { progress ->
            emit(AsyncState.Loading(progress.percent))
        }
        // After task completes, finalize...
        val result = extractor.extract(uri, password)
        val doc = when (result) {
            is PdfExtractionResult.Success -> result.document
            is PdfExtractionResult.PasswordProtected -> ... // trigger dialog retry
            is PdfExtractionResult.Corrupted -> ... // emit error
            is PdfExtractionResult.TooLarge -> ... // emit error
        }
        cache.put(doc)
        emit(AsyncState.Success(doc))
    }

    fun getBook(id: String): PdfDocument? = cache.get(id)
    fun listBooks(): List<PdfDocument> = cache.all()
}
```

### 5d. `PdfExtractorViewModel.kt` — UI State

```kotlin
sealed interface PdfExtractorUiState {
    data object Idle : PdfExtractorUiState
    data class Loading(val progress: Float) : PdfExtractorUiState
    data class NeedsPassword(val uri: Uri) : PdfExtractorUiState
    data class Error(val message: String) : PdfExtractorUiState
    data class Success(val document: PdfDocument) : PdfExtractorUiState
}

class PdfExtractorViewModel(
    private val repository: PdfRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<PdfExtractorUiState>(PdfExtractorUiState.Idle)
    val state: StateFlow<PdfExtractorUiState> = _state.asStateFlow()

    private val _libraryBooks = MutableStateFlow<List<PdfDocument>>(emptyList())
    val libraryBooks: StateFlow<List<PdfDocument>> = _libraryBooks.asStateFlow()

    fun onPdfPicked(uri: Uri) { ... }
    fun onPasswordEntered(password: String) { ... }
    fun onBookSelected(document: PdfDocument) { ... }
    fun removeBook(id: String) { ... }
}
```

### 5e. ReaderScreen Integration

The existing `ReaderScreen` uses `samplePages: List<String>`. Replace this with:

```kotlin
@Composable
fun ReaderScreen(
    document: PdfDocument,
    // ... existing params ...
) {
    // document.pages replaces samplePages
    val currentPageText = document.pages[pageIndex]
    // Use document.totalPages for page indicator
}
```

### 5f. LibraryScreen Integration

- Replace `sampleBooks` hardcoded list with `viewModel.libraryBooks.collectAsState()`
- Add a `FloatingActionButton` with `Icons.Default.Add` that triggers `PdfFilePicker`
- Each book card shows real title/author from PDF metadata
- Long-press on a book card shows delete option

---

## 6. Threading & Concurrency

| Concern | Strategy |
|---|---|
| **PDF loading** | `Dispatchers.Default` — CPU-bound parsing |
| **Progress emission** | `Dispatchers.Main` — UI updates via Flow collection |
| **Cancellation** | `currentCoroutineContext().ensureActive()` checked before each page |
| **Lifecycle** | ViewModel scope (`viewModelScope.launch`) auto-cancels on screen rotation/exit |
| **Large files (>100 pages)** | Default: limit to first 100 pages with configurable max. Show warning. |

---

## 7. Memory Management

| Concern | Strategy |
|---|---|
| **Page text storage** | Store as `List<String>` — each page is a String. For a 300-page novel at ~2KB/page = ~600KB, well within limits |
| **PDFBox document** | `PDDocument` held only during extraction; closed immediately after. Never kept in memory. |
| **Cache limit** | LRU with max 5 documents. Eviction writes to temp file if needed. |
| **Bitmap rendering** | Not needed for text extraction; avoided entirely |
| **GC pressure** | Use `ReusableLineTextStripper` (PDFBox optimization) for very large documents |

---

## 8. Error Handling Matrix

| Error | Detection | User-facing message | Recovery |
|---|---|---|---|
| File not found | `FileNotFoundException` | "File not found" | Dismiss |
| Password protected | `PDDocument.isEncrypted` | "Enter password" | Retry with password dialog |
| Wrong password | `IOException("Invalid password")` | "Incorrect password" | Retry dialog |
| Corrupted PDF | `IOException` | "This PDF appears to be damaged" | Dismiss |
| Too large (>50 MB) | `ParcelFileDescriptor.statSize` | "File is too large (max 50 MB)" | Dismiss |
| No text in PDF (scanned) | All pages return empty strings | "This PDF has no selectable text (it may be a scanned document)" | Open as images via PdfRenderer (future) |
| Out of memory | `OutOfMemoryError` | "Not enough memory to process this PDF" | Reduce page limit |
| Permission denied | `SecurityException` | "Could not access this file" | Guide to permission settings |
| File deleted after pick | `IOException` | "File is no longer available" | Remove from library |

---

## 9. Permissions Strategy

| Android Version | Approach |
|---|---|
| **API 24 - 32** | `READ_EXTERNAL_STORAGE` permission requested via `ActivityResultContracts.RequestPermission` (not at install time). Use SAF as primary mechanism, fallback to direct file access. |
| **API 33+** | No storage permission needed. SAF (`ACTION_OPEN_DOCUMENT`) grants persistent read URI permission via `takePersistableUriPermission()`. |
| **All versions** | SAF is the primary mechanism — user picks files through the system picker. |

The key call in `PdfFilePicker`:
```kotlin
val pickPdf = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri: Uri? ->
    uri?.let { viewModel.onPdfPicked(it) }
}

// Grant persistent access
contentResolver.takePersistableUriPermission(uri, FLAG_READ_PERMISSION)
```

---

## 10. Testing Plan

| Test | Type | Location | What it covers |
|---|---|---|---|
| `PdfExtractorTest` | Unit | `app/src/test/` | Extract text from a known PDF; verify output matches expected; test password-protected, corrupted, empty PDFs |
| `PdfRepositoryTest` | Unit | `app/src/test/` | Cache put/get/evict; flow emission ordering |
| `PdfExtractorViewModelTest` | Unit | `app/src/test/` | State transitions: Idle → Loading → Success/Error; password retry flow |
| `BookCacheTest` | Unit | `app/src/test/` | LRU eviction order; max size enforcement; null on miss |
| `PdfExtractorInstrumentedTest` | Instrumented | `app/src/androidTest/` | SAF integration; content resolver; reading a real PDF from test assets |
| `LibraryScreenTest` | Compose UI | `app/src/androidTest/` | FAB click triggers picker; book cards show real data; delete works |
| `ReaderScreenTest` | Compose UI | `app/src/androidTest/` | Page navigation with real pages; page count display |

Test PDF files to include in `app/src/androidTest/assets/`:
- `sample-text.pdf` — 3 pages of lorem ipsum
- `password-1234.pdf` — password-protected with password "1234"
- `multi-column.pdf` — 2-column layout to verify `sortByPosition`

---

## 11. Implementation Order

### Phase 1 — Foundation (files: 3, lines: ~200)
1. Add pdfbox-android dependency to `libs.versions.toml` and `app/build.gradle.kts`
2. Create `PdfDocument.kt`, `PdfExtractionResult.kt`, `PdfExtractionProgress.kt`
3. Create `PdfFileResolver.kt`
4. Create `PdfExtractor.kt` with basic text extraction (single page)
5. Write unit tests for `PdfExtractor`

### Phase 2 — Async Pipeline (files: 4, lines: ~300)
6. Create `PdfExtractorTask.kt` with coroutine-based progress Flow
7. Create `PdfMetadataReader.kt`
8. Create `BookCache.kt` (LRU cache)
9. Create `PdfRepository.kt` coordinating extractor + task + cache
10. Write unit tests for repository and cache

### Phase 3 — Android Integration (files: 4, lines: ~250)
11. Create `PdfFilePicker.kt`
12. Create `PdfExtractorViewModel.kt`
13. Create `PdfPasswordDialog.kt`, `PdfLoadingIndicator.kt`, `PdfErrorBanner.kt`
14. Modify `LibraryScreen.kt` — add FAB, replace sampleBooks, wire to ViewModel
15. Modify `ReaderScreen.kt` — accept `PdfDocument` instead of `samplePages`
16. Update `AndroidManifest.xml` with storage permission (API < 33)

### Phase 4 — Polish (files: 2, lines: ~100)
17. Add `PdfPasswordCache.kt` for convenience
18. Handle edge cases: scanned PDFs (empty extraction), very large files, back-navigation during extraction
19. Instrumented tests with real PDFs
20. Final cleanup & lint

---

## 12. File Modification Summary

### Modified files:
| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `pdfboxAndroid` version + library entry |
| `app/build.gradle.kts` | Add `implementation(libs.pdfbox.android)` |
| `AndroidManifest.xml` | Add `READ_EXTERNAL_STORAGE` permission (maxSdk 32) |
| `MainActivity.kt` | Inject ViewModel, pass to screens |
| `ui/screens/LibraryScreen.kt` | Replace hardcoded data with real books, add FAB, wire picker |
| `ui/screens/ReaderScreen.kt` | Accept `PdfDocument` instead of `samplePages` |

### New files (17 total):
| # | Path | Lines (est.) |
|---|---|---|
| 1 | `model/pdf/PdfDocument.kt` | 15 |
| 2 | `model/pdf/PdfExtractionResult.kt` | 25 |
| 3 | `model/pdf/PdfExtractionProgress.kt` | 10 |
| 4 | `pdf/PdfExtractor.kt` | 80 |
| 5 | `pdf/PdfExtractorTask.kt` | 60 |
| 6 | `pdf/PdfPasswordCache.kt` | 30 |
| 7 | `pdf/PdfFilePicker.kt` | 40 |
| 8 | `pdf/PdfFileResolver.kt` | 35 |
| 9 | `pdf/PdfMetadataReader.kt` | 30 |
| 10 | `data/PdfRepository.kt` | 80 |
| 11 | `data/BookCache.kt` | 60 |
| 12 | `ui/viewmodel/PdfExtractorViewModel.kt` | 100 |
| 13 | `ui/components/PdfLoadingIndicator.kt` | 30 |
| 14 | `ui/components/PdfPasswordDialog.kt` | 50 |
| 15 | `ui/components/PdfErrorBanner.kt` | 25 |
|| **Total new** | **~670 lines** |

---

## 13. Architectural Diagram

```
┌─────────────────────────────────────────────────────┐
│                   UI Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │ LibraryScreen │  │ ReaderScreen │  │  Dialogs   ││
│  │  (modified)   │  │  (modified)  │  │(password,  ││
│  │               │  │              │  │ error, etc)││
│  └──────┬───────┘  └──────┬───────┘  └────────────┘│
│         │                 │                          │
│  ┌──────┴─────────────────┴──────────────────────┐  │
│  │        PdfExtractorViewModel                  │  │
│  │   (StateFlow<PdfExtractorUiState>)            │  │
│  └───────────────────┬──────────────────────────┘  │
├──────────────────────┼──────────────────────────────┤
│            Domain    │                              │
│  ┌───────────────────┴──────────────────────────┐  │
│  │            PdfRepository                     │  │
│  │  (orchestrates extraction + caching + flow)  │  │
│  └──┬──────────────┬──────────────┬─────────────┘  │
│     │              │              │                 │
│  ┌──┴───┐    ┌─────┴─────┐  ┌────┴─────┐          │
│  │Cache │    │Extractor  │  │File      │          │
│  │  +   │    │  Task     │  │Resolver  │          │
│  │Meta  │    │(coroutine)│  │(SAF)     │          │
│  └──────┘    └─────┬─────┘  └──────────┘          │
│                    │                                │
│  ┌─────────────────┴──────────────────────────┐  │
│  │           PdfExtractor                     │  │
│  │   (PDFBox PDDocument + PDFTextStripper)    │  │
│  └────────────────────────────────────────────┘  │
├───────────────────────────────────────────────────┤
│              Android Platform                     │
│  ┌────────────────────────────────────────────┐  │
│  │ StorageAccessFramework | ContentResolver   │  │
│  │   → ParcelFileDescriptor → PDFBox          │  │
│  └────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 14. Future Considerations (Not in Scope for V1)

- **Scanned PDFs**: OCR via Google ML Kit Text Recognition (requires `com.google.mlkit:text-recognition`)
- **PDF rendering** (page images): `android.graphics.pdf.PdfRenderer` or `com.github.barteksc:android-pdf-viewer`
- **Bookmarks / highlights**: Room database for per-book reading state
- **Search within PDF**: `PDFTextStripper` with `getText()` → simple `indexOf()` / regex search
- **Table of contents**: `PDDocumentOutline` parsing for navigation chapters
- **EPUB support**: Separate extractor with same `PdfDocument` interface
- **Batch import**: Scan a directory for all PDFs
- **Background sync**: Download PDFs from cloud storage

---

## 15. Verification Checklist

- [ ] pdfbox-android compiles without errors on minSdk 24
- [ ] `PdfExtractor` extracts text from a simple text PDF correctly
- [ ] `PdfExtractor` detects password-protected PDFs and returns `PasswordProtected`
- [ ] `PdfExtractor` handles wrong password gracefully
- [ ] `PdfExtractorTask` emits progress as pages are extracted
- [ ] `PdfExtractorTask` can be cancelled mid-extraction
- [ ] `BookCache` evicts oldest entry when at capacity
- [ ] `PdfFilePicker` filters to `application/pdf`
- [ ] `PdfFileResolver` resolves both `content://` and `file://` URIs
- [ ] `PdfRepository` exposes correct `Flow` state transitions
- [ ] `PdfExtractorViewModel` state machine works: Idle → Loading(with progress) → Success/Error
- [ ] LibraryScreen displays real book title/author from PDF metadata
- [ ] ReaderScreen displays extracted pages with correct page count
- [ ] Reading mode themes (Sepia, Dark, etc.) work with extracted text
- [ ] Back-navigation during extraction cancels the coroutine
- [ ] Android API 24 device runs the app without crashes
- [ ] Android API 36 device runs the app without warnings
- [ ] No `OutOfMemoryError` for a 300-page PDF
- [ ] Unit tests pass for extractor, repository, ViewModel
- [ ] Instrumented tests pass for library screen interaction
- [ ] R8/proguard rules for PDFBox are added (PDFBox uses reflection)
