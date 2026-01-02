package com.liveasadodo.conversationgenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liveasadodo.conversationgenerator.data.repository.ConversationHistoryRepository

/**
 * Factory for creating ConversationDetailViewModel with required dependencies.
 */
class ConversationDetailViewModelFactory(
    private val repository: ConversationHistoryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationDetailViewModel::class.java)) {
            return ConversationDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
