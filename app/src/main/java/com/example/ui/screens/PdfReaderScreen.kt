package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.BookAudioPlayer
import com.example.data.model.BookItem
import com.example.data.model.BookmarkItem
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReaderViewMode
import com.example.data.model.UiState
import com.example.data.model.WorldwideLanguages
import com.example.data.pdf.PageWordToken
import com.example.data.pdf.PdfPageSession
import com.example.ui.theme.ReaderChromeNavy
import com.example.ui.theme.ReaderChromeOnSurface
import com.example.ui.theme.ReaderChromeSurface
import com.example.ui.theme.ReaderInkDark
import com.example.ui.theme.ReaderInkLight
import com.example.ui.theme.ReaderInkSepia
import com.example.ui.theme.ReaderPaperDark
import com.example.ui.theme.ReaderPaperLight
import com.example.ui.theme.ReaderPaperSepia
import com.example.ui.viewmodel.BookshelfViewModel
import com.example.ui.viewmodel.LineTranslationUiState
import com.example.ui.viewmodel.PageLinesUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private data class ActivePageSelection(
    val pageIndex: Int,
    val tokens: List<PageWordToken>,
    val startIndex: Int,
    val endIndex: Int
) {
    val selectedText: String
        get() {
            if (tokens.isEmpty()) return ""
            val from = startIndex.coerceIn(0, tokens.lastIndex)
            val to = endIndex.coerceIn(from, tokens.lastIndex)
            return tokens.subList(from, to + 1).joinToString(" ") { it.text }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    book: BookItem,
    initialPageOverride: Int?,
    viewModel: BookshelfViewModel,
    onClose: () -> Unit,
    onSwitchToPlay: (Int) -> Unit = {},
    onOpenLanguagePicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val bookmarksUiState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val savedViewMode by viewModel.readerViewMode.collectAsStateWithLifecycle()
    val preferredLanguageTag by viewModel.preferredLanguageTag.collectAsStateWithLifecycle()
    val pageLinesState by viewModel.pageLinesState.collectAsStateWithLifecycle()
    val lineTranslationState by viewModel.lineTranslationState.collectAsStateWithLifecycle()

    val bookBookmarks = remember(bookmarksUiState, book.id) {
        (bookmarksUiState as? UiState.Success)?.data
            ?.filter { it.bookId == book.id }
            ?.sortedBy { it.pageIndex }
            ?: emptyList()
    }

    var readerTheme by remember(profile?.readerTheme) {
        mutableStateOf(profile?.parsedReaderTheme ?: ReaderThemeMode.LIGHT)
    }
    var readerViewMode by remember(savedViewMode) {
        mutableStateOf(savedViewMode)
    }

    val pdfFile = remember(book.id) { viewModel.getLocalPdfFile(book.id) }
    val session = remember(pdfFile.absolutePath) { PdfPageSession(pdfFile) }

    var pageCount by remember { mutableIntStateOf(book.totalPagesInt) }
    var currentPageIndex by remember(book.id, initialPageOverride) {
        mutableIntStateOf(
            (initialPageOverride ?: book.currentPageInt).coerceIn(0, (book.totalPagesInt - 1).coerceAtLeast(0))
        )
    }
    var sessionPagesTurned by remember { mutableIntStateOf(0) }
    var lastSavedPage by remember { mutableIntStateOf(currentPageIndex) }

    val scrollListState = rememberLazyListState(initialFirstVisibleItemIndex = currentPageIndex)

    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var preferTwoPageSpread by remember { mutableStateOf(true) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showLineTranslatorSheet by remember { mutableStateOf(false) }
    var lineInspectorPageIndex by remember { mutableIntStateOf(currentPageIndex) }
    var pendingLongPressYFraction by remember { mutableStateOf<Float?>(null) }
    val haptic = LocalHapticFeedback.current

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val flipProgress = remember { Animatable(0f) }
    var flipDirection by remember { mutableIntStateOf(0) } // +1 next, -1 prev, 0 idle
    var isAnimatingTurn by remember { mutableStateOf(false) }

    val renderedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val pageCoordinates = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    var flipContainerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var activeSelection by remember { mutableStateOf<ActivePageSelection?>(null) }
    var showInlineTranslationCard by remember { mutableStateOf(false) }
    var commentTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var inPageFeedback by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val inlineTranslationAudio = remember(context) { BookAudioPlayer(context) }

    LaunchedEffect(inPageFeedback) {
        if (inPageFeedback != null) {
            delay(2200L)
            inPageFeedback = null
        }
    }

    LaunchedEffect(preferredLanguageTag, showInlineTranslationCard) {
        val currentSel = activeSelection
        if (showInlineTranslationCard && currentSel != null) {
            val text = currentSel.selectedText
            if (text.isNotBlank()) {
                viewModel.translateSelectedLines(listOf(text), preferredLanguageTag)
            }
        }
    }

    fun launchWebSearch(queryText: String) {
        val cleaned = queryText.trim().take(240)
        if (cleaned.isBlank()) return
        val url = "https://www.google.com/search?q=${Uri.encode(cleaned)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            inPageFeedback = "No browser app found for Web search"
        }
    }

    fun openLineInspectorForPage(targetPage: Int, yFraction: Float?, forceRefresh: Boolean = false) {
        val safePage = targetPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        lineInspectorPageIndex = safePage
        pendingLongPressYFraction = yFraction
        showLineTranslatorSheet = true
        scope.launch {
            val bmp = renderedBitmaps[safePage]
                ?: session.renderPage(safePage, 1080)?.also {
                    renderedBitmaps[safePage] = it
                }
            if (bmp != null) {
                viewModel.loadPageLines(book.id, safePage, bmp, forceRefresh = forceRefresh)
            }
        }
    }

    fun startInPageSelection(
        targetPage: Int,
        localOffset: Offset,
        sheetSize: IntSize
    ) {
        val safePage = targetPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        scope.launch {
            val bmp = renderedBitmaps[safePage]
                ?: session.renderPage(safePage, 1080)?.also {
                    renderedBitmaps[safePage] = it
                }
            if (bmp == null) {
                openLineInspectorForPage(safePage, null)
                return@launch
            }
            val tokens = viewModel.getOrExtractPageWords(book.id, safePage, bmp)
            if (tokens.isEmpty()) {
                val yFrac = if (sheetSize.height > 0) {
                    (localOffset.y / sheetSize.height.toFloat()).coerceIn(0f, 1f)
                } else null
                openLineInspectorForPage(safePage, yFrac)
                return@launch
            }
            val norm = pixelToBitmapNormalized(localOffset, sheetSize, bmp)
            val hitIdx = findNearestWordIndex(tokens, norm.x, norm.y)
            activeSelection = ActivePageSelection(
                pageIndex = safePage,
                tokens = tokens,
                startIndex = hitIdx,
                endIndex = hitIdx
            )
            showInlineTranslationCard = false
            viewModel.clearLineTranslation()
        }
    }

    DisposableEffect(inlineTranslationAudio) {
        onDispose { inlineTranslationAudio.close() }
    }

    DisposableEffect(session) {
        onDispose {
            val delta = sessionPagesTurned
            if (currentPageIndex != lastSavedPage || delta > 0) {
                viewModel.saveReadingProgress(book, currentPageIndex, delta)
            }
            session.close()
        }
    }

    LaunchedEffect(session) {
        runCatching {
            val actualCount = session.open()
            if (actualCount > 0) {
                pageCount = actualCount
                if (currentPageIndex >= actualCount) {
                    currentPageIndex = actualCount - 1
                }
            }
        }
    }

    BackHandler {
        when {
            showInlineTranslationCard -> showInlineTranslationCard = false
            activeSelection != null -> activeSelection = null
            else -> {
                val delta = sessionPagesTurned
                sessionPagesTurned = 0
                lastSavedPage = currentPageIndex
                viewModel.saveReadingProgress(book, currentPageIndex, delta)
                onClose()
            }
        }
    }

    fun persistProgressIncrement(targetPage: Int) {
        val movedForward = if (targetPage > lastSavedPage) 1 else 0
        currentPageIndex = targetPage
        if (movedForward > 0) sessionPagesTurned += 1
        lastSavedPage = targetPage
        viewModel.saveReadingProgress(book, targetPage, movedForward)
    }

    LaunchedEffect(scrollListState, readerViewMode, pageCount) {
        if (readerViewMode == ReaderViewMode.PAGE_SCROLL) {
            snapshotFlow { scrollListState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { visiblePage ->
                    val safePage = visiblePage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                    if (safePage != currentPageIndex) {
                        currentPageIndex = safePage
                    }
                }
        }
    }

    LaunchedEffect(currentPageIndex, readerViewMode) {
        if (readerViewMode == ReaderViewMode.PAGE_SCROLL && currentPageIndex != lastSavedPage) {
            delay(450L)
            persistProgressIncrement(currentPageIndex)
        }
    }

    fun jumpToPage(targetPage: Int) {
        val safeTarget = targetPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        if (safeTarget == currentPageIndex) return
        if (readerViewMode == ReaderViewMode.PAGE_SCROLL) {
            currentPageIndex = safeTarget
            scope.launch {
                scrollListState.scrollToItem(safeTarget)
            }
            persistProgressIncrement(safeTarget)
        } else {
            zoomScale = 1f
            panOffset = Offset.Zero
            persistProgressIncrement(safeTarget)
        }
    }

    fun animateTurn(direction: Int, stepSize: Int) {
        if (readerViewMode == ReaderViewMode.PAGE_SCROLL) {
            val target = (currentPageIndex + direction).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            if (target != currentPageIndex) {
                currentPageIndex = target
                scope.launch {
                    scrollListState.animateScrollToItem(target)
                }
                persistProgressIncrement(target)
            }
            return
        }
        if (isAnimatingTurn) return
        val target = (currentPageIndex + direction * stepSize).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        if (target == currentPageIndex) return

        scope.launch {
            isAnimatingTurn = true
            flipDirection = direction
            flipProgress.snapTo(0f)
            flipProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            )
            persistProgressIncrement(target)
            flipDirection = 0
            flipProgress.snapTo(0f)
            isAnimatingTurn = false
        }
    }

    val paperBg = when (readerTheme) {
        ReaderThemeMode.LIGHT -> ReaderPaperLight
        ReaderThemeMode.SEPIA -> ReaderPaperSepia
        ReaderThemeMode.DARK -> ReaderPaperDark
    }
    val paperInk = when (readerTheme) {
        ReaderThemeMode.LIGHT -> ReaderInkLight
        ReaderThemeMode.SEPIA -> ReaderInkSepia
        ReaderThemeMode.DARK -> ReaderInkDark
    }
    val pageColorFilter = remember(readerTheme) { buildReaderColorFilter(readerTheme) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ReaderChromeNavy)
    ) {
        val density = LocalDensity.current
        val isWideViewport = maxWidth >= 700.dp
        val isTwoPageSpread = isWideViewport && preferTwoPageSpread
        val stepSize = if (isTwoPageSpread) 2 else 1

        // Snap left page index to even index in two-page spread
        val leftPageIndex = if (isTwoPageSpread) (currentPageIndex / 2) * 2 else currentPageIndex
        val rightPageIndex = if (isTwoPageSpread) leftPageIndex + 1 else -1

        val targetRenderWidth = with(density) {
            if (isTwoPageSpread) (maxWidth / 2f).toPx().toInt() else maxWidth.toPx().toInt()
        }.coerceIn(720, 1600)

        // Pre-render visible + adjacent pages
        LaunchedEffect(leftPageIndex, isTwoPageSpread, pageCount, targetRenderWidth) {
            val pagesToWarm = buildList {
                for (offset in -2..3) {
                    val idx = leftPageIndex + offset
                    if (idx in 0 until pageCount) add(idx)
                }
            }
            for (idx in pagesToWarm) {
                if (!renderedBitmaps.containsKey(idx)) {
                    val bmp = session.renderPage(idx, targetRenderWidth)
                    if (bmp != null) {
                        renderedBitmaps[idx] = bmp
                    }
                }
            }
            // Keep cache map lean
            val keysToDrop = renderedBitmaps.keys.filter { abs(it - leftPageIndex) > 5 }
            keysToDrop.forEach { renderedBitmaps.remove(it) }
        }

        val containerWidth = maxWidth
        val viewportWidthPx = with(density) { containerWidth.toPx() }
        val isScrollMode = readerViewMode == ReaderViewMode.PAGE_SCROLL

        if (isScrollMode) {
            val scrollRenderWidth = with(density) {
                (containerWidth.toPx() * zoomScale).toInt()
            }.coerceIn(720, 1800)

            LaunchedEffect(currentPageIndex, pageCount, scrollRenderWidth, readerViewMode) {
                val pagesToWarm = buildList {
                    for (offset in -2..3) {
                        val idx = currentPageIndex + offset
                        if (idx in 0 until pageCount) add(idx)
                    }
                }
                for (idx in pagesToWarm) {
                    if (!renderedBitmaps.containsKey(idx)) {
                        val bmp = session.renderPage(idx, scrollRenderWidth)
                        if (bmp != null) renderedBitmaps[idx] = bmp
                    }
                }
                val keysToDrop = renderedBitmaps.keys.filter { abs(it - currentPageIndex) > 6 }
                keysToDrop.forEach { renderedBitmaps.remove(it) }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                zoomScale = if (zoomScale > 1.05f) 1f else 1.75f
                            },
                            onTap = {
                                showControls = !showControls
                                isFullscreen = !showControls
                            }
                        )
                    }
                    .horizontalScroll(rememberScrollState(), enabled = zoomScale > 1.02f)
            ) {
                LazyColumn(
                    state = scrollListState,
                    modifier = Modifier
                        .width(containerWidth * zoomScale)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .testTag("pdf_page_scroll_list"),
                    contentPadding = PaddingValues(
                        top = if (isFullscreen) 10.dp else 62.dp,
                        bottom = if (isFullscreen) 16.dp else 104.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pageCount, key = { it }) { pageIdx ->
                        LaunchedEffect(pageIdx, scrollRenderWidth) {
                            if (!renderedBitmaps.containsKey(pageIdx)) {
                                session.renderPage(pageIdx, scrollRenderWidth)?.let { bmp ->
                                    renderedBitmaps[pageIdx] = bmp
                                }
                            }
                        }
                        val bmp = renderedBitmaps[pageIdx]
                        val aspect = if (bmp != null && bmp.height > 0) {
                            (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.45f, 1.8f)
                        } else {
                            0.707f
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            var pageSheetSize by remember(pageIdx) { mutableStateOf(IntSize.Zero) }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp,
                                color = paperBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect)
                                    .onSizeChanged { pageSheetSize = it }
                                    .pointerInput(pageIdx) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                activeSelection = null
                                                showInlineTranslationCard = false
                                                zoomScale = if (zoomScale > 1.05f) 1f else 1.75f
                                            },
                                            onTap = {
                                                if (activeSelection != null || showInlineTranslationCard) {
                                                    activeSelection = null
                                                    showInlineTranslationCard = false
                                                } else {
                                                    showControls = !showControls
                                                    isFullscreen = !showControls
                                                }
                                            },
                                            onLongPress = { offset ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                startInPageSelection(pageIdx, offset, pageSheetSize)
                                            }
                                        )
                                    }
                            ) {
                                PdfPageSheet(
                                    bitmap = bmp,
                                    pageNumber = pageIdx + 1,
                                    paperColor = paperBg,
                                    inkColor = paperInk,
                                    colorFilter = pageColorFilter,
                                    selection = activeSelection?.takeIf { it.pageIndex == pageIdx },
                                    onUpdateSelection = { newStart, newEnd ->
                                        activeSelection = activeSelection?.let { current ->
                                            if (current.pageIndex == pageIdx) {
                                                current.copy(startIndex = newStart, endIndex = newEnd)
                                            } else current
                                        }
                                    },
                                    onCopySelection = { text ->
                                        clipboardManager.setText(AnnotatedString(text))
                                        activeSelection = null
                                        inPageFeedback = "Copied to clipboard"
                                    },
                                    onSelectAllPage = {
                                        activeSelection = activeSelection?.let { current ->
                                            if (current.pageIndex == pageIdx && current.tokens.isNotEmpty()) {
                                                current.copy(startIndex = 0, endIndex = current.tokens.lastIndex)
                                            } else current
                                        }
                                    },
                                    onAddCommentSelection = { text ->
                                        commentTarget = pageIdx to text
                                    },
                                    onTranslateSelection = { text ->
                                        showInlineTranslationCard = true
                                        viewModel.translateSelectedLines(listOf(text), preferredLanguageTag)
                                    },
                                    onWebSearchSelection = { text ->
                                        launchWebSearch(text)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "Page ${pageIdx + 1} of $pageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = paperInk.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            // 3D Page-Flip Interactive Surface
            val hasSelection = activeSelection != null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { flipContainerCoords = it }
                    .pointerInput(zoomScale, isTwoPageSpread, leftPageIndex, pageCount, hasSelection) {
                        detectTapGestures(
                            onDoubleTap = {
                                activeSelection = null
                                showInlineTranslationCard = false
                                if (zoomScale > 1.05f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 2.2f
                                }
                            },
                            onTap = { offset ->
                                if (activeSelection != null || showInlineTranslationCard) {
                                    activeSelection = null
                                    showInlineTranslationCard = false
                                } else if (zoomScale > 1.05f) {
                                    showControls = !showControls
                                } else {
                                    when {
                                        offset.x < viewportWidthPx * 0.2f -> animateTurn(-1, stepSize)
                                        offset.x > viewportWidthPx * 0.8f -> animateTurn(1, stepSize)
                                        else -> {
                                            showControls = !showControls
                                            isFullscreen = !showControls
                                        }
                                    }
                                }
                            },
                            onLongPress = { offset ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val tappedPage = if (isTwoPageSpread && offset.x > viewportWidthPx * 0.5f && rightPageIndex in 0 until pageCount) {
                                    rightPageIndex
                                } else {
                                    leftPageIndex
                                }
                                val outerCoords = flipContainerCoords
                                val pageCoords = pageCoordinates[tappedPage]
                                if (outerCoords != null && pageCoords != null && pageCoords.isAttached && outerCoords.isAttached) {
                                    val localPos = pageCoords.localPositionOf(outerCoords, offset)
                                    startInPageSelection(tappedPage, localPos, pageCoords.size)
                                } else {
                                    val yFrac = if (size.height > 0) {
                                        (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    } else null
                                    openLineInspectorForPage(tappedPage, yFrac)
                                }
                            }
                        )
                    }
                    .pointerInput(zoomScale, hasSelection) {
                        if (!hasSelection) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                zoomScale = nextScale
                                if (nextScale > 1.02f) {
                                    val maxPanX = (size.width * (nextScale - 1f)) / 2f
                                    val maxPanY = (size.height * (nextScale - 1f)) / 2f
                                    panOffset = Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                } else {
                                    panOffset = Offset.Zero
                                }
                            }
                        }
                    }
                    .pointerInput(zoomScale, isTwoPageSpread, leftPageIndex, pageCount, hasSelection) {
                        if (zoomScale <= 1.05f && !hasSelection) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    if (isAnimatingTurn) return@detectHorizontalDragGestures
                                    totalDrag += dragAmount
                                    val dir = if (totalDrag < 0) 1 else -1
                                    val target = leftPageIndex + dir * stepSize
                                    if (target in 0 until pageCount) {
                                        flipDirection = dir
                                        val progress = (abs(totalDrag) / (viewportWidthPx * 0.45f)).coerceIn(0f, 0.96f)
                                        scope.launch { flipProgress.snapTo(progress) }
                                    }
                                },
                                onDragEnd = {
                                    if (flipDirection == 0) return@detectHorizontalDragGestures
                                    val dir = flipDirection
                                    val currentProg = flipProgress.value
                                    val target = (leftPageIndex + dir * stepSize).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                                    scope.launch {
                                        isAnimatingTurn = true
                                        if (currentProg > 0.22f && target != leftPageIndex) {
                                            flipProgress.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                                            persistProgressIncrement(target)
                                        } else {
                                            flipProgress.animateTo(0f, tween(200))
                                        }
                                        flipDirection = 0
                                        flipProgress.snapTo(0f)
                                        isAnimatingTurn = false
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        flipProgress.animateTo(0f, tween(180))
                                        flipDirection = 0
                                    }
                                }
                            )
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(
                            top = if (isFullscreen) 10.dp else 60.dp,
                            bottom = if (isFullscreen) 12.dp else 88.dp,
                            start = 10.dp,
                            end = 10.dp
                        )
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val onSelectionUpdate: (Int, Int, Int) -> Unit = { pageIdx, s, e ->
                        activeSelection = activeSelection?.let { cur ->
                            if (cur.pageIndex == pageIdx) cur.copy(startIndex = s, endIndex = e) else cur
                        }
                    }
                    val onCopySel: (String) -> Unit = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        activeSelection = null
                        inPageFeedback = "Copied to clipboard"
                    }
                    val onSelectAllSel: (Int) -> Unit = { pageIdx ->
                        activeSelection = activeSelection?.let { cur ->
                            if (cur.pageIndex == pageIdx && cur.tokens.isNotEmpty()) {
                                cur.copy(startIndex = 0, endIndex = cur.tokens.lastIndex)
                            } else cur
                        }
                    }
                    val onCommentSel: (Int, String) -> Unit = { pageIdx, text ->
                        commentTarget = pageIdx to text
                    }
                    val onTranslateSel: (String) -> Unit = { text ->
                        showInlineTranslationCard = true
                        viewModel.translateSelectedLines(listOf(text), preferredLanguageTag)
                    }
                    val onWebSearchSel: (String) -> Unit = { text ->
                        launchWebSearch(text)
                    }

                    if (isTwoPageSpread) {
                        TwoPageBookSpread(
                            leftPageIndex = leftPageIndex,
                            rightPageIndex = rightPageIndex,
                            pageCount = pageCount,
                            bitmaps = renderedBitmaps,
                            flipDirection = flipDirection,
                            flipProgress = flipProgress.value,
                            paperColor = paperBg,
                            inkColor = paperInk,
                            colorFilter = pageColorFilter,
                            activeSelection = activeSelection,
                            onRegisterPageCoords = { idx, coords -> pageCoordinates[idx] = coords },
                            onUpdateSelection = onSelectionUpdate,
                            onCopySelection = onCopySel,
                            onSelectAllPage = onSelectAllSel,
                            onAddCommentSelection = onCommentSel,
                            onTranslateSelection = onTranslateSel,
                            onWebSearchSelection = onWebSearchSel
                        )
                    } else {
                        SinglePageFlipView(
                            currentPageIndex = leftPageIndex,
                            pageCount = pageCount,
                            bitmaps = renderedBitmaps,
                            flipDirection = flipDirection,
                            flipProgress = flipProgress.value,
                            paperColor = paperBg,
                            inkColor = paperInk,
                            colorFilter = pageColorFilter,
                            activeSelection = activeSelection,
                            onRegisterPageCoords = { idx, coords -> pageCoordinates[idx] = coords },
                            onUpdateSelection = { s, e -> onSelectionUpdate(leftPageIndex, s, e) },
                            onCopySelection = onCopySel,
                            onSelectAllPage = { onSelectAllSel(leftPageIndex) },
                            onAddCommentSelection = { text -> onCommentSel(leftPageIndex, text) },
                            onTranslateSelection = onTranslateSel,
                            onWebSearchSelection = onWebSearchSel
                        )
                    }
                }
            }
        }

        // Top Floating Reader Chrome
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Surface(
                color = ReaderChromeNavy.copy(alpha = 0.96f),
                contentColor = ReaderChromeOnSurface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val actionIconSize = 20.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actionBtnModifier = if (isWideViewport) {
                        Modifier.size(width = 42.dp, height = 46.dp)
                    } else {
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                    }
                    IconButton(
                        onClick = {
                            val delta = sessionPagesTurned
                            sessionPagesTurned = 0
                            lastSavedPage = currentPageIndex
                            viewModel.saveReadingProgress(book, currentPageIndex, delta)
                            onClose()
                        },
                        modifier = actionBtnModifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to bookshelf",
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    if (isWideViewport) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Page ${currentPageIndex + 1} of $pageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Switch between Page Scroll and 3D Flip
                    IconButton(
                        onClick = {
                            val nextMode = if (readerViewMode == ReaderViewMode.PAGE_SCROLL) {
                                ReaderViewMode.BOOK_FLIP
                            } else {
                                ReaderViewMode.PAGE_SCROLL
                            }
                            readerViewMode = nextMode
                            zoomScale = 1f
                            panOffset = Offset.Zero
                            viewModel.setReaderViewMode(nextMode)
                            if (nextMode == ReaderViewMode.PAGE_SCROLL) {
                                scope.launch { scrollListState.scrollToItem(currentPageIndex) }
                            }
                        },
                        modifier = actionBtnModifier.testTag("toggle_view_mode_button")
                    ) {
                        Icon(
                            imageVector = if (readerViewMode == ReaderViewMode.PAGE_SCROLL) {
                                Icons.Filled.AutoStories
                            } else {
                                Icons.Filled.ViewAgenda
                            },
                            contentDescription = if (readerViewMode == ReaderViewMode.PAGE_SCROLL) {
                                "Switch to 3D page flip"
                            } else {
                                "Switch to page scroll"
                            },
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    // Select lines & translate
                    IconButton(
                        onClick = {
                            openLineInspectorForPage(currentPageIndex, null)
                        },
                        modifier = actionBtnModifier.testTag("reader_translate_lines_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "Select & translate lines",
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    // Switch to Play / Audio mode
                    IconButton(
                        onClick = {
                            val delta = sessionPagesTurned
                            sessionPagesTurned = 0
                            lastSavedPage = currentPageIndex
                            viewModel.saveReadingProgress(book, currentPageIndex, delta)
                            onSwitchToPlay(currentPageIndex)
                        },
                        modifier = actionBtnModifier.testTag("reader_switch_to_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = "Play audio & summary",
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    IconButton(
                        onClick = onOpenLanguagePicker,
                        modifier = actionBtnModifier.testTag("reader_language_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = "Select language",
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    if (isWideViewport && readerViewMode == ReaderViewMode.BOOK_FLIP) {
                        IconButton(
                            onClick = { preferTwoPageSpread = !preferTwoPageSpread },
                            modifier = actionBtnModifier.testTag("toggle_spread_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChromeReaderMode,
                                contentDescription = if (preferTwoPageSpread) "Switch to single page" else "Switch to two-page spread",
                                tint = if (preferTwoPageSpread) Color(0xFF8AB4F8) else ReaderChromeOnSurface.copy(alpha = 0.78f),
                                modifier = Modifier.size(actionIconSize)
                            )
                        }
                    }

                    val isCurrentBookmarked = bookBookmarks.any { it.pageIndex.toInt() == currentPageIndex }
                    IconButton(
                        onClick = { showAddBookmarkDialog = true },
                        modifier = actionBtnModifier.testTag("add_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isCurrentBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark current page",
                            tint = if (isCurrentBookmarked) Color(0xFFFFD147) else ReaderChromeOnSurface,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    IconButton(
                        onClick = { showBookmarksSheet = true },
                        modifier = actionBtnModifier.testTag("open_bookmarks_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmarks,
                            contentDescription = "View bookmarks",
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    IconButton(
                        onClick = { showThemeSheet = true },
                        modifier = actionBtnModifier.testTag("reader_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Reading theme",
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    IconButton(
                        onClick = {
                            isFullscreen = true
                            showControls = false
                        },
                        modifier = actionBtnModifier.testTag("fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Fullscreen reading mode",
                            modifier = Modifier.size(actionIconSize)
                        )
                    }
                }
            }
        }

        // Fullscreen restore pill
        AnimatedVisibility(
            visible = !showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Surface(
                color = ReaderChromeNavy.copy(alpha = 0.85f),
                contentColor = ReaderChromeOnSurface,
                shape = CircleShape,
                tonalElevation = 2.dp
            ) {
                IconButton(
                    onClick = {
                        showControls = true
                        isFullscreen = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.FullscreenExit,
                        contentDescription = "Show reader controls"
                    )
                }
            }
        }

        // Bottom Floating Page Navigation & Zoom Bar
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = ReaderChromeNavy.copy(alpha = 0.97f),
                contentColor = ReaderChromeOnSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { animateTurn(-1, stepSize) },
                            enabled = currentPageIndex > 0 && !isAnimatingTurn,
                            modifier = Modifier.testTag("prev_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous page"
                            )
                        }

                        if (pageCount > 1) {
                            Slider(
                                value = currentPageIndex.toFloat(),
                                onValueChange = { value ->
                                    jumpToPage(value.toInt())
                                },
                                valueRange = 0f..(pageCount - 1).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF7FB0FA),
                                    inactiveTrackColor = ReaderChromeSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("page_scrubber_slider")
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        IconButton(
                            onClick = { animateTurn(1, stepSize) },
                            enabled = currentPageIndex < pageCount - 1 && !isAnimatingTurn,
                            modifier = Modifier.testTag("next_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next page"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (readerViewMode == ReaderViewMode.BOOK_FLIP && isTwoPageSpread && rightPageIndex < pageCount) {
                                "Pages ${leftPageIndex + 1}–${rightPageIndex + 1} / $pageCount"
                            } else {
                                "Page ${currentPageIndex + 1} / $pageCount"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = ReaderChromeOnSurface.copy(alpha = 0.85f)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ReaderChromeSurface,
                            modifier = Modifier
                                .clickable {
                                    openLineInspectorForPage(currentPageIndex, null)
                                }
                                .testTag("bottom_translate_lines_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Translate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Translate Lines",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    zoomScale = (zoomScale - 0.35f).coerceIn(1f, 3.5f)
                                    if (zoomScale <= 1.02f) panOffset = Offset.Zero
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = ReaderChromeOnSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.35f).coerceIn(1f, 3.5f) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Feedback Pill for in-page Copy / Comment / Web Search
        AnimatedVisibility(
            visible = inPageFeedback != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (showControls) 72.dp else 16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = inPageFeedback.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }

        // Inline Translation Card triggered from in-page floating toolbar
        AnimatedVisibility(
            visible = showInlineTranslationCard && activeSelection != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    bottom = if (showControls) 108.dp else 18.dp
                )
        ) {
            val currentSel = activeSelection
            if (currentSel != null) {
                InlineTranslationOverlayCard(
                    selectedText = currentSel.selectedText,
                    targetLanguageTag = preferredLanguageTag,
                    translationState = lineTranslationState,
                    onChangeLanguage = onOpenLanguagePicker,
                    onCopyTranslation = { translated ->
                        clipboardManager.setText(AnnotatedString(translated))
                        inPageFeedback = "Copied translation to clipboard"
                    },
                    onListenTranslation = { translated, langTag ->
                        inlineTranslationAudio.playText(translated, langTag)
                    },
                    onWebSearch = { launchWebSearch(currentSel.selectedText) },
                    onDismiss = { showInlineTranslationCard = false }
                )
            }
        }
    }

    val pendingComment = commentTarget
    if (pendingComment != null) {
        var commentText by remember(pendingComment) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { commentTarget = null },
            title = { Text("Add Comment • Page ${pendingComment.first + 1}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF64B5F6).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${pendingComment.second.take(180)}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Write your comment or note") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = commentText.trim()
                        val quote = pendingComment.second.trim().take(120)
                        val notePayload = if (trimmed.isNotEmpty()) {
                            "\"$quote\" — $trimmed"
                        } else {
                            "\"$quote\""
                        }
                        viewModel.addBookmark(book, pendingComment.first, notePayload)
                        commentTarget = null
                        activeSelection = null
                        inPageFeedback = "Comment saved to Bookmarks"
                    },
                    modifier = Modifier.testTag("save_comment_button")
                ) {
                    Text("Save Comment")
                }
            },
            dismissButton = {
                TextButton(onClick = { commentTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddBookmarkDialog) {
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Bookmark Page ${currentPageIndex + 1}") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Optional note or quote") },
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBookmark(book, currentPageIndex, noteText)
                        showAddBookmarkDialog = false
                    },
                    modifier = Modifier.testTag("confirm_bookmark_button")
                ) {
                    Text("Save Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bookmarks in ${book.title}",
                    style = MaterialTheme.typography.titleLarge
                )
                if (bookBookmarks.isEmpty()) {
                    Text(
                        text = "No bookmarks yet. Tap the ribbon icon while reading to mark important pages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(bookBookmarks, key = { it.id }) { bm ->
                            BookmarkRowCard(
                                bookmark = bm,
                                onClick = {
                                    showBookmarksSheet = false
                                    jumpToPage(bm.pageIndex.toInt())
                                },
                                onDelete = { viewModel.deleteBookmark(bm.id) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Reading Theme",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReaderThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = readerTheme == mode,
                            onClick = {
                                readerTheme = mode
                                viewModel.updatePreferences(readerTheme = mode)
                            },
                            label = { Text(mode.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (showLineTranslatorSheet) {
        PageLineTranslatorSheet(
            pageNumber = lineInspectorPageIndex + 1,
            linesState = pageLinesState,
            translationState = lineTranslationState,
            targetLanguageTag = preferredLanguageTag,
            initialLongPressYFraction = pendingLongPressYFraction,
            onReloadLines = {
                openLineInspectorForPage(lineInspectorPageIndex, null, forceRefresh = true)
            },
            onOpenLanguagePicker = onOpenLanguagePicker,
            onTranslateLines = { chosenLines ->
                viewModel.translateSelectedLines(chosenLines, preferredLanguageTag)
            },
            onClearTranslation = viewModel::clearLineTranslation,
            onDismiss = {
                showLineTranslatorSheet = false
                pendingLongPressYFraction = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PageLineTranslatorSheet(
    pageNumber: Int,
    linesState: PageLinesUiState,
    translationState: LineTranslationUiState,
    targetLanguageTag: String,
    initialLongPressYFraction: Float?,
    onReloadLines: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onTranslateLines: (List<String>) -> Unit,
    onClearTranslation: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val activeLanguage = remember(targetLanguageTag) { WorldwideLanguages.findByTag(targetLanguageTag) }
    val translationAudioPlayer = remember(context) { BookAudioPlayer(context) }

    val workingLines = remember(pageNumber, linesState) {
        mutableStateListOf<String>().apply {
            if (linesState is PageLinesUiState.Success) addAll(linesState.lines)
        }
    }
    var selectedIndices by remember(pageNumber, linesState) { mutableStateOf(emptySet<Int>()) }
    var activeLineActionIndex by remember(pageNumber, linesState) { mutableStateOf<Int?>(null) }
    var cutHistory by remember(pageNumber, linesState) { mutableStateOf<Pair<Int, String>?>(null) }
    var actionFeedback by remember(pageNumber, linesState) { mutableStateOf<String?>(null) }
    var editableSelectionBuffer by remember(pageNumber, linesState) { mutableStateOf<String?>(null) }
    val lineListState = rememberLazyListState()

    LaunchedEffect(pageNumber, linesState, initialLongPressYFraction) {
        if (linesState is PageLinesUiState.Success &&
            initialLongPressYFraction != null &&
            workingLines.isNotEmpty()
        ) {
            val bounds = linesState.lineYBounds
            val targetIdx = if (bounds.size == workingLines.size) {
                val exactMatch = bounds.indexOfFirst { (top, bottom) ->
                    initialLongPressYFraction in (top - 0.015f)..(bottom + 0.015f)
                }
                if (exactMatch >= 0) {
                    exactMatch
                } else {
                    bounds.indices.minByOrNull { idx ->
                        val mid = (bounds[idx].first + bounds[idx].second) / 2f
                        abs(mid - initialLongPressYFraction)
                    } ?: 0
                }
            } else {
                (initialLongPressYFraction * workingLines.size)
                    .toInt()
                    .coerceIn(0, workingLines.lastIndex)
            }
            activeLineActionIndex = targetIdx
            selectedIndices = setOf(targetIdx)
            lineListState.animateScrollToItem(targetIdx)
        }
    }

    DisposableEffect(translationAudioPlayer) {
        onDispose { translationAudioPlayer.close() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Page Lines • Select, Cut, Copy & Translate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Page $pageNumber • Tap or long-press any line for Select, Cut, Copy & Translate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onReloadLines) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Re-scan page lines")
                }
            }

            // Target Language Picker Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenLanguagePicker)
                    .testTag("line_translator_language_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Translate to: ${activeLanguage.label}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            when (linesState) {
                is PageLinesUiState.Idle,
                is PageLinesUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Extracting lines from Page $pageNumber…")
                    }
                }

                is PageLinesUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = linesState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(onClick = onReloadLines) {
                            Text("Retry Line Extraction")
                        }
                    }
                }

                is PageLinesUiState.Success -> {
                    // Long-Press / Selection Contextual Action Toolbar (Select • Cut • Copy • Translate)
                    val actionIdx = activeLineActionIndex ?: selectedIndices.firstOrNull()
                    if (actionIdx != null && actionIdx in workingLines.indices) {
                        val activeLineText = workingLines[actionIdx]
                        val targetTextForActions = if (actionIdx in selectedIndices && selectedIndices.size > 1) {
                            selectedIndices.sorted().mapNotNull { workingLines.getOrNull(it) }.joinToString("\n")
                        } else {
                            activeLineText
                        }
                        val targetListForTranslate = if (actionIdx in selectedIndices && selectedIndices.size > 1) {
                            selectedIndices.sorted().mapNotNull { workingLines.getOrNull(it) }
                        } else {
                            listOf(activeLineText)
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("long_press_line_action_bar")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Line ${actionIdx + 1}: $activeLineText",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            activeLineActionIndex = null
                                            selectedIndices = emptySet()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss line actions",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    LineActionPill(
                                        icon = Icons.Filled.SelectAll,
                                        label = "Select",
                                        onClick = {
                                            selectedIndices = selectedIndices + actionIdx
                                            editableSelectionBuffer = targetTextForActions
                                            actionFeedback = "Line ${actionIdx + 1} selected for text inspection"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("line_action_select")
                                    )
                                    LineActionPill(
                                        icon = Icons.Filled.ContentCut,
                                        label = "Cut",
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(targetTextForActions))
                                            val removedText = workingLines.removeAt(actionIdx)
                                            cutHistory = actionIdx to removedText
                                            selectedIndices = selectedIndices
                                                .filter { it != actionIdx }
                                                .map { if (it > actionIdx) it - 1 else it }
                                                .toSet()
                                            activeLineActionIndex = null
                                            if (editableSelectionBuffer == removedText) {
                                                editableSelectionBuffer = null
                                            }
                                            actionFeedback = "Cut line to clipboard"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("line_action_cut")
                                    )
                                    LineActionPill(
                                        icon = Icons.Filled.ContentCopy,
                                        label = "Copy",
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(targetTextForActions))
                                            actionFeedback = "Copied to clipboard"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("line_action_copy")
                                    )
                                    LineActionPill(
                                        icon = Icons.Filled.Translate,
                                        label = "Translate",
                                        onClick = {
                                            selectedIndices = selectedIndices + actionIdx
                                            onTranslateLines(targetListForTranslate)
                                            activeLineActionIndex = null
                                        },
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .testTag("line_action_translate")
                                    )
                                    LineActionPill(
                                        icon = Icons.Filled.Public,
                                        label = "Search",
                                        onClick = {
                                            val cleaned = targetTextForActions.trim().take(240)
                                            if (cleaned.isNotBlank()) {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://www.google.com/search?q=${Uri.encode(cleaned)}")
                                                ).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                runCatching { context.startActivity(intent) }
                                                    .onFailure { actionFeedback = "No browser app found" }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("line_action_web_search")
                                    )
                                }
                            }
                        }
                    }

                    // Feedback & Undo Cut bar
                    if (actionFeedback != null || cutHistory != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = actionFeedback.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            val undoItem = cutHistory
                            if (undoItem != null) {
                                TextButton(
                                    onClick = {
                                        val insertIdx = undoItem.first.coerceIn(0, workingLines.size)
                                        workingLines.add(insertIdx, undoItem.second)
                                        selectedIndices = selectedIndices + insertIdx
                                        cutHistory = null
                                        actionFeedback = "Restored cut line"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("undo_cut_line_button")
                                ) {
                                    Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Undo Cut")
                                }
                            }
                        }
                    }

                    // Native Select / Cut / Copy / Translate Editor Buffer when "Select" is tapped
                    val selectionBuffer = editableSelectionBuffer
                    if (selectionBuffer != null) {
                        OutlinedTextField(
                            value = selectionBuffer,
                            onValueChange = { editableSelectionBuffer = it },
                            label = { Text("Selected Line Text (drag handles to select/cut/copy)") },
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                IconButton(onClick = { editableSelectionBuffer = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close text editor")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("selected_line_editable_field")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIndices.size} of ${workingLines.size} lines selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    selectedIndices = workingLines.indices.toSet()
                                    onClearTranslation()
                                }
                            ) {
                                Text("Select All")
                            }
                            if (selectedIndices.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        selectedIndices = emptySet()
                                        activeLineActionIndex = null
                                        editableSelectionBuffer = null
                                        onClearTranslation()
                                    }
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = lineListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(workingLines.size) { index ->
                            val line = workingLines[index]
                            val isSelected = index in selectedIndices
                            val isActionTarget = index == activeLineActionIndex
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isActionTarget -> MaterialTheme.colorScheme.primaryContainer
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = {
                                            selectedIndices = if (isSelected) {
                                                selectedIndices - index
                                            } else {
                                                selectedIndices + index
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            activeLineActionIndex = index
                                            if (index !in selectedIndices) {
                                                selectedIndices = selectedIndices + index
                                            }
                                        }
                                    )
                                    .testTag("selectable_line_$index")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                        contentDescription = if (isSelected) "Selected line" else "Unselected line",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    val selectedLines = remember(selectedIndices, workingLines.size) {
                        selectedIndices.sorted().mapNotNull { workingLines.getOrNull(it) }
                    }

                    Button(
                        onClick = { onTranslateLines(selectedLines) },
                        enabled = selectedLines.isNotEmpty() && translationState !is LineTranslationUiState.Loading,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("translate_selected_lines_button")
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedLines.isEmpty()) "Select or long-press a line"
                            else "Translate ${selectedLines.size} Line(s) to ${activeLanguage.displayName}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    when (translationState) {
                        is LineTranslationUiState.Idle -> Unit
                        is LineTranslationUiState.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Translating into ${activeLanguage.displayName}…")
                            }
                        }
                        is LineTranslationUiState.Error -> {
                            Text(
                                text = translationState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is LineTranslationUiState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("translated_lines_result_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Translation (${WorldwideLanguages.findByTag(translationState.languageTag).displayName})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(translationState.translatedText))
                                                    actionFeedback = "Copied translation to clipboard"
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy translation",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy")
                                            }
                                            TextButton(
                                                onClick = {
                                                    translationAudioPlayer.playText(
                                                        translationState.translatedText,
                                                        translationState.languageTag
                                                    )
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Listen to translation",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Listen")
                                            }
                                        }
                                    }
                                    SelectionContainer {
                                        Text(
                                            text = translationState.translatedText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SinglePageFlipView(
    currentPageIndex: Int,
    pageCount: Int,
    bitmaps: Map<Int, Bitmap>,
    flipDirection: Int,
    flipProgress: Float,
    paperColor: Color,
    inkColor: Color,
    colorFilter: ColorFilter?,
    activeSelection: ActivePageSelection? = null,
    onRegisterPageCoords: (Int, LayoutCoordinates) -> Unit = { _, _ -> },
    onUpdateSelection: (Int, Int) -> Unit = { _, _ -> },
    onCopySelection: (String) -> Unit = {},
    onSelectAllPage: () -> Unit = {},
    onAddCommentSelection: (String) -> Unit = {},
    onTranslateSelection: (String) -> Unit = {},
    onWebSearchSelection: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(10.dp, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (flipDirection == 0 || flipProgress <= 0.001f) {
            PdfPageSheet(
                bitmap = bitmaps[currentPageIndex],
                pageNumber = currentPageIndex + 1,
                paperColor = paperColor,
                inkColor = inkColor,
                colorFilter = colorFilter,
                selection = activeSelection?.takeIf { it.pageIndex == currentPageIndex },
                onUpdateSelection = onUpdateSelection,
                onCopySelection = onCopySelection,
                onSelectAllPage = onSelectAllPage,
                onAddCommentSelection = onAddCommentSelection,
                onTranslateSelection = onTranslateSelection,
                onWebSearchSelection = onWebSearchSelection,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { onRegisterPageCoords(currentPageIndex, it) }
            )
        } else if (flipDirection > 0) {
            // Turning forward: Next page underneath, current page folding left around spine
            val nextIdx = (currentPageIndex + 1).coerceAtMost(pageCount - 1)
            PdfPageSheet(
                bitmap = bitmaps[nextIdx],
                pageNumber = nextIdx + 1,
                paperColor = paperColor,
                inkColor = inkColor,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic reveal shadow on underlying page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - flipProgress) * 0.25f))
            )

            val angle = -180f * flipProgress
            val showingBack = flipProgress > 0.5f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = angle
                        cameraDistance = 16f * density
                    }
            ) {
                if (!showingBack) {
                    PdfPageSheet(
                        bitmap = bitmaps[currentPageIndex],
                        pageNumber = currentPageIndex + 1,
                        paperColor = paperColor,
                        inkColor = inkColor,
                        colorFilter = colorFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Realistic back of turning paper leaf
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(paperColor.copy(alpha = 0.96f))
                    )
                }
                PageFoldShadingOverlay(progress = flipProgress)
            }
        } else {
            // Turning backward: current page stays underneath while previous page folds back into place
            val prevIdx = (currentPageIndex - 1).coerceAtLeast(0)
            PdfPageSheet(
                bitmap = bitmaps[currentPageIndex],
                pageNumber = currentPageIndex + 1,
                paperColor = paperColor,
                inkColor = inkColor,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )

            val angle = -180f * (1f - flipProgress)
            val showingFront = flipProgress > 0.5f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = angle
                        cameraDistance = 16f * density
                    }
            ) {
                if (showingFront) {
                    PdfPageSheet(
                        bitmap = bitmaps[prevIdx],
                        pageNumber = prevIdx + 1,
                        paperColor = paperColor,
                        inkColor = inkColor,
                        colorFilter = colorFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(paperColor.copy(alpha = 0.96f))
                    )
                }
                PageFoldShadingOverlay(progress = 1f - flipProgress)
            }
        }
    }
}

@Composable
private fun TwoPageBookSpread(
    leftPageIndex: Int,
    rightPageIndex: Int,
    pageCount: Int,
    bitmaps: Map<Int, Bitmap>,
    flipDirection: Int,
    flipProgress: Float,
    paperColor: Color,
    inkColor: Color,
    colorFilter: ColorFilter?,
    activeSelection: ActivePageSelection? = null,
    onRegisterPageCoords: (Int, LayoutCoordinates) -> Unit = { _, _ -> },
    onUpdateSelection: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onCopySelection: (String) -> Unit = {},
    onSelectAllPage: (Int) -> Unit = {},
    onAddCommentSelection: (Int, String) -> Unit = { _, _ -> },
    onTranslateSelection: (String) -> Unit = {},
    onWebSearchSelection: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(12.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(paperColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left static page
            val displayLeft = if (flipDirection < 0 && flipProgress > 0.01f) {
                (leftPageIndex - 2).coerceAtLeast(0)
            } else {
                leftPageIndex
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                PdfPageSheet(
                    bitmap = bitmaps[displayLeft],
                    pageNumber = displayLeft + 1,
                    paperColor = paperColor,
                    inkColor = inkColor,
                    colorFilter = colorFilter,
                    selection = if (flipDirection == 0) activeSelection?.takeIf { it.pageIndex == displayLeft } else null,
                    onUpdateSelection = { s, e -> onUpdateSelection(displayLeft, s, e) },
                    onCopySelection = onCopySelection,
                    onSelectAllPage = { onSelectAllPage(displayLeft) },
                    onAddCommentSelection = { txt -> onAddCommentSelection(displayLeft, txt) },
                    onTranslateSelection = onTranslateSelection,
                    onWebSearchSelection = onWebSearchSelection,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { onRegisterPageCoords(displayLeft, it) }
                )
            }

            // Central book spine gutter shadow
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.34f),
                                Color.Black.copy(alpha = 0.14f)
                            )
                        )
                    )
            )

            // Right static page
            val displayRight = if (flipDirection > 0 && flipProgress > 0.01f) {
                rightPageIndex + 2
            } else {
                rightPageIndex
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (displayRight in 0 until pageCount) {
                    PdfPageSheet(
                        bitmap = bitmaps[displayRight],
                        pageNumber = displayRight + 1,
                        paperColor = paperColor,
                        inkColor = inkColor,
                        colorFilter = colorFilter,
                        selection = if (flipDirection == 0) activeSelection?.takeIf { it.pageIndex == displayRight } else null,
                        onUpdateSelection = { s, e -> onUpdateSelection(displayRight, s, e) },
                        onCopySelection = onCopySelection,
                        onSelectAllPage = { onSelectAllPage(displayRight) },
                        onAddCommentSelection = { txt -> onAddCommentSelection(displayRight, txt) },
                        onTranslateSelection = onTranslateSelection,
                        onWebSearchSelection = onWebSearchSelection,
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { onRegisterPageCoords(displayRight, it) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(paperColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "End of Book",
                            style = MaterialTheme.typography.titleMedium,
                            color = inkColor.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        // 3D Center-Hinged Turning Leaf for Two-Page Spread
        if (flipDirection != 0 && flipProgress > 0.001f) {
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = if (flipDirection > 0) -180f * flipProgress else -180f * (1f - flipProgress)
                            cameraDistance = 18f * density
                        }
                ) {
                    val pastMidpoint = flipProgress > 0.5f
                    if (flipDirection > 0) {
                        if (!pastMidpoint && rightPageIndex in 0 until pageCount) {
                            PdfPageSheet(
                                bitmap = bitmaps[rightPageIndex],
                                pageNumber = rightPageIndex + 1,
                                paperColor = paperColor,
                                inkColor = inkColor,
                                colorFilter = colorFilter,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val newLeft = (leftPageIndex + 2).coerceAtMost(pageCount - 1)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { scaleX = -1f } // Correct orientation after >90deg Y rotation
                            ) {
                                PdfPageSheet(
                                    bitmap = bitmaps[newLeft],
                                    pageNumber = newLeft + 1,
                                    paperColor = paperColor,
                                    inkColor = inkColor,
                                    colorFilter = colorFilter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        if (!pastMidpoint) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { scaleX = -1f }
                            ) {
                                PdfPageSheet(
                                    bitmap = bitmaps[leftPageIndex],
                                    pageNumber = leftPageIndex + 1,
                                    paperColor = paperColor,
                                    inkColor = inkColor,
                                    colorFilter = colorFilter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            val prevRight = (leftPageIndex - 1).coerceAtLeast(0)
                            PdfPageSheet(
                                bitmap = bitmaps[prevRight],
                                pageNumber = prevRight + 1,
                                paperColor = paperColor,
                                inkColor = inkColor,
                                colorFilter = colorFilter,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    PageFoldShadingOverlay(progress = flipProgress)
                }
            }
        }
    }
}

private data class PageBitmapTransform(
    val originX: Float,
    val originY: Float,
    val drawnW: Float,
    val drawnH: Float
)

private fun computeBitmapTransform(sheetSize: IntSize, bitmap: Bitmap): PageBitmapTransform {
    val boxW = sheetSize.width.toFloat().coerceAtLeast(1f)
    val boxH = sheetSize.height.toFloat().coerceAtLeast(1f)
    val bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
    val bmpH = bitmap.height.toFloat().coerceAtLeast(1f)
    val scale = minOf(boxW / bmpW, boxH / bmpH)
    val drawnW = bmpW * scale
    val drawnH = bmpH * scale
    return PageBitmapTransform(
        originX = (boxW - drawnW) / 2f,
        originY = (boxH - drawnH) / 2f,
        drawnW = drawnW,
        drawnH = drawnH
    )
}

private fun pixelToBitmapNormalized(
    pixelOffset: Offset,
    sheetSize: IntSize,
    bitmap: Bitmap
): Offset {
    val tf = computeBitmapTransform(sheetSize, bitmap)
    return Offset(
        x = ((pixelOffset.x - tf.originX) / tf.drawnW).coerceIn(0f, 1f),
        y = ((pixelOffset.y - tf.originY) / tf.drawnH).coerceIn(0f, 1f)
    )
}

private fun findNearestWordIndex(
    tokens: List<PageWordToken>,
    normX: Float,
    normY: Float
): Int {
    if (tokens.isEmpty()) return 0
    val exactIdx = tokens.indexOfFirst { t ->
        normX in (t.left - 0.01f)..(t.right + 0.01f) &&
            normY in (t.top - 0.008f)..(t.bottom + 0.008f)
    }
    if (exactIdx >= 0) return exactIdx

    return tokens.indices.minByOrNull { idx ->
        val t = tokens[idx]
        val dx = when {
            normX < t.left -> t.left - normX
            normX > t.right -> normX - t.right
            else -> 0f
        }
        val dy = when {
            normY < t.top -> t.top - normY
            normY > t.bottom -> normY - t.bottom
            else -> 0f
        }
        // Weight vertical line distance slightly higher so dragging along a line stays on that line
        (dx * dx) + (dy * dy * 4.0f)
    } ?: 0
}

private data class HighlightLineSpan(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

private fun buildLineHighlightSpans(selectedTokens: List<PageWordToken>): List<HighlightLineSpan> {
    if (selectedTokens.isEmpty()) return emptyList()
    val spans = mutableListOf<HighlightLineSpan>()
    var curLeft = selectedTokens[0].left
    var curTop = selectedTokens[0].top
    var curRight = selectedTokens[0].right
    var curBottom = selectedTokens[0].bottom

    for (i in 1 until selectedTokens.size) {
        val next = selectedTokens[i]
        val curMidY = (curTop + curBottom) / 2f
        val nextMidY = (next.top + next.bottom) / 2f
        val lineH = (curBottom - curTop).coerceAtLeast(0.012f)
        val sameLine = abs(nextMidY - curMidY) < lineH * 0.72f && next.left >= curLeft - 0.05f
        if (sameLine) {
            curLeft = minOf(curLeft, next.left)
            curTop = minOf(curTop, next.top)
            curRight = maxOf(curRight, next.right)
            curBottom = maxOf(curBottom, next.bottom)
        } else {
            spans += HighlightLineSpan(curLeft, curTop, curRight, curBottom)
            curLeft = next.left
            curTop = next.top
            curRight = next.right
            curBottom = next.bottom
        }
    }
    spans += HighlightLineSpan(curLeft, curTop, curRight, curBottom)
    return spans
}

@Composable
private fun PdfPageSheet(
    bitmap: Bitmap?,
    pageNumber: Int,
    paperColor: Color,
    inkColor: Color,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
    selection: ActivePageSelection? = null,
    onUpdateSelection: (Int, Int) -> Unit = { _, _ -> },
    onCopySelection: (String) -> Unit = {},
    onSelectAllPage: () -> Unit = {},
    onAddCommentSelection: (String) -> Unit = {},
    onTranslateSelection: (String) -> Unit = {},
    onWebSearchSelection: (String) -> Unit = {}
) {
    var sheetSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(paperColor)
            .onSizeChanged { sheetSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page $pageNumber",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )

            if (selection != null && selection.tokens.isNotEmpty() && sheetSize.width > 0 && sheetSize.height > 0) {
                val tf = remember(sheetSize, bitmap) { computeBitmapTransform(sheetSize, bitmap) }
                val tokens = selection.tokens
                val startIdx = selection.startIndex.coerceIn(0, tokens.lastIndex)
                val endIdx = selection.endIndex.coerceIn(startIdx, tokens.lastIndex)
                val selectedTokens = tokens.subList(startIdx, endIdx + 1)
                val spans = remember(selectedTokens) { buildLineHighlightSpans(selectedTokens) }

                val startToken = tokens[startIdx]
                val endToken = tokens[endIdx]
                val startAnchorPx = Offset(
                    x = tf.originX + startToken.left * tf.drawnW,
                    y = tf.originY + startToken.bottom * tf.drawnH
                )
                val endAnchorPx = Offset(
                    x = tf.originX + endToken.right * tf.drawnW,
                    y = tf.originY + endToken.bottom * tf.drawnH
                )

                val handleRadiusPx = with(density) { 12.dp.toPx() }
                val handleTouchRadiusPx = with(density) { 32.dp.toPx() }
                val handleColor = Color(0xFFC5A3FF)
                val highlightBlue = Color(0xFF64B5F6).copy(alpha = 0.42f)

                val latestStartIdx by rememberUpdatedState(startIdx)
                val latestEndIdx by rememberUpdatedState(endIdx)
                val latestStartAnchor by rememberUpdatedState(startAnchorPx)
                val latestEndAnchor by rememberUpdatedState(endAnchorPx)
                val latestOnUpdate by rememberUpdatedState(onUpdateSelection)

                // Selection Highlight + Draggable Teardrop Handles Layer
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(tokens, sheetSize, bitmap) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val sAnchor = latestStartAnchor
                                val eAnchor = latestEndAnchor
                                val startCenter = Offset(sAnchor.x - handleRadiusPx * 0.7f, sAnchor.y + handleRadiusPx)
                                val endCenter = Offset(eAnchor.x + handleRadiusPx * 0.7f, eAnchor.y + handleRadiusPx)
                                val distStart = (down.position - startCenter).getDistance()
                                val distEnd = (down.position - endCenter).getDistance()

                                var draggingStart = when {
                                    distStart <= handleTouchRadiusPx && distStart <= distEnd -> true
                                    distEnd <= handleTouchRadiusPx -> false
                                    else -> null
                                } ?: return@awaitEachGesture

                                down.consume()
                                var curStart = latestStartIdx
                                var curEnd = latestEndIdx

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                        ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    change.consume()
                                    val adjustedPos = Offset(
                                        x = change.position.x,
                                        y = (change.position.y - handleRadiusPx * 1.4f).coerceAtLeast(0f)
                                    )
                                    val norm = pixelToBitmapNormalized(adjustedPos, sheetSize, bitmap)
                                    val hitIdx = findNearestWordIndex(tokens, norm.x, norm.y)
                                    if (draggingStart) {
                                        if (hitIdx <= curEnd) {
                                            curStart = hitIdx
                                        } else {
                                            curStart = curEnd
                                            curEnd = hitIdx
                                            draggingStart = false
                                        }
                                    } else {
                                        if (hitIdx >= curStart) {
                                            curEnd = hitIdx
                                        } else {
                                            curEnd = curStart
                                            curStart = hitIdx
                                            draggingStart = true
                                        }
                                    }
                                    latestOnUpdate(curStart, curEnd)
                                }
                            }
                        }
                ) {
                    val padX = 3f
                    val padY = 3f
                    spans.forEach { span ->
                        val leftPx = (tf.originX + span.left * tf.drawnW - padX).coerceAtLeast(0f)
                        val topPx = (tf.originY + span.top * tf.drawnH - padY).coerceAtLeast(0f)
                        val rightPx = (tf.originX + span.right * tf.drawnW + padX).coerceAtMost(size.width)
                        val bottomPx = (tf.originY + span.bottom * tf.drawnH + padY).coerceAtMost(size.height)
                        drawRoundRect(
                            color = highlightBlue,
                            topLeft = Offset(leftPx, topPx),
                            size = Size((rightPx - leftPx).coerceAtLeast(4f), (bottomPx - topPx).coerceAtLeast(4f)),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }

                    // Start teardrop handle (tip at top-right pointing up to startAnchorPx)
                    val sR = handleRadiusPx
                    val sPath = Path().apply {
                        moveTo(startAnchorPx.x, startAnchorPx.y)
                        lineTo(startAnchorPx.x, startAnchorPx.y + sR)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                left = startAnchorPx.x - 2f * sR,
                                top = startAnchorPx.y,
                                right = startAnchorPx.x,
                                bottom = startAnchorPx.y + 2f * sR
                            ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 270f,
                            forceMoveTo = false
                        )
                        lineTo(startAnchorPx.x, startAnchorPx.y)
                        close()
                    }
                    drawPath(path = sPath, color = handleColor)

                    // End teardrop handle (tip at top-left pointing up to endAnchorPx)
                    val eR = handleRadiusPx
                    val ePath = Path().apply {
                        moveTo(endAnchorPx.x, endAnchorPx.y)
                        lineTo(endAnchorPx.x + eR, endAnchorPx.y)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                left = endAnchorPx.x,
                                top = endAnchorPx.y,
                                right = endAnchorPx.x + 2f * eR,
                                bottom = endAnchorPx.y + 2f * eR
                            ),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 270f,
                            forceMoveTo = false
                        )
                        lineTo(endAnchorPx.x, endAnchorPx.y)
                        close()
                    }
                    drawPath(path = ePath, color = handleColor)
                }

                // Floating White Pill Action Menu (Copy | Select All | Add comment | Translate | Web search)
                var menuSize by remember { mutableStateOf(IntSize.Zero) }
                val firstSpan = spans.first()
                val lastSpan = spans.last()
                val minLeftNorm = spans.minOf { it.left }
                val maxRightNorm = spans.maxOf { it.right }
                val centerXPx = tf.originX + ((minLeftNorm + maxRightNorm) / 2f) * tf.drawnW
                val topLinePx = tf.originY + firstSpan.top * tf.drawnH
                val bottomLinePx = tf.originY + lastSpan.bottom * tf.drawnH
                val marginPx = with(density) { 8.dp.toPx() }
                val aboveGapPx = with(density) { 12.dp.toPx() }
                val belowGapPx = with(density) { 34.dp.toPx() }

                val menuX = (centerXPx - menuSize.width / 2f).coerceIn(
                    marginPx,
                    (sheetSize.width - menuSize.width - marginPx).coerceAtLeast(marginPx)
                )
                val preferredAboveY = topLinePx - menuSize.height - aboveGapPx
                val menuY = if (preferredAboveY >= marginPx) {
                    preferredAboveY
                } else {
                    (bottomLinePx + belowGapPx).coerceAtMost(
                        (sheetSize.height - menuSize.height - marginPx).coerceAtLeast(marginPx)
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    FloatingSelectionActionPill(
                        maxWidthDp = with(density) { (sheetSize.width - 16.dp.toPx()).coerceAtLeast(220f).toDp() },
                        onCopy = { onCopySelection(selection.selectedText) },
                        onSelectAll = onSelectAllPage,
                        onAddComment = { onAddCommentSelection(selection.selectedText) },
                        onTranslate = { onTranslateSelection(selection.selectedText) },
                        onWebSearch = { onWebSearchSelection(selection.selectedText) },
                        modifier = Modifier
                            .offset { IntOffset(menuX.roundToInt(), menuY.roundToInt()) }
                            .onSizeChanged { menuSize = it }
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = inkColor)
                Text(
                    text = "Rendering page $pageNumber…",
                    style = MaterialTheme.typography.bodySmall,
                    color = inkColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun FloatingSelectionActionPill(
    maxWidthDp: androidx.compose.ui.unit.Dp,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onAddComment: () -> Unit,
    onTranslate: () -> Unit,
    onWebSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        contentColor = Color(0xFF1F2329),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color(0x16000000)),
        modifier = modifier
            .widthIn(max = maxWidthDp)
            .testTag("floating_selection_pill_menu")
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionMenuPillItem(
                label = "Copy",
                onClick = onCopy,
                testTag = "selection_pill_copy"
            )
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = Color(0x1A000000)
            )
            SelectionMenuPillItem(
                label = "Select All",
                onClick = onSelectAll,
                testTag = "selection_pill_select_all"
            )
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = Color(0x1A000000)
            )
            SelectionMenuPillItem(
                label = "Add comment",
                onClick = onAddComment,
                testTag = "selection_pill_add_comment"
            )
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = Color(0x1A000000)
            )
            SelectionMenuPillItem(
                label = "Translate",
                icon = Icons.Filled.Translate,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = onTranslate,
                testTag = "selection_pill_translate"
            )
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = Color(0x1A000000)
            )
            SelectionMenuPillItem(
                label = "Web search",
                icon = Icons.Filled.Public,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onWebSearch,
                testTag = "selection_pill_web_search"
            )
        }
    }
}

@Composable
private fun SelectionMenuPillItem(
    label: String,
    onClick: () -> Unit,
    testTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accentColor: Color = Color(0xFF1F2329)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = accentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun InlineTranslationOverlayCard(
    selectedText: String,
    targetLanguageTag: String,
    translationState: LineTranslationUiState,
    onChangeLanguage: () -> Unit,
    onCopyTranslation: (String) -> Unit,
    onListenTranslation: (String, String) -> Unit,
    onWebSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeLang = remember(targetLanguageTag) { WorldwideLanguages.findByTag(targetLanguageTag) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inline_translation_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Translator",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onChangeLanguage)
                            .testTag("inline_translator_language_pill")
                    ) {
                        Text(
                            text = "${activeLang.displayName} ▾",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onWebSearch,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = "Web search selected text",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close translator",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = selectedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            when (translationState) {
                is LineTranslationUiState.Idle,
                is LineTranslationUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Translating into ${activeLang.displayName}…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is LineTranslationUiState.Error -> {
                    Text(
                        text = translationState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is LineTranslationUiState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = translationState.translatedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { onCopyTranslation(translationState.translatedText) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy")
                                }
                                TextButton(
                                    onClick = { onListenTranslation(translationState.translatedText, translationState.languageTag) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Listen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageFoldShadingOverlay(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val foldIntensity = sin(progress.coerceIn(0f, 1f) * PI).toFloat()
        if (foldIntensity > 0.01f) {
            // Dynamic crease shadow and highlight across bending sheet
            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Black.copy(alpha = 0.28f * foldIntensity),
                    0.22f to Color.White.copy(alpha = 0.16f * foldIntensity),
                    0.75f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.20f * foldIntensity)
                )
            )
        }
    }
}

@Composable
fun BookmarkRowCard(
    bookmark: BookmarkItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    showBookTitle: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "p. ${bookmark.pageNumberDisplay}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showBookTitle) {
                    Text(
                        text = bookmark.bookTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = bookmark.note?.takeIf { it.isNotBlank() } ?: "Bookmarked page ${bookmark.pageNumberDisplay}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete bookmark"
                )
            }
        }
    }
}

private fun buildReaderColorFilter(mode: ReaderThemeMode): ColorFilter? {
    return when (mode) {
        ReaderThemeMode.LIGHT -> null
        ReaderThemeMode.SEPIA -> {
            // Warm parchment tint preserving crisp ink contrast
            val matrix = ColorMatrix(
                floatArrayOf(
                    0.95f, 0f, 0f, 0f, 4f,
                    0f, 0.90f, 0f, 0f, 0f,
                    0f, 0f, 0.78f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(matrix)
        }
        ReaderThemeMode.DARK -> {
            // Comfortable night-mode inversion with warm cream text & soft charcoal paper
            val matrix = ColorMatrix(
                floatArrayOf(
                    -0.86f, 0f, 0f, 0f, 232f,
                    0f, -0.87f, 0f, 0f, 226f,
                    0f, 0f, -0.89f, 0f, 214f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(matrix)
        }
    }
}
