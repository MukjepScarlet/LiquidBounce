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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.injection.mixins.minecraft.core.MixinColorParticleOptionAccessor
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinLivingEntityAccessor
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.ArrayDeque
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min

internal object NametagEffectsInference {

    private data class EffectSignature(
        val particleTypeId: Int,
        val normalizedColorArgb: Int?,
    )

    data class InferredEffect(
        val effect: Holder<MobEffect>,
        val confidence: Float,
    )

    private data class EntityInferenceState(
        val history: ArrayDeque<FrameObservation> = ArrayDeque(),
        var observationCount: Int = 0,
        var ambientTrueCount: Int = 0,
    )

    private data class FrameObservation(
        val signatures: Set<EffectSignature>,
        val ambient: Boolean,
    )

    private val entityStates = Int2ObjectOpenHashMap<EntityInferenceState>()
    private val expectedSignatureByEffect = IdentityHashMap<Holder<MobEffect>, EffectSignature?>()

    private val entityIdsCache = IntOpenHashSet()

    fun clearAll() {
        entityStates.clear()
        expectedSignatureByEffect.clear()
    }

    fun clearMissingEntities(activeEntities: Iterable<Entity>) {
        activeEntities.forEach { entityIdsCache.add(it.id) }
        clearMissingEntities(entityIdsCache)
        entityIdsCache.clear()
    }

    fun clearMissingEntities(activeEntityIds: IntSet) {
        entityStates.keys.retainAll(activeEntityIds)
    }

    fun clearEntity(entityId: Int) {
        entityStates.remove(entityId)
    }

    fun observe(entity: LivingEntity, tick: Long) {
        val config = NametagTextFormatter.EffectsInference
        if (!config.enabled) {
            return
        }

        if (tick % config.updateIntervalTicks != 0L) {
            return
        }

        val particleAccessor = MixinLivingEntityAccessor.getDataEffectParticles()
        val ambientAccessor = MixinLivingEntityAccessor.getDataEffectAmbienceId()
        val particleOptions = entity.entityData.get(particleAccessor)
        val ambient = entity.entityData.get(ambientAccessor)
        val frame = FrameObservation(
            signatures = particleOptions.mapNotNullTo(HashSet(), ::toSignature),
            ambient = ambient
        )

        if (frame.signatures.isEmpty()) {
            clearEntity(entity.id)
            return
        }

        val state = entityStates.computeIfAbsent(entity.id) { EntityInferenceState() }
        pushFrame(state, frame, config.windowTicks)
    }

    fun infer(entity: LivingEntity): List<InferredEffect> {
        val config = NametagTextFormatter.EffectsInference
        if (!config.enabled) {
            return emptyList()
        }

        if (entity.position().cameraDistanceSq() > config.maxInferenceDistanceSq.toDouble()) {
            return emptyList()
        }

        val candidates = config.candidates
        if (candidates.isEmpty()) {
            return emptyList()
        }

        val state = entityStates[entity.id] ?: return emptyList()
        return inferFromObservations(
            observations = state.history,
            candidates = candidates.map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder),
            minConfidence = config.minConfidence,
            maxDisplayEffects = config.maxDisplayEffects
        )
    }

    private fun inferFromObservations(
        observations: Collection<FrameObservation>,
        candidates: Collection<Holder<MobEffect>>,
        minConfidence: Float,
        maxDisplayEffects: Int,
    ): List<InferredEffect> {
        if (observations.isEmpty() || candidates.isEmpty()) {
            return emptyList()
        }

        val ambientCount = observations.count { it.ambient }
        val ambientRatio = ambientCount.toFloat() / observations.size.toFloat()

        class Scored(private val effect: Holder<MobEffect>, private val raw: Float) {
            fun toInferredEffect(sum: Float) = InferredEffect(effect, confidence = raw / sum)
        }
        val scored = ArrayList<Scored>(candidates.size)
        var sum = 0.0f
        for (candidate in candidates) {
            val score = scoreCandidate(observations, candidate, ambientRatio)
            if (score > 0.0f) {
                scored += Scored(candidate, score)
                sum += score
            }
        }

        if (sum <= 0.0f || scored.isEmpty()) {
            return emptyList()
        }

        val normalized = scored
            .mapToArray { it.toInferredEffect(sum) }
            .filter { it.confidence >= minConfidence }
            .sortedByDescending { it.confidence }

        if (normalized.isEmpty()) {
            return emptyList()
        }

        val size = min(maxDisplayEffects, normalized.size)
        val top = normalized.subList(0, size).toMutableList()
        if (top.size >= normalized.size) {
            return top
        }

        val lastConfidence = top.last().confidence
        var index = top.size
        while (index < normalized.size && normalized[index].confidence == lastConfidence) {
            top += normalized[index]
            index++
        }

        return top
    }

    private fun scoreCandidate(
        observations: Collection<FrameObservation>,
        effect: Holder<MobEffect>,
        ambientRatio: Float,
    ): Float {
        var matched = 0
        var total = 0

        for (frame in observations) {
            total += frame.signatures.size
            matched += frame.signatures.count { matchesEffect(effect, it) }
        }

        if (total == 0) {
            return 0.0f
        }

        val baseScore = matched.toFloat() / total.toFloat()
        if (baseScore <= 0.0f) {
            return 0.0f
        }

        val ambientScore = if (ambientRatio >= 0.5f) 1.0f else 0.9f
        return baseScore * ambientScore
    }

    private fun matchesEffect(effect: Holder<MobEffect>, observed: EffectSignature): Boolean {
        val expected = expectedSignatureByEffect.computeIfAbsent(effect) {
            val syntheticInstance = MobEffectInstance(it, 200, 0, false, true, true)
            toSignature(it.value().createParticleOptions(syntheticInstance))
        } ?: return false

        return observed.particleTypeId == expected.particleTypeId &&
            (observed.normalizedColorArgb == null || expected.normalizedColorArgb == null || observed.normalizedColorArgb == expected.normalizedColorArgb)
    }

    private fun pushFrame(state: EntityInferenceState, frame: FrameObservation, windowTicks: Int) {
        state.history.addLast(frame)
        state.observationCount++
        if (frame.ambient) {
            state.ambientTrueCount++
        }

        val maxWindow = max(1, windowTicks)
        while (state.history.size > maxWindow) {
            val removed = state.history.removeFirst()
            state.observationCount--
            if (removed.ambient) {
                state.ambientTrueCount--
            }
        }
    }

    private fun toSignature(options: ParticleOptions): EffectSignature? {
        val typeId = BuiltInRegistries.PARTICLE_TYPE.getId(options.type)
        if (typeId < 0) {
            return null
        }

        val color = (options as? MixinColorParticleOptionAccessor)?.color

        return EffectSignature(
            particleTypeId = typeId,
            normalizedColorArgb = color
        )
    }
}
