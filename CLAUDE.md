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

### Error Handling
- Use sealed class `ApiResult<T>` pattern (Success, Error, NetworkError, Loading)
- Implement retry logic with exponential backoff (max 3 attempts)
- Handle all HTTP error codes: 400, 401, 429, 500
- User-friendly error messages (no technical details)

### Rate Limiting
- Free tier: 15 RPM, 1M TPM, 1,500 RPD
- Implement 4-second minimum interval between requests

## Code Patterns

### Architecture
- MVVM pattern: Activity → ViewModel → Repository → ApiService
- Coroutines with proper dispatchers (IO for network, Main for UI)
- LiveData for state management

### Input Validation
- Max lengths: Situation (500 chars), Key Sentence (200 chars)
- Validate before API calls
- Trim and sanitize user input

### Security Checklist
- [ ] No hardcoded API keys
- [ ] HTTPS enforced
- [ ] Input validation implemented
- [ ] Error messages don't expose sensitive info
- [ ] ProGuard configured for release builds

## References
- [Google AI Studio](https://aistudio.google.com/)
- [Gemini API Documentation](https://ai.google.dev/docs)
