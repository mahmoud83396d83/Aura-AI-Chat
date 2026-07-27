package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Gemini Models & API ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @field:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @field:Json(name = "role") val role: String? = null,
    @field:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @field:Json(name = "contents") val contents: List<GeminiContent>,
    @field:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @field:Json(name = "content") val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @field:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- OpenRouter Models & API ---

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @field:Json(name = "role") val role: String, // "user", "assistant", "system"
    @field:Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    @field:Json(name = "model") val model: String,
    @field:Json(name = "messages") val messages: List<OpenRouterMessage>,
    @field:Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessageResponse(
    @field:Json(name = "role") val role: String? = null,
    @field:Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @field:Json(name = "message") val message: OpenRouterMessageResponse? = null,
    @field:Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenDetails(
    @field:Json(name = "reasoning_tokens") val reasoningTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterUsage(
    @field:Json(name = "prompt_tokens") val promptTokens: Int? = null,
    @field:Json(name = "completion_tokens") val completionTokens: Int? = null,
    @field:Json(name = "total_tokens") val totalTokens: Int? = null,
    @field:Json(name = "completion_tokens_details") val completionTokensDetails: TokenDetails? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    @field:Json(name = "choices") val choices: List<OpenRouterChoice>? = null,
    @field:Json(name = "usage") val usage: OpenRouterUsage? = null,
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "model") val model: String? = null
)

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "http://localhost",
        @Header("X-Title") title: String = "SmartAgent",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

// --- Service Client Instantiator ---

object AiRetrofitClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    val openRouterApi: OpenRouterApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterApi::class.java)
    }
}
