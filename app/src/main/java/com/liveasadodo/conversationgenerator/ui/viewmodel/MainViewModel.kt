package com.liveasadodo.conversationgenerator.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liveasadodo.conversationgenerator.data.model.ApiResult
import com.liveasadodo.conversationgenerator.data.model.Formality
import com.liveasadodo.conversationgenerator.data.model.Language
import com.liveasadodo.conversationgenerator.data.repository.ConversationRepository
import com.liveasadodo.conversationgenerator.util.InputValidator
import com.liveasadodo.conversationgenerator.util.ValidationResult
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

    private val _formality = MutableLiveData<Formality>(Formality.CASUAL)
    val formality: LiveData<Formality> = _formality

    private val _conversationLength = MutableLiveData<Int>(3)
    val conversationLength: LiveData<Int> = _conversationLength

    fun setGenerationLanguage(language: Language) {
        _generationLanguage.value = language
    }

    fun setInterfaceLanguage(language: Language) {
        _interfaceLanguage.value = language
    }

    fun setFormality(formality: Formality) {
        _formality.value = formality
    }

    fun setConversationLength(length: Int) {
        _conversationLength.value = length
    }

    fun generateConversation(
        situation: String,
        keySentence: String? = null,
        difficulty: String = "intermediate"
    ) {
        // Validate input
        when (val validationResult = InputValidator.validateSituation(situation)) {
            is ValidationResult.Valid -> {
                _validationError.value = null
                _conversationState.value = ApiResult.Loading

                viewModelScope.launch {
                    val result = repository.generateConversation(
                        situation = validationResult.input,
                        keySentence = keySentence,
                        generationLanguage = _generationLanguage.value ?: Language.ENGLISH,
                        interfaceLanguage = _interfaceLanguage.value,
                        formality = _formality.value ?: Formality.CASUAL,
                        conversationLength = _conversationLength.value ?: 3,
                        difficulty = difficulty
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
