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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemNumberConstraintEnforcer.SatisfactionStatus
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.minecraft.world.item.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemNumberConstraintEnforcerTest {

    @Test
    fun `same slot only counts once for the same constraint group`() {
        val slot = virtualSlot(0)
        val blockFacet = ItemFacet(slot)
        val stoneFacet = ItemFacet(slot)
        val blocksAndStone = constraintGroup(id = 0, desiredAmount = 128)
        val enforcer = enforcer(
            blockFacet to constraint(blocksAndStone, amount = 64),
            stoneFacet to constraint(blocksAndStone, amount = 64),
        )

        enforcer.addItem(blockFacet)
        enforcer.addItem(stoneFacet)

        assertEquals(SatisfactionStatus.NOT_SATISFIED, enforcer.getSatisfactionStatus(blockFacet))
    }

    @Test
    fun `same slot counts once for each distinct constraint group`() {
        val slot = virtualSlot(0)
        val firstFacet = ItemFacet(slot)
        val secondFacet = ItemFacet(slot)
        val firstGroup = constraintGroup(id = 0, desiredAmount = 64)
        val secondGroup = constraintGroup(id = 1, desiredAmount = 64)
        val enforcer = enforcer(
            firstFacet to constraint(firstGroup, amount = 64),
            secondFacet to constraint(secondGroup, amount = 64),
        )

        enforcer.addItem(firstFacet)
        enforcer.addItem(secondFacet)

        assertEquals(SatisfactionStatus.SATISFIED, enforcer.getSatisfactionStatus(firstFacet))
        assertEquals(SatisfactionStatus.SATISFIED, enforcer.getSatisfactionStatus(secondFacet))
    }

    @Test
    fun `different slots count separately for the same constraint group`() {
        val firstFacet = ItemFacet(virtualSlot(0))
        val secondFacet = ItemFacet(virtualSlot(1))
        val group = constraintGroup(id = 0, desiredAmount = 128)
        val enforcer = enforcer(
            firstFacet to constraint(group, amount = 64),
            secondFacet to constraint(group, amount = 64),
        )

        enforcer.addItem(firstFacet)
        enforcer.addItem(secondFacet)

        assertEquals(SatisfactionStatus.SATISFIED, enforcer.getSatisfactionStatus(firstFacet))
    }

    private fun enforcer(vararg constraints: Pair<ItemFacet, ItemConstraintInfo>): ItemNumberConstraintEnforcer {
        val constraintsByFacet = constraints.toMap()
        val provider = object : ItemAmountConstraintProvider {
            override fun getConstraints(item: ItemFacet) = arrayListOf(constraintsByFacet.getValue(item))

            override fun getAllocationPriority(itemGroup: ItemCategory) = 0
        }
        val template = CleanupPlanTemplate(
            slotContentMap = emptyMap(),
            itemAmountConstraintProvider = provider,
            itemBlacklist = emptySet(),
            restrictions = CleanupPlanTemplate.CleanupPlanRestrictions(emptyMap()),
        )

        return ItemNumberConstraintEnforcer(template, constraintsByFacet.keys.toList())
    }

    private fun constraintGroup(id: Int, desiredAmount: Int) = SpecificItemGroupConstraintGroup(
        acceptableRange = desiredAmount..Int.MAX_VALUE,
        priority = id,
        groupId = id,
    )

    private fun constraint(group: ItemNumberConstraintGroup, amount: Int) = ItemConstraintInfo(
        group = group,
        amountAddedByItem = amount,
        default = false,
    )

    private fun virtualSlot(id: Int) = VirtualItemSlot(ItemStack.EMPTY, ItemSlot.Type.INVENTORY, id)
}
