/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.minecraft.world.item.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals

class CleanupPlanGeneratorTest {

    @Test
    fun `items marked to keep are added to useful items`() {
        val alreadyUsefulSlot = virtualSlot(0)
        val itemToKeep = virtualSlot(1)
        val dispensableItem = virtualSlot(2)
        val usefulItems = hashSetOf<ItemSlot>(alreadyUsefulSlot)

        usefulItems.addItemsMarkedToKeep(
            listOf(
                facet(itemToKeep, shouldKeep = true),
                facet(dispensableItem, shouldKeep = false),
            )
        )

        assertEquals(setOf<ItemSlot>(alreadyUsefulSlot, itemToKeep), usefulItems)
    }

    private fun virtualSlot(id: Int) = VirtualItemSlot(ItemStack.EMPTY, ItemSlot.Type.INVENTORY, id)

    private fun facet(slot: ItemSlot, shouldKeep: Boolean) = object : ItemFacet(slot) {
        override fun shouldKeep() = shouldKeep
    }
}
