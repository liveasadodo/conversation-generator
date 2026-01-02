# Application Specification

## Overview

Conversation Generator is an Android app that generates multilingual conversation examples using Google AI Studio API (Gemini). Users can specify situations, select generation/interface languages, and optionally include key sentences.

## Core Features

### 1. Situation Input
- Text input for situation/scenario (max 500 characters)
- Clear button to reset
- Example situations for quick start

### 2. Key Sentence (Optional)
- Users can specify a key sentence to be naturally included in the conversation
- Max 200 characters
- Use cases: Practicing specific phrases, learning grammar patterns in context

### 3. Language Selection
- **Generation Language**: Language for conversation (English, Japanese, Spanish, French, German, Chinese, Korean, Hindi)
- **Interface Language**: UI language and translation (English, Japanese)
- Settings persist across sessions
- Automatic translation when generation ≠ interface language

### 4. Formality Level Selection
- **Formality**: Select the formality level of the conversation
  - **Formal**: Polite, professional language for business or official settings (honorifics, respectful expressions)
  - **Business Casual**: Professional but friendly language for colleagues (balanced politeness)
  - **Casual**: Everyday conversational language between friends (natural, relaxed tone)
  - **Broken/Intimate**: Very casual language for close friends/family (colloquialisms, slang, informal expressions)
- Default: Casual
- Settings persist across sessions

### 5. Conversation Length Selection
- **Length**: Select the number of exchanges (turns) in the conversation
  - Range: 2 to 5 turns
  - Each turn consists of Speaker A and Speaker B dialogue
- Default: 3 turns
- Settings persist across sessions

### 6. Conversation Generation
- Generates conversations with specified number of exchanges using Gemini API
- Two-column layout: Original (left) and Translation (right) when translation enabled
- Loading indicator during generation

### 7. Text-to-Speech
- Android TextToSpeech API for audio playback
- Speaker button next to each dialogue line
- Play/Stop toggle with visual feedback
- Auto-stop when switching lines
- Play all button to play entire conversation sequentially
- Play all stops individual line playback
- Visual feedback showing play/stop state
- Supports all 8 generation languages
- Offline capable (when language data installed)
- **Speaker-specific voices**: Automatically assigns distinct voice characteristics (pitch and speech rate) to each speaker
  - Each unique speaker gets a consistent voice profile throughout the conversation
  - Supports 2+ speakers with automatically differentiated voices
  - Voice profiles use pitch variations (0.7-1.3) and speech rate adjustments (0.85-1.15) for clear auditory distinction

### 8. Conversation History
- Local storage using Room database
- Stores last 30 conversations
- Auto-delete oldest when limit exceeded
- Favorite conversations preserved beyond limit
- Filter by favorites
- Delete individual conversations
- View historical conversation details

### 9. Output Management
- Copy to clipboard
- Share via Android share menu

## Technical Specifications

### API Integration

#### Gemini API
- **Endpoint**: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- **Model**: `gemini-2.5-flash`
- **Authentication**: API key as query parameter

#### Request Formats

**Response Format: JSON**
All LLM responses must be in JSON format for stable parsing. The prompt instructs the model to respond with the following structure:

```json
{
  "title": "Conversation title in generation language",
  "titleTranslation": "Title in interface language (null if no translation)",
  "lines": [
    {
      "speaker": "Speaker name in generation language",
      "speakerTranslation": "Speaker name in interface language (null if no translation)",
      "text": "Dialogue text in generation language",
      "translation": "Dialogue text in interface language (null if no translation)"
    }
  ]
}
```

**Standard Generation:**
```
Prompt: Generate a natural {language} conversation with {length} exchanges for: {situation}
Response format: JSON (see above)
Note: {length} exchanges means {length} pairs of Speaker A and Speaker B dialogue
```

**With Key Sentence:**
```
Additional constraint: Must naturally include the key sentence: {keySentence}
```

**With Translation:**
```
Additional instruction: Provide {interfaceLanguage} translations in the JSON fields
```

**With Formality:**
```
Additional instruction based on formality level:
- Formal: Use formal, polite language with honorifics and respectful expressions
- Business Casual: Use professional but friendly language suitable for colleagues
- Casual: Use everyday conversational language with natural, relaxed tone
- Broken: Use very casual, intimate language with colloquialisms and slang
```

#### Generation Config
```json
{
  "temperature": 0.7,
  "maxOutputTokens": 1024,
  "topP": 0.95,
  "topK": 40
}
```

### Architecture

**MVVM Pattern:**
```
Activity → ViewModel → Repository → ApiService → Gemini API
   ↑                                                   ↓
   └─────────── LiveData ← Repository ← Response ─────┘
```

**Components:**
- MainActivity: Main UI
- ConversationDetailActivity: View saved conversations
- HistoryActivity: Conversation history list
- MainViewModel: Business logic
- ConversationRepository: API operations
- ConversationHistoryRepository: Database operations
- GeminiApiService: Retrofit interface
- TTSManager: Text-to-speech management

**Utility Components:**
- TTSManager: Text-to-speech with speaker voice profiles
- RateLimiter: API request throttling (4-second minimum interval)
- RetryUtil: Exponential backoff retry logic (max 3 attempts, 2s/4s/8s delays)
- ConversationParser: JSON response parsing
- PromptTemplates: API prompt generation
- InputValidator: User input validation
- ModelConfig: API configuration constants (temperature, tokens, top-p, top-k)

### Data Models

**Language Enum:**
```kotlin
enum class Language(val displayName: String, val code: String, val flag: String) {
    ENGLISH("English", "en", "🇺🇸"),
    JAPANESE("日本語", "ja", "🇯🇵"),
    SPANISH("Español", "es", "🇪🇸"),
    FRENCH("Français", "fr", "🇫🇷"),
    GERMAN("Deutsch", "de", "🇩🇪"),
    CHINESE("中文", "zh", "🇨🇳"),
    KOREAN("한국어", "ko", "🇰🇷"),
    HINDI("हिन्दी", "hi", "🇮🇳")
}
```

**Formality Enum:**
```kotlin
enum class Formality(val stringResId: Int) {
    FORMAL(R.string.formality_formal),
    BUSINESS_CASUAL(R.string.formality_business_casual),
    CASUAL(R.string.formality_casual),
    BROKEN(R.string.formality_broken)
}
```
Note: Formality uses string resource IDs for localization instead of hardcoded display names.

**Database Entity:**
```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val situation: String,
    val keySentence: String?,
    val conversationText: String,
    val generationLanguage: String,
    val interfaceLanguage: String?,
    val formality: String = "CASUAL",
    val conversationLength: Int = 3,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
```

### Database

**Room Database:**
- Max conversations: 30
- Auto-delete oldest (except favorites) when limit exceeded
- ConversationDao provides CRUD operations
- Supports filtering by favorites
- Newest conversations first (ordered by timestamp DESC)

### Security

- API keys stored in SharedPreferences (never hardcoded)
- HTTPS enforced
- Input validation (max lengths, sanitization)
- No sensitive data in error messages

### Error Handling

| Error | User Message | Action |
|-------|-------------|--------|
| 400 | Invalid input format | Show error, retry |
| 401 | Invalid API key | Prompt settings |
| 429 | Too many requests | Exponential backoff |
| 500 | Service unavailable | Retry with backoff |
| Network | Network error | Check connection |

**Retry Strategy:**
- Max 3 attempts
- Exponential backoff: 2s, 4s, 8s

### UI/UX

**Main Screen:**
1. Header (title, history button)
2. Language selection (generation, interface)
3. Situation input
4. Key sentence input (optional, collapsible)
5. Formality selection (spinner)
6. Conversation length selection (SeekBar with display, 2-5 turns)
7. Action buttons (Generate, Clear)
8. Result display with speaker buttons and play all button
9. Copy/Share buttons

**History Screen:**
1. Filter toggle (All/Favorites)
2. Conversation list (newest first)
3. Swipe to delete
4. Tap to view details

**Detail Screen:**
1. Conversation title
2. Dialogue lines with speaker buttons and play all button
3. Copy/Share buttons

**Theme:**
- Material Design 3
- Light mode

### Performance

- API timeout: 30 seconds
- UI responsiveness: < 100ms
- Loading indicator for operations > 500ms
- Efficient memory handling
- Coroutines for background operations

### Deployment

- Min SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)
- JDK: 17
- Gradle: 8.7+
- AGP: 8.7.3
- ProGuard: Currently disabled (minifyEnabled false)

### Localization

- Interface languages: English, Japanese
- Localized UI strings via `values/strings.xml` and `values-ja/strings.xml`
- Example situations localized per interface language

## Future Enhancements

- Additional interface languages (Spanish, French, Chinese, Korean)
- Conversation difficulty levels (beginner, intermediate, advanced)
