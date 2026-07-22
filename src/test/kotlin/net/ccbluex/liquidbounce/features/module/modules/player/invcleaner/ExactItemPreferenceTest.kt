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

import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanSlotContent
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.SlotContentPreference
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ExactItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.FoodItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MiningToolItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PrimitiveItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.SharpAxeFacet
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ExactItemPreferenceTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
            bindComponents(Items.DIAMOND_AXE)
            bindComponents(Items.NETHERITE_AXE)
            bindComponents(Items.APPLE, FoodProperties(4, 2.4f, false))
            bindComponents(Items.BREAD, FoodProperties(5, 6.0f, false))
        }

        private fun bindComponents(item: Item, food: FoodProperties? = null) {
            val components = DataComponentMap.builder()
                .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
                .apply {
                    food?.let { set(DataComponents.FOOD, it) }
                }
                .build()

            item.builtInRegistryHolder().bindComponents(components)
        }
    }

    @Test
    fun `exact axe preference handles dynamic domain facets and rejects other items`() {
        val ordinaryAxe = virtualSlot(ItemStack(Items.DIAMOND_AXE), ItemSlot.Type.HOTBAR, 0)
        val sharpnessAxe = virtualSlot(
            ItemStack(Items.DIAMOND_AXE).apply {
                set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            },
            ItemSlot.Type.INVENTORY,
            1,
        )
        val excludedAxe = virtualSlot(ItemStack(Items.NETHERITE_AXE), ItemSlot.Type.INVENTORY, 2)
        val organizer = organizer(SlotContentPreference(ItemType.EXACT_ITEM, setOf(Items.DIAMOND_AXE)))
        val rack = ItemDispenserRack(
            organizer,
            listOf(
                ExactItemFacet(
                    PrimitiveItemFacet(
                        ordinaryAxe,
                        ItemCategory(ItemType.TOOL, MiningToolItemFacet.MASK_AXE),
                    )
                ),
                ExactItemFacet(SharpAxeFacet(sharpnessAxe)),
                ExactItemFacet(SharpAxeFacet(excludedAxe)),
            )
        )
        val wishId = organizer.organizedWishes.single().id

        assertSame(sharpnessAxe, rack.nextItemForGroup(wishId)?.itemSlot)
        assertSame(ordinaryAxe, rack.nextItemForGroup(wishId)?.itemSlot)
        assertNull(rack.nextItemForGroup(wishId))
    }

    @Test
    fun `exact food preference retains food quality comparator`() {
        val worseApple = foodSlot(
            item = Items.APPLE,
            nutrition = 2,
            saturation = 1.0f,
            type = ItemSlot.Type.HOTBAR,
            id = 0,
        )
        val betterApple = foodSlot(
            item = Items.APPLE,
            nutrition = 6,
            saturation = 6.0f,
            type = ItemSlot.Type.INVENTORY,
            id = 1,
        )
        val excludedBread = foodSlot(
            item = Items.BREAD,
            nutrition = 20,
            saturation = 40.0f,
            type = ItemSlot.Type.INVENTORY,
            id = 2,
        )
        val organizer = organizer(SlotContentPreference(ItemType.EXACT_ITEM, setOf(Items.APPLE)))
        val rack = ItemDispenserRack(
            organizer,
            listOf(
                ExactItemFacet(FoodItemFacet(worseApple)),
                ExactItemFacet(FoodItemFacet(betterApple)),
                ExactItemFacet(FoodItemFacet(excludedBread)),
            )
        )
        val wishId = organizer.organizedWishes.single().id

        assertSame(betterApple, rack.nextItemForGroup(wishId)?.itemSlot)
        assertSame(worseApple, rack.nextItemForGroup(wishId)?.itemSlot)
        assertNull(rack.nextItemForGroup(wishId))
    }

    @Test
    fun `single item conversion keeps exact identity`() {
        val preference = FrontendSlotPreference.SingleSlotPreference(Items.APPLE)
            .toBackendRepresentation()
            .contentPreference!!

        assertEquals(ItemType.EXACT_ITEM, preference.itemType)
        assertEquals(setOf(Items.APPLE), preference.subtypes)
    }

    private fun organizer(preference: SlotContentPreference): WishOrganizer {
        val targetSlot = virtualSlot(ItemStack.EMPTY, ItemSlot.Type.HOTBAR, 8)
        val template = CleanupPlanTemplate(
            slotContentMap = mapOf(targetSlot to CleanupPlanSlotContent(listOf(preference), priority = 0)),
            itemAmountConstraintProvider = NoConstraints,
            itemBlacklist = emptySet(),
            restrictions = CleanupPlanRestrictions(emptyMap()),
        )
        return WishOrganizer(template)
    }

    private fun foodSlot(
        item: Item,
        nutrition: Int,
        saturation: Float,
        type: ItemSlot.Type,
        id: Int,
    ): ItemSlot {
        val stack = ItemStack(item)
        stack.set(DataComponents.FOOD, FoodProperties(nutrition, saturation, false))
        return virtualSlot(stack, type, id)
    }

    private fun virtualSlot(stack: ItemStack, type: ItemSlot.Type, id: Int) = VirtualItemSlot(stack, type, id)

    private object NoConstraints : ItemAmountConstraintProvider {
        override fun getConstraints(item: ItemFacet) = arrayListOf<ItemConstraintInfo>()

        override fun getAllocationPriority(itemGroup: ItemCategory) = 0
    }
}
