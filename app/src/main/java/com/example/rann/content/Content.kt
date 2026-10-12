package com.example.rann.content

import com.example.rann.model.ArmorRank
import com.example.rann.model.BoosterType
import com.example.rann.model.Balance
import com.example.rann.model.CreatureType
import com.example.rann.model.LevelModifier
import com.example.rann.model.PowerUpType
import com.example.rann.model.Rank

// ======================================================
// STATIC CONTENT: armors, boosters, sinks, titles, achievements.
// ======================================================

data class CoinSink(
    val id: String,
    val icon: String,
    val name: String,
    val price: Int,
    val description: String,
)

data class TitleDef(
    val id: String,
    val label: String,
    val requirement: String,
)

data class AchievementDef(
    val id: Int,
    val icon: String,
    val name: String,
    val description: String,
    val rewardCoins: Int,
)

object Content {

    val TITLES: List<TitleDef> = listOf(
        TitleDef("Novice", "Novice", "Default title"),
        TitleDef("Pitfighter", "Pitfighter", "Clear Bronze rank"),
        TitleDef("Frostbreaker", "Frostbreaker", "Defeat the Frost Tyrant"),
        TitleDef("Emberlord's Bane", "Emberlord's Bane", "Defeat the Emberlord"),
        TitleDef("Temple Warden", "Temple Warden", "Defeat the Temple Guardian"),
        TitleDef("Crystal Sovereign", "Crystal Sovereign", "Defeat the Crystal Queen"),
        TitleDef("Stormcaller", "Stormcaller", "Defeat the Stormlord"),
        TitleDef("Rannslayer", "Rannslayer", "Defeat Rann's Avatar"),
        TitleDef("Chimera Hunter", "Chimera Hunter", "Clear any Elite trial"),
        TitleDef("Omega Breaker", "Omega Breaker", "Defeat the Omega Chimera"),
    )

    val ACHIEVEMENTS: List<AchievementDef> = listOf(
        AchievementDef(0, "🗡️", "First Blood", "Clear your first campaign level", 100),
        AchievementDef(1, "🔥", "Forge Apprentice", "Merge 10 weapons in the Forge", 150),
        AchievementDef(2, "💎", "Mythic Smith", "Forge a Mythic Blade", 500),
        AchievementDef(3, "⚡", "Swift Hunter", "Clear any level in under 50 seconds", 200),
        AchievementDef(4, "🛡️", "Untouchable", "Win a battle without taking damage", 250),
        AchievementDef(5, "👑", "Rank Climber", "Clear a rank boss", 300),
        AchievementDef(6, "🪙", "Coin Baron", "Earn 10,000 coins in total", 400),
        AchievementDef(7, "🦁", "Chimera Hunter", "Win an Elite trial", 350),
        AchievementDef(8, "💠", "Platinum Soul", "Reach the Platinum rank", 500),
        AchievementDef(9, "🐲", "Rannslayer", "Defeat Rann's Avatar", 1500),
    )

    val SINKS: List<CoinSink> = listOf(
        CoinSink("speedup", "⏩", "Forge Speed-up", Balance.SINK_FORGE_SPEEDUP, "Instantly spawn one dagger in the Forge"),
        CoinSink("fragment", "🧩", "Weapon Fragment", Balance.SINK_WEAPON_FRAGMENT, "Instantly claim a random forged weapon"),
        CoinSink("gradeskip", "⏭️", "Grade Skip Token", Balance.SINK_GRADE_SKIP, "Mark your next uncleared level as cleared (grade D)"),
    )

    val POWERUPS: List<PowerUpType> = PowerUpType.values().toList()

    val ARMOR_ORDER: List<ArmorRank> = ArmorRank.values().toList()

    val BOOSTER_ORDER: List<BoosterType> = BoosterType.values().toList()

    /** Creature pool for a rank, used when a level does not pin its own mix. */
    fun creaturePoolForRank(rank: Rank): List<CreatureType> = when (rank) {
        Rank.BRONZE -> listOf(CreatureType.GRIFFIN, CreatureType.SKYHAWK)
        Rank.SILVER -> listOf(CreatureType.SKYHAWK, CreatureType.DRAKE, CreatureType.GRIFFIN)
        Rank.GOLD -> listOf(CreatureType.IRONBOAR, CreatureType.DRAKE, CreatureType.GRIFFIN)
        Rank.PLATINUM -> listOf(CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR, CreatureType.VENOMTAIL)
        Rank.DIAMOND -> listOf(CreatureType.SKYHAWK, CreatureType.VENOMTAIL, CreatureType.DRAKE)
        Rank.MASTERS -> listOf(CreatureType.SKYHAWK, CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR)
        Rank.GRAND_MASTERS -> CreatureType.values().toList()
    }

    /** Modifier pool from which a level draws its 1-4 modifiers. */
    fun modifierPool(rank: Rank, levelInRank: Int): List<LevelModifier> {
        val easy = listOf(LevelModifier.NPC_SWIFT, LevelModifier.NO_HEAL, LevelModifier.SLIPPERY)
        val mid = easy + listOf(
            LevelModifier.NPC_TOUGH, LevelModifier.NPC_FIERCE, LevelModifier.FAST_STORM,
            LevelModifier.DARKNESS, LevelModifier.LOW_GRAVITY, LevelModifier.SLOW_FORGE,
        )
        val hard = mid + listOf(
            LevelModifier.NPC_LEGION, LevelModifier.NPC_ELITE, LevelModifier.RISING_LAVA,
            LevelModifier.IRON_SKINLESS, LevelModifier.NO_SHIELD, LevelModifier.NO_POWERS,
            LevelModifier.VENOM_HAZE,
        )
        val brutal = hard + listOf(
            LevelModifier.BOSS_RUSH, LevelModifier.IRONMAN, LevelModifier.SPEEDRUN,
            LevelModifier.MIRROR_MATCH,
        )
        return when {
            levelInRank <= 20 -> easy
            levelInRank <= 35 -> mid
            levelInRank <= 45 -> hard + if (rank.ordinal >= 3) listOf(LevelModifier.BOSS_RUSH) else emptyList()
            else -> brutal
        }
    }
}
