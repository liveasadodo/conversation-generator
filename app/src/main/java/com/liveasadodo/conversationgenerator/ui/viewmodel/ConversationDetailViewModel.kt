package com.liveasadodo.conversationgenerator.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liveasadodo.conversationgenerator.data.database.ConversationEntity
import com.liveasadodo.conversationgenerator.data.repository.ConversationHistoryRepository
import com.liveasadodo.conversationgenerator.util.ConversationParser
import com.liveasadodo.conversationgenerator.util.ParsedConversation
import kotlinx.coroutines.launch

/**
 * ViewModel for ConversationDetailActivity.
 * Manages conversation data loading and parsing following MVVM pattern.
 */
class ConversationDetailViewModel(
    private val repository: ConversationHistoryRepository
) : ViewModel() {

    private val _conversationState = MutableLiveData<ConversationDetailState>()
    val conversationState: LiveData<ConversationDetailState> = _conversationState

    private val _parsedConversation = MutableLiveData<ParsedConversation?>()
    val parsedConversation: LiveData<ParsedConversation?> = _parsedConversation

    /**
     * Loads a conversation by ID from the repository.
     * Updates the conversationState LiveData with the result.
     */
    fun loadConversation(conversationId: Long) {
        _conversationState.value = ConversationDetailState.Loading

        viewModelScope.launch {
            try {
                val conversation = repository.getConversationById(conversationId)
                if (conversation != null) {
                    // Parse the conversation text
                    val parsed = ConversationParser.parse(conversation.conversationText)
                    _parsedConversation.value = parsed

                    _conversationState.value = ConversationDetailState.Success(conversation)
                } else {
                    _conversationState.value = ConversationDetailState.NotFound
                }
            } catch (e: Exception) {
                _conversationState.value = ConversationDetailState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Gets the formatted text for sharing or copying.
     */
    fun getFormattedText(): String {
        return _parsedConversation.value?.let {
            ConversationParser.formatForCopy(it)
        } ?: ""
    }
}

/**
 * Sealed class representing different states of conversation detail loading.
 */
sealed class ConversationDetailState {
    object Loading : ConversationDetailState()
    data class Success(val conversation: ConversationEntity) : ConversationDetailState()
    object NotFound : ConversationDetailState()
    data class Error(val message: String) : ConversationDetailState()
}
