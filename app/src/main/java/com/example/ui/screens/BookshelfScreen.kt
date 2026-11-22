package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BookItem
import com.example.data.model.BookshelfFilter
import com.example.data.model.BookshelfSort
import com.example.data.model.UiState
import com.example.ui.viewmodel.BookshelfViewModel
import java.io.File

private enum class ShelfSegment(val label: String) {
    EBOOKS("Ebooks"),
    AUDIO_PLAY("Audio Play"),
    ARCHIVED("Archived")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookshelfScreen(
    viewModel: BookshelfViewModel,
    activeLanguageTag: String,
    onOpenProfile: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenSignOut: () -> Unit,
    onOpenBookRead: (BookItem) -> Unit,
    onOpenBookPlay: (BookItem) -> Unit,
    onRequestUploadPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val booksUiState by viewModel.filteredBooksState.collectAsStateWithLifecycle()
    val allBooksState by viewModel.booksState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    var activeSegment by remember { mutableStateOf(ShelfSegment.EBOOKS) }
    var editingBook by remember { mutableStateOf<BookItem?>(null) }
    var actionChooserBook by remember { mutableStateOf<BookItem?>(null) }
    var relinkTargetBookId by remember { mutableStateOf<String?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var profileMenuExpanded by remember { mutableStateOf(false) }

    val relinkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val targetId = relinkTargetBookId
        relinkTargetBookId = null
        if (uri != null && targetId != null) {
            viewModel.relinkPdf(targetId, uri)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = isImporting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val profileName = profile?.displayName?.takeIf { it.isNotBlank() } ?: "Reader"

            // Compact Name Profile greeting row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .clickable { onOpenProfile() }
                        .testTag("bookshelf_name_profile_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Name Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = profileName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Reference Top White Search Pill + Crisp Circular Favbooks Logo Avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search books",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_books_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = viewModel::setSearchQuery,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bookshelf_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1F2EC),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { profileMenuExpanded = true }
                                    .testTag("top_profile_logo_menu")
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_bookshelf_logo),
                                    contentDescription = "Favbooks menu",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }

                            DropdownMenu(
                                expanded = profileMenuExpanded,
                                onDismissRequest = { profileMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Name Profile ($profileName)") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onOpenProfile()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Language (${activeLanguageTag.uppercase()})") },
                                    leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onOpenLanguagePicker()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Load Sample PDF Book") },
                                    leadingIcon = { Icon(Icons.Filled.AutoStories, contentDescription = null) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        viewModel.loadSampleBook()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sign Out") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onOpenSignOut()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Reference Segmented White Pill Track (Ebooks / Audio Play / Archived)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShelfSegment.entries.forEach { segment ->
                        val isSelected = activeSegment == segment
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable {
                                    activeSegment = segment
                                    when (segment) {
                                        ShelfSegment.EBOOKS -> {
                                            if (selectedFilter == BookshelfFilter.ARCHIVED) {
                                                viewModel.setFilter(BookshelfFilter.ALL)
                                            }
                                        }
                                        ShelfSegment.AUDIO_PLAY -> {
                                            if (selectedFilter == BookshelfFilter.ARCHIVED) {
                                                viewModel.setFilter(BookshelfFilter.ALL)
                                            }
                                        }
                                        ShelfSegment.ARCHIVED -> {
                                            viewModel.setFilter(BookshelfFilter.ARCHIVED)
                                        }
                                    }
                                }
                                .testTag("segment_${segment.name.lowercase()}")
                        ) {
                            Text(
                                text = segment.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            when (val state = booksUiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Unable to load library",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val allBooks = (allBooksState as? UiState.Success)?.data ?: emptyList()
                    val filteredBooks = state.data

                    if (allBooks.isEmpty()) {
                        EmptyBookshelfState(
                            onUploadPdf = onRequestUploadPdf,
                            onLoadSample = { viewModel.loadSampleBook() }
                        )
                    } else {
                        val activeNonArchived = remember(allBooks) { allBooks.filter { !it.archived } }
                        val popularCovers = remember(activeNonArchived) {
                            val favs = activeNonArchived.filter { it.favorite }
                            if (favs.isNotEmpty()) favs else activeNonArchived.take(8)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 14.dp,
                                bottom = 110.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Popular / Featured Horizontal Carousel (matches reference Left screen)
                            if (activeSegment != ShelfSegment.ARCHIVED && popularCovers.isNotEmpty() && searchQuery.isBlank()) {
                                item {
                                    SectionHeaderRow(
                                        title = if (activeSegment == ShelfSegment.AUDIO_PLAY) "Full-Book Audio Ready" else "Popular Books",
                                        actionLabel = "See All",
                                        onAction = { viewModel.setFilter(BookshelfFilter.ALL) }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(popularCovers, key = { "carousel_${it.id}" }) { book ->
                                            val hasPdf = viewModel.hasLocalPdf(book.id)
                                            FeaturedCoverTile(
                                                book = book,
                                                coverFile = viewModel.getCoverFile(book.id),
                                                showAudioBadge = activeSegment == ShelfSegment.AUDIO_PLAY,
                                                onClick = {
                                                    if (!hasPdf) {
                                                        relinkTargetBookId = book.id
                                                        relinkLauncher.launch(arrayOf("application/pdf"))
                                                    } else if (activeSegment == ShelfSegment.AUDIO_PLAY) {
                                                        onOpenBookPlay(book)
                                                    } else {
                                                        actionChooserBook = book
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Buttercream Category / Filter Chips (matches Reference Right screen "Genres")
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Shelf Filters",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 1.dp,
                                            modifier = Modifier
                                                .clickable { sortMenuExpanded = true }
                                                .testTag("sort_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                                    contentDescription = "Sort books",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = selectedSort.label,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = sortMenuExpanded,
                                            onDismissRequest = { sortMenuExpanded = false }
                                        ) {
                                            BookshelfSort.entries.forEach { sortOption ->
                                                DropdownMenuItem(
                                                    text = { Text(sortOption.label) },
                                                    onClick = {
                                                        viewModel.setSort(sortOption)
                                                        sortMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BookshelfFilter.entries.forEach { filter ->
                                        val isChosen = selectedFilter == filter
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isChosen) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            },
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.setFilter(filter)
                                                    activeSegment = if (filter == BookshelfFilter.ARCHIVED) {
                                                        ShelfSegment.ARCHIVED
                                                    } else if (activeSegment == ShelfSegment.ARCHIVED) {
                                                        ShelfSegment.EBOOKS
                                                    } else {
                                                        activeSegment
                                                    }
                                                }
                                                .testTag("filter_chip_${filter.name.lowercase()}")
                                        ) {
                                            Text(
                                                text = filter.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isChosen) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                },
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section Title for List Cards
                            item {
                                SectionHeaderRow(
                                    title = when (selectedFilter) {
                                        BookshelfFilter.ARCHIVED -> "Archived Collection"
                                        BookshelfFilter.FAVORITES -> "Favorite Books"
                                        BookshelfFilter.READING -> "Currently Reading"
                                        BookshelfFilter.COMPLETED -> "Finished Books"
                                        else -> "Your Books (${filteredBooks.size})"
                                    },
                                    actionLabel = if (selectedFilter != BookshelfFilter.ALL) "Reset" else null,
                                    onAction = {
                                        viewModel.setFilter(BookshelfFilter.ALL)
                                        activeSegment = ShelfSegment.EBOOKS
                                    }
                                )
                            }

                            if (filteredBooks.isEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "No matching books",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Try another filter chip or upload a PDF book.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            TextButton(onClick = {
                                                viewModel.setSearchQuery("")
                                                viewModel.setFilter(BookshelfFilter.ALL)
                                                activeSegment = ShelfSegment.EBOOKS
                                            }) {
                                                Text("Show All Books")
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(filteredBooks, key = { it.id }) { book ->
                                    val hasPdf = viewModel.hasLocalPdf(book.id)
                                    ReferenceBookListCard(
                                        book = book,
                                        coverFile = viewModel.getCoverFile(book.id),
                                        hasLocalPdf = hasPdf,
                                        onOpen = {
                                            if (!hasPdf) {
                                                relinkTargetBookId = book.id
                                                relinkLauncher.launch(arrayOf("application/pdf"))
                                            } else if (activeSegment == ShelfSegment.AUDIO_PLAY) {
                                                onOpenBookPlay(book)
                                            } else {
                                                actionChooserBook = book
                                            }
                                        },
                                        onRead = {
                                            if (hasPdf) onOpenBookRead(book)
                                            else {
                                                relinkTargetBookId = book.id
                                                relinkLauncher.launch(arrayOf("application/pdf"))
                                            }
                                        },
                                        onPlay = {
                                            if (hasPdf) onOpenBookPlay(book)
                                            else {
                                                relinkTargetBookId = book.id
                                                relinkLauncher.launch(arrayOf("application/pdf"))
                                            }
                                        },
                                        onToggleFavorite = { viewModel.toggleFavorite(book) },
                                        onToggleArchive = { viewModel.toggleArchive(book) },
                                        onEditDetails = { editingBook = book },
                                        onRelinkPdf = {
                                            relinkTargetBookId = book.id
                                            relinkLauncher.launch(arrayOf("application/pdf"))
                                        },
                                        onDelete = { viewModel.deleteBook(book.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingBook?.let { book ->
        EditBookDialog(
            book = book,
            onDismiss = { editingBook = null },
            onSave = { newTitle, newAuthor ->
                viewModel.updateBookDetails(book.id, newTitle, newAuthor)
                editingBook = null
            }
        )
    }

    actionChooserBook?.let { chosenBook ->
        ReadOrPlayChooserSheet(
            book = chosenBook,
            coverFile = viewModel.getCoverFile(chosenBook.id),
            onReadSelected = {
                actionChooserBook = null
                onOpenBookRead(chosenBook)
            },
            onPlaySelected = {
                actionChooserBook = null
                onOpenBookPlay(chosenBook)
            },
            onToggleArchive = {
                viewModel.toggleArchive(chosenBook)
                actionChooserBook = null
            },
            onDismiss = { actionChooserBook = null }
        )
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun FeaturedCoverTile(
    book: BookItem,
    coverFile: File?,
    showAudioBadge: Boolean,
    onClick: () -> Unit
) {
    val spineColor = parseHexColor(book.coverColorHex, MaterialTheme.colorScheme.secondary)
    Column(
        modifier = Modifier
            .width(126.dp)
            .clickable(onClick = onClick)
            .testTag("featured_book_${book.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
        ) {
            BookCoverArtwork(
                title = book.title,
                author = book.author,
                spineColor = spineColor,
                coverFile = coverFile,
                modifier = Modifier.fillMaxSize()
            )
            if (showAudioBadge) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = "Audio Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReferenceBookListCard(
    book: BookItem,
    coverFile: File?,
    hasLocalPdf: Boolean,
    onOpen: () -> Unit,
    onRead: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onEditDetails: () -> Unit,
    onRelinkPdf: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val spineColor = parseHexColor(book.coverColorHex, MaterialTheme.colorScheme.secondary)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("book_card_${book.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverArtwork(
                title = book.title,
                author = book.author,
                spineColor = spineColor,
                coverFile = coverFile,
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(0.74f)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("favorite_button_${book.id}")
                        ) {
                            Icon(
                                imageVector = if (book.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (book.favorite) "Remove favorite" else "Favorite book",
                                tint = if (book.favorite) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Book options",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (book.archived) "Restore from Archive" else "Archive Book") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (book.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleArchive()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit title & author") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEditDetails()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Re-link PDF file") },
                                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRelinkPdf()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete book") },
                                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }

                // Meta line matching reference UI's "Ebook • ★ 4.9"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (book.completed) "Finished • ${book.totalPagesInt} pp"
                        else "Page ${book.currentPageInt + 1}/${book.totalPagesInt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (book.completed) Icons.Filled.CheckCircle else Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${book.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Compact 2-option Read / Play pills right on card
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onRead)
                            .testTag("card_read_${book.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (hasLocalPdf) Icons.AutoMirrored.Filled.MenuBook else Icons.Filled.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (hasLocalPdf) "Read" else "Link PDF",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    if (hasLocalPdf) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable(onClick = onPlay)
                                .testTag("card_play_${book.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Play",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadOrPlayChooserSheet(
    book: BookItem,
    coverFile: File?,
    onReadSelected: () -> Unit,
    onPlaySelected: () -> Unit,
    onToggleArchive: () -> Unit,
    onDismiss: () -> Unit
) {
    val spineColor = parseHexColor(book.coverColorHex, MaterialTheme.colorScheme.secondary)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCoverArtwork(
                    title = book.title,
                    author = book.author,
                    spineColor = spineColor,
                    coverFile = coverFile,
                    modifier = Modifier
                        .width(64.dp)
                        .aspectRatio(0.7f)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Choose how to open",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.author} • Page ${book.currentPageInt + 1} of ${book.totalPagesInt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Option 1: Read
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onReadSelected)
                    .testTag("chooser_read_option")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "1. Read Full Book",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Page scroll or 3D flip reader with line-by-line selection & translation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Option 2: Play
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlaySelected)
                    .testTag("chooser_play_option")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "2. Play Full Book & Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Continuous page-by-page Audio Play & Book Small Summary in all languages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onToggleArchive,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (book.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (book.archived) "Restore Book from Archive" else "Move Book to Archive")
            }
        }
    }
}

@Composable
fun BookCoverArtwork(
    title: String,
    author: String,
    spineColor: Color,
    coverFile: File?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 12.dp, bottomEnd = 12.dp),
        shadowElevation = 4.dp,
        color = spineColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverFile != null) {
                AsyncImage(
                    model = coverFile,
                    contentDescription = "Cover of $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(spineColor, spineColor.copy(alpha = 0.78f))
                            )
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = author,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Realistic book spine shadow along left edge
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun EmptyBookshelfState(
    onUploadPdf: () -> Unit,
    onLoadSample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_shelf),
            contentDescription = "Empty wooden bookshelf waiting for books",
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(190.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.empty_shelf_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_shelf_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onUploadPdf,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_upload_pdf_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.upload_pdf), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onLoadSample,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("load_sample_book_button")
            ) {
                Icon(Icons.Filled.AutoStories, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.load_sample_book))
            }
        }
    }
}

@Composable
private fun EditBookDialog(
    book: BookItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Book Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, author) },
                enabled = title.trim().isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun parseHexColor(hex: String, fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}
