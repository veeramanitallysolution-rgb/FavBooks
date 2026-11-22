package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromRaw(raw: String?): AppThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class ReaderThemeMode(val label: String) {
    LIGHT("Paper"),
    SEPIA("Sepia"),
    DARK("Night");

    companion object {
        fun fromRaw(raw: String?): ReaderThemeMode =
            entries.firstOrNull { it.name == raw } ?: LIGHT
    }
}

enum class ReaderViewMode(val label: String) {
    PAGE_SCROLL("Page Scroll"),
    BOOK_FLIP("3D Flip");

    companion object {
        fun fromRaw(raw: String?): ReaderViewMode =
            entries.firstOrNull { it.name == raw } ?: PAGE_SCROLL
    }
}

enum class BookshelfFilter(val label: String) {
    ALL("All"),
    READING("Reading"),
    COMPLETED("Finished"),
    FAVORITES("Favorites"),
    UNREAD("Unread"),
    ARCHIVED("Archived")
}

enum class BookshelfSort(val label: String) {
    RECENT("Recently Read"),
    TITLE("Title A–Z"),
    PROGRESS("Progress")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Reader",
    val email: String? = null,
    val dailyGoalPages: Long = 20L,
    val totalPagesRead: Long = 0L,
    val streakDays: Long = 0L,
    val lastReadDate: String? = null,
    val reminderEnabled: Boolean = false,
    val reminderHour: Long = 20L,
    val reminderMinute: Long = 0L,
    val appTheme: String = AppThemeMode.SYSTEM.name,
    val readerTheme: String = ReaderThemeMode.LIGHT.name,
    val preferredLanguageTag: String = "en",
    val readerViewMode: String = ReaderViewMode.PAGE_SCROLL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedAppTheme: AppThemeMode get() = AppThemeMode.fromRaw(appTheme)
    val parsedReaderTheme: ReaderThemeMode get() = ReaderThemeMode.fromRaw(readerTheme)
    val parsedReaderViewMode: ReaderViewMode get() = ReaderViewMode.fromRaw(readerViewMode)
}

@Keep
@IgnoreExtraProperties
data class BookItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val author: String = "Unknown Author",
    val pageCount: Long = 1L,
    val currentPage: Long = 0L,
    val coverColorHex: String = "#1D2D44",
    val fileName: String = "book.pdf",
    val fileSizeBytes: Long = 0L,
    val favorite: Boolean = false,
    val completed: Boolean = false,
    val archived: Boolean = false,
    val summary: String? = null,
    val summaryLanguage: String? = null,
    @ServerTimestamp val lastReadAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalPagesInt: Int get() = pageCount.toInt().coerceAtLeast(1)
    val currentPageInt: Int get() = currentPage.toInt().coerceIn(0, (totalPagesInt - 1).coerceAtLeast(0))

    val progressFraction: Float
        get() = when {
            completed -> 1f
            totalPagesInt <= 1 -> if (currentPageInt > 0) 1f else 0f
            else -> ((currentPageInt + 1).toFloat() / totalPagesInt.toFloat()).coerceIn(0f, 1f)
        }

    val progressPercent: Int
        get() = if (currentPageInt == 0 && !completed && lastReadAt == null) 0
        else (progressFraction * 100).toInt().coerceIn(0, 100)
}

@Keep
@IgnoreExtraProperties
data class BookmarkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val pageIndex: Long = 0L,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val pageNumberDisplay: Int get() = pageIndex.toInt().coerceAtLeast(0) + 1
}

@Keep
@IgnoreExtraProperties
data class ReadingLogItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val pagesRead: Long = 0L,
    val minutesRead: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
