package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.snapshotFlow
import com.example.audio.BookAudioPlayer
import com.example.data.model.BookItem
import com.example.data.model.BookmarkItem
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReaderViewMode
import com.example.data.model.UiState
import com.example.data.model.WorldwideLanguages
import com.example.data.pdf.PdfPageSession
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
import kotlin.math.sin

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

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val flipProgress = remember { Animatable(0f) }
    var flipDirection by remember { mutableIntStateOf(0) } // +1 next, -1 prev, 0 idle
    var isAnimatingTurn by remember { mutableStateOf(false) }

    val renderedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

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
        val delta = sessionPagesTurned
        sessionPagesTurned = 0
        lastSavedPage = currentPageIndex
        viewModel.saveReadingProgress(book, currentPageIndex, delta)
        onClose()
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
            .background(paperBg)
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
                        .testTag("pdf_page_scroll_list"),
                    contentPadding = PaddingValues(
                        top = if (isFullscreen) 12.dp else 72.dp,
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp,
                                color = paperBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect)
                            ) {
                                PdfPageSheet(
                                    bitmap = bmp,
                                    pageNumber = pageIdx + 1,
                                    paperColor = paperBg,
                                    inkColor = paperInk,
                                    colorFilter = pageColorFilter,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(zoomScale, isTwoPageSpread, leftPageIndex, pageCount) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoomScale > 1.05f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 2.2f
                                }
                            },
                            onTap = { offset ->
                                if (zoomScale > 1.05f) {
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
                            }
                        )
                    }
                    .pointerInput(zoomScale) {
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
                    .pointerInput(zoomScale, isTwoPageSpread, leftPageIndex, pageCount) {
                        if (zoomScale <= 1.05f) {
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
                        .padding(
                            top = if (isFullscreen) 12.dp else 68.dp,
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
                            colorFilter = pageColorFilter
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
                            colorFilter = pageColorFilter
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
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val delta = sessionPagesTurned
                            sessionPagesTurned = 0
                            lastSavedPage = currentPageIndex
                            viewModel.saveReadingProgress(book, currentPageIndex, delta)
                            onClose()
                        },
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to bookshelf"
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
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
                        modifier = Modifier.testTag("toggle_view_mode_button")
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
                            }
                        )
                    }

                    // Select lines & translate
                    IconButton(
                        onClick = {
                            showLineTranslatorSheet = true
                            scope.launch {
                                val bmp = renderedBitmaps[currentPageIndex]
                                    ?: session.renderPage(currentPageIndex, 1080)?.also {
                                        renderedBitmaps[currentPageIndex] = it
                                    }
                                if (bmp != null) {
                                    viewModel.loadPageLines(book.id, currentPageIndex, bmp)
                                }
                            }
                        },
                        modifier = Modifier.testTag("reader_translate_lines_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "Select & translate lines"
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
                        modifier = Modifier.testTag("reader_switch_to_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = "Play audio & summary"
                        )
                    }

                    IconButton(
                        onClick = onOpenLanguagePicker,
                        modifier = Modifier.testTag("reader_language_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = "Select language"
                        )
                    }

                    if (isWideViewport && readerViewMode == ReaderViewMode.BOOK_FLIP) {
                        IconButton(
                            onClick = { preferTwoPageSpread = !preferTwoPageSpread },
                            modifier = Modifier.testTag("toggle_spread_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChromeReaderMode,
                                contentDescription = if (preferTwoPageSpread) "Switch to single page" else "Switch to two-page spread",
                                tint = if (preferTwoPageSpread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isCurrentBookmarked = bookBookmarks.any { it.pageIndex.toInt() == currentPageIndex }
                    IconButton(
                        onClick = { showAddBookmarkDialog = true },
                        modifier = Modifier.testTag("add_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isCurrentBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark current page",
                            tint = if (isCurrentBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { showBookmarksSheet = true },
                        modifier = Modifier.testTag("open_bookmarks_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmarks,
                            contentDescription = "View bookmarks"
                        )
                    }

                    IconButton(
                        onClick = { showThemeSheet = true },
                        modifier = Modifier.testTag("reader_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Reading theme"
                        )
                    }

                    IconButton(
                        onClick = {
                            isFullscreen = true
                            showControls = false
                        },
                        modifier = Modifier.testTag("fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Fullscreen reading mode"
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable {
                                    showLineTranslatorSheet = true
                                    scope.launch {
                                        val bmp = renderedBitmaps[currentPageIndex]
                                            ?: session.renderPage(currentPageIndex, 1080)?.also {
                                                renderedBitmaps[currentPageIndex] = it
                                            }
                                        if (bmp != null) {
                                            viewModel.loadPageLines(book.id, currentPageIndex, bmp)
                                        }
                                    }
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
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Translate Lines",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
            pageNumber = currentPageIndex + 1,
            linesState = pageLinesState,
            translationState = lineTranslationState,
            targetLanguageTag = preferredLanguageTag,
            onReloadLines = {
                scope.launch {
                    val bmp = renderedBitmaps[currentPageIndex]
                        ?: session.renderPage(currentPageIndex, 1080)?.also {
                            renderedBitmaps[currentPageIndex] = it
                        }
                    if (bmp != null) {
                        viewModel.loadPageLines(book.id, currentPageIndex, bmp, forceRefresh = true)
                    }
                }
            },
            onOpenLanguagePicker = onOpenLanguagePicker,
            onTranslateLines = { chosenLines ->
                viewModel.translateSelectedLines(chosenLines, preferredLanguageTag)
            },
            onClearTranslation = viewModel::clearLineTranslation,
            onDismiss = { showLineTranslatorSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageLineTranslatorSheet(
    pageNumber: Int,
    linesState: PageLinesUiState,
    translationState: LineTranslationUiState,
    targetLanguageTag: String,
    onReloadLines: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onTranslateLines: (List<String>) -> Unit,
    onClearTranslation: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeLanguage = remember(targetLanguageTag) { WorldwideLanguages.findByTag(targetLanguageTag) }
    val translationAudioPlayer = remember(context) { BookAudioPlayer(context) }
    var selectedIndices by remember(pageNumber, linesState) { mutableStateOf(emptySet<Int>()) }

    DisposableEffect(translationAudioPlayer) {
        onDispose { translationAudioPlayer.close() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select & Translate Lines",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Page $pageNumber • Tap lines to select & translate",
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
                    val lines = linesState.lines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIndices.size} of ${lines.size} lines selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    selectedIndices = lines.indices.toSet()
                                    onClearTranslation()
                                }
                            ) {
                                Text("Select All")
                            }
                            if (selectedIndices.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        selectedIndices = emptySet()
                                        onClearTranslation()
                                    }
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(lines.size) { index ->
                            val line = lines[index]
                            val isSelected = index in selectedIndices
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIndices = if (isSelected) {
                                            selectedIndices - index
                                        } else {
                                            selectedIndices + index
                                        }
                                    }
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
                                        tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
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

                    val selectedLines = remember(selectedIndices, lines) {
                        selectedIndices.sorted().mapNotNull { lines.getOrNull(it) }
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
                            text = if (selectedLines.isEmpty()) "Select lines above to translate"
                            else "Translate ${selectedLines.size} Selected Line(s) to ${activeLanguage.displayName}",
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
private fun SinglePageFlipView(
    currentPageIndex: Int,
    pageCount: Int,
    bitmaps: Map<Int, Bitmap>,
    flipDirection: Int,
    flipProgress: Float,
    paperColor: Color,
    inkColor: Color,
    colorFilter: ColorFilter?
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
                modifier = Modifier.fillMaxSize()
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
    colorFilter: ColorFilter?
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
                    modifier = Modifier.fillMaxSize()
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
                        modifier = Modifier.fillMaxSize()
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

@Composable
private fun PdfPageSheet(
    bitmap: Bitmap?,
    pageNumber: Int,
    paperColor: Color,
    inkColor: Color,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(paperColor),
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
