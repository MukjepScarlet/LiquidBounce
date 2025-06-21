package net.ccbluex.liquidbounce.api.thirdparty.translator

import net.ccbluex.liquidbounce.config.types.Choice

abstract class TranslatorChoice(
    name: String
) : Choice(name), TranslatorApi
