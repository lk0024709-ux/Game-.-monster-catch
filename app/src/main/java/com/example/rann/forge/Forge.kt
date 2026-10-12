package com.example.rann.forge

import com.example.rann.model.Balance
import com.example.rann.model.WeaponTier
import kotlin.math.max

// ======================================================
// THE FORGE — idle weapon merging (home screen mini-game)
// Pure Kotlin: safe to unit test and to simulate offline.
// ======================================================

enum class DropKind { MOVE, MERGE, SWAP, REFUSE }

data class DropOutcome(val kind: DropKind, val newTier: Int = -1)

object ForgeRules {
    const val GRID_SIZE = 9
    const val BASE_INTERVAL = 5f
    const val MIN_INTERVAL = 1.5f
    const val MERGES_PER_SPEEDUP = 10
    const val SPEEDUP_PER_MERGE = 0.3f
    const val FORGE_NOW_COOLDOWN = 10f

    /** Auto-forge interval: 5s, minus 0.3s per 10 merges, floor 1.5s. */
    fun intervalFor(merges: Int): Float {
        val steps = merges / MERGES_PER_SPEEDUP
        return max(MIN_INTERVAL, BASE_INTERVAL - SPEEDUP_PER_MERGE * steps)
    }

    fun mergeOf(tier: Int): Int? = if (tier < WeaponTier.maxTier()) tier + 1 else null

    /**
     * Drop resolution on the 3x3 grid.
     * - empty target: move
     * - same tier: merge (unless max tier: refuse)
     * - different tier: swap
     */
    fun resolveDrop(sourceTier: Int, targetTier: Int?): DropOutcome {
        if (targetTier == null) return DropOutcome(DropKind.MOVE)
        if (targetTier == sourceTier) {
            val merged = mergeOf(sourceTier)
            return if (merged == null) DropOutcome(DropKind.REFUSE, sourceTier)
            else DropOutcome(DropKind.MERGE, merged)
        }
        return DropOutcome(DropKind.SWAP)
    }

    fun mergeReward(): Int = Balance.MERGE_COINS
}

/** Mutable forge grid plus idle bookkeeping. */
class ForgeState(
    val slots: MutableList<Int?> = MutableList(ForgeRules.GRID_SIZE) { null },
    var merges: Int = 0,
    var lastTickMillis: Long = 0L,
) {

    val intervalSeconds: Float get() = ForgeRules.intervalFor(merges)

    fun isFull(): Boolean = slots.none { it == null }

    fun filledCount(): Int = slots.count { it != null }

    fun firstEmptyIndex(): Int = slots.indexOfFirst { it == null }

    fun highestTier(): Int? = slots.filterNotNull().maxOrNull()

    fun daggerCount(): Int = slots.count { it == WeaponTier.DAGGER.tier }

    /** Spawns a dagger into the first empty slot. Returns the slot or -1 when full. */
    fun spawnDagger(): Int {
        val idx = firstEmptyIndex()
        if (idx < 0) return -1
        slots[idx] = WeaponTier.DAGGER.tier
        return idx
    }

    fun addWeapon(tier: Int): Boolean {
        val idx = firstEmptyIndex()
        if (idx < 0) return false
        slots[idx] = tier.coerceIn(1, WeaponTier.maxTier())
        return true
    }

    /** Instant spawn from the furnace button (10s cooldown handled by the UI layer). */
    fun forgeNow(): Int = spawnDagger()

    fun applyDrop(from: Int, to: Int): DropOutcome {
        if (from == to) return DropOutcome(DropKind.REFUSE)
        val source = slots.getOrNull(from) ?: return DropOutcome(DropKind.REFUSE)
        val target = slots.getOrNull(to)
        val outcome = ForgeRules.resolveDrop(source, target)
        when (outcome.kind) {
            DropKind.MOVE -> {
                slots[to] = source
                slots[from] = null
            }
            DropKind.MERGE -> {
                slots[to] = outcome.newTier
                slots[from] = null
                merges++
                if (lastTickMillis == 0L) lastTickMillis = System.currentTimeMillis()
            }
            DropKind.SWAP -> {
                slots[to] = source
                slots[from] = target
            }
            DropKind.REFUSE -> Unit
        }
        return outcome
    }

    /**
     * Offline catch-up: every full [intervalSeconds] of absence forges one dagger,
     * but only while empty slots remain (the furnace pauses when the grid is FULL).
     *
     * @return number of daggers forged while away
     */
    fun simulateOffline(nowMillis: Long): Int {
        if (lastTickMillis <= 0L) {
            lastTickMillis = nowMillis
            return 0
        }
        val elapsedMs = (nowMillis - lastTickMillis).coerceAtLeast(0L)
        var spawned = 0
        var remaining = elapsedMs
        val intervalMs = (intervalSeconds * 1000f).toLong().coerceAtLeast(200L)
        while (remaining >= intervalMs && !isFull()) {
            if (spawnDagger() < 0) break
            remaining -= intervalMs
            spawned++
            if (spawned > 500) break // guard rail
        }
        // keep the leftover progress towards the next dagger
        lastTickMillis = nowMillis - remaining.coerceAtMost(intervalMs)
        return spawned
    }

    fun tickElapsedMillis(nowMillis: Long): Long {
        if (lastTickMillis <= 0L) lastTickMillis = nowMillis
        return nowMillis - lastTickMillis
    }

    fun progressToNext(nowMillis: Long): Float {
        val elapsed = tickElapsedMillis(nowMillis).coerceAtLeast(0L)
        val interval = (intervalSeconds * 1000f).toLong().coerceAtLeast(1L)
        return (elapsed % interval).toFloat() / interval.toFloat()
    }

    fun markTick(nowMillis: Long) {
        lastTickMillis = nowMillis
    }

    fun toSlotCsv(): String = slots.joinToString(",") { (it ?: -1).toString() }

    companion object {
        fun fromSlotCsv(csv: String, merges: Int, lastTickMillis: Long): ForgeState {
            val parsed = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
            val slots = MutableList<Int?>(ForgeRules.GRID_SIZE) { null }
            for (i in 0 until minOf(ForgeRules.GRID_SIZE, parsed.size)) {
                val v = parsed[i]
                slots[i] = if (v in 1..WeaponTier.maxTier()) v else null
            }
            return ForgeState(slots, merges, lastTickMillis)
        }
    }
}
