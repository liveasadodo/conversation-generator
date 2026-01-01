package com.example.conversationgenerator.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.conversationgenerator.data.model.Language
import java.util.Locale

/**
 * Manages TextToSpeech functionality for the app
 */
class TTSManager(
    private val context: Context,
    private val onInitialized: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentUtteranceId: String? = null
    private var playAllTexts: List<String> = emptyList()
    private var playAllSpeakers: List<String?> = emptyList()
    private var currentPlayAllIndex: Int = 0
    private var playAllLanguage: Language? = null
    private var onPlayAllComplete: (() -> Unit)? = null
    private var onPlayAllProgress: ((Int, Int) -> Unit)? = null
    private var isPlayingAll = false

    // Speaker voice profiles mapping
    private val speakerVoiceProfiles = mutableMapOf<String, VoiceProfile>()
    private val voicePresets = listOf(
        VoiceProfile(pitch = 0.7f, speechRate = 0.9f),   // Deep, slow
        VoiceProfile(pitch = 1.3f, speechRate = 1.1f),   // High, fast
        VoiceProfile(pitch = 0.85f, speechRate = 1.05f), // Medium-low, slightly fast
        VoiceProfile(pitch = 1.15f, speechRate = 0.95f), // Medium-high, slightly slow
        VoiceProfile(pitch = 1.0f, speechRate = 0.85f),  // Normal pitch, slow
        VoiceProfile(pitch = 1.0f, speechRate = 1.15f)   // Normal pitch, fast
    )

    companion object {
        private const val TAG = "TTSManager"
    }

    /**
     * Voice profile for a speaker
     */
    data class VoiceProfile(
        val pitch: Float,
        val speechRate: Float
    )

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                Log.d(TAG, "TTS initialized successfully")
                setupUtteranceProgressListener()
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
            onInitialized(isInitialized)
        }
    }

    private fun setupUtteranceProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Utterance started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Utterance done: $utteranceId")
                if (isPlayingAll && utteranceId?.startsWith("play_all_") == true) {
                    playNextInQueue()
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Utterance error: $utteranceId")
                if (isPlayingAll) {
                    stopPlayAll()
                }
            }
        })
    }

    /**
     * Get or assign a voice profile for a speaker
     */
    private fun getVoiceProfileForSpeaker(speaker: String): VoiceProfile {
        return speakerVoiceProfiles.getOrPut(speaker) {
            val index = speakerVoiceProfiles.size % voicePresets.size
            val profile = voicePresets[index]
            Log.d(TAG, "Assigned voice profile to $speaker: pitch=${profile.pitch}, rate=${profile.speechRate}")
            profile
        }
    }

    /**
     * Apply voice profile settings to TTS
     */
    private fun applyVoiceProfile(profile: VoiceProfile) {
        tts?.setPitch(profile.pitch)
        tts?.setSpeechRate(profile.speechRate)
    }

    /**
     * Speak the given text in the specified language
     * @param text The text to speak
     * @param language The language for pronunciation
     * @param speaker The speaker name (optional, for voice differentiation)
     * @return True if speech started successfully, false otherwise
     */
    fun speak(text: String, language: Language, speaker: String? = null): Boolean {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized")
            return false
        }

        val locale = language.toLocale()
        val result = tts?.setLanguage(locale)

        return when (result) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.e(TAG, "Language not supported: ${language.displayName}")
                false
            }
            else -> {
                stop() // Stop any currently playing speech

                // Apply speaker-specific voice profile if speaker is provided
                if (speaker != null) {
                    val profile = getVoiceProfileForSpeaker(speaker)
                    applyVoiceProfile(profile)
                }

                val utteranceId = "tts_${System.currentTimeMillis()}"
                currentUtteranceId = utteranceId
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                Log.d(TAG, "Speaking: $text in ${language.displayName}" +
                    if (speaker != null) " by $speaker" else "")
                true
            }
        }
    }

    /**
     * Stop any currently playing speech
     */
    fun stop() {
        if (isInitialized && tts?.isSpeaking == true) {
            tts?.stop()
            Log.d(TAG, "Speech stopped")
        }
        currentUtteranceId = null
    }

    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    /**
     * Check if a specific utterance is currently playing
     */
    fun isSpeaking(utteranceId: String): Boolean {
        return isSpeaking() && currentUtteranceId == utteranceId
    }

    /**
     * Check if the specified language is available
     */
    fun isLanguageAvailable(language: Language): Boolean {
        if (!isInitialized || tts == null) {
            return false
        }

        val locale = language.toLocale()
        val result = tts?.isLanguageAvailable(locale)

        return result == TextToSpeech.LANG_AVAILABLE ||
               result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
               result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    /**
     * Set speech rate (1.0 is normal speed)
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    /**
     * Set pitch (1.0 is normal pitch)
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    /**
     * Play all texts sequentially
     * @param texts List of texts to speak
     * @param language Language for pronunciation
     * @param speakers List of speaker names corresponding to each text (optional)
     * @param onComplete Callback when all texts have been spoken
     * @param onProgress Callback for progress updates (current index, total count)
     * @return True if play all started successfully, false otherwise
     */
    fun playAll(
        texts: List<String>,
        language: Language,
        speakers: List<String?>? = null,
        onComplete: (() -> Unit)? = null,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Boolean {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized")
            return false
        }

        if (texts.isEmpty()) {
            Log.w(TAG, "No texts to play")
            return false
        }

        // Check if language is available
        val locale = language.toLocale()
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Language not supported: ${language.displayName}")
            return false
        }

        // Stop any currently playing speech
        stop()

        // Set up play all state
        playAllTexts = texts
        playAllSpeakers = speakers ?: List(texts.size) { null }
        playAllLanguage = language
        currentPlayAllIndex = 0
        onPlayAllComplete = onComplete
        onPlayAllProgress = onProgress
        isPlayingAll = true

        // Start playing first text
        playNextInQueue()

        Log.d(TAG, "Started playing all: ${texts.size} texts in ${language.displayName}")
        return true
    }

    private fun playNextInQueue() {
        if (!isPlayingAll || currentPlayAllIndex >= playAllTexts.size) {
            // All done
            stopPlayAll()
            return
        }

        val text = playAllTexts[currentPlayAllIndex]
        val speaker = playAllSpeakers.getOrNull(currentPlayAllIndex)
        val utteranceId = "play_all_$currentPlayAllIndex"
        currentUtteranceId = utteranceId

        // Apply speaker-specific voice profile if speaker is provided
        if (speaker != null) {
            val profile = getVoiceProfileForSpeaker(speaker)
            applyVoiceProfile(profile)
        }

        // Report progress
        onPlayAllProgress?.invoke(currentPlayAllIndex, playAllTexts.size)

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        Log.d(TAG, "Playing ${currentPlayAllIndex + 1}/${playAllTexts.size}: $text" +
            if (speaker != null) " by $speaker" else "")

        currentPlayAllIndex++
    }

    /**
     * Stop play all and reset state
     */
    fun stopPlayAll() {
        if (isPlayingAll) {
            stop()
            isPlayingAll = false
            onPlayAllComplete?.invoke()
            playAllTexts = emptyList()
            playAllSpeakers = emptyList()
            playAllLanguage = null
            currentPlayAllIndex = 0
            onPlayAllComplete = null
            onPlayAllProgress = null
            Log.d(TAG, "Stopped play all")
        }
    }

    /**
     * Check if play all is currently active
     */
    fun isPlayingAll(): Boolean {
        return isPlayingAll
    }

    /**
     * Release TTS resources
     * Must be called when done using TTS
     */
    fun shutdown() {
        stopPlayAll()
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.d(TAG, "TTS shut down")
    }
}
