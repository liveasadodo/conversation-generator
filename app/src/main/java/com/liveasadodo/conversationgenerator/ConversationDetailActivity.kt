package com.liveasadodo.conversationgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.liveasadodo.conversationgenerator.data.model.Formality
import com.liveasadodo.conversationgenerator.data.model.Language
import com.liveasadodo.conversationgenerator.databinding.ActivityConversationDetailBinding
import com.liveasadodo.conversationgenerator.ui.base.BaseActivity
import com.liveasadodo.conversationgenerator.ui.viewmodel.ConversationDetailViewModel
import com.liveasadodo.conversationgenerator.ui.viewmodel.ConversationDetailViewModelFactory
import com.liveasadodo.conversationgenerator.ui.viewmodel.ConversationDetailState

class ConversationDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityConversationDetailBinding
    private lateinit var viewModel: ConversationDetailViewModel
    private lateinit var ttsController: com.liveasadodo.conversationgenerator.util.TTSController
    private var generationLanguage: Language = Language.ENGLISH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        val factory = ConversationDetailViewModelFactory(historyRepository)
        viewModel = ViewModelProvider(this, factory)[ConversationDetailViewModel::class.java]

        // Initialize TTS Controller
        ttsController = com.liveasadodo.conversationgenerator.util.TTSController(this)

        val conversationId = intent.getLongExtra("CONVERSATION_ID", -1)
        if (conversationId == -1L) {
            finish()
            return
        }

        setupListeners()
        observeViewModel()
        viewModel.loadConversation(conversationId)
    }

    private fun observeViewModel() {
        viewModel.conversationState.observe(this) { state ->
            when (state) {
                is ConversationDetailState.Loading -> {
                    // Could show loading indicator here if needed
                }
                is ConversationDetailState.Success -> {
                    displayConversationDetails(state.conversation)
                }
                is ConversationDetailState.NotFound -> {
                    Toast.makeText(this, "Conversation not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ConversationDetailState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        viewModel.parsedConversation.observe(this) { parsed ->
            parsed?.let {
                displayParsedConversation(it)
            }
        }
    }

    private fun displayConversationDetails(conversation: com.liveasadodo.conversationgenerator.data.database.ConversationEntity) {
        binding.titleTextView.text = conversation.title
        // Get generation language from saved conversation
        generationLanguage = Language.fromDisplayName(conversation.generationLanguage)

        // Display key sentence if present
        if (!conversation.keySentence.isNullOrBlank()) {
            binding.keySentenceLabel.visibility = View.VISIBLE
            binding.keySentenceLabel.text = getString(R.string.label_key_sentence_display, conversation.keySentence)
        } else {
            binding.keySentenceLabel.visibility = View.GONE
        }

        // Display formality
        val formality = Formality.fromName(conversation.formality)
        binding.formalityLabel.text = getString(formality.stringResId)

        // Display conversation length
        binding.conversationLengthLabel.text = getString(
            R.string.conversation_length_format,
            conversation.conversationLength
        )
    }

    private fun displayParsedConversation(parsed: com.liveasadodo.conversationgenerator.util.ParsedConversation) {
        // Display conversation using helper
        com.liveasadodo.conversationgenerator.util.ConversationDisplayHelper.displayConversation(
            context = this,
            container = binding.conversationContainer,
            titleTextView = binding.conversationTitle,
            parsedConversation = parsed,
            onSpeakButtonClick = { button, text, speaker ->
                handleSpeakButtonClick(button, text, speaker)
            }
        )
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
        val textToCopy = viewModel.getFormattedText()

        val clip = ClipData.newPlainText("Conversation", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.message_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareConversation() {
        val textToShare = viewModel.getFormattedText()
        val title = viewModel.parsedConversation.value?.title ?: "Conversation"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textToShare)
            putExtra(Intent.EXTRA_SUBJECT, title)
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
            parsedConversation = viewModel.parsedConversation.value,
            language = generationLanguage,
            onComplete = { }
        )
    }

    override fun onDestroy() {
        ttsController.shutdown()
        super.onDestroy()
    }
}
