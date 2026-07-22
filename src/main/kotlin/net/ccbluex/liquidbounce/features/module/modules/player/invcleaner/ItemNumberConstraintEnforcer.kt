package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot

/**
 * This class serves two functions:
 * - Keeps track of the current state of the fulfilment of the item number limits.
 * - Decides whether an item is useful or not.
 */
class ItemNumberConstraintEnforcer(
    private val template: CleanupPlanTemplate,
    itemFacets: List<ItemFacet>,
) {
    private val currentLimit = Object2IntOpenHashMap<ItemNumberConstraintGroup>()
    private val countedConstraintGroupsBySlot = HashMap<ItemSlot, HashSet<ItemNumberConstraintGroup>>()
    private val slotsWithNonDefaultConstraints = buildSet {
        for (facet in itemFacets) {
            if (template.itemAmountConstraintProvider.hasNonDefaultConstraints(facet)) {
                this += facet.itemSlot
            }
        }
    }

    /**
     * Decides whether the given item facet is useful.
     * The decision is made based on the items that have been added via [addItem]
     */
    fun getSatisfactionStatus(item: ItemFacet): SatisfactionStatus {
        val constraints = getApplyingConstraints(item)

        constraints.sortBy { it.group.priority }

        for (constraintInfo in constraints) {
            val currentCount = this.currentLimit.getOrDefault(constraintInfo.group, 0)

            if (currentCount > constraintInfo.group.acceptableRange.last) {
                return SatisfactionStatus.OVERSATURATED
            } else if (currentCount < constraintInfo.group.acceptableRange.first) {
                return SatisfactionStatus.NOT_SATISFIED
            }
        }

        return SatisfactionStatus.SATISFIED
    }

    internal fun hasApplyingConstraints(item: ItemFacet): Boolean = getApplyingConstraints(item).isNotEmpty()

    /**
     * Called when an item is kept in the inventory.
     */
    fun addItem(item: ItemFacet) {
        val constraints = getApplyingConstraints(item)
        val countedConstraintGroups = countedConstraintGroupsBySlot.getOrPut(item.itemSlot, ::HashSet)

        for (constraintInfo in constraints) {
            if (countedConstraintGroups.add(constraintInfo.group)) {
                this.currentLimit.addTo(constraintInfo.group, constraintInfo.amountAddedByItem)
            }
        }
    }

    private fun getApplyingConstraints(item: ItemFacet): ArrayList<ItemConstraintInfo> {
        return template.itemAmountConstraintProvider.getApplyingConstraints(
            item,
            useDefaultConstraints = item.itemSlot !in slotsWithNonDefaultConstraints,
        )
    }

    enum class SatisfactionStatus {
        /**
         * Keep the item
         */
        NOT_SATISFIED,

        /**
         * The item is not needed - except for filling slots.
         */
        SATISFIED,

        /**
         * The item shouldn't be kept - even if there are still slots to fill.
         */
        OVERSATURATED,
    }
}
