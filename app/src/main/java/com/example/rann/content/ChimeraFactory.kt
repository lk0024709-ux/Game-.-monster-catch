package com.example.rann.content

import com.example.rann.model.Balance
import com.example.rann.model.ChimeraAbility
import com.example.rann.model.ChimeraDefense
import com.example.rann.model.ChimeraType

// ======================================================
// ELITE MODE — THE CHIMERA TRIALS (7 fusion bosses, 1v1)
// ======================================================
data class ChimeraSpec(
    val index: Int,
    val type: ChimeraType,
    val hp: Float,
    /** Per-chimera health trim so the entry trials stay approachable. */
    val hpScale: Float = 1f,
    val abilities: List<ChimeraAbility>,
    val defenses: List<ChimeraDefense>,
    val coinReward: Int,
    val title: String?,
    val recommendedTier: Int,
) {
    val displayName: String get() = type.displayName
    val emoji: String get() = type.emoji
    val arenaName: String get() = type.arenaName

    fun abilitiesSummary(): String = abilities.joinToString(" · ") { it.displayName }

    fun defensesSummary(): String = defenses.joinToString(" · ") { it.displayName }
}

object ChimeraFactory {

    val ALL: List<ChimeraSpec> = listOf(
        ChimeraSpec(
            index = 0,
            type = ChimeraType.GRYPHON_PRIME,
            hp = ChimeraType.GRYPHON_PRIME.baseHp * ChimeraType.hpMultiplier * 0.75f,
            abilities = listOf(ChimeraAbility.DIVE_STRIKE, ChimeraAbility.SHOCKWAVE, ChimeraAbility.TAIL_SWIPE),
            defenses = listOf(ChimeraDefense.IRON_HIDE, ChimeraDefense.DODGE_INSTINCT),
            coinReward = Balance.eliteCoins(0, false),
            title = "Chimera Hunter",
            recommendedTier = 2,
        ),
        ChimeraSpec(
            index = 1,
            type = ChimeraType.BASILISK,
            hp = ChimeraType.BASILISK.baseHp * ChimeraType.hpMultiplier * 0.9f,
            abilities = listOf(
                ChimeraAbility.POISON_BREATH, ChimeraAbility.PETRIFY,
                ChimeraAbility.TAIL_SWIPE, ChimeraAbility.LIGHTNING_STRIKE,
            ),
            defenses = listOf(ChimeraDefense.IRON_HIDE, ChimeraDefense.REGEN),
            coinReward = Balance.eliteCoins(1, false),
            title = null,
            recommendedTier = 3,
        ),
        ChimeraSpec(
            index = 2,
            type = ChimeraType.RAVAGER,
            hp = ChimeraType.RAVAGER.baseHp * ChimeraType.hpMultiplier * 0.95f,
            abilities = listOf(
                ChimeraAbility.CHARGE, ChimeraAbility.ROAR,
                ChimeraAbility.SHOCKWAVE, ChimeraAbility.TAIL_SWIPE,
            ),
            defenses = listOf(ChimeraDefense.IRON_HIDE, ChimeraDefense.DAMAGE_REFLECT),
            coinReward = Balance.eliteCoins(2, false),
            title = null,
            recommendedTier = 3,
        ),
        ChimeraSpec(
            index = 3,
            type = ChimeraType.WYVERN,
            hp = ChimeraType.WYVERN.baseHp * ChimeraType.hpMultiplier * 0.9f,
            abilities = listOf(
                ChimeraAbility.FIREBALLS, ChimeraAbility.DIVE_STRIKE, ChimeraAbility.LIGHTNING_STRIKE,
            ),
            defenses = listOf(ChimeraDefense.DODGE_INSTINCT, ChimeraDefense.ENERGY_SHIELD),
            coinReward = Balance.eliteCoins(3, false),
            title = null,
            recommendedTier = 4,
        ),
        ChimeraSpec(
            index = 4,
            type = ChimeraType.DEATHROLL,
            hp = ChimeraType.DEATHROLL.baseHp * ChimeraType.hpMultiplier * 0.8f,
            abilities = listOf(
                ChimeraAbility.ROLL_ATTACK, ChimeraAbility.POISON_BREATH, ChimeraAbility.CHARGE,
            ),
            defenses = listOf(
                ChimeraDefense.IRON_HIDE, ChimeraDefense.DAMAGE_REFLECT, ChimeraDefense.REGEN,
            ),
            coinReward = Balance.eliteCoins(4, false),
            title = null,
            recommendedTier = 4,
        ),
        ChimeraSpec(
            index = 5,
            type = ChimeraType.RANNS_CHIMERA,
            hp = ChimeraType.RANNS_CHIMERA.baseHp * ChimeraType.hpMultiplier * 0.95f,
            abilities = listOf(
                ChimeraAbility.DIVE_STRIKE, ChimeraAbility.SHOCKWAVE, ChimeraAbility.POISON_BREATH,
                ChimeraAbility.FIREBALLS, ChimeraAbility.CHARGE, ChimeraAbility.TAIL_SWIPE,
            ),
            defenses = listOf(
                ChimeraDefense.IRON_HIDE, ChimeraDefense.REGEN,
                ChimeraDefense.ENERGY_SHIELD, ChimeraDefense.SUMMON_MINIONS,
            ),
            coinReward = Balance.eliteCoins(5, false),
            title = null,
            recommendedTier = 5,
        ),
        ChimeraSpec(
            index = 6,
            type = ChimeraType.OMEGA_CHIMERA,
            hp = ChimeraType.OMEGA_CHIMERA.baseHp * ChimeraType.hpMultiplier,
            abilities = ChimeraAbility.values().toList(),
            defenses = ChimeraDefense.values().toList(),
            coinReward = Balance.eliteCoins(6, false),
            title = "Omega Breaker",
            recommendedTier = 6,
        ),
    )

    fun of(index: Int): ChimeraSpec = ALL[index.coerceIn(0, ALL.size - 1)]

    fun isUnlocked(cleared: Collection<Int>, index: Int): Boolean = index in cleared
}
