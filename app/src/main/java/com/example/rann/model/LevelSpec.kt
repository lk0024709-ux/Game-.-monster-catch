package com.example.rann.model

import kotlin.math.max

// ======================================================
// LEVEL SPECS, BOSSES, GRADING & PROGRESSION
// ======================================================

/** One of the 350 campaign levels. */
data class LevelSpec(
    val globalLevel: Int,
    val rankIndex: Int,
    val levelInRank: Int, // 1..50
    val mapIndex: Int,
    val creatureMix: List<CreatureType>,
    val npcCount: Int,
    val modifiers: List<LevelModifier>,
    val isBoss: Boolean,
    val bossSpec: BossSpec?,
    val difficulty: Float,
    val phaseLabel: String,
) {
    val rank: Rank get() = Rank.of(rankIndex)
    val map: MapTheme get() = Maps.of(mapIndex)
    val isMiniBoss: Boolean get() = !isBoss && levelInRank % 10 == 0

    fun displayName(): String = "${rank.displayName} $levelInRank"

    fun previewCoins(): Int = Balance.campaignCoins(this, LevelGrade.D, false)

    fun summary(): String = when {
        isBoss -> "Rank Boss — ${bossSpec?.name ?: "Unknown"}"
        isMiniBoss -> "Mini-boss — ${creatureMix.joinToString(" · ") { it.label }}"
        else -> creatureMix.joinToString(" · ") { it.label }
    }
}

/** Handcrafted rank boss. */
data class BossSpec(
    val index: Int, // 0..6
    val name: String,
    val icon: String,
    val hp: Float,
    val size: Float,
    val damageMult: Float,
    val speedMult: Float,
    val creature: CreatureType,
    val traits: List<BossTrait>,
    val adds: List<CreatureType>,
    val rewardCoins: Int,
    val unlockText: String,
    val title: String?,
    val phases: Int = 1,
)

enum class BossTrait(val label: String) {
    CLONE_SPAWN("Spawns fragile clones"),
    CHAIN_LIGHTNING("Chain lightning volley"),
    SUMMONS("Summons skyhawks every 15s"),
    SPLIT_PHASES("Three escalating phases"),
    TWIN("Fights as a twin"),
    LAVA_AURA("Seething lava aura"),
}

// ------------------------------------------------------
// RESULT GRADING
// ------------------------------------------------------
data class BattleStats(
    var elapsed: Float = 0f,
    var damageTaken: Float = 0f,
    var damageDealt: Float = 0f,
    var kills: Int = 0,
    var pickups: Int = 0,
    var revivesUsed: Int = 0,
    var hitsLanded: Int = 0,
    var chestsOpened: Int = 0,
) {
    val noDamage: Boolean get() = damageTaken <= 0.01f
}

data class SubScore(
    val icon: String,
    val label: String,
    val valueText: String,
    val grade: LevelGrade,
)

enum class BonusKind(val icon: String) {
    FIRST_CLEAR("🎉"),
    NEW_BEST("📈"),
    NO_DAMAGE("🎯"),
    SPEEDY("⏱️"),
    DAILY("📅"),
    LOSS("🛡️"),
    BOSS_TITLE("👑"),
    CHESTS("🎁"),
}

data class CoinBonus(val kind: BonusKind, val label: String, val amount: Int)

data class GradeResult(
    val grade: LevelGrade,
    val subScores: List<SubScore>,
    val baseCoins: Int,
    val bonuses: List<CoinBonus>,
    val totalCoins: Int,
    val firstClear: Boolean,
    val newBest: Boolean,
    val titleUnlocked: String? = null,
    val timeSec: Float = 0f,
    val levelLabel: String = "",
    val won: Boolean = true,
    val nextLevelGlobal: Int? = null,
)

object Grader {

    private fun timeGrade(t: Float): LevelGrade = when {
        t < 50f -> LevelGrade.SS
        t < 70f -> LevelGrade.S
        t < 90f -> LevelGrade.A
        t < 120f -> LevelGrade.B
        t < 150f -> LevelGrade.C
        else -> LevelGrade.D
    }

    private fun hpGrade(hpFraction: Float): LevelGrade = when {
        hpFraction >= 0.999f -> LevelGrade.SS
        hpFraction >= 0.80f -> LevelGrade.S
        hpFraction >= 0.60f -> LevelGrade.A
        hpFraction >= 0.40f -> LevelGrade.B
        hpFraction >= 0.20f -> LevelGrade.C
        else -> LevelGrade.D
    }

    private fun damageGrade(taken: Float): LevelGrade = when {
        taken <= 0.01f -> LevelGrade.SS
        taken < 15f -> LevelGrade.S
        taken < 30f -> LevelGrade.A
        taken < 60f -> LevelGrade.B
        taken < 100f -> LevelGrade.C
        else -> LevelGrade.D
    }

    fun subScores(stats: BattleStats, hpFraction: Float): List<SubScore> = listOf(
        SubScore("⏱️", "Time", "${stats.elapsed.toInt()}s", timeGrade(stats.elapsed)),
        SubScore("❤️", "Health", "${(hpFraction * 100).toInt()}%", hpGrade(hpFraction)),
        SubScore("💥", "Damage taken", stats.damageTaken.toInt().toString(), damageGrade(stats.damageTaken)),
    )

    fun finalGrade(subs: List<SubScore>): LevelGrade =
        if (subs.isEmpty()) LevelGrade.NONE else LevelGrade.lowest(*subs.map { it.grade }.toTypedArray())

    /**
     * Full result of a finished battle.
     * @param bestPrevious previous best grade of this level (NONE if never cleared)
     * @param dailyFirstWin true when this is the first win of the day (already consumed flag applied by caller)
     */
    fun evaluate(
        won: Boolean,
        spec: LevelSpec?,
        chimeraIndex: Int?,
        stats: BattleStats,
        hpFraction: Float,
        bestPrevious: LevelGrade,
        dailyFirstWin: Boolean,
        unlockedTitle: String? = null,
        nextLevelGlobal: Int? = null,
        extraCoins: Int = 0,
    ): GradeResult {
        val label = spec?.displayName() ?: "Elite Trial"
        if (!won) {
            return GradeResult(
                grade = LevelGrade.NONE,
                subScores = subScores(stats, hpFraction),
                baseCoins = 0,
                bonuses = listOf(CoinBonus(BonusKind.LOSS, "Consolation", Balance.LOSS_COINS)),
                totalCoins = Balance.LOSS_COINS + extraCoins,
                firstClear = false,
                newBest = false,
                timeSec = stats.elapsed,
                levelLabel = label,
                won = false,
            )
        }

        val subs = subScores(stats, hpFraction)
        val grade = finalGrade(subs)
        val firstClear = bestPrevious == LevelGrade.NONE
        val newBest = !firstClear && grade.strength > bestPrevious.strength

        val baseCoins = if (spec != null) {
            Balance.campaignCoins(spec, grade, firstClear)
        } else {
            Balance.eliteCoins(chimeraIndex ?: 0, firstClear)
        }

        val bonuses = mutableListOf<CoinBonus>()
        if (firstClear) bonuses.add(CoinBonus(BonusKind.FIRST_CLEAR, "First clear", Balance.BONUS_FIRST_CLEAR))
        if (newBest && !firstClear) bonuses.add(CoinBonus(BonusKind.NEW_BEST, "New best grade", Balance.BONUS_NEW_BEST))
        if (stats.noDamage) bonuses.add(CoinBonus(BonusKind.NO_DAMAGE, "Untouched", Balance.BONUS_NO_DAMAGE))
        if (stats.elapsed < 60f) bonuses.add(CoinBonus(BonusKind.SPEEDY, "Blitz (sub-60s)", Balance.BONUS_SUB_60))
        if (dailyFirstWin) bonuses.add(CoinBonus(BonusKind.DAILY, "Daily first win", Balance.BONUS_DAILY_WIN))
        if (unlockedTitle != null) bonuses.add(CoinBonus(BonusKind.BOSS_TITLE, "Title: $unlockedTitle", 0))

        val bonusTotal = bonuses.sumOf { it.amount }
        return GradeResult(
            grade = grade,
            subScores = subs,
            baseCoins = baseCoins,
            bonuses = bonuses,
            totalCoins = baseCoins + bonusTotal + extraCoins,
            firstClear = firstClear,
            newBest = newBest,
            titleUnlocked = unlockedTitle,
            timeSec = stats.elapsed,
            levelLabel = label,
            won = true,
            nextLevelGlobal = nextLevelGlobal,
        )
    }
}

// ------------------------------------------------------
// PROGRESSION / UNLOCKS
// ------------------------------------------------------
object Progression {

    /** Grades of cleared campaign levels keyed by global level. */
    fun clearedCount(grades: Map<Int, String>): Int = grades.count { (_, g) -> g != LevelGrade.NONE.letter }

    fun highestCleared(grades: Map<Int, String>): Int =
        grades.filterValues { it != LevelGrade.NONE.letter }.keys.maxOrNull() ?: 0

    fun bestGrade(grades: Map<Int, String>, globalLevel: Int): LevelGrade =
        LevelGrade.fromLetter(grades[globalLevel])

    fun rankClearedCount(grades: Map<Int, String>, rankIndex: Int): Int {
        val from = rankIndex * 50 + 1
        val to = from + 49
        return (from..to).count { LevelGrade.fromLetter(grades[it]) != LevelGrade.NONE }
    }

    fun isRankBossCleared(grades: Map<Int, String>, rankIndex: Int): Boolean =
        LevelGrade.fromLetter(grades[(rankIndex + 1) * 50]) != LevelGrade.NONE

    /** 7 ranks unlocked by total wins, plus the previous rank boss must fall. */
    fun isRankUnlocked(grades: Map<Int, String>, rankIndex: Int): Boolean {
        if (rankIndex == 0) return true
        val wins = clearedCount(grades)
        val byWins = wins >= Rank.of(rankIndex).unlockWins
        return byWins && isRankBossCleared(grades, rankIndex - 1)
    }

    /**
     * Levels unlock progressively: any cleared level unlocks the next one,
     * and the player always has the next 3 challengeable levels available.
     */
    fun isLevelUnlocked(grades: Map<Int, String>, globalLevel: Int): Boolean {
        if (globalLevel <= 1) return true
        if (!isRankUnlocked(grades, (globalLevel - 1) / 50)) return false
        val highest = highestCleared(grades)
        return globalLevel <= max(highest + 3, 3)
    }

    fun isEliteUnlocked(grades: Map<Int, String>): Boolean = isRankBossCleared(grades, 0)

    fun nextPlayableLevel(grades: Map<Int, String>): Int {
        val highest = highestCleared(grades)
        return (highest + 1).coerceIn(1, Balance.TOTAL_LEVELS)
    }

    fun progressPercent(grades: Map<Int, String>): Float =
        clearedCount(grades).toFloat() / Balance.TOTAL_LEVELS.toFloat()
}
