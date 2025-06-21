package net.ccbluex.liquidbounce.api.thirdparty.translator

import net.ccbluex.liquidbounce.features.command.commands.translate.CommandAutoTranslate

interface TranslatorApi {
    suspend fun translate(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage = TranslateLanguage.of(CommandAutoTranslate.languageCode),
        text: String
    ): TranslationResult
}
q
