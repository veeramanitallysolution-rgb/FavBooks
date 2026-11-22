package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable
import java.text.BreakIterator
import java.util.Locale

data class AudioPlaybackState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val speechRate: Float = 1.0f,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val currentChunkText: String = "",
    val languageSupported: Boolean = true,
    val statusNote: String? = null
) {
    val progressFraction: Float
        get() = if (totalChunks <= 0) 0f else ((currentChunkIndex + 1).toFloat() / totalChunks.toFloat()).coerceIn(0f, 1f)
}

class BookAudioPlayer(context: Context) : Closeable {

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var chunks: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var currentLocale: Locale = Locale.ENGLISH
    private var speechRate: Float = 1.0f
    private var onCompletedCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureListener()
                applyLanguage(currentLocale)
                _state.value = _state.value.copy(isReady = true)
            } else {
                _state.value = _state.value.copy(
                    isReady = false,
                    statusNote = "Device speech engine unavailable."
                )
            }
        }
    }

    private fun configureListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.removePrefix("chunk_")?.toIntOrNull() ?: currentIndex
                currentIndex = idx
                _state.value = _state.value.copy(
                    isPlaying = true,
                    isPaused = false,
                    currentChunkIndex = idx,
                    currentChunkText = chunks.getOrElse(idx) { "" }
                )
            }

            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.removePrefix("chunk_")?.toIntOrNull() ?: currentIndex
                val next = idx + 1
                if (next < chunks.size) {
                    speakChunk(next)
                } else {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isPaused = false,
                        currentChunkIndex = (chunks.size - 1).coerceAtLeast(0)
                    )
                    onCompletedCallback?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isPaused = false,
                    statusNote = "Audio playback interrupted."
                )
            }
        })
    }

    fun setLanguage(languageTag: String) {
        val locale = Locale.forLanguageTag(languageTag)
        currentLocale = locale
        if (_state.value.isReady) {
            applyLanguage(locale)
        }
    }

    private fun applyLanguage(locale: Locale) {
        val engine = tts ?: return
        val result = engine.setLanguage(locale)
        val supported = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        _state.value = _state.value.copy(
            languageSupported = supported,
            statusNote = if (supported) null else "Voice pack for ${locale.displayName} not installed on device; using default voice fallback."
        )
        if (!supported) {
            engine.setLanguage(Locale.ENGLISH)
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.6f, 2.2f)
        tts?.setSpeechRate(speechRate)
        _state.value = _state.value.copy(speechRate = speechRate)
        if (_state.value.isPlaying) {
            speakChunk(currentIndex)
        }
    }

    fun playText(
        text: String,
        languageTag: String,
        onComplete: (() -> Unit)? = null
    ) {
        onCompletedCallback = onComplete
        setLanguage(languageTag)
        chunks = splitIntoSentences(text, currentLocale)
        currentIndex = 0
        if (chunks.isEmpty()) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isPaused = false,
                totalChunks = 0,
                currentChunkIndex = 0,
                currentChunkText = ""
            )
            return
        }
        _state.value = _state.value.copy(
            totalChunks = chunks.size,
            currentChunkIndex = 0,
            currentChunkText = chunks.first()
        )
        speakChunk(0)
    }

    fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(
            isPlaying = false,
            isPaused = chunks.isNotEmpty()
        )
    }

    fun resume() {
        if (chunks.isEmpty()) return
        speakChunk(currentIndex.coerceIn(0, chunks.lastIndex))
    }

    fun stop() {
        tts?.stop()
        currentIndex = 0
        _state.value = _state.value.copy(
            isPlaying = false,
            isPaused = false,
            currentChunkIndex = 0
        )
    }

    private fun speakChunk(index: Int) {
        val engine = tts ?: return
        if (index !in chunks.indices) return
        currentIndex = index
        engine.setSpeechRate(speechRate)
        engine.speak(chunks[index], TextToSpeech.QUEUE_FLUSH, null, "chunk_$index")
    }

    override fun close() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    companion object {
        fun splitIntoSentences(text: String, locale: Locale = Locale.ENGLISH): List<String> {
            val clean = text.trim()
            if (clean.isEmpty()) return emptyList()
            val iterator = BreakIterator.getSentenceInstance(locale)
            iterator.setText(clean)
            val result = mutableListOf<String>()
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val sentence = clean.substring(start, end).trim()
                if (sentence.isNotEmpty()) {
                    result.add(sentence)
                }
                start = end
                end = iterator.next()
            }
            return if (result.isEmpty()) listOf(clean) else result
        }
    }
}
