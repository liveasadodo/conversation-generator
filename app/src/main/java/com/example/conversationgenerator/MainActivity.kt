package com.example.conversationgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.conversationgenerator.data.api.RetrofitClient
import com.example.conversationgenerator.data.model.ApiResult
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
        input.hint = "Enter your Google AI Studio API Key"

        MaterialAlertDialogBuilder(this)
            .setTitle("API Key Required")
            .setMessage("Please enter your Google AI Studio API key to use this app.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = input.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    saveApiKey(apiKey)
                    initializeViewModel(apiKey)
                    setupUI()
                    observeViewModel()
                } else {
                    Toast.makeText(this, "API key is required", Toast.LENGTH_SHORT).show()
                    finish()
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
