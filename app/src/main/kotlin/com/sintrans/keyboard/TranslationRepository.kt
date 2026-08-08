package com.sintrans.keyboard

interface TranslationRepository {
    /**
     * Translates a string asynchronously to the specified target language.
     * @param text The source text to translate.
     * @param targetLang The ISO language code of the destination language (e.g., "en", "ta").
     * @return The translated text.
     */
    suspend fun translate(text: String, targetLang: String): String
}
