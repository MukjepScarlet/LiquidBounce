package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslateLanguage
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslatorApi
import net.ccbluex.liquidbounce.api.thirdparty.translator.providers.GoogleTranslateApi
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleTranslation : ClientModule(
    name = "Translation",
    category = Category.CLIENT,
    notActivatable = true,
    hide = true
), TranslatorApi {
    private val providers = choices("Provider", 0) {
        arrayOf(
            GoogleTranslateApi(it)
        )
    }

    override suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult {
        return providers.activeChoice.translateInternal(
            sourceLanguage,
            targetLanguage,
            text
        )
    }
}
