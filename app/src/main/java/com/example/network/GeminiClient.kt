package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateAnswer(
        prompt: String,
        systemPrompt: String? = null,
        history: List<GeminiContent> = emptyList(),
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing or default placeholder!")
            return@withContext "Hallo! Bitte richte deinen Gemini API-Schlüssel in den AI Studio Secrets ein, damit ich dir antworten kann."
        }

        // Combine history and current prompt
        val contentsList = mutableListOf<GeminiContent>()
        contentsList.addAll(history)
        contentsList.add(GeminiContent(parts = listOf(GeminiPart(text = prompt)), role = "user"))

        val systemInstruction = systemPrompt?.let {
            GeminiSystemInstruction(parts = listOf(GeminiPart(text = it)))
        }

        val geminiRequest = GeminiRequest(
            contents = contentsList,
            systemInstruction = systemInstruction
        )

        val jsonRequest = try {
            requestAdapter.toJson(geminiRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Serialization error", e)
            return@withContext "Error: Failed to serialize request."
        }

        val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "API Error: ${response.code} - $bodyString")
                    return@withContext "Es tut mir leid, Erna hat ein Problem mit der API-Verbindung (Code: ${response.code})."
                }

                if (bodyString == null) {
                    return@withContext "Fehler: Die Antwort war leer."
                }

                val geminiResponse = responseAdapter.fromJson(bodyString)
                val replyText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                return@withContext replyText ?: "Ich konnte keine Antwort generieren."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception calling Gemini API", e)
            return@withContext "Netzwerkfehler: Bitte überprüfe deine Internetverbindung. ${e.localizedMessage}"
        }
    }
}
