package com.example.conversationgenerator.data.repository

import android.util.Log
import com.example.conversationgenerator.BuildConfig
import com.example.conversationgenerator.data.api.GeminiApiService
import com.example.conversationgenerator.data.model.ApiResult
import com.example.conversationgenerator.data.model.Content
import com.example.conversationgenerator.data.model.GeminiApiRequest
import com.example.conversationgenerator.data.model.GenerationConfig
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
        difficulty: String = "intermediate",
        length: String = "medium"
    ): ApiResult<String> {
        return withContext(ioDispatcher) {
            try {
                // Apply rate limiting
                RateLimiter.waitIfNeeded()

                // Get model name based on build type
                val modelName = ModelConfig.getModelName(BuildConfig.BUILD_TYPE)

                // Generate prompt based on parameters
                val prompt = when {
                    difficulty != "intermediate" -> PromptTemplates.generateWithDifficulty(situation, difficulty)
                    length != "medium" -> PromptTemplates.generateWithLength(situation, length)
                    else -> PromptTemplates.generateConversationPrompt(situation)
                }

                // Log request in debug mode
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Request - Model: $modelName, Situation: $situation")
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
                                maxOutputTokens = ModelConfig.MAX_OUTPUT_TOKENS,
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
                when (error.code()) {
                    400 -> ApiResult.Error(400, "Invalid request")
                    401, 403 -> ApiResult.Error(401, "Invalid API key")
                    429 -> ApiResult.Error(429, "Rate limit exceeded")
                    500 -> ApiResult.Error(500, "Server error")
                    else -> ApiResult.Error(error.code(), "Unknown error")
                }
            }
            is IOException -> ApiResult.NetworkError
            else -> ApiResult.Error(0, error.message ?: "Unknown error")
        }
    }
}
