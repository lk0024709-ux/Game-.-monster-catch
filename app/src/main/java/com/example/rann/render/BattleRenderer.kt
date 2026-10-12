package com.example.rann.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.rann.model.Balance
import com.example.rann.model.BattleState
import com.example.rann.model.MapFeature
import com.example.rann.sim.BattleSim
import com.example.rann.sim.Chest
import com.example.rann.sim.ChimeraFighter
import com.example.rann.sim.Crystal
import com.example.rann.sim.FACTION_PLAYER
import com.example.rann.sim.Geyser
import com.example.rann.sim.NpcFighter
import com.example.rann.sim.Pickup
import com.example.rann.sim.PlayerFighter
import com.example.rann.sim.Shrine
import com.example.rann.sim.Strike
import com.example.rann.sim.Zone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

// ======================================================
// BATTLE SCENE RENDERER
// ======================================================
//
// Everything below draws in WORLD units. The camera transform is applied once
// (translate + scale), so map features stay in sync with simulation coordinates
// and hit detection matches what the player sees.

class BattleCamera {
    var x = 0f
    var y = 0f
    var zoom = 1f
    var shakeX = 0f
    var shakeY = 0f

    fun follow(
        sim: BattleSim,
        viewW: Float,
        viewH: Float,
        dt: Float,
        shakeEnabled: Boolean,
        frame: Float,
    ) {
        val p = sim.player
        val target = (min(viewW, viewH) / Balance.CAMERA_REFERENCE)
            .coerceIn(Balance.CAMERA_MIN_ZOOM, Balance.CAMERA_MAX_ZOOM)
        zoom += (target - zoom) * 0.12f

        val halfW = viewW / 2f / zoom
        val halfH = viewH / 2f / zoom
        val leadX = p.x + cos(p.facing) * 18f + p.vx * 0.12f
        val leadY = p.y + sin(p.facing) * 18f + p.vy * 0.12f

        val maxStep = Balance.CAMERA_MAX_SPEED * dt.coerceIn(0f, 0.05f)
        val dx = leadX - x
        val dy = leadY - y
        val dist = hypot(dx, dy)
        if (dist <= maxStep || dist < 0.001f) {
            x = leadX
            y = leadY
        } else {
            x += dx / dist * maxStep
            y += dy / dist * maxStep
        }

        x = if (sim.world <= halfW * 2f) sim.world / 2f else x.coerceIn(halfW, sim.world - halfW)
        y = if (sim.world <= halfH * 2f) sim.world / 2f else y.coerceIn(halfH, sim.world - halfH)

        if (shakeEnabled && sim.screenShake > 0.01f) {
            val amp = sim.screenShake * clamp01(sim.screenShake) * 13f
            shakeX = sin(frame * 2.7f) * amp
            shakeY = cos(frame * 3.4f) * amp * 0.8f
        } else {
            shakeX = 0f
            shakeY = 0f
        }
    }

    fun worldToScreen(wx: Float, wy: Float, viewW: Float, viewH: Float): Offset =
        Offset((wx - x) * zoom + viewW / 2f + shakeX, (wy - y) * zoom + viewH / 2f + shakeY)

    fun visibleBounds(viewW: Float, viewH: Float): Rect {
        val halfW = viewW / 2f / zoom
        val halfH = viewH / 2f / zoom
        return Rect(x - halfW, y - halfH, x + halfW, y + halfH)
    }
}

data class RenderFlags(
    val showFloaters: Boolean = true,
    val shake: Boolean = true,
)

private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)

private fun fade(life: Float, max: Float): Float = clamp01(life / max)

// ------------------------------------------------------
// MAIN ENTRY
// ------------------------------------------------------
fun DrawScope.drawBattleScene(
    sim: BattleSim,
    cam: BattleCamera,
    time: Float,
    flags: RenderFlags,
) {
    val w = size.width
    val h = size.height
    val map = sim.map

    drawRect(color = Color(map.sky))

    withTransform({
        translate(left = w / 2f + cam.shakeX, top = h / 2f + cam.shakeY)
        scale(scaleX = cam.zoom, scaleY = cam.zoom, pivot = Offset.Zero)
        translate(left = -cam.x, top = -cam.y)
    }) {
        val viewHalfW = w / 2f / cam.zoom
        val viewHalfH = h / 2f / cam.zoom
        val left = cam.x - viewHalfW
        val top = cam.y - viewHalfH
        val right = cam.x + viewHalfW
        val bottom = cam.y + viewHalfH

        // ground + decorations (clipped to view + margin)
        clipRect(
            left = left - 60f,
            top = top - 60f,
            right = right + 60f,
            bottom = bottom + 60f,
        ) {
            drawGround(Color(map.ground), Color(map.groundAlt), left, top, right, bottom, sim)
            drawDecorations(sim, left, top, right, bottom, time)
            drawWorldBorder(sim)
        }

        // world-space props and hazards
        for (zone in sim.zones) drawZone(zone, time)
        for (wall in sim.walls) drawMovingWall(wall)
        for (crystal in sim.crystals) {
            drawFighterShadowAt(crystal.x, crystal.y + 4f, 20f)
            drawCrystal(crystal, time)
        }
        for (chest in sim.chests) drawChestVisual(chest.x, chest.y, time)
        for (shrine in sim.shrines) drawShrine(shrine, sim.player.shrineCd > 0f, time)
        for (pickup in sim.pickups) drawPickupVisual(pickup.x, pickup.y, pickup.type.icon, Color(pickup.type.color), time)
        for (geyser in sim.geysers) drawGeyser(geyser, time)
        for (sanctuary in sim.sanctuaries) drawSanctuaryDome(Offset(sanctuary.x, sanctuary.y), Balance.SANCTUARY_RADIUS, sanctuary.life, time)
        for (strike in sim.strikes) drawStrike(strike, time)

        // fighters, painted back to front for a faux-3D feel
        val fighters = ArrayList<Any>(sim.npcs.size + 2)
        fighters.add(sim.player)
        fighters.addAll(sim.npcs)
        sim.chimera?.let { fighters.add(it) }
        val sorted = fighters.sortedBy { f ->
            when (f) {
                is PlayerFighter -> f.y
                is NpcFighter -> f.y
                is ChimeraFighter -> f.y
                else -> 0f
            }
        }
        for (f in sorted) {
            when (f) {
                is PlayerFighter -> {
                    if (!f.alive) continue
                    drawFighterShadow(f)
                    drawWarrior(f, time)
                    drawHealthBar(f, width = 54f, yOff = 16f, showAlways = false)
                }
                is NpcFighter -> {
                    if (!f.alive) continue
                    drawFighterShadowAt(f.x, f.y, f.radius)
                    drawCreature(f, time)
                    drawHealthBar(
                        f,
                        width = f.radius * 2.2f,
                        yOff = 14f,
                        showAlways = f.boss != null,
                    )
                    if (f.boss != null) {
                        worldText(
                            text = f.boss?.name ?: "Boss",
                            x = f.x,
                            y = f.y - f.radius - 30f,
                            size = 22f,
                            color = Color(0xFFF5F1EA),
                            bold = true,
                        )
                    }
                }
                is ChimeraFighter -> {
                    if (!f.alive) continue
                    drawFighterShadowAt(f.x, f.y, f.radius)
                    drawChimera(f, time)
                    drawHealthBar(f, width = f.radius * 2.4f, yOff = 26f, showAlways = false)
                }
            }
        }

        // projectiles
        for (p in sim.projectiles) {
            val dir = if (hypot(p.vx, p.vy) > 0.01f) atan2(p.vy, p.vx) else 0f
            drawLine(
                color = Color(p.color).copy(alpha = 0.35f),
                start = Offset(p.x - cos(dir) * 26f, p.y - sin(dir) * 26f),
                end = Offset(p.x, p.y),
                strokeWidth = p.radius * 1.4f,
            )
            drawCircle(color = Color(p.color), radius = p.radius, center = Offset(p.x, p.y))
            drawCircle(
                color = Color(0x99FFFFFF),
                radius = p.radius * 0.45f,
                center = Offset(p.x, p.y),
            )
        }

        // one-shot effects
        for (e in sim.fx) {
            val a = fade(e.life, e.maxLife)
            val col = Color(e.color)
            when (e.kind) {
                0 -> {
                    // slash arc
                    drawArc(
                        color = col.copy(alpha = a * 0.9f),
                        startAngle = Math.toDegrees((e.angle - e.arc / 2f).toDouble()).toFloat(),
                        sweepAngle = Math.toDegrees(e.arc.toDouble()).toFloat(),
                        useCenter = false,
                        topLeft = Offset(e.x - e.radius, e.y - e.radius),
                        size = Size(e.radius * 2f, e.radius * 2f),
                        style = Stroke(width = 7f + 9f * a),
                    )
                }
                1 -> {
                    // expanding ring
                    val r = e.radius * (1f - a * 0.55f)
                    drawCircle(
                        color = col.copy(alpha = a * 0.8f),
                        radius = r,
                        center = Offset(e.x, e.y),
                        style = Stroke(width = 4f + 6f * a),
                    )
                }
                2 -> {
                    // burst
                    val r = e.radius * (0.55f + 0.45f * (1f - a))
                    drawCircle(color = col.copy(alpha = a * 0.55f), radius = r, center = Offset(e.x, e.y))
                    drawCircle(
                        color = col.copy(alpha = a * 0.9f),
                        radius = r,
                        center = Offset(e.x, e.y),
                        style = Stroke(width = 3f),
                    )
                }
                3 -> {
                    // travelling bolt
                    drawLine(
                        color = col.copy(alpha = 0.75f * a + 0.25f),
                        start = Offset(e.x, e.y),
                        end = Offset(e.x2, e.y2),
                        strokeWidth = e.radius * 0.5f + 3f,
                    )
                    drawCircle(color = Color(0xCCFFFFFF), radius = e.radius * 0.32f, center = Offset(e.x2, e.y2))
                    drawCircle(color = col.copy(alpha = a), radius = e.radius * 0.6f, center = Offset(e.x2, e.y2))
                }
                4 -> {
                    // directional flash
                    val ang = atan2(e.y2 - e.y, e.x2 - e.x)
                    rotate(degrees = Math.toDegrees(ang.toDouble()).toFloat(), pivot = Offset(e.x, e.y)) {
                        drawArc(
                            color = col.copy(alpha = a * 0.7f),
                            startAngle = -28f,
                            sweepAngle = 56f,
                            useCenter = true,
                            topLeft = Offset(e.x, e.y - e.radius),
                            size = Size(e.radius * 2f, e.radius * 2f),
                        )
                    }
                }
            }
        }

        // storm tint above the field, below floating text
        drawStorm(sim, time)

        if (flags.showFloaters) {
            for (ft in sim.floaters) {
                val a = clamp01(ft.life / 0.95f)
                worldText(
                    text = ft.text,
                    x = ft.x,
                    y = ft.y,
                    size = if (ft.big) 26f else 19f,
                    color = Color(ft.color).copy(alpha = a),
                    bold = ft.big,
                )
            }
        }
    }

    drawScreenVignettes(sim, w, h, time)
}

// ------------------------------------------------------
// GROUND
// ------------------------------------------------------
private fun DrawScope.drawGround(
    ground: Color,
    alt: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    sim: BattleSim,
) {
    drawRect(
        color = ground,
        topLeft = Offset(0f, 0f),
        size = Size(sim.world, sim.world),
    )
    // soft patchwork of lighter ground
    val cell = 260f
    var gy = (top / cell).toInt() * cell - cell
    while (gy < bottom + cell) {
        var gx = (left / cell).toInt() * cell - cell
        while (gx < right + cell) {
            val h = hash2(gx.toInt(), gy.toInt())
            if (h % 3 == 0) {
                val s = cell * (0.45f + (h % 5) * 0.08f)
                drawCircle(
                    color = alt.copy(alpha = 0.55f),
                    radius = s * 0.5f,
                    center = Offset(gx + cell * 0.5f, gy + cell * 0.5f),
                )
            }
            gx += cell
        }
        gy += cell
    }
}

private fun DrawScope.drawDecorations(
    sim: BattleSim,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    time: Float,
) {
    val cell = 190f
    val accent = Color(sim.map.accent)
    var gy = (top / cell).toInt() * cell - cell
    while (gy < bottom + cell) {
        var gx = (left / cell).toInt() * cell - cell
        while (gx < right + cell) {
            val h = hash2(gx.toInt(), gy.toInt())
            val cx = gx + ((h % 100) / 100f) * cell
            val cy = gy + (((h / 100) % 100) / 100f) * cell
            drawTileDecor(sim.map.index, cx, cy, h, accent, time)
            gx += cell
        }
        gy += cell
    }
}

private fun DrawScope.drawTileDecor(
    mapIndex: Int,
    x: Float,
    y: Float,
    h: Int,
    accent: Color,
    time: Float,
) {
    when (mapIndex) {
        0 -> {
            // Dust Pit — dead shrubs and bleached bones
            if (h % 4 == 0) {
                drawLine(accent.copy(alpha = 0.5f), Offset(x, y), Offset(x + 12f, y - 16f), strokeWidth = 3f)
                drawLine(accent.copy(alpha = 0.4f), Offset(x, y), Offset(x - 11f, y - 13f), strokeWidth = 3f)
            }
            if (h % 7 == 0) {
                drawCircle(Color(0x66F5F1EA), radius = 3.2f, center = Offset(x + 22f, y + 14f))
                drawCircle(Color(0x66F5F1EA), radius = 2.4f, center = Offset(x + 30f, y + 18f))
            }
        }
        1 -> {
            // Frost Hollow — pines
            if (h % 3 == 0) {
                val size = 16f + (h % 5) * 3f
                val pine = Path().apply {
                    moveTo(x, y - size)
                    lineTo(x + size * 0.6f, y + size * 0.4f)
                    lineTo(x - size * 0.6f, y + size * 0.4f)
                    close()
                }
                drawPath(pine, Color(0xFF3F6212).copy(alpha = 0.75f))
                drawRect(
                    color = Color(0xFF6B4F2A),
                    topLeft = Offset(x - 2f, y + size * 0.4f),
                    size = Size(4f, 8f),
                )
            }
        }
        2 -> {
            // Ember Forge — basalt shards with embers
            if (h % 3 == 0) {
                val s = 12f + (h % 4) * 5f
                drawRoundRect(
                    color = Color(0xFF2E2620),
                    topLeft = Offset(x - s * 0.5f, y - s * 0.35f),
                    size = Size(s, s * 0.7f),
                    cornerRadius = CornerRadius(5f, 5f),
                )
                val glow = 0.35f + 0.3f * sin(time * 1.5f + h)
                drawCircle(Color(0xFFC2410C).copy(alpha = glow), radius = 4.5f, center = Offset(x, y))
            }
        }
        3 -> {
            // Jade Temple — tiles and gold veins
            drawRect(
                color = Color(0xFFB6CCB2).copy(alpha = 0.35f),
                topLeft = Offset(x - 34f, y - 34f),
                size = Size(68f, 68f),
                style = Stroke(width = 2f),
            )
            if (h % 5 == 0) {
                drawLine(accent.copy(alpha = 0.5f), Offset(x - 20f, y), Offset(x + 20f, y + 6f), strokeWidth = 2f)
            }
        }
        4 -> {
            // Crystal Cavern — shaded rock with shards
            drawCircle(Color(0xFF3B2B52).copy(alpha = 0.35f), radius = 30f + (h % 4) * 6f, center = Offset(x, y))
            if (h % 3 == 0) {
                val s = 10f + (h % 5) * 4f
                val shard = Path().apply {
                    moveTo(x, y - s)
                    lineTo(x + s * 0.45f, y)
                    lineTo(x, y + s * 0.6f)
                    lineTo(x - s * 0.45f, y)
                    close()
                }
                drawPath(shard, Color(0xFF6366F1).copy(alpha = 0.55f))
            }
        }
        5 -> {
            // Storm Citadel — flagstones with cracks
            drawRect(
                color = Color(0x33000000),
                topLeft = Offset(x - 40f, y - 40f),
                size = Size(80f, 80f),
                style = Stroke(width = 2f),
            )
            if (h % 4 == 0) {
                drawLine(Color(0x55F5F1EA), Offset(x - 16f, y + 12f), Offset(x + 18f, y - 14f), strokeWidth = 1.6f)
            }
        }
        else -> {
            // Throne of Rann — obsidian with gold veins
            drawCircle(Color(0x332A231C), radius = 44f, center = Offset(x, y))
            if (h % 3 == 0) {
                drawLine(Color(0x66E0A82E), Offset(x - 24f, y - 10f), Offset(x + 26f, y + 12f), strokeWidth = 2f)
                drawLine(Color(0x44E0A82E), Offset(x + 6f, y - 26f), Offset(x + 12f, y + 24f), strokeWidth = 1.6f)
            }
        }
    }
}

private fun DrawScope.drawWorldBorder(sim: BattleSim) {
    drawRect(
        color = Color(0x88B45309),
        topLeft = Offset.Zero,
        size = Size(sim.world, sim.world),
        style = Stroke(width = 14f),
    )
}

private fun hash2(a: Int, b: Int): Int {
    var h = a * 374761393 + b * 668265263
    h = (h xor (h shr 13)) * 1274126177
    return h and 0x7FFFFFFF
}

// ------------------------------------------------------
// HAZARDS & PROPS
// ------------------------------------------------------
private fun DrawScope.drawZone(zone: Zone, time: Float) {
    val center = Offset(zone.x, zone.y)
    when (zone.kind) {
        0 -> {
            // lava
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B), Color(0xFFC2410C), Color(0xFF7C2D12)),
                    center = center,
                    radius = zone.radius,
                ),
                radius = zone.radius,
                center = center,
            )
            val bubbles = 3
            for (i in 0 until bubbles) {
                val a = time * 0.9f + i * 2.1f
                val bx = zone.x + cos(a) * zone.radius * 0.45f
                val by = zone.y + sin(a * 1.3f) * zone.radius * 0.45f
                drawCircle(Color(0x66FEF3C7), radius = 5f + 3f * sin(a * 2f), center = Offset(bx, by))
            }
            drawCircle(Color(0x88B91C1C), radius = zone.radius, center = center, style = Stroke(width = 4f))
        }
        1 -> {
            // ice
            drawCircle(Color(0x66BFDBFE), radius = zone.radius, center = center)
            drawCircle(Color(0x88E0F2FE), radius = zone.radius, center = center, style = Stroke(width = 3f))
            drawLine(
                Color(0x55FFFFFF),
                Offset(zone.x - zone.radius * 0.5f, zone.y - zone.radius * 0.2f),
                Offset(zone.x + zone.radius * 0.35f, zone.y + zone.radius * 0.4f),
                strokeWidth = 2f,
            )
        }
        2 -> {
            // heat zone
            drawCircle(Color(0x44F59E0B), radius = zone.radius, center = center)
            drawCircle(Color(0x66D97706), radius = zone.radius, center = center, style = Stroke(width = 3f))
        }
        else -> {
            // treasure shrine
            drawCircle(Color(0x33E0A82E), radius = zone.radius, center = center)
            drawCircle(Color(0xFFE0A82E), radius = zone.radius * 0.55f, center = center, style = Stroke(width = 3f))
            drawCircle(Color(0xCCF3D9A4), radius = 12f + 2f * sin(time * 3f), center = center)
            worldText("💎", zone.x, zone.y + 8f, 26f, Color.White)
        }
    }
}

private fun DrawScope.drawGeyser(g: Geyser, time: Float) {
    val center = Offset(g.x, g.y)
    if (g.state == 0) {
        val pulse = 0.5f + 0.5f * sin(time * 6f)
        drawCircle(Color(0x33C2410C), radius = 76f, center = center)
        drawDashedCircle(center, 76f, Color(0xFFC2410C).copy(alpha = 0.5f + 0.4f * pulse), 3f, floatArrayOf(12f, 10f))
    } else {
        val t = clamp01(g.stateTimer / 0.9f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE4A3), Color(0x99C2410C), Color(0x00C2410C)),
                center = center,
                radius = 92f * (0.6f + (1f - t) * 0.6f),
            ),
            radius = 92f * (0.6f + (1f - t) * 0.6f),
            center = center,
        )
    }
}

private fun DrawScope.drawStrike(strike: Strike, time: Float) {
    val center = Offset(strike.x, strike.y)
    val t = clamp01(1f - strike.telegraph / Balance.LIGHTNING_TELEGRAPH)
    drawCircle(Color(0x22D97706), radius = strike.radius, center = center)
    drawCircle(
        color = Color(0xFFD97706).copy(alpha = 0.35f + 0.45f * t),
        radius = strike.radius,
        center = center,
        style = Stroke(width = 3f),
    )
    drawCircle(Color(0x55B91C1C), radius = strike.radius * t, center = center)
    worldText("⚡", strike.x, strike.y + 8f, 30f, Color(0xFFFFFFFF))
}

private fun DrawScope.drawShrine(shrine: Shrine, cooldown: Boolean, time: Float) {
    val center = Offset(shrine.x, shrine.y)
    drawShrineGlow(center, 62f, time, cooldown)
    // small stone totem
    drawRoundRect(
        color = Color(0xFF78716C),
        topLeft = Offset(shrine.x - 11f, shrine.y - 26f),
        size = Size(22f, 24f),
        cornerRadius = CornerRadius(6f, 6f),
    )
    drawCircle(
        color = if (cooldown) Color(0xFF78716C) else Color(0xFF15803D),
        radius = 9f,
        center = Offset(shrine.x, shrine.y - 28f),
    )
    drawCircle(Color(0x99FFFFFF), radius = 3.5f, center = Offset(shrine.x - 3f, shrine.y - 31f))
}

// ------------------------------------------------------
// STORM (safe zone)
// ------------------------------------------------------
private fun DrawScope.drawStorm(sim: BattleSim, time: Float) {
    if (sim.setup.isElite) return
    val storm = sim.storm
    val center = Offset(storm.cx, storm.cy)
    val radius = storm.radius
    if (radius > sim.world * 1.6f) return

    val outside = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(-2000f, -2000f, sim.world + 2000f, sim.world + 2000f))
        addOval(Rect(center, radius, center, radius))
    }
    drawPath(outside, Color(0x33B45309))

    val danger = storm.dps / 30f
    if (danger > 0.02f) {
        drawPath(outside, Color(0xFFB91C1C).copy(alpha = 0.10f + 0.22f * danger))
    }

    val pulse = 0.55f + 0.45f * sin(time * 2.2f)
    drawDashedCircle(center, radius, Color(0xFFB91C1C).copy(alpha = 0.55f + 0.3f * pulse), 5f, floatArrayOf(26f, 18f))

    if (storm.shrinkT > 0f && storm.toRadius < storm.fromRadius) {
        drawCircle(
            color = Color(0x66B91C1C),
            radius = storm.toRadius,
            center = center,
            style = Stroke(width = 2f),
        )
    }
}

// ------------------------------------------------------
// SCREEN-SPACE OVERLAYS
// ------------------------------------------------------
private fun DrawScope.drawScreenVignettes(sim: BattleSim, w: Float, h: Float, time: Float) {
    // storm fog / darkness / outside-zone warnings
    if (sim.fogActive) {
        drawRect(color = Color(0x66DCC79C))
    }
    if (sim.player.vision < 1f) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x00000000), Color(0xB31C1917)),
                center = Offset(w / 2f, h / 2f),
                radius = min(w, h) * (0.42f * sim.player.vision + 0.12f),
            )
        )
    }
    if (sim.isPlayerOutsideZone()) {
        val pulse = 0.35f + 0.25f * sin(time * 5f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x00B91C1C), Color(0xFFB91C1C).copy(alpha = pulse)),
                center = Offset(w / 2f, h / 2f),
                radius = min(w, h) * 0.72f,
            )
        )
    }
    // gentle top/bottom shading so the HUD reads
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0x551C1917),
            0.22f to Color(0x001C1917),
            0.78f to Color(0x001C1917),
            1f to Color(0x661C1917),
        ),
        size = Size(w, h),
    )
}

// ------------------------------------------------------
// MINIMAP
// ------------------------------------------------------
fun DrawScope.drawMinimap(sim: BattleSim) {
    val pad = 8f
    val avail = min(size.width, size.height) - pad * 2f
    val scale = avail / sim.world
    val ox = (size.width - sim.world * scale) / 2f
    val oy = (size.height - sim.world * scale) / 2f

    fun mapX(v: Float) = ox + v * scale
    fun mapY(v: Float) = oy + v * scale

    drawRect(
        color = Color(sim.map.groundAlt),
        topLeft = Offset(ox, oy),
        size = Size(sim.world * scale, sim.world * scale),
    )

    // storm circle
    if (!sim.setup.isElite && sim.storm.radius < sim.world) {
        drawCircle(
            color = Color(0x55B91C1C),
            radius = sim.storm.radius * scale,
            center = Offset(mapX(sim.storm.cx), mapY(sim.storm.cy)),
            style = Stroke(width = 2f),
        )
    }

    for (shrine in sim.shrines) drawCircle(Color(0xFF15803D), radius = 3f, center = Offset(mapX(shrine.x), mapY(shrine.y)))
    for (chest in sim.chests) drawCircle(Color(0xFFD97706), radius = 2.5f, center = Offset(mapX(chest.x), mapY(chest.y)))
    for (crystal in sim.crystals) if (crystal.alive) {
        drawCircle(Color(0xFF6366F1), radius = 2.5f, center = Offset(mapX(crystal.x), mapY(crystal.y)))
    }
    for (npc in sim.npcs) {
        if (!npc.alive) continue
        drawCircle(
            color = if (npc.boss != null) Color(0xFF7C2D12) else Color(0xFFB45309),
            radius = if (npc.boss != null) 4.5f else 3f,
            center = Offset(mapX(npc.x), mapY(npc.y)),
        )
    }
    sim.chimera?.let { c ->
        if (c.alive) drawCircle(Color(0xFFB91C1C), radius = 5.5f, center = Offset(mapX(c.x), mapY(c.y)))
    }
    // player with a facing tick
    val px = mapX(sim.player.x)
    val py = mapY(sim.player.y)
    drawCircle(Color(0xFF1C1917), radius = 4.5f, center = Offset(px, py))
    drawLine(
        color = Color(0xFFD97706),
        start = Offset(px, py),
        end = Offset(px + cos(sim.player.facing) * 9f, py + sin(sim.player.facing) * 9f),
        strokeWidth = 2.5f,
    )
    drawRect(
        color = Color(0x66F5F1EA),
        topLeft = Offset(ox, oy),
        size = Size(sim.world * scale, sim.world * scale),
        style = Stroke(width = 1.5f),
    )
}

fun DrawScope.drawWindArrow(angle: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val len = min(size.width, size.height) * 0.34f
    rotate(degrees = Math.toDegrees(angle.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
        drawLine(
            color = Color(0xFFF3D9A4),
            start = Offset(cx - len, cy),
            end = Offset(cx + len, cy),
            strokeWidth = 3.5f,
        )
        val head = Path().apply {
            moveTo(cx + len + 6f, cy)
            lineTo(cx + len - 6f, cy - 7f)
            lineTo(cx + len - 6f, cy + 7f)
            close()
        }
        drawPath(head, Color(0xFFF3D9A4))
    }
}

// kept for callers that highlight the fighter throwing an ability
internal fun DrawScope.drawFactionRing(x: Float, y: Float, radius: Float, faction: Int) {
    val color = if (faction == FACTION_PLAYER) Color(0xFFD97706) else Color(0xFFB91C1C)
    drawCircle(color = color.copy(alpha = 0.5f), radius = radius, center = Offset(x, y), style = Stroke(width = 2f))
}
