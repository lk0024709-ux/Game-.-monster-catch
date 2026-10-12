package com.example.rann.sim

import com.example.rann.content.ChimeraSpec
import com.example.rann.model.ArmorRank
import com.example.rann.model.Balance
import com.example.rann.model.BattleKind
import com.example.rann.model.BattleState
import com.example.rann.model.BossSpec
import com.example.rann.model.BoosterType
import com.example.rann.model.CreatureType
import com.example.rann.model.LevelModifier
import com.example.rann.model.MapFeature
import com.example.rann.model.MapTheme
import com.example.rann.model.PowerUpType
import com.example.rann.model.WeaponTier
import com.example.rann.model.BattleStats

// ======================================================
// SIMULATION ENTITIES (pure Kotlin — no rendering, no Android)
// ======================================================

const val FACTION_PLAYER = 0
const val FACTION_BOSS = 1
const val FACTION_NEUTRAL = -1

/** Everything the battle needs to know before the first frame. */
data class BattleSetup(
    val kind: BattleKind,
    val worldSize: Float,
    val map: MapTheme,
    val mapIndex: Int,
    val levelGlobal: Int,
    val levelLabel: String,
    val difficulty: Float,
    val modifiers: List<LevelModifier>,
    val creatureMix: List<CreatureType>,
    val npcCount: Int,
    val bossSpec: BossSpec?,
    val chimeraSpec: ChimeraSpec?,
    val isMiniBossLevel: Boolean,
    val weapon: WeaponTier,
    val armor: ArmorRank,
    val booster: BoosterType? = null,
    val elitePlayerBuffs: Boolean,
    val seed: Long,
    val dailyDoubleCoins: Boolean = false,
) {
    val isElite: Boolean get() = kind == BattleKind.ELITE
    fun has(mod: LevelModifier): Boolean = modifiers.contains(mod)
    /** Rank index 0..6 derived from the campaign level (elite uses max rank scaling). */
    val rankIndex: Int
        get() = if (isElite) 6 else ((levelGlobal - 1) / 50).coerceIn(0, 6)
}

/** Live control input, written by the UI layer every frame. */
class BattleInput {
    var moveX = 0f
    var moveY = 0f
    var attack = false
}

// ------------------------------------------------------
// FIGHTERS
// ------------------------------------------------------
open class Fighter(
    var x: Float,
    var y: Float,
    var radius: Float,
    var maxHp: Float,
    var faction: Int,
) {
    var vx = 0f
    var vy = 0f
    var knockX = 0f
    var knockY = 0f
    var hp = maxHp
    var facing = 0f
    var alive = true
    var attackCd = 0f
    var hitFlash = 0f
    var swingAnim = 0f
    var swingDir = 0f

    // status
    var poisonStacks = 0
    var poisonTimer = 0f
    var stun = 0f
    var slowTimer = 0f
    var slowFactor = 0f
    var shieldHp = 0f
    var dmgBuffTimer = 0f
    var dmgBuffAmount = 0f
    var speedBuffTimer = 0f
    var speedBuffAmount = 0f
    var vampTimer = 0f
    var invuln = 0f
    var shrineCd = 0f
    var sanctuaryCd = 0f
    var inSanctuary = false
    var onIce = false
    var inHeat = false

    val hpFraction: Float get() = if (maxHp <= 0f) 0f else (hp / maxHp).coerceIn(0f, 1f)

    fun damageMultiplier(): Float = 1f + dmgBuffAmount

    fun speedMultiplier(): Float = (1f + speedBuffAmount) * (1f - slowFactor)
}

class PlayerFighter(x: Float, y: Float, maxHp: Float) :
    Fighter(x, y, Balance.PLAYER_RADIUS, maxHp, FACTION_PLAYER) {
    var weapon: WeaponTier = WeaponTier.SWORD
    var armor: ArmorRank = ArmorRank.CLOTH
    var revives = 0
    var armorEffectScale = 1f
    var vision: Float = 1f
    var attackBuffer = 0f
    var coinsCollected = 0
    var damageTakenBuff = 0f // extra damage reduction (iron skin etc.)
    var swings = 0
}

class NpcFighter(
    x: Float,
    y: Float,
    val type: CreatureType,
    hp: Float,
    var speed: Float,
    var damage: Float,
    radius: Float,
    faction: Int,
) : Fighter(x, y, radius, hp, faction) {

    var elite = false
    var isClone = false
    var isMinion = false
    var label: String = type.label
    var attackInterval: Float = type.attackInterval
    var reach: Float = type.reach

    var target: Fighter? = null
    var retargetTimer: Float = 0f
    var specialTimer: Float = 0f
    var state: Int = 0 // 0 chase, 1 telegraph, 2 dash/charge, 3 recover, 4 orbit
    var stateTimer: Float = 0f
    var didContactDamage = false

    // boss extras
    var boss: BossSpec? = null
    var bossPhase = 1
    var traitTimer = 0f
    var summonTimer = 15f
    var chainTimer = 9f
    var cloneSpawnTimer = 20f
    var auraRadius = 0f
    var auraDps = 0f
    var decoyLife = 0f
    var summonDrop = true
}

class ChimeraFighter(
    x: Float,
    y: Float,
    val spec: ChimeraSpec,
) : Fighter(x, y, (18f * spec.type.sizeMult).coerceAtMost(95f), spec.hp, FACTION_BOSS) {

    var speed = 165f
    var damage = 34f * spec.type.damageMult
    var phase = 1
    var castAbility: com.example.rann.model.ChimeraAbility? = null
    var castTimer = 0f
    var castTargetX = 0f
    var castTargetY = 0f
    var abilityCd = 2.6f
    var lastAbility: com.example.rann.model.ChimeraAbility? = null
    var shiftArmed = true
    var enraged = false
    var retreatTimer = 0f
    var shieldTimer = 0f
    var minionTimer = 20f
    var mirrorTimer = 18f
    var dashTimer = 0f
    val cooldowns = HashMap<com.example.rann.model.ChimeraAbility, Float>()

    val ironHide: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.IRON_HIDE)
    val regen: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.REGEN)
    val energyShield: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.ENERGY_SHIELD)
    val dodge: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.DODGE_INSTINCT)
    val reflect: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.DAMAGE_REFLECT)
    val phaseShift: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.PHASE_SHIFT)
    val summonMinions: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.SUMMON_MINIONS)
    val mirrorImage: Boolean get() = spec.defenses.contains(com.example.rann.model.ChimeraDefense.MIRROR_IMAGE)
}

class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var damage: Float,
    var radius: Float,
    val faction: Int,
    var life: Float,
    val kind: Int,
    var target: Fighter? = null,
    var turnRate: Float = 0f,
    val color: Long = 0xFFC2410C,
) {
    var dead = false
}

class FloatText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Long,
    var life: Float = 0.95f,
    val big: Boolean = false,
) {
    var dead = false
}

/** One-shot visual effects. kind: 0 slash, 1 ring, 2 burst, 3 bolt, 4 flash, 5 merge-ish spark. */
class Fx(
    var kind: Int,
    var x: Float,
    var y: Float,
    var x2: Float,
    var y2: Float,
    var life: Float,
    val maxLife: Float,
    val radius: Float,
    val angle: Float,
    val color: Long,
    val arc: Float,
) {
    var dead = false
}

class Zone(
    var x: Float,
    var y: Float,
    var radius: Float,
    val kind: Int, // 0 lava, 1 ice, 2 heat, 3 treasure shrine
) {
    var baseRadius: Float = radius
}

class Geyser(var x: Float, var y: Float, var state: Int = 0, var stateTimer: Float = 0f)

class MovingWall(
    var x: Float,
    var y: Float,
    val w: Float,
    val h: Float,
    val horizontal: Boolean,
    val trackFrom: Float,
    val trackTo: Float,
    val speed: Float,
) {
    var dir = 1f
}

class Crystal(var x: Float, var y: Float, var hp: Float, var alive: Boolean = true)

class Shrine(var x: Float, var y: Float, var relocateTimer: Float)

class Sanctuary(var x: Float, var y: Float, var life: Float)

class Chest(var x: Float, var y: Float, var life: Float)

class Pickup(var x: Float, var y: Float, val type: PowerUpType, var life: Float)

class Strike(
    var x: Float,
    var y: Float,
    var telegraph: Float,
    var resolved: Boolean = false,
    val damage: Float = Balance.LIGHTNING_DAMAGE,
    val radius: Float = 90f,
)

class StormState(
    var phase: Int = 0,
    var radius: Float,
    var fromRadius: Float,
    var toRadius: Float,
    var shrinkT: Float = 0f,
    var cx: Float,
    var cy: Float,
    var phaseTimer: Float = 0f,
    var dps: Float = 0f,
    var nextPhaseAt: Float = Balance.STORM_PHASE_START[1],
)

class WindState(var angle: Float, var timer: Float, val strength: Float)
