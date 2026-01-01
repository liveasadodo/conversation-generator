package com.example.conversationgenerator.util

import android.content.Context
import android.speech.tts.TextToSpeech
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

    companion object {
        private const val TAG = "TTSManager"
    }

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
            onInitialized(isInitialized)
        }
    }

    /**
     * Speak the given text in the specified language
     * @param text The text to speak
     * @param language The language for pronunciation
     * @return True if speech started successfully, false otherwise
     */
    fun speak(text: String, language: Language): Boolean {
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
                val utteranceId = "tts_${System.currentTimeMillis()}"
                currentUtteranceId = utteranceId
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                Log.d(TAG, "Speaking: $text in ${language.displayName}")
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
     * Release TTS resources
     * Must be called when done using TTS
     */
    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.d(TAG, "TTS shut down")
    }
}
