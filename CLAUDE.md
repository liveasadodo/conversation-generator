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
- Model: `gemini-2.5-flash` (production)
- All API responses MUST be in JSON format
- Use ConversationParser utility for parsing responses
- See SPECIFICATION.md for endpoint details and response structure

### Error Handling
- Use sealed class `ApiResult<T>` pattern (Success, Error, NetworkError, Loading)
- Implement retry logic with exponential backoff (max 3 attempts)
- Handle all HTTP error codes: 400, 401, 429, 500
- User-friendly error messages (NEVER expose technical details or sensitive info)
- Use RetryUtil for consistent retry behavior

### Rate Limiting
- ALWAYS enforce 4-second minimum interval between API requests
- Use RateLimiter utility to prevent rate limit violations

## Code Patterns

### Architecture
- MUST follow MVVM pattern: Activity → ViewModel → Repository → ApiService
- Use Coroutines with proper dispatchers (IO for network, Main for UI)
- Use LiveData for state management
- See SPECIFICATION.md for component details

### Data Models
- NEVER hardcode display names in enums - use string resources for localization
- Use proper Room annotations for database entities
- See SPECIFICATION.md for data model definitions

### Input Validation
- ALWAYS validate user input before API calls
- Enforce max lengths: Situation (500 chars), Key Sentence (200 chars)
- Trim and sanitize all user input
- Use InputValidator utility for consistency

### Text-to-Speech
- Use TTSManager singleton for all TTS operations
- NEVER create multiple TTS instances
- Always provide speaker information for voice differentiation
- Handle TTS initialization failures gracefully

### Utility Usage
- Use existing utility classes instead of reimplementing logic
- See SPECIFICATION.md for utility class details

### Security Checklist
Before submitting code, verify:
- [ ] No hardcoded API keys or sensitive data
- [ ] HTTPS enforced for all network calls
- [ ] Input validation implemented for all user inputs
- [ ] Error messages don't expose technical details or sensitive info
- [ ] No sensitive data logged in production builds

### Code Quality
- Follow Kotlin coding conventions
- Prefer immutability (val over var)
- Keep functions focused and concise
