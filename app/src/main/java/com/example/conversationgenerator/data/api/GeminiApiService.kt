package com.example.conversationgenerator.data.api

import com.example.conversationgenerator.data.model.GeminiApiRequest
import com.example.conversationgenerator.data.model.GeminiApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GeminiApiRequest
    ): GeminiApiResponse
}
