package net.ccbluex.liquidbounce.api.thirdparty.translator

import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.MutableText

sealed class TranslationResult {
    data class Success(
        val origin: String,
        val translation: String,
        val fromLanguage: String,
        val toLanguage: String
    ) : TranslationResult() {
        val isValid = origin != translation && fromLanguage != toLanguage

        fun toResultText(): MutableText = "".asText()
            .append(regular("("))
            .append(variable(fromLanguage))
            .append(regular("->"))
            .append(variable(toLanguage))
            .append(regular(") "))
            .append(regular(translation).copyable(copyContent = translation))
    }

    data class Failure(
        val ex: Exception,
    )
}
