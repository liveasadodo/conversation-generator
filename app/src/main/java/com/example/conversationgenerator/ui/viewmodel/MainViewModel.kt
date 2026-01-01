package com.example.conversationgenerator.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conversationgenerator.data.model.ApiResult
import com.example.conversationgenerator.data.model.Language
import com.example.conversationgenerator.data.repository.ConversationRepository
import com.example.conversationgenerator.util.InputValidator
import com.example.conversationgenerator.util.ValidationResult
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ConversationRepository
) : ViewModel() {

    private val _conversationState = MutableLiveData<ApiResult<String>>()
    val conversationState: LiveData<ApiResult<String>> = _conversationState

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    private val _generationLanguage = MutableLiveData<Language>(Language.ENGLISH)
    val generationLanguage: LiveData<Language> = _generationLanguage

    private val _interfaceLanguage = MutableLiveData<Language>(Language.JAPANESE)
    val interfaceLanguage: LiveData<Language> = _interfaceLanguage

    fun setGenerationLanguage(language: Language) {
        _generationLanguage.value = language
    }

    fun setInterfaceLanguage(language: Language) {
        _interfaceLanguage.value = language
    }

    fun generateConversation(
        situation: String,
        difficulty: String = "intermediate",
        length: String = "medium"
    ) {
        // Validate input
        when (val validationResult = InputValidator.validateSituation(situation)) {
            is ValidationResult.Valid -> {
                _validationError.value = null
                _conversationState.value = ApiResult.Loading

                viewModelScope.launch {
                    val result = repository.generateConversation(
                        situation = validationResult.input,
                        generationLanguage = _generationLanguage.value ?: Language.ENGLISH,
                        interfaceLanguage = _interfaceLanguage.value,
                        difficulty = difficulty,
                        length = length
                    )
                    _conversationState.value = result
                }
            }
            is ValidationResult.Error -> {
                _validationError.value = validationResult.message
                _conversationState.value = ApiResult.Error(0, validationResult.message)
            }
        }
    }

    fun clearConversation() {
        _conversationState.value = ApiResult.Loading
        _validationError.value = null
    }

    fun clearValidationError() {
        _validationError.value = null
    }
}
