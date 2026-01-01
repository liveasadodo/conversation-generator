package com.example.conversationgenerator.data.repository

import android.util.Log
import com.example.conversationgenerator.BuildConfig
import com.example.conversationgenerator.data.api.GeminiApiService
import com.example.conversationgenerator.data.model.ApiResult
import com.example.conversationgenerator.data.model.Content
import com.example.conversationgenerator.data.model.GeminiApiRequest
import com.example.conversationgenerator.data.model.GenerationConfig
import com.example.conversationgenerator.data.model.Language
import com.example.conversationgenerator.data.model.Part
import com.example.conversationgenerator.util.ModelConfig
import com.example.conversationgenerator.util.PromptTemplates
import com.example.conversationgenerator.util.RateLimiter
import com.example.conversationgenerator.util.RetryUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ConversationRepository(
    private val apiService: GeminiApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val TAG = "ConversationRepository"

    suspend fun generateConversation(
        situation: String,
        keySentence: String? = null,
        generationLanguage: Language = Language.ENGLISH,
        interfaceLanguage: Language? = null,
        formality: com.example.conversationgenerator.data.model.Formality = com.example.conversationgenerator.data.model.Formality.CASUAL,
        difficulty: String = "intermediate",
        length: String = "medium"
    ): ApiResult<String> {
        return withContext(ioDispatcher) {
            try {
                // Apply rate limiting
                RateLimiter.waitIfNeeded()

                // Get model name based on build type
                val modelName = ModelConfig.getModelName(BuildConfig.BUILD_TYPE)

                // Check if translation is needed
                val includeTranslation = interfaceLanguage != null && interfaceLanguage != generationLanguage

                // Generate prompt based on parameters
                val prompt = when {
                    difficulty != "intermediate" -> PromptTemplates.generateWithDifficulty(situation, difficulty)
                    length != "medium" -> PromptTemplates.generateWithLength(situation, length)
                    else -> PromptTemplates.generateConversationPrompt(situation, generationLanguage, interfaceLanguage, keySentence, formality)
                }

                // Adjust max tokens based on whether translation is included
                val maxTokens = if (includeTranslation) 2048 else ModelConfig.MAX_OUTPUT_TOKENS

                // Log request in debug mode
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Request - Model: $modelName, Language: ${generationLanguage.displayName}, " +
                            "Interface: ${interfaceLanguage?.displayName ?: "None"}, Situation: $situation")
                }

                // Make API call with retry logic
                val response = RetryUtil.callWithRetry {
                    apiService.generateContent(
                        model = modelName,
                        request = GeminiApiRequest(
                            contents = listOf(
                                Content(
                                    parts = listOf(Part(prompt))
                                )
                            ),
                            generationConfig = GenerationConfig(
                                temperature = ModelConfig.TEMPERATURE,
                                maxOutputTokens = maxTokens,
                                topP = ModelConfig.TOP_P,
                                topK = ModelConfig.TOP_K
                            )
                        )
                    )
                }

                response.fold(
                    onSuccess = { apiResponse ->
                        // Log response in debug mode
                        if (BuildConfig.DEBUG) {
                            val tokens = apiResponse.usageMetadata?.totalTokenCount ?: 0
                            Log.d(TAG, "Response - Total tokens: $tokens")
                        }

                        val text = apiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (text != null) {
                            ApiResult.Success(text)
                        } else {
                            Log.e(TAG, "Empty response from API")
                            ApiResult.Error(0, "Empty response from API")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error: ${error.message}", error)
                        handleError(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                ApiResult.Error(0, e.message ?: "Unexpected error")
            }
        }
    }

    private fun handleError(error: Throwable): ApiResult<Nothing> {
        return when (error) {
            is HttpException -> {
                // Try to parse error response body for more details
                val errorBody = try {
                    error.response()?.errorBody()?.string()
                } catch (e: Exception) {
                    null
                }

                // Check if error body contains API key related message
                val isApiKeyError = errorBody?.contains("API key", ignoreCase = true) == true ||
                                   errorBody?.contains("API_KEY_INVALID", ignoreCase = true) == true

                when (error.code()) {
                    400 -> {
                        if (isApiKeyError) {
                            ApiResult.Error(400, "Invalid API key. Please check your Google AI Studio API key.")
                        } else {
                            ApiResult.Error(400, "Invalid request. Please try again.")
                        }
                    }
                    401, 403 -> ApiResult.Error(401, "Invalid API key. Please check your Google AI Studio API key.")
                    404 -> ApiResult.Error(404, "Model not found. Please update the app or contact support.")
                    429 -> ApiResult.Error(429, "Rate limit exceeded. Please wait a moment and try again.")
                    500 -> ApiResult.Error(500, "Server error. Please try again later.")
                    else -> ApiResult.Error(error.code(), "Error: ${error.message()}")
                }
            }
            is IOException -> ApiResult.NetworkError
            else -> ApiResult.Error(0, error.message ?: "Unknown error")
        }
    }
}
