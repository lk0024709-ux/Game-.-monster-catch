package com.example.rann.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.rann.model.ChimeraType
import com.example.rann.model.CreatureType
import com.example.rann.sim.ChimeraFighter
import com.example.rann.sim.Fighter
import com.example.rann.sim.NpcFighter
import com.example.rann.sim.PlayerFighter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ======================================================
// PROCEDURAL CANVAS ART — every creature is layered shapes.
// No sprite assets anywhere in the game.
// ======================================================

private val textPaint = Paint().apply {
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
}

internal fun DrawScope.worldText(
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    bold: Boolean = false,
    align: Paint.Align = Paint.Align.CENTER,
) {
    val canvas = drawContext.canvas.nativeCanvas
    textPaint.color = color.toArgb()
    textPaint.textSize = size
    textPaint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    textPaint.textAlign = align
    val save = canvas.save()
    canvas.drawText(text, x, y, textPaint)
    canvas.restoreToCount(save)
}

/** Hue-rotated faction tint so every creature on the field is distinguishable. */
private fun factionTint(base: Long, faction: Int): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toInt(), hsv)
    hsv[0] = (hsv[0] + (faction * 41f)) % 360f
    hsv[1] = (hsv[1] * 0.95f).coerceIn(0.25f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun longColor(v: Long): Color = Color(v)

private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)

/** Flash the body towards warm cream (never pure white). */
private fun blended(base: Color, flash: Float): Color =
    if (flash <= 0f) base else androidx.compose.ui.graphics.lerp(base, Color(0xFFF5F1EA), clamp01(flash * 4.5f) * 0.55f)

// ------------------------------------------------------
// GROUND SHADOW + HEALTH BAR
// ------------------------------------------------------
internal fun DrawScope.drawFighterShadow(f: Fighter) {
    drawOval(
        color = Color(0x26000000),
        topLeft = Offset(f.x - f.radius * 1.05f, f.y + f.radius * 0.42f),
        size = Size(f.radius * 2.1f, f.radius * 0.78f),
    )
}

internal fun DrawScope.drawHealthBar(f: Fighter, width: Float, yOff: Float, showAlways: Boolean = false) {
    if (!showAlways && f.hpFraction >= 0.999f) return
    val w = width
    val h = 6f
    val left = f.x - w / 2f
    val top = f.y - f.radius - yOff
    drawRoundRect(
        color = Color(0x33000000),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(h / 2f),
    )
    val frac = f.hpFraction
    drawRoundRect(
        color = when {
            frac > 0.6f -> Color(0xFF15803D)
            frac > 0.3f -> Color(0xFFD97706)
            else -> Color(0xFFB91C1C)
        },
        topLeft = Offset(left + 1f, top + 1f),
        size = Size((w - 2f) * frac, h - 2f),
        cornerRadius = CornerRadius((h - 2f) / 2f),
    )
}

// ------------------------------------------------------
// THE WARRIOR
// ------------------------------------------------------
internal fun DrawScope.drawWarrior(p: PlayerFighter, elapsed: Float) {
    val body = Color(0xFF3F6212)
    val skin = Color(0xFFE7C9A6)
    val metal = Color(0xFFB7B2A6)
    val accent = Color(0xFF7C2D12)
    val r = p.radius

    drawFighterShadow(p)

    rotate(degrees = Math.toDegrees(p.facing.toDouble()).toFloat(), pivot = Offset(p.x, p.y)) {
        // cloak
        drawOval(
            color = accent,
            topLeft = Offset(p.x - r * 1.15f, p.y - r * 0.95f),
            size = Size(r * 2.3f, r * 1.9f),
        )
        // torso
        drawCircle(color = blended(body, p.hitFlash), radius = r, center = Offset(p.x, p.y))
        // shoulder plate
        drawCircle(color = metal, radius = r * 0.42f, center = Offset(p.x + r * 0.15f, p.y - r * 0.42f))
        // head + helm
        drawCircle(color = skin, radius = r * 0.44f, center = Offset(p.x + r * 0.5f, p.y))
        drawArc(
            color = metal,
            startAngle = -140f,
            sweepAngle = 280f,
            useCenter = true,
            topLeft = Offset(p.x + r * 0.06f, p.y - r * 0.46f),
            size = Size(r * 0.88f, r * 0.92f),
        )
    }

    // sword: sweeps through the arc while swinging
    val swingT = 1f - clamp01(p.swingAnim / 0.22f)
    if (p.swingAnim > 0f) {
        val arc = 2.6f
        val a = p.swingDir - arc / 2f + arc * swingT
        drawSword(p.x, p.y, a, r + p.weapon.reach * 0.8f, Color(0xFFEDE7DB))
        // slash ghost
        drawArc(
            color = Color(0x55F3D9A4),
            startAngle = Math.toDegrees((p.swingDir - arc / 2f).toDouble()).toFloat(),
            sweepAngle = Math.toDegrees((arc * swingT).toDouble()).toFloat(),
            useCenter = true,
            topLeft = Offset(p.x - p.weapon.reach, p.y - p.weapon.reach),
            size = Size(p.weapon.reach * 2f, p.weapon.reach * 2f),
        )
    } else {
        drawSword(p.x, p.y, p.facing - 0.5f, r + p.weapon.reach * 0.55f, Color(0xFFEDE7DB))
    }
}

private fun DrawScope.drawSword(x: Float, y: Float, angle: Float, length: Float, blade: Color) {
    val tipX = x + cos(angle) * length
    val tipY = y + sin(angle) * length
    drawLine(color = Color(0xFF6B4A2A), start = Offset(x, y), end = Offset(x + cos(angle) * length * 0.22f, y + sin(angle) * length * 0.22f), strokeWidth = 7f)
    drawLine(color = blade, start = Offset(x + cos(angle) * length * 0.2f, y + sin(angle) * length * 0.2f), end = Offset(tipX, tipY), strokeWidth = 5f)
}

// ------------------------------------------------------
// WILD CREATURES
// ------------------------------------------------------
internal fun DrawScope.drawCreature(n: NpcFighter, time: Float) {
    val base = if (n.boss != null) longColor(n.boss!!.creature.color) else longColor(n.type.color)
    val body = factionTint(base, n.faction)
    val bodyColor = blended(body, n.hitFlash)
    val dark = androidx.compose.ui.graphics.lerp(bodyColor, Color(0xFF1C1917), 0.35f)
    val light = androidx.compose.ui.graphics.lerp(bodyColor, Color(0xFFF5F1EA), 0.28f)
    val r = n.radius
    val alpha = if (n.isClone) 0.62f else 1f

    drawFighterShadow(n)

    // elite / boss halo
    if (n.elite || n.boss != null) {
        drawCircle(
            color = Color(0x33D97706),
            radius = r * 1.5f,
            center = Offset(n.x, n.y),
        )
    }
    if (n.state == 1) {
        // charge telegraph
        drawArc(
            color = Color(0x99B91C1C),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(n.x - r * 2.2f, n.y - r * 2.2f),
            size = Size(r * 4.4f, r * 4.4f),
            style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
        )
    }

    val facingDeg = Math.toDegrees(n.facing.toDouble()).toFloat()
    val bob = sin(time * 3f + n.faction) * (r * 0.08f)

    rotate(degrees = facingDeg, pivot = Offset(n.x, n.y)) {
        when (n.type) {
            CreatureType.GRIFFIN -> {
                // wings
                val flap = sin(time * 9f + n.faction) * 0.25f
                drawOval(light, Offset(n.x - r * 1.7f, n.y - r * 0.55f - flap * r), Size(r * 1.5f, r * 1.1f))
                drawOval(light, Offset(n.x + r * 0.2f, n.y - r * 0.55f + flap * r), Size(r * 1.5f, r * 1.1f))
                drawOval(bodyColor, Offset(n.x - r, n.y - r * 0.72f), Size(r * 2f, r * 1.45f))
                drawCircle(dark, radius = r * 0.55f, center = Offset(n.x + r * 0.85f, n.y - r * 0.1f + bob))
                // beak
                val beak = Path().apply {
                    moveTo(n.x + r * 1.35f, n.y - r * 0.15f + bob)
                    lineTo(n.x + r * 1.9f, n.y + r * 0.05f + bob)
                    lineTo(n.x + r * 1.3f, n.y + r * 0.25f + bob)
                    close()
                }
                drawPath(beak, light)
                // tail
                drawLine(dark, Offset(n.x - r * 0.9f, n.y + r * 0.3f), Offset(n.x - r * 1.8f, n.y + r * 0.75f), strokeWidth = 5f)
            }
            CreatureType.DRAKE -> {
                drawOval(bodyColor, Offset(n.x - r * 1.25f, n.y - r * 0.6f), Size(r * 2.1f, r * 1.2f))
                drawOval(light, Offset(n.x - r * 0.2f, n.y - r * 0.45f), Size(r * 0.9f, r * 0.9f))
                // neck + head
                val neck = Path().apply {
                    moveTo(n.x + r * 0.5f, n.y - r * 0.25f)
                    lineTo(n.x + r * 1.5f, n.y - r * 0.55f + bob)
                    lineTo(n.x + r * 1.6f, n.y + r * 0.1f + bob)
                    lineTo(n.x + r * 0.5f, n.y + r * 0.3f)
                    close()
                }
                drawPath(neck, bodyColor)
                drawCircle(dark, radius = r * 0.45f, center = Offset(n.x + r * 1.75f, y = n.y - r * 0.2f + bob))
                // horn + tail
                drawLine(light, Offset(n.x + r * 1.7f, n.y - r * 0.55f + bob), Offset(n.x + r * 2.1f, n.y - r * 0.95f + bob), strokeWidth = 4f)
                drawLine(dark, Offset(n.x - r * 1.2f, n.y), Offset(n.x - r * 2.2f, n.y + r * 0.6f), strokeWidth = 6f)
                // smoulder
                drawCircle(Color(0x66C2410C), radius = r * 0.3f, center = Offset(n.x + r * 2.15f, n.y + r * 0.1f + bob))
            }
            CreatureType.SKYHAWK -> {
                val flap = sin(time * 12f + n.faction) * 0.35f
                val wing = Path().apply {
                    moveTo(n.x, n.y)
                    lineTo(n.x - r * 1.4f, n.y - r * 1.1f - flap * r)
                    lineTo(n.x - r * 2.2f, n.y + r * 0.1f - flap * r * 0.4f)
                    close()
                }
                val wing2 = Path().apply {
                    moveTo(n.x, n.y)
                    lineTo(n.x + r * 1.4f, n.y - r * 1.1f - flap * r)
                    lineTo(n.x + r * 2.2f, n.y + r * 0.1f - flap * r * 0.4f)
                    close()
                }
                drawPath(wing, light)
                drawPath(wing2, light)
                drawOval(bodyColor, Offset(n.x - r * 0.85f, n.y - r * 0.6f), Size(r * 1.7f, r * 1.3f))
                drawCircle(dark, radius = r * 0.4f, center = Offset(n.x + r * 0.8f, n.y + bob))
                drawLine(light, Offset(n.x + r * 1.05f, n.y + bob), Offset(n.x + r * 1.5f, n.y + r * 0.15f + bob), strokeWidth = 4f)
                drawLine(dark, Offset(n.x - r * 0.7f, n.y + r * 0.3f), Offset(n.x - r * 1.5f, n.y + r * 0.65f), strokeWidth = 5f)
            }
            CreatureType.IRONBOAR -> {
                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(n.x - r * 1.15f, n.y - r * 0.85f),
                    size = Size(r * 2.1f, r * 1.7f),
                    cornerRadius = CornerRadius(r * 0.7f),
                )
                // armour plates
                drawLine(dark, Offset(n.x - r * 0.6f, n.y - r * 0.7f), Offset(n.x - r * 0.6f, n.y + r * 0.7f), strokeWidth = 3f)
                drawLine(dark, Offset(n.x + r * 0.2f, n.y - r * 0.75f), Offset(n.x + r * 0.2f, n.y + r * 0.75f), strokeWidth = 3f)
                drawCircle(light, radius = r * 0.6f, center = Offset(n.x + r * 1.15f, n.y + bob))
                // tusks
                val tusk = Path().apply {
                    moveTo(n.x + r * 1.5f, n.y - r * 0.2f)
                    lineTo(n.x + r * 2.1f, n.y - r * 0.55f)
                    lineTo(n.x + r * 1.55f, n.y - r * 0.05f)
                    close()
                }
                drawPath(tusk, Color(0xFFF5F1EA))
                drawLine(dark, Offset(n.x - r * 1.1f, n.y - r * 0.3f), Offset(n.x - r * 1.7f, n.y - r * 0.7f), strokeWidth = 5f)
            }
            CreatureType.VENOMTAIL -> {
                // coiled body: three decreasing circles
                drawCircle(bodyColor, radius = r * 0.95f, center = Offset(n.x - r * 0.2f, n.y + r * 0.1f))
                drawCircle(bodyColor, radius = r * 0.7f, center = Offset(n.x - r * 0.75f, n.y - r * 0.35f))
                drawCircle(dark, radius = r * 0.5f, center = Offset(n.x - r * 1.25f, n.y - r * 0.7f))
                // hood + head
                drawCircle(light, radius = r * 0.62f, center = Offset(n.x + r * 0.75f, n.y + bob))
                drawCircle(dark, radius = r * 0.3f, center = Offset(n.x + r * 1.05f, n.y - r * 0.05f + bob))
                // venom drip
                val drip = abs(sin(time * 2.4f + n.faction)) * r * 0.4f
                drawCircle(Color(0xCC4D7C0F), radius = r * 0.16f, center = Offset(n.x + r * 1.25f, n.y + r * 0.35f + drip))
            }
        }
    }

    // poison stacks above head
    if (n.poisonStacks > 0) {
        worldText("☠".repeat(n.poisonStacks.coerceAtMost(5)), n.x, n.y - n.radius - 22f, 16f, Color(0xFF4D7C0F))
    }

    // name + health for bosses
    if (n.boss != null) {
        worldText(n.label, n.x, n.y - n.radius - 34f, 24f, Color(0xFFF5F1EA), bold = true)
        drawHealthBar(n, width = n.radius * 2.6f, yOff = 44f, showAlways = true)
    } else {
        drawHealthBar(n, width = n.radius * 2.1f, yOff = 12f)
    }

    if (alpha < 1f) {
        drawCircle(Color(0x336366F1), radius = r * 0.6f, center = Offset(n.x, n.y))
    }
}

// ------------------------------------------------------
// CHIMERAS — layered fusions
// ------------------------------------------------------
internal fun DrawScope.drawChimera(c: ChimeraFighter, time: Float) {
    val body = blended(longColor(c.spec.type.body), c.hitFlash)
    val accent = longColor(c.spec.type.accent)
    val dark = androidx.compose.ui.graphics.lerp(body, Color(0xFF1C1917), 0.4f)
    val r = c.radius
    val pulse = 1f + sin(time * 4f) * 0.02f

    drawFighterShadow(c)

    // phase aura
    drawCircle(
        color = if (c.enraged) Color(0x33B91C1C) else Color(0x26D97706),
        radius = r * (1.6f * pulse),
        center = Offset(c.x, c.y),
    )
    if (c.energyShield && c.shieldHp > 0f) {
        drawCircle(
            color = Color(0x550891B2),
            radius = r * 1.35f,
            center = Offset(c.x, c.y),
            style = Stroke(width = 4f),
        )
    }

    // casting telegraph
    val cast = c.castAbility
    if (cast != null) {
        val t = (c.castTimer / (cast.telegraph * if (c.phase == 1) 1f else 0.6f)).coerceIn(0f, 1f)
        val radius = if (cast.radius > 130f) cast.radius else cast.radius + c.radius
        drawCircle(
            color = Color(0x33B91C1C),
            radius = radius,
            center = Offset(c.castTargetX, c.castTargetY),
        )
        drawCircle(
            color = Color(0x99B91C1C),
            radius = radius * (1f - t),
            center = Offset(c.castTargetX, c.castTargetY),
            style = Stroke(width = 5f),
        )
        worldText(cast.displayName, c.x, c.y - r - 52f, 24f, accent, bold = true)
    }

    val facingDeg = Math.toDegrees(c.facing.toDouble()).toFloat()
    rotate(degrees = facingDeg, pivot = Offset(c.x, c.y)) {
        // tail
        val tail = Path().apply {
            moveTo(c.x - r * 0.6f, c.y + r * 0.3f)
            lineTo(c.x - r * 2.3f, c.y + r * 0.9f + sin(time * 3f) * r * 0.15f)
            lineTo(c.x - r * 2.0f, c.y - r * 0.1f)
            close()
        }
        drawPath(tail, dark)
        // wings
        val flap = sin(time * 6f) * 0.3f
        val wing = Path().apply {
            moveTo(c.x - r * 0.2f, c.y - r * 0.4f)
            lineTo(c.x - r * 1.6f, c.y - r * 1.9f - flap * r)
            lineTo(c.x - r * 2.4f, c.y + r * 0.1f - flap * r * 0.5f)
            close()
        }
        drawPath(wing, androidx.compose.ui.graphics.lerp(body, Color(0xFFF5F1EA), 0.22f))
        // hind quarters (boar half)
        drawRoundRect(
            color = dark,
            topLeft = Offset(c.x - r * 1.1f, c.y - r * 0.75f),
            size = Size(r * 1.5f, r * 1.5f),
            cornerRadius = CornerRadius(r * 0.5f),
        )
        // main body
        drawOval(body, Offset(c.x - r * 0.7f, c.y - r * 0.85f * pulse), Size(r * 1.9f, r * 1.7f * pulse))
        // lion head
        drawCircle(body, radius = r * 0.72f, center = Offset(c.x + r * 0.95f, c.y - r * 0.25f))
        // mane
        drawArc(
            color = accent,
            startAngle = -110f,
            sweepAngle = 220f,
            useCenter = true,
            topLeft = Offset(c.x + r * 0.15f, c.y - r * 1.0f),
            size = Size(r * 1.7f, r * 1.5f),
        )
        drawCircle(dark, radius = r * 0.34f, center = Offset(c.x + r * 1.5f, c.y - r * 0.2f))
        // beak / snout
        val snout = Path().apply {
            moveTo(c.x + r * 1.76f, c.y - r * 0.32f)
            lineTo(c.x + r * 2.25f, c.y - r * 0.05f)
            lineTo(c.x + r * 1.76f, c.y + r * 0.18f)
            close()
        }
        drawPath(snout, accent)
        // second head (serpent/dragon half)
        drawCircle(androidx.compose.ui.graphics.lerp(body, Color(0xFF166534), 0.35f), radius = r * 0.5f, center = Offset(c.x + r * 0.3f, c.y - r * 0.85f))
    }

    // phase pips + hp bar
    val segments = 3
    val w = r * 3.0f
    val h = 12f
    val left = c.x - w / 2f
    val top = c.y - r - 52f
    drawRoundRect(Color(0x44000000), Offset(left, top), Size(w, h), CornerRadius(h / 2f))
    val perPhase = c.maxHp / segments
    for (i in 0 until segments) {
        val segStart = perPhase * i
        val segHp = (c.hp - segStart).coerceIn(0f, perPhase)
        if (segHp <= 0f) continue
        val x0 = left + (w - 4f) * (i.toFloat() / segments) + 2f
        val segW = (w - 4f) / segments - 2f
        drawRoundRect(
            color = if (c.phase > i + 1) Color(0xFFB45309) else Color(0xFFB91C1C),
            topLeft = Offset(x0, top + 2f),
            size = Size(segW * (segHp / perPhase), h - 4f),
            cornerRadius = CornerRadius((h - 4f) / 2f),
        )
    }
    worldText("${c.spec.displayName}  •  Phase ${c.phase}", c.x, top - 8f, 26f, Color(0xFFF5F1EA), bold = true)
    if (c.spec.index == 6) worldText(c.spec.emoji, c.x, top - 40f, 34f, Color(0xFFF5F1EA))
}

// ------------------------------------------------------
// SMALL HELPERS USED BY THE SCENE RENDERER
// ------------------------------------------------------
internal fun DrawScope.drawDashedCircle(center: Offset, radius: Float, color: Color, width: Float, dash: FloatArray) {
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(dash)),
    )
}

internal fun DrawScope.drawRing(center: Offset, radius: Float, color: Color, width: Float) {
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = width))
}

internal fun DrawScope.drawSanctuaryDome(center: Offset, radius: Float, life: Float, time: Float) {
    drawCircle(color = Color(0x33F5F1EA), radius = radius, center = center)
    rotate(degrees = time * 40f, pivot = center) {
        drawDashedCircle(center, radius, Color(0x99F5F1EA), 3f, floatArrayOf(18f, 14f))
    }
    worldText(life.toInt().toString(), center.x, center.y + 8f, 26f, Color(0x99F5F1EA), bold = true)
}

internal fun DrawScope.drawShrineGlow(center: Offset, radius: Float, time: Float, cooldown: Boolean) {
    drawCircle(
        color = if (cooldown) Color(0x2278716C) else Color(0x4415803D),
        radius = radius * (1f + sin(time * 2f) * 0.03f),
        center = center,
    )
    drawRing(center, radius, if (cooldown) Color(0x6678716C) else Color(0xAAEDE7DB), 3f)
    worldText("➕", center.x, center.y + 9f, 30f, if (cooldown) Color(0x8878716C) else Color(0xFF15803D), align = Paint.Align.CENTER)
}

internal fun DrawScope.drawChestVisual(x: Float, y: Float, time: Float) {
    val bob = sin(time * 3f) * 2f
    drawFighterShadowAt(x, y, 18f)
    drawRoundRect(
        color = Color(0xFF8A6A3B),
        topLeft = Offset(x - 16f, y - 12f + bob),
        size = Size(32f, 22f),
        cornerRadius = CornerRadius(5f),
    )
    drawRoundRect(
        color = Color(0xFFD97706),
        topLeft = Offset(x - 16f, y - 12f + bob),
        size = Size(32f, 8f),
        cornerRadius = CornerRadius(4f),
    )
    drawCircle(Color(0xFFF3D9A4), radius = 3f, center = Offset(x, y - 1f + bob))
}

internal fun DrawScope.drawFighterShadowAt(x: Float, y: Float, radius: Float) {
    drawOval(Color(0x26000000), Offset(x - radius, y + radius * 0.35f), Size(radius * 2f, radius * 0.7f))
}

internal fun DrawScope.drawPickupVisual(x: Float, y: Float, icon: String, color: Color, time: Float) {
    val bob = sin(time * 4f) * 3f
    drawCircle(color = Color(0x22000000), radius = 14f, center = Offset(x, y + 12f))
    drawCircle(color = androidx.compose.ui.graphics.lerp(color, Color(0xFFF5F1EA), 0.55f), radius = 13f, center = Offset(x, y + bob))
    drawRing(Offset(x, y + bob), 13f, color, 2.5f)
    worldText(icon, x, y + 6f + bob, 18f, Color(0xFF1C1917))
}

internal fun DrawScope.drawCrystal(c: com.example.rann.sim.Crystal, time: Float) {
    if (!c.alive) {
        drawCircle(Color(0x336366F1), radius = 18f, center = Offset(c.x, c.y))
        return
    }
    val pulse = 1f + sin(time * 2.5f) * 0.05f
    val path = Path().apply {
        moveTo(c.x, c.y - 34f * pulse)
        lineTo(c.x + 20f, c.y)
        lineTo(c.x, c.y + 30f * pulse)
        lineTo(c.x - 20f, c.y)
        close()
    }
    drawPath(path, Color(0xCC6366F1))
    drawPath(path, Color(0xFF3F3A6B), style = Stroke(width = 3f))
    drawLine(Color(0x99F5F1EA), Offset(c.x, c.y - 26f), Offset(c.x, c.y + 24f), strokeWidth = 2f)
}

internal fun DrawScope.drawMovingWall(w: com.example.rann.sim.MovingWall) {
    drawRoundRect(
        color = Color(0xFF6B6558),
        topLeft = Offset(w.x - w.w / 2f, w.y - w.h / 2f),
        size = Size(w.w, w.h),
        cornerRadius = CornerRadius(6f),
    )
    drawRoundRect(
        color = Color(0xFF8A8577),
        topLeft = Offset(w.x - w.w / 2f + 4f, w.y - w.h / 2f + 4f),
        size = Size(w.w - 8f, w.h - 8f),
        cornerRadius = CornerRadius(4f),
    )
    // track
    if (w.horizontal) {
        drawLine(Color(0x331C1917), Offset(w.trackFrom, w.y), Offset(w.trackTo, w.y), strokeWidth = 3f)
    } else {
        drawLine(Color(0x331C1917), Offset(w.x, w.trackFrom), Offset(w.x, w.trackTo), strokeWidth = 3f)
    }
}

// ------------------------------------------------------
// MENU SHOWCASES — reuse the battle art in the UI
// ------------------------------------------------------
fun DrawScope.drawShowcaseCreature(type: CreatureType, x: Float, y: Float, radius: Float, time: Float) {
    val n = NpcFighter(x, y, type, type.baseHp, type.baseSpeed, type.baseDamage, radius, 2)
    n.hp = n.maxHp
    drawCreature(n, time)
}

fun DrawScope.drawShowcaseWarrior(x: Float, y: Float, radius: Float, time: Float) {
    val p = PlayerFighter(x, y, 100f)
    p.radius = radius
    p.x = x
    p.y = y
    drawWarrior(p, time)
}

fun DrawScope.drawShowcaseChimera(index: Int, x: Float, y: Float, radius: Float, time: Float) {
    val c = ChimeraFighter(x, y, com.example.rann.content.ChimeraFactory.of(index))
    c.radius = radius
    c.x = x
    c.y = y
    drawChimera(c, time)
}

/** Public helper so the UI can show a legend of the creature roster. */
fun creatureRoster(): List<Pair<CreatureType, Long>> = CreatureType.values().map { it to it.color }

fun chimeraRoster(): List<Pair<ChimeraType, String>> = ChimeraType.values().map { it to it.emoji }
