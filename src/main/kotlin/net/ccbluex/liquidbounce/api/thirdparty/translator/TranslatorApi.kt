package net.ccbluex.liquidbounce.api.thirdparty.translator

import net.ccbluex.liquidbounce.features.command.commands.translate.CommandAutoTranslate

/**
 * Interface for a translation API that translates text from one language to another.
 * The implementation provides two methods: `translate` and `translateInternal`.
 *
 * The `translate` method is the entry point, which is a public method that automatically detects the source language
 * (unless specified) and translates the input text into a target language. The `translateInternal` method is the
 * lower-level internal logic that handles the actual translation work.
 *
 * The API ensures that the provided text is not empty before attempting to perform the translation.
 */
interface TranslatorApi {
    /**
     * Translates the provided text from a source language to a target language.
     * The source language is auto-detected by default, and the target language is defined by the `targetLanguage` parameter.
     *
     * @param sourceLanguage The language of the input text. Defaults to `TranslateLanguage.Auto` (auto-detection).
     * @param targetLanguage The language to which the text should be translated.
     *                       Defaults to a language code from `CommandAutoTranslate.languageCode`.
     * @param text The text to translate.
     *
     * @return A [TranslationResult] object containing the translation result.
     *
     * @throws IllegalArgumentException If the `text` parameter is blank.
     */
    suspend fun translate(
        sourceLanguage: TranslateLanguage = TranslateLanguage.Auto,
        targetLanguage: TranslateLanguage = TranslateLanguage.of(CommandAutoTranslate.languageCode),
        text: String
    ): TranslationResult {
        require(text.isNotBlank()) { "Text cannot be blank." }

        return translateInternal(
            sourceLanguage, targetLanguage, text
        )
    }

    /**
     * Internal method that performs the actual translation from one language to another.
     * This method is used by the `translate` method and is not meant to be called directly by consumers.
     *
     * @param sourceLanguage The source language.
     * @param targetLanguage The target language.
     * @param text The text to translate.
     *
     * @return A [TranslationResult] object containing the translation result.
     */
    suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult
}
