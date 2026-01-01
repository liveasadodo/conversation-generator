# Application Specification

## Overview

Conversation Generator is an Android application that generates multilingual conversation examples based on user-provided situations using Google AI Studio API (Gemini). Users can select the generation language for the conversation and optionally display translations in their preferred interface language.

## Core Features

### 1. Situation Input
- Users can input any situation or scenario in text format
- Input validation (max 500 characters recommended)
- Clear button to reset input field
- Example situations for quick start:
  - Ordering at a restaurant
  - Asking for directions
  - Checking in at a hotel

#### Key Sentence (Optional)
- Users can optionally specify a key sentence that must be included in the generated conversation
- When provided, the AI will generate a conversation that naturally incorporates the specified sentence
- Key sentence validation (max 200 characters recommended)
- Use cases:
  - Practicing specific phrases or expressions
  - Learning particular grammar patterns in context
  - Ensuring conversations cover specific vocabulary
- The key sentence will appear naturally within one of the speaker's dialogues

### 2. Language Selection
- **Generation Language**: Select the language for generating conversations
  - Supported languages: English, Japanese, Spanish, French, German, Chinese, Korean, Hindi
  - Default: English
- **Interface Language**: Select the language for UI and translations
  - Supported languages: English, Japanese
  - Default: Japanese
  - **All UI strings and example situations are displayed in the selected interface language**
- Language settings persist across app sessions

### 3. Conversation Generation
- Generate natural conversations in the selected generation language using Google AI Studio API (Gemini)
- Conversations consist of 2-3 exchanges
- Display generated conversation in readable format
- Optionally display translation in interface language (when different from generation language)
  - **Two-column layout**: Original sentence on the left, translation on the right (when translation is enabled)
  - Each dialogue line is displayed side-by-side for easy comparison
- Loading indicator during generation

### 4. Output Management
- Copy generated conversation to clipboard
- Share conversation via Android share menu
- Display success/error messages appropriately

### 5. Text-to-Speech (Audio Playback)
- **Audio playback for each conversation line** using Android TextToSpeech API
- **Speaker button** displayed next to each dialogue line
- **Automatic language detection**: TTS uses the generation language for pronunciation
- **Playback controls**:
  - Play/Stop toggle on each line
  - Visual feedback during playback (icon change)
  - Automatic stop when switching to another line
- **Multi-language support**: All 8 generation languages supported
  - English, Japanese, Spanish, French, German, Chinese, Korean, Hindi
- **Offline capability**: Works when language data is installed on device
- **Fallback handling**: Graceful error messages if language not available

## Technical Specifications

### API Integration

#### Google AI Studio API (Gemini)
- **Endpoint**: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- **Model**: `gemini-2.5-flash` (recommended for production - fast and cost-effective)
- **Max Tokens**: Configured via `generationConfig.maxOutputTokens`
- **API Key**: Passed as query parameter `?key=YOUR_API_KEY`

#### Request Format

**Generation Only (No Translation)**
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Please generate a natural {generation_language} conversation suitable for the following situation. The conversation should consist of 2-3 exchanges.\n\nSituation: {user_input}\n\nFormat:\n**Title**\n\nSpeaker A: ...\nSpeaker B: ..."
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 1024,
    "topP": 0.95,
    "topK": 40
  }
}
```

**Generation with Key Sentence**
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Please generate a natural {generation_language} conversation suitable for the following situation. The conversation should consist of 2-3 exchanges and MUST naturally include the following key sentence.\n\nSituation: {user_input}\nKey Sentence: {key_sentence}\n\nIMPORTANT: The key sentence must appear naturally within one of the speaker's dialogues. Adjust the conversation flow to make the key sentence fit naturally.\n\nFormat:\n**Title**\n\nSpeaker A: ...\nSpeaker B: ..."
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 1024,
    "topP": 0.95,
    "topK": 40
  }
}
```

**Generation with Translation**
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Please generate a natural {generation_language} conversation suitable for the following situation. The conversation should consist of 2-3 exchanges.\n\nSituation: {user_input}\n\nFormat:\n**Title**\n\nSpeaker A: ...\n({interface_language} translation: ...)\nSpeaker B: ...\n({interface_language} translation: ...)\n\nProvide the conversation in {generation_language} with {interface_language} translations in parentheses after each line."
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 2048,
    "topP": 0.95,
    "topK": 40
  }
}
```

**Generation with Translation and Key Sentence**
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Please generate a natural {generation_language} conversation suitable for the following situation. The conversation should consist of 2-3 exchanges and MUST naturally include the following key sentence.\n\nSituation: {user_input}\nKey Sentence: {key_sentence}\n\nIMPORTANT: The key sentence must appear naturally within one of the speaker's dialogues. Adjust the conversation flow to make the key sentence fit naturally.\n\nFormat:\n**Title**\n\nSpeaker A: ...\n[TRANSLATION]: ...\nSpeaker B: ...\n[TRANSLATION]: ...\n\nProvide the conversation in {generation_language} with {interface_language} translations marked with [TRANSLATION]."
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 2048,
    "topP": 0.95,
    "topK": 40
  }
}
```

#### Response Format
```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "**At a Restaurant - Ordering Food**\n\nWaiter: Good evening! Are you ready to order?\nCustomer: Yes, I'd like the grilled salmon, please.\n\nWaiter: Excellent choice! How would you like that cooked?\nCustomer: Medium, please. And could I have a side salad instead of fries?\n\nWaiter: Of course! Anything to drink?\nCustomer: Just water, thank you."
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 45,
    "candidatesTokenCount": 89,
    "totalTokenCount": 134
  }
}
```

### Architecture

#### MVVM Pattern
- **Model**: Data classes for API requests/responses
- **View**: Activities and XML layouts
- **ViewModel**: Business logic and state management

#### Components
1. **MainActivity**: Main UI screen
2. **MainViewModel**: Handles business logic and API calls
3. **ConversationRepository**: Manages data operations
4. **GeminiApiService**: Retrofit interface for API communication

#### Data Flow
```
User Input → ViewModel → Repository → API Service → Gemini API
                ↓                                        ↓
            LiveData ← Repository ← Response ← Gemini API
                ↓
              View
```

### Data Models

#### ConversationRequest
```kotlin
data class ConversationRequest(
    val situation: String,
    val keySentence: String? = null, // Optional key sentence to include in conversation
    val generationLanguage: String = "English", // English, Japanese, Spanish, French, German, Chinese, Korean, Hindi
    val interfaceLanguage: String = "English", // English, Japanese
    val includeTranslation: Boolean = false, // Auto-enable when generationLanguage != interfaceLanguage
    val difficulty: String = "intermediate", // beginner, intermediate, advanced
    val length: String = "medium" // short (1-2 turns), medium (2-3 turns), long (4-5 turns)
)
```

#### Language
```kotlin
enum class Language(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    JAPANESE("日本語", "ja"),
    SPANISH("Español", "es"),
    FRENCH("Français", "fr"),
    GERMAN("Deutsch", "de"),
    CHINESE("中文", "zh"),
    KOREAN("한국어", "ko"),
    HINDI("हिन्दी", "hi")
}
```

#### GeminiApiRequest
```kotlin
data class GeminiApiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 1024,
    val topP: Double = 0.95,
    val topK: Int = 40
)
```

#### GeminiApiResponse
```kotlin
data class GeminiApiResponse(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata?
)

data class Candidate(
    val content: ResponseContent,
    val finishReason: String,
    val index: Int
)

data class ResponseContent(
    val parts: List<Part>,
    val role: String
)

data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)
```

### Security Requirements

#### API Key Management
- Store API key in SharedPreferences (encrypted)
- Never hardcode API keys in source code
- Exclude API keys from version control
- User must input API key on first launch or in settings

#### Network Security
- Use HTTPS only (enforced by Google AI API)
- Validate SSL certificates
- Consider certificate pinning for production

#### Input Validation
- Maximum input length: 500 characters
- Filter potentially inappropriate content
- Sanitize user input before API calls

### Error Handling

#### Error Types
| Error Code | Description | User Message | Action |
|-----------|-------------|--------------|--------|
| 400 | Invalid request | "Invalid input format" | Show error, allow retry |
| 401 | Authentication error | "API key is invalid" | Prompt to check API key |
| 429 | Rate limit exceeded | "Too many requests. Please wait." | Implement exponential backoff |
| 500 | Server error | "Service unavailable. Please try again." | Retry with backoff |
| Network | No internet connection | "Network error occurred" | Check connection, allow retry |

#### Retry Strategy
- Maximum 3 retry attempts
- Exponential backoff: 2s, 4s, 8s
- User notification on final failure

### Performance Requirements

#### Response Time
- API call timeout: 30 seconds
- UI responsiveness: < 100ms for user interactions
- Loading indicator shown for operations > 500ms

#### Resource Usage
- Memory: Efficient handling of API responses
- Network: Minimize unnecessary API calls
- Battery: Use Coroutines for background operations

### UI/UX Specifications

#### Main Screen Layout
1. **Header**: App title and history button
2. **Language Selection Section**:
   - **Generation Language Spinner**: Dropdown to select conversation language
     - Options: English, Japanese, Spanish, French, German, Chinese, Korean, Hindi
   - **Interface Language Spinner**: Dropdown to select UI/translation language
     - Options: English, Japanese
   - **Translation Toggle**: Checkbox to show/hide translations (auto-enabled when languages differ)
3. **Input Section**:
   - **Situation Input**: Multi-line EditText for situation input
     - Character counter (optional, max 500 chars)
     - Example chips/buttons for quick input
   - **Key Sentence Input** (Optional, collapsible):
     - Single-line or multi-line EditText for key sentence
     - Character counter (optional, max 200 chars)
     - Help text explaining the feature
     - Can be toggled visible/hidden
4. **Action Buttons**:
   - Generate button (primary action)
   - Clear button (secondary action)
5. **Result Section**:
   - ScrollView for generated conversation
   - Each conversation line displays:
     - Speaker label
     - Original text (and translation if enabled) in two-column layout
     - **Speaker button** (icon) for audio playback
   - Copy and Share buttons
6. **Loading State**: Progress indicator overlay

#### Text-to-Speech UI
- **Speaker Icon**: Material icon next to each conversation line
- **State Indicators**:
  - Default: Volume icon (not playing)
  - Playing: Volume up icon or animated indicator
  - Error: Volume off icon (language not available)
- **Button Behavior**:
  - Tap to play the original text in generation language
  - Tap again to stop playback
  - Playing stops automatically when line finishes
  - Only one line plays at a time

#### States
- **Initial**: Empty input, generate button enabled
- **Loading**: Progress indicator, generate button disabled
- **Success**: Display result, show copy/share buttons
- **Error**: Show error message, allow retry

#### Theme
- Material Design 3
- Primary color: Blue (#2196F3)
- Accent color: Green (#4CAF50)
- Support light mode (dark mode optional for future)

### Future Enhancements

#### Phase 3 (Completed)
- **Conversation history storage** (local database using Room)
  - Store last 30 generated conversations
  - Automatically delete oldest entries when limit exceeded
  - Display history in reverse chronological order (newest first)
- **Favorite conversations feature**
  - Mark/unmark conversations as favorites
  - Favorites are preserved even when history limit is exceeded
  - Filter to show only favorites
- **History UI**
  - View saved conversations
  - Delete individual conversations
  - Search/filter functionality

#### Phase 4
- Audio playback of conversations (TTS)
- Offline mode with cached conversations
- User accounts and cloud sync
- Additional interface languages (Spanish, French, Chinese, Korean, etc.)

### Localization

#### Interface Language Support
- All UI strings (labels, buttons, messages) are localized based on the selected interface language
- Example situations are localized based on the selected interface language
- Currently supported interface languages: English, Japanese
- Localized strings are stored in `values/strings.xml` (English) and `values-ja/strings.xml` (Japanese)
- String resources use the standard Android localization mechanism

## Testing Requirements

### Unit Tests
- ViewModel logic
- Repository operations
- Data model validation
- Input sanitization

### Integration Tests
- API communication
- Error handling flows
- Retry mechanism

### UI Tests
- User input scenarios
- Button interactions
- State transitions
- Error message display

## Deployment

### Minimum SDK
- API 24 (Android 7.0)

### Target SDK
- API 34 (Android 14)

### Build Variants
- Debug: Uses gemini-1.5-flash for cost efficiency
- Release: Uses gemini-1.5-flash for production (or gemini-1.5-pro for higher quality)

### ProGuard
- Enable minification for release builds
- Keep Retrofit and Gson classes
- Obfuscate business logic

## Cost Estimation

### Google AI Studio Pricing (Free Tier)
- **Free tier**: 15 requests per minute (RPM), 1 million tokens per minute (TPM), 1,500 requests per day (RPD)
- **Gemini 1.5 Flash**: Free for up to rate limits
- **Gemini 1.5 Pro**: Free for up to rate limits

### Per Conversation
- Average input: 100 tokens
- Average output: 150 tokens
- Cost per generation: **FREE** (within rate limits)

### Monthly Budget (Example)
- Up to 1,500 requests/day = 45,000 requests/month
- Estimated cost: **$0** (free tier)
- Note: For higher usage, consider paid tier or rate limiting strategies
