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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanSlotContent
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.SlotContentPreference
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.minecraft.world.item.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals

class WishOrganizerTest {

    @Test
    fun `wishes are ordered by every priority dimension`() {
        val lowerPriority = virtualSlot(0)
        val fallback = virtualSlot(1)
        val exactItem = virtualSlot(2)
        val specialized = virtualSlot(3)
        val ordinary = virtualSlot(4)

        val organizer = WishOrganizer(
            CleanupPlanTemplate(
                slotContentMap = linkedMapOf(
                    lowerPriority to content(priority = 0, ItemType.EXACT_ITEM),
                    fallback to content(priority = 1, ItemType.FOOD, ItemType.EXACT_ITEM),
                    exactItem to content(priority = 1, ItemType.EXACT_ITEM),
                    ordinary to content(priority = 1, ItemType.FOOD),
                    specialized to content(priority = 1, ItemType.PEARL),
                ),
                itemAmountConstraintProvider = NoConstraints,
                itemBlacklist = emptySet(),
                restrictions = CleanupPlanRestrictions(emptyMap()),
            )
        )

        assertEquals(
            listOf(exactItem, specialized, fallback, ordinary, fallback, lowerPriority),
            organizer.organizedWishes.map { it.targetSlot },
        )
    }

    private fun content(priority: Int, vararg itemTypes: ItemType) = CleanupPlanSlotContent(
        slotContentPreferences = itemTypes.map { SlotContentPreference(it) },
        priority = priority,
    )

    private fun virtualSlot(id: Int) = VirtualItemSlot(ItemStack.EMPTY, ItemSlot.Type.INVENTORY, id)

    private object NoConstraints : ItemAmountConstraintProvider {
        override fun getConstraints(item: ItemFacet) = arrayListOf<ItemConstraintInfo>()

        override fun getAllocationPriority(itemGroup: ItemCategory) = 0
    }
}
