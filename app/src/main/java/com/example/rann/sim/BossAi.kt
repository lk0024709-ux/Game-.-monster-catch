package com.example.rann.sim

import com.example.rann.model.Balance
import com.example.rann.model.BossTrait
import com.example.rann.model.ChimeraAbility
import com.example.rann.model.ChimeraDefense
import com.example.rann.model.ChimeraType
import com.example.rann.model.CreatureType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ======================================================
// RANK BOSS TRAITS
// ======================================================
internal fun BattleSim.updateBossTraits(n: NpcFighter, dt: Float) {
    val boss = n.boss ?: return

    if (boss.traits.contains(BossTrait.SPLIT_PHASES)) {
        val frac = n.hpFraction
        if (n.bossPhase == 1 && frac <= 0.66f) {
            n.bossPhase = 2
            n.invuln = 1f
            n.damage *= 1.2f
            n.summonTimer = 1.5f
            addFx(Fx(1, n.x, n.y, 0f, 0f, 0.7f, 0.7f, 240f, 0f, 0xFFD97706, 0f))
            addFloater(n.x, n.y - n.radius - 30f, "PHASE 2", 0xFFD97706, big = true)
            bannerText = "${boss.name} — Phase 2"
            bannerTimer = 2.2f
            screenShake = 0.8f
        } else if (n.bossPhase == 2 && frac <= 0.33f) {
            n.bossPhase = 3
            n.invuln = 1f
            n.damage *= 1.3f
            n.speed *= 1.2f
            n.auraRadius = max(n.auraRadius, 120f)
            n.auraDps = max(n.auraDps, 3f)
            addFx(Fx(1, n.x, n.y, 0f, 0f, 0.9f, 0.9f, 320f, 0f, 0xFF7C2D12, 0f))
            addFloater(n.x, n.y - n.radius - 30f, "PHASE 3", 0xFF7C2D12, big = true)
            bannerText = "${boss.name} — final phase, all twists active"
            bannerTimer = 2.6f
            activeCycleFeature = MapFeatureRef.LIGHTNING
            twistTimer = 12f
            screenShake = 1f
        }
    }

    if (boss.traits.contains(BossTrait.SUMMONS)) {
        n.summonTimer -= dt
        if (n.summonTimer <= 0f) {
            n.summonTimer = 15f
            val adds = boss.adds.ifEmpty { listOf(n.type) }
            var spawned = 0
            for (i in 0 until 3) {
                val living = npcs.count { it.alive && !it.isClone }
                if (living > 26) break
                val type = adds[(i + rng.nextInt(adds.size)) % adds.size]
                val a = spawnCreature(
                    type = type,
                    x = n.x + rng.range(-140f, 140f),
                    y = n.y + rng.range(-140f, 140f),
                    hp = type.baseHp * (0.6f + setup.difficulty * 0.25f),
                    damage = type.baseDamage * (0.7f + setup.difficulty * 0.2f),
                    faction = FACTION_BOSS + 20 + spawned + i,
                    label = "${type.label} (summoned)",
                    isMinion = true,
                )
                a.attackInterval = type.attackInterval
                spawned++
            }
            if (spawned > 0) {
                addFloater(n.x, n.y - n.radius - 20f, "SUMMONS!", 0xFF3F6212, big = true)
                addFx(Fx(1, n.x, n.y, 0f, 0f, 0.5f, 0.5f, 160f, 0f, 0xFF3F6212, 0f))
                soundEvents.add(Sfx.CLONE)
            }
        }
    }

    if (boss.traits.contains(BossTrait.CHAIN_LIGHTNING)) {
        n.chainTimer -= dt
        if (n.chainTimer <= 0f) {
            n.chainTimer = 9f
            val px = player.x
            val py = player.y
            spawnStrike(px, py, 0.85f)
            spawnStrike(px + 90f, py + 40f, 1.05f)
            spawnStrike(px - 80f, py - 60f, 1.25f)
            addFloater(n.x, n.y - n.radius - 20f, "CHAIN LIGHTNING", 0xFFD97706)
            soundEvents.add(Sfx.LIGHTNING)
        }
    }

    if (boss.traits.contains(BossTrait.CLONE_SPAWN)) {
        n.cloneSpawnTimer -= dt
        if (n.cloneSpawnTimer <= 0f) {
            n.cloneSpawnTimer = 20f
            for (i in 0 until 2) {
                val c = spawnCreature(
                    type = n.type,
                    x = n.x + rng.range(-160f, 160f),
                    y = n.y + rng.range(-160f, 160f),
                    hp = n.maxHp * 0.18f,
                    damage = n.damage * 0.5f,
                    faction = FACTION_BOSS + 40 + i,
                    label = "${boss.name} clone",
                    isClone = true,
                    radiusScale = 0.8f,
                )
                c.attackInterval = n.attackInterval * 1.2f
            }
            addFloater(n.x, n.y - n.radius - 20f, "MIRROR CLONES", 0xFF6366F1)
        }
    }
}

/** Small indirection so BossAi does not need to import the MapFeature enum twice. */
private object MapFeatureRef {
    val LIGHTNING = com.example.rann.model.MapFeature.LIGHTNING_STORM
}

// ======================================================
// CHIMERA AI — adaptive 3-phase duel (Elite Mode)
// ======================================================
internal fun BattleSim.updateChimera(dt: Float) {
    val c = chimera ?: return
    if (!c.alive) return
    tickStatuses(c, dt)
    val p = player

    // ability cooldowns tick down
    for (k in c.cooldowns.keys.toList()) {
        c.cooldowns[k] = max(0f, (c.cooldowns[k] ?: 0f) - dt)
    }

    // ---------------- phases ----------------
    val frac = c.hpFraction
    if (c.phase == 1 && frac <= 0.66f) enterChimeraPhase(c, 2)
    if (c.phase == 2 && frac <= 0.33f) enterChimeraPhase(c, 3)

    // ---------------- defenses ----------------
    if (c.regen && frac < 0.45f) c.hp = min(c.maxHp, c.hp + 8f * dt)
    if (c.energyShield) {
        c.shieldTimer -= dt
        if (c.shieldTimer <= 0f) {
            c.shieldTimer = 15f
            c.shieldHp = 200f
        }
    }
    if (c.summonMinions) {
        c.minionTimer -= dt
        if (c.minionTimer <= 0f) {
            c.minionTimer = 20f
            for (i in 0 until 2) {
                val m = spawnCreature(
                    type = c.spec.type.minionType(),
                    x = c.x + rng.range(-150f, 150f),
                    y = c.y + rng.range(-150f, 150f),
                    hp = 150f,
                    damage = 14f + c.phase * 4f,
                    faction = FACTION_BOSS + 30 + i,
                    label = "minion",
                    isMinion = true,
                )
                m.attemptInterval(c.phase)
            }
            addFloater(c.x, c.y - c.radius - 24f, "MINIONS!", 0xFF3F6212)
        }
    }
    if (c.mirrorImage) {
        c.mirrorTimer -= dt
        if (c.mirrorTimer <= 0f) {
            c.mirrorTimer = 26f
            for (i in 0 until 2) {
                val t = listOf(
                    CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR,
                )[(c.spec.index + i) % 3]
                spawnCreature(
                    type = t,
                    x = c.x + rng.range(-120f, 120f),
                    y = c.y + rng.range(-120f, 120f),
                    hp = c.maxHp * 0.05f,
                    damage = c.damage * 0.35f,
                    faction = FACTION_BOSS + 50 + i,
                    label = "mirror image",
                    isClone = true,
                    decoyLife = 10f,
                    radiusScale = c.spec.type.sizeMult * 0.35f,
                )
            }
            addFloater(c.x, c.y - c.radius - 24f, "MIRROR IMAGE", 0xFF6366F1)
        }
    }
    // phase shift safety net
    if (c.phaseShift && c.shiftArmed && frac < 0.34f && c.phase == 3) {
        c.shiftArmed = false
        c.invuln = 1f
        addFx(Fx(1, c.x, c.y, 0f, 0f, 0.6f, 0.6f, 260f, 0f, 0xFFF5F1EA, 0f))
    }

    val dist = hypot(p.x - c.x, p.y - c.y)

    // ---------------- movement ----------------
    // cornered chimera with regen backs off for two seconds
    if (frac < 0.20f && c.regen && c.retreatTimer <= 0f && !c.enraged) {
        c.retreatTimer = 2f
        addFloater(c.x, c.y - c.radius - 20f, "RECOVERING", 0xFF3F6212)
    }
    if (c.retreatTimer > 0f) c.retreatTimer -= dt

    val casting = c.castAbility != null
    if (c.retreatTimer > 0f) {
        val ax = if (c.x < p.x) -1f else 1f
        val ay = if (c.y < p.y) -1f else 1f
        moveToward(c, c.x + ax * 200f, c.y + ay * 200f, c.speed * 0.9f, dt)
    } else if (casting) {
        c.vx = 0f
        c.vy = 0f
        c.facing = atan2(p.y - c.y, p.x - c.x)
    } else {
        val want = if (dist < 120f) -1f else if (dist > 260f) 1f else 0f
        if (want != 0f) {
            moveToward(c, c.x + (p.x - c.x) * want, c.y + (p.y - c.y) * want, c.speed, dt)
        } else {
            val d = dist.coerceAtLeast(0.001f)
            val nx = (p.x - c.x) / d
            val ny = (p.y - c.y) / d
            c.vx = -ny * c.speed * 0.6f
            c.vy = nx * c.speed * 0.6f
            c.facing = atan2(ny, nx)
        }
    }

    // ---------------- ability casting ----------------
    if (casting) {
        c.castTimer -= dt
        if (c.castTimer <= 0f) {
            val ability = c.castAbility!!
            executeChimeraAbility(c, ability, c.castTargetX, c.castTargetY)
            c.castAbility = null
            c.abilityCd = c.abilityInterval() * rng.range(0.9f, 1.25f)
        }
    } else {
        c.abilityCd -= dt
        if (c.abilityCd <= 0f && c.retreatTimer <= 0f) {
            val pick = selectChimeraAbility(c, dist)
            if (pick != null) {
                c.castAbility = pick
                val telMult = when (c.phase) {
                    1 -> 1f
                    2 -> 0.7f
                    else -> 0.5f
                }
                c.castTimer = pick.telegraph * telMult
                c.castTargetX = p.x
                c.castTargetY = p.y
                c.cooldowns[pick] = pick.cooldown
                c.lastAbility = pick
            } else {
                c.abilityCd = 0.6f
            }
        }
    }

    // basic melee: a heavy swipe whenever the player hugs the chimera
    if (!casting && c.retreatTimer <= 0f && c.attackCd <= 0f && dist <= c.radius + p.radius + 80f) {
        c.attackCd = 1.0f
        c.swingAnim = 0.2f
        val angle = atan2(p.y - c.y, p.x - c.x)
        c.swingDir = angle
        dealDamage(
            p, c.damage * 0.45f, fromPlayer = false, source = c,
            knockPower = 150f, kx = cos(angle), ky = sin(angle),
        )
        addFx(Fx(0, c.x, c.y, 0f, 0f, 0.2f, 0.2f, c.radius + 90f, angle, c.spec.type.accent, TWO_PI * 0.4f))
    }

    // enraged look in phase 3
    c.enraged = c.phase == 3

    integrate(c, dt)
    clampWorld(c)
}

private fun ChimeraAbility.range(): Float = when (this) {
    ChimeraAbility.TAIL_SWIPE, ChimeraAbility.DIVE_STRIKE, ChimeraAbility.CHARGE,
    ChimeraAbility.ROLL_ATTACK, ChimeraAbility.SHOCKWAVE -> 320f
    else -> 420f
}

private fun ChimeraFighter.abilityInterval(): Float = when (phase) {
    1 -> 2.5f
    2 -> 1.9f
    else -> 1.8f
}

private fun NpcFighter.attemptInterval(phase: Int) {
    attackInterval = 1.6f - phase * 0.2f
}

private fun ChimeraType.minionType(): CreatureType = when (this) {
    ChimeraType.GRYPHON_PRIME -> CreatureType.GRIFFIN
    ChimeraType.BASILISK -> CreatureType.VENOMTAIL
    ChimeraType.RAVAGER -> CreatureType.IRONBOAR
    ChimeraType.WYVERN -> CreatureType.SKYHAWK
    ChimeraType.DEATHROLL -> CreatureType.IRONBOAR
    ChimeraType.RANNS_CHIMERA -> CreatureType.GRIFFIN
    ChimeraType.OMEGA_CHIMERA -> CreatureType.DRAKE
}

private fun BattleSim.enterChimeraPhase(c: ChimeraFighter, phase: Int) {
    c.phase = phase
    c.damage *= if (phase == 2) 1.12f else 1.25f
    c.speed *= if (phase == 2) 1.08f else 1.15f
    if (c.phaseShift) {
        c.invuln = 1f
        addFx(Fx(1, c.x, c.y, 0f, 0f, 0.5f, 0.5f, 220f, 0f, 0xFFF5F1EA, 0f))
        addFloater(c.x, c.y - c.radius - 30f, "PHASE SHIFT", 0xFFF5F1EA, big = true)
    }
    if (c.summonMinions) c.minionTimer = 0.8f
    bannerText = "${c.spec.displayName} — Phase $phase"
    bannerTimer = 2.2f
    screenShake = 0.7f
    addFloater(c.x, c.y - c.radius - 46f, "PHASE $phase", 0xFFD97706, big = true)
}

private fun BattleSim.selectChimeraAbility(c: ChimeraFighter, dist: Float): ChimeraAbility? {
    // phases gate how much of the arsenal is online: 2 → 3 → everything
    val available = when (c.phase) {
        1 -> c.spec.abilities.take(2)
        2 -> c.spec.abilities.take(3)
        else -> c.spec.abilities
    }
    val ready = available.filter { (c.cooldowns[it] ?: 0f) <= 0f && it != c.lastAbility }
    if (ready.isEmpty()) return null
    val meleeSet = setOf(
        ChimeraAbility.TAIL_SWIPE, ChimeraAbility.DIVE_STRIKE,
        ChimeraAbility.CHARGE, ChimeraAbility.ROLL_ATTACK, ChimeraAbility.SHOCKWAVE,
    )
    val preferMelee = dist < 210f
    val preferred = ready.filter { (it in meleeSet) == preferMelee }
    val pool = if (preferred.isNotEmpty()) preferred else ready
    // aggro: punish a wounded player with the heaviest hit available
    if (player.hpFraction < 0.30f) {
        return pool.maxByOrNull { it.baseDamage } ?: pool.first()
    }
    // respectful of range: never pick an ability the player is far outside of
    val inRange = pool.filter { dist <= it.radius + 120f }
    val finalPool = if (inRange.isNotEmpty()) inRange else pool
    return finalPool[rng.nextInt(finalPool.size)]
}

private fun BattleSim.executeChimeraAbility(c: ChimeraFighter, ability: ChimeraAbility, tx: Float, ty: Float) {
    val p = player
    val abilityDamage = ability.baseDamage * c.spec.type.damageMult * (1f + (c.phase - 1) * 0.15f) * 0.45f
    val distToPlayer = hypot(p.x - c.x, p.y - c.y)

    when (ability) {
        ChimeraAbility.DIVE_STRIKE -> {
            val angle = atan2(p.y - c.y, p.x - c.x)
            c.x = (p.x - cos(angle) * 60f).coerceIn(c.radius, world - c.radius)
            c.y = (p.y - sin(angle) * 60f).coerceIn(c.radius, world - c.radius)
            addFx(Fx(2, c.x, c.y, 0f, 0f, 0.4f, 0.4f, ability.radius, 0f, c.spec.type.accent, 0f))
            if (hypot(p.x - c.x, p.y - c.y) <= ability.radius + p.radius) {
                dealDamage(p, abilityDamage, fromPlayer = false, source = c,
                    knockPower = 260f, kx = cos(angle), ky = sin(angle))
            }
            screenShake = min(1f, screenShake + 0.5f)
        }

        ChimeraAbility.SHOCKWAVE -> {
            addFx(Fx(1, c.x, c.y, 0f, 0f, 0.5f, 0.5f, ability.radius, 0f, c.spec.type.accent, 0f))
            if (hypot(p.x - c.x, p.y - c.y) <= ability.radius + p.radius) {
                val angle = atan2(p.y - c.y, p.x - c.x)
                dealDamage(p, abilityDamage, fromPlayer = false, source = c,
                    knockPower = 300f, kx = cos(angle), ky = sin(angle))
            }
            screenShake = min(1f, screenShake + 0.6f)
        }

        ChimeraAbility.POISON_BREATH -> {
            val angle = atan2(p.y - c.y, p.x - c.x)
            addFx(Fx(4, c.x, c.y, c.x + cos(angle) * ability.radius, c.y + sin(angle) * ability.radius,
                0.45f, 0.45f, ability.radius, angle, 0xFF4D7C0F, TWO_PI * 0.22f))
            if (distToPlayer <= ability.radius && abs(angleDelta(atan2(p.y - c.y, p.x - c.x), c.facing)) < 0.75f) {
                dealDamage(p, abilityDamage, fromPlayer = false, source = c)
                p.poisonStacks = min(5, p.poisonStacks + 3)
                p.poisonTimer = 4f
                addFloater(p.x, p.y - 30f, "POISON x${p.poisonStacks}", 0xFF4D7C0F)
            }
        }

        ChimeraAbility.FIREBALLS -> {
            val base = atan2(p.y - c.y, p.x - c.x)
            for (i in -1..1) {
                val angle = base + i * 0.22f
                projectiles.add(
                    Projectile(
                        x = c.x + cos(angle) * (c.radius + 8f),
                        y = c.y + sin(angle) * (c.radius + 8f),
                        vx = cos(angle) * 300f,
                        vy = sin(angle) * 300f,
                        damage = 20f * c.spec.type.damageMult + (c.phase - 1) * 6f,
                        radius = 12f,
                        faction = FACTION_BOSS,
                        life = 3f,
                        kind = 1,
                        target = p,
                        turnRate = 2.2f,
                        color = 0xFFD97706,
                    )
                )
            }
        }

        ChimeraAbility.PETRIFY -> {
            addFx(Fx(1, c.x, c.y, 0f, 0f, 0.4f, 0.4f, ability.radius, 0f, 0xFF78716C, 0f))
            if (distToPlayer <= ability.radius) {
                p.slowFactor = 0.70f
                p.slowTimer = 3f
                dealDamage(p, abilityDamage, fromPlayer = false, source = c, isDot = true)
                addFloater(p.x, p.y - 34f, "PETRIFIED", 0xFF78716C, big = true)
            }
        }

        ChimeraAbility.CHARGE -> {
            val angle = atan2(p.y - c.y, p.x - c.x)
            c.x = (p.x + cos(angle) * 40f).coerceIn(c.radius, world - c.radius)
            c.y = (p.y + sin(angle) * 40f).coerceIn(c.radius, world - c.radius)
            addFx(Fx(3, c.x - cos(angle) * 200f, c.y - sin(angle) * 200f, c.x, c.y, 0.3f, 0.3f, 40f, angle, c.spec.type.body, 0f))
            if (hypot(p.x - c.x, p.y - c.y) <= ability.radius + p.radius) {
                dealDamage(p, abilityDamage, fromPlayer = false, source = c,
                    knockPower = 320f, kx = cos(angle), ky = sin(angle))
                p.stun = 1f
                addFloater(p.x, p.y - 30f, "STUNNED", 0xFFB91C1C, big = true)
            }
            screenShake = min(1f, screenShake + 0.7f)
        }

        ChimeraAbility.ROAR -> {
            addFx(Fx(1, c.x, c.y, 0f, 0f, 0.6f, 0.6f, ability.radius, 0f, 0xFFB45309, 0f))
            if (distToPlayer <= ability.radius) {
                dealDamage(p, abilityDamage, fromPlayer = false, source = c, isDot = true)
                p.slowFactor = max(p.slowFactor, 0.4f)
                p.slowTimer = max(p.slowTimer, 4f)
                addFloater(p.x, p.y - 34f, "SHAKEN", 0xFFB45309)
            }
            screenShake = min(1f, screenShake + 0.4f)
        }

        ChimeraAbility.ROLL_ATTACK -> {
            val angle = if (distToPlayer > 0f) atan2(p.y - c.y, p.x - c.x) else rng.range(0f, TWO_PI)
            val nx = (c.x + cos(angle) * 220f).coerceIn(c.radius, world - c.radius)
            val ny = (c.y + sin(angle) * 220f).coerceIn(c.radius, world - c.radius)
            addFx(Fx(3, c.x, c.y, nx, ny, 0.32f, 0.32f, 44f, angle, c.spec.type.body, 0f))
            // anything along the roll path takes the hit
            val dx = nx - c.x
            val dy = ny - c.y
            val len = hypot(dx, dy).coerceAtLeast(1f)
            val t = (((p.x - c.x) * dx + (p.y - c.y) * dy) / (len * len)).coerceIn(0f, 1f)
            val px = c.x + dx * t
            val py = c.y + dy * t
            if (hypot(p.x - px, p.y - py) <= 62f + p.radius) {
                dealDamage(p, abilityDamage, fromPlayer = false, source = c,
                    knockPower = 240f, kx = cos(angle), ky = sin(angle))
            }
            c.x = nx
            c.y = ny
            screenShake = min(1f, screenShake + 0.5f)
        }

        ChimeraAbility.TAIL_SWIPE -> {
            val angle = atan2(p.y - c.y, p.x - c.x)
            c.facing = angle
            addFx(Fx(0, c.x, c.y, 0f, 0f, 0.24f, 0.24f, ability.radius, angle, c.spec.type.accent, TWO_PI * 0.4f))
            if (distToPlayer <= ability.radius + p.radius) {
                dealDamage(p, abilityDamage, fromPlayer = false, source = c,
                    knockPower = 180f, kx = cos(angle), ky = sin(angle))
            }
        }

        ChimeraAbility.LIGHTNING_STRIKE -> {
            val lead = 0.5f
            val px = (p.x + p.vx * lead).coerceIn(60f, world - 60f)
            val py = (p.y + p.vy * lead).coerceIn(60f, world - 60f)
            spawnStrike(px, py, 0.7f, damage = 30f + (c.phase - 1) * 8f, radius = 100f)
            spawnStrike(tx + rng.range(-70f, 70f), ty + rng.range(-70f, 70f), 1.0f, damage = 22f, radius = 90f)
            soundEvents.add(Sfx.LIGHTNING)
        }
    }
    addFloater(c.x, c.y - c.radius - 16f, ability.displayName, c.spec.type.accent)
}

/** Chimera ranged-ability distance preference, used by the renderer for telegraph rings. */
internal fun ChimeraAbility.telegraphRadius(): Float = radius
