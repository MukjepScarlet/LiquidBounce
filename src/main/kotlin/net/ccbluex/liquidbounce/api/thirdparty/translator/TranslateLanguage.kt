package net.ccbluex.liquidbounce.api.thirdparty.translator

sealed class TranslateLanguage {
    object Auto : TranslateLanguage()

    class Of private constructor(val language: String) : TranslateLanguage() {
        companion object {
            fun create(language: String): TranslateLanguage {
                require(language.lowercase() != "auto") { "'auto' is not allowed as a custom language" }
                require(language.isNotBlank()) { "Language cannot be blank" }
                return Of(language)
            }
        }
    }

    companion object {
        fun of(language: String): TranslateLanguage {
            return if (language.lowercase() == "auto") Auto
            else Of.create(language)
        }
    }
}
