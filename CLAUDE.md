# Claude API Integration Guide

This application uses Anthropic's Claude API to generate English conversation examples.

## Prerequisites

### 1. Obtaining an API Key

1. Visit [Anthropic Console](https://console.anthropic.com/)
2. Create an account or log in
3. Create a new API key in the API Keys section
4. Store your API key securely

### 2. Configuring the API Key

How to set up the API key in the app:

```kotlin
// Save to SharedPreferences (recommended)
val sharedPreferences = getSharedPreferences("api_keys", Context.MODE_PRIVATE)
sharedPreferences.edit()
    .putString("claude_api_key", "your-api-key-here")
    .apply()
```

**Warning**: Do not commit API keys to Git. The configuration file is included in `.gitignore`.

## Claude API Specification

### Endpoint

```
POST https://api.anthropic.com/v1/messages
```

### Request Headers

```
x-api-key: YOUR_API_KEY
anthropic-version: 2023-06-01
content-type: application/json
```

### Request Body Example

```json
{
  "model": "claude-3-5-sonnet-20241022",
  "max_tokens": 1024,
  "messages": [
    {
      "role": "user",
      "content": "Please generate a natural English conversation suitable for the following situation. The conversation should consist of 2-3 exchanges.\n\nSituation: Ordering at a restaurant"
    }
  ]
}
```

### Response Example

```json
{
  "id": "msg_01XYZ...",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "**At a Restaurant - Ordering Food**\n\nWaiter: Good evening! Are you ready to order?\nCustomer: Yes, I'd like the grilled salmon, please.\n\nWaiter: Excellent choice! How would you like that cooked?\nCustomer: Medium, please. And could I have a side salad instead of fries?\n\nWaiter: Of course! Anything to drink?\nCustomer: Just water, thank you."
    }
  ],
  "model": "claude-3-5-sonnet-20241022",
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 45,
    "output_tokens": 89
  }
}
```

## Available Models

| Model Name | Description | Recommended Use |
|---------|------|---------|
| claude-3-5-sonnet-20241022 | Latest high-performance model | Production (Recommended) |
| claude-3-5-haiku-20241022 | Fast, cost-effective model | Development & Testing |
| claude-3-opus-20240229 | Highest performance model | Complex conversations |

## Prompt Design Best Practices

### Effective Prompt Example

```
Please generate a natural English conversation suitable for the following situation.

Requirements:
- Conversation should consist of 2-3 exchanges
- Use practical and natural expressions
- Clearly distinguish each speaker's dialogue
- Include appropriate greetings and closing

Situation: {user_input}

Format:
**Title**

Speaker A: ...
Speaker B: ...
```

### Prompt Customization Parameters

```kotlin
data class ConversationRequest(
    val situation: String,
    val difficulty: String = "intermediate", // beginner, intermediate, advanced
    val length: String = "medium", // short (1-2 turns), medium (2-3 turns), long (4-5 turns)
    val includeTranslation: Boolean = false // Include Japanese translation
)
```

## Pricing Information

Claude API pricing is based on the number of tokens used:

### Claude 3.5 Sonnet (Recommended)
- Input: $3.00 / 1M tokens
- Output: $15.00 / 1M tokens

### Claude 3.5 Haiku (Development)
- Input: $0.80 / 1M tokens
- Output: $4.00 / 1M tokens

**Cost Estimate**:
- 1 conversation generation (approx. 100 input + 150 output tokens): $0.00003
- 1000 generations: approx. $0.03

## Error Handling

### Common Error Codes

| Status Code | Description | Solution |
|----------------|------|---------|
| 400 | Invalid request | Check request body |
| 401 | Authentication error | Verify API key |
| 429 | Rate limit exceeded | Increase retry interval |
| 500 | Server error | Retry with exponential backoff |

### Retry Strategy

```kotlin
suspend fun callClaudeApiWithRetry(
    request: ClaudeRequest,
    maxRetries: Int = 3
): Result<ClaudeResponse> {
    repeat(maxRetries) { attempt ->
        try {
            return Result.success(apiService.generateConversation(request))
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            delay(2000L * (attempt + 1)) // Exponential backoff
        }
    }
    throw Exception("Max retries exceeded")
}
```

## Security Best Practices

1. **API Key Protection**
   - Do not hardcode
   - Store encrypted in SharedPreferences
   - Do not include in version control

2. **Communication Encryption**
   - Use HTTPS (Claude API only supports HTTPS)
   - Consider certificate pinning

3. **User Input Validation**
   - Limit input size (e.g., max 500 characters)
   - Filter inappropriate content

## Reference Links

- [Claude API Documentation](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- [Anthropic Console](https://console.anthropic.com/)
- [Claude API Pricing](https://www.anthropic.com/api)
- [Rate Limits](https://docs.anthropic.com/claude/reference/rate-limits)

## Sample Code

For complete implementation examples, refer to:
- `app/src/main/java/com/example/conversationgenerator/api/ClaudeApiService.kt`
- `app/src/main/java/com/example/conversationgenerator/repository/ConversationRepository.kt`
- `app/src/main/java/com/example/conversationgenerator/viewmodel/MainViewModel.kt`
