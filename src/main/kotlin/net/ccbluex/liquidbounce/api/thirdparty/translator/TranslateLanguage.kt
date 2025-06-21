package net.ccbluex.liquidbounce.api.thirdparty.translator

sealed interface TranslateLanguage {
    val literal: String

    object Auto : TranslateLanguage {
        override val literal = "auto"
    }

    class Literal internal constructor(override val literal: String) : TranslateLanguage

    companion object {
        fun of(language: String): TranslateLanguage {
            return when (language.lowercase()) {
                "auto" -> Auto
                else -> Literal(language)
            }
        }
    }
}

fun String.asLanguage() = TranslateLanguage.of(this)
