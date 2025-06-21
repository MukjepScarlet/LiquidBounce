package net.ccbluex.liquidbounce.api.thirdparty.translator

import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.MutableText

sealed class TranslationResult(
    val isValid: Boolean
) {
    abstract fun toResultText(): MutableText

    data class Success(
        val origin: String,
        val translation: String,
        val fromLanguage: TranslateLanguage,
        val toLanguage: TranslateLanguage
    ) : TranslationResult(
        origin != translation && fromLanguage != toLanguage
    ) {
        override fun toResultText(): MutableText = "".asText()
            .append(regular("("))
            .append(variable(fromLanguage.asString()))
            .append(regular("->"))
            .append(variable(toLanguage.asString()))
            .append(regular(") "))
            .append(regular(translation).copyable(copyContent = translation))
    }

    data class Failure(
        val ex: Exception,
    ) : TranslationResult(false) {
        override fun toResultText(): MutableText = "".asText()
            .append(markAsError("Failed to translate: "))
            .append(markAsError(ex.message!!))
    }
}
