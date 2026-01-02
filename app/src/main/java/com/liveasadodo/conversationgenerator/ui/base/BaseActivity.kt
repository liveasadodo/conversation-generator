package com.liveasadodo.conversationgenerator.ui.base

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.liveasadodo.conversationgenerator.data.database.ConversationDatabase
import com.liveasadodo.conversationgenerator.data.repository.ConversationHistoryRepository
import com.liveasadodo.conversationgenerator.data.repository.PreferencesRepository
import java.util.Locale

/**
 * Base activity that provides common functionality for all activities.
 * - Manages locale configuration based on user preferences
 * - Provides access to shared repositories (PreferencesRepository, ConversationHistoryRepository)
 * - Ensures consistent initialization across all activities
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var preferencesRepository: PreferencesRepository
    protected lateinit var historyRepository: ConversationHistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved locale before onCreate
        applySavedLocale()

        super.onCreate(savedInstanceState)

        // Initialize shared repositories
        initializeRepositories()
    }

    /**
     * Applies the saved interface language locale.
     * Called before super.onCreate() to ensure UI uses correct language.
     */
    private fun applySavedLocale() {
        val tempPrefsRepository = PreferencesRepository(this)
        val savedInterfaceLanguageCode = tempPrefsRepository.getInterfaceLanguageCode()
        val locale = Locale(savedInterfaceLanguageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)
    }

    /**
     * Initializes shared repositories that are commonly used across activities.
     */
    private fun initializeRepositories() {
        preferencesRepository = PreferencesRepository(this)

        val database = ConversationDatabase.getDatabase(this)
        historyRepository = ConversationHistoryRepository(database.conversationDao())
    }

    /**
     * Changes the application locale and recreates the activity.
     * @param languageCode The language code to switch to (e.g., "en", "ja")
     */
    protected fun changeLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)

        // Recreate activity to apply new locale
        recreate()
    }
}
