package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BookAiService
import com.example.data.model.AppThemeMode
import com.example.data.model.BookItem
import com.example.data.model.BookmarkItem
import com.example.data.model.BookshelfFilter
import com.example.data.model.BookshelfSort
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReaderViewMode
import com.example.data.model.ReadingLogItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WorldwideLanguages
import com.example.data.pdf.PdfPageSession
import com.example.data.pdf.PdfStorageManager
import com.example.data.repository.BookshelfRepository
import com.example.data.repository.ThemePreferencesStore
import com.example.ui.theme.BookSpinePalette
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface SummaryUiState {
    data object Idle : SummaryUiState
    data object Loading : SummaryUiState
    data class Success(val text: String, val languageTag: String) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}

sealed interface AudioPrepUiState {
    data object Idle : AudioPrepUiState
    data object Loading : AudioPrepUiState
    data class Ready(val pageIndex: Int, val text: String, val languageTag: String) : AudioPrepUiState
    data class Error(val message: String) : AudioPrepUiState
}

sealed interface PageLinesUiState {
    data object Idle : PageLinesUiState
    data object Loading : PageLinesUiState
    data class Success(val pageIndex: Int, val lines: List<String>) : PageLinesUiState
    data class Error(val message: String) : PageLinesUiState
}

sealed interface LineTranslationUiState {
    data object Idle : LineTranslationUiState
    data object Loading : LineTranslationUiState
    data class Success(val translatedText: String, val languageTag: String) : LineTranslationUiState
    data class Error(val message: String) : LineTranslationUiState
}

class BookshelfViewModel(
    private val repository: BookshelfRepository,
    private val pdfStorage: PdfStorageManager,
    private val themeStore: ThemePreferencesStore,
    val currentUserId: String,
    private val aiService: BookAiService = BookAiService()
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapFirestoreErrorOrRethrow(error, "observeUserProfile")
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val booksState: StateFlow<UiState<List<BookItem>>> = repository.observeBooks(currentUserId)
        .map<List<BookItem>, UiState<List<BookItem>>> { books ->
            UiState.Success(books)
        }
        .catch { error ->
            val msg = unwrapFirestoreErrorOrRethrow(error, "observeBooks")
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val bookmarksState: StateFlow<UiState<List<BookmarkItem>>> = repository.observeBookmarks(currentUserId)
        .map<List<BookmarkItem>, UiState<List<BookmarkItem>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { error ->
            val msg = unwrapFirestoreErrorOrRethrow(error, "observeBookmarks")
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val readingLogsState: StateFlow<UiState<List<ReadingLogItem>>> = repository.observeReadingLogs(currentUserId)
        .map<List<ReadingLogItem>, UiState<List<ReadingLogItem>>> { list ->
            UiState.Success(list.sortedBy { it.dateKey })
        }
        .catch { error ->
            val msg = unwrapFirestoreErrorOrRethrow(error, "observeReadingLogs")
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val preferredLanguageTag: StateFlow<String> = themeStore.languageTagFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = "en"
        )

    val readerViewMode: StateFlow<ReaderViewMode> = themeStore.readerViewModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ReaderViewMode.PAGE_SCROLL
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(BookshelfFilter.ALL)
    val selectedFilter: StateFlow<BookshelfFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(BookshelfSort.RECENT)
    val selectedSort: StateFlow<BookshelfSort> = _selectedSort.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _summaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val summaryState: StateFlow<SummaryUiState> = _summaryState.asStateFlow()

    private val _audioPrepState = MutableStateFlow<AudioPrepUiState>(AudioPrepUiState.Idle)
    val audioPrepState: StateFlow<AudioPrepUiState> = _audioPrepState.asStateFlow()

    private val _pageLinesState = MutableStateFlow<PageLinesUiState>(PageLinesUiState.Idle)
    val pageLinesState: StateFlow<PageLinesUiState> = _pageLinesState.asStateFlow()

    private val _lineTranslationState = MutableStateFlow<LineTranslationUiState>(LineTranslationUiState.Idle)
    val lineTranslationState: StateFlow<LineTranslationUiState> = _lineTranslationState.asStateFlow()

    private val audioTextCache = mutableMapOf<String, String>()
    private val pageLinesCache = mutableMapOf<String, List<String>>()

    private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val uiMessages = _uiMessages.asSharedFlow()

    val filteredBooksState: StateFlow<UiState<List<BookItem>>> = combine(
        booksState,
        searchQuery,
        selectedFilter,
        selectedSort
    ) { state, query, filter, sort ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val filtered = applyFilterAndSort(state.data, query, filter, sort)
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val archivedBooksState: StateFlow<UiState<List<BookItem>>> = booksState.map { state ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> UiState.Success(
                state.data.filter { it.archived }
                    .sortedByDescending { it.updatedAt?.seconds ?: it.lastReadAt?.seconds ?: 0L }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val continueReadingBook: StateFlow<BookItem?> = booksState.map { state ->
        val books = ((state as? UiState.Success)?.data ?: return@map null).filter { !it.archived }
        val activeUnfinished = books
            .filter { !it.completed && it.currentPageInt > 0 }
            .maxByOrNull { it.lastReadAt?.seconds ?: it.updatedAt?.seconds ?: 0L }
        activeUnfinished ?: books.maxByOrNull { it.lastReadAt?.seconds ?: it.createdAt?.seconds ?: 0L }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            val savedLang = runCatching { themeStore.languageTagFlow.first() }.getOrDefault("en")
            val pendingName = runCatching { themeStore.pendingProfileNameFlow.first() }.getOrNull()
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(
                    user = user,
                    defaultLanguageTag = savedLang,
                    customDisplayName = pendingName
                )
                if (pendingName != null) {
                    themeStore.setPendingProfileName(null)
                }
            }
        }
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    themeStore.setAppTheme(profile.parsedAppTheme)
                    themeStore.setReaderTheme(profile.parsedReaderTheme)
                    themeStore.setPreferredLanguageTag(profile.preferredLanguageTag)
                    themeStore.setReaderViewMode(profile.parsedReaderViewMode)
                }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val clean = newName.trim().take(80)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfileName(currentUserId, clean).fold(
                onSuccess = { _uiMessages.tryEmit("Profile name updated to $clean") },
                onFailure = { err -> _uiMessages.tryEmit(err.localizedMessage ?: "Could not update profile name") }
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: BookshelfFilter) {
        _selectedFilter.value = filter
    }

    fun setSort(sort: BookshelfSort) {
        _selectedSort.value = sort
    }

    fun hasLocalPdf(bookId: String): Boolean = pdfStorage.hasLocalPdf(currentUserId, bookId)

    fun getLocalPdfFile(bookId: String): File = pdfStorage.getPdfFile(currentUserId, bookId)

    fun getCoverFile(bookId: String): File? {
        val file = pdfStorage.getCoverFile(currentUserId, bookId)
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun resetBookMediaStates() {
        _summaryState.value = SummaryUiState.Idle
        _audioPrepState.value = AudioPrepUiState.Idle
        _pageLinesState.value = PageLinesUiState.Idle
        _lineTranslationState.value = LineTranslationUiState.Idle
    }

    fun generateBookSummary(
        book: BookItem,
        languageTag: String = preferredLanguageTag.value,
        forceRefresh: Boolean = false
    ) {
        val existing = book.summary?.trim()
        val existingLang = book.summaryLanguage
        if (!forceRefresh && !existing.isNullOrBlank() && existingLang.equals(languageTag, ignoreCase = true)) {
            _summaryState.value = SummaryUiState.Success(existing, languageTag)
            return
        }
        if (_summaryState.value is SummaryUiState.Loading) return

        viewModelScope.launch {
            _summaryState.value = SummaryUiState.Loading
            val pdfFile = getLocalPdfFile(book.id).takeIf { it.exists() }
            val fallbackBitmaps = mutableListOf<Bitmap>()
            if (pdfFile != null && pdfFile.length() > 12_000_000L) {
                val session = PdfPageSession(pdfFile)
                runCatching {
                    val count = session.open()
                    for (idx in 0 until count.coerceAtMost(3)) {
                        session.renderPage(idx, 900)?.let { fallbackBitmaps.add(it) }
                    }
                }
                session.close()
            }

            val languageOption = WorldwideLanguages.findByTag(languageTag)
            aiService.generateSmallSummary(
                pdfFile = pdfFile,
                bookTitle = book.title,
                author = book.author,
                targetLanguage = languageOption,
                fallbackPageBitmaps = fallbackBitmaps
            ).fold(
                onSuccess = { summaryText ->
                    _summaryState.value = SummaryUiState.Success(summaryText, languageOption.tag)
                    repository.saveBookSummary(
                        userId = currentUserId,
                        bookId = book.id,
                        summary = summaryText,
                        summaryLanguage = languageOption.tag
                    )
                },
                onFailure = { err ->
                    _summaryState.value = SummaryUiState.Error(
                        err.localizedMessage ?: "Unable to generate book summary right now."
                    )
                }
            )
        }
    }

    fun preparePageAudio(
        book: BookItem,
        pageIndex: Int,
        pageBitmap: Bitmap,
        languageTag: String = preferredLanguageTag.value
    ) {
        val cacheKey = "${book.id}:$pageIndex:${languageTag.lowercase()}"
        val cached = audioTextCache[cacheKey]
        if (cached != null) {
            _audioPrepState.value = AudioPrepUiState.Ready(
                pageIndex = pageIndex,
                text = cached,
                languageTag = languageTag
            )
            return
        }
        if (_audioPrepState.value is AudioPrepUiState.Loading) return
        viewModelScope.launch {
            _audioPrepState.value = AudioPrepUiState.Loading
            val languageOption = WorldwideLanguages.findByTag(languageTag)
            aiService.extractPageTextForAudio(
                pageBitmap = pageBitmap,
                pageNumber = pageIndex + 1,
                targetLanguage = languageOption
            ).fold(
                onSuccess = { spokenText ->
                    audioTextCache[cacheKey] = spokenText
                    _audioPrepState.value = AudioPrepUiState.Ready(
                        pageIndex = pageIndex,
                        text = spokenText,
                        languageTag = languageOption.tag
                    )
                },
                onFailure = { err ->
                    _audioPrepState.value = AudioPrepUiState.Error(
                        err.localizedMessage ?: "Could not prepare audio for page ${pageIndex + 1}."
                    )
                }
            )
        }
    }

    fun prewarmPageAudio(
        bookId: String,
        pageIndex: Int,
        pageBitmap: Bitmap,
        languageTag: String = preferredLanguageTag.value
    ) {
        val cacheKey = "$bookId:$pageIndex:${languageTag.lowercase()}"
        if (audioTextCache.containsKey(cacheKey)) return
        viewModelScope.launch {
            val languageOption = WorldwideLanguages.findByTag(languageTag)
            aiService.extractPageTextForAudio(
                pageBitmap = pageBitmap,
                pageNumber = pageIndex + 1,
                targetLanguage = languageOption
            ).onSuccess { spokenText ->
                audioTextCache[cacheKey] = spokenText
            }
        }
    }

    fun loadPageLines(
        bookId: String,
        pageIndex: Int,
        pageBitmap: Bitmap,
        forceRefresh: Boolean = false
    ) {
        val cacheKey = "$bookId:$pageIndex"
        if (!forceRefresh) {
            val cached = pageLinesCache[cacheKey]
            if (cached != null) {
                _pageLinesState.value = PageLinesUiState.Success(pageIndex, cached)
                return
            }
        }
        if (_pageLinesState.value is PageLinesUiState.Loading) return
        viewModelScope.launch {
            _pageLinesState.value = PageLinesUiState.Loading
            _lineTranslationState.value = LineTranslationUiState.Idle
            aiService.extractPageLines(
                pageBitmap = pageBitmap,
                pageNumber = pageIndex + 1
            ).fold(
                onSuccess = { lines ->
                    pageLinesCache[cacheKey] = lines
                    _pageLinesState.value = PageLinesUiState.Success(pageIndex, lines)
                },
                onFailure = { err ->
                    _pageLinesState.value = PageLinesUiState.Error(
                        err.localizedMessage ?: "Could not extract lines on page ${pageIndex + 1}."
                    )
                }
            )
        }
    }

    fun translateSelectedLines(
        lines: List<String>,
        languageTag: String = preferredLanguageTag.value
    ) {
        if (lines.isEmpty()) return
        viewModelScope.launch {
            _lineTranslationState.value = LineTranslationUiState.Loading
            val targetLanguage = WorldwideLanguages.findByTag(languageTag)
            aiService.translateLines(
                lines = lines,
                targetLanguage = targetLanguage
            ).fold(
                onSuccess = { translated ->
                    _lineTranslationState.value = LineTranslationUiState.Success(
                        translatedText = translated,
                        languageTag = targetLanguage.tag
                    )
                },
                onFailure = { err ->
                    _lineTranslationState.value = LineTranslationUiState.Error(
                        err.localizedMessage ?: "Translation failed."
                    )
                }
            )
        }
    }

    fun clearLineTranslation() {
        _lineTranslationState.value = LineTranslationUiState.Idle
    }

    fun setPreferredLanguage(tag: String) {
        val normalized = WorldwideLanguages.findByTag(tag).tag
        updatePreferences(preferredLanguageTag = normalized)
    }

    fun setReaderViewMode(mode: ReaderViewMode) {
        updatePreferences(readerViewMode = mode)
    }

    fun importPdf(uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            val bookId = "bk_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val importResult = pdfStorage.importPdfFromUri(currentUserId, bookId, uri)
            importResult.fold(
                onSuccess = { imported ->
                    val spineColor = BookSpinePalette[kotlin.math.abs(imported.title.hashCode()) % BookSpinePalette.size]
                    val saveResult = repository.addBook(
                        userId = currentUserId,
                        bookId = imported.bookId,
                        title = imported.title,
                        author = imported.author,
                        pageCount = imported.pageCount,
                        coverColorHex = spineColor,
                        fileName = imported.fileName,
                        fileSizeBytes = imported.fileSizeBytes
                    )
                    saveResult.fold(
                        onSuccess = {
                            _uiMessages.tryEmit("Added \"${imported.title}\" (${imported.pageCount} pages)")
                        },
                        onFailure = { err ->
                            pdfStorage.deleteLocalBookFiles(currentUserId, bookId)
                            _uiMessages.tryEmit(err.localizedMessage ?: "Failed to save book to cloud")
                        }
                    )
                },
                onFailure = { err ->
                    _uiMessages.tryEmit(err.localizedMessage ?: "Could not import PDF file")
                }
            )
            _isImporting.value = false
        }
    }

    fun relinkPdf(bookId: String, uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            val importResult = pdfStorage.importPdfFromUri(currentUserId, bookId, uri)
            importResult.fold(
                onSuccess = { imported ->
                    repository.updateBookFileMetadata(
                        userId = currentUserId,
                        bookId = bookId,
                        pageCount = imported.pageCount,
                        fileName = imported.fileName,
                        fileSizeBytes = imported.fileSizeBytes
                    ).fold(
                        onSuccess = { _uiMessages.tryEmit("PDF re-linked and ready to read") },
                        onFailure = { err -> _uiMessages.tryEmit(err.localizedMessage ?: "Failed to update book metadata") }
                    )
                },
                onFailure = { err ->
                    _uiMessages.tryEmit(err.localizedMessage ?: "Could not open selected PDF")
                }
            )
            _isImporting.value = false
        }
    }

    fun loadSampleBook() {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            val bookId = "bk_sample_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            pdfStorage.generateSamplePdfBook(currentUserId, bookId).fold(
                onSuccess = { imported ->
                    repository.addBook(
                        userId = currentUserId,
                        bookId = imported.bookId,
                        title = imported.title,
                        author = imported.author,
                        pageCount = imported.pageCount,
                        coverColorHex = BookSpinePalette[0],
                        fileName = imported.fileName,
                        fileSizeBytes = imported.fileSizeBytes
                    ).fold(
                        onSuccess = { _uiMessages.tryEmit("Sample PDF added to your shelf") },
                        onFailure = { err ->
                            pdfStorage.deleteLocalBookFiles(currentUserId, bookId)
                            _uiMessages.tryEmit(err.localizedMessage ?: "Failed to save sample book")
                        }
                    )
                },
                onFailure = { err ->
                    _uiMessages.tryEmit(err.localizedMessage ?: "Failed to generate sample PDF")
                }
            )
            _isImporting.value = false
        }
    }

    fun saveReadingProgress(book: BookItem, newPageIndex: Int, pagesTurnedDelta: Int) {
        viewModelScope.launch {
            repository.updateReadingProgress(
                userId = currentUserId,
                book = book,
                newPageIndex = newPageIndex,
                pagesTurnedDelta = pagesTurnedDelta,
                profile = userProfile.value
            )
        }
    }

    fun toggleFavorite(book: BookItem) {
        viewModelScope.launch {
            repository.toggleFavorite(currentUserId, book.id, !book.favorite).onFailure { err ->
                _uiMessages.tryEmit(err.localizedMessage ?: "Could not update favorite")
            }
        }
    }

    fun toggleArchive(book: BookItem) {
        val nextArchived = !book.archived
        viewModelScope.launch {
            repository.toggleArchive(currentUserId, book.id, nextArchived).fold(
                onSuccess = {
                    _uiMessages.tryEmit(
                        if (nextArchived) "Moved \"${book.title}\" to Archive"
                        else "Restored \"${book.title}\" to Bookshelf"
                    )
                },
                onFailure = { err ->
                    _uiMessages.tryEmit(err.localizedMessage ?: "Could not update archive status")
                }
            )
        }
    }

    fun updateBookDetails(bookId: String, title: String, author: String) {
        viewModelScope.launch {
            repository.updateBookDetails(currentUserId, bookId, title, author).fold(
                onSuccess = { _uiMessages.tryEmit("Book details updated") },
                onFailure = { err -> _uiMessages.tryEmit(err.localizedMessage ?: "Update failed") }
            )
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(currentUserId, bookId).fold(
                onSuccess = {
                    pdfStorage.deleteLocalBookFiles(currentUserId, bookId)
                    _uiMessages.tryEmit("Removed book from shelf")
                },
                onFailure = { err -> _uiMessages.tryEmit(err.localizedMessage ?: "Delete failed") }
            )
        }
    }

    fun addBookmark(book: BookItem, pageIndex: Int, note: String?) {
        viewModelScope.launch {
            repository.addBookmark(
                userId = currentUserId,
                bookId = book.id,
                bookTitle = book.title,
                pageIndex = pageIndex,
                note = note
            ).fold(
                onSuccess = { _uiMessages.tryEmit("Bookmarked page ${pageIndex + 1}") },
                onFailure = { err -> _uiMessages.tryEmit(err.localizedMessage ?: "Could not save bookmark") }
            )
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            repository.deleteBookmark(currentUserId, bookmarkId).fold(
                onSuccess = { _uiMessages.tryEmit("Bookmark removed") },
                onFailure = { err -> _uiMessages.tryEmit(err.localizedMessage ?: "Could not remove bookmark") }
            )
        }
    }

    fun updatePreferences(
        dailyGoalPages: Int = userProfile.value?.dailyGoalPages?.toInt() ?: 20,
        reminderEnabled: Boolean = userProfile.value?.reminderEnabled ?: false,
        reminderHour: Int = userProfile.value?.reminderHour?.toInt() ?: 20,
        reminderMinute: Int = userProfile.value?.reminderMinute?.toInt() ?: 0,
        appTheme: AppThemeMode = userProfile.value?.parsedAppTheme ?: AppThemeMode.SYSTEM,
        readerTheme: ReaderThemeMode = userProfile.value?.parsedReaderTheme ?: ReaderThemeMode.LIGHT,
        preferredLanguageTag: String = this.preferredLanguageTag.value,
        readerViewMode: ReaderViewMode = this.readerViewMode.value
    ) {
        viewModelScope.launch {
            themeStore.setAppTheme(appTheme)
            themeStore.setReaderTheme(readerTheme)
            themeStore.setPreferredLanguageTag(preferredLanguageTag)
            themeStore.setReaderViewMode(readerViewMode)
            repository.updatePreferences(
                userId = currentUserId,
                dailyGoalPages = dailyGoalPages,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                appTheme = appTheme,
                readerTheme = readerTheme,
                preferredLanguageTag = preferredLanguageTag,
                readerViewMode = readerViewMode
            ).onFailure { err ->
                _uiMessages.tryEmit(err.localizedMessage ?: "Could not sync settings")
            }
        }
    }

    private fun unwrapFirestoreErrorOrRethrow(error: Throwable, op: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error in $op", rootError)
        return rootError.localizedMessage ?: "Unable to sync with library"
    }

    companion object {
        private const val TAG = "BookshelfVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun applyFilterAndSort(
            books: List<BookItem>,
            query: String,
            filter: BookshelfFilter,
            sort: BookshelfSort
        ): List<BookItem> {
            val trimmed = query.trim().lowercase()
            val searched = if (trimmed.isEmpty()) books else {
                books.filter {
                    it.title.lowercase().contains(trimmed) ||
                        it.author.lowercase().contains(trimmed)
                }
            }
            val filtered = when (filter) {
                BookshelfFilter.ALL -> searched.filter { !it.archived }
                BookshelfFilter.READING -> searched.filter { !it.archived && !it.completed && it.currentPageInt > 0 }
                BookshelfFilter.COMPLETED -> searched.filter { !it.archived && it.completed }
                BookshelfFilter.FAVORITES -> searched.filter { !it.archived && it.favorite }
                BookshelfFilter.UNREAD -> searched.filter { !it.archived && !it.completed && it.currentPageInt == 0 }
                BookshelfFilter.ARCHIVED -> searched.filter { it.archived }
            }
            return when (sort) {
                BookshelfSort.RECENT -> filtered.sortedByDescending {
                    it.lastReadAt?.seconds ?: it.createdAt?.seconds ?: 0L
                }
                BookshelfSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
                BookshelfSort.PROGRESS -> filtered.sortedByDescending { it.progressFraction }
            }
        }
    }
}
