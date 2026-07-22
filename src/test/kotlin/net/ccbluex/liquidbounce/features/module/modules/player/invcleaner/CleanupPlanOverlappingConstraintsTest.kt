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
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.minecraft.core.MappedRegistry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals

class CleanupPlanOverlappingConstraintsTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
            listOf(
                Items.EGG,
                Items.SNOWBALL,
                Items.DIAMOND_HELMET,
                Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS,
                Items.DIAMOND_BOOTS,
            ).forEach {
                it.builtInRegistryHolder().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS)
            }
            @Suppress("UNCHECKED_CAST")
            (BuiltInRegistries.ITEM as MappedRegistry<Item>).apply {
                bindAllTagsToEmpty()
                freeze()
            }
        }
    }

    @Test
    fun `shared category satisfies overlapping constraints without keeping an extra category`() {
        val rules = listOf(
            rule(64, Items.EGG),
            rule(32, Items.EGG, Items.SNOWBALL),
        )

        assertEquals(setOf(Items.EGG), usefulItems(rules))
    }

    @Test
    fun `overlapping constraint allocation does not depend on rule order`() {
        val rules = listOf(
            rule(32, Items.EGG, Items.SNOWBALL),
            rule(64, Items.EGG),
        )

        assertEquals(setOf(Items.EGG), usefulItems(rules))
    }

    @Test
    fun `independent constraints still keep each required category`() {
        val rules = listOf(
            rule(64, Items.EGG),
            rule(32, Items.SNOWBALL),
        )

        assertEquals(setOf(Items.EGG, Items.SNOWBALL), usefulItems(rules))
    }

    private fun usefulItems(rules: List<ModuleInventoryCleaner.CategoriesAmount>): Set<Item> {
        val snowballSlot = virtualSlot(ItemStack(Items.SNOWBALL, 32), 0)
        val eggSlot = virtualSlot(ItemStack(Items.EGG, 64), 1)
        val provider = ModuleInventoryCleaner.AmountItemAmountConstraintProvider(rules)
        val template = CleanupPlanTemplate(
            slotContentMap = emptyMap(),
            itemAmountConstraintProvider = provider,
            itemBlacklist = emptySet(),
            restrictions = CleanupPlanRestrictions(emptyMap()),
        )
        return CleanupPlanGenerator(template, listOf(snowballSlot, eggSlot)).plan.usefulItems
            .mapTo(hashSetOf()) { it.itemStack.item }
    }

    private fun rule(amount: Int, vararg items: Item) = ModuleInventoryCleaner.CategoriesAmount(
        categories = items.map { ItemCategory(ItemType.EXACT_ITEM, it) },
        desiredAmount = amount,
    )

    private fun virtualSlot(stack: ItemStack, id: Int) =
        VirtualItemSlot(stack, ItemSlot.Type.INVENTORY, id)
}
