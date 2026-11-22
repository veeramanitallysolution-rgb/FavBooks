package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.BookAudioPlayer
import com.example.data.model.BookItem
import com.example.data.model.WorldwideLanguages
import com.example.data.pdf.PdfPageSession
import com.example.ui.viewmodel.AudioPrepUiState
import com.example.ui.viewmodel.BookshelfViewModel
import com.example.ui.viewmodel.SummaryUiState
import kotlinx.coroutines.launch

@Composable
fun BookAudioPlayScreen(
    book: BookItem,
    initialPageOverride: Int?,
    viewModel: BookshelfViewModel,
    onSwitchToRead: (Int) -> Unit,
    onClose: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val preferredLanguageTag by viewModel.preferredLanguageTag.collectAsStateWithLifecycle()
    val activeLanguage = remember(preferredLanguageTag) {
        WorldwideLanguages.findByTag(preferredLanguageTag)
    }

    val summaryState by viewModel.summaryState.collectAsStateWithLifecycle()
    val audioPrepState by viewModel.audioPrepState.collectAsStateWithLifecycle()

    val audioPlayer = remember(context) { BookAudioPlayer(context) }
    val playbackState by audioPlayer.state.collectAsStateWithLifecycle()

    val pdfFile = remember(book.id) { viewModel.getLocalPdfFile(book.id) }
    val session = remember(pdfFile.absolutePath) { PdfPageSession(pdfFile) }

    var pageCount by remember(book.id) { mutableIntStateOf(book.totalPagesInt) }
    var currentPageIndex by remember(book.id, initialPageOverride) {
        mutableIntStateOf(
            (initialPageOverride ?: book.currentPageInt).coerceIn(0, (book.totalPagesInt - 1).coerceAtLeast(0))
        )
    }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var autoPlayPendingPage by remember { mutableStateOf<Int?>(null) }
    var continuousFullBookPlay by remember { mutableStateOf(true) }
    var activeAudioSourceLabel by remember { mutableStateOf("No audio playing yet") }

    DisposableEffect(audioPlayer, session) {
        onDispose {
            audioPlayer.close()
            session.close()
        }
    }

    LaunchedEffect(book.id) {
        viewModel.resetBookMediaStates()
        if (!book.summary.isNullOrBlank()) {
            viewModel.generateBookSummary(
                book = book,
                languageTag = book.summaryLanguage ?: preferredLanguageTag,
                forceRefresh = false
            )
        } else {
            viewModel.generateBookSummary(
                book = book,
                languageTag = preferredLanguageTag,
                forceRefresh = false
            )
        }
        runCatching {
            val count = session.open()
            if (count > 0) {
                pageCount = count
                if (currentPageIndex >= count) {
                    currentPageIndex = count - 1
                }
            }
        }
    }

    LaunchedEffect(currentPageIndex, session) {
        currentPageBitmap = session.renderPage(currentPageIndex, 980)
    }

    fun launchPageAudio(targetPage: Int) {
        scope.launch {
            val bmp = if (targetPage == currentPageIndex && currentPageBitmap != null) {
                currentPageBitmap
            } else {
                session.renderPage(targetPage, 980)?.also {
                    if (targetPage == currentPageIndex) currentPageBitmap = it
                }
            }
            if (bmp != null) {
                autoPlayPendingPage = targetPage
                viewModel.preparePageAudio(
                    book = book,
                    pageIndex = targetPage,
                    pageBitmap = bmp,
                    languageTag = activeLanguage.tag
                )
            }
        }
    }

    LaunchedEffect(audioPrepState) {
        val ready = audioPrepState as? AudioPrepUiState.Ready ?: return@LaunchedEffect
        if (autoPlayPendingPage == ready.pageIndex) {
            autoPlayPendingPage = null
            activeAudioSourceLabel = "Page ${ready.pageIndex + 1} of $pageCount (${WorldwideLanguages.findByTag(ready.languageTag).displayName})"
            audioPlayer.playText(
                text = ready.text,
                languageTag = ready.languageTag,
                onComplete = {
                    val nextPage = ready.pageIndex + 1
                    if (continuousFullBookPlay && nextPage < pageCount) {
                        currentPageIndex = nextPage
                        launchPageAudio(nextPage)
                    } else if (nextPage >= pageCount) {
                        activeAudioSourceLabel = "Finished full book (${pageCount} pages)"
                    }
                }
            )
            viewModel.saveReadingProgress(book, ready.pageIndex, 1)

            // Pre-warm next page in background for smooth full-book continuous listening
            val upcoming = ready.pageIndex + 1
            if (continuousFullBookPlay && upcoming < pageCount) {
                scope.launch {
                    session.renderPage(upcoming, 980)?.let { nextBmp ->
                        viewModel.prewarmPageAudio(
                            bookId = book.id,
                            pageIndex = upcoming,
                            pageBitmap = nextBmp,
                            languageTag = ready.languageTag
                        )
                    }
                }
            }
        }
    }

    BackHandler {
        audioPlayer.stop()
        viewModel.saveReadingProgress(book, currentPageIndex, 0)
        onClose()
    }

    fun triggerPageAudio() {
        val ready = audioPrepState as? AudioPrepUiState.Ready
        if (ready != null &&
            ready.pageIndex == currentPageIndex &&
            ready.languageTag.equals(activeLanguage.tag, ignoreCase = true)
        ) {
            activeAudioSourceLabel = "Page ${ready.pageIndex + 1} of $pageCount (${activeLanguage.displayName})"
            audioPlayer.playText(
                text = ready.text,
                languageTag = activeLanguage.tag,
                onComplete = {
                    val nextPage = currentPageIndex + 1
                    if (continuousFullBookPlay && nextPage < pageCount) {
                        currentPageIndex = nextPage
                        launchPageAudio(nextPage)
                    } else if (nextPage >= pageCount) {
                        activeAudioSourceLabel = "Finished full book (${pageCount} pages)"
                    }
                }
            )
            viewModel.saveReadingProgress(book, currentPageIndex, 1)
            return
        }

        launchPageAudio(currentPageIndex)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            audioPlayer.stop()
                            viewModel.saveReadingProgress(book, currentPageIndex, 0)
                            onClose()
                        },
                        modifier = Modifier.testTag("audio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to bookshelf"
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Play Mode • Page ${currentPageIndex + 1} of $pageCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleArchive(book) },
                        modifier = Modifier.testTag("audio_archive_button")
                    ) {
                        Icon(
                            imageVector = if (book.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            contentDescription = if (book.archived) "Restore from Archive" else "Archive book"
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable(onClick = onOpenLanguagePicker)
                            .testTag("audio_language_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = "Select language",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = activeLanguage.tag.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            audioPlayer.stop()
                            onSwitchToRead(currentPageIndex)
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("switch_to_read_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Read", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card with artwork + language info
                val spineColor = parseHexColor(book.coverColorHex, MaterialTheme.colorScheme.primary)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCoverArtwork(
                            title = book.title,
                            author = book.author,
                            spineColor = spineColor,
                            coverFile = viewModel.getCoverFile(book.id),
                            modifier = Modifier
                                .width(78.dp)
                                .aspectRatio(0.7f)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "AUDIO & BOOK SMALL SUMMARY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${book.author} • ${book.totalPagesInt} pages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = onOpenLanguagePicker,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Language: ${activeLanguage.label}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // Book Small Summary Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.testTag("book_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Book Small Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "In ${activeLanguage.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.generateBookSummary(
                                        book = book,
                                        languageTag = activeLanguage.tag,
                                        forceRefresh = true
                                    )
                                },
                                modifier = Modifier.testTag("refresh_summary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Generate or translate summary"
                                )
                            }
                        }

                        when (val state = summaryState) {
                            is SummaryUiState.Idle -> {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.generateBookSummary(
                                            book = book,
                                            languageTag = activeLanguage.tag,
                                            forceRefresh = true
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Small Summary in ${activeLanguage.displayName}")
                                }
                            }

                            is SummaryUiState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                    Text(
                                        text = "Summarizing book in ${activeLanguage.displayName}…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is SummaryUiState.Error -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.generateBookSummary(
                                                book = book,
                                                languageTag = activeLanguage.tag,
                                                forceRefresh = true
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Retry Summary")
                                    }
                                }
                            }

                            is SummaryUiState.Success -> {
                                val differentLanguage = !state.languageTag.equals(activeLanguage.tag, ignoreCase = true)
                                if (differentLanguage) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Shown in ${WorldwideLanguages.findByTag(state.languageTag).displayName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            TextButton(
                                                onClick = {
                                                    viewModel.generateBookSummary(
                                                        book = book,
                                                        languageTag = activeLanguage.tag,
                                                        forceRefresh = true
                                                    )
                                                }
                                            ) {
                                                Text("Translate to ${activeLanguage.displayName}")
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = state.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            activeAudioSourceLabel = "Book Small Summary (${WorldwideLanguages.findByTag(state.languageTag).displayName})"
                                            audioPlayer.playText(state.text, state.languageTag)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("play_summary_audio_button")
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Summary")
                                    }
                                }
                            }
                        }
                    }
                }

                // Audio Player & Page Narration Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.testTag("audio_player_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Audio Play",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = activeAudioSourceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Compact page thumbnail
                            currentPageBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Preview of page ${currentPageIndex + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                )
                            }
                        }

                        // Page Selector
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentPageIndex > 0) {
                                            currentPageIndex -= 1
                                        }
                                    },
                                    enabled = currentPageIndex > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous page"
                                    )
                                }
                                Text(
                                    text = "Page ${currentPageIndex + 1} of $pageCount",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = {
                                        if (currentPageIndex < pageCount - 1) {
                                            currentPageIndex += 1
                                        }
                                    },
                                    enabled = currentPageIndex < pageCount - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next page"
                                    )
                                }
                            }
                        }

                        // Full-Book Continuous Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = continuousFullBookPlay,
                                onClick = { continuousFullBookPlay = true },
                                label = { Text("Play Full Book (Auto-Next)") },
                                modifier = Modifier.weight(1f).testTag("full_book_play_mode_chip")
                            )
                            FilterChip(
                                selected = !continuousFullBookPlay,
                                onClick = { continuousFullBookPlay = false },
                                label = { Text("Single Page Only") }
                            )
                        }

                        Button(
                            onClick = ::triggerPageAudio,
                            enabled = audioPrepState !is AudioPrepUiState.Loading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("prepare_play_page_audio_button")
                        ) {
                            if (audioPrepState is AudioPrepUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Preparing Page ${currentPageIndex + 1} in ${activeLanguage.displayName}…")
                            } else {
                                Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (continuousFullBookPlay) {
                                        "Play Full Book from Page ${currentPageIndex + 1} (${activeLanguage.displayName})"
                                    } else {
                                        "Play Page ${currentPageIndex + 1} (${activeLanguage.displayName})"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (audioPrepState is AudioPrepUiState.Error) {
                            Text(
                                text = (audioPrepState as AudioPrepUiState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Playback progress & live sentence
                        if (playbackState.totalChunks > 0) {
                            LinearProgressIndicator(
                                progress = { playbackState.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = playbackState.currentChunkText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(12.dp)
                            )
                        }

                        playbackState.statusNote?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Transport controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { audioPlayer.stop() },
                                enabled = playbackState.isPlaying || playbackState.isPaused,
                                shape = CircleShape,
                                modifier = Modifier.testTag("audio_stop_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = "Stop playback")
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Button(
                                onClick = {
                                    when {
                                        playbackState.isPlaying -> audioPlayer.pause()
                                        playbackState.isPaused -> audioPlayer.resume()
                                        else -> triggerPageAudio()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("audio_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        playbackState.isPlaying -> "Pause"
                                        playbackState.isPaused -> "Resume"
                                        else -> "Play"
                                    }
                                )
                            }
                        }

                        // Speech rate pills
                        val rates = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rates.forEach { rate ->
                                FilterChip(
                                    selected = kotlin.math.abs(playbackState.speechRate - rate) < 0.05f,
                                    onClick = { audioPlayer.setSpeechRate(rate) },
                                    label = { Text("${rate}x") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
