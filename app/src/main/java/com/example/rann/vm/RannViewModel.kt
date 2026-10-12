package com.example.rann.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rann.audio.Sound
import com.example.rann.content.ChimeraFactory
import com.example.rann.content.Content
import com.example.rann.content.LevelFactory
import com.example.rann.forge.DropKind
import com.example.rann.forge.DropOutcome
import com.example.rann.model.ArmorRank
import com.example.rann.model.Balance
import com.example.rann.model.BattleKind
import com.example.rann.model.BonusKind
import com.example.rann.model.BoosterType
import com.example.rann.model.ChimeraType
import com.example.rann.model.CoinBonus
import com.example.rann.model.GradeResult
import com.example.rann.model.Grader
import com.example.rann.model.LevelGrade
import com.example.rann.model.Maps
import com.example.rann.model.Rank
import com.example.rann.model.WeaponTier
import com.example.rann.profile.PlayerProfile
import com.example.rann.profile.ProfileStore
import com.example.rann.sim.BattleSetup
import com.example.rann.sim.BattleSim
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen { SPLASH, HOME, LEVELS, ELITE, STORE, SETTINGS, BATTLE }

data class BattleRequest(
    val setup: BattleSetup,
    val title: String,
    val subtitle: String,
    val mapName: String,
    val twistText: String,
    val isElite: Boolean,
    val eliteIndex: Int,
    val levelGlobal: Int,
    val showcase: List<Pair<String, String>>,
)

class RannViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ProfileStore(app)
    val profile: PlayerProfile = store.load()

    private val rev = mutableIntStateOf(0)

    /** Read this anywhere in the UI to stay in sync with profile mutations. */
    val revision: Int get() = rev.intValue

    var screen by mutableStateOf(AppScreen.SPLASH)

    /** 0 = Campaign, 1 = Forge (both live on the Levels screen). */
    var levelsTab by mutableIntStateOf(0)
    var battle by mutableStateOf<BattleRequest?>(null)
    var lastResult by mutableStateOf<GradeResult?>(null)
    var lastBattleOutcome by mutableStateOf<GradeResult?>(null)
    var offlineForged by mutableIntStateOf(0)
    var forgeNowCooldown by mutableStateOf(0f)
    var toasts by mutableStateOf<List<String>>(emptyList())

    private var lastSave = 0L

    init {
        val forged = profile.forge.simulateOffline(System.currentTimeMillis())
        if (forged > 0) offlineForged = forged
        Sound.enabled = profile.settings.sound
        Sound.hapticsEnabled = profile.settings.haptics
        touch(save = true)
        viewModelScope.launch {
            while (isActive) {
                delay(200)
                tickForge()
            }
        }
    }

    // ==================================================
    // FORGE
    // ==================================================
    private fun tickForge() {
        val now = System.currentTimeMillis()
        if (forgeNowCooldown > 0f) forgeNowCooldown = (forgeNowCooldown - 0.2f).coerceAtLeast(0f)
        val intervalMs = (profile.forge.intervalSeconds * 1000f).toLong()
        if (now - profile.forge.lastTickMillis >= intervalMs) {
            profile.forge.lastTickMillis = now
            if (!profile.forge.isFull()) {
                profile.forge.spawnDagger()
                Sound.tap()
                touch()
            }
        }
        if (now - lastSave > 5000L) {
            lastSave = now
            store.save(profile)
        }
    }

    fun forgeProgress(): Float = profile.forge.progressToNext(System.currentTimeMillis())

    fun forgeNow() {
        if (forgeNowCooldown > 0f) return
        if (profile.forge.isFull()) {
            notify("Grid is full — merge something first")
            return
        }
        profile.forge.spawnDagger()
        profile.forge.markTick(System.currentTimeMillis())
        forgeNowCooldown = 10f
        Sound.tap()
        touch(save = true)
    }

    fun dropWeapon(from: Int, to: Int): DropOutcome {
        if (from == to) return DropOutcome(DropKind.REFUSE)
        val before = profile.forge.slots.getOrNull(from)
        val outcome = profile.forge.applyDrop(from, to)
        when (outcome.kind) {
            DropKind.MERGE -> {
                profile.addCoins(Balance.MERGE_COINS)
                profile.stats.totalMerges++
                Sound.merge()
                Sound.hapticMerge()
                if (outcome.newTier > profile.equippedWeaponTier) {
                    profile.equippedWeaponTier = outcome.newTier
                    val tier = WeaponTier.of(outcome.newTier)
                    notify("${tier.icon} ${tier.displayName} forged and equipped!")
                } else {
                    notify("Merged into ${WeaponTier.of(outcome.newTier).displayName} (+${Balance.MERGE_COINS} 🪙)")
                }
                checkAchievements(null, null, null, null)
                touch(save = true)
            }
            DropKind.MOVE, DropKind.SWAP -> {
                Sound.tap()
                touch(save = true)
            }
            DropKind.REFUSE -> {
                if (before == WeaponTier.maxTier().tier) notify("Two Mythic Blades cannot merge further")
                Sound.play(com.example.rann.sim.Sfx.HURT)
            }
        }
        return outcome
    }

    fun equipWeaponTier(tier: Int) {
        if (profile.forge.slots.contains(tier)) {
            profile.equippedWeaponTier = tier
            Sound.ui()
            touch(save = true)
        }
    }

    fun forgeSpeedUp() {
        if (profile.buyForgeSpeedUp()) {
            Sound.coin()
            notify("A dagger clatters into the grid")
            touch(save = true)
        } else {
            notify(if (profile.forge.isFull()) "Grid is full" else "Not enough coins")
        }
    }

    fun buyWeaponFragment() {
        if (profile.buyWeaponFragment()) {
            Sound.coin()
            notify("Weapon fragment reforged")
            checkAchievements(null, null, null, null)
            touch(save = true)
        } else {
            notify(if (profile.forge.isFull()) "Grid is full" else "Not enough coins")
        }
    }

    fun buyGradeSkip() {
        val level = profile.buyGradeSkip()
        if (level != null) {
            Sound.coin()
            notify("Level $level marked as cleared (D)")
            touch(save = true)
        } else {
            notify("Not enough coins or nothing left to skip")
        }
    }

    // ==================================================
    // STORE
    // ==================================================
    fun buyArmor(rank: ArmorRank) {
        if (profile.ownsArmor(rank)) {
            profile.equipArmor(rank)
            Sound.ui()
            notify("${rank.displayName} equipped")
        } else if (profile.buyArmor(rank)) {
            profile.equipArmor(rank)
            Sound.coin()
            notify("${rank.displayName} purchased")
        } else {
            notify("Not enough coins")
            Sound.play(com.example.rann.sim.Sfx.HURT)
        }
        touch(save = true)
    }

    fun buyBooster(type: BoosterType) {
        if (profile.buyBooster(type)) {
            Sound.coin()
            notify("${type.displayName} armed for the next battle")
        } else {
            notify("Not enough coins")
        }
        touch(save = true)
    }

    fun clearBooster() {
        profile.consumeBooster()
        notify("Booster discard")
        touch(save = true)
    }

    // ==================================================
    // BATTLE START
    // ==================================================
    fun startLevel(level: Int) {
        val spec = LevelFactory.spec(level)
        val booster = profile.armedBooster()
        val setup = BattleSetup(
            kind = BattleKind.CAMPAIGN,
            worldSize = Balance.WORLD_CAMPAIGN,
            map = spec.map,
            mapIndex = spec.mapIndex,
            levelGlobal = level,
            levelLabel = spec.displayName(),
            difficulty = spec.difficulty,
            modifiers = spec.modifiers,
            creatureMix = spec.creatureMix,
            npcCount = spec.npcCount,
            bossSpec = spec.bossSpec,
            chimeraSpec = null,
            isMiniBossLevel = spec.isMiniBoss,
            weapon = profile.equippedWeapon(),
            armor = profile.equippedArmorRank(),
            booster = booster,
            elitePlayerBuffs = false,
            seed = System.currentTimeMillis() xor (level * 7919L),
        )
        profile.consumeBooster()
        battle = BattleRequest(
            setup = setup,
            title = if (spec.isBoss) "${spec.bossSpec?.icon ?: "👑"} ${spec.bossSpec?.name ?: "Rank Boss"}" else spec.displayName(),
            subtitle = spec.summary(),
            mapName = "${spec.map.icon} ${spec.map.name}",
            twistText = spec.map.twistText,
            isElite = false,
            eliteIndex = -1,
            levelGlobal = level,
            showcase = spec.creatureMix.map { it.label to it.icon },
        )
        lastResult = null
        screen = AppScreen.BATTLE
        touch(save = true)
    }

    fun startElite(index: Int) {
        val spec = ChimeraFactory.of(index)
        val setup = BattleSetup(
            kind = BattleKind.ELITE,
            worldSize = Balance.WORLD_ELITE,
            map = Maps.eliteArena(index),
            mapIndex = index,
            levelGlobal = 350,
            levelLabel = "Elite: ${spec.displayName}",
            difficulty = 1f + index * 0.25f,
            modifiers = emptyList(),
            creatureMix = emptyList(),
            npcCount = 0,
            bossSpec = null,
            chimeraSpec = spec,
            isMiniBossLevel = false,
            weapon = profile.equippedWeapon(),
            armor = profile.equippedArmorRank(),
            booster = null,
            elitePlayerBuffs = true,
            seed = System.currentTimeMillis() xor (index * 104729L),
        )
        battle = BattleRequest(
            setup = setup,
            title = "${spec.emoji} ${spec.displayName}",
            subtitle = "Chimera Trial ${index + 1} of ${ChimeraFactory.ALL.size}",
            mapName = spec.arenaName,
            twistText = "Pure 1v1 — no twists, no helpers, no boosters.",
            isElite = true,
            eliteIndex = index,
            levelGlobal = -1,
            showcase = emptyList(),
        )
        lastResult = null
        screen = AppScreen.BATTLE
        touch(save = true)
    }

    fun retryBattle() {
        val req = battle ?: return
        if (req.isElite) startElite(req.eliteIndex) else startLevel(req.levelGlobal)
    }

    fun nextLevel() {
        val req = battle ?: return
        if (req.isElite) {
            val next = req.eliteIndex + 1
            if (next < ChimeraFactory.ALL.size && profile.isChimeraUnlocked(next)) {
                startElite(next)
            } else {
                screen = AppScreen.ELITE
            }
        } else {
            val next = req.levelGlobal + 1
            if (next <= Balance.TOTAL_LEVELS && profile.isLevelUnlocked(next)) {
                startLevel(next)
            } else {
                screen = AppScreen.LEVELS
            }
        }
    }

    // ==================================================
    // BATTLE END
    // ==================================================
    fun completeBattle(sim: BattleSim): GradeResult {
        val req = battle ?: return GradeResult(LevelGrade.NONE, emptyList(), 0, emptyList(), 0, false, false)
        val now = System.currentTimeMillis()
        val won = sim.state == com.example.rann.model.BattleState.WON
        val dailyFirstWin = won && profile.dailyFirstWinAvailable(now)

        profile.stats.matchesPlayed++
        profile.stats.totalKills += sim.stats.kills
        profile.stats.damageDealt += sim.stats.damageDealt
        if (won) {
            profile.stats.wins++
            profile.winStreak++
            profile.bestStreak = maxOf(profile.bestStreak, profile.winStreak)
        } else {
            profile.stats.losses++
            profile.winStreak = 0
        }

        var unlockedTitle: String? = null
        var result: GradeResult

        if (req.isElite) {
            val spec = ChimeraFactory.of(req.eliteIndex)
            val alreadyCleared = profile.eliteCleared.contains(req.eliteIndex)
            if (won && !alreadyCleared) {
                profile.recordEliteClear(req.eliteIndex)
                profile.stats.eliteWins++
                unlockedTitle = spec.title
                unlockedTitle?.let { profile.unlockTitle(it) }
            }
            result = Grader.evaluate(
                won = won,
                spec = null,
                chimeraIndex = req.eliteIndex,
                stats = sim.stats,
                hpFraction = sim.player.hpFraction,
                bestPrevious = if (alreadyCleared) LevelGrade.D else LevelGrade.NONE,
                dailyFirstWin = false,
                unlockedTitle = unlockedTitle,
            )
        } else {
            val level = req.levelGlobal
            val spec = LevelFactory.spec(level)
            val bestPrevious = profile.gradeFor(level)
            val bossTitle = if (won && spec.isBoss) spec.bossSpec?.title else null
            if (bossTitle != null) profile.unlockTitle(bossTitle)
            if (won && level == Balance.TOTAL_LEVELS) profile.rannslayer = true
            result = Grader.evaluate(
                won = won,
                spec = spec,
                chimeraIndex = null,
                stats = sim.stats,
                hpFraction = sim.player.hpFraction,
                bestPrevious = bestPrevious,
                dailyFirstWin = dailyFirstWin,
                unlockedTitle = bossTitle,
                nextLevelGlobal = if (level < Balance.TOTAL_LEVELS) level + 1 else null,
            )
            if (won) {
                profile.recordClear(level, result.grade)
                if (result.grade == LevelGrade.SS) profile.stats.flawlessClears++
            }
        }

        if (dailyFirstWin) profile.claimDailyWin(now)

        // in-battle loot (chests + kills)
        val loot = sim.matchCoins
        val multiplier = profile.earnedCoinMultiplier
        val totalCoins = ((result.totalCoins) * multiplier).toInt() + loot
        val bonuses = if (loot > 0) {
            result.bonuses + CoinBonus(BonusKind.CHESTS, "Chests & kills", loot)
        } else {
            result.bonuses
        }
        result = result.copy(
            bonuses = bonuses,
            totalCoins = totalCoins,
            titleUnlocked = unlockedTitle,
        )
        profile.addCoins(totalCoins)
        if (won && result.timeSec > 0f) {
            profile.stats.bestTimeSec = if (profile.stats.bestTimeSec == 0f) result.timeSec
            else minOf(profile.stats.bestTimeSec, result.timeSec)
        }

        checkAchievements(won, req.levelGlobal, req.eliteIndex, sim)

        lastResult = result
        lastBattleOutcome = result
        if (won) {
            Sound.win()
            Sound.hapticWin()
        } else {
            Sound.lose()
            Sound.hapticLose()
        }
        touch(save = true)
        return result
    }

    fun clearResult() {
        lastResult = null
    }

    // ==================================================
    // ACHIEVEMENTS / TITLES / SETTINGS
    // ==================================================
    private fun checkAchievements(won: Boolean?, level: Int?, eliteIndex: Int?, sim: BattleSim?) {
        fun grant(id: Int) {
            if (profile.grantAchievement(id)) {
                val def = Content.ACHIEVEMENTS.firstOrNull { it.id == id }
                if (def != null) notify("🏅 ${def.name} — +${def.rewardCoins} 🪙")
            }
        }
        if (profile.wonCount() >= 1) grant(0)
        if (profile.stats.totalMerges >= 10) grant(1)
        if (profile.forge.highestTier() == WeaponTier.maxTier().tier) grant(2)
        if (won == true && level != null) {
            val stats = sim?.stats
            if (stats != null) {
                if (stats.elapsed < 50f) grant(3)
                if (stats.noDamage) grant(4)
            }
        }
        if (Rank.values().any { profile.isRankBossClearedFor(it) }) grant(5)
        if (profile.stats.coinsEarned >= 10_000) grant(6)
        if (profile.stats.eliteWins >= 1) grant(7)
        if (profile.isRankUnlocked(Rank.PLATINUM)) grant(8)
        if (profile.rannslayer) grant(9)
    }

    private fun PlayerProfile.isRankBossClearedFor(rank: Rank): Boolean =
        com.example.rann.model.Progression.isRankBossCleared(grades, rank.index)

    fun setSound(on: Boolean) {
        profile.settings.sound = on
        Sound.enabled = on
        touch(save = true)
    }

    fun setHaptics(on: Boolean) {
        profile.settings.haptics = on
        Sound.hapticsEnabled = on
        touch(save = true)
    }

    fun setScreenShake(on: Boolean) {
        profile.settings.screenShake = on
        touch(save = true)
    }

    fun setFloatingText(on: Boolean) {
        profile.settings.showFloatingText = on
        touch(save = true)
    }

    fun equipTitle(title: String) {
        if (profile.titles.contains(title)) {
            profile.equippedTitle = title
            Sound.ui()
            touch(save = true)
        }
    }

    fun resetProgress() {
        store.clear()
        val fresh = PlayerProfile()
        profile.coins = fresh.coins
        profile.grades.clear()
        profile.eliteCleared.clear()
        profile.titles.clear()
        profile.titles.add("Novice")
        profile.equippedTitle = "Novice"
        profile.achievements.clear()
        profile.ownedArmors.clear()
        profile.ownedArmors.add(ArmorRank.CLOTH.ordinal)
        profile.equippedArmor = ArmorRank.CLOTH.ordinal
        profile.equippedWeaponTier = WeaponTier.SWORD.tier
        profile.pendingBooster = null
        profile.forge.slots.fill(null)
        profile.forge.merges = 0
        profile.forge.lastTickMillis = System.currentTimeMillis()
        profile.rannslayer = false
        profile.winStreak = 0
        profile.bestStreak = 0
        profile.stats.matchesPlayed = 0
        profile.stats.wins = 0
        profile.stats.losses = 0
        profile.stats.totalKills = 0
        profile.stats.totalMerges = 0
        profile.stats.coinsEarned = 0
        profile.stats.eliteWins = 0
        profile.stats.flawlessClears = 0
        profile.stats.bestTimeSec = 0f
        profile.stats.damageDealt = 0f
        profile.lastDailyWinDay = -1L
        notify("Progress reset")
        touch(save = true)
    }

    // ==================================================
    // NAVIGATION HELPERS
    // ==================================================
    fun notify(message: String) {
        toasts = (toasts + message).takeLast(3)
    }

    fun consumeToast(): String? {
        val first = toasts.firstOrNull() ?: return null
        toasts = toasts.drop(1)
        return first
    }

    fun consumeOfflineNotice(): Int {
        val n = offlineForged
        offlineForged = 0
        return n
    }

    private fun touch(save: Boolean = false) {
        rev.intValue = rev.intValue + 1
        if (save) {
            lastSave = System.currentTimeMillis()
            store.save(profile)
        }
    }

    override fun onCleared() {
        store.save(profile)
        super.onCleared()
    }

    // ---- small helpers used by screens ----
    fun nextLevelToPlay(): Int = com.example.rann.model.Progression.nextPlayableLevel(profile.grades)

    fun progressPercent(): Float = com.example.rann.model.Progression.progressPercent(profile.grades)

    fun eliteUnlockIndexes(): List<Boolean> = ChimeraFactory.ALL.map { profile.isChimeraUnlocked(it.index) }

    fun chimeraTypeFor(index: Int): ChimeraType = ChimeraFactory.of(index).type
}
