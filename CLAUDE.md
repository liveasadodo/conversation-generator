# Google AI Studio (Gemini) API Coding Standards

This document outlines the coding standards and best practices for integrating Google AI Studio API (Gemini) in this project.

## API Configuration

### API Key Management

**DO:**
```kotlin
// Store in SharedPreferences
val sharedPreferences = getSharedPreferences("api_keys", Context.MODE_PRIVATE)
sharedPreferences.edit()
    .putString("gemini_api_key", "your-api-key-here")
    .apply()

// Retrieve securely
private fun getApiKey(): String? {
    return sharedPreferences.getString("gemini_api_key", null)
}
```

**DON'T:**
```kotlin
// Never hardcode API keys
const val API_KEY = "AIza..." // ❌ NEVER DO THIS

// Never commit to version control
// api_keys.xml should be in .gitignore
```

### API Service Setup

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    fun create(apiKey: String): GeminiApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val url = originalRequest.url.newBuilder()
                    .addQueryParameter("key", apiKey)
                    .build()

                val request = originalRequest.newBuilder()
                    .url(url)
                    .addHeader("content-type", "application/json")
                    .build()

                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

// GeminiApiService.kt
interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GeminiApiRequest
    ): GeminiApiResponse
}
```

## Prompt Engineering Standards

### Template Structure

```kotlin
object PromptTemplates {
    fun generateConversationPrompt(situation: String): String {
        return """
            Please generate a natural English conversation suitable for the following situation.

            Requirements:
            - Conversation should consist of 2-3 exchanges
            - Use practical and natural expressions
            - Clearly distinguish each speaker's dialogue
            - Include appropriate greetings and closing

            Situation: $situation

            Format:
            **Title**

            Speaker A: ...
            Speaker B: ...
        """.trimIndent()
    }

    fun generateWithDifficulty(situation: String, difficulty: String): String {
        val vocabularyLevel = when (difficulty) {
            "beginner" -> "Use simple, everyday vocabulary (A1-A2 level)"
            "intermediate" -> "Use common vocabulary and some idiomatic expressions (B1-B2 level)"
            "advanced" -> "Use sophisticated vocabulary and natural idioms (C1-C2 level)"
            else -> "Use natural, practical expressions"
        }

        return """
            Please generate a natural English conversation suitable for the following situation.

            Requirements:
            - Conversation should consist of 2-3 exchanges
            - $vocabularyLevel
            - Clearly distinguish each speaker's dialogue
            - Include appropriate greetings and closing

            Situation: $situation
        """.trimIndent()
    }
}
```

### Best Practices

1. **Be Specific**: Clearly state the format and requirements
2. **Use Examples**: Include format examples when needed
3. **Set Constraints**: Specify conversation length, difficulty level
4. **Consistent Format**: Use consistent prompt structure across the app

## Error Handling Standards

### Retry Logic

```kotlin
suspend fun <T> callWithRetry(
    maxRetries: Int = 3,
    initialDelay: Long = 2000L,
    maxDelay: Long = 10000L,
    factor: Double = 2.0,
    block: suspend () -> T
): Result<T> {
    var currentDelay = initialDelay

    repeat(maxRetries) { attempt ->
        try {
            return Result.success(block())
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) {
                return Result.failure(e)
            }

            // Log retry attempt
            Log.w("RetryUtil", "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)

            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }

    return Result.failure(Exception("Max retries exceeded"))
}
```

### Error Response Handling

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

suspend fun generateConversation(situation: String): ApiResult<String> {
    return try {
        val response = callWithRetry {
            apiService.generateContent(
                model = "gemini-1.5-flash",
                request = GeminiApiRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(PromptTemplates.generateConversationPrompt(situation))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.7,
                        maxOutputTokens = 1024,
                        topP = 0.95,
                        topK = 40
                    )
                )
            )
        }

        response.fold(
            onSuccess = {
                val text = it.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    ApiResult.Success(text)
                } else {
                    ApiResult.Error(0, "Empty response from API")
                }
            },
            onFailure = {
                when (it) {
                    is HttpException -> {
                        when (it.code()) {
                            400 -> ApiResult.Error(400, "Invalid request")
                            401, 403 -> ApiResult.Error(401, "Invalid API key")
                            429 -> ApiResult.Error(429, "Rate limit exceeded")
                            500 -> ApiResult.Error(500, "Server error")
                            else -> ApiResult.Error(it.code(), "Unknown error")
                        }
                    }
                    is IOException -> ApiResult.NetworkError
                    else -> ApiResult.Error(0, it.message ?: "Unknown error")
                }
            }
        )
    } catch (e: Exception) {
        ApiResult.Error(0, e.message ?: "Unexpected error")
    }
}
```

## Input Validation Standards

### Sanitization

```kotlin
object InputValidator {
    private const val MAX_LENGTH = 500
    private const val MIN_LENGTH = 3

    fun validateSituation(input: String): ValidationResult {
        val trimmed = input.trim()

        return when {
            trimmed.isEmpty() -> ValidationResult.Error("Please enter a situation")
            trimmed.length < MIN_LENGTH -> ValidationResult.Error("Situation too short")
            trimmed.length > MAX_LENGTH -> ValidationResult.Error("Situation too long (max $MAX_LENGTH characters)")
            containsInappropriateContent(trimmed) -> ValidationResult.Error("Inappropriate content detected")
            else -> ValidationResult.Valid(trimmed)
        }
    }

    private fun containsInappropriateContent(input: String): Boolean {
        // Implement content filtering logic
        // This is a placeholder - implement actual filtering
        return false
    }
}

sealed class ValidationResult {
    data class Valid(val input: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

## Coroutines Standards

### ViewModel Pattern

```kotlin
class MainViewModel(
    private val repository: ConversationRepository
) : ViewModel() {

    private val _conversationState = MutableLiveData<ApiResult<String>>()
    val conversationState: LiveData<ApiResult<String>> = _conversationState

    fun generateConversation(situation: String) {
        viewModelScope.launch {
            _conversationState.value = ApiResult.Loading

            when (val validationResult = InputValidator.validateSituation(situation)) {
                is ValidationResult.Valid -> {
                    val result = repository.generateConversation(validationResult.input)
                    _conversationState.value = result
                }
                is ValidationResult.Error -> {
                    _conversationState.value = ApiResult.Error(0, validationResult.message)
                }
            }
        }
    }
}
```

### Dispatcher Usage

```kotlin
// Repository pattern with proper dispatchers
class ConversationRepository(
    private val apiService: GeminiApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun generateConversation(situation: String): ApiResult<String> {
        return withContext(ioDispatcher) {
            // API call on IO dispatcher
            generateConversation(situation)
        }
    }
}
```

## Testing Standards

### Unit Test Example

```kotlin
@Test
fun `generateConversation with valid input returns success`() = runTest {
    // Given
    val situation = "Ordering at a restaurant"
    val expectedResponse = GeminiApiResponse(
        candidates = listOf(
            Candidate(
                content = ResponseContent(
                    parts = listOf(Part("**At a Restaurant**\n\n...")),
                    role = "model"
                ),
                finishReason = "STOP",
                index = 0
            )
        ),
        usageMetadata = null
    )

    coEvery { apiService.generateContent(any(), any()) } returns expectedResponse

    // When
    val result = repository.generateConversation(situation)

    // Then
    assertTrue(result is ApiResult.Success)
    assertNotNull((result as ApiResult.Success).data)
}

@Test
fun `generateConversation with network error returns NetworkError`() = runTest {
    // Given
    val situation = "Asking for directions"

    coEvery { apiService.generateContent(any(), any()) } throws IOException()

    // When
    val result = repository.generateConversation(situation)

    // Then
    assertTrue(result is ApiResult.NetworkError)
}
```

## Model Configuration

### Production vs Development

```kotlin
object ModelConfig {
    fun getModelName(buildType: String): String {
        return when (buildType) {
            "debug" -> "gemini-1.5-flash"  // Fast and free for development
            "release" -> "gemini-1.5-flash"  // Fast and free for production
            // Alternative: "gemini-1.5-pro" for higher quality if needed
            else -> "gemini-1.5-flash"
        }
    }

    const val MAX_OUTPUT_TOKENS = 1024
    const val TEMPERATURE = 0.7
    const val TOP_P = 0.95
    const val TOP_K = 40
}

// Usage in Repository
val modelName = ModelConfig.getModelName(BuildConfig.BUILD_TYPE)
```

## Generation Configuration Best Practices

### Temperature Settings
- **0.0-0.3**: More deterministic, formal conversations
- **0.4-0.7**: Balanced creativity and consistency (recommended)
- **0.8-1.0**: More creative and varied responses

### Token Limits
- Set `maxOutputTokens` based on expected conversation length
- Typical conversation (2-3 exchanges): 512-1024 tokens
- Longer conversations: 1024-2048 tokens

## Safety Settings (Optional)

```kotlin
data class SafetySettings(
    val category: String,
    val threshold: String
)

// Add to GeminiApiRequest if needed
val safetySettings = listOf(
    SafetySettings("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE"),
    SafetySettings("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE"),
    SafetySettings("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE"),
    SafetySettings("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE")
)
```

## Logging Standards

```kotlin
object ApiLogger {
    private const val TAG = "GeminiAPI"

    fun logRequest(situation: String, model: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Request - Model: $model, Situation: $situation")
        }
    }

    fun logResponse(response: GeminiApiResponse) {
        if (BuildConfig.DEBUG) {
            val tokens = response.usageMetadata?.totalTokenCount ?: 0
            Log.d(TAG, "Response - Total tokens: $tokens")
        }
    }

    fun logError(error: Throwable) {
        Log.e(TAG, "Error: ${error.message}", error)
    }
}
```

## Rate Limiting

### Free Tier Limits
- 15 requests per minute (RPM)
- 1 million tokens per minute (TPM)
- 1,500 requests per day (RPD)

### Implementation Strategy
```kotlin
object RateLimiter {
    private var lastRequestTime = 0L
    private const val MIN_REQUEST_INTERVAL = 4000L // 4 seconds = ~15 RPM

    suspend fun waitIfNeeded() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime

        if (elapsed < MIN_REQUEST_INTERVAL) {
            delay(MIN_REQUEST_INTERVAL - elapsed)
        }

        lastRequestTime = System.currentTimeMillis()
    }
}

// Usage in Repository
suspend fun generateConversation(situation: String): ApiResult<String> {
    RateLimiter.waitIfNeeded()
    // ... make API call
}
```

## Security Checklist

- [ ] API keys stored in SharedPreferences, never hardcoded
- [ ] API keys excluded from version control (.gitignore)
- [ ] HTTPS enforced for all network communication
- [ ] Input validation implemented
- [ ] Content filtering for inappropriate input
- [ ] Error messages don't expose sensitive information
- [ ] ProGuard rules configured for release builds
- [ ] Rate limiting implemented for free tier

## Code Review Checklist

- [ ] Prompts are clear and specific
- [ ] Error handling covers all API error codes
- [ ] Retry logic implemented with exponential backoff
- [ ] Input validation performed before API calls
- [ ] Coroutines use appropriate dispatchers
- [ ] Loading states handled in UI
- [ ] Unit tests cover success and error cases
- [ ] Logging appropriate for debug/release builds
- [ ] No API keys in code or version control
- [ ] Rate limiting respects free tier limits

## References

- [Google AI Studio](https://aistudio.google.com/)
- [Gemini API Documentation](https://ai.google.dev/docs)
- [Gemini API Quickstart](https://ai.google.dev/tutorials/rest_quickstart)
- [API Reference](https://ai.google.dev/api/rest)
- [Rate Limits](https://ai.google.dev/docs/rate_limits)
