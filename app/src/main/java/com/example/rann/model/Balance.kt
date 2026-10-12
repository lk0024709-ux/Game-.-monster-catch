package com.example.rann.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// ======================================================
// TUNING CONSTANTS — one place to balance the whole game.
// ======================================================
object Balance {
    // Campaign shape (the model layer owns these so nothing points "upwards")
    const val TOTAL_LEVELS = 350
    const val LEVELS_PER_RANK = 50

    // Player core
    const val PLAYER_HP = 190f
    const val PLAYER_SPAWN_GRACE = 1.2f
    const val IDLE_REGEN = 6f
    const val IDLE_REGEN_DELAY = 4f
    const val PLAYER_SPEED = 220f
    const val PLAYER_RADIUS = 18f
    const val PLAYER_REGEN_BASE = 0f

    // Attack resolution
    const val ATTACK_ARC_FRACTION = 0.85f // × PI
    const val ATTACK_ANIM = 0.22f
    const val HIT_FLASH = 0.16f
    const val ATTACK_BUFFER = 0.12f

    // Worlds
    const val WORLD_CAMPAIGN = 2400f
    const val WORLD_ELITE = 1600f

    // Camera
    const val CAMERA_REFERENCE = 620f
    const val CAMERA_MIN_ZOOM = 0.55f
    const val CAMERA_MAX_ZOOM = 2.6f
    const val CAMERA_MAX_SPEED = 900f

    // Storm (battle royale zone)
    val STORM_RADII = floatArrayOf(1200f, 900f, 600f, 350f, 150f, 60f)
    val STORM_DPS = floatArrayOf(0f, 3f, 6f, 10f, 18f, 30f)
    val STORM_PHASE_START = floatArrayOf(0f, 20f, 45f, 75f, 105f, 150f)
    const val STORM_SHRINK_TIME = 5f

    // Shrines / sanctuaries
    const val SHRINE_COUNT = 3
    const val SHRINE_HEAL = 5f
    const val SHRINE_COOLDOWN = 15f
    const val SHRINE_RELOCATE = 40f
    const val SHRINE_RADIUS = 66f

    const val SANCTUARY_INTERVAL = 25f
    const val SANCTUARY_LIFE = 12f
    const val SANCTUARY_RADIUS = 80f
    const val SANCTUARY_COOLDOWN = 8f

    // Pickups / hazard tuning
    const val CHEST_INTERVAL = 12f
    const val CHEST_MAX = 3
    const val PICKUP_LIFE = 22f
    const val HEAL_AMOUNT = 0.35f

    const val LAVA_DPS = 5f
    const val CHILL_DPS = 2f
    const val GEYSER_INTERVAL = 8f
    const val GEYSER_DAMAGE = 20f
    const val GEYSER_TELEGRAPH = 1f
    const val LIGHTNING_INTERVAL = 6f
    const val LIGHTNING_DAMAGE = 35f
    const val LIGHTNING_TELEGRAPH = 1.5f
    const val CLONE_INTERVAL = 20f
    const val CLONE_HP_FRACTION = 0.6f
    const val CLONE_CRYSTAL_STALL = 15f

    // Anti camping
    const val COWARD_TRIGGER = 30f
    const val COWARD_DURATION = 15f
    const val COWARD_PENALTY = 0.20f
    const val BLOODLUST_TIME = 180f
    const val BLOODLUST_DAMAGE = 0.25f
    const val BLOODLUST_SPEED = 0.10f

    // Elite mode player buffs
    const val ELITE_HP_MULT = 2f
    const val ELITE_DAMAGE_MULT = 1.5f
    const val ELITE_REVIVES = 2
    const val ELITE_POWER_DROP = 1.3f
    const val ELITE_SHRINE_MULT = 2f

    // NPC aggro / spacing
    const val NPC_RETARGET = 0.4f
    const val SEPARATION_FORCE = 1f
    const val DRAKE_PREFERRED_RANGE = 180f
    const val SKYHAWK_ORBIT_RANGE = 150f

    // Level scaling
    const val NPC_HP_SCALE = 3.4f
    const val NPC_DAMAGE_SCALE = 0.34f
    const val BOSS_HP_SCALE = 1.3f

    /**
     * Player health grows with the rank they are fighting in, so late ranks
     * stay survivable without turning armor into the only progression axis.
     */
    fun playerMaxHp(rankIndex: Int, armorHpBonus: Int, elite: Boolean): Float {
        var hp = PLAYER_HP + armorHpBonus + rankIndex * 28f
        if (elite) hp *= ELITE_HP_MULT
        return hp
    }

    fun npcHpMultiplier(difficulty: Float, rankIndex: Int): Float =
        NPC_HP_SCALE * difficulty.pow(0.85f) * (1f + rankIndex * 0.12f)

    fun npcDamageMultiplier(difficulty: Float, rankIndex: Int): Float =
        NPC_DAMAGE_SCALE * (1f + (difficulty - 1f) * 0.28f) * (1f + rankIndex * 0.09f)

    fun npcSpeedMultiplier(rankIndex: Int): Float = 1f + rankIndex * 0.03f

    fun npcCountFor(rankIndex: Int, levelInRank: Int): Int =
        8 + (levelInRank / 5) + rankIndex * 2

    fun levelDifficulty(levelInRank: Int): Float = 1f + (levelInRank / 50f) * 2f

    // Rewards
    fun campaignBaseCoins(levelInRank: Int): Int = 40 + levelInRank * 8

    fun campaignCoins(spec: LevelSpec, grade: LevelGrade, firstClear: Boolean): Int {
        if (grade == LevelGrade.NONE) return 0
        val base = campaignBaseCoins(spec.levelInRank)
        val raw = base.toFloat() * grade.coinMult * spec.rank.coinMult
        return (raw * (if (firstClear) 1.5f else 1f)).toInt()
    }

    // Elite rewards
    fun eliteCoins(chimeraIndex: Int, firstClear: Boolean): Int {
        val base = 600 + chimeraIndex * 450
        return if (firstClear) (base * 1.5f).toInt() else base
    }

    // Bonus coins
    const val BONUS_FIRST_CLEAR = 300
    const val BONUS_NEW_BEST = 200
    const val BONUS_NO_DAMAGE = 100
    const val BONUS_SUB_60 = 75
    const val BONUS_DAILY_WIN = 100
    const val LOSS_COINS = 30
    const val MERGE_COINS = 5

    // Coin sinks
    const val SINK_FORGE_SPEEDUP = 200
    const val SINK_WEAPON_FRAGMENT = 500
    const val SINK_GRADE_SKIP = 3000

    // Helper
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    fun approach(current: Float, target: Float, delta: Float): Float =
        if (current < target) min(current + delta, target) else max(current - delta, target)
}
