package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppThemeMode
import com.example.data.model.BookItem
import com.example.data.model.BookmarkItem
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReaderViewMode
import com.example.data.model.ReadingLogItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookshelfRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun booksCol(uid: String) = userDoc(uid).collection("books")
    private fun bookmarksCol(uid: String) = userDoc(uid).collection("bookmarks")
    private fun logsCol(uid: String) = userDoc(uid).collection("readingLogs")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val docRef = userDoc(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    fun observeBooks(userId: String): Flow<List<BookItem>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/books"
        emitAll(
            booksCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BookItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeBookmarks(userId: String): Flow<List<BookmarkItem>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/bookmarks"
        emitAll(
            bookmarksCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BookmarkItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeReadingLogs(userId: String): Flow<List<ReadingLogItem>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/readingLogs"
        emitAll(
            logsCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ReadingLogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        user: FirebaseUser,
        defaultLanguageTag: String = "en",
        customDisplayName: String? = null
    ): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Signed-in user mismatch." }
            val ref = userDoc(uid)
            val snapshot = ref.get().await()
            val chosenName = customDisplayName?.trim()?.take(80)?.takeIf { it.isNotEmpty() }
            if (!snapshot.exists()) {
                val safeName = chosenName
                    ?: (user.displayName ?: "Book Lover").trim().take(80).ifBlank { "Reader" }
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to user.email?.trim()?.take(120),
                    "dailyGoalPages" to 20L,
                    "totalPagesRead" to 0L,
                    "streakDays" to 0L,
                    "reminderEnabled" to false,
                    "reminderHour" to 20L,
                    "reminderMinute" to 0L,
                    "appTheme" to AppThemeMode.SYSTEM.name,
                    "readerTheme" to ReaderThemeMode.LIGHT.name,
                    "preferredLanguageTag" to defaultLanguageTag.take(24).ifBlank { "en" },
                    "readerViewMode" to ReaderViewMode.PAGE_SCROLL.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
            } else if (chosenName != null) {
                ref.update(
                    mapOf(
                        "displayName" to chosenName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileName(userId: String, displayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanName = displayName.trim().take(80).ifBlank { "Reader" }
            userDoc(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addBook(
        userId: String,
        bookId: String,
        title: String,
        author: String,
        pageCount: Int,
        coverColorHex: String,
        fileName: String,
        fileSizeBytes: Long
    ): Result<Unit> {
        val path = "users/$userId/books/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(bookId.isNotBlank()) { "Book ID required." }
            val safePages = pageCount.coerceIn(1, 20000).toLong()
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().take(200).ifBlank { "Untitled Book" },
                "author" to author.trim().take(120).ifBlank { "Unknown Author" },
                "pageCount" to safePages,
                "currentPage" to 0L,
                "coverColorHex" to coverColorHex.take(16).ifBlank { "#1D2D44" },
                "fileName" to fileName.trim().take(255).ifBlank { "book.pdf" },
                "fileSizeBytes" to fileSizeBytes.coerceIn(0L, 2_000_000_000L),
                "favorite" to false,
                "completed" to false,
                "archived" to false,
                "lastReadAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            booksCol(uid).document(bookId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBookSummary(
        userId: String,
        bookId: String,
        summary: String,
        summaryLanguage: String
    ): Result<Unit> {
        val path = "users/$userId/books/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            booksCol(uid).document(bookId).update(
                mapOf(
                    "summary" to summary.trim().take(4000),
                    "summaryLanguage" to summaryLanguage.trim().take(24).ifBlank { "en" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateReadingProgress(
        userId: String,
        book: BookItem,
        newPageIndex: Int,
        pagesTurnedDelta: Int,
        profile: UserProfile?
    ): Result<Unit> {
        val path = "users/$userId/books/${book.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(book.id.isNotBlank()) { "Book ID required." }

            val maxPageIdx = (book.totalPagesInt - 1).coerceAtLeast(0)
            val safePage = newPageIndex.coerceIn(0, maxPageIdx)
            val isCompleted = safePage >= maxPageIdx

            booksCol(uid).document(book.id).update(
                mapOf(
                    "currentPage" to safePage.toLong(),
                    "completed" to isCompleted,
                    "lastReadAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (pagesTurnedDelta > 0) {
                val todayKey = todayDateKey()
                val logRef = logsCol(uid).document(todayKey)
                val logSnap = logRef.get().await()
                val addedPages = pagesTurnedDelta.coerceIn(1, 500).toLong()
                val addedMinutes = (addedPages / 2L).coerceAtLeast(1L)

                if (logSnap.exists()) {
                    logRef.update(
                        mapOf(
                            "pagesRead" to FieldValue.increment(addedPages),
                            "minutesRead" to FieldValue.increment(addedMinutes),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    logRef.set(
                        mapOf(
                            "userId" to uid,
                            "dateKey" to todayKey,
                            "pagesRead" to addedPages,
                            "minutesRead" to addedMinutes,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                val newStreak = computeUpdatedStreak(
                    previousDateKey = profile?.lastReadDate,
                    previousStreak = profile?.streakDays ?: 0L,
                    todayKey = todayKey
                )
                userDoc(uid).update(
                    mapOf(
                        "totalPagesRead" to FieldValue.increment(addedPages),
                        "streakDays" to newStreak,
                        "lastReadDate" to todayKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(userId: String, bookId: String, favorite: Boolean): Result<Unit> {
        val path = "users/$userId/books/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            booksCol(uid).document(bookId).update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleArchive(userId: String, bookId: String, archived: Boolean): Result<Unit> {
        val path = "users/$userId/books/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            booksCol(uid).document(bookId).update(
                mapOf(
                    "archived" to archived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBookDetails(
        userId: String,
        bookId: String,
        title: String,
        author: String
    ): Result<Unit> {
        val path = "users/$userId/books/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            booksCol(uid).document(bookId).update(
                mapOf(
                    "title" to title.trim().take(200).ifBlank { "Untitled Book" },
                    "author" to author.trim().take(120).ifBlank { "Unknown Author" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBookFileMetadata(
        userId: String,
        bookId: String,
        pageCount: Int,
        fileName: String,
        fileSizeBytes: Long
    ): Result<Unit> {
        val path = "users/$userId/books/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            booksCol(uid).document(bookId).update(
                mapOf(
                    "pageCount" to pageCount.coerceIn(1, 20000).toLong(),
                    "fileName" to fileName.trim().take(255).ifBlank { "book.pdf" },
                    "fileSizeBytes" to fileSizeBytes.coerceIn(0L, 2_000_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBook(userId: String, bookId: String): Result<Unit> {
        val path = "users/$userId/books/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val relatedBookmarks = bookmarksCol(uid).whereEqualTo("bookId", bookId).get().await()
            for (doc in relatedBookmarks.documents) {
                doc.reference.delete().await()
            }
            booksCol(uid).document(bookId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addBookmark(
        userId: String,
        bookId: String,
        bookTitle: String,
        pageIndex: Int,
        note: String?
    ): Result<Unit> {
        val docRef = bookmarksCol(userId).document()
        val path = docRef.path
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanNote = note?.trim()?.take(300)?.takeIf { it.isNotEmpty() }
            val payload = mapOf(
                "userId" to uid,
                "bookId" to bookId,
                "bookTitle" to bookTitle.trim().take(200).ifBlank { "Untitled Book" },
                "pageIndex" to pageIndex.coerceIn(0, 19999).toLong(),
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBookmark(userId: String, bookmarkId: String): Result<Unit> {
        val path = "users/$userId/bookmarks/$bookmarkId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            bookmarksCol(uid).document(bookmarkId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String,
        dailyGoalPages: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        appTheme: AppThemeMode,
        readerTheme: ReaderThemeMode,
        preferredLanguageTag: String,
        readerViewMode: ReaderViewMode
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            userDoc(uid).update(
                mapOf(
                    "dailyGoalPages" to dailyGoalPages.coerceIn(1, 500).toLong(),
                    "reminderEnabled" to reminderEnabled,
                    "reminderHour" to reminderHour.coerceIn(0, 23).toLong(),
                    "reminderMinute" to reminderMinute.coerceIn(0, 59).toLong(),
                    "appTheme" to appTheme.name,
                    "readerTheme" to readerTheme.name,
                    "preferredLanguageTag" to preferredLanguageTag.trim().take(24).ifBlank { "en" },
                    "readerViewMode" to readerViewMode.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception) ?: return
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    companion object {
        fun todayDateKey(calendar: Calendar = Calendar.getInstance()): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return fmt.format(calendar.time)
        }

        fun computeUpdatedStreak(
            previousDateKey: String?,
            previousStreak: Long,
            todayKey: String,
            now: Calendar = Calendar.getInstance()
        ): Long {
            if (previousDateKey == todayKey) {
                return previousStreak.coerceAtLeast(1L)
            }
            val yesterdayCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayKey = todayDateKey(yesterdayCal)
            return if (previousDateKey == yesterdayKey) {
                (previousStreak + 1L).coerceAtLeast(1L)
            } else {
                1L
            }
        }
    }
}
