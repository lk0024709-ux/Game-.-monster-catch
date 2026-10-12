package com.example.rann.content

import com.example.rann.model.Balance
import com.example.rann.model.BossSpec
import com.example.rann.model.BossTrait
import com.example.rann.model.CreatureType
import com.example.rann.model.LevelModifier
import com.example.rann.model.LevelSpec
import com.example.rann.model.Maps
import com.example.rann.model.Rank

// ======================================================
// 350 LEVELS = 7 RANKS × 50 LEVELS
// Fully deterministic: same seed -> same level, forever.
// ======================================================
object LevelFactory {

    const val TOTAL_LEVELS = Balance.TOTAL_LEVELS
    const val LEVELS_PER_RANK = Balance.LEVELS_PER_RANK

    private val specCache = HashMap<Int, LevelSpec>()
    private val bossCache = HashMap<Int, BossSpec>()

    // --------------------------------------------------
    // RANK BOSSES (handcrafted)
    // --------------------------------------------------
    val BOSSES: List<BossSpec> = listOf(
        BossSpec(
            index = 0, name = "Alpha Griffin", icon = "🦅", hp = 400f, size = 2.6f,
            damageMult = 1.35f, speedMult = 1.1f, creature = CreatureType.GRIFFIN,
            traits = emptyList(),
            adds = List(6) { CreatureType.GRIFFIN },
            rewardCoins = 2000, unlockText = "Silver rank unlocked",
            title = "Pitfighter",
        ),
        BossSpec(
            index = 1, name = "Frost Tyrant", icon = "🐉", hp = 500f, size = 2.8f,
            damageMult = 1.4f, speedMult = 1.0f, creature = CreatureType.DRAKE,
            traits = listOf(BossTrait.SUMMONS),
            adds = List(4) { CreatureType.DRAKE } + List(4) { CreatureType.SKYHAWK },
            rewardCoins = 4000, unlockText = "Gold rank unlocked",
            title = "Frostbreaker",
        ),
        BossSpec(
            index = 2, name = "Emberlord", icon = "🐗", hp = 700f, size = 3.0f,
            damageMult = 1.45f, speedMult = 1.05f, creature = CreatureType.IRONBOAR,
            traits = listOf(BossTrait.LAVA_AURA),
            adds = List(5) { CreatureType.IRONBOAR } + List(3) { CreatureType.VENOMTAIL },
            rewardCoins = 7000, unlockText = "Platinum rank unlocked",
            title = "Emberlord's Bane",
        ),
        BossSpec(
            index = 3, name = "Temple Guardian", icon = "🗿", hp = 500f, size = 2.7f,
            damageMult = 1.4f, speedMult = 1.0f, creature = CreatureType.GRIFFIN,
            traits = listOf(BossTrait.TWIN, BossTrait.CLONE_SPAWN),
            adds = listOf(
                CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR, CreatureType.VENOMTAIL,
                CreatureType.SKYHAWK, CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR,
            ),
            rewardCoins = 11000, unlockText = "Diamond rank unlocked",
            title = "Temple Warden",
        ),
        BossSpec(
            index = 4, name = "Crystal Queen", icon = "💠", hp = 500f, size = 2.9f,
            damageMult = 1.4f, speedMult = 1.15f, creature = CreatureType.VENOMTAIL,
            traits = listOf(BossTrait.CLONE_SPAWN, BossTrait.SUMMONS),
            adds = listOf(
                CreatureType.SKYHAWK, CreatureType.VENOMTAIL, CreatureType.DRAKE,
                CreatureType.GRIFFIN, CreatureType.IRONBOAR, CreatureType.VENOMTAIL,
            ),
            rewardCoins = 16000, unlockText = "Masters rank unlocked",
            title = "Crystal Sovereign",
        ),
        BossSpec(
            index = 5, name = "Stormlord", icon = "🌩️", hp = 900f, size = 3.2f,
            damageMult = 1.5f, speedMult = 1.2f, creature = CreatureType.SKYHAWK,
            traits = listOf(BossTrait.CHAIN_LIGHTNING, BossTrait.SUMMONS),
            adds = List(10) {
                listOf(
                    CreatureType.SKYHAWK, CreatureType.GRIFFIN, CreatureType.DRAKE,
                    CreatureType.IRONBOAR, CreatureType.VENOMTAIL,
                )[it % 5]
            },
            rewardCoins = 22000, unlockText = "Grand Masters unlocked",
            title = "Stormcaller",
        ),
        BossSpec(
            index = 6, name = "Rann's Avatar", icon = "👑", hp = 1500f, size = 3.6f,
            damageMult = 1.6f, speedMult = 1.25f, creature = CreatureType.GRIFFIN,
            traits = listOf(BossTrait.SPLIT_PHASES, BossTrait.CHAIN_LIGHTNING, BossTrait.SUMMONS),
            adds = List(12) { CreatureType.values()[it % CreatureType.values().size] },
            rewardCoins = 50000, unlockText = "Rannslayer — permanent 1.5x coin multiplier",
            title = "Rannslayer",
            phases = 3,
        ),
    )

    fun bossSpec(rankIndex: Int): BossSpec = BOSSES[rankIndex.coerceIn(0, BOSSES.size - 1)]

    // --------------------------------------------------
    // LEVEL SPEC GENERATION
    // --------------------------------------------------
    fun spec(globalLevel: Int): LevelSpec {
        val level = globalLevel.coerceIn(1, TOTAL_LEVELS)
        specCache[level]?.let { return it }
        val rankIndex = (level - 1) / LEVELS_PER_RANK
        val levelInRank = ((level - 1) % LEVELS_PER_RANK) + 1
        val rank = Rank.of(rankIndex)
        val mapIndex = rank.index
        val isBoss = levelInRank == LEVELS_PER_RANK
        val boss = if (isBoss) bossSpec(rankIndex) else null

        val mix = creatureMix(rankIndex, levelInRank)
        val mods = if (isBoss) bossModifiers(rankIndex) else modifiersFor(rankIndex, levelInRank)

        var count = Balance.npcCountFor(rankIndex, levelInRank)
        if (mods.contains(LevelModifier.NPC_LEGION)) count += 5
        if (isBoss) count = boss!!.adds.size

        val spec = LevelSpec(
            globalLevel = level,
            rankIndex = rankIndex,
            levelInRank = levelInRank,
            mapIndex = mapIndex,
            creatureMix = mix,
            npcCount = count,
            modifiers = mods,
            isBoss = isBoss,
            bossSpec = boss,
            difficulty = Balance.levelDifficulty(levelInRank),
            phaseLabel = phaseLabel(levelInRank),
        )
        specCache[level] = spec
        return spec
    }

    fun allLevels(): List<LevelSpec> = (1..TOTAL_LEVELS).map { spec(it) }

    fun phaseLabel(levelInRank: Int): String = when {
        levelInRank <= 10 -> "Onboarding"
        levelInRank <= 20 -> "Duo"
        levelInRank <= 30 -> "Trio"
        levelInRank <= 40 -> "Escalation"
        levelInRank <= 49 -> "Gauntlet"
        else -> "Rank Boss"
    }

    /** Creature mix per level phase (1 -> 2 -> 3 -> 4 -> 5 types). */
    fun creatureMix(rankIndex: Int, levelInRank: Int): List<CreatureType> {
        val pool = Content.creaturePoolForRank(Rank.of(rankIndex))
        val types = when {
            levelInRank <= 10 -> 1
            levelInRank <= 20 -> 2
            levelInRank <= 30 -> 3
            levelInRank <= 40 -> 4
            else -> 5
        }
        return (0 until types).map { pool[it % pool.size] }
    }

    /** 1-4 deterministic modifiers from the rank's pool. */
    fun modifiersFor(rankIndex: Int, levelInRank: Int): List<LevelModifier> {
        val rank = Rank.of(rankIndex)
        val pool = Content.modifierPool(rank, levelInRank)
        val count = when {
            levelInRank <= 12 -> 0
            levelInRank <= 24 -> 1
            levelInRank <= 36 -> 2
            levelInRank <= 44 -> 3
            else -> 4
        }
        if (count == 0) return emptyList()
        val rng = LevelRandom(rankIndex * 10_000L + levelInRank * 31L)
        val picked = LinkedHashSet<LevelModifier>()
        var guard = 0
        while (picked.size < count && guard < 60) {
            picked.add(pool[rng.nextInt(pool.size)])
            guard++
        }
        return picked.toList()
    }

    private fun bossModifiers(rankIndex: Int): List<LevelModifier> = when (rankIndex) {
        0 -> listOf(LevelModifier.NPC_FIERCE)
        1 -> listOf(LevelModifier.NO_HEAL, LevelModifier.FAST_STORM)
        2 -> listOf(LevelModifier.RISING_LAVA, LevelModifier.NPC_TOUGH)
        3 -> listOf(LevelModifier.MIRROR_MATCH, LevelModifier.NO_SHIELD)
        4 -> listOf(LevelModifier.DARKNESS, LevelModifier.NO_POWERS)
        5 -> listOf(LevelModifier.LOW_GRAVITY, LevelModifier.FAST_STORM, LevelModifier.NPC_SWIFT)
        // Rann's Avatar: every modifier — except the two that make the fight
        // mathematically impossible (1 HP and a 60s clock against a 1950 HP boss).
        else -> LevelModifier.values().filter {
            it != LevelModifier.IRONMAN && it != LevelModifier.SPEEDRUN
        }
    }

    fun mapFor(rankIndex: Int) = Maps.of(rankIndex)
}

/**
 * Tiny deterministic RNG (xorshift32) - pure Kotlin so every platform
 * produces byte-identical levels and battles.
 */
class LevelRandom(seed: Long) {
    private var state: Int = (seed and 0x7FFFFFFF).toInt().let { if (it == 0) 0x2545F491 else it }

    fun nextInt(): Int {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        state = x
        return x and 0x7FFFFFFF
    }

    fun nextInt(bound: Int): Int = if (bound <= 0) 0 else nextInt() % bound

    fun nextFloat(): Float = (nextInt() % 10_000) / 10_000f

    fun range(min: Float, max: Float): Float = min + (max - min) * nextFloat()

    fun chance(p: Float): Boolean = nextFloat() < p
}
