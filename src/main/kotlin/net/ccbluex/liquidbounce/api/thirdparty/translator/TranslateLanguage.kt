package net.ccbluex.liquidbounce.api.thirdparty.translator

sealed class TranslateLanguage {
    object Auto : TranslateLanguage()

    class Literal internal constructor(val language: String) : TranslateLanguage()

    companion object {
        fun of(language: String): TranslateLanguage {
            return when (language.lowercase()) {
                "auto" -> Auto
                else -> Literal(language)
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            is Auto -> "auto"
            is Literal -> this.language
        }
    }
}

fun String.asLanguage() = TranslateLanguage.of(this)
