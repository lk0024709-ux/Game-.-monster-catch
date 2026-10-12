package com.example.rann.sim

import com.example.rann.content.LevelRandom
import com.example.rann.model.ArmorRank
import com.example.rann.model.Balance
import com.example.rann.model.BattleKind
import com.example.rann.model.BattleState
import com.example.rann.model.CreatureType
import com.example.rann.model.LevelModifier
import com.example.rann.model.MapFeature
import com.example.rann.model.PowerUpType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ======================================================
// BATTLE SIMULATION
// Top-down free-for-all: the player vs wild creatures that
// also tear each other apart. Deterministic per setup seed.
// ======================================================
object Sfx {
    const val SLASH = 0
    const val HIT = 1
    const val HURT = 2
    const val DEATH = 3
    const val PICKUP = 4
    const val CHEST = 5
    const val LIGHTNING = 6
    const val GEYSER = 7
    const val SHIELD = 8
    const val REVIVE = 9
    const val WIN = 10
    const val LOSE = 11
    const val CLONE = 12
    const val POWER = 13
}

class BattleSim(val setup: BattleSetup) {

    // ---------------- world ----------------
    val rng = LevelRandom(setup.seed)
    val world = setup.worldSize
    val map = setup.map
    val mods = setup.modifiers
    val rankIndex: Int = ((setup.levelGlobal - 1) / 50).coerceIn(0, 6)
    val staticFeatures: Set<MapFeature> = setup.map.features.toSet()

    // ---------------- actors ----------------
    val player: PlayerFighter
    val npcs = ArrayList<NpcFighter>()
    var chimera: ChimeraFighter? = null
    val projectiles = ArrayList<Projectile>()
    val floaters = ArrayList<FloatText>()
    val fx = ArrayList<Fx>()

    // ---------------- features ----------------
    val zones = ArrayList<Zone>()
    val geysers = ArrayList<Geyser>()
    val walls = ArrayList<MovingWall>()
    val crystals = ArrayList<Crystal>()
    val shrines = ArrayList<Shrine>()
    val sanctuaries = ArrayList<Sanctuary>()
    val chests = ArrayList<Chest>()
    val pickups = ArrayList<Pickup>()
    val strikes = ArrayList<Strike>()

    // ---------------- zone / weather ----------------
    val storm: StormState
    var wind: WindState? = null
    var activeCycleFeature: MapFeature? = null

    // ---------------- timers ----------------
    var elapsed = 0f
    var state = BattleState.RUNNING
    var outcomeTimer = 0f
    var matchCoins = 0
    var failReason: String? = null

    internal var chestTimer = 0f
    internal var sanctuaryTimer = 8f
    internal var geyserTimer = 4f
    internal var lightningTimer = 3f
    internal var cloneTimer = Balance.CLONE_INTERVAL
    internal var cloneBlockTimer = 0f
    internal var fogTimer = 0f
    internal var fogActive = false
    internal var fogDuration = 0f
    internal var twistTimer = 0f
    internal var twistIndex = 0
    internal var cowardTimer = 0f
    internal var noActivityTimer = 0f
    internal var bloodlust = false
    internal var bossAbilityFx = 0f
    internal var lastSwingCount = 0
    internal val treasureTimers = HashMap<Int, Float>()

    var playerOutsideZone = false
    internal var playerLastHurt = -999f
    val stats = com.example.rann.model.BattleStats()
    val soundEvents = ArrayList<Int>()
    var screenShake = 0f
    var bannerText: String? = null
    var bannerTimer = 0f

    init {
        val elite = setup.isElite
        var maxHp = Balance.playerMaxHp(rankIndexForScaling(), setup.armor.hpBonus, elite)
        if (mods.contains(LevelModifier.IRONMAN)) maxHp = 1f
        player = PlayerFighter(world * 0.5f, world * 0.5f, maxHp).also { p ->
            p.weapon = setup.weapon
            p.armor = setup.armor
            p.revives = setup.armor.revives + (if (elite) Balance.ELITE_REVIVES else 0) +
                (if (setup.booster == com.example.rann.model.BoosterType.REVIVE_POTION) 1 else 0)
            if (setup.booster == com.example.rann.model.BoosterType.IRON_SKIN) p.damageTakenBuff = 0.20f
            p.armorEffectScale = if (mods.contains(LevelModifier.IRON_SKINLESS)) 0.5f else 1f
            p.vision = if (mods.contains(LevelModifier.DARKNESS)) 0.7f else 1f
            p.invuln = Balance.PLAYER_SPAWN_GRACE
        }
        val startRadius = if (elite) 4000f else Balance.STORM_RADII[0]
        storm = StormState(
            phase = 0,
            radius = startRadius,
            fromRadius = startRadius,
            toRadius = startRadius,
            cx = world * 0.5f,
            cy = world * 0.5f,
            dps = 0f,
            nextPhaseAt = if (elite) Float.MAX_VALUE else Balance.STORM_PHASE_START[1],
        )
        if (staticFeatures.contains(MapFeature.WIND_PUSH)) {
            wind = WindState(angle = rng.range(0f, TWO_PI), timer = 10f, strength = 20f)
        }
        buildHazards()
        spawnEnemies()
        buildShrines()
        if (elite) spawnChimera()
        bannerText = when {
            elite -> setup.chimeraSpec?.displayName
            setup.bossSpec != null -> setup.bossSpec.name
            else -> "Rannbhoomi"
        }
        bannerTimer = 2.2f
    }

    /** Elite fights scale like Grand Masters. */
    private fun rankIndexForScaling(): Int = if (setup.isElite) 6 else rankIndex

    private fun buildHazards() {
        val pools = if (staticFeatures.contains(MapFeature.LAVA_POOLS)) 5 else 0
        repeat(pools) {
            zones.add(
                Zone(
                    x = rng.range(world * 0.18f, world * 0.82f),
                    y = rng.range(world * 0.18f, world * 0.82f),
                    radius = rng.range(52f, 92f),
                    kind = 0,
                )
            )
        }
        if (staticFeatures.contains(MapFeature.ICE_PATCHES)) {
            repeat(6) {
                zones.add(
                    Zone(
                        x = rng.range(world * 0.15f, world * 0.85f),
                        y = rng.range(world * 0.15f, world * 0.85f),
                        radius = rng.range(90f, 150f),
                        kind = 1,
                    )
                )
            }
        }
        if (staticFeatures.contains(MapFeature.HEAT_ZONES)) {
            repeat(3) {
                zones.add(
                    Zone(
                        x = rng.range(world * 0.2f, world * 0.8f),
                        y = rng.range(world * 0.2f, world * 0.8f),
                        radius = 110f,
                        kind = 2,
                    )
                )
            }
        }
        if (staticFeatures.contains(MapFeature.TREASURE_SHRINES)) {
            repeat(2) {
                zones.add(
                    Zone(
                        x = rng.range(world * 0.25f, world * 0.75f),
                        y = rng.range(world * 0.25f, world * 0.75f),
                        radius = 60f,
                        kind = 3,
                    )
                )
            }
        }
        if (staticFeatures.contains(MapFeature.MOVING_WALLS)) {
            repeat(2) { i ->
                val y = world * (0.35f + i * 0.3f)
                walls.add(
                    MovingWall(
                        x = world * 0.25f, y = y, w = 300f, h = 34f, horizontal = true,
                        trackFrom = world * 0.12f, trackTo = world * 0.88f, speed = 70f,
                    )
                )
                val x = world * (0.3f + i * 0.4f)
                walls.add(
                    MovingWall(
                        x = x, y = world * 0.3f, w = 34f, h = 300f, horizontal = false,
                        trackFrom = world * 0.12f, trackTo = world * 0.88f, speed = 60f,
                    )
                )
            }
        }
        if (staticFeatures.contains(MapFeature.DESTRUCTIBLE_CRYSTALS)) {
            repeat(3) { i ->
                val angle = i * (TWO_PI / 3f)
                crystals.add(
                    Crystal(
                        x = world * 0.5f + cos(angle) * world * 0.3f,
                        y = world * 0.5f + sin(angle) * world * 0.3f,
                        hp = 60f * (1f + rankIndex * 0.4f),
                    )
                )
            }
        }
    }

    private fun spawnEnemies() {
        val boss = setup.bossSpec
        val count = setup.npcCount
        if (count > 0) {
            for (i in 0 until count) {
                val type = when {
                    boss != null && i < boss.adds.size -> boss.adds[i]
                    setup.creatureMix.isNotEmpty() -> setup.creatureMix[i % setup.creatureMix.size]
                    else -> CreatureType.GRIFFIN
                }
                val angle = (i.toFloat() / count) * TWO_PI + rng.range(-0.25f, 0.25f)
                val dist = rng.range(world * 0.18f, world * 0.42f)
                val x = (world * 0.5f + cos(angle) * dist).coerceIn(60f, world - 60f)
                val y = (world * 0.5f + sin(angle) * dist).coerceIn(60f, world - 60f)

                val npcCountPenalty = if (mods.contains(LevelModifier.NPC_LEGION)) 1f else 1f
                var hp = type.baseHp * Balance.npcHpMultiplier(setup.difficulty, rankIndex) * npcCountPenalty
                var dmg = type.baseDamage * Balance.npcDamageMultiplier(setup.difficulty, rankIndex)
                var speed = type.baseSpeed * Balance.npcSpeedMultiplier(rankIndex)
                if (mods.contains(LevelModifier.NPC_TOUGH)) hp *= 1.25f
                if (mods.contains(LevelModifier.NPC_FIERCE)) dmg *= 1.25f
                if (mods.contains(LevelModifier.NPC_SWIFT)) speed *= 1.20f
                if (mods.contains(LevelModifier.MIRROR_MATCH)) dmg *= 1f + 0.09f * setup.weapon.tier

                val elite = mods.contains(LevelModifier.NPC_ELITE) ||
                    (setup.isMiniBossLevel && i == 0)
                if (elite) {
                    hp *= 1.4f
                    dmg *= 1.25f
                }
                val n = NpcFighter(
                    x = x, y = y, type = type, hp = hp,
                    speed = speed, damage = dmg,
                    radius = type.size * (if (elite) 1.15f else 1f),
                    faction = 2 + i,
                )
                n.elite = elite
                n.attackInterval = type.attackInterval * 1.12f
                if (elite) n.label = "Elite ${type.label}"
                npcs.add(n)
            }
        }

        if (boss != null) {
            val bossX = world * 0.5f
            val bossY = world * 0.35f
            val twin = boss.traits.contains(com.example.rann.model.BossTrait.TWIN)
            val copies = if (twin) 2 else 1
            for (c in 0 until copies) {
                val nb = NpcFighter(
                    x = bossX + c * 120f - 60f, y = bossY,
                    type = boss.creature,
                    hp = boss.hp * Balance.BOSS_HP_SCALE,
                    speed = boss.creature.baseSpeed * boss.speedMult,
                    damage = boss.creature.baseDamage * boss.damageMult *
                        Balance.npcDamageMultiplier(setup.difficulty, rankIndex) * 0.8f,
                    radius = (18f * boss.size).coerceAtMost(80f),
                    faction = FACTION_BOSS + c,
                )
                nb.boss = boss
                nb.label = boss.name
                nb.reach = boss.creature.reach * 1.35f
                nb.attackInterval = boss.creature.attackInterval * 0.9f
                nb.traitTimer = 8f
                if (boss.traits.contains(com.example.rann.model.BossTrait.LAVA_AURA)) {
                    nb.auraRadius = 140f
                    nb.auraDps = 4f
                }
                npcs.add(nb)
            }
        }

        if (setup.has(LevelModifier.BOSS_RUSH) && !setup.isElite && boss == null) {
            val type = setup.creatureMix.firstOrNull() ?: CreatureType.IRONBOAR
            val nb = NpcFighter(
                x = world * 0.5f, y = world * 0.22f, type = type,
                hp = type.baseHp * 4f * Balance.npcHpMultiplier(setup.difficulty, rankIndex),
                speed = type.baseSpeed * 1.1f,
                damage = type.baseDamage * 1.6f * Balance.npcDamageMultiplier(setup.difficulty, rankIndex),
                radius = type.size * 1.8f,
                faction = FACTION_BOSS,
            )
            nb.boss = com.example.rann.model.BossSpec(
                index = rankIndex, name = "Rann's Champion", icon = type.icon,
                hp = 1f, size = 1.8f, damageMult = 1.6f, speedMult = 1.1f,
                creature = type, traits = emptyList(), adds = emptyList(),
                rewardCoins = 400, unlockText = "", title = null,
            )
            nb.elite = true
            nb.label = "Rann's Champion"
            npcs.add(nb)
        }

        // Throne of Rann: neutral guardian until provoked
        if (staticFeatures.contains(MapFeature.ARENA_BOSS) && boss == null) {
            val avatar = NpcFighter(
                x = world * 0.5f, y = world * 0.5f, type = CreatureType.GRIFFIN,
                hp = 500f + rankIndex * 40f,
                speed = 175f, damage = 26f, radius = 54f,
                faction = FACTION_NEUTRAL,
            )
            avatar.label = "Rann's Avatar"
            avatar.boss = com.example.rann.model.BossSpec(
                index = 6, name = "Rann's Avatar", icon = "👑", hp = 500f, size = 3f,
                damageMult = 1.4f, speedMult = 1.15f, creature = CreatureType.GRIFFIN,
                traits = listOf(com.example.rann.model.BossTrait.SUMMONS),
                adds = emptyList(), rewardCoins = 0, unlockText = "", title = null,
            )
            avatar.traitTimer = 12f
            npcs.add(avatar)
        }
    }

    private fun buildShrines() {
        for (i in 0 until Balance.SHRINE_COUNT) {
            val angle = i * (TWO_PI / Balance.SHRINE_COUNT) + rng.range(-0.4f, 0.4f)
            val dist = world * 0.28f
            shrines.add(
                Shrine(
                    x = (world * 0.5f + cos(angle) * dist).coerceIn(120f, world - 120f),
                    y = (world * 0.5f + sin(angle) * dist).coerceIn(120f, world - 120f),
                    relocateTimer = Balance.SHRINE_RELOCATE,
                )
            )
        }
    }

    private fun spawnChimera() {
        val spec = setup.chimeraSpec ?: return
        val c = ChimeraFighter(world * 0.5f, world * 0.32f, spec)
        c.maxHp = spec.hp * spec.hpScale
        c.hp = c.maxHp
        c.speed = 165f
        c.damage = 26f * spec.type.damageMult
        c.shieldHp = if (c.energyShield) 200f else 0f
        chimera = c
    }

    // ==================================================
    // MAIN TICK
    // ==================================================
    fun update(dtRaw: Float, input: BattleInput) {
        val dt = dtRaw.coerceIn(0f, 0.033f)
        if (state == BattleState.WON || state == BattleState.LOST) {
            outcomeTimer += dt
            updateVisuals(dt)
            return
        }
        elapsed += dt
        stats.elapsed = elapsed
        if (bannerTimer > 0f) bannerTimer -= dt
        if (screenShake > 0f) screenShake = max(0f, screenShake - dt * 2.4f)

        if (mods.contains(LevelModifier.SPEEDRUN) && elapsed > 60f) {
            failReason = "Time up — SPEEDRUN demands a 60s clear"
            state = BattleState.LOST
            soundEvents.add(Sfx.LOSE)
            return
        }
        if (elapsed > Balance.BLOODLUST_TIME && !bloodlust) {
            bloodlust = true
            bannerText = "Creatures go into bloodlust"
            bannerTimer = 2.2f
        }

        updateStorm(dt)
        updateWind(dt)
        updateFeatures(dt)
        updatePlayer(dt, input)
        updateNpcs(dt)
        updateChimera(dt)
        updateProjectiles(dt)
        updateSanctuaries(dt)
        updateOverTime(dt)
        updateShrines(dt)
        updateChests(dt)
        updatePickups(dt)
        updateStrikes(dt)
        separate()
        updateVisuals(dt)
        checkOutcome()
    }

    private fun updateVisuals(dt: Float) {
        for (f in floaters) {
            f.life -= dt
            f.y -= 42f * dt
            if (f.life <= 0f) f.dead = true
        }
        floaters.removeAll { it.dead }
        for (e in fx) {
            e.life -= dt
            if (e.life <= 0f) e.dead = true
        }
        fx.removeAll { it.dead }
    }

    // ==================================================
    // STORM / WIND
    // ==================================================
    private fun stormSpeedFactor(): Float {
        var f = 1f / map.shrinkMult
        if (mods.contains(LevelModifier.FAST_STORM)) f *= 1.30f
        return f
    }

    private fun updateStorm(dt: Float) {
        if (setup.isElite) return
        val factor = stormSpeedFactor()
        storm.phaseTimer += dt * factor
        if (storm.shrinkT > 0f || storm.radius > storm.toRadius + 0.5f) {
            storm.shrinkT += dt * factor
            val dur = Balance.STORM_SHRINK_TIME
            val t = (storm.shrinkT / dur).coerceIn(0f, 1f)
            storm.radius = Balance.lerp(storm.fromRadius, storm.toRadius, t)
            if (t >= 1f) storm.shrinkT = 0f
        }
        if (storm.phase < Balance.STORM_RADII.size - 1 && storm.phaseTimer >= Balance.STORM_PHASE_START[storm.phase + 1]) {
            storm.phase++
            storm.fromRadius = storm.radius
            storm.toRadius = Balance.STORM_RADII[storm.phase]
            storm.shrinkT = 0.0001f
            storm.dps = Balance.STORM_DPS[storm.phase]
            if (storm.phase >= 3) {
                // random offset for the late phases, kept inside the new circle
                val offMax = (storm.radius - storm.toRadius).coerceAtLeast(60f) * 0.6f
                storm.cx = (storm.cx + rng.range(-offMax, offMax)).coerceIn(storm.toRadius * 0.4f, world - storm.toRadius * 0.4f)
                storm.cy = (storm.cy + rng.range(-offMax, offMax)).coerceIn(storm.toRadius * 0.4f, world - storm.toRadius * 0.4f)
            }
            storm.nextPhaseAt = if (storm.phase + 1 < Balance.STORM_PHASE_START.size) {
                Balance.STORM_PHASE_START[storm.phase + 1]
            } else {
                Float.MAX_VALUE
            }
            bannerText = when (storm.phase) {
                1 -> "The storm closes in"
                2 -> "Storm: heavy pressure"
                3 -> "Storm: lethal"
                4 -> "Storm: critical"
                else -> "Sudden death"
            }
            bannerTimer = 2.4f
        }
    }

    fun distanceToZoneEdge(x: Float, y: Float): Float =
        storm.radius - hypot(x - storm.cx, y - storm.cy)

    private fun updateWind(dt: Float) {
        val w = wind ?: return
        if (!hasFeature(MapFeature.WIND_PUSH)) return
        w.timer -= dt
        if (w.timer <= 0f) {
            w.timer = 10f
            w.angle += rng.range(-1.1f, 1.1f)
        }
        val px = cos(w.angle) * w.strength * dt
        val py = sin(w.angle) * w.strength * dt
        for (f in allFighters()) {
            if (!f.alive) continue
            f.x = (f.x + px).coerceIn(f.radius, world - f.radius)
            f.y = (f.y + py).coerceIn(f.radius, world - f.radius)
        }
    }

    // ==================================================
    // PLAYER
    // ==================================================
    private fun updatePlayer(dt: Float, input: BattleInput) {
        val p = player
        if (!p.alive) return
        tickStatuses(p, dt)

        // movement
        var speed = Balance.PLAYER_SPEED * (1f + p.armor.speedBonusPercent)
        speed *= p.speedMultiplier()
        speed *= if (fogActive) 0.85f else 1f
        if (mods.contains(LevelModifier.SLIPPERY) || p.onIce) speed *= 1f
        var ix = input.moveX
        var iy = input.moveY
        val mag = hypot(ix, iy)
        if (mag > 1f) {
            ix /= mag
            iy /= mag
        }
        var dx = ix * speed
        var dy = iy * speed
        if (p.stun > 0f) {
            dx = 0f
            dy = 0f
        }
        if (p.onIce || mods.contains(LevelModifier.SLIPPERY)) {
            p.vx = Balance.lerp(p.vx, dx, (dt * 2.2f).coerceIn(0f, 1f))
            p.vy = Balance.lerp(p.vy, dy, (dt * 2.2f).coerceIn(0f, 1f))
        } else {
            p.vx = dx
            p.vy = dy
        }
        p.x += (p.vx + p.knockX) * dt
        p.y += (p.vy + p.knockY) * dt
        decayKnock(p, dt)
        clampWorld(p)

        if (hypot(ix, iy) > 0.05f) p.facing = atan2(iy, ix)

        // regeneration: armor regen always, plus out-of-combat recovery
        if (!mods.contains(LevelModifier.NO_HEAL)) {
            var regen = p.armor.regenPerSec * p.armorEffectScale
            if (elapsed - playerLastHurt > Balance.IDLE_REGEN_DELAY) regen += Balance.IDLE_REGEN
            if (regen > 0f && p.hp < p.maxHp) p.hp = min(p.maxHp, p.hp + regen * dt)
        }

        // attacking
        if (input.attack) p.attackBuffer = Balance.ATTACK_BUFFER
        p.attackBuffer = max(0f, p.attackBuffer - dt)
        if (p.attackCd > 0f) p.attackCd -= dt
        if (p.attackBuffer > 0f && p.attackCd <= 0f && p.stun <= 0f && !p.inSanctuary) {
            p.attackBuffer = 0f
            playerAttack()
        }
    }

    private fun playerAttack() {
        val p = player
        val w = p.weapon
        var arc = PI.toFloat() * Balance.ATTACK_ARC_FRACTION * w.arcMult
        val reach = w.reach + p.radius
        // aim assist: snap to the closest hostile in a generous cone
        val assist = nearestHostile(p.x, p.y, reach * 1.9f)
        if (assist != null) {
            p.facing = atan2(assist.y - p.y, assist.x - p.x)
        }
        p.swingAnim = Balance.ATTACK_ANIM
        p.swingDir = p.facing
        p.swings++
        addFx(Fx(0, p.x, p.y, 0f, 0f, Balance.ATTACK_ANIM, Balance.ATTACK_ANIM, reach, p.facing, 0xFFF3D9A4, arc))
        soundEvents.add(Sfx.SLASH)

        val damageMult = (if (setup.isElite) Balance.ELITE_DAMAGE_MULT else 1f) *
            (1f + p.dmgBuffAmount) *
            (if (cowardTimer > 0f) 1f - Balance.COWARD_PENALTY else 1f)
        var cooldown = w.cooldown * (if (mods.contains(LevelModifier.SLOW_FORGE)) 1.3f else 1f)
        p.attackCd = cooldown

        val candidates = mutableListOf<Fighter>()
        for (n in npcs) {
            if (!n.alive || n.faction == FACTION_NEUTRAL || n.inSanctuary) continue
            val d = hypot(n.x - p.x, n.y - p.y)
            if (d > reach + n.radius) continue
            val a = atan2(n.y - p.y, n.x - p.x)
            if (abs(angleDelta(a, p.facing)) > arc / 2f) continue
            candidates.add(n)
        }
        chimera?.let { c ->
            if (c.alive && !c.inSanctuary) {
                val d = hypot(c.x - p.x, c.y - p.y)
                if (d <= reach + c.radius) {
                    val a = atan2(c.y - p.y, c.x - p.x)
                    if (abs(angleDelta(a, p.facing)) <= arc / 2f) candidates.add(c)
                }
            }
        }
        candidates.sortBy { hypot(it.x - p.x, it.y - p.y) }
        val hits = candidates.take(w.maxTargets)
        for (t in hits) {
            val dmg = w.damage * damageMult
            val dir = atan2(t.y - p.y, t.x - p.x)
            dealDamage(t, dmg, fromPlayer = true, source = p, knockPower = 90f, kx = cos(dir), ky = sin(dir))
            stats.hitsLanded++
            if (p.vampTimer > 0f) p.hp = min(p.maxHp, p.hp + 3f)
        }
        if (hits.isEmpty()) return

        // Storm Blade / Mythic: lightning burst
        if (w.aoeDamage > 0f) {
            val anchor = hits.first()
            val burstDamage = w.aoeDamage * damageMult
            for (f in allEnemies()) {
                if (f === anchor || !f.alive) continue
                if (hypot(f.x - anchor.x, f.y - anchor.y) <= 60f + f.radius) {
                    dealDamage(f, burstDamage, fromPlayer = true, source = p)
                }
            }
            addFx(Fx(2, anchor.x, anchor.y, 0f, 0f, 0.32f, 0.32f, 62f, 0f, 0xFF0891B2, 0f))
        }
        // Mythic double strike
        if (w.doubleHitChance > 0f && rng.chance(w.doubleHitChance)) {
            val t = hits.first()
            dealDamage(t, w.damage * damageMult * 0.7f, fromPlayer = true, source = p)
            addFloater(t.x, t.y - t.radius - 12f, "DOUBLE!", 0xFFD97706, big = true)
        }
    }

    // ==================================================
    // PROJECTILES
    // ==================================================
    private fun updateProjectiles(dt: Float) {
        for (pr in projectiles) {
            if (pr.life <= 0f) {
                pr.dead = true
                continue
            }
            if (pr.turnRate > 0f && pr.target != null && pr.target!!.alive) {
                val t = pr.target!!
                val desired = atan2(t.y - pr.y, t.x - pr.x)
                val cur = atan2(pr.vy, pr.vx)
                val na = cur + (angleDelta(desired, cur)).coerceIn(-pr.turnRate * dt, pr.turnRate * dt)
                val sp = hypot(pr.vx, pr.vy)
                pr.vx = cos(na) * sp
                pr.vy = sin(na) * sp
            }
            pr.x += pr.vx * dt
            pr.y += pr.vy * dt
            pr.life -= dt
            if (pr.x < 0f || pr.y < 0f || pr.x > world || pr.y > world) pr.dead = true
            if (pr.dead) continue
            for (f in allFighters()) {
                if (!f.alive || f.faction == pr.faction) continue
                if (f is PlayerFighter && pr.faction == FACTION_NEUTRAL) continue
                if (f.faction == FACTION_NEUTRAL && pr.faction != FACTION_PLAYER) continue
                if (hypot(f.x - pr.x, f.y - pr.y) <= f.radius + pr.radius) {
                    val fromPlayer = pr.faction == FACTION_PLAYER
                    dealDamage(f, pr.damage, fromPlayer = fromPlayer, source = null)
                    addFx(Fx(2, pr.x, pr.y, 0f, 0f, 0.26f, 0.26f, 34f, 0f, pr.color, 0f))
                    pr.dead = true
                    break
                }
            }
        }
        projectiles.removeAll { it.dead }
    }

    // ==================================================
    // DAMAGE / DEATH
    // ==================================================
    internal fun dealDamage(
        target: Fighter,
        amount: Float,
        fromPlayer: Boolean,
        source: Fighter? = null,
        knockPower: Float = 0f,
        kx: Float = 0f,
        ky: Float = 0f,
        isDot: Boolean = false,
        armorPiercing: Boolean = false,
    ) {
        if (!target.alive || amount <= 0f) return
        if (target.inSanctuary) return
        if (target.invuln > 0f && !armorPiercing) return

        // neutral guardian wakes up when provoked
        if (target is NpcFighter && target.faction == FACTION_NEUTRAL && fromPlayer) {
            target.faction = FACTION_BOSS
            addFloater(target.x, target.y - target.radius - 24f, "RANN'S WRATH", 0xFFD97706, big = true)
            bannerText = "Rann's Avatar awakens"
            bannerTimer = 2.2f
        }

        var dmg = amount

        if (target is ChimeraFighter) {
            if (!isDot && target.dodge && rng.chance(0.30f)) {
                addFloater(target.x, target.y - target.radius, "DODGE", 0xFF78716C)
                return
            }
            if (target.ironHide && !armorPiercing) dmg *= 0.60f
        }

        if (target === player && !armorPiercing) {
            val def = player.armor.defensePercent * player.armorEffectScale
            dmg *= (1f - def)
            dmg *= (1f - player.damageTakenBuff.coerceIn(0f, 0.8f))
        }

        if (target.shieldHp > 0f && !armorPiercing) {
            val absorb = min(target.shieldHp, dmg)
            target.shieldHp -= absorb
            dmg -= absorb
            addFloater(target.x, target.y - target.radius - 6f, "-${absorb.toInt()} ward", 0xFF0891B2)
        }

        if (dmg <= 0f) return

        target.hp -= dmg
        target.hitFlash = Balance.HIT_FLASH
        if (fromPlayer) stats.damageDealt += dmg
        if (target === player) {
            playerLastHurt = elapsed
            stats.damageTaken += dmg
            screenShake = min(1f, screenShake + dmg / 40f)
            soundEvents.add(Sfx.HURT)
        } else if (!isDot) {
            soundEvents.add(Sfx.HIT)
        }
        if (!isDot) {
            if (knockPower > 0f && target.stun <= 0f) {
                val power = knockPower * (if (mods.contains(LevelModifier.LOW_GRAVITY)) 1.5f else 1f)
                target.knockX += kx * power
                target.knockY += ky * power
            }
            addFloater(
                target.x + rng.range(-6f, 6f),
                target.y - target.radius - 8f,
                dmg.toInt().toString(),
                if (fromPlayer) 0xFF1C1917 else 0xFFB91C1C,
                big = dmg >= 40f,
            )
        }

        // reflect
        if (source != null && source.alive) {
            if (target === player && player.armor.reflectPercent > 0f && !isDot) {
                dealDamage(
                    source, dmg * player.armor.reflectPercent * player.armorEffectScale,
                    fromPlayer = true, source = player, isDot = true,
                )
            }
            if (target is ChimeraFighter && target.reflect && source === player && !isDot && rng.chance(0.35f)) {
                addFloater(player.x, player.y - player.radius - 20f, "REFLECT", 0xFF7C2D12)
                // thorn damage is capped so burst hits never one-shot the warrior
                dealDamage(player, min(dmg * 0.15f, 8f), fromPlayer = false, source = target, isDot = true)
            }
        }

        // activity tracking (anti-camping)
        noActivityTimer = 0f

        if (target.hp <= 0f) killFighter(target)
    }

    /** Environmental deaths bypass the poison bookkeeping above. */
    private fun killChill(f: Fighter) {
        if (f.alive && f.hp <= 0f) killFighter(f)
    }

    internal fun killFighter(f: Fighter) {
        if (!f.alive) return
        f.alive = false
        f.hp = 0f
        addFx(Fx(2, f.x, f.y, 0f, 0f, 0.45f, 0.45f, f.radius * 2.4f, 0f, 0xFF7C2D12, 0f))
        soundEvents.add(Sfx.DEATH)

        if (f === player) {
            if (player.revives > 0) {
                player.revives--
                stats.revivesUsed++
                player.alive = true
                player.hp = player.maxHp * 0.4f
                player.invuln = 2f
                addFx(Fx(1, player.x, player.y, 0f, 0f, 0.6f, 0.6f, 120f, 0f, 0xFFD97706, 0f))
                addFloater(player.x, player.y - 40f, "REVIVED", 0xFF15803D, big = true)
                soundEvents.add(Sfx.REVIVE)
                return
            }
            state = BattleState.LOST
            addFloater(player.x, player.y - 40f, "DEFEATED", 0xFFB91C1C, big = true)
            soundEvents.add(Sfx.LOSE)
            return
        }

        val hostile = f.faction != FACTION_NEUTRAL
        if (hostile) {
            stats.kills++
            matchCoins += 2 + rng.nextInt(8)
        }
        val n = f as? NpcFighter
        if (n != null && !n.isClone && !n.isMinion) {
            if (rng.chance(0.15f) && !mods.contains(LevelModifier.NO_POWERS)) {
                pickups.add(Pickup(f.x, f.y, randomPowerUp(), Balance.PICKUP_LIFE))
            }
            if (n.boss != null) {
                addFx(Fx(1, f.x, f.y, 0f, 0f, 0.9f, 0.9f, 220f, 0f, 0xFFD97706, 0f))
                screenShake = 1f
            }
        }
        if (f is ChimeraFighter) {
            addFx(Fx(1, f.x, f.y, 0f, 0f, 1.2f, 1.2f, 300f, 0f, 0xFFD97706, 0f))
            screenShake = 1f
        }
    }

    // ==================================================
    // OVER-TIME EFFECTS (poison, storm, hazards, auras)
    // ==================================================
    private fun updateOverTime(dt: Float) {
        for (f in allFighters()) {
            if (!f.alive || f.inSanctuary) continue
            // poison
            if (f.poisonTimer > 0f) {
                f.poisonTimer -= dt
                if (f.poisonStacks > 0) {
                    val dmg = 2f * f.poisonStacks * dt
                    f.hp -= dmg
                    if (f === player) stats.damageTaken += dmg
                    if (f.hp <= 0f) killFighter(f)
                }
                if (f.poisonTimer <= 0f) f.poisonStacks = 0
            }
            if (!f.alive) continue
            // zone damage
            f.onIce = false
            f.inHeat = false
            for (z in zones) {
                val inside = hypot(f.x - z.x, f.y - z.y) <= z.radius
                if (!inside) continue
                when (z.kind) {
                    1 -> f.onIce = true
                    2 -> f.inHeat = true
                    0 -> {
                        val lavaMult = if (mods.contains(LevelModifier.RISING_LAVA)) 1.4f else 1f
                        f.hp -= Balance.LAVA_DPS * lavaMult * dt
                        if (f === player) stats.damageTaken += Balance.LAVA_DPS * lavaMult * dt
                        if (f.hp <= 0f) killFighter(f)
                    }
                }
            }
            if (!f.alive) continue
            // chill outside heat zones (Frost Hollow)
            if (staticFeatures.contains(MapFeature.HEAT_ZONES) && !f.inHeat && !f.inSanctuary) {
                f.hp -= Balance.CHILL_DPS * dt
                if (f === player) stats.damageTaken += Balance.CHILL_DPS * dt
                if (f.hp <= 0f) killFighter(f)
                if (!f.alive) continue
            }
            // toxic haze modifier
            if (mods.contains(LevelModifier.VENOM_HAZE)) {
                f.hp -= 1f * dt
                if (f === player) stats.damageTaken += 1f * dt
                if (f.hp <= 0f) killChill(f)
                if (!f.alive) continue
            }
            // storm damage
            val dz = hypot(f.x - storm.cx, f.y - storm.cy)
            if (dz > storm.radius) {
                val dps = storm.dps
                if (dps > 0f) {
                    f.hp -= dps * dt
                    if (f === player) {
                        stats.damageTaken += dps * dt
                        screenShake = min(1f, screenShake + dt * 0.4f)
                    }
                    if (f.hp <= 0f) killFighter(f)
                    if (!f.alive) continue
                }
                if (f === player) playerOutsideZone = true
            } else if (f === player) {
                playerOutsideZone = false
            }
        }

        // boss aura damage (Emberlord etc.)
        for (n in npcs) {
            if (!n.alive || n.auraRadius <= 0f) continue
            for (other in allFighters()) {
                if (other === n || !other.alive) continue
                if (hypot(other.x - n.x, other.y - n.y) <= n.auraRadius + other.radius) {
                    dealDamage(other, n.auraDps * dt, fromPlayer = false, isDot = true)
                }
            }
        }

        // anti camping
        noActivityTimer += dt
        if (noActivityTimer > Balance.COWARD_TRIGGER && cowardTimer <= 0f) {
            cowardTimer = Balance.COWARD_DURATION
            addFloater(player.x, player.y - 40f, "COWARD!", 0xFF78716C, big = true)
            bannerText = "Coward debuff — you dealt no damage for 30s"
            bannerTimer = 2.4f
        }
        if (cowardTimer > 0f) cowardTimer -= dt
    }

    internal fun addFx(f: Fx) {
        if (fx.size > 240) fx.removeAt(0)
        fx.add(f)
    }

    internal fun addFloater(x: Float, y: Float, text: String, color: Long, big: Boolean = false) {
        if (floaters.size > 90) floaters.removeAt(0)
        floaters.add(FloatText(x, y, text, color, big = big))
    }

    internal fun tickStatuses(f: Fighter, dt: Float) {
        f.attackCd = max(0f, f.attackCd - dt)
        f.hitFlash = max(0f, f.hitFlash - dt)
        f.swingAnim = max(0f, f.swingAnim - dt)
        f.invuln = max(0f, f.invuln - dt)
        f.stun = max(0f, f.stun - dt)
        f.shrineCd = max(0f, f.shrineCd - dt)
        f.sanctuaryCd = max(0f, f.sanctuaryCd - dt)
        if (f.slowTimer > 0f) {
            f.slowTimer -= dt
            if (f.slowTimer <= 0f) f.slowFactor = 0f
        }
        if (f.dmgBuffTimer > 0f) {
            f.dmgBuffTimer -= dt
            if (f.dmgBuffTimer <= 0f) f.dmgBuffAmount = 0f
        }
        if (f.speedBuffTimer > 0f) {
            f.speedBuffTimer -= dt
            if (f.speedBuffTimer <= 0f) f.speedBuffAmount = 0f
        }
        if (f.vampTimer > 0f) f.vampTimer -= dt
        decayKnock(f, dt)
    }

    private fun decayKnock(f: Fighter, dt: Float) {
        val decay = (1f - 7f * dt).coerceIn(0f, 1f)
        f.knockX *= decay
        f.knockY *= decay
    }

    // ==================================================
    // SHRINES / SANCTUARIES / CHESTS / PICKUPS / STRIKES
    // ==================================================
    private fun updateShrines(dt: Float) {
        val healPerSec = Balance.SHRINE_HEAL * (if (setup.isElite) Balance.ELITE_SHRINE_MULT else 1f)
        for (s in shrines) {
            s.relocateTimer -= dt
            if (s.relocateTimer <= 0f) {
                s.relocateTimer = Balance.SHRINE_RELOCATE
                val angle = rng.range(0f, TWO_PI)
                val dist = rng.range(0f, storm.radius * 0.55f)
                s.x = (storm.cx + cos(angle) * dist).coerceIn(80f, world - 80f)
                s.y = (storm.cy + sin(angle) * dist).coerceIn(80f, world - 80f)
            }
            for (f in allFighters()) {
                if (!f.alive || f.shrineCd > 0f) continue
                if (hypot(f.x - s.x, f.y - s.y) <= Balance.SHRINE_RADIUS * 0.6f) {
                    if (mods.contains(LevelModifier.NO_HEAL)) continue
                    f.hp = min(f.maxHp, f.hp + healPerSec * dt)
                    f.shrineCd = Balance.SHRINE_COOLDOWN
                    if (f === player) {
                        addFx(Fx(0, s.x, s.y, 0f, 0f, 0.3f, 0.3f, 40f, 0f, 0xFF15803D, 0f))
                    }
                }
            }
        }
    }

    private fun updateSanctuaries(dt: Float) {
        sanctuaryTimer -= dt
        if (sanctuaryTimer <= 0f) {
            sanctuaryTimer = Balance.SANCTUARY_INTERVAL
            if (sanctuaries.size < 2) {
                val angle = rng.range(0f, TWO_PI)
                val dist = rng.range(0f, storm.radius * 0.5f)
                sanctuaries.add(
                    Sanctuary(
                        x = (storm.cx + cos(angle) * dist).coerceIn(80f, world - 80f),
                        y = (storm.cy + sin(angle) * dist).coerceIn(80f, world - 80f),
                        life = Balance.SANCTUARY_LIFE,
                    )
                )
            }
        }
        for (s in sanctuaries) s.life -= dt
        sanctuaries.removeAll { it.life <= 0f }

        for (f in allFighters()) {
            f.inSanctuary = false
            if (!f.alive) continue
            for (s in sanctuaries) {
                if (hypot(f.x - s.x, f.y - s.y) <= Balance.SANCTUARY_RADIUS) {
                    f.inSanctuary = true
                    if (f === player && player.inSanctuary) player.invuln = max(player.invuln, 0.2f)
                }
            }
        }
    }

    private fun updateChests(dt: Float) {
        chestTimer -= dt
        val magnet = setup.booster == com.example.rann.model.BoosterType.CHEST_MAGNET
        if (chestTimer <= 0f) {
            chestTimer = Balance.CHEST_INTERVAL * (if (magnet) 0.5f else 1f)
            if (chests.size < Balance.CHEST_MAX) {
                val angle = rng.range(0f, TWO_PI)
                val dist = rng.range(0f, storm.radius * 0.7f)
                chests.add(
                    Chest(
                        x = (storm.cx + cos(angle) * dist).coerceIn(60f, world - 60f),
                        y = (storm.cy + sin(angle) * dist).coerceIn(60f, world - 60f),
                        life = 60f,
                    )
                )
            }
        }
        for (c in chests) {
            c.life -= dt
            if (hypot(player.x - c.x, player.y - c.y) <= player.radius + 16f && player.alive) {
                c.life = -1f
                val coins = 6 + rng.nextInt(20)
                matchCoins += coins
                stats.chestsOpened++
                addFloater(c.x, c.y - 14f, "+$coins 🪙", 0xFFD97706, big = true)
                if (!mods.contains(LevelModifier.NO_POWERS) && rng.chance(0.35f * (if (setup.isElite) Balance.ELITE_POWER_DROP else 1f))) {
                    pickups.add(Pickup(c.x, c.y + 18f, randomPowerUp(), Balance.PICKUP_LIFE))
                }
                soundEvents.add(Sfx.CHEST)
            }
        }
        chests.removeAll { it.life <= 0f }
    }

    private fun updatePickups(dt: Float) {
        for (p in pickups) {
            p.life -= dt
            if (!player.alive) continue
            if (hypot(player.x - p.x, player.y - p.y) <= player.radius + 20f) {
                p.life = -1f
                applyPower(p.type)
                stats.pickups++
                soundEvents.add(Sfx.POWER)
            }
        }
        pickups.removeAll { it.life <= 0f }
    }

    private fun updateStrikes(dt: Float) {
        for (s in strikes) {
            if (s.resolved) continue
            s.telegraph -= dt
            if (s.telegraph <= 0f) {
                s.resolved = true
                addFx(Fx(3, s.x, s.y - 320f, s.x, s.y, 0.35f, 0.35f, s.radius * 0.5f, 0f, 0xFFD97706, 0f))
                screenShake = min(1f, screenShake + 0.5f)
                soundEvents.add(Sfx.LIGHTNING)
                for (f in allFighters()) {
                    if (!f.alive) continue
                    if (hypot(f.x - s.x, f.y - s.y) <= s.radius + f.radius) {
                        dealDamage(f, s.damage, fromPlayer = false, isDot = false)
                    }
                }
            }
        }
        strikes.removeAll { it.resolved }
    }

    fun spawnStrike(
        x: Float,
        y: Float,
        telegraph: Float,
        damage: Float = Balance.LIGHTNING_DAMAGE,
        radius: Float = 90f,
    ) {
        strikes.add(Strike(x, y, telegraph, damage = damage, radius = radius))
    }

    fun randomPowerUp(): PowerUpType {
        val pool = PowerUpType.values().filter { t ->
            !(mods.contains(LevelModifier.NO_SHIELD) && t == PowerUpType.SHIELD) &&
                !(mods.contains(LevelModifier.NO_HEAL) && t == PowerUpType.HEAL)
        }
        return pool[rng.nextInt(pool.size)]
    }

    fun applyPower(type: PowerUpType) {
        if (type == PowerUpType.COIN) {
            matchCoins += 12 + rng.nextInt(28)
            addFloater(player.x, player.y - 36f, "+coins", 0xFFD97706)
            return
        }
        if (type == PowerUpType.HEAL && mods.contains(LevelModifier.NO_HEAL)) return
        if (type == PowerUpType.SHIELD && mods.contains(LevelModifier.NO_SHIELD)) return
        when (type) {
            PowerUpType.HEAL -> {
                player.hp = min(player.maxHp, player.hp + player.maxHp * Balance.HEAL_AMOUNT)
                addFloater(player.x, player.y - 36f, "+HP", 0xFF15803D)
            }
            PowerUpType.SHIELD -> {
                player.shieldHp = player.maxHp * 0.40f
                addFloater(player.x, player.y - 36f, "WARD", 0xFF0891B2)
            }
            PowerUpType.RAGE -> {
                player.dmgBuffAmount = 0.5f
                player.dmgBuffTimer = type.duration
                addFloater(player.x, player.y - 36f, "RAGE", 0xFFB91C1C)
            }
            PowerUpType.SWIFT -> {
                player.speedBuffAmount = 0.3f
                player.speedBuffTimer = type.duration
                addFloater(player.x, player.y - 36f, "SWIFT", 0xFF3F6212)
            }
            PowerUpType.VAMP -> {
                player.vampTimer = type.duration
                addFloater(player.x, player.y - 36f, "LEECH", 0xFF7C2D12)
            }
            PowerUpType.COIN -> Unit
        }
        addFx(Fx(1, player.x, player.y, 0f, 0f, 0.4f, 0.4f, 70f, 0f, type.color, 0f))
    }

    fun activePowerList(): List<Triple<String, String, Float>> {
        val list = mutableListOf<Triple<String, String, Float>>()
        if (player.dmgBuffTimer > 0f) list.add(Triple("😡", "Rage", player.dmgBuffTimer))
        if (player.speedBuffTimer > 0f) list.add(Triple("💨", "Swift", player.speedBuffTimer))
        if (player.vampTimer > 0f) list.add(Triple("🩸", "Leech", player.vampTimer))
        if (player.shieldHp > 0f) list.add(Triple("🛡️", "Ward", player.shieldHp / (player.maxHp * 0.4f)))
        if (cowardTimer > 0f) list.add(Triple("🐄", "Coward", cowardTimer))
        return list
    }

    // ==================================================
    // PHYSICS HELPERS
    // ==================================================
    internal fun allFighters(): List<Fighter> {
        val list = ArrayList<Fighter>(npcs.size + 2)
        list.add(player)
        list.addAll(npcs)
        chimera?.let { list.add(it) }
        return list
    }

    internal fun allEnemies(): List<Fighter> {
        val list = ArrayList<Fighter>(npcs.size + 1)
        for (n in npcs) if (n.faction != FACTION_NEUTRAL) list.add(n)
        chimera?.let { list.add(it) }
        return list
    }

    internal fun nearestHostile(x: Float, y: Float, maxDist: Float, faction: Int = FACTION_PLAYER): Fighter? {
        var best: Fighter? = null
        var bestScore = maxDist
        for (f in allFighters()) {
            if (!f.alive) continue
            if (f.faction == faction) continue
            if (f.faction == FACTION_NEUTRAL) continue
            if (f.inSanctuary) continue
            var d = hypot(f.x - x, f.y - y)
            // creatures hold a grudge against each other: the human is one more target,
            // not the only one. A rival creature 25% further away is still preferred.
            if (f is PlayerFighter && faction != FACTION_PLAYER) d *= 1.25f
            if (d < bestScore) {
                bestScore = d
                best = f
            }
        }
        return best
    }

    internal fun separate() {
        val list = allFighters()
        for (i in list.indices) {
            val a = list[i]
            if (!a.alive) continue
            for (j in i + 1 until list.size) {
                val b = list[j]
                if (!b.alive) continue
                var dx = b.x - a.x
                var dy = b.y - a.y
                var d = hypot(dx, dy)
                val minD = a.radius + b.radius
                if (d < minD && d > 0.0001f) {
                    val push = (minD - d) * 0.5f * Balance.SEPARATION_FORCE
                    dx /= d
                    dy /= d
                    a.x -= dx * push
                    a.y -= dy * push
                    b.x += dx * push
                    b.y += dy * push
                } else if (d <= 0.0001f) {
                    a.x -= rng.range(0.5f, 2f)
                    b.x += rng.range(0.5f, 2f)
                }
            }
        }
        // walls block movement
        for (w in walls) {
            for (f in list) {
                if (!f.alive) continue
                resolveWallCollision(f, w)
            }
        }
        list.forEach { clampWorld(it) }
    }

    private fun resolveWallCollision(f: Fighter, w: MovingWall) {
        val left = w.x - w.w / 2f
        val right = w.x + w.w / 2f
        val top = w.y - w.h / 2f
        val bottom = w.y + w.h / 2f
        val closestX = f.x.coerceIn(left, right)
        val closestY = f.y.coerceIn(top, bottom)
        val dx = f.x - closestX
        val dy = f.y - closestY
        val d = hypot(dx, dy)
        if (d > f.radius) return
        if (d > 0.0001f) {
            val push = (f.radius - d)
            f.x += dx / d * push
            f.y += dy / d * push
        } else {
            // centre inside the wall: push out on the shortest axis
            val toLeft = f.x - left
            val toRight = right - f.x
            val toTop = f.y - top
            val toBottom = bottom - f.y
            val m = minOf(toLeft, toRight, toTop, toBottom)
            when (m) {
                toLeft -> f.x = left - f.radius
                toRight -> f.x = right + f.radius
                toTop -> f.y = top - f.radius
                else -> f.y = bottom + f.radius
            }
        }
    }

    internal fun clampWorld(f: Fighter) {
        f.x = f.x.coerceIn(f.radius, world - f.radius)
        f.y = f.y.coerceIn(f.radius, world - f.radius)
    }

    internal fun moveToward(f: Fighter, tx: Float, ty: Float, speed: Float, dt: Float) {
        val dx = tx - f.x
        val dy = ty - f.y
        val d = hypot(dx, dy)
        if (d < 0.001f) {
            f.vx = 0f
            f.vy = 0f
            return
        }
        f.vx = dx / d * speed
        f.vy = dy / d * speed
        f.facing = atan2(dy, dx)
    }

    internal fun integrate(f: Fighter, dt: Float) {
        f.x += (f.vx + f.knockX) * dt
        f.y += (f.vy + f.knockY) * dt
        clampWorld(f)
    }

    // ==================================================
    // OUTCOME
    // ==================================================
    internal fun checkOutcome() {
        if (state != BattleState.RUNNING) return
        if (!player.alive) {
            state = BattleState.LOST
            soundEvents.add(Sfx.LOSE)
            return
        }
        var anyEnemy = false
        for (n in npcs) {
            if (n.alive && (n.faction != FACTION_NEUTRAL || n.hp < n.maxHp)) {
                anyEnemy = true
                break
            }
        }
        val ch = chimera
        if (ch != null && ch.alive) anyEnemy = true
        if (!anyEnemy) {
            state = BattleState.WON
            soundEvents.add(Sfx.WIN)
        }
    }

    fun isPlayerOutsideZone(): Boolean = playerOutsideZone

    fun zoneStatusText(): String {
        val cur = storm.radius.toInt()
        val nxt = storm.toRadius.toInt()
        val nextIn = (storm.nextPhaseAt - storm.phaseTimer).coerceAtLeast(0f)
        return "Zone: $cur → $nxt • Next in ${nextIn.toInt()}s"
    }

    fun drainSounds(): List<Int> {
        if (soundEvents.isEmpty()) return emptyList()
        val copy = ArrayList(soundEvents)
        soundEvents.clear()
        return copy
    }

    fun timeText(): String {
        val total = elapsed.toInt()
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    fun livingEnemyCount(): Int {
        var c = npcs.count { it.alive && (it.faction != FACTION_NEUTRAL || it.hp < it.maxHp) }
        if (chimera?.alive == true) c++
        return c
    }

    fun hpFractionForResult(): Float = player.hpFraction
}

// helpers shared by the AI files
internal fun angleDelta(a: Float, b: Float): Float {
    var d = a - b
    while (d > PI.toFloat()) d -= 2f * PI.toFloat()
    while (d < -PI.toFloat()) d += 2f * PI.toFloat()
    return d
}

const val TWO_PI = 6.2831855f
