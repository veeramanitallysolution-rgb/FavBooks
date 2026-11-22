package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

data class ImportedPdfResult(
    val bookId: String,
    val title: String,
    val author: String,
    val pageCount: Int,
    val fileName: String,
    val fileSizeBytes: Long
)

class PdfStorageManager(private val context: Context) {

    private fun booksDir(userId: String): File =
        File(context.filesDir, "books/$userId").apply { mkdirs() }

    private fun coversDir(userId: String): File =
        File(context.filesDir, "covers/$userId").apply { mkdirs() }

    fun getPdfFile(userId: String, bookId: String): File =
        File(booksDir(userId), "$bookId.pdf")

    fun getCoverFile(userId: String, bookId: String): File =
        File(coversDir(userId), "$bookId.png")

    fun hasLocalPdf(userId: String, bookId: String): Boolean {
        val file = getPdfFile(userId, bookId)
        return file.exists() && file.length() > 0L
    }

    suspend fun importPdfFromUri(
        userId: String,
        bookId: String,
        uri: Uri
    ): Result<ImportedPdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = resolveDisplayName(uri) ?: "book_$bookId.pdf"
            val targetFile = getPdfFile(userId, bookId)
            val tempFile = File(targetFile.parentFile, "$bookId.tmp")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to read the selected PDF file.")

            if (tempFile.length() == 0L) {
                tempFile.delete()
                error("Selected PDF file is empty.")
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val pageCount = inspectAndRenderCover(userId, bookId, targetFile)
            val metadata = PdfMetadataExtractor.extract(targetFile, displayName)

            ImportedPdfResult(
                bookId = bookId,
                title = metadata.title,
                author = metadata.author,
                pageCount = pageCount,
                fileName = displayName.take(255),
                fileSizeBytes = targetFile.length()
            )
        }
    }

    suspend fun generateSamplePdfBook(
        userId: String,
        bookId: String
    ): Result<ImportedPdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = getPdfFile(userId, bookId)
            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val chapters = listOf(
                Triple(
                    "The Art of Thoughtful Reading",
                    "A Digital Library Edition",
                    "Welcome to Favbooks.\n\nThis sample edition is a genuine multi-page PDF generated on your device and rendered natively through Android's PdfRenderer.\n\nSwipe left or right, tap page edges, or use the bottom controls to experience smooth 3D page-turning physics. Pinch to zoom in on fine typography, toggle Paper, Sepia, or Night themes, and bookmark passages you wish to revisit."
                ),
                Triple(
                    "Chapter I • Quiet Spaces",
                    "On Building a Personal Sanctuary",
                    "A private library is more than an archive of files; it is a quiet room of the mind. Every volume we finish leaves behind a trace—a margin note, a remembered phrase, or a shift in perspective.\n\nBy keeping your reading progress synced across sessions, you never lose your place between chapters. Even ten quiet pages before sleep builds momentum over the course of a year."
                ),
                Triple(
                    "Chapter II • Typography & Paper",
                    "Why Contrast & Rhythm Matter",
                    "Legibility begins with generous margins, clear line height, and comfortable contrast against the background.\n\nDuring bright mornings, crisp paper feels effortless. In the afternoon, warm sepia reduces glare. At night, inverted dark pages let you read without straining your eyes. Switch reading themes anytime from the reader toolbar."
                ),
                Triple(
                    "Chapter III • Two-Page Spreads",
                    "Adapting Across Devices",
                    "On a handheld phone in portrait orientation, a single full-width page keeps typography crisp and comfortable for one-handed reading.\n\nRotate your device to landscape—or open your bookshelf on a foldable or tablet—and the reader opens into a classic two-page book spread with a central binding shadow."
                ),
                Triple(
                    "Chapter IV • Daily Habits",
                    "The Compound Effect of 20 Pages",
                    "Reading twenty pages a day equals more than seven thousand pages in a single year—roughly twenty-five full-length books.\n\nUse the Progress dashboard to adjust your daily page target, inspect your seven-day reading chart, and schedule a gentle evening reminder so your streak stays unbroken."
                ),
                Triple(
                    "Chapter V • Bookmarks & Notes",
                    "Capturing Ideas While You Read",
                    "Whenever a paragraph stands out, tap the Bookmark ribbon in the top corner of the reader and jot down a short reflection.\n\nAll of your bookmarks remain organized by book in your Bookmarks tab, ready to jump you directly back to the exact page with one tap."
                ),
                Triple(
                    "Chapter VI • Your Own PDFs",
                    "Bringing Your Collection Together",
                    "Tap the Upload PDF button on your Bookshelf to import any PDF document from your device—novels, technical manuals, research papers, or sheet music.\n\nTitle, author, page count, and cover artwork are extracted automatically so your shelves look curated from the moment you add a file."
                ),
                Triple(
                    "Colophon",
                    "End of Sample Edition",
                    "You have reached the final page of this bundled sample PDF.\n\nFeel free to keep it on your shelf as a benchmark, or upload your own favorite PDFs from your device storage to begin building your personal digital library."
                )
            )

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 58, 52)
                textSize = 26f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(184, 93, 59)
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(32, 30, 28)
                textSize = 14.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(120, 112, 102)
                textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(212, 198, 178)
                strokeWidth = 1.5f
            }

            chapters.forEachIndexed { index, (heading, subheading, body) ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Warm paper tint
                canvas.drawColor(Color.rgb(252, 249, 243))

                if (index == 0) {
                    // Cover page styling
                    val coverBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(30, 58, 52)
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                    }
                    canvas.drawRect(36f, 36f, pageWidth - 36f, pageHeight - 36f, coverBorderPaint)

                    val accentBand = Paint().apply { color = Color.rgb(30, 58, 52) }
                    canvas.drawRect(36f, 36f, pageWidth - 36f, 190f, accentBand)

                    val coverTitlePaint = TextPaint(titlePaint).apply {
                        color = Color.WHITE
                        textSize = 30f
                    }
                    drawWrappedText(canvas, heading, coverTitlePaint, 64f, 88f, pageWidth - 128)
                    val coverSubPaint = TextPaint(subtitlePaint).apply {
                        color = Color.rgb(245, 223, 204)
                        textSize = 15f
                    }
                    canvas.drawText(subheading, 64f, 162f, coverSubPaint)
                    drawWrappedText(canvas, body, bodyPaint, 64f, 240f, pageWidth - 128)
                } else {
                    canvas.drawText(subheading.uppercase(), 56f, 76f, subtitlePaint)
                    val titleHeight = drawWrappedText(canvas, heading, titlePaint, 56f, 94f, pageWidth - 112)
                    val ruleY = 94f + titleHeight + 14f
                    canvas.drawLine(56f, ruleY, pageWidth - 56f, ruleY, rulePaint)
                    drawWrappedText(canvas, body, bodyPaint, 56f, ruleY + 28f, pageWidth - 112)
                }

                canvas.drawText("— ${index + 1} of ${chapters.size} —", pageWidth / 2f, pageHeight - 40f, footerPaint)
                document.finishPage(page)
            }

            FileOutputStream(targetFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            val pageCount = inspectAndRenderCover(userId, bookId, targetFile)
            ImportedPdfResult(
                bookId = bookId,
                title = "The Art of Thoughtful Reading",
                author = "Editorial Press",
                pageCount = pageCount,
                fileName = "the_art_of_thoughtful_reading.pdf",
                fileSizeBytes = targetFile.length()
            )
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int
    ): Int {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1.25f)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height
    }

    private fun inspectAndRenderCover(userId: String, bookId: String, pdfFile: File): Int {
        val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val count = renderer.pageCount.coerceAtLeast(1)
                renderer.openPage(0).use { page ->
                    val targetWidth = 480
                    val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                    val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val coverFile = getCoverFile(userId, bookId)
                    FileOutputStream(coverFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                    bitmap.recycle()
                }
                return count
            }
        }
    }

    fun deleteLocalBookFiles(userId: String, bookId: String) {
        getPdfFile(userId, bookId).delete()
        getCoverFile(userId, bookId).delete()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')
    }
}

/**
 * Thread-safe PDF page renderer with an in-memory LRU cache for crisp, flicker-free
 * 3D page turns and pinch-to-zoom.
 */
class PdfPageSession(private val pdfFile: File) : Closeable {
    private val mutex = Mutex()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    private val cache = object : LruCache<String, Bitmap>(16) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Let GC collect evicted bitmaps safely without recycling while Compose may still draw a frame.
        }
    }

    suspend fun open(): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (renderer == null) {
                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                descriptor = pfd
                renderer = PdfRenderer(pfd)
            }
            renderer?.pageCount ?: 0
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val pdfRenderer = renderer ?: run {
                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                descriptor = pfd
                PdfRenderer(pfd).also { renderer = it }
            }
            if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) return@withLock null

            val bucketWidth = targetWidthPx.coerceIn(600, 1800)
            val cacheKey = "${pageIndex}_$bucketWidth"
            cache.get(cacheKey)?.let { return@withLock it }

            pdfRenderer.openPage(pageIndex).use { page ->
                val ratio = bucketWidth.toFloat() / page.width.coerceAtLeast(1)
                val heightPx = (page.height * ratio).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(bucketWidth, heightPx, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(cacheKey, bitmap)
                bitmap
            }
        }
    }

    override fun close() {
        cache.evictAll()
        runCatching { renderer?.close() }
        runCatching { descriptor?.close() }
        renderer = null
        descriptor = null
    }
}
