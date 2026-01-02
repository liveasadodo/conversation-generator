package com.liveasadodo.conversationgenerator.data.model

import com.google.gson.annotations.SerializedName

// Request Models
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

// Response Models
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
