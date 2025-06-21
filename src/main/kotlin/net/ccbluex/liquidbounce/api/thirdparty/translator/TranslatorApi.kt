package net.ccbluex.liquidbounce.api.thirdparty.translator

import net.ccbluex.liquidbounce.features.command.commands.translate.CommandAutoTranslate

interface TranslatorApi {
    suspend fun translate(
        sourceLanguage: TranslateLanguage = TranslateLanguage.Auto,
        targetLanguage: TranslateLanguage = TranslateLanguage.of(CommandAutoTranslate.languageCode),
        text: String
    ): TranslationResult {
        require(text.isNotBlank()) { "Text cannot be blank." }
l
        return translateInternal(
            sourceLanguage, targetLanguage, text
        )
    }

    suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult
}
