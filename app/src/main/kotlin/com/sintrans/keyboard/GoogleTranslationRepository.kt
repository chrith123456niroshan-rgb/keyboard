package com.sintrans.keyboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class GoogleTranslationRepository : TranslationRepository {
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext ""

        val apiKey = BuildConfig.TRANSLATION_API_KEY
        if (apiKey.isEmpty() || apiKey == "default_free_api_key") {
            // Fallback to MyMemory translation API if no Google API key is configured
            return@withContext fallbackTranslate(trimmed)
        }

        val url = "https://translation.googleapis.com/language/translate/v2?key=$apiKey"
        
        // Build Google Translation API POST payload
        val jsonBody = JSONObject().apply {
            put("q", trimmed)
            put("target", "en")
            put("source", "si")
            put("format", "text")
        }

        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Google Translate API HTTP Error: ${response.code}")
                }
                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                val jsonObject = JSONObject(responseBody)
                val data = jsonObject.getJSONObject("data")
                val translations = data.getJSONArray("translations")
                if (translations.length() > 0) {
                    return@withContext translations.getJSONObject(0).getString("translatedText")
                } else {
                    throw IOException("No translations returned from Google API")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If Google API fails (e.g. quota, network), fallback to MyMemory to maintain user experience
            return@withContext fallbackTranslate(trimmed)
        }
    }

    private suspend fun fallbackTranslate(text: String): String {
        val fallbackRepo = MyMemoryTranslationRepository()
        return try {
            fallbackRepo.translate(text)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
