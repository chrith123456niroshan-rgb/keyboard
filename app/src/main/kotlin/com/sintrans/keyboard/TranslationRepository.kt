package com.sintrans.keyboard

interface TranslationRepository {
    /**
     * Translates a string asynchronously.
     * @param text The source text to translate.
     * @return The translated text.
     */
    suspend fun translate(text: String): String
}
