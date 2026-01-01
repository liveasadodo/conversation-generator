# Conversation Generator

An Android app that generates conversation examples in various languages and situations using AI to help with language learning.

## Features

- AI-powered conversation generation using Google AI Studio API
- Generate conversations in multiple languages (English, Chinese, Hindi, etc.)
- Customize scenarios and key phrases to include in conversations
- Adjustable conversation length and formality levels
- Text-to-speech playback with native speakers' voices
- Save and manage conversation history
- Mark favorite conversations for quick access

## Tech Stack

- Kotlin
- Android SDK (API 24+)
- Material Design 3
- Retrofit (API communication)
- Coroutines (asynchronous processing)
- ViewModel & LiveData (Architecture Components)
- Room Database (local data persistence)
- Gson (JSON serialization)

## Build Requirements

- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17
- Android SDK 34
- Gradle 8.7+ (AGP 8.7.3)

## Setup

1. Clone or download the project
2. Open the project in Android Studio
3. Run Gradle Sync
4. Get your Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)
5. Build and run on emulator or physical device
6. On first launch, enter your Gemini API key when prompted
   - The API key is securely stored in SharedPreferences
   - You can update it anytime from Settings menu
