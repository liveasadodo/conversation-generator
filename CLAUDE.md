# Claude API Coding Standards

This document outlines the coding standards and best practices for integrating Claude API in this project.

## API Configuration

### API Key Management

**DO:**
```kotlin
// Store in SharedPreferences
val sharedPreferences = getSharedPreferences("api_keys", Context.MODE_PRIVATE)
sharedPreferences.edit()
    .putString("claude_api_key", "your-api-key-here")
    .apply()

// Retrieve securely
private fun getApiKey(): String? {
    return sharedPreferences.getString("claude_api_key", null)
}
```

**DON'T:**
```kotlin
// Never hardcode API keys
const val API_KEY = "sk-ant-..." // ❌ NEVER DO THIS

// Never commit to version control
// api_keys.xml should be in .gitignore
```

### API Service Setup

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "https://api.anthropic.com/"
    private const val ANTHROPIC_VERSION = "2023-06-01"

    fun create(apiKey: String): ClaudeApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
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
            .create(ClaudeApiService::class.java)
    }
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
            apiService.generateConversation(
                ClaudeApiRequest(
                    model = "claude-3-5-sonnet-20241022",
                    max_tokens = 1024,
                    messages = listOf(
                        Message("user", PromptTemplates.generateConversationPrompt(situation))
                    )
                )
            )
        }

        response.fold(
            onSuccess = { ApiResult.Success(it.content.first().text) },
            onFailure = {
                when (it) {
                    is HttpException -> {
                        when (it.code()) {
                            401 -> ApiResult.Error(401, "Invalid API key")
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
    private val apiService: ClaudeApiService,
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
    val expectedResponse = ClaudeApiResponse(/* ... */)

    coEvery { apiService.generateConversation(any()) } returns expectedResponse

    // When
    val result = repository.generateConversation(situation)

    // Then
    assertTrue(result is ApiResult.Success)
    assertEquals(expectedResponse.content.first().text, (result as ApiResult.Success).data)
}

@Test
fun `generateConversation with network error returns NetworkError`() = runTest {
    // Given
    val situation = "Asking for directions"

    coEvery { apiService.generateConversation(any()) } throws IOException()

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
            "debug" -> "claude-3-5-haiku-20241022"  // Faster, cheaper for development
            "release" -> "claude-3-5-sonnet-20241022"  // Higher quality for production
            else -> "claude-3-5-sonnet-20241022"
        }
    }

    const val MAX_TOKENS = 1024
}

// Usage in Repository
val modelName = ModelConfig.getModelName(BuildConfig.BUILD_TYPE)
```

## Logging Standards

```kotlin
object ApiLogger {
    private const val TAG = "ClaudeAPI"

    fun logRequest(situation: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Request - Situation: $situation")
        }
    }

    fun logResponse(response: ClaudeApiResponse) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Response - Tokens: ${response.usage.input_tokens + response.usage.output_tokens}")
        }
    }

    fun logError(error: Throwable) {
        Log.e(TAG, "Error: ${error.message}", error)
    }
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
- [ ] Certificate pinning considered for production

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

## References

- [Claude API Documentation](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- [Anthropic Console](https://console.anthropic.com/)
- [Best Practices for Prompt Engineering](https://docs.anthropic.com/claude/docs/prompt-engineering)
- [Rate Limits](https://docs.anthropic.com/claude/reference/rate-limits)
