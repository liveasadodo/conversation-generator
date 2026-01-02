package com.liveasadodo.conversationgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.liveasadodo.conversationgenerator.data.api.RetrofitClient
import com.liveasadodo.conversationgenerator.data.model.ApiResult
import com.liveasadodo.conversationgenerator.data.model.Formality
import com.liveasadodo.conversationgenerator.data.model.Language
import com.liveasadodo.conversationgenerator.data.repository.ConversationRepository
import com.liveasadodo.conversationgenerator.databinding.ActivityMainBinding
import com.liveasadodo.conversationgenerator.ui.adapter.LanguageSpinnerAdapter
import com.liveasadodo.conversationgenerator.ui.base.BaseActivity
import com.liveasadodo.conversationgenerator.ui.viewmodel.MainViewModel
import com.liveasadodo.conversationgenerator.ui.viewmodel.MainViewModelFactory
import com.liveasadodo.conversationgenerator.util.ConversationParser
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var ttsController: com.liveasadodo.conversationgenerator.util.TTSController
    private var generatedConversation: String = ""
    private var parsedConversation: com.liveasadodo.conversationgenerator.util.ParsedConversation? = null
    private var currentSituation: String = ""
    private var currentKeySentence: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun getApiKey(): String? {
        return preferencesRepository.getApiKey()
    }

    private fun saveApiKey(apiKey: String) {
        preferencesRepository.saveApiKey(apiKey)
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
        val savedGenerationLanguage = preferencesRepository.getGenerationLanguage()
        val savedInterfaceLanguage = preferencesRepository.getInterfaceLanguage()

        // Setup Generation Language Spinner
        val generationLanguages = Language.getGenerationLanguages()
        val generationAdapter = LanguageSpinnerAdapter(this, generationLanguages)
        binding.generationLanguageSpinner.adapter = generationAdapter

        // Set saved language or default to English
        val savedGenerationIndex = generationLanguages.indexOfFirst { it.code == savedGenerationLanguage.code }
        binding.generationLanguageSpinner.setSelection(if (savedGenerationIndex >= 0) savedGenerationIndex else 0)

        binding.generationLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = generationLanguages[position]
                viewModel.setGenerationLanguage(selectedLanguage)
                // Save preference
                preferencesRepository.saveGenerationLanguage(selectedLanguage)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup Interface Language Spinner
        val interfaceLanguages = Language.getInterfaceLanguages()
        val interfaceAdapter = LanguageSpinnerAdapter(this, interfaceLanguages)
        binding.interfaceLanguageSpinner.adapter = interfaceAdapter

        // Set saved language or default to Japanese
        val savedInterfaceIndex = interfaceLanguages.indexOfFirst { it.code == savedInterfaceLanguage.code }
        binding.interfaceLanguageSpinner.setSelection(if (savedInterfaceIndex >= 0) savedInterfaceIndex else 1)

        binding.interfaceLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = interfaceLanguages[position]
                val currentLanguageCode = preferencesRepository.getInterfaceLanguageCode()

                // Only change locale if it's different from current
                if (selectedLanguage.code != currentLanguageCode) {
                    viewModel.setInterfaceLanguage(selectedLanguage)
                    // Save preference
                    preferencesRepository.saveInterfaceLanguage(selectedLanguage)
                    // Change locale
                    changeLocale(selectedLanguage.code)
                } else {
                    viewModel.setInterfaceLanguage(selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup Formality Spinner
        val formalities = Formality.getAllFormalities()
        val savedFormality = preferencesRepository.getFormality()

        val formalityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            formalities.map { getString(it.stringResId) }
        )
        formalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.formalitySpinner.adapter = formalityAdapter

        // Set saved formality or default to Casual
        val savedFormalityIndex = formalities.indexOfFirst { it.name == savedFormality.name }
        binding.formalitySpinner.setSelection(if (savedFormalityIndex >= 0) savedFormalityIndex else 2) // CASUAL is index 2

        binding.formalitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedFormality = formalities[position]
                viewModel.setFormality(selectedFormality)
                // Save preference
                preferencesRepository.saveFormality(selectedFormality)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupConversationLengthSeekBar() {
        val savedLength = preferencesRepository.getConversationLength()

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
                preferencesRepository.saveConversationLength(length)
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

        // Display conversation using helper
        parsedConversation?.let { parsed ->
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
        preferencesRepository.clearApiKey()

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
            val conversationLength = viewModel.conversationLength.value ?: preferencesRepository.getConversationLength()

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
