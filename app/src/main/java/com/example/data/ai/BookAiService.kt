package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.LanguageOption
import com.example.data.pdf.ExtractedPageLine
import com.example.data.pdf.LocalPdfTextExtractor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class BookAiService(
    private val context: Context? = null
) {

    private val primaryModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val liteModel by lazy {
        Firebase.ai.generativeModel("gemini-3.5-flash-lite")
    }

    suspend fun generateSmallSummary(
        pdfFile: File?,
        bookTitle: String,
        author: String,
        targetLanguage: LanguageOption,
        fallbackPageBitmaps: List<Bitmap> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val localOpeningText = if (context != null) {
            LocalPdfTextExtractor.extractOpeningPagesText(context, pdfFile, maxPages = 5)
        } else {
            ""
        }

        val promptText = buildString {
            append("You are an expert literary editor. Provide a clear, concise 'Book Small Summary' ")
            append("written entirely in ${targetLanguage.displayName} (${targetLanguage.nativeName}, BCP-47: ${targetLanguage.tag}).\n")
            append("Format:\n")
            append("1) One crisp overview paragraph (3-4 sentences).\n")
            append("2) 3 short bullet points highlighting key ideas or themes.\n")
            append("Keep total length under 160 words. Do not use markdown code fences.\n")
            append("Book Title: \"$bookTitle\" by $author.")
            if (localOpeningText.isNotBlank()) {
                append("\n\nOpening book text:\n")
                append(localOpeningText.take(4500))
            }
        }

        val cloudResult = tryCloudModels { useLite ->
            val model = if (useLite) liteModel else primaryModel
            val inputContent = content {
                if (localOpeningText.isBlank()) {
                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() in 1L..12_000_000L) {
                        inlineData(pdfFile.readBytes(), "application/pdf")
                    } else if (fallbackPageBitmaps.isNotEmpty()) {
                        fallbackPageBitmaps.take(4).forEach { bmp -> image(bmp) }
                    }
                }
                text(promptText)
            }
            withTimeout(60_000L) { model.generateContent(inputContent) }.text?.trim()
        }

        cloudResult.fold(
            onSuccess = { text -> Result.success(text.take(3800)) },
            onFailure = { err ->
                // Clean on-device fallback so quota limits never show raw API error walls
                val fallback = buildLocalBookSummary(
                    bookTitle = bookTitle,
                    author = author,
                    openingText = localOpeningText,
                    targetLanguage = targetLanguage,
                    fallbackBitmaps = fallbackPageBitmaps
                )
                if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(IllegalStateException(sanitizeError(err, "Unable to generate book summary right now.")))
                }
            }
        )
    }

    suspend fun extractPageTextForAudio(
        pdfFile: File?,
        pageIndex: Int,
        pageBitmap: Bitmap,
        targetLanguage: LanguageOption
    ): Result<String> = withContext(Dispatchers.IO) {
        val pageNumber = pageIndex + 1
        // 1. Always try fast, quota-free on-device PDFBox + ML Kit OCR extraction first
        val localText = LocalPdfTextExtractor.extractPageSpeechText(
            context = context,
            pdfFile = pdfFile,
            pageIndex = pageIndex,
            pageBitmap = pageBitmap
        )

        if (localText.isNotBlank()) {
            // If user wants English (or text isn't pure Latin and likely already localized), speak immediately with 0 API calls
            val isMostlyLatin = localText.count { it.code <= 0x024F } >= (localText.length * 0.7f)
            if (targetLanguage.tag.equals("en", ignoreCase = true) || !isMostlyLatin) {
                return@withContext Result.success(localText)
            }

            // User requested narration translated into another language: try Gemini -> ML Kit Translate -> clean local text
            val translatedCloud = tryCloudModels { useLite ->
                val model = if (useLite) liteModel else primaryModel
                val prompt = "Translate this book page narration naturally into ${targetLanguage.displayName} (${targetLanguage.nativeName}) " +
                    "ready for text-to-speech playback. Output ONLY the spoken text without markdown.\n\n${localText.take(4000)}"
                withTimeout(45_000L) { model.generateContent(prompt) }.text?.trim()
            }
            if (translatedCloud.isSuccess) {
                return@withContext translatedCloud
            }

            val offlineTranslated = LocalPdfTextExtractor.translateOfflineMlKit(localText, targetLanguage.tag)
            if (offlineTranslated.isSuccess) {
                return@withContext offlineTranslated
            }

            // Still play the original local page text instead of failing Audio Play!
            return@withContext Result.success(localText)
        }

        // 2. Only if both PDFBox and on-device OCR returned empty, ask Gemini Vision
        val cloudVisionResult = tryCloudModels { useLite ->
            val model = if (useLite) liteModel else primaryModel
            val promptText = "Read and transcribe all main body text from page $pageNumber of this book image, " +
                "outputting clean natural spoken prose in ${targetLanguage.displayName} (${targetLanguage.nativeName}). " +
                "Omit headers, page numbers, and markdown formatting."
            withTimeout(45_000L) {
                model.generateContent(
                    content {
                        image(pageBitmap)
                        text(promptText)
                    }
                )
            }.text?.trim()
        }

        cloudVisionResult.recoverCatching { err ->
            throw IllegalStateException(
                sanitizeError(err, "No readable text found on page $pageNumber.")
            )
        }
    }

    suspend fun extractPageLines(
        pdfFile: File?,
        pageIndex: Int,
        pageBitmap: Bitmap
    ): Result<List<ExtractedPageLine>> = withContext(Dispatchers.IO) {
        val pageNumber = pageIndex + 1

        // 1. Instant on-device extraction (ML Kit OCR with line bounding boxes + PDFBox text stream)
        val localLines = LocalPdfTextExtractor.extractPageLines(
            context = context,
            pdfFile = pdfFile,
            pageIndex = pageIndex,
            pageBitmap = pageBitmap
        )
        if (localLines.isNotEmpty()) {
            return@withContext Result.success(localLines)
        }

        // 2. Fallback to Gemini only if local extraction yielded zero text (e.g., handwritten/decorative page)
        val cloudLines = tryCloudModels { useLite ->
            val model = if (useLite) liteModel else primaryModel
            val promptText = "Extract every readable line or sentence from page $pageNumber of this book image in reading order. " +
                "Return ONLY the lines, one line per row, without bullet symbols, numbering prefixes, or markdown formatting."
            withTimeout(45_000L) {
                model.generateContent(
                    content {
                        image(pageBitmap)
                        text(promptText)
                    }
                )
            }.text?.trim()
        }

        cloudLines.fold(
            onSuccess = { raw ->
                val parsed = raw.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(150)
                    .toList()
                if (parsed.isEmpty()) {
                    Result.failure(IllegalStateException("No selectable lines detected on page $pageNumber."))
                } else {
                    val step = 1f / parsed.size.toFloat()
                    Result.success(
                        parsed.mapIndexed { idx, line ->
                            ExtractedPageLine(line, idx * step, ((idx + 1) * step).coerceAtMost(1f))
                        }
                    )
                }
            },
            onFailure = { err ->
                Result.failure(
                    IllegalStateException(
                        sanitizeError(err, "No selectable lines detected on page $pageNumber.")
                    )
                )
            }
        )
    }

    suspend fun translateLines(
        lines: List<String>,
        targetLanguage: LanguageOption
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanInput = lines.joinToString("\n") { it.trim() }.trim()
        if (cleanInput.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Select at least one line to translate."))
        }

        val promptText = buildString {
            append("Translate the following book lines accurately and naturally into ")
            append("${targetLanguage.displayName} (${targetLanguage.nativeName}, BCP-47: ${targetLanguage.tag}).\n")
            append("Preserve line breaks cleanly. Output ONLY the translated text without commentary or markdown.\n\n")
            append(cleanInput)
        }

        val cloudResult = tryCloudModels { useLite ->
            val model = if (useLite) liteModel else primaryModel
            withTimeout(40_000L) { model.generateContent(promptText) }.text?.trim()
        }

        if (cloudResult.isSuccess) {
            return@withContext cloudResult
        }

        // Fallback to on-device ML Kit Translation when cloud AI quota is exhausted
        val offlineResult = LocalPdfTextExtractor.translateOfflineMlKit(cleanInput, targetLanguage.tag)
        if (offlineResult.isSuccess) {
            return@withContext offlineResult
        }

        val err = cloudResult.exceptionOrNull() ?: offlineResult.exceptionOrNull()
        Result.failure(
            IllegalStateException(
                sanitizeError(err, "Translation unavailable right now. Try again shortly.")
            )
        )
    }

    private suspend fun buildLocalBookSummary(
        bookTitle: String,
        author: String,
        openingText: String,
        targetLanguage: LanguageOption,
        fallbackBitmaps: List<Bitmap>
    ): String? {
        var sourceText = openingText
        if (sourceText.isBlank() && fallbackBitmaps.isNotEmpty()) {
            val ocrLines = fallbackBitmaps.take(3).flatMap { bmp ->
                LocalPdfTextExtractor.extractLinesWithOcr(bmp).map { it.text }
            }
            sourceText = ocrLines.joinToString(" ")
        }
        if (sourceText.isBlank()) return null

        val sentences = sourceText
            .replace(Regex("\\s+"), " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 24 }
            .distinct()
            .take(5)

        val overview = if (sentences.isNotEmpty()) {
            sentences.take(2).joinToString(" ")
        } else {
            sourceText.take(260)
        }
        val bullets = sentences.drop(2).ifEmpty {
            listOf(sourceText.take(140))
        }.take(3)

        val englishSummary = buildString {
            append("$bookTitle by $author\n\n")
            append(overview)
            append("\n\nKey Highlights:")
            bullets.forEach { bullet ->
                append("\n• ").append(bullet)
            }
        }

        if (!targetLanguage.tag.equals("en", ignoreCase = true)) {
            val translated = LocalPdfTextExtractor.translateOfflineMlKit(englishSummary, targetLanguage.tag)
            if (translated.isSuccess) {
                return translated.getOrNull()
            }
        }
        return englishSummary
    }

    private suspend fun tryCloudModels(
        invoke: suspend (useLite: Boolean) -> String?
    ): Result<String> {
        return try {
            val first = invoke(false)
            if (!first.isNullOrBlank()) {
                Result.success(first)
            } else {
                Result.failure(IllegalStateException("Empty AI response."))
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.w(TAG, "Primary model timed out, trying lite model", e)
                tryLiteFallback(invoke, e)
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary Gemini model failed, trying lite fallback", e)
            tryLiteFallback(invoke, e)
        }
    }

    private suspend fun tryLiteFallback(
        invoke: suspend (useLite: Boolean) -> String?,
        originalError: Exception
    ): Result<String> {
        return try {
            val second = invoke(true)
            if (!second.isNullOrBlank()) {
                Result.success(second)
            } else {
                Result.failure(originalError)
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Result.failure(IllegalStateException("Request timed out. Please try again."))
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lite Gemini model also failed", e)
            Result.failure(e)
        }
    }

    private fun sanitizeError(err: Throwable?, fallback: String): String {
        val msg = err?.message.orEmpty()
        return when {
            msg.contains("quota", ignoreCase = true) ||
                msg.contains("rate-limit", ignoreCase = true) ||
                msg.contains("rate limit", ignoreCase = true) ||
                msg.contains("429") ->
                "Daily cloud AI quota reached. Local PDF reading and audio playback remain available."
            msg.contains("timeout", ignoreCase = true) ->
                "Request timed out. Please try again."
            msg.isBlank() -> fallback
            else -> msg.lineSequence().firstOrNull()?.take(180) ?: fallback
        }
    }

    private companion object {
        const val TAG = "BookAiService"
    }
}
