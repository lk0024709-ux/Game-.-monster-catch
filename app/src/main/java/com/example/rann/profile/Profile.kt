package com.example.rann.profile

import com.example.rann.content.ChimeraFactory
import com.example.rann.content.Content
import com.example.rann.forge.ForgeRules
import com.example.rann.forge.ForgeState
import com.example.rann.model.ArmorRank
import com.example.rann.model.BoosterType
import com.example.rann.model.LevelGrade
import com.example.rann.model.Progression
import com.example.rann.model.Rank
import com.example.rann.model.WeaponTier

// ======================================================
// PLAYER PROFILE — everything that persists
// ======================================================

data class ProfileSettings(
    var sound: Boolean = true,
    var haptics: Boolean = true,
    var screenShake: Boolean = true,
    var showFloatingText: Boolean = true,
)

data class ProfileStats(
    var matchesPlayed: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var totalKills: Int = 0,
    var totalMerges: Int = 0,
    var coinsEarned: Int = 0,
    var eliteWins: Int = 0,
    var flawlessClears: Int = 0,
    var bestTimeSec: Float = 0f,
    var damageDealt: Float = 0f,
)

class PlayerProfile {

    var version = 1

    // ---- currency ----
    var coins: Int = 250

    // ---- forge ----
    val forge = ForgeState()

    // ---- gear ----
    var equippedWeaponTier: Int = WeaponTier.SWORD.tier
    val ownedArmors: MutableSet<Int> = mutableSetOf(ArmorRank.CLOTH.ordinal)
    var equippedArmor: Int = ArmorRank.CLOTH.ordinal
    var pendingBooster: Int? = null

    // ---- campaign ----
    val grades: MutableMap<Int, String> = mutableMapOf()
    val eliteCleared: MutableSet<Int> = mutableSetOf()

    // ---- identity ----
    val titles: MutableSet<String> = mutableSetOf("Novice")
    var equippedTitle: String = "Novice"
    val achievements: MutableSet<Int> = mutableSetOf()
    var rannslayer: Boolean = false

    // ---- daily / streaks ----
    var lastDailyWinDay: Long = -1L
    var winStreak: Int = 0
    var bestStreak: Int = 0

    // ---- misc ----
    val stats = ProfileStats()
    val settings = ProfileSettings()
    var lastPlayedMillis: Long = 0L

    // ==================================================
    // HELPERS
    // ==================================================
    val earnedCoinMultiplier: Float get() = if (rannslayer) 1.5f else 1f

    fun addCoins(amount: Int) {
        if (amount == 0) return
        coins = (coins + amount).coerceAtLeast(0)
        if (amount > 0) stats.coinsEarned += amount
    }

    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0 || coins < amount) return false
        coins -= amount
        return true
    }

    fun equippedArmorRank(): ArmorRank = ArmorRank.values()[equippedArmor.coerceIn(0, ArmorRank.values().size - 1)]

    fun equippedWeapon(): WeaponTier = WeaponTier.of(equippedWeaponTier)

    fun bestWeaponTier(): Int = maxOf(forge.highestTier() ?: 0, equippedWeaponTier.coerceAtLeast(WeaponTier.DAGGER.tier))

    // ---- armor shop ----
    fun ownsArmor(rank: ArmorRank): Boolean = ownedArmors.contains(rank.ordinal)

    fun buyArmor(rank: ArmorRank): Boolean {
        if (ownsArmor(rank)) return false
        if (!spendCoins(rank.price)) return false
        ownedArmors.add(rank.ordinal)
        return true
    }

    fun equipArmor(rank: ArmorRank) {
        if (ownsArmor(rank)) equippedArmor = rank.ordinal
    }

    // ---- boosters ----
    fun buyBooster(type: BoosterType): Boolean {
        if (!spendCoins(type.price)) return false
        pendingBooster = type.ordinal
        return true
    }

    fun armedBooster(): BoosterType? = pendingBooster?.let { BoosterType.values().getOrNull(it) }

    fun consumeBooster() {
        pendingBooster = null
    }

    // ---- campaign results ----
    fun gradeFor(globalLevel: Int): LevelGrade = LevelGrade.fromLetter(grades[globalLevel])

    fun recordClear(globalLevel: Int, grade: LevelGrade) {
        if (grade == LevelGrade.NONE) return
        val best = gradeFor(globalLevel)
        if (grade.strength > best.strength) grades[globalLevel] = grade.letter
    }

    fun recordEliteClear(index: Int) {
        eliteCleared.add(index)
    }

    fun wonCount(): Int = Progression.clearedCount(grades)

    fun highestCleared(): Int = Progression.highestCleared(grades)

    fun isRankUnlocked(rank: Rank): Boolean = Progression.isRankUnlocked(grades, rank.index)

    fun isLevelUnlocked(globalLevel: Int): Boolean = Progression.isLevelUnlocked(grades, globalLevel)

    fun isEliteUnlocked(): Boolean = Progression.isEliteUnlocked(grades)

    /** Elite trials unlock one by one: the first is free once Bronze falls. */
    fun isChimeraUnlocked(index: Int): Boolean {
        if (!isEliteUnlocked()) return false
        if (index == 0) return true
        return eliteCleared.contains(index - 1) || eliteCleared.containsAll((0 until index).toList())
    }

    fun unlockTitle(title: String) {
        if (titles.add(title)) equippedTitle = title
    }

    fun grantAchievement(id: Int): Boolean {
        if (!achievements.add(id)) return false
        val def = Content.ACHIEVEMENTS.firstOrNull { it.id == id } ?: return true
        addCoins(def.rewardCoins)
        return true
    }

    fun isSameDay(other: Long): Boolean {
        val a = lastDailyWinDay
        return a > 0 && other - a < 24L * 60L * 60L * 1000L && other / 86_400_000L == a / 86_400_000L
    }

    fun dailyFirstWinAvailable(nowMillis: Long): Boolean = !isSameDay(nowMillis)

    fun claimDailyWin(nowMillis: Long) {
        lastDailyWinDay = nowMillis
    }

    // ---- forge convenience ----
    fun forgeIntervalSeconds(): Float = forge.intervalSeconds

    fun buyForgeSpeedUp(): Boolean {
        if (forge.isFull()) return false
        if (!spendCoins(com.example.rann.model.Balance.SINK_FORGE_SPEEDUP)) return false
        forge.spawnDagger()
        forge.markTick(System.currentTimeMillis())
        return true
    }

    fun buyWeaponFragment(): Boolean {
        if (forge.isFull()) return false
        if (!spendCoins(com.example.rann.model.Balance.SINK_WEAPON_FRAGMENT)) return false
        val tier = (1..minOf(WeaponTier.maxTier(), maxOf(2, forge.merges / 8 + 1))).random()
        forge.addWeapon(tier)
        return true
    }

    fun buyGradeSkip(): Int? {
        if (!spendCoins(com.example.rann.model.Balance.SINK_GRADE_SKIP)) return null
        val target = nextUnclearedLevel() ?: return null
        grades[target] = LevelGrade.D.letter
        return target
    }

    fun nextUnclearedLevel(): Int? {
        for (l in 1..350) {
            if (gradeFor(l) == LevelGrade.NONE && isLevelUnlocked(l)) return l
        }
        return null
    }

    fun forgeSpeedupProgress(): Float = 1f - (forge.merges % ForgeRules.MERGES_PER_SPEEDUP) / ForgeRules.MERGES_PER_SPEEDUP.toFloat()

    fun eliteRecommendedTier(index: Int): Int = ChimeraFactory.of(index).recommendedTier
}

// ======================================================
// SERIALIZATION — tiny line based codec, robust to junk.
// ======================================================
object ProfileCodec {

    private const val MAGIC = "rannbhoomi-profile"

    fun encode(p: PlayerProfile): String {
        val sb = StringBuilder()
        sb.append("magic=").append(MAGIC).append('\n')
        sb.append("v=").append(p.version).append('\n')
        sb.append("coins=").append(p.coins).append('\n')
        sb.append("forgeSlots=").append(p.forge.toSlotCsv()).append('\n')
        sb.append("forgeMerges=").append(p.forge.merges).append('\n')
        sb.append("forgeTick=").append(p.forge.lastTickMillis).append('\n')
        sb.append("weapon=").append(p.equippedWeaponTier).append('\n')
        sb.append("armor=").append(p.equippedArmor).append('\n')
        sb.append("armorsOwned=").append(p.ownedArmors.joinToString(",")).append('\n')
        sb.append("booster=").append(p.pendingBooster ?: -1).append('\n')
        sb.append("grades=").append(
            p.grades.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
        ).append('\n')
        sb.append("elite=").append(p.eliteCleared.sorted().joinToString(",")).append('\n')
        sb.append("titles=").append(p.titles.joinToString("|")).append('\n')
        sb.append("title=").append(p.equippedTitle).append('\n')
        sb.append("achievements=").append(p.achievements.sorted().joinToString(",")).append('\n')
        sb.append("rannslayer=").append(if (p.rannslayer) 1 else 0).append('\n')
        sb.append("daily=").append(p.lastDailyWinDay).append('\n')
        sb.append("streak=").append(p.winStreak).append('\n')
        sb.append("bestStreak=").append(p.bestStreak).append('\n')
        sb.append("lastPlayed=").append(p.lastPlayedMillis).append('\n')
        sb.append("st.matches=").append(p.stats.matchesPlayed).append('\n')
        sb.append("st.wins=").append(p.stats.wins).append('\n')
        sb.append("st.losses=").append(p.stats.losses).append('\n')
        sb.append("st.kills=").append(p.stats.totalKills).append('\n')
        sb.append("st.merges=").append(p.stats.totalMerges).append('\n')
        sb.append("st.coins=").append(p.stats.coinsEarned).append('\n')
        sb.append("st.elite=").append(p.stats.eliteWins).append('\n')
        sb.append("st.flawless=").append(p.stats.flawlessClears).append('\n')
        sb.append("st.bestTime=").append(p.stats.bestTimeSec).append('\n')
        sb.append("st.damage=").append(p.stats.damageDealt).append('\n')
        sb.append("se.sound=").append(if (p.settings.sound) 1 else 0).append('\n')
        sb.append("se.haptics=").append(if (p.settings.haptics) 1 else 0).append('\n')
        sb.append("se.shake=").append(if (p.settings.screenShake) 1 else 0).append('\n')
        sb.append("se.floats=").append(if (p.settings.showFloatingText) 1 else 0).append('\n')
        return sb.toString()
    }

    fun decode(raw: String?): PlayerProfile {
        val p = PlayerProfile()
        if (raw.isNullOrBlank()) return p
        return try {
            val map = HashMap<String, String>()
            for (line in raw.split('\n')) {
                val idx = line.indexOf('=')
                if (idx <= 0) continue
                map[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
            if (map["magic"] != MAGIC) return p
            p.version = map["v"]?.toIntOrNull() ?: 1
            p.coins = (map["coins"]?.toIntOrNull() ?: 250).coerceAtLeast(0)

            val slots = map["forgeSlots"] ?: ""
            val merges = map["forgeMerges"]?.toIntOrNull() ?: 0
            val tick = map["forgeTick"]?.toLongOrNull() ?: 0L
            val restored = ForgeState.fromSlotCsv(slots, merges, tick)
            for (i in restored.slots.indices) p.forge.slots[i] = restored.slots[i]
            p.forge.merges = restored.merges
            p.forge.lastTickMillis = restored.lastTickMillis

            p.equippedWeaponTier = (map["weapon"]?.toIntOrNull() ?: WeaponTier.SWORD.tier)
                .coerceIn(1, WeaponTier.maxTier())
            p.equippedArmor = (map["armor"]?.toIntOrNull() ?: ArmorRank.CLOTH.ordinal)
                .coerceIn(0, ArmorRank.values().size - 1)
            (map["armorsOwned"] ?: "").split(",").forEach { tok ->
                tok.trim().toIntOrNull()?.let { if (it in 0 until ArmorRank.values().size) p.ownedArmors.add(it) }
            }
            p.ownedArmors.add(ArmorRank.CLOTH.ordinal)
            val booster = map["booster"]?.toIntOrNull() ?: -1
            p.pendingBooster = if (booster in BoosterType.values().indices) booster else null

            (map["grades"] ?: "").split(",").forEach { tok ->
                val parts = tok.split(":")
                if (parts.size == 2) {
                    val level = parts[0].trim().toIntOrNull()
                    val grade = LevelGrade.fromLetter(parts[1].trim())
                    if (level != null && level in 1..350 && grade != LevelGrade.NONE) {
                        p.grades[level] = grade.letter
                    }
                }
            }
            (map["elite"] ?: "").split(",").forEach { tok ->
                tok.trim().toIntOrNull()?.let { if (it in 0 until ChimeraFactory.ALL.size) p.eliteCleared.add(it) }
            }
            p.titles.clear()
            (map["titles"] ?: "Novice").split("|").forEach { t ->
                if (t.isNotBlank()) p.titles.add(t.trim())
            }
            if (p.titles.isEmpty()) p.titles.add("Novice")
            p.equippedTitle = map["title"]?.takeIf { p.titles.contains(it) } ?: p.titles.first()
            (map["achievements"] ?: "").split(",").forEach { tok ->
                tok.trim().toIntOrNull()?.let { p.achievements.add(it) }
            }
            p.rannslayer = (map["rannslayer"]?.toIntOrNull() ?: 0) == 1
            p.lastDailyWinDay = map["daily"]?.toLongOrNull() ?: -1L
            p.winStreak = map["streak"]?.toIntOrNull() ?: 0
            p.bestStreak = map["bestStreak"]?.toIntOrNull() ?: 0
            p.lastPlayedMillis = map["lastPlayed"]?.toLongOrNull() ?: 0L
            p.stats.matchesPlayed = map["st.matches"]?.toIntOrNull() ?: 0
            p.stats.wins = map["st.wins"]?.toIntOrNull() ?: 0
            p.stats.losses = map["st.losses"]?.toIntOrNull() ?: 0
            p.stats.totalKills = map["st.kills"]?.toIntOrNull() ?: 0
            p.stats.totalMerges = map["st.merges"]?.toIntOrNull() ?: 0
            p.stats.coinsEarned = map["st.coins"]?.toIntOrNull() ?: 0
            p.stats.eliteWins = map["st.elite"]?.toIntOrNull() ?: 0
            p.stats.flawlessClears = map["st.flawless"]?.toIntOrNull() ?: 0
            p.stats.bestTimeSec = map["st.bestTime"]?.toFloatOrNull() ?: 0f
            p.stats.damageDealt = map["st.damage"]?.toFloatOrNull() ?: 0f
            p.settings.sound = (map["se.sound"]?.toIntOrNull() ?: 1) == 1
            p.settings.haptics = (map["se.haptics"]?.toIntOrNull() ?: 1) == 1
            p.settings.screenShake = (map["se.shake"]?.toIntOrNull() ?: 1) == 1
            p.settings.showFloatingText = (map["se.floats"]?.toIntOrNull() ?: 1) == 1
            p
        } catch (t: Throwable) {
            PlayerProfile()
        }
    }
}
