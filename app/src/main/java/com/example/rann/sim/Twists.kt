package com.example.rann.sim

import com.example.rann.model.Balance
import com.example.rann.model.LevelModifier
import com.example.rann.model.MapFeature
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

// ======================================================
// MAP TWISTS — every ranked arena plays differently
// ======================================================

/** 0..1 charge of a treasure shrine, used by the renderer's progress ring. */
internal fun BattleSim.treasureProgress(zoneIndex: Int): Float =
    ((treasureTimers[zoneIndex] ?: 0f) / 2f).coerceIn(0f, 1f)

internal fun BattleSim.hasFeature(feature: MapFeature): Boolean =
    staticFeatures.contains(feature) || activeCycleFeature == feature

private val TWIST_CYCLE = listOf(
    MapFeature.LAVA_POOLS,
    MapFeature.FIRE_GEYSERS,
    MapFeature.LIGHTNING_STORM,
    MapFeature.WIND_PUSH,
    MapFeature.SAND_FOG,
    MapFeature.ICE_PATCHES,
)

internal fun BattleSim.updateFeatures(dt: Float) {
    updateTwistCycle(dt)
    updateSandFog(dt)
    updateLava(dt)
    updateGeysers(dt)
    updateWalls(dt)
    updateMirrorClones(dt)
    updateCrystals(dt)
    updateLightningStorm(dt)
    updateTreasureShrines(dt)
}

// ------------------------------------------------------
// Throne of Rann: every twist, cycling every 30s
// ------------------------------------------------------
private fun BattleSim.updateTwistCycle(dt: Float) {
    if (!staticFeatures.contains(MapFeature.TWIST_CYCLE)) return
    twistTimer -= dt
    if (twistTimer <= 0f) {
        twistTimer = 30f
        twistIndex++
        val feature = TWIST_CYCLE[twistIndex % TWIST_CYCLE.size]
        activeCycleFeature = feature
        when (feature) {
            MapFeature.LAVA_POOLS -> ensureLavaPools(4)
            MapFeature.ICE_PATCHES -> ensureIcePatches(4)
            MapFeature.WIND_PUSH -> if (wind == null) {
                wind = WindState(angle = rng.range(0f, TWO_PI), timer = 10f, strength = 20f)
            }
            else -> Unit
        }
        bannerText = "Twist: ${featureLabel(feature)}"
        bannerTimer = 2.4f
    }
}

private fun featureLabel(feature: MapFeature): String = when (feature) {
    MapFeature.LAVA_POOLS -> "Rising Lava"
    MapFeature.FIRE_GEYSERS -> "Fire Geysers"
    MapFeature.LIGHTNING_STORM -> "Rift Lightning"
    MapFeature.WIND_PUSH -> "Howling Wind"
    MapFeature.SAND_FOG -> "Sandfog"
    MapFeature.ICE_PATCHES -> "Black Ice"
    MapFeature.MOVING_WALLS -> "Crushing Walls"
    MapFeature.TREASURE_SHRINES -> "Treasure Shrines"
    MapFeature.MIRROR_CLONES -> "Mirror Clones"
    MapFeature.DESTRUCTIBLE_CRYSTALS -> "Crystal Veins"
    MapFeature.HEAT_ZONES -> "Heat Vents"
    MapFeature.TWIST_CYCLE -> "Everything"
    MapFeature.ARENA_BOSS -> "Rann's Avatar"
}

// ------------------------------------------------------
// Sandfog: 5s of murk every 12s
// ------------------------------------------------------
private fun BattleSim.updateSandFog(dt: Float) {
    if (!hasFeature(MapFeature.SAND_FOG)) return
    fogTimer += dt
    if (!fogActive && fogTimer >= 12f) {
        fogTimer = 0f
        fogActive = true
        fogDuration = 5f
        bannerText = "Sandfog rolls in"
        bannerTimer = 1.8f
    }
    if (fogActive) {
        fogDuration -= dt
        if (fogDuration <= 0f) fogActive = false
    }
}

// ------------------------------------------------------
// Lava pools
// ------------------------------------------------------
private fun BattleSim.ensureLavaPools(count: Int) {
    if (zones.any { it.kind == 0 }) return
    repeat(count) {
        zones.add(
            Zone(
                x = rng.range(world * 0.18f, world * 0.82f),
                y = rng.range(world * 0.18f, world * 0.82f),
                radius = rng.range(52f, 88f),
                kind = 0,
            ).also { it.baseRadius = it.radius }
        )
    }
}

private fun BattleSim.ensureIcePatches(count: Int) {
    if (zones.any { it.kind == 1 }) return
    repeat(count) {
        zones.add(
            Zone(
                x = rng.range(world * 0.2f, world * 0.8f),
                y = rng.range(world * 0.2f, world * 0.8f),
                radius = rng.range(90f, 140f),
                kind = 1,
            ).also { it.baseRadius = it.radius }
        )
    }
}

private fun BattleSim.updateLava(dt: Float) {
    if (!hasFeature(MapFeature.LAVA_POOLS)) return
    ensureLavaPools(4)
    if (!mods.contains(LevelModifier.RISING_LAVA)) return
    for (z in zones) {
        if (z.kind != 0) continue
        z.radius = min(z.baseRadius * 1.8f, z.radius + 1.1f * dt)
    }
}

// ------------------------------------------------------
// Fire geysers (1s telegraph, 20 damage + knockback)
// ------------------------------------------------------
private fun BattleSim.updateGeysers(dt: Float) {
    if (!hasFeature(MapFeature.FIRE_GEYSERS)) return
    geyserTimer -= dt
    if (geyserTimer <= 0f) {
        geyserTimer = Balance.GEYSER_INTERVAL * (if (mods.contains(LevelModifier.FAST_STORM)) 0.8f else 1f)
        val angle = rng.range(0f, TWO_PI)
        val dist = rng.range(0f, storm.radius * 0.8f)
        geysers.add(
            Geyser(
                x = (storm.cx + cos(angle) * dist).coerceIn(60f, world - 60f),
                y = (storm.cy + sin(angle) * dist).coerceIn(60f, world - 60f),
                state = 0,
                stateTimer = Balance.GEYSER_TELEGRAPH,
            )
        )
    }
    for (g in geysers) {
        when (g.state) {
            0 -> {
                g.stateTimer -= dt
                if (g.stateTimer <= 0f) {
                    g.state = 1
                    g.stateTimer = 0.35f
                    soundEvents.add(Sfx.GEYSER)
                    screenShake = min(1f, screenShake + 0.35f)
                    addFx(Fx(2, g.x, g.y, 0f, 0f, 0.4f, 0.4f, 74f, 0f, 0xFFC2410C, 0f))
                    for (f in allFighters()) {
                        if (!f.alive || f.inSanctuary) continue
                        if (hypot(f.x - g.x, f.y - g.y) <= 74f + f.radius) {
                            val a = kotlin.math.atan2(f.y - g.y, f.x - g.x)
                            dealDamage(
                                f, Balance.GEYSER_DAMAGE, fromPlayer = false,
                                knockPower = 240f, kx = cos(a), ky = sin(a),
                            )
                        }
                    }
                }
            }
            else -> {
                g.stateTimer -= dt
            }
        }
    }
    geysers.removeAll { it.state == 1 && it.stateTimer <= 0f }
}

// ------------------------------------------------------
// Moving walls on tracks
// ------------------------------------------------------
private fun BattleSim.updateWalls(dt: Float) {
    if (!hasFeature(MapFeature.MOVING_WALLS)) return
    for (w in walls) {
        if (w.horizontal) {
            w.x += w.dir * w.speed * dt
            if (w.x < w.trackFrom) {
                w.x = w.trackFrom
                w.dir = 1f
            } else if (w.x > w.trackTo) {
                w.x = w.trackTo
                w.dir = -1f
            }
        } else {
            w.y += w.dir * w.speed * dt
            if (w.y < w.trackFrom) {
                w.y = w.trackFrom
                w.dir = 1f
            } else if (w.y > w.trackTo) {
                w.y = w.trackTo
                w.dir = -1f
            }
        }
    }
}

// ------------------------------------------------------
// Crystal Cavern mirror clones
// ------------------------------------------------------
private fun BattleSim.updateMirrorClones(dt: Float) {
    if (cloneBlockTimer > 0f) cloneBlockTimer -= dt
    if (!hasFeature(MapFeature.MIRROR_CLONES)) return
    cloneTimer -= dt
    if (cloneTimer > 0f) return
    cloneTimer = Balance.CLONE_INTERVAL
    if (cloneBlockTimer > 0f) return
    val donors = npcs.filter { it.alive && !it.isClone && !it.isMinion && it.faction != FACTION_NEUTRAL }
        .shuffledWith(rng)
        .take(2)
    if (donors.isEmpty()) return
    for ((i, donor) in donors.withIndex()) {
        val clone = spawnCreature(
            type = donor.type,
            x = donor.x + rng.range(-70f, 70f),
            y = donor.y + rng.range(-70f, 70f),
            hp = donor.maxHp * Balance.CLONE_HP_FRACTION,
            damage = donor.damage * 0.8f,
            faction = FACTION_BOSS + 60 + i,
            label = "${donor.label} clone",
            isClone = true,
            radiusScale = 0.9f,
        )
        clone.attackInterval = donor.attackInterval
        clone.reach = donor.reach
    }
    addFloater(player.x, player.y - 50f, "MIRROR CLONES!", 0xFF6366F1, big = true)
    soundEvents.add(Sfx.CLONE)
}

private fun <T> List<T>.shuffledWith(rng: com.example.rann.content.LevelRandom): List<T> {
    val copy = toMutableList()
    for (i in copy.indices.reversed()) {
        val j = rng.nextInt(i + 1)
        val tmp = copy[i]
        copy[i] = copy[j]
        copy[j] = tmp
    }
    return copy
}

// ------------------------------------------------------
// Destructible crystals: smashing one stalls the clones
// ------------------------------------------------------
private fun BattleSim.updateCrystals(dt: Float) {
    if (!staticFeatures.contains(MapFeature.DESTRUCTIBLE_CRYSTALS)) return
    if (player.swings != lastSwingCount) {
        lastSwingCount = player.swings
        val arc = kotlin.math.PI.toFloat() * Balance.ATTACK_ARC_FRACTION * player.weapon.arcMult
        for (c in crystals) {
            if (!c.alive) continue
            val d = hypot(c.x - player.x, c.y - player.y)
            if (d > player.weapon.reach + 26f + player.radius) continue
            val a = kotlin.math.atan2(c.y - player.y, c.x - player.x)
            if (abs(angleDelta(a, player.facing)) > arc / 2f) continue
            c.hp -= player.weapon.damage
            addFloater(c.x, c.y - 30f, "-${player.weapon.damage.toInt()}", 0xFF6366F1)
            addFx(Fx(2, c.x, c.y, 0f, 0f, 0.25f, 0.25f, 34f, 0f, 0xFF6366F1, 0f))
            if (c.hp <= 0f) {
                c.alive = false
                cloneBlockTimer = Balance.CLONE_CRYSTAL_STALL
                cloneTimer = min(cloneTimer, Balance.CLONE_INTERVAL)
                addFx(Fx(1, c.x, c.y, 0f, 0f, 0.6f, 0.6f, 120f, 0f, 0xFF6366F1, 0f))
                addFloater(c.x, c.y - 44f, "CLONES STALLED 15s", 0xFF6366F1, big = true)
                bannerText = "Crystal shattered — cloning stalled 15s"
                bannerTimer = 2.2f
            }
        }
    }
}

// ------------------------------------------------------
// Storm Citadel lightning
// ------------------------------------------------------
private fun BattleSim.updateLightningStorm(dt: Float) {
    if (!hasFeature(MapFeature.LIGHTNING_STORM)) return
    lightningTimer -= dt
    if (lightningTimer > 0f) return
    lightningTimer = Balance.LIGHTNING_INTERVAL * (if (mods.contains(LevelModifier.FAST_STORM)) 0.75f else 1f)
    val target: Fighter = if (rng.chance(0.6f) || npcs.isEmpty()) {
        player
    } else {
        npcs.filter { it.alive && it.faction != FACTION_NEUTRAL }.randomOrNullWith(rng) ?: player
    }
    val lead = 0.6f
    val x = (target.x + target.vx * lead + rng.range(-40f, 40f)).coerceIn(50f, world - 50f)
    val y = (target.y + target.vy * lead + rng.range(-40f, 40f)).coerceIn(50f, world - 50f)
    spawnStrike(x, y, Balance.LIGHTNING_TELEGRAPH)
}

private fun <T> List<T>.randomOrNullWith(rng: com.example.rann.content.LevelRandom): T? =
    if (isEmpty()) null else this[rng.nextInt(size)]

// ------------------------------------------------------
// Jade Temple treasure shrines: stand 2s for a free power-up
// ------------------------------------------------------
private fun BattleSim.updateTreasureShrines(dt: Float) {
    if (!staticFeatures.contains(MapFeature.TREASURE_SHRINES)) return
    zones.forEachIndexed { index, zone ->
        if (zone.kind != 3) return@forEachIndexed
        val current = treasureTimers[index] ?: 0f
        val inside = hypot(player.x - zone.x, player.y - zone.y) <= zone.radius * 0.7f
        if (inside && current >= 0f) {
            val next = current + dt
            if (next >= 2f) {
                treasureTimers[index] = -10f
                if (!mods.contains(LevelModifier.NO_POWERS)) {
                    applyPower(randomPowerUp())
                    addFloater(zone.x, zone.y - 40f, "SHRINE GIFT", 0xFFD97706, big = true)
                    bannerText = "Treasure shrine granted a power-up"
                    bannerTimer = 2f
                }
            } else {
                treasureTimers[index] = next
            }
        } else if (current < 0f) {
            treasureTimers[index] = min(0f, current + dt)
        } else if (!inside) {
            treasureTimers[index] = 0f
        }
    }
}
