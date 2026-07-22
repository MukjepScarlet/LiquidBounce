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

import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanSlotContent
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.ArmorItemSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.findNonEmptySlotsInInventory
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.buildEnumMap

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */
object ModuleInventoryCleaner : ClientModule(
    "InventoryCleaner", ModuleCategories.PLAYER,
    aliases = listOf("InventoryManager")
) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    @Suppress("unused")
    private val inventoryPresets by inventoryPreset()

    private val itemsBlackList by items("ItemsBlacklist", itemSortedSetOf())

    val cleanupTemplateFromSettings: CleanupPlanTemplate
        get() {
            val specifiedSlotTargets = this.inventoryPresets.items
            val currentRestrictionMap = hashMapOf<ItemSlot, RestrictionType>()

            val slotTargets = buildEnumMap<HotbarItemSlot, _> {
                for ((slot, choice) in specifiedSlotTargets) {
                    val wishes = choice.mapNotNull {
                        val representation = it.toBackendRepresentation()

                        currentRestrictionMap.compute(slot) { _, b ->
                            maxOf(b ?: RestrictionType.NONE, representation.slotRestriction)
                        }

                        representation.contentPreference
                    }

                    this[slot] = CleanupPlanSlotContent(wishes, 0)
                }
            }

            // Disallow tampering with armor slots since auto armor already handles them
            ArmorItemSlot.entries.forEach { currentRestrictionMap[it] = RestrictionType.FORBID_TAMPERING }

            if (ModuleOffhand.isOperating() || !HotbarItemSlot.OFFHAND.canBeSwapTarget) {
                currentRestrictionMap[HotbarItemSlot.OFFHAND] = RestrictionType.FORBID_TAMPERING
            }

            val desiredItemCounts = this.inventoryPresets.itemLimitRules.map { rule ->
                val converted = buildList {
                    for (item in rule.items) {
                        val preference = item.toBackendRepresentation().contentPreference ?: continue
                        preference.subtypes.mapTo(this) { ItemCategory(preference.itemType, it) }
                    }
                }

                CategoriesAmount(converted, rule.itemCount)
            }

            val constraintProvider = AmountItemAmountConstraintProvider(
                desiredItemsInSpecificCategories = desiredItemCounts
            )

            return CleanupPlanTemplate(
                slotTargets,
                itemAmountConstraintProvider = constraintProvider,
                itemBlacklist = itemsBlackList,
                restrictions = CleanupPlanRestrictions(currentRestrictionMap)
            )
        }

    @Suppress("unused")
    private val handleInventorySchedule = handler<ScheduleInventoryActionEvent> { event ->
        val currentInventorySlots = findNonEmptySlotsInInventory()
        val cleanupPlan = CleanupPlanGenerator(cleanupTemplateFromSettings, currentInventorySlots).plan

        val hotbarSwap = cleanupPlan.swaps.firstOrNull()
        if (hotbarSwap != null) {
            val target = hotbarSwap.to as? HotbarItemSlot
                ?: error("Invalid swap target: ${hotbarSwap.to}. Only hotbar slots are supported.")

            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performSwap(null, hotbarSwap.from, target)
            )
            return@handler
        }

        val slotToMerge = cleanupPlan.findSlotsToMerge().firstOrNull()
        if (slotToMerge != null) {
            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performMergeStack(slot = slotToMerge),
            )
            return@handler
        }

        val itemToThrow = cleanupPlan.findItemsToThrowOut(currentInventorySlots).firstOrNull()
            ?: currentInventorySlots.firstOrNull { it.itemStack.item in itemsBlackList }
            ?: return@handler

        event.schedule(
            inventoryConstraints,
            InventoryAction.Click.performThrow(screen = null, itemToThrow),
            Priority.NOT_IMPORTANT
        )
    }

    internal data class CategoriesAmount(val categories: List<ItemCategory>, val desiredAmount: Int)

    internal class AmountItemAmountConstraintProvider(
        /**
         * Contains information about specific item groups constraints like `[snowball, egg] -> 32`.
         * In that example, the inventory cleaner would not start throwing out items until at least 32 items of
         * snowballs or eggs are in the inventory.
         */
        desiredItemsInSpecificCategories: List<CategoriesAmount>
    ) : ItemAmountConstraintProvider {
        /**
         * Contains all specific item groups in which an item is.
         *
         * For these rules: `[egg, snowball] -> 32, [egg, carrot] -> 64`, this list would look like this:
         * - `egg` -> `[0, 1]`
         * - `snowball` -> `[0]`
         * - `carrot` -> `[1]`
         */
        private val itemSpecificGroupMap = buildMap {
            desiredItemsInSpecificCategories.forEachIndexed { idx, (categories, desiredAmount) ->
                val group = SpecificItemGroup(id = idx, desiredAmount = desiredAmount, priority = idx)

                for (category in categories) {
                    getOrPut(category, ::ArrayList).add(group)
                }
            }
        }

        override fun getConstraints(facet: ItemFacet): ArrayList<ItemConstraintInfo> {
            val constraints = ArrayList<ItemConstraintInfo>()

            for (group in this.itemSpecificGroupMap.getOrDefault(facet.category, emptyList())) {
                val info = ItemConstraintInfo(
                    group = SpecificItemGroupConstraintGroup(
                        acceptableRange = group.desiredAmount..Int.MAX_VALUE,
                        priority = group.priority,
                        groupId = group.id
                    ),
                    amountAddedByItem = facet.itemStack.count,
                    default = false
                )

                constraints.add(info)
            }

            for ((function, amountAdded) in facet.providedItemFunctions) {
                val info = ItemConstraintInfo(
                    group = ItemFunctionCategoryConstraintGroup(
                        1..Int.MAX_VALUE,
                        1000,
                        function
                    ),
                    amountAddedByItem = amountAdded,
                    default = true
                )

                constraints.add(info)
            }

            if (facet.providedItemFunctions.isEmpty() && facet.category.type != ItemType.EXACT_ITEM) {
                val defaultDesiredAmount = if (facet.category.type.oneIsSufficient) 1 else Int.MAX_VALUE

                val info = ItemConstraintInfo(
                    group = ItemCategoryConstraintGroup(
                        defaultDesiredAmount..Int.MAX_VALUE,
                        1000,
                        facet.category
                    ),
                    amountAddedByItem = facet.itemStack.count,
                    default = true
                )

                constraints.add(info)
            }

            return constraints
        }

        override fun getAllocationPriority(itemGroup: ItemCategory): Int {
            return -this.itemSpecificGroupMap.getOrDefault(itemGroup, emptyList()).size
        }

        private class SpecificItemGroup(val id: Int, val desiredAmount: Int, val priority: Int)
    }
}
