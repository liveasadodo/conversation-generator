package com.liveasadodo.conversationgenerator

import android.os.Bundle
import android.widget.Toast
import com.liveasadodo.conversationgenerator.databinding.ActivitySettingsBinding
import com.liveasadodo.conversationgenerator.ui.base.BaseActivity

/**
 * Settings screen for managing app preferences.
 * - API key management (view, edit, save)
 * - App version information display
 */
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_settings)
        }
    }

    private fun loadCurrentSettings() {
        // Load current API key
        val apiKey = preferencesRepository.getApiKey()
        binding.apiKeyEditText.setText(apiKey ?: "")

        // Load and display app version
        binding.appVersionTextView.text = BuildConfig.VERSION_NAME
    }

    private fun setupListeners() {
        // Save button click listener
        binding.saveButton.setOnClickListener {
            saveApiKey()
        }
    }

    private fun saveApiKey() {
        val apiKey = binding.apiKeyEditText.text.toString().trim()

        // Validate API key
        if (apiKey.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.error_empty_api_key),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Save API key
        preferencesRepository.saveApiKey(apiKey)

        // Show success message
        Toast.makeText(
            this,
            getString(R.string.message_api_key_saved),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
