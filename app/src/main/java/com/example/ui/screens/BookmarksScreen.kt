package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BookItem
import com.example.data.model.UiState
import com.example.ui.viewmodel.BookshelfViewModel

@Composable
fun BookmarksScreen(
    viewModel: BookshelfViewModel,
    onOpenBookmark: (BookItem, Int) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarksUiState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val booksUiState by viewModel.booksState.collectAsStateWithLifecycle()
    val booksById = ((booksUiState as? UiState.Success)?.data ?: emptyList()).associateBy { it.id }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = bookmarksUiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                val bookmarks = state.data
                if (bookmarks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmarks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No bookmarks yet",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = "While reading any PDF, tap the bookmark ribbon in the top bar to save passages and jump right back later.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bm ->
                            BookmarkRowCard(
                                bookmark = bm,
                                showBookTitle = true,
                                onClick = {
                                    val targetBook = booksById[bm.bookId]
                                    if (targetBook == null) {
                                        onShowMessage("Book not found on shelf")
                                    } else if (!viewModel.hasLocalPdf(targetBook.id)) {
                                        onShowMessage("Link the PDF on your Bookshelf first to open this bookmark")
                                    } else {
                                        onOpenBookmark(targetBook, bm.pageIndex.toInt())
                                    }
                                },
                                onDelete = { viewModel.deleteBookmark(bm.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
