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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain

/**
 * Retains exact item identity while delegating quality comparisons to the facet for the stack's actual domain.
 */
internal class ExactItemFacet(private val rankingFacet: ItemFacet) : ItemFacet(rankingFacet.itemSlot) {

    companion object {
        private val CROSS_CATEGORY_COMPARATOR = ComparatorChain<ExactItemFacet>(
            compareBy { it.rankingFacet.shouldKeep() },
            compareBy { it.rankingFacet.category.type.allocationPriority.priority },
            PREFER_ITEMS_IN_HOTBAR,
            STABILIZE_COMPARISON,
        )
    }

    override val category = ItemCategory(ItemType.EXACT_ITEM, itemStack.item)

    override fun shouldKeep(): Boolean = rankingFacet.shouldKeep()

    override fun compareTo(other: ItemFacet): Int {
        other as ExactItemFacet
        val otherRankingFacet = other.rankingFacet

        if (rankingFacet.category == otherRankingFacet.category) {
            return rankingFacet.compareTo(otherRankingFacet)
        }

        return CROSS_CATEGORY_COMPARATOR.compare(this, other)
    }
}
