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
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertTrue

class CleanupPlanBlacklistTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
            Items.SNOWBALL.builtInRegistryHolder().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS)
        }
    }

    @Test
    fun `blacklisted items are excluded from every planning stage`() {
        val targetSlot = virtualSlot(ItemStack.EMPTY, ItemSlot.Type.HOTBAR, 0)
        val blacklistedSlots = listOf(
            virtualSlot(ItemStack(Items.SNOWBALL, 32), ItemSlot.Type.CONTAINER, 1),
            virtualSlot(ItemStack(Items.SNOWBALL, 16), ItemSlot.Type.INVENTORY, 2),
        )
        val template = CleanupPlanTemplate(
            slotContentMap = mapOf(
                targetSlot to CleanupPlanSlotContent(
                    slotContentPreferences = listOf(
                        SlotContentPreference(ItemType.EXACT_ITEM, setOf(Items.SNOWBALL))
                    ),
                    priority = 0,
                )
            ),
            itemAmountConstraintProvider = NoConstraints,
            itemBlacklist = setOf(Items.SNOWBALL),
            restrictions = CleanupPlanRestrictions(emptyMap()),
        )

        val plan = CleanupPlanGenerator(template, blacklistedSlots).plan

        assertTrue(plan.usefulItems.none { it in blacklistedSlots })
        assertTrue(plan.swaps.none { it.from in blacklistedSlots || it.to in blacklistedSlots })
        assertTrue(plan.mergeableItems.values.flatten().none { it in blacklistedSlots })
    }

    private fun virtualSlot(stack: ItemStack, type: ItemSlot.Type, id: Int) = VirtualItemSlot(stack, type, id)

    private object NoConstraints : ItemAmountConstraintProvider {
        override fun getConstraints(item: ItemFacet) = arrayListOf<ItemConstraintInfo>()

        override fun getAllocationPriority(itemGroup: ItemCategory) = 0
    }
}
