package com.example.conversationgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.conversationgenerator.data.api.RetrofitClient
import com.example.conversationgenerator.data.model.ApiResult
import com.example.conversationgenerator.data.model.Language
import com.example.conversationgenerator.data.repository.ConversationRepository
import com.example.conversationgenerator.databinding.ActivityMainBinding
import com.example.conversationgenerator.ui.viewmodel.MainViewModel
import com.example.conversationgenerator.ui.viewmodel.MainViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var generatedConversation: String = ""

    companion object {
        private const val PREFS_NAME = "api_keys"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val PREFS_LANGUAGE = "language_prefs"
        private const val KEY_GENERATION_LANGUAGE = "generation_language"
        private const val KEY_INTERFACE_LANGUAGE = "interface_language"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Example chips click listeners
        binding.example1Chip.setOnClickListener {
            binding.situationEditText.setText(R.string.example_1)
        }

        binding.example2Chip.setOnClickListener {
            binding.situationEditText.setText(R.string.example_2)
        }

        binding.example3Chip.setOnClickListener {
            binding.situationEditText.setText(R.string.example_3)
        }

        // Generate button
        binding.generateButton.setOnClickListener {
            val situation = binding.situationEditText.text.toString()
            viewModel.generateConversation(situation)
        }

        // Clear button
        binding.clearButton.setOnClickListener {
            binding.situationEditText.text?.clear()
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
    }

    private fun setupLanguageSpinners() {
        // Load saved language preferences
        val languagePrefs = getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val savedGenerationLanguageCode = languagePrefs.getString(KEY_GENERATION_LANGUAGE, Language.ENGLISH.code)
        val savedInterfaceLanguageCode = languagePrefs.getString(KEY_INTERFACE_LANGUAGE, Language.JAPANESE.code)

        // Setup Generation Language Spinner
        val generationLanguages = Language.getGenerationLanguages()
        val generationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            generationLanguages.map { it.displayName }
        )
        generationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
        val interfaceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            interfaceLanguages.map { it.displayName }
        )
        interfaceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.interfaceLanguageSpinner.adapter = interfaceAdapter

        // Set saved language or default to Japanese
        val savedInterfaceIndex = interfaceLanguages.indexOfFirst { it.code == savedInterfaceLanguageCode }
        binding.interfaceLanguageSpinner.setSelection(if (savedInterfaceIndex >= 0) savedInterfaceIndex else 1)

        binding.interfaceLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = interfaceLanguages[position]
                viewModel.setInterfaceLanguage(selectedLanguage)
                // Save preference
                languagePrefs.edit().putString(KEY_INTERFACE_LANGUAGE, selectedLanguage.code).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
        binding.resultTextView.text = conversation
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
        val clip = ClipData.newPlainText("Conversation", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.message_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareConversation(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "English Conversation")
        }
        startActivity(Intent.createChooser(intent, getString(R.string.button_share)))
    }
}
