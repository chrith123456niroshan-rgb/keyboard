package com.sintrans.keyboard

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class MyMemoryTranslationRepository : TranslationRepository {
    private val client = OkHttpClient()

    override suspend fun translate(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext ""

        val encodedText = Uri.encode(trimmed)
        // MyMemory Translation API Sinhalese (si) to the selected target language
        val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=si|$targetLang"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SinTransKeyboard/1.0")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error code: ${response.code}")
                }
                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                val jsonObject = JSONObject(responseBody)
                
                val responseStatus = jsonObject.optInt("responseStatus", 200)
                if (responseStatus == 200 || responseStatus == 304) {
                    val responseData = jsonObject.getJSONObject("responseData")
                    return@withContext responseData.getString("translatedText")
                } else {
                    throw IOException("API Error Status: $responseStatus")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
