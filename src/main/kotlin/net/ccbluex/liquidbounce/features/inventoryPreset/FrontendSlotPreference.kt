package net.ccbluex.liquidbounce.features.inventoryPreset

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MiningToolItemFacet
import net.minecraft.world.item.Item

/**
 * Contains the frontend representation of the user defined preference of what should a slot contain.
 */
sealed class FrontendSlotPreference {
    /**
     * Converts the frontend representation of the user
     * configured preset into a version
     * which the [net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanGenerator] understands.
     */
    abstract fun toBackendRepresentation(): ConvertedSlotPreference
    abstract fun serialize(context: JsonSerializationContext): JsonObject

    data class SingleSlotPreference(private val item: Item) : FrontendSlotPreference() {
        override fun toBackendRepresentation(): ConvertedSlotPreference {
            val contentPreference = CleanupPlanTemplate.SlotContentPreference(
                itemType = ItemType.EXACT_ITEM,
                subtypes = setOf(item)
            )

            return ConvertedSlotPreference(contentPreference)
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "SINGLE")

            add("item", context.serialize(item))
        }
    }

    data class GroupSlotPreference(private val itemGroupType: ItemGroupType) : FrontendSlotPreference() {
        override fun toBackendRepresentation(): ConvertedSlotPreference {
            return ConvertedSlotPreference(itemGroupType.preference)
        }

        /**
         * Enum representing item categories used for preset item classification.
         */
        enum class ItemGroupType(val preference: CleanupPlanTemplate.SlotContentPreference) {
            @SerializedName("ARROWS")
            ARROWS(CleanupPlanTemplate.SlotContentPreference(ItemType.ARROW)),
            @SerializedName("SWORD")
            SWORD(CleanupPlanTemplate.SlotContentPreference(ItemType.SWORD)),
            @SerializedName("WEAPON")
            WEAPON(CleanupPlanTemplate.SlotContentPreference(ItemType.WEAPON)),
            @SerializedName("AXE")
            AXE_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    ItemType.TOOL,
                    setOf(MiningToolItemFacet.MASK_AXE)
                )
            ),
            @SerializedName("HOE")
            HOE_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    ItemType.TOOL,
                    setOf(MiningToolItemFacet.MASK_HOE)
                )
            ),
            @SerializedName("SHOVEL")
            SHOVEL_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    ItemType.TOOL,
                    setOf(MiningToolItemFacet.MASK_SHOVEL)
                )
            ),
            @SerializedName("PICKAXE")
            PICKAXE_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    ItemType.TOOL,
                    setOf(MiningToolItemFacet.MASK_PICKAXE)
                )
            ),
            @SerializedName("FOOD")
            FOOD(CleanupPlanTemplate.SlotContentPreference(ItemType.FOOD)),
            @SerializedName("POTION")
            POTION(CleanupPlanTemplate.SlotContentPreference(ItemType.POTION)),
            @SerializedName("BLOCK")
            BLOCK(CleanupPlanTemplate.SlotContentPreference(ItemType.BLOCK)),
            @SerializedName("THROWABLE")
            THROWABLE(CleanupPlanTemplate.SlotContentPreference(ItemType.THROWABLE))
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "GROUP")

            add("group", context.serialize(itemGroupType))
        }
    }

    data object IgnoreSlotPreference : FrontendSlotPreference() {
        override fun toBackendRepresentation(): ConvertedSlotPreference {
            return ConvertedSlotPreference(null, RestrictionType.FORBID_TAMPERING)
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "IGNORE")
        }
    }

    data object AnySlotPreference : FrontendSlotPreference() {
        override fun toBackendRepresentation(): ConvertedSlotPreference {
            return ConvertedSlotPreference(null, RestrictionType.NONE)
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "ANY")
        }
    }

    data class ConvertedSlotPreference(
        val contentPreference: CleanupPlanTemplate.SlotContentPreference?,
        val slotRestriction: RestrictionType = RestrictionType.NONE
    )
}
