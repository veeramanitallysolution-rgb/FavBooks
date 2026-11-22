package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.LanguageOption
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class BookAiService {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun generateSmallSummary(
        pdfFile: File?,
        bookTitle: String,
        author: String,
        targetLanguage: LanguageOption,
        fallbackPageBitmaps: List<Bitmap> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val promptText = buildString {
                append("You are an expert literary editor. Provide a clear, concise 'Book Small Summary' ")
                append("written entirely in ${targetLanguage.displayName} (${targetLanguage.nativeName}, BCP-47: ${targetLanguage.tag}).\n")
                append("Format:\n")
                append("1) One crisp overview paragraph (3-4 sentences).\n")
                append("2) 3 short bullet points highlighting key ideas or themes.\n")
                append("Keep total length under 160 words. Do not use markdown code fences.\n")
                append("Book Title: \"$bookTitle\" by $author.")
            }

            val inputContent = content {
                if (pdfFile != null && pdfFile.exists() && pdfFile.length() in 1L..12_000_000L) {
                    inlineData(pdfFile.readBytes(), "application/pdf")
                } else if (fallbackPageBitmaps.isNotEmpty()) {
                    fallbackPageBitmaps.take(4).forEach { bmp ->
                        image(bmp)
                    }
                }
                text(promptText)
            }

            val response = withTimeout(60_000L) {
                model.generateContent(inputContent)
            }

            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Empty summary returned."))
            } else {
                Result.success(text.take(3800))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Book summary generation timed out", e)
            Result.failure(IllegalStateException("Summary generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate book summary", e)
            Result.failure(e)
        }
    }

    suspend fun extractPageTextForAudio(
        pageBitmap: Bitmap,
        pageNumber: Int,
        targetLanguage: LanguageOption
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val promptText = "Read and transcribe all main body text from page $pageNumber of this book image, " +
                "translating naturally into ${targetLanguage.displayName} (${targetLanguage.nativeName}) if the page is in another language. " +
                "Output clean, natural spoken prose ready for text-to-speech narration. Omit headers, page numbers, and markdown formatting."

            val response = withTimeout(45_000L) {
                model.generateContent(
                    content {
                        image(pageBitmap)
                        text(promptText)
                    }
                )
            }

            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("No readable text detected on page $pageNumber."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Page audio transcription timed out", e)
            Result.failure(IllegalStateException("Audio preparation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract page text for audio", e)
            Result.failure(e)
        }
    }

    suspend fun extractPageLines(
        pageBitmap: Bitmap,
        pageNumber: Int
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val promptText = "Extract every readable line or sentence from page $pageNumber of this book image in reading order. " +
                "Return ONLY the lines, one line per row, without bullet symbols, numbering prefixes, or markdown formatting. " +
                "Skip decorative page headers/footers."

            val response = withTimeout(45_000L) {
                model.generateContent(
                    content {
                        image(pageBitmap)
                        text(promptText)
                    }
                )
            }

            val raw = response.text?.trim().orEmpty()
            val lines = raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(120)
                .toList()

            if (lines.isEmpty()) {
                Result.failure(IllegalStateException("No selectable lines detected on page $pageNumber."))
            } else {
                Result.success(lines)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Line extraction timed out", e)
            Result.failure(IllegalStateException("Line extraction timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract lines from page $pageNumber", e)
            Result.failure(e)
        }
    }

    suspend fun translateLines(
        lines: List<String>,
        targetLanguage: LanguageOption
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
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

            val response = withTimeout(40_000L) {
                model.generateContent(promptText)
            }

            val translated = response.text?.trim()
            if (translated.isNullOrBlank()) {
                Result.failure(IllegalStateException("Translation returned empty output."))
            } else {
                Result.success(translated)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Line translation timed out", e)
            Result.failure(IllegalStateException("Translation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to translate lines", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "BookAiService"
    }
}
