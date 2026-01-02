package com.liveasadodo.conversationgenerator.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.liveasadodo.conversationgenerator.R
import com.liveasadodo.conversationgenerator.data.model.Language

/**
 * Helper class for displaying parsed conversations in a consistent way.
 * Eliminates code duplication between MainActivity and ConversationDetailActivity.
 */
object ConversationDisplayHelper {

    /**
     * Displays a parsed conversation in the provided container.
     *
     * @param context The context for inflating views
     * @param container The LinearLayout container to display the conversation in
     * @param titleTextView The TextView to display the conversation title
     * @param parsedConversation The parsed conversation to display
     * @param onSpeakButtonClick Callback for when a speak button is clicked (button, text, speaker)
     */
    fun displayConversation(
        context: Context,
        container: LinearLayout,
        titleTextView: TextView,
        parsedConversation: ParsedConversation,
        onSpeakButtonClick: (ImageButton, String, String) -> Unit
    ) {
        // Display title with translation if available
        val titleText = if (parsedConversation.titleTranslation != null) {
            "${parsedConversation.title}\n(${parsedConversation.titleTranslation})"
        } else {
            parsedConversation.title
        }
        titleTextView.text = titleText
        titleTextView.visibility = if (parsedConversation.title.isNotEmpty()) View.VISIBLE else View.GONE

        // Clear previous content
        container.removeAllViews()

        // Add conversation lines
        val inflater = LayoutInflater.from(context)
        parsedConversation.lines.forEach { line ->
            val lineView = inflater.inflate(R.layout.item_conversation_line, container, false)

            val speakerLabel = lineView.findViewById<TextView>(R.id.speakerLabel)
            val originalText = lineView.findViewById<TextView>(R.id.originalText)
            val translationText = lineView.findViewById<TextView>(R.id.translationText)
            val translationContainer = lineView.findViewById<View>(R.id.translationContainer)
            val singleText = lineView.findViewById<TextView>(R.id.singleText)
            val speakButton = lineView.findViewById<ImageButton>(R.id.speakButton)

            // Display speaker name with translation if available
            val speakerText = if (line.speakerTranslation != null) {
                "${line.speaker} (${line.speakerTranslation})"
            } else {
                line.speaker
            }
            speakerLabel.text = speakerText

            if (line.translationText != null) {
                // Show two-column layout
                translationContainer.visibility = View.VISIBLE
                singleText.visibility = View.GONE
                originalText.text = line.originalText
                translationText.text = line.translationText
            } else {
                // Show single column layout
                translationContainer.visibility = View.GONE
                singleText.visibility = View.VISIBLE
                singleText.text = line.originalText
            }

            // Setup speaker button
            speakButton.setOnClickListener {
                onSpeakButtonClick(speakButton, line.originalText, line.speaker)
            }

            container.addView(lineView)
        }
    }
}
