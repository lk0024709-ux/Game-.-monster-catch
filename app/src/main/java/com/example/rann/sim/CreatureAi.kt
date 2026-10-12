package com.example.rann.sim

import com.example.rann.model.Balance
import com.example.rann.model.CreatureType
import com.example.rann.model.MapFeature
import com.example.rann.model.LevelModifier
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ======================================================
// WILD CREATURE AI — every NPC fights EVERY other faction.
// Free-for-all: the player is just one more target.
// ======================================================

private fun dist(a: Fighter, b: Fighter): Float = hypot(b.x - a.x, b.y - a.y)

internal fun BattleSim.updateNpcs(dt: Float) {
    // snapshot: summons and clones can be added mid-iteration
    for (n in ArrayList(npcs)) {
        if (!n.alive) continue
        tickStatuses(n, dt)

        // temporary decoys fade away
        if (n.decoyLife > 0f) {
            n.decoyLife -= dt
            if (n.decoyLife <= 0f) {
                killFighter(n)
                continue
            }
        }

        // the neutral throne guardian only patrols until provoked
        if (n.faction == FACTION_NEUTRAL) {
            patrolGuardian(n, dt)
            continue
        }

        updateNpcTargeting(n, dt)
        updateNpcBehavior(n, dt)
        if (n.boss != null) updateBossTraits(n, dt)
    }
    npcs.removeAll { !it.alive && it.decoyLife <= 0f }
}

private fun BattleSim.patrolGuardian(n: NpcFighter, dt: Float) {
    val angle = elapsed * 0.35f + n.retargetTimer
    val cx = world * 0.5f
    val cy = world * 0.5f
    val tx = cx + cos(angle) * 140f
    val ty = cy + sin(angle) * 140f
    moveToward(n, tx, ty, n.speed * 0.4f, dt)
    integrate(n, dt)
}

private fun BattleSim.updateNpcTargeting(n: NpcFighter, dt: Float) {
    n.retargetTimer -= dt
    val cur = n.target
    if (n.retargetTimer <= 0f || cur == null || !cur.alive) {
        n.retargetTimer = Balance.NPC_RETARGET
        n.target = nearestHostile(n.x, n.y, 2400f, n.faction)
        n.specialTimer = max(n.specialTimer, 0f)
    }
}

private fun BattleSim.updateNpcBehavior(n: NpcFighter, dt: Float) {
    val target = n.target ?: run {
        // wander toward the zone centre
        moveToward(n, storm.cx, storm.cy, n.speed * 0.5f, dt)
        integrate(n, dt)
        return
    }

    var speed = n.speed * n.speedMultiplier()
    if (bloodlust) speed *= 1f + Balance.BLOODLUST_SPEED
    if (fogActive) speed *= 0.85f
    if (n.stun > 0f) speed = 0f

    // storm pressure beats everything else
    val dz = hypot(n.x - storm.cx, n.y - storm.cy)
    if (dz > storm.radius - 50f) {
        moveToward(n, storm.cx, storm.cy, speed * 1.1f, dt)
        integrate(n, dt)
        meleeAttack(n, target, dt)
        return
    }

    // wounded creatures look for a healing shrine
    if (n.hpFraction < 0.4f && n.shrineCd <= 0f && !mods.contains(LevelModifier.NO_HEAL)) {
        val shrine = shrines.minByOrNull { hypot(it.x - n.x, it.y - n.y) }
        if (shrine != null && hypot(shrine.x - n.x, shrine.y - n.y) < 520f) {
            moveToward(n, shrine.x, shrine.y, speed, dt)
            integrate(n, dt)
            return
        }
    }

    when (n.type) {
        CreatureType.GRIFFIN -> griffinBehavior(n, target, speed, dt)
        CreatureType.DRAKE -> drakeBehavior(n, target, speed, dt)
        CreatureType.SKYHAWK -> skyhawkBehavior(n, target, speed, dt)
        CreatureType.IRONBOAR -> ironboarBehavior(n, target, speed, dt)
        CreatureType.VENOMTAIL -> venomtailBehavior(n, target, speed, dt)
    }

    integrate(n, dt)
    meleeAttack(n, target, dt)
}

// ------------------------------------------------------
// GRIFFIN — balanced aggressor with a wing-flap dash
// ------------------------------------------------------
private fun BattleSim.griffinBehavior(n: NpcFighter, target: Fighter, speed: Float, dt: Float) {
    if (n.state == 2) {
        n.stateTimer -= dt
        moveToward(n, target.x, target.y, speed * 2f, dt)
        contactDamage(n, target, n.damage * 0.9f, 70f, once = false)
        if (n.stateTimer <= 0f) n.state = 0
        return
    }
    n.specialTimer -= dt
    val d = dist(n, target)
    if (n.specialTimer <= 0f && d < 460f) {
        n.specialTimer = 4f
        n.state = 2
        n.stateTimer = 0.5f
        addFloater(n.x, n.y - n.radius - 10f, "FLAP", 0xFFC2410C)
    }
    val stopAt = (n.reach * 0.8f + target.radius)
    if (d > stopAt) {
        moveToward(n, target.x, target.y, speed, dt)
    } else {
        orbit(n, target, speed * 0.6f, 1f)
    }
}

// ------------------------------------------------------
// DRAKE — ranged artillery that breathes fire
// ------------------------------------------------------
private fun BattleSim.drakeBehavior(n: NpcFighter, target: Fighter, speed: Float, dt: Float) {
    val d = dist(n, target)
    when {
        d < Balance.DRAKE_PREFERRED_RANGE - 40f -> moveToward(n, n.x * 2f - target.x, n.y * 2f - target.y, speed, dt)
        d > Balance.DRAKE_PREFERRED_RANGE + 40f -> moveToward(n, target.x, target.y, speed, dt)
        else -> orbit(n, target, speed * 0.7f, 1f)
    }
    n.specialTimer -= dt
    if (n.specialTimer <= 0f && d < n.reach) {
        n.specialTimer = n.attackInterval
        n.attackCd = n.attackInterval
        val angle = atan2(target.y - n.y, target.x - n.x)
        projectiles.add(
            Projectile(
                x = n.x + cos(angle) * (n.radius + 6f),
                y = n.y + sin(angle) * (n.radius + 6f),
                vx = cos(angle) * 400f,
                vy = sin(angle) * 400f,
                damage = n.damage * 0.85f,
                radius = 9f,
                faction = n.faction,
                life = 1.4f,
                kind = 0,
                color = 0xFFC2410C,
            )
        )
        addFx(Fx(2, n.x, n.y, 0f, 0f, 0.22f, 0.22f, 26f, 0f, 0xFFD97706, 0f))
    }
}

// ------------------------------------------------------
// SKYHAWK — circles at range and dives
// ------------------------------------------------------
private fun BattleSim.skyhawkBehavior(n: NpcFighter, target: Fighter, speed: Float, dt: Float) {
    if (n.state == 2) {
        n.stateTimer -= dt
        moveToward(n, target.x, target.y, speed * 3f, dt)
        if (!n.didContactDamage && dist(n, target) <= n.radius + target.radius + 6f) {
            n.didContactDamage = true
            dealDamage(target, n.damage * 1.5f, fromPlayer = false, source = n, knockPower = 120f,
                kx = (target.x - n.x).let { if (it >= 0f) 1f else -1f },
                ky = (target.y - n.y).let { if (it >= 0f) 1f else -1f })
            addFx(Fx(2, target.x, target.y, 0f, 0f, 0.24f, 0.24f, 40f, 0f, 0xFF92400E, 0f))
        }
        if (n.stateTimer <= 0f) {
            n.state = 0
            n.didContactDamage = false
        }
        return
    }
    n.specialTimer -= dt
    if (n.specialTimer <= 0f) {
        n.specialTimer = 3f
        n.state = 2
        n.stateTimer = 0.4f
        n.didContactDamage = false
        addFloater(n.x, n.y - n.radius - 10f, "DIVE", 0xFF92400E)
    }
    orbit(n, target, speed, radius = Balance.SKYHAWK_ORBIT_RANGE)
}

// ------------------------------------------------------
// IRONBOAR — telegraphed charge
// ------------------------------------------------------
private fun BattleSim.ironboarBehavior(n: NpcFighter, target: Fighter, speed: Float, dt: Float) {
    when (n.state) {
        1 -> { // telegraph
            n.stateTimer -= dt
            n.facing = atan2(target.y - n.y, target.x - n.x)
            n.vx = 0f
            n.vy = 0f
            if (n.stateTimer <= 0f) {
                n.state = 2
                n.stateTimer = 1.0f
                n.didContactDamage = false
            }
        }
        2 -> { // charge
            n.stateTimer -= dt
            n.vx = cos(n.facing) * speed * 2.5f
            n.vy = sin(n.facing) * speed * 2.5f
            if (!n.didContactDamage && dist(n, target) <= n.radius + target.radius + 12f) {
                n.didContactDamage = true
                dealDamage(
                    target, n.damage * 1.5f, fromPlayer = false, source = n,
                    knockPower = 260f, kx = cos(n.facing), ky = sin(n.facing),
                )
                addFx(Fx(2, target.x, target.y, 0f, 0f, 0.28f, 0.28f, 52f, 0f, 0xFF44403C, 0f))
            }
            if (n.stateTimer <= 0f) {
                n.state = 3
                n.stateTimer = 0.6f
            }
        }
        3 -> { // recover
            n.stateTimer -= dt
            n.vx = 0f
            n.vy = 0f
            if (n.stateTimer <= 0f) n.state = 0
        }
        else -> {
            n.specialTimer -= dt
            val d = dist(n, target)
            if (n.specialTimer <= 0f && d < 520f && d > 60f) {
                n.specialTimer = 5f
                n.state = 1
                n.stateTimer = 0.6f
                n.facing = atan2(target.y - n.y, target.x - n.x)
                addFloater(n.x, n.y - n.radius - 10f, "!", 0xFFB45309, big = true)
            } else if (d > n.reach * 0.85f + target.radius) {
                moveToward(n, target.x, target.y, speed, dt)
            } else {
                orbit(n, target, speed * 0.5f, 0.7f)
            }
        }
    }
}

// ------------------------------------------------------
// VENOMTAIL — stacking poison bites
// ------------------------------------------------------
private fun BattleSim.venomtailBehavior(n: NpcFighter, target: Fighter, speed: Float, dt: Float) {
    val d = dist(n, target)
    val stopAt = n.reach * 0.8f + target.radius
    if (d > stopAt) {
        moveToward(n, target.x, target.y, speed, dt)
    } else {
        orbit(n, target, speed * 0.8f, 1f)
    }
}

// ------------------------------------------------------
// SHARED MELEE / MOVEMENT HELPERS
// ------------------------------------------------------
internal fun BattleSim.meleeAttack(n: NpcFighter, target: Fighter, dt: Float) {
    if (n.attackCd > 0f || n.state == 1 || n.state == 2) return
    val d = dist(n, target)
    if (d > n.reach + target.radius + 4f) return
    n.attackCd = n.attackInterval
    n.swingAnim = 0.18f
    n.swingDir = atan2(target.y - n.y, target.x - n.x)
    var dmg = n.damage
    if (bloodlust) dmg *= 1f + Balance.BLOODLUST_DAMAGE
    val dirX = (target.x - n.x).let { if (it >= 0f) 1f else -1f }
    val dirY = (target.y - n.y).let { if (it >= 0f) 1f else -1f }
    dealDamage(target, dmg, fromPlayer = false, source = n, knockPower = 80f, kx = dirX, ky = dirY)
    addFx(Fx(0, n.x, n.y, 0f, 0f, 0.18f, 0.18f, n.reach, n.swingDir, 0xFFB45309, TWO_PI * 0.35f))
    if (n.type == CreatureType.VENOMTAIL) {
        target.poisonStacks = min(5, target.poisonStacks + 1)
        target.poisonTimer = 4f
        if (target === player) addFloater(player.x, player.y - 30f, "VENOM x${target.poisonStacks}", 0xFF4D7C0F)
    }
}

private fun BattleSim.contactDamage(n: NpcFighter, target: Fighter, dmg: Float, knock: Float, once: Boolean) {
    if (once && n.didContactDamage) return
    if (dist(n, target) > n.radius + target.radius + 6f) return
    if (once) n.didContactDamage = true
    var d = dmg
    if (bloodlust) d *= 1f + Balance.BLOODLUST_DAMAGE
    dealDamage(target, d, fromPlayer = false, source = n, knockPower = knock,
        kx = cos(n.facing), ky = sin(n.facing))
}

/** Circles a target at [radius] distance, strafing sideways. */
internal fun BattleSim.orbit(n: NpcFighter, target: Fighter, speed: Float, radius: Float) {
    val dx = n.x - target.x
    val dy = n.y - target.y
    val d = hypot(dx, dy).coerceAtLeast(0.001f)
    val nx = dx / d
    val ny = dy / d
    val radial = if (d > radius) -1f else 1f
    val tx = -ny
    val ty = nx
    val strafe = if (n.faction % 2 == 0) 1f else -1f
    var vx = (nx * radial + tx * strafe * 0.85f) * speed
    var vy = (ny * radial + ty * strafe * 0.85f) * speed
    val mag = hypot(vx, vy)
    if (mag > speed) {
        vx = vx / mag * speed
        vy = vy / mag * speed
    }
    n.vx = vx
    n.vy = vy
    if (mag > 1f) n.facing = atan2(vy, vx)
}

/** Legacy fork marker used by features: creatures rarely cross the whole map. */
internal fun BattleSim.npcDistanceTo(n: NpcFighter, x: Float, y: Float): Float = hypot(n.x - x, n.y - y)

internal fun BattleSim.spawnCreature(
    type: CreatureType,
    x: Float,
    y: Float,
    hp: Float,
    damage: Float,
    faction: Int,
    label: String,
    isClone: Boolean = false,
    isMinion: Boolean = false,
    decoyLife: Float = 0f,
    radiusScale: Float = 1f,
    speedScale: Float = 1f,
): NpcFighter {
    val n = NpcFighter(
        x = x.coerceIn(40f, world - 40f),
        y = y.coerceIn(40f, world - 40f),
        type = type,
        hp = hp,
        speed = type.baseSpeed * speedScale,
        damage = damage,
        radius = type.size * radiusScale,
        faction = faction,
    )
    n.isClone = isClone
    n.isMinion = isMinion
    n.decoyLife = decoyLife
    n.label = label
    npcs.add(n)
    return n
}
