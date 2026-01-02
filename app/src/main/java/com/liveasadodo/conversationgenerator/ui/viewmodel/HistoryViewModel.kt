package com.liveasadodo.conversationgenerator.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.liveasadodo.conversationgenerator.data.database.ConversationEntity
import com.liveasadodo.conversationgenerator.data.repository.ConversationHistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: ConversationHistoryRepository
) : ViewModel() {

    private val _showFavoritesOnly = MutableLiveData(false)
    val showFavoritesOnly: LiveData<Boolean> = _showFavoritesOnly

    val conversations: LiveData<List<ConversationEntity>> = _showFavoritesOnly.switchMap { showFavorites ->
        if (showFavorites) {
            repository.favoriteConversations
        } else {
            repository.allConversations
        }
    }

    fun toggleFavorite(conversation: ConversationEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(conversation)
        }
    }

    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            repository.deleteConversation(conversation)
        }
    }

    fun toggleFilter() {
        _showFavoritesOnly.value = !(_showFavoritesOnly.value ?: false)
    }
}
