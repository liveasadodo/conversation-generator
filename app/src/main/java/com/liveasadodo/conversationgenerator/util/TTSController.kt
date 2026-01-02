package com.liveasadodo.conversationgenerator.util

import android.content.Context
import android.widget.ImageButton
import android.widget.Toast
import com.liveasadodo.conversationgenerator.R
import com.liveasadodo.conversationgenerator.data.model.Language

/**
 * Controller class that manages TTS operations for conversation playback.
 * This class encapsulates the common TTS logic used across multiple activities,
 * reducing code duplication and centralizing TTS state management.
 */
class TTSController(private val context: Context) {

    private val ttsManager: TTSManager = TTSManager(context)
    private var currentPlayingButton: ImageButton? = null

    /**
     * Handles the speak button click for a single conversation line.
     * Manages play/pause state and button icons.
     *
     * @param button The ImageButton that was clicked
     * @param text The text to speak
     * @param speaker The speaker identifier for voice selection
     * @param language The language to use for speech
     * @param playAllButton Optional play all button to stop if currently active
     * @return true if speech was started successfully, false otherwise
     */
    fun handleSpeakButtonClick(
        button: ImageButton,
        text: String,
        speaker: String,
        language: Language,
        playAllButton: ImageButton? = null
    ): Boolean {
        // Stop play all if active
        if (ttsManager.isPlayingAll()) {
            ttsManager.stopPlayAll()
            playAllButton?.setImageResource(android.R.drawable.ic_media_play)
        }

        if (ttsManager.isSpeaking() && currentPlayingButton == button) {
            // Stop if already playing this line
            ttsManager.stop()
            button.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            currentPlayingButton = null
            return true
        } else {
            // Stop any currently playing
            currentPlayingButton?.setImageResource(android.R.drawable.ic_lock_silent_mode_off)

            // Start playing new line with speaker-specific voice
            val success = ttsManager.speak(text, language, speaker)
            if (success) {
                button.setImageResource(android.R.drawable.ic_lock_silent_mode)
                currentPlayingButton = button

                // Reset button icon when speech finishes
                button.postDelayed({
                    if (currentPlayingButton == button && !ttsManager.isSpeaking()) {
                        button.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                        currentPlayingButton = null
                    }
                }, 100)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_tts_language_not_available, language.displayName),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return success
        }
    }

    /**
     * Handles the play all button click to play entire conversation.
     * Manages sequential playback of all lines with proper speaker voices.
     *
     * @param playAllButton The play all button
     * @param parsedConversation The parsed conversation containing all lines
     * @param language The language to use for speech
     * @param onComplete Callback to execute when playback completes
     * @return true if playback was started successfully, false otherwise
     */
    fun handlePlayAllButtonClick(
        playAllButton: ImageButton,
        parsedConversation: ParsedConversation?,
        language: Language,
        onComplete: () -> Unit
    ): Boolean {
        if (ttsManager.isPlayingAll()) {
            // Stop play all
            ttsManager.stopPlayAll()
            playAllButton.setImageResource(android.R.drawable.ic_media_play)
            return true
        } else {
            // Stop any individual line playback
            if (currentPlayingButton != null) {
                currentPlayingButton?.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                currentPlayingButton = null
                ttsManager.stop()
            }

            // Get all texts and speakers from parsed conversation
            val texts = parsedConversation?.lines?.map { it.originalText } ?: emptyList()
            val speakers = parsedConversation?.lines?.map { it.speaker } ?: emptyList()

            if (texts.isEmpty()) {
                Toast.makeText(context, R.string.error_no_conversation, Toast.LENGTH_SHORT).show()
                return false
            }

            // Start play all with speaker-specific voices
            val success = ttsManager.playAll(
                texts = texts,
                language = language,
                speakers = speakers,
                onComplete = {
                    onComplete()
                    // Reset play all button icon
                    playAllButton.post {
                        playAllButton.setImageResource(android.R.drawable.ic_media_play)
                    }
                }
            )

            if (success) {
                playAllButton.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_tts_language_not_available, language.displayName),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return success
        }
    }

    /**
     * Stops all TTS playback and resets button states.
     */
    fun stopAll() {
        ttsManager.stop()
        ttsManager.stopPlayAll()
        currentPlayingButton?.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
        currentPlayingButton = null
    }

    /**
     * Checks if TTS is currently speaking.
     */
    fun isSpeaking(): Boolean = ttsManager.isSpeaking()

    /**
     * Checks if play all is currently active.
     */
    fun isPlayingAll(): Boolean = ttsManager.isPlayingAll()

    /**
     * Shuts down the TTS engine. Should be called in onDestroy().
     */
    fun shutdown() {
        ttsManager.shutdown()
    }
}
