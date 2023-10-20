/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import kotlin.math.ceil

object ArmorComparator: MinecraftInstance() {
	fun getBestArmorSet(stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): ArmorSet? {
		val thePlayer = mc.thePlayer ?: return null

		// Consider dropped armor pieces
		val droppedStacks =
			if (!entityStacksMap.isNullOrEmpty())
				entityStacksMap.keys.mapNotNull { stack ->
					if (stack.item is ItemArmor) -1 to stack
					else null
				}
			else emptyList()

		// Consider currently equipped armor, when searching useful stuff in chests
		// Index is null for equipped armor when searching through a chest to prevent any accidental impossible interactions
		val equippedArmorWhenInChest =
			if (thePlayer.openContainer.windowId != 0)
				thePlayer.inventory.armorInventory.mapNotNull { null to (it ?: return@mapNotNull null) }
			else emptyList()

		val inventoryStacks =
			stacks.mapIndexedNotNull { index, itemStack ->
				if (itemStack?.item is ItemArmor) index to itemStack
				else null
			}

		val armorMap =
			(droppedStacks + equippedArmorWhenInChest + inventoryStacks)
				.asSequence()
				.sortedBy { (index, stack) ->
					// Sort items by distance from player, equipped items are always preferred with distance -1
					if (index == -1)
						thePlayer.getDistanceSqToEntity(entityStacksMap?.get(stack) ?: return@sortedBy -1.0)
					else -1.0
				}
				// Prioritise sets that are in lower parts of inventory (not in chest) or equipped, prevents stealing multiple armor duplicates.
				.sortedByDescending {
					if (it.second in thePlayer.inventory.armorInventory) Int.MAX_VALUE
					else it.first ?: Int.MAX_VALUE
				}
				// Prioritise sets with more durability, enchantments
				.sortedByDescending { it.second.totalDurability }
				.sortedByDescending { it.second.enchantmentCount }
				.sortedByDescending { it.second.enchantmentSum }
				.groupBy { (it.second.item as ItemArmor).armorType }

		val helmets = armorMap[0] ?: NULL_LIST
		val chestplates = armorMap[1] ?: NULL_LIST
		val leggings = armorMap[2] ?: NULL_LIST
		val boots = armorMap[3] ?: NULL_LIST

		val armorCombinations =
			helmets.flatMap { helmet ->
				chestplates.flatMap { chestplate ->
					leggings.flatMap { leggings ->
						boots.map { boots ->
							ArmorSet(helmet, chestplate, leggings, boots)
						}
					}
				}
			}

		return armorCombinations.maxByOrNull { it.defenseFactor }
	}
}

class ArmorSet(private vararg val armorPairs: Pair<Int?, ItemStack>?) : Iterable<Pair<Int?, ItemStack>?> {
	/**
	 * 1.4.6 - 1.8.9 Armor calculations
	 * https://minecraft.fandom.com/wiki/Armor?oldid=927013#Enchantments
	 *
	 * @return Average defense of the whole armor set.
	 */
	val defenseFactor by lazy {
		var baseDefensePercentage = 0
		var epf = 0

		forEach { pair ->
			val stack = pair?.second ?: return@forEach
			val item = stack.item as ItemArmor
			baseDefensePercentage += item.armorMaterial.getDamageReductionAmount(item.armorType) * 4

			val protectionLvl = stack.getEnchantmentLevel(Enchantment.protection)

			// Protection 4 has enchantment protection factor hardcoded to 5, other levels are equal to their epf (see wiki)
			epf += if (protectionLvl == 4) 5 else protectionLvl
		}

		val baseDefense = baseDefensePercentage / 100f

		baseDefense + (1 - baseDefense) * ceil(epf.coerceAtMost(25) * 0.75f) * 0.04f
	}

	override fun iterator() = armorPairs.iterator()

	operator fun contains(stack: ItemStack) = armorPairs.any { it?.second == stack }

	operator fun contains(index: Int) = armorPairs.any { it?.first == index }

	fun indexOf(stack: ItemStack) = armorPairs.find { it?.second == stack }?.first ?: -1

	operator fun get(index: Int) = armorPairs.getOrNull(index)
}

private val NULL_LIST = listOf<Pair<Int?, ItemStack>?>(null)