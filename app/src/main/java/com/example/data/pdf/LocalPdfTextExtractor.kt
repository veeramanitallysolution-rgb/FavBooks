package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

data class ExtractedPageLine(
    val text: String,
    val topFraction: Float,
    val bottomFraction: Float
)

data class PageWordToken(
    val index: Int,
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

object LocalPdfTextExtractor {

    private const val TAG = "LocalPdfTextExtractor"

    @Volatile
    private var pdfBoxInitialized = false

    private fun ensurePdfBox(context: Context) {
        if (!pdfBoxInitialized) {
            synchronized(this) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxInitialized = true
                }
            }
        }
    }

    suspend fun extractPageTextFromPdf(
        context: Context,
        pdfFile: File?,
        pageIndex: Int
    ): String = withContext(Dispatchers.IO) {
        if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0L) return@withContext ""
        runCatching {
            ensurePdfBox(context)
            PDDocument.load(pdfFile).use { doc ->
                if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return@use ""
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = pageIndex + 1
                    endPage = pageIndex + 1
                }
                stripper.getText(doc).trim()
            }
        }.onFailure { err ->
            Log.w(TAG, "PDFBox page text extraction failed on page ${pageIndex + 1}", err)
        }.getOrDefault("")
    }

    suspend fun extractOpeningPagesText(
        context: Context,
        pdfFile: File?,
        maxPages: Int = 5
    ): String = withContext(Dispatchers.IO) {
        if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0L) return@withContext ""
        runCatching {
            ensurePdfBox(context)
            PDDocument.load(pdfFile).use { doc ->
                val end = doc.numberOfPages.coerceAtMost(maxPages)
                if (end <= 0) return@use ""
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = 1
                    endPage = end
                }
                stripper.getText(doc).trim().take(6000)
            }
        }.onFailure { err ->
            Log.w(TAG, "PDFBox opening pages extraction failed", err)
        }.getOrDefault("")
    }

    suspend fun extractLinesWithOcr(pageBitmap: Bitmap): List<ExtractedPageLine> =
        withContext(Dispatchers.Default) {
            val h = pageBitmap.height.coerceAtLeast(1).toFloat()
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(pageBitmap, 0)
            suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val lines = visionText.textBlocks
                            .flatMap { it.lines }
                            .mapNotNull { line ->
                                val clean = line.text.trim()
                                if (clean.isEmpty()) null
                                else {
                                    val box = line.boundingBox
                                    val top = ((box?.top ?: 0) / h).coerceIn(0f, 1f)
                                    val bottom = ((box?.bottom ?: 0) / h).coerceIn(top, 1f)
                                    ExtractedPageLine(clean, top, bottom)
                                }
                            }
                            .sortedBy { it.topFraction }
                            .take(150)
                        if (cont.isActive) cont.resume(lines)
                    }
                    .addOnFailureListener { err ->
                        Log.w(TAG, "ML Kit OCR failed", err)
                        if (cont.isActive) cont.resume(emptyList())
                    }
            }
        }

    suspend fun extractWordTokens(pageBitmap: Bitmap): List<PageWordToken> =
        withContext(Dispatchers.Default) {
            val w = pageBitmap.width.coerceAtLeast(1).toFloat()
            val h = pageBitmap.height.coerceAtLeast(1).toFloat()
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(pageBitmap, 0)
            suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val sortedLines = visionText.textBlocks
                            .flatMap { it.lines }
                            .sortedWith(
                                compareBy<com.google.mlkit.vision.text.Text.Line>(
                                    { ((it.boundingBox?.top ?: 0) / h) },
                                    { ((it.boundingBox?.left ?: 0) / w) }
                                )
                            )
                        val rawWords = mutableListOf<PageWordToken>()
                        var idx = 0
                        for (line in sortedLines) {
                            val sortedElements = line.elements.sortedBy { it.boundingBox?.left ?: 0 }
                            for (element in sortedElements) {
                                val wordText = element.text.trim()
                                val box = element.boundingBox ?: continue
                                if (wordText.isEmpty()) continue
                                val l = (box.left / w).coerceIn(0f, 1f)
                                val t = (box.top / h).coerceIn(0f, 1f)
                                val r = (box.right / w).coerceIn(l + 0.002f, 1f)
                                val b = (box.bottom / h).coerceIn(t + 0.002f, 1f)
                                rawWords.add(PageWordToken(idx++, wordText, l, t, r, b))
                            }
                        }
                        if (cont.isActive) cont.resume(rawWords)
                    }
                    .addOnFailureListener { err ->
                        Log.w(TAG, "ML Kit word token extraction failed", err)
                        if (cont.isActive) cont.resume(emptyList())
                    }
            }
        }

    suspend fun extractPageLines(
        context: Context?,
        pdfFile: File?,
        pageIndex: Int,
        pageBitmap: Bitmap
    ): List<ExtractedPageLine> {
        val pdfText = if (context != null) {
            extractPageTextFromPdf(context, pdfFile, pageIndex)
        } else {
            ""
        }

        // If the embedded PDF contains non-Latin Unicode scripts (e.g. Tamil, Hindi, Arabic, CJK),
        // preserve the exact Unicode lines from PDFBox directly.
        val hasNonLatinScript = pdfText.any { ch -> ch.code > 0x024F }
        if (hasNonLatinScript && pdfText.isNotBlank()) {
            return splitPlainTextIntoLines(pdfText)
        }

        // Otherwise prefer ML Kit OCR on the rendered page bitmap so visual line order
        // and y-bounding-boxes align accurately with what the user sees & long-presses.
        val ocrLines = extractLinesWithOcr(pageBitmap)
        if (ocrLines.isNotEmpty()) {
            return ocrLines
        }

        if (pdfText.isNotBlank()) {
            return splitPlainTextIntoLines(pdfText)
        }

        return emptyList()
    }

    suspend fun extractPageSpeechText(
        context: Context?,
        pdfFile: File?,
        pageIndex: Int,
        pageBitmap: Bitmap
    ): String {
        val pdfText = if (context != null) {
            extractPageTextFromPdf(context, pdfFile, pageIndex)
        } else {
            ""
        }
        if (pdfText.isNotBlank()) {
            return normalizeForSpeech(pdfText)
        }
        val ocrLines = extractLinesWithOcr(pageBitmap)
        if (ocrLines.isNotEmpty()) {
            return normalizeForSpeech(ocrLines.joinToString(" ") { it.text })
        }
        return ""
    }

    suspend fun translateOfflineMlKit(
        text: String,
        targetLanguageTag: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Nothing to translate."))

        val mlKitTarget = TranslateLanguage.fromLanguageTag(targetLanguageTag)
            ?: TranslateLanguage.fromLanguageTag(targetLanguageTag.substringBefore('-'))
            ?: return@withContext Result.failure(
                UnsupportedOperationException("Offline translation pack is unavailable for this language.")
            )

        if (mlKitTarget == TranslateLanguage.ENGLISH) {
            return@withContext Result.success(clean)
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(mlKitTarget)
            .build()
        val translator = Translation.getClient(options)

        try {
            val ready = suspendCancellableCoroutine<Boolean> { cont ->
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                    .addOnSuccessListener { if (cont.isActive) cont.resume(true) }
                    .addOnFailureListener { err ->
                        Log.w(TAG, "ML Kit translation model download failed", err)
                        if (cont.isActive) cont.resume(false)
                    }
            }
            if (!ready) {
                return@withContext Result.failure(
                    IllegalStateException("Could not download offline translation language pack.")
                )
            }

            val translated = suspendCancellableCoroutine<String?> { cont ->
                translator.translate(clean)
                    .addOnSuccessListener { out -> if (cont.isActive) cont.resume(out) }
                    .addOnFailureListener { err ->
                        Log.w(TAG, "ML Kit translation failed", err)
                        if (cont.isActive) cont.resume(null)
                    }
            }

            if (translated.isNullOrBlank()) {
                Result.failure(IllegalStateException("Translation could not be completed."))
            } else {
                Result.success(translated.trim())
            }
        } finally {
            runCatching { translator.close() }
        }
    }

    fun normalizeForSpeech(raw: String): String {
        return raw
            .replace(Regex("-\\s*\\n\\s*"), "")
            .replace(Regex("\\s*\\n+\\s*"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun splitPlainTextIntoLines(text: String): List<ExtractedPageLine> {
        val rawLines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(150)
            .toList()
        if (rawLines.isEmpty()) return emptyList()
        val step = 1f / rawLines.size.toFloat()
        return rawLines.mapIndexed { idx, line ->
            ExtractedPageLine(
                text = line,
                topFraction = idx * step,
                bottomFraction = ((idx + 1) * step).coerceAtMost(1f)
            )
        }
    }
}
