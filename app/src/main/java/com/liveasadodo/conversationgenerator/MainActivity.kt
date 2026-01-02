package com.liveasadodo.conversationgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import java.util.Locale
import kotlinx.coroutines.launch
import com.liveasadodo.conversationgenerator.data.api.RetrofitClient
import com.liveasadodo.conversationgenerator.data.model.ApiResult
import com.liveasadodo.conversationgenerator.data.model.Formality
import com.liveasadodo.conversationgenerator.data.model.Language
import com.liveasadodo.conversationgenerator.data.repository.ConversationRepository
import com.liveasadodo.conversationgenerator.databinding.ActivityMainBinding
import com.liveasadodo.conversationgenerator.ui.adapter.LanguageSpinnerAdapter
import com.liveasadodo.conversationgenerator.ui.viewmodel.MainViewModel
import com.liveasadodo.conversationgenerator.ui.viewmodel.MainViewModelFactory
import com.liveasadodo.conversationgenerator.util.ConversationParser
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var historyRepository: com.liveasadodo.conversationgenerator.data.repository.ConversationHistoryRepository
    private lateinit var ttsController: com.liveasadodo.conversationgenerator.util.TTSController
    private var generatedConversation: String = ""
    private var parsedConversation: com.liveasadodo.conversationgenerator.util.ParsedConversation? = null
    private var currentSituation: String = ""
    private var currentKeySentence: String? = null

    companion object {
        private const val PREFS_NAME = "api_keys"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val PREFS_LANGUAGE = "language_prefs"
        private const val KEY_GENERATION_LANGUAGE = "generation_language"
        private const val KEY_INTERFACE_LANGUAGE = "interface_language"
        private const val KEY_FORMALITY = "formality"
        private const val KEY_CONVERSATION_LENGTH = "conversation_length"
        private const val DEFAULT_CONVERSATION_LENGTH = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved interface language
        applySavedLocale()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize history repository
        val database = com.liveasadodo.conversationgenerator.data.database.ConversationDatabase.getDatabase(this)
        historyRepository = com.liveasadodo.conversationgenerator.data.repository.ConversationHistoryRepository(database.conversationDao())

        // Initialize TTS Controller
        ttsController = com.liveasadodo.conversationgenerator.util.TTSController(this)

        // Check for API key
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            showApiKeyDialog()
        } else {
            initializeViewModel(apiKey)
            setupUI()
            observeViewModel()
        }
    }

    private fun applySavedLocale() {
        val languagePrefs = getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val savedInterfaceLanguageCode = languagePrefs.getString(KEY_INTERFACE_LANGUAGE, Language.JAPANESE.code)
        val locale = Locale(savedInterfaceLanguageCode ?: Language.JAPANESE.code)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)
    }

    private fun changeLocale(language: Language) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)

        // Recreate activity to apply new locale
        recreate()
    }

    private fun getApiKey(): String? {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_GEMINI_API_KEY, null)
    }

    private fun saveApiKey(apiKey: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(KEY_GEMINI_API_KEY, apiKey)
            .apply()
    }

    private fun showApiKeyDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter API key"

        val message = """
            Please enter your Google AI Studio API key.

            To get an API key:
            1. Visit https://aistudio.google.com/
            2. Click "Get API key"
            3. Create a new API key
            4. Copy and paste it here
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("API Key Required")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = input.text.toString().trim()
                if (apiKey.isEmpty()) {
                    Toast.makeText(this, "API key is required", Toast.LENGTH_SHORT).show()
                    finish()
                } else if (apiKey.length < 20) {
                    Toast.makeText(this, "API key seems too short. Please check and try again.", Toast.LENGTH_LONG).show()
                    // Show dialog again
                    showApiKeyDialog()
                } else {
                    saveApiKey(apiKey)
                    initializeViewModel(apiKey)
                    setupUI()
                    observeViewModel()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun initializeViewModel(apiKey: String) {
        val apiService = RetrofitClient.create(apiKey, BuildConfig.DEBUG)
        val repository = ConversationRepository(apiService)
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    private fun setupUI() {
        // Setup language spinners
        setupLanguageSpinners()

        // Setup conversation length SeekBar
        setupConversationLengthSeekBar()

        // Example chips click listeners
        binding.example1Chip.setOnClickListener {
            binding.situationEditText.setText(getString(R.string.example_1))
        }

        binding.example2Chip.setOnClickListener {
            binding.situationEditText.setText(getString(R.string.example_2))
        }

        binding.example3Chip.setOnClickListener {
            binding.situationEditText.setText(getString(R.string.example_3))
        }

        // Generate button
        binding.generateButton.setOnClickListener {
            val situation = binding.situationEditText.text.toString()
            val keySentence = binding.keySentenceEditText.text.toString().trim()
            currentSituation = situation
            currentKeySentence = keySentence.ifBlank { null }
            viewModel.generateConversation(situation, currentKeySentence)
        }

        // History button
        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // Clear button
        binding.clearButton.setOnClickListener {
            binding.situationEditText.text?.clear()
            binding.keySentenceEditText.text?.clear()
            binding.resultCard.visibility = View.GONE
            viewModel.clearConversation()
        }

        // Copy button
        binding.copyButton.setOnClickListener {
            copyToClipboard(generatedConversation)
        }

        // Share button
        binding.shareButton.setOnClickListener {
            shareConversation(generatedConversation)
        }

        // Play all button
        binding.playAllButton.setOnClickListener {
            handlePlayAllButtonClick()
        }
    }

    private fun setupLanguageSpinners() {
        // Load saved language preferences
        val languagePrefs = getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val savedGenerationLanguageCode = languagePrefs.getString(KEY_GENERATION_LANGUAGE, Language.ENGLISH.code)
        val savedInterfaceLanguageCode = languagePrefs.getString(KEY_INTERFACE_LANGUAGE, Language.JAPANESE.code)

        // Setup Generation Language Spinner
        val generationLanguages = Language.getGenerationLanguages()
        val generationAdapter = LanguageSpinnerAdapter(this, generationLanguages)
        binding.generationLanguageSpinner.adapter = generationAdapter

        // Set saved language or default to English
        val savedGenerationIndex = generationLanguages.indexOfFirst { it.code == savedGenerationLanguageCode }
        binding.generationLanguageSpinner.setSelection(if (savedGenerationIndex >= 0) savedGenerationIndex else 0)

        binding.generationLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = generationLanguages[position]
                viewModel.setGenerationLanguage(selectedLanguage)
                // Save preference
                languagePrefs.edit().putString(KEY_GENERATION_LANGUAGE, selectedLanguage.code).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup Interface Language Spinner
        val interfaceLanguages = Language.getInterfaceLanguages()
        val interfaceAdapter = LanguageSpinnerAdapter(this, interfaceLanguages)
        binding.interfaceLanguageSpinner.adapter = interfaceAdapter

        // Set saved language or default to Japanese
        val savedInterfaceIndex = interfaceLanguages.indexOfFirst { it.code == savedInterfaceLanguageCode }
        binding.interfaceLanguageSpinner.setSelection(if (savedInterfaceIndex >= 0) savedInterfaceIndex else 1)

        binding.interfaceLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = interfaceLanguages[position]
                val currentLanguageCode = languagePrefs.getString(KEY_INTERFACE_LANGUAGE, Language.JAPANESE.code)

                // Only change locale if it's different from current
                if (selectedLanguage.code != currentLanguageCode) {
                    viewModel.setInterfaceLanguage(selectedLanguage)
                    // Save preference
                    languagePrefs.edit().putString(KEY_INTERFACE_LANGUAGE, selectedLanguage.code).apply()
                    // Change locale
                    changeLocale(selectedLanguage)
                } else {
                    viewModel.setInterfaceLanguage(selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup Formality Spinner
        val formalities = Formality.getAllFormalities()
        val savedFormalityName = languagePrefs.getString(KEY_FORMALITY, Formality.CASUAL.name)

        val formalityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            formalities.map { getString(it.stringResId) }
        )
        formalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.formalitySpinner.adapter = formalityAdapter

        // Set saved formality or default to Casual
        val savedFormalityIndex = formalities.indexOfFirst { it.name == savedFormalityName }
        binding.formalitySpinner.setSelection(if (savedFormalityIndex >= 0) savedFormalityIndex else 2) // CASUAL is index 2

        binding.formalitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedFormality = formalities[position]
                viewModel.setFormality(selectedFormality)
                // Save preference
                languagePrefs.edit().putString(KEY_FORMALITY, selectedFormality.name).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupConversationLengthSeekBar() {
        val languagePrefs = getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val savedLength = languagePrefs.getInt(KEY_CONVERSATION_LENGTH, DEFAULT_CONVERSATION_LENGTH)

        // SeekBar range is 0-3, representing 2-5 turns
        binding.conversationLengthSeekBar.progress = savedLength - 2
        updateConversationLengthText(savedLength)
        viewModel.setConversationLength(savedLength)

        binding.conversationLengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val length = progress + 2 // Convert 0-3 to 2-5
                updateConversationLengthText(length)
                viewModel.setConversationLength(length)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val length = (seekBar?.progress ?: 0) + 2
                // Save preference
                languagePrefs.edit().putInt(KEY_CONVERSATION_LENGTH, length).apply()
            }
        })
    }

    private fun updateConversationLengthText(length: Int) {
        binding.conversationLengthValue.text = getString(R.string.conversation_length_format, length)
    }

    private fun observeViewModel() {
        viewModel.conversationState.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    showLoading(true)
                    binding.resultCard.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    showLoading(false)
                    generatedConversation = result.data
                    displayResult(result.data)

                    // Save to history
                    saveConversationToHistory(result.data)
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    binding.resultCard.visibility = View.GONE
                    showError(result.message)
                }
                is ApiResult.NetworkError -> {
                    showLoading(false)
                    binding.resultCard.visibility = View.GONE
                    showError(getString(R.string.error_network))
                }
            }
        }

        viewModel.validationError.observe(this) { error ->
            if (error != null) {
                binding.situationInputLayout.error = error
            } else {
                binding.situationInputLayout.error = null
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.generateButton.isEnabled = !loading
    }

    private fun displayResult(conversation: String) {
        binding.resultCard.visibility = View.VISIBLE

        // Parse the conversation
        parsedConversation = ConversationParser.parse(conversation)

        // Display title with translation if available
        val parsed = parsedConversation
        val titleText = if (parsed?.titleTranslation != null) {
            "${parsed.title}\n(${parsed.titleTranslation})"
        } else {
            parsed?.title ?: ""
        }
        binding.conversationTitle.text = titleText
        binding.conversationTitle.visibility = if (parsed?.title?.isNotEmpty() == true) View.VISIBLE else View.GONE

        // Clear previous content
        binding.conversationContainer.removeAllViews()

        // Add conversation lines
        parsedConversation?.lines?.forEach { line ->
            val lineView = layoutInflater.inflate(R.layout.item_conversation_line, binding.conversationContainer, false)

            val speakerLabel = lineView.findViewById<TextView>(R.id.speakerLabel)
            val originalText = lineView.findViewById<TextView>(R.id.originalText)
            val translationText = lineView.findViewById<TextView>(R.id.translationText)
            val translationContainer = lineView.findViewById<View>(R.id.translationContainer)
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
                handleSpeakButtonClick(speakButton, line.originalText, line.speaker)
            }

            binding.conversationContainer.addView(lineView)
        }
    }

    private fun showError(message: String) {
        // If error is related to API key, offer to reset it
        if (message.contains("API key", ignoreCase = true)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("API Key Error")
                .setMessage(message + "\n\nWould you like to enter a new API key?")
                .setPositiveButton("Yes") { _, _ ->
                    resetApiKey()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetApiKey() {
        // Clear existing API key
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(KEY_GEMINI_API_KEY).apply()

        // Show dialog to enter new API key
        showApiKeyDialog()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Use formatted version if parsed conversation is available
        val textToCopy = if (parsedConversation != null) {
            ConversationParser.formatForCopy(parsedConversation!!)
        } else {
            text
        }

        val clip = ClipData.newPlainText("Conversation", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.message_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareConversation(text: String) {
        // Use formatted version if parsed conversation is available
        val textToShare = if (parsedConversation != null) {
            ConversationParser.formatForCopy(parsedConversation!!)
        } else {
            text
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textToShare)
            putExtra(Intent.EXTRA_SUBJECT, parsedConversation?.title ?: "Conversation")
        }
        startActivity(Intent.createChooser(intent, getString(R.string.button_share)))
    }

    private fun saveConversationToHistory(conversationText: String) {
        lifecycleScope.launch {
            val title = parsedConversation?.title ?: "Conversation"
            val genLang = viewModel.generationLanguage.value?.displayName ?: "English"
            val intLang = viewModel.interfaceLanguage.value?.displayName
            val formalityName = viewModel.formality.value?.name ?: Formality.CASUAL.name
            val conversationLength = viewModel.conversationLength.value ?: DEFAULT_CONVERSATION_LENGTH

            historyRepository.saveConversation(
                title = title,
                situation = currentSituation,
                keySentence = currentKeySentence,
                conversationText = conversationText,
                generationLanguage = genLang,
                interfaceLanguage = intLang,
                formality = formalityName,
                conversationLength = conversationLength
            )
        }
    }

    private fun handleSpeakButtonClick(button: android.widget.ImageButton, text: String, speaker: String) {
        val generationLanguage = viewModel.generationLanguage.value ?: Language.ENGLISH
        ttsController.handleSpeakButtonClick(
            button = button,
            text = text,
            speaker = speaker,
            language = generationLanguage,
            playAllButton = binding.playAllButton
        )
    }

    private fun handlePlayAllButtonClick() {
        val generationLanguage = viewModel.generationLanguage.value ?: Language.ENGLISH
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
