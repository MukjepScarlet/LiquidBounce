package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.api.thirdparty.translator.providers.GoogleTranslateApi
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleTranslation : ClientModule (
    name = "Translation",
    category = Category.CLIENT,
    notActivatable = true,
    hide = true
) {
    val provider = choices("Provider", 0) {
        arrayOf(
            GoogleTranslateApi(it)
        )
    }
}
