package com.liveasadodo.conversationgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.liveasadodo.conversationgenerator.data.database.ConversationDatabase
import com.liveasadodo.conversationgenerator.data.model.Formality
import com.liveasadodo.conversationgenerator.data.model.Language
import com.liveasadodo.conversationgenerator.data.repository.ConversationHistoryRepository
import com.liveasadodo.conversationgenerator.databinding.ActivityConversationDetailBinding
import com.liveasadodo.conversationgenerator.util.ConversationParser
import kotlinx.coroutines.launch

class ConversationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationDetailBinding
    private lateinit var ttsController: com.liveasadodo.conversationgenerator.util.TTSController
    private var parsedConversation: com.liveasadodo.conversationgenerator.util.ParsedConversation? = null
    private var generationLanguage: Language = Language.ENGLISH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TTS Controller
        ttsController = com.liveasadodo.conversationgenerator.util.TTSController(this)

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
                binding.formalityLabel.text = getString(formality.stringResId)

                // Display conversation length
                binding.conversationLengthLabel.text = getString(
                    R.string.conversation_length_format,
                    conversation.conversationLength
                )

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

        // Display title with translation if available
        val parsed = parsedConversation
        val titleText = if (parsed?.titleTranslation != null) {
            "${parsed.title}\n(${parsed.titleTranslation})"
        } else {
            parsed?.title ?: ""
        }
        binding.conversationTitle.text = titleText

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

            // Display speaker name with translation if available
            val speakerText = if (line.speakerTranslation != null) {
                "${line.speaker} (${line.speakerTranslation})"
            } else {
                line.speaker
            }
            speakerLabel.text = speakerText

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
                handleSpeakButtonClick(speakButton, line.originalText, line.speaker)
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

        binding.playAllButton.setOnClickListener {
            handlePlayAllButtonClick()
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

    private fun handleSpeakButtonClick(button: android.widget.ImageButton, text: String, speaker: String) {
        ttsController.handleSpeakButtonClick(
            button = button,
            text = text,
            speaker = speaker,
            language = generationLanguage,
            playAllButton = binding.playAllButton
        )
    }

    private fun handlePlayAllButtonClick() {
        ttsController.handlePlayAllButtonClick(
            playAllButton = binding.playAllButton,
            parsedConversation = parsedConversation,
            language = generationLanguage,
            onComplete = { }
        )
    }

    override fun onDestroy() {
        ttsController.shutdown()
        super.onDestroy()
    }
}
