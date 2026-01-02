# Project Coding Standards

## Development Workflow

**IMPORTANT: Always update SPECIFICATION.md before implementing features.**

Workflow:
```
User Request → Review/Update SPECIFICATION.md → Implement Code → Test
```

## API Integration Standards

### API Key Security
- Store API keys in SharedPreferences (never hardcode)
- Exclude from version control (.gitignore)
- Pass as query parameter: `?key=YOUR_API_KEY`

### Gemini API Configuration
- Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- Model: `gemini-2.5-flash` (production)
- Request/response format: See SPECIFICATION.md
- All API responses must be in JSON format
- Use ConversationParser utility for parsing responses
- Response structure: title, titleTranslation, lines array with speaker data

### Error Handling
- Use sealed class `ApiResult<T>` pattern (Success, Error, NetworkError, Loading)
- Implement retry logic with exponential backoff (max 3 attempts)
- Handle all HTTP error codes: 400, 401, 429, 500
- User-friendly error messages (no technical details)
- Retry delays: 2s, 4s, 8s (exponential backoff)

### Rate Limiting
- Free tier: 15 RPM, 1M TPM, 1,500 RPD
- Implement 4-second minimum interval between requests
- Use RateLimiter utility to enforce rate limits

## Code Patterns

### Architecture
- MVVM pattern: Activity → ViewModel → Repository → ApiService
- Coroutines with proper dispatchers (IO for network, Main for UI)
- LiveData for state management

### Data Models
- Language enum: displayName, code, flag (emoji)
- Formality enum: uses stringResId for localization (not hardcoded display names)
- ConversationEntity: Room database entity with all conversation metadata

### Input Validation
- Max lengths: Situation (500 chars), Key Sentence (200 chars)
- Validate before API calls
- Trim and sanitize user input
- Use InputValidator utility

### Text-to-Speech
- Use TTSManager singleton for TTS operations
- Speaker-specific voice profiles (pitch: 0.7-1.3, speechRate: 0.85-1.15)
- Play all feature with sequential playback
- Automatic speaker voice differentiation
- Supports all 8 generation languages

### Utility Classes
- TTSManager: Text-to-speech with speaker voice profiles
- RateLimiter: API request throttling
- RetryUtil: Exponential backoff retry logic
- ConversationParser: JSON response parsing
- PromptTemplates: API prompt generation
- InputValidator: User input validation
- ModelConfig: API configuration constants

### Security Checklist
- [ ] No hardcoded API keys
- [ ] HTTPS enforced
- [ ] Input validation implemented
- [ ] Error messages don't expose sensitive info
- [ ] ProGuard for release builds (currently disabled - minifyEnabled false)

## Build Configuration
- Min SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)
- JDK: 17
- Gradle: 8.7+
- AGP: 8.7.3
- Room Database: 2.6.1
- Retrofit: 2.9.0
- Kotlin Coroutines: 1.7.3

## References
- [Google AI Studio](https://aistudio.google.com/)
- [Gemini API Documentation](https://ai.google.dev/docs)
