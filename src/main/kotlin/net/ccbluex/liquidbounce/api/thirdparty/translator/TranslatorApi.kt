package net.ccbluex.liquidbounce.api.thirdparty.translator

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.features.command.commands.translate.CommandAutoTranslate

abstract class TranslatorApi(
    name: String
) : Choice(name) {
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

    abstract suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult
}
