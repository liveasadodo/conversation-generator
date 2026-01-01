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
import com.example.conversationgenerator.data.repository.ConversationHistoryRepository
import com.example.conversationgenerator.databinding.ActivityConversationDetailBinding
import com.example.conversationgenerator.util.ConversationParser
import kotlinx.coroutines.launch

class ConversationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationDetailBinding
    private var parsedConversation: com.example.conversationgenerator.util.ParsedConversation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}
