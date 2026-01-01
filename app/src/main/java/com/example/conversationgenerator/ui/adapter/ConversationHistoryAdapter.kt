package com.example.conversationgenerator.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conversationgenerator.R
import com.example.conversationgenerator.data.database.ConversationEntity
import com.example.conversationgenerator.data.model.Formality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ConversationHistoryAdapter(
    private val onItemClick: (ConversationEntity) -> Unit,
    private val onFavoriteClick: (ConversationEntity) -> Unit,
    private val onDeleteClick: (ConversationEntity) -> Unit
) : ListAdapter<ConversationEntity, ConversationHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = getItem(position)
        holder.bind(conversation)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val situationText: TextView = itemView.findViewById(R.id.situationText)
        private val keySentenceText: TextView = itemView.findViewById(R.id.keySentenceText)
        private val languageInfo: TextView = itemView.findViewById(R.id.languageInfo)
        private val formalityText: TextView = itemView.findViewById(R.id.formalityText)
        private val conversationLengthText: TextView = itemView.findViewById(R.id.conversationLengthText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favoriteButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(conversation: ConversationEntity) {
            titleText.text = conversation.title
            situationText.text = conversation.situation

            // Show key sentence if present
            if (!conversation.keySentence.isNullOrBlank()) {
                keySentenceText.visibility = View.VISIBLE
                keySentenceText.text = itemView.context.getString(R.string.label_key_sentence_display, conversation.keySentence)
            } else {
                keySentenceText.visibility = View.GONE
            }

            // Format language info
            val langInfo = if (conversation.interfaceLanguage != null && conversation.interfaceLanguage != conversation.generationLanguage) {
                "${conversation.generationLanguage} → ${conversation.interfaceLanguage}"
            } else {
                conversation.generationLanguage
            }
            languageInfo.text = langInfo

            // Show formality
            val formality = Formality.fromName(conversation.formality)
            formalityText.text = itemView.context.getString(formality.stringResId)

            // Show conversation length
            conversationLengthText.text = itemView.context.getString(
                R.string.conversation_length_format,
                conversation.conversationLength
            )

            // Format timestamp
            timestampText.text = formatTimestamp(conversation.timestamp)

            // Set favorite icon
            favoriteButton.setImageResource(
                if (conversation.isFavorite) android.R.drawable.star_big_on
                else android.R.drawable.star_big_off
            )

            // Click listeners
            itemView.setOnClickListener { onItemClick(conversation) }
            favoriteButton.setOnClickListener { onFavoriteClick(conversation) }
            deleteButton.setOnClickListener { onDeleteClick(conversation) }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes min ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours hours ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days days ago"
                }
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ConversationEntity>() {
        override fun areItemsTheSame(oldItem: ConversationEntity, newItem: ConversationEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConversationEntity, newItem: ConversationEntity): Boolean {
            return oldItem == newItem
        }
    }
}
