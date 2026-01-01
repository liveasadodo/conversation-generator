package com.example.conversationgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conversationgenerator.data.database.ConversationDatabase
import com.example.conversationgenerator.data.model.Formality
import com.example.conversationgenerator.data.model.Language
import com.example.conversationgenerator.data.repository.ConversationHistoryRepository
import com.example.conversationgenerator.databinding.ActivityConversationDetailBinding
import com.example.conversationgenerator.util.ConversationParser
import kotlinx.coroutines.launch

class ConversationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationDetailBinding
    private lateinit var ttsManager: com.example.conversationgenerator.util.TTSManager
    private var parsedConversation: com.example.conversationgenerator.util.ParsedConversation? = null
    private var generationLanguage: Language = Language.ENGLISH
    private var currentPlayingButton: android.widget.ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TTS
        ttsManager = com.example.conversationgenerator.util.TTSManager(this)

        val conversationId = intent.getLongExtra("CONVERSATION_ID", -1)
        if (conversationId == -1L) {
            finish()
            return
        }

        loadConversation(conversationId)
        setupListeners()
    }

    private fun loadConversation(conversationId: Long) {
        lifecycleScope.launch {
            val database = ConversationDatabase.getDatabase(this@ConversationDetailActivity)
            val repository = ConversationHistoryRepository(database.conversationDao())

            val conversation = repository.getConversationById(conversationId)
            if (conversation != null) {
                binding.titleTextView.text = conversation.title
                // Get generation language from saved conversation
                generationLanguage = Language.fromDisplayName(conversation.generationLanguage)

                // Display key sentence if present
                if (!conversation.keySentence.isNullOrBlank()) {
                    binding.keySentenceLabel.visibility = android.view.View.VISIBLE
                    binding.keySentenceLabel.text = getString(R.string.label_key_sentence_display, conversation.keySentence)
                } else {
                    binding.keySentenceLabel.visibility = android.view.View.GONE
                }

                // Display formality
                val formality = Formality.fromName(conversation.formality)
                val currentLanguage = if (resources.configuration.locales[0].language == "ja") {
                    Language.JAPANESE
                } else {
                    Language.ENGLISH
                }
                binding.formalityLabel.text = formality.getDisplayName(currentLanguage)

                displayConversation(conversation.conversationText)
            } else {
                Toast.makeText(this@ConversationDetailActivity, "Conversation not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayConversation(conversationText: String) {
        // Parse the conversation
        parsedConversation = ConversationParser.parse(conversationText)

        // Display title
        binding.conversationTitle.text = parsedConversation?.title ?: ""

        // Clear previous content
        binding.conversationContainer.removeAllViews()

        // Add conversation lines
        parsedConversation?.lines?.forEach { line ->
            val lineView = layoutInflater.inflate(R.layout.item_conversation_line, binding.conversationContainer, false)

            val speakerLabel = lineView.findViewById<TextView>(R.id.speakerLabel)
            val originalText = lineView.findViewById<TextView>(R.id.originalText)
            val translationText = lineView.findViewById<TextView>(R.id.translationText)
            val translationContainer = lineView.findViewById<android.view.View>(R.id.translationContainer)
            val singleText = lineView.findViewById<TextView>(R.id.singleText)
            val speakButton = lineView.findViewById<android.widget.ImageButton>(R.id.speakButton)

            speakerLabel.text = line.speaker

            if (line.translationText != null) {
                // Show two-column layout
                translationContainer.visibility = android.view.View.VISIBLE
                singleText.visibility = android.view.View.GONE
                originalText.text = line.originalText
                translationText.text = line.translationText
            } else {
                // Show single column layout
                translationContainer.visibility = android.view.View.GONE
                singleText.visibility = android.view.View.VISIBLE
                singleText.text = line.originalText
            }

            // Setup speaker button
            speakButton.setOnClickListener {
                handleSpeakButtonClick(speakButton, line.originalText)
            }

            binding.conversationContainer.addView(lineView)
        }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.copyButton.setOnClickListener {
            copyToClipboard()
        }

        binding.shareButton.setOnClickListener {
            shareConversation()
        }
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val textToCopy = if (parsedConversation != null) {
            ConversationParser.formatForCopy(parsedConversation!!)
        } else {
            ""
        }

        val clip = ClipData.newPlainText("Conversation", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.message_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareConversation() {
        val textToShare = if (parsedConversation != null) {
            ConversationParser.formatForCopy(parsedConversation!!)
        } else {
            ""
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textToShare)
            putExtra(Intent.EXTRA_SUBJECT, parsedConversation?.title ?: "Conversation")
        }
        startActivity(Intent.createChooser(intent, getString(R.string.button_share)))
    }

    private fun handleSpeakButtonClick(button: android.widget.ImageButton, text: String) {
        if (ttsManager.isSpeaking() && currentPlayingButton == button) {
            // Stop if already playing this line
            ttsManager.stop()
            button.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            currentPlayingButton = null
        } else {
            // Stop any currently playing
            if (currentPlayingButton != null) {
                currentPlayingButton?.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            }

            // Start playing new line
            val success = ttsManager.speak(text, generationLanguage)
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
                    this,
                    getString(R.string.error_tts_language_not_available, generationLanguage.displayName),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }
}
