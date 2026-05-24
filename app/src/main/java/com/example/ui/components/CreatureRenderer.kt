package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.CreatureDNA
import com.example.game.ProceduralSystems
import kotlin.math.cos
import kotlin.math.sin

// ======================================================
// AAA COMPOSABLE PROCEDURAL CREATURE RENDERER
// ======================================================

@Composable
fun CreatureRenderer(
    speciesId: String,
    isShiny: Boolean = false,
    scale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    // Legacy compatible wrapper converting species/shiny to procedural DNA
    val mockEntity = remember(speciesId, isShiny) {
        val calculatedType = when (speciesId) {
            "pyrofox", "pyrofang", "pyrosaur" -> "FIRE"
            "pipfin", "aquafin", "leviapod" -> "WATER"
            "seedling", "leafant", "terrasaur" -> "NATURE"
            "sparky", "voltex" -> "ELECTRIC"
            "cubfrost", "glaciard" -> "ICE"
            "boulder", "goliath" -> "EARTH"
            "shadowasp", "phantomrazer" -> "DARK"
            "solargryph" -> "LIGHT"
            "voiddragon" -> "DRAGON"
            "runicgolem" -> "MYSTIC"
            else -> com.example.game.CreatureDatabase.getSpecies(speciesId)?.type ?: "WIND"
        }
        com.example.data.model.CreatureEntity(
            speciesId = speciesId,
            isShiny = isShiny,
            type = calculatedType,
            moves = "tackle",
            rarity = "COMMON",
            name = speciesId
        )
    }
    val dna = remember(mockEntity) { ProceduralSystems.getOrCreateDNA(mockEntity) }
    CreatureDnaRenderer(dna, modifier, "IDLE", scale)
}

@Composable
fun CreatureRenderer(
    entity: com.example.data.model.CreatureEntity,
    modifier: Modifier = Modifier,
    animationState: String = "IDLE",
    scale: Float = 1.0f
) {
    val dna = remember(entity) { ProceduralSystems.getOrCreateDNA(entity) }
    CreatureDnaRenderer(dna, modifier, animationState, scale)
}

@Composable
fun CreatureDnaRenderer(
    dna: CreatureDNA,
    modifier: Modifier = Modifier,
    animationState: String = "IDLE",
    scaleFactor: Float = 1.0f
) {
    // Transition rates for breathing/undulation idle ticks
    val infiniteTransition = rememberInfiniteTransition(label = "proc_creature_idle")
    
    val breatheOffsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_offset"
    )

    val limbAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "limb_swing"
    )

    val tailAngle by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail_whip"
    )

    val rotatingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "atmosphere_rotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            
            // Adjust elevation depending on jump movement characteristics
            val baseMovement = ProceduralSystems.speciesMap[dna.speciesA]?.movement ?: com.example.game.SurvivalMovementStyle.WALKING
            val jumpOffset = if (baseMovement == com.example.game.SurvivalMovementStyle.HOPPING) {
                // bouncing curve
                val bounce = sin(Math.toRadians(rotatingAngle.toDouble() * 3)).toFloat()
                if (bounce > 0f) -bounce * 15f else 0f
            } else {
                0f
            }
            
            val cy = (size.height / 2f) + (breatheOffsetY * scaleFactor) + (jumpOffset * scaleFactor)

            // Background dynamic atmospheric shadow/glow
            drawShadowAura(cx, cy, dna, rotatingAngle, scaleFactor)

            // Setup transformations for standard performance scale
            withTransform({
                scale(dna.scale * scaleFactor, dna.scale * scaleFactor, pivot = Offset(cx, cy))
                
                // Attack stretch effect
                if (animationState == "ATTACK") {
                    translate(-15f, 0f)
                    scale(1.15f, 0.95f, pivot = Offset(cx, cy))
                }
                // Hurt squish effect
                if (animationState == "HURT") {
                    val shake = sin(System.currentTimeMillis().toDouble() * 0.1).toFloat() * 8f
                    translate(shake, 0f)
                    scale(0.9f, 0.85f, pivot = Offset(cx, cy))
                }
            }) {
                val finalPc = if (animationState == "HURT") Color(0xFFFF8A80) else dna.primaryColor
                val finalSc = if (animationState == "HURT") Color(0xFFFFCDD2) else dna.secondaryColor
                val finalAc = if (animationState == "HURT") Color.Red else dna.accentColor

                // 1. Render Tail (Rendered in background so body overlays it)
                drawModularTail(cx, cy, dna, finalAc, tailAngle)

                // 2. Render Limbs / Feet
                drawModularLimbs(cx, cy, dna, finalPc, limbAngle)

                // 3. Render Wings (Behind body)
                drawModularWings(cx, cy, dna, finalSc, limbAngle)

                // 4. Render Main Silhouette Body Block
                drawModularBody(cx, cy, dna, finalPc, finalSc, rotatingAngle)

                // 5. Render Spine Plates / Spikes Mutations
                drawModularMutations(cx, cy, dna, finalAc, rotatingAngle)

                // 6. Render Eyes
                drawModularEyes(cx, cy, dna, finalAc, rotatingAngle)

                // 7. Render Snout / Mouth Details
                drawModularSnout(cx, cy, dna, finalSc)

                // 8. Render Horns
                drawModularHorns(cx, cy, dna, finalAc)

                // 9. Accessories Visual overlays
                drawModularAccessories(cx, cy, dna, finalAc)
            }

            // Foreground elemental weather floating particles
            drawElementalForegroundVFX(cx, cy, dna, rotatingAngle, scaleFactor)
        }
    }
}

// ======================================================
// LAYERED DNAS ARCHITECTURE CANVAS IMPLEMENTATION
// ======================================================

private fun DrawScope.drawShadowAura(cx: Float, cy: Float, dna: CreatureDNA, rot: Float, sf: Float) {
    val shadowWidth = 65f * dna.scale * sf
    val shadowHeight = 15f * dna.scale * sf
    val shadowY = cy + 45f * dna.scale * sf

    // 1. Dark base shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(cx - shadowWidth, shadowY - shadowHeight),
        size = Size(shadowWidth * 2f, shadowHeight * 2f)
    )

    // 2. Rarity Special Glowing Rings & Atmospheric Distortions
    when (dna.rarity) {
        "RARE" -> {
            drawCircle(
                brush = Brush.radialGradient(listOf(dna.accentColor.copy(alpha = 0.35f), Color.Transparent)),
                radius = 75f * sf,
                center = Offset(cx, cy)
            )
        }
        "EPIC" -> {
            drawCircle(
                brush = Brush.radialGradient(listOf(dna.accentColor.copy(alpha = 0.45f), Color.Transparent)),
                radius = 90f * sf,
                center = Offset(cx, cy)
            )
            // Animated magic particles orbiting
            val rVal = Math.toRadians(rot.toDouble())
            val px = cx + 80f * cos(rVal).toFloat() * sf
            val py = cy + 50f * sin(rVal).toFloat() * sf
            drawCircle(dna.accentColor, radius = 5f * sf, center = Offset(px, py))
        }
        "LEGENDARY" -> {
            // Elegant glowing halo boundary
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.08f + sin(Math.toRadians(rot.toDouble())).toFloat() * 0.05f),
                radius = 110f * sf,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.3f),
                radius = 100f * sf,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f * sf)
            )
        }
        "MYTHIC" -> {
            // Dynamic shift star / spacetime nebula gradient
            val grad = Brush.sweepGradient(
                colors = listOf(dna.primaryColor, dna.accentColor, Color(0xFF00FF7F), dna.primaryColor),
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = grad,
                radius = 125f * sf,
                center = Offset(cx, cy),
                style = Stroke(width = 3.5f * sf, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), rot))
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)),
                radius = 140f * sf,
                center = Offset(cx, cy)
            )
        }
    }
}

private fun DrawScope.drawModularBody(cx: Float, cy: Float, dna: CreatureDNA, pc: Color, sc: Color, rot: Float) {
    val rect = Rect(cx - 35f, cy - 35f, cx + 35f, cy + 35f)
    
    when (dna.speciesA) {
        "mouse", "rabbit", "spider" -> {
            // Round compact ball body
            drawCircle(Brush.radialGradient(listOf(sc, pc), center = Offset(cx - 10f, cy - 10f), radius = 50f), radius = 35f, center = Offset(cx, cy))
            if (dna.speciesA == "rabbit") {
                // oversized tall ears
                drawOval(pc, topLeft = Offset(cx - 18f, cy - 65f), size = Size(11f, 35f))
                drawOval(sc, topLeft = Offset(cx - 16f, cy - 58f), size = Size(7f, 25f))
                drawOval(pc, topLeft = Offset(cx + 7f, cy - 65f), size = Size(11f, 35f))
                drawOval(sc, topLeft = Offset(cx + 9f, cy - 58f), size = Size(7f, 25f))
            } else if (dna.speciesA == "mouse") {
                // big round mouse ears
                drawCircle(pc, radius = 17f, center = Offset(cx - 25f, cy - 25f))
                drawCircle(sc, radius = 10f, center = Offset(cx - 25f, cy - 25f))
                drawCircle(pc, radius = 17f, center = Offset(cx + 25f, cy - 25f))
                drawCircle(sc, radius = 10f, center = Offset(cx + 25f, cy - 25f))
            }
        }
        "cat" -> {
            // Slender body cylinder
            drawRoundRect(pc, topLeft = Offset(cx - 24f, cy - 20f), size = Size(48f, 50f), cornerRadius = CornerRadius(20f, 20f))
            // pointed high ears
            val leftEar = Path().apply {
                moveTo(cx - 20f, cy - 20f)
                lineTo(cx - 25f, cy - 42f)
                lineTo(cx - 6f, cy - 18f)
                close()
            }
            drawPath(leftEar, pc)
            val rightEar = Path().apply {
                moveTo(cx + 20f, cy - 20f)
                lineTo(cx + 25f, cy - 42f)
                lineTo(cx + 6f, cy - 18f)
                close()
            }
            drawPath(rightEar, pc)
        }
        "frog" -> {
            // squat wider bean oval
            drawOval(Brush.linearGradient(listOf(pc, sc)), topLeft = Offset(cx - 40f, cy - 18f), size = Size(80f, 48f))
            // large bulbous eye rings on top head
            drawCircle(pc, radius = 11f, center = Offset(cx - 18f, cy - 16f))
            drawCircle(pc, radius = 11f, center = Offset(cx + 18f, cy - 16f))
        }
        "bird", "owl" -> {
            // bird chest
            drawCircle(pc, radius = 33f, center = Offset(cx, cy))
            // feathered stomach overlay texture
            drawCircle(sc, radius = 20f, center = Offset(cx, cy + 10f))
            if (dna.speciesA == "owl") {
                // feathered alert brow
                val brow = Path().apply {
                    moveTo(cx - 25f, cy - 30f)
                    lineTo(cx, cy - 15f)
                    lineTo(cx + 25f, cy - 30f)
                }
                drawPath(brow, sc, style = Stroke(width = 4f))
            }
        }
        "wolf", "fox" -> {
            // Sturdy rectangular torso
            drawRoundRect(pc, topLeft = Offset(cx - 28f, cy - 15f), size = Size(58f, 48f), cornerRadius = CornerRadius(16f, 16f))
            // Fluffy neck/scruff ring
            drawCircle(sc, radius = 22f, center = Offset(cx - 15f, cy))
            if (dna.speciesA == "fox") {
                // elegant white muzzle patch
                drawArc(
                    color = Color.White.copy(alpha = 0.8f),
                    startAngle = 45f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(cx - 30f, cy + 5f),
                    size = Size(30f, 20f)
                )
            }
        }
        "snake" -> {
            // Segments curving undulating Y offset
            for (i in 0 until 5) {
                val segX = cx - 45f + (i * 18f)
                val segY = cy + sin(Math.toRadians((rot + (i * 60)).toDouble())).toFloat() * 12f
                val r = 16f - (i * 1.5f)
                drawCircle(
                    brush = Brush.radialGradient(listOf(sc, pc), center = Offset(segX, segY), radius = r),
                    radius = r,
                    center = Offset(segX, segY)
                )
            }
        }
        "turtle" -> {
            // giant hexagonal shell plate
            drawCircle(sc, radius = 38f, center = Offset(cx, cy))
            // Draw internal hexagons for scale texture
            for (ang in 0 until 360 step 60) {
                val rad = Math.toRadians(ang.toDouble())
                val ox = cx + 18f * cos(rad).toFloat()
                val oy = cy + 18f * sin(rad).toFloat()
                drawCircle(pc, radius = 8f, center = Offset(ox, oy))
            }
            // little head protruding
            drawCircle(pc, radius = 14f, center = Offset(cx - 38f, cy - 12f))
        }
        "dragon" -> {
            // scaly lizard body back arc
            val dragPath = Path().apply {
                moveTo(cx - 30f, cy + 20f)
                quadraticTo(cx, cy - 25f, cx + 40f, cy + 10f)
                lineTo(cx + 25f, cy + 25f)
                close()
            }
            drawPath(dragPath, pc)
            // spine plates
            drawCircle(sc, radius = 10f, center = Offset(cx - 15f, cy - 10f))
            drawCircle(sc, radius = 8f, center = Offset(cx + 5f, cy - 4f))
        }
        "shark" -> {
            // streamlined dolphin body oval
            drawOval(pc, topLeft = Offset(cx - 45f, cy - 16f), size = Size(90f, 34f))
            // dorsal fin
            val dorsal = Path().apply {
                moveTo(cx - 5f, cy - 16f)
                quadraticTo(cx - 15f, cy - 42f, cx + 10f, cy - 12f)
            }
            drawPath(dorsal, pc)
            drawPath(dorsal, sc, style = Stroke(width = 3f))
        }
        "lizard" -> {
            // flat sleek cylinder
            drawRoundRect(pc, topLeft = Offset(cx - 35f, cy - 10f), size = Size(70f, 25f), cornerRadius = CornerRadius(10f, 10f))
            drawCircle(sc, radius = 15f, center = Offset(cx - 25f, cy - 5f))
        }
        "golem" -> {
            // disjointed block plates
            drawRect(pc, topLeft = Offset(cx - 30f, cy - 25f), size = Size(60f, 50f))
            drawRect(sc, topLeft = Offset(cx - 20f, cy - 15f), size = Size(40f, 30f))
            // glowing matrix energy eyes slit
            drawRect(Color(0xFF00FF7F), topLeft = Offset(cx - 15f, cy - 8f), size = Size(30f, 4f))
        }
        else -> {
            drawCircle(pc, radius = 32f, center = Offset(cx, cy))
        }
    }

    // Blend hybrid secondary body traits if set!
    if (dna.speciesB != null) {
        // Hybrid accent overlay marks
        drawCircle(sc.copy(alpha = 0.45f), radius = 10f, center = Offset(cx - 15f, cy + 15f))
    }
}

private fun DrawScope.drawModularTail(cx: Float, cy: Float, dna: CreatureDNA, ac: Color, tailAngle: Float) {
    withTransform({
        rotate(tailAngle, pivot = Offset(cx + 25f, cy + 10f))
    }) {
        val path = Path()
        when (dna.tailStyle) {
            "FLAME_BRUSH" -> {
                path.apply {
                    moveTo(cx + 25f, cy + 10f)
                    quadraticTo(cx + 65f, cy - 15f, cx + 75f, cy - 48f)
                    quadraticTo(cx + 45f, cy + 22f, cx + 25f, cy + 15f)
                    close()
                }
                drawPath(path, ac)
                // draw flame particles
                drawCircle(Color(0xFFFFD700), radius = 6f, center = Offset(cx + 70f, cy - 35f))
            }
            "SPIKED_CLUB" -> {
                // club rod
                drawLine(ac, start = Offset(cx + 25f, cy + 15f), end = Offset(cx + 65f, cy + 5f), strokeWidth = 8f)
                // ball spikes
                drawCircle(Color.Gray, radius = 12f, center = Offset(cx + 65f, cy + 5f))
                drawCircle(ac, radius = 4f, center = Offset(cx + 65f, cy - 5f))
                drawCircle(ac, radius = 4f, center = Offset(cx + 75f, cy + 5f))
            }
            "FEATHER_COIL" -> {
                path.apply {
                    moveTo(cx + 25f, cy + 10f)
                    quadraticTo(cx + 50f, cy + 30f, cx + 70f, cy + 18f)
                    quadraticTo(cx + 45f, cy - 5f, cx + 25f, cy + 10f)
                }
                drawPath(path, dna.secondaryColor)
            }
            "FIN_SPLIT" -> {
                // sweeping dolphin tail
                path.apply {
                    moveTo(cx + 25f, cy + 8f)
                    quadraticTo(cx + 50f, cy, cx + 60f, cy - 12f)
                    lineTo(cx + 65f, cy + 12f)
                    close()
                }
                drawPath(path, dna.primaryColor)
            }
            "LIGHTNING_BOLT" -> {
                path.apply {
                    moveTo(cx + 25f, cy + 10f)
                    lineTo(cx + 48f, cy - 5f)
                    lineTo(cx + 38f, cy + 12f)
                    lineTo(cx + 65f, cy - 10f)
                }
                drawPath(path, Color(0xFFFFEB3B), style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "CRYSTAL_SPIN" -> {
                // floating rotating crystal node behind
                drawCircle(Color(0xFFE91E63), radius = 10f, center = Offset(cx + 50f, cy + 5f))
            }
            "NONE" -> {}
        }
    }
}

private fun DrawScope.drawModularWings(cx: Float, cy: Float, dna: CreatureDNA, sc: Color, limbAngle: Float) {
    val wingNeeded = dna.speciesA == "bird" || dna.speciesA == "owl" || dna.speciesA == "dragon" || dna.speciesB == "bird" || dna.speciesB == "dragon"
    if (!wingNeeded) return

    val flapping = limbAngle * 1.5f
    // Left Wing
    withTransform({
        rotate(-flapping, pivot = Offset(cx - 20f, cy - 5f))
    }) {
        val wing = Path().apply {
            moveTo(cx - 15f, cy - 5f)
            quadraticTo(cx - 75f, cy - 52f, cx - 60f, cy - 10f)
            close()
        }
        drawPath(wing, sc)
        drawPath(wing, dna.accentColor, style = Stroke(width = 2f))
    }

    // Right Wing
    withTransform({
        rotate(flapping, pivot = Offset(cx + 20f, cy - 5f))
    }) {
        val wing = Path().apply {
            moveTo(cx + 15f, cy - 5f)
            quadraticTo(cx + 75f, cy - 52f, cx + 60f, cy - 10f)
            close()
        }
        drawPath(wing, sc)
        drawPath(wing, dna.accentColor, style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawModularLimbs(cx: Float, cy: Float, dna: CreatureDNA, pc: Color, limbAngle: Float) {
    val hasLimbs = dna.speciesA != "snake" && dna.speciesA != "shark"
    if (!hasLimbs) return

    if (dna.speciesA == "spider") {
        // draw 8 jointed legs
        for (i in 0 until 4) {
            val angOffset = i * 25f - 40f
            val leftLeg = Path().apply {
                moveTo(cx - 20f, cy + 10f)
                lineTo(cx - 55f + angOffset, cy + 5f)
                lineTo(cx - 48f + angOffset, cy + 35f)
            }
            drawPath(leftLeg, pc, style = Stroke(width = 4f, cap = StrokeCap.Round))
            
            val rightLeg = Path().apply {
                moveTo(cx + 20f, cy + 10f)
                lineTo(cx + 55f - angOffset, cy + 5f)
                lineTo(cx + 48f - angOffset, cy + 35f)
            }
            drawPath(rightLeg, pc, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
        return
    }

    // Standard Quadruped/Round feet walker bars
    val ly = cy + 28f
    val rWidth = 8f
    val rHeight = 18f

    // Front Left Foot
    withTransform({
        rotate(limbAngle, pivot = Offset(cx - 18f, ly))
    }) {
        drawRoundRect(
            color = pc,
            topLeft = Offset(cx - 22f, ly),
            size = Size(rWidth, rHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    // Front Right Foot
    withTransform({
        rotate(-limbAngle, pivot = Offset(cx + 18f, ly))
    }) {
        drawRoundRect(
            color = pc,
            topLeft = Offset(cx + 14f, ly),
            size = Size(rWidth, rHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    // Back Foot shadow-colored
    withTransform({
        rotate(-limbAngle * 0.8f, pivot = Offset(cx, ly))
    }) {
        drawRoundRect(
            color = pc.copy(alpha = 0.6f),
            topLeft = Offset(cx - 4f, ly + 2f),
            size = Size(rWidth, rHeight - 2f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

private fun DrawScope.drawModularEyes(cx: Float, cy: Float, dna: CreatureDNA, ac: Color, rot: Float) {
    if (dna.speciesA == "golem") return // Golem has neon visor drawn already

    var lx = cx - 14f
    var rx = cx + 14f
    var ey = cy - 6f

    // Shift coordinates based on snout alignment
    when (dna.speciesA) {
        "cat", "wolf", "fox" -> {
            lx = cx - 18f
            rx = cx + 2f // oriented semi 3/4 profiles
            ey = cy - 10f
        }
        "frog", "turtle" -> {
            lx = cx - 18f
            rx = cx + 18f
            ey = cy - 15f
        }
    }

    when (dna.eyeStyle) {
        "ANIME_SHINY" -> {
            // Shiny dark circles with glossy top white sparkles
            drawCircle(Color.Black, radius = 6.5f, center = Offset(lx, ey))
            drawCircle(Color.Black, radius = 6.5f, center = Offset(rx, ey))
            drawCircle(Color.White, radius = 2.5f, center = Offset(lx - 2f, ey - 2f))
            drawCircle(Color.White, radius = 2.5f, center = Offset(rx - 2f, ey - 2f))
        }
        "GLOWING_EMBERS" -> {
            // Intense orange glowing nodes
            val ember = Brush.radialGradient(listOf(Color.White, Color(0xFFFF5722)))
            drawCircle(ember, radius = 7f, center = Offset(lx, ey))
            drawCircle(ember, radius = 7f, center = Offset(rx, ey))
        }
        "SHADOW_RING" -> {
            // Deep purple circles with black core
            drawCircle(Color(0xFF7E57C2), radius = 8f, center = Offset(lx, ey))
            drawCircle(Color(0xFF7E57C2), radius = 8f, center = Offset(rx, ey))
            drawCircle(Color.Black, radius = 4f, center = Offset(lx, ey))
            drawCircle(Color.Black, radius = 4f, center = Offset(rx, ey))
        }
        "CAT_SLIT" -> {
            drawCircle(Color(0xFFEEFF41), radius = 6.5f, center = Offset(lx, ey))
            drawCircle(Color(0xFFEEFF41), radius = 6.5f, center = Offset(rx, ey))
            // draw vertical line
            drawLine(Color.Black, start = Offset(lx, ey - 4f), end = Offset(lx, ey + 4f), strokeWidth = 2f)
            drawLine(Color.Black, start = Offset(rx, ey - 4f), end = Offset(rx, ey + 4f), strokeWidth = 2f)
        }
        "CROSS" -> {
            // Angry pixel crosses
            drawLine(ac, start = Offset(lx - 4f, ey - 4f), end = Offset(lx + 4f, ey + 4f), strokeWidth = 2.5f)
            drawLine(ac, start = Offset(lx + 4f, ey - 4f), end = Offset(lx - 4f, ey + 4f), strokeWidth = 2.5f)
            drawLine(ac, start = Offset(rx - 4f, ey - 4f), end = Offset(rx + 4f, ey + 4f), strokeWidth = 2.5f)
            drawLine(ac, start = Offset(rx + 4f, ey - 4f), end = Offset(rx - 4f, ey + 4f), strokeWidth = 2.5f)
        }
    }
}

private fun DrawScope.drawModularSnout(cx: Float, cy: Float, dna: CreatureDNA, sc: Color) {
    val needsSnout = dna.speciesA == "cat" || dna.speciesA == "wolf" || dna.speciesA == "fox" || dna.speciesA == "lizard"
    if (!needsSnout) return

    val snoutX = cx - 10f
    val snoutY = cy + 2f

    val path = Path().apply {
        moveTo(snoutX, snoutY)
        lineTo(snoutX - 12f, snoutY + 6f)
        lineTo(snoutX + 2f, snoutY + 11f)
        close()
    }
    drawPath(path, sc)
    // nose tip
    drawCircle(Color.Black, radius = 2.5f, center = Offset(snoutX - 11f, snoutY + 6f))
}

private fun DrawScope.drawModularHorns(cx: Float, cy: Float, dna: CreatureDNA, ac: Color) {
    if (dna.hornStyle == "NONE") return

    val ly = cy - 35f
    when (dna.hornStyle) {
        "RAM_SPIRAL" -> {
            drawCircle(ac, radius = 10f, center = Offset(cx - 15f, ly), style = Stroke(width = 4f))
            drawCircle(ac, radius = 10f, center = Offset(cx + 15f, ly), style = Stroke(width = 4f))
        }
        "UNIDORN" -> {
            // center single pointy horn
            val horn = Path().apply {
                moveTo(cx - 5f, ly + 5f)
                lineTo(cx, ly - 30f)
                lineTo(cx + 5f, ly + 5f)
                close()
            }
            drawPath(horn, Color(0xFFFFD700))
        }
        "DRAGON_SPIKES" -> {
            // two sweeping spikes
            drawLine(ac, start = Offset(cx - 10f, ly + 5f), end = Offset(cx - 28f, ly - 18f), strokeWidth = 5f)
            drawLine(ac, start = Offset(cx + 10f, ly + 5f), end = Offset(cx + 28f, ly - 18f), strokeWidth = 5f)
        }
        "CRYSTAL_PRISM" -> {
            drawCircle(Color(0xFF00E5FF), radius = 8f, center = Offset(cx, ly - 8f))
        }
        "ANTENNA" -> {
            drawLine(Color.Black, start = Offset(cx, cy - 25f), end = Offset(cx, cy - 50f), strokeWidth = 3f)
            drawCircle(ac, radius = 6f, center = Offset(cx, cy - 50f))
        }
    }
}

private fun DrawScope.drawModularAccessories(cx: Float, cy: Float, dna: CreatureDNA, ac: Color) {
    if (dna.accessoryStyle == "NONE") return

    when (dna.accessoryStyle) {
        "RUNE_AMULET" -> {
            // collar string
            drawLine(Color.Gray, start = Offset(cx - 15f, cy + 18f), end = Offset(cx + 15f, cy + 18f), strokeWidth = 3f)
            // floating diamond amulet
            val path = Path().apply {
                moveTo(cx, cy + 14f)
                lineTo(cx + 6f, cy + 22f)
                lineTo(cx, cy + 30f)
                lineTo(cx - 6f, cy + 22f)
                close()
            }
            drawPath(path, Color(0xFFFFD700))
        }
        "CYBER_VISOR" -> {
            // neon glow panel over eyes
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(cx - 24f, cy - 14f),
                size = Size(48f, 10f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
        "FLOWER_CROWN" -> {
            // flower chain over forehead
            for (i in -2..2) {
                val fx = cx + (i * 10f)
                val fy = cy - 30f
                drawCircle(Color(0xFFFF4081), radius = 5f, center = Offset(fx, fy))
                drawCircle(Color.White, radius = 2f, center = Offset(fx, fy))
            }
        }
        "SPARK_COLLAR" -> {
            drawLine(Color(0xFFFFEB3B), start = Offset(cx - 18f, cy + 15f), end = Offset(cx + 18f, cy + 15f), strokeWidth = 5f)
        }
    }
}

private fun DrawScope.drawModularMutations(cx: Float, cy: Float, dna: CreatureDNA, ac: Color, rot: Float) {
    if (dna.mutations.isEmpty()) return

    dna.mutations.forEach { mut ->
        when (mut) {
            "spikes" -> {
                // draw 3 red triangular spikes on back
                for (i in 0 until 3) {
                    val sx = cx + 8f + (i * 12f)
                    val sy = cy - 24f + (i * 6f)
                    val tri = Path().apply {
                        moveTo(sx - 4f, sy)
                        lineTo(sx, sy - 10f)
                        lineTo(sx + 4f, sy)
                        close()
                    }
                    drawPath(tri, ac)
                }
            }
            "crystal_growth" -> {
                // glowing magenta crystal rocks
                drawCircle(Color(0xFFE91E63), radius = 7f, center = Offset(cx - 25f, cy + 15f))
                drawCircle(Color(0xFF00E5FF), radius = 5f, center = Offset(cx + 25f, cy + 15f))
            }
            "cyber_implants" -> {
                // metallic panel with yellow lights
                drawRoundRect(
                    color = Color.DarkGray,
                    topLeft = Offset(cx + 12f, cy - 8f),
                    size = Size(16f, 16f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawCircle(Color.Yellow, radius = 2.5f, center = Offset(cx + 20f, cy))
            }
            "scales" -> {
                // draw scale lines
                val path = Path().apply {
                    moveTo(cx - 10f, cy + 8f)
                    quadraticTo(cx - 5f, cy + 12f, cx, cy + 8f)
                    moveTo(cx, cy + 12f)
                    quadraticTo(cx + 5f, cy + 16f, cx + 10f, cy + 12f)
                }
                drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(width = 2.5f))
            }
            "poison_veins" -> {
                // glowing purple lines
                drawLine(
                    color = Color(0xFFA100FF),
                    start = Offset(cx - 15f, cy - 10f),
                    end = Offset(cx - 22f, cy + 15f),
                    strokeWidth = 3f
                )
            }
            "lava_cracks" -> {
                // fiery glowing fissures
                drawLine(
                    color = Color(0xFFFF3D00),
                    start = Offset(cx - 10f, cy + 5f),
                    end = Offset(cx + 10f, cy + 5f),
                    strokeWidth = 4f
                )
            }
            "glowing_runes" -> {
                // ancient runic glyphs
                drawCircle(
                    color = Color(0xFF00FF7F),
                    radius = 8f,
                    center = Offset(cx - 15f, cy + 18f),
                    style = Stroke(width = 2f)
                )
            }
            "shadow_smoke" -> {
                // black smoke puffs floating off body
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = 9f,
                    center = Offset(cx + 30f, cy - 18f)
                )
            }
            "energy_lines" -> {
                // neon electric circuit overlays
                drawLine(
                    color = Color(0xFFFF5722),
                    start = Offset(cx - 10f, cy - 20f),
                    end = Offset(cx + 15f, cy - 20f),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

private fun DrawScope.drawElementalForegroundVFX(cx: Float, cy: Float, dna: CreatureDNA, rot: Float, sf: Float) {
    // 4 elemental particle nodes circulating in 2D space
    val numNodes = 4
    for (i in 0 until numNodes) {
        val ang = rot + (i * (360f / numNodes))
        val rad = Math.toRadians(ang.toDouble())
        val dist = 70f * sf
        val px = cx + dist * cos(rad).toFloat()
        val py = cy + dist * sin(rad).toFloat()

        when (dna.element) {
            "FIRE", "MAGMA" -> {
                // flame sparks
                val fire = Path().apply {
                    moveTo(px, py - 9f)
                    quadraticTo(px + 4f, py - 1f, px, py + 9f)
                    quadraticTo(px - 4f, py - 1f, px, py - 9f)
                }
                drawPath(fire, Color(0xFFFF3D00))
            }
            "ICE" -> {
                // frozen snowflake prisms
                drawRect(Color(0xFFE0F7FA), topLeft = Offset(px - 4f, py - 4f), size = Size(8f, 8f))
            }
            "ELECTRIC" -> {
                // high-frequency lightning spurs
                drawLine(Color(0xFFFFEB3B), start = Offset(px - 6f, py - 6f), end = Offset(px + 6f, py + 6f), strokeWidth = 3.5f)
            }
            "TOXIC" -> {
                // purple acid bubbles
                drawCircle(Color(0xFFBA68C8).copy(alpha = 0.6f), radius = 6f, center = Offset(px, py))
            }
            "SHADOW" -> {
                // black dark-mist dust
                drawCircle(Color(0xFF311B92).copy(alpha = 0.8f), radius = 5f, center = Offset(px, py))
            }
            "CRYSTAL" -> {
                // shiny crystal diamonds
                val diamond = Path().apply {
                    moveTo(px, py - 7f)
                    lineTo(px + 5f, py)
                    lineTo(px, py + 7f)
                    lineTo(px - 5f, py)
                    close()
                }
                drawPath(diamond, Color(0xFFE91E63))
            }
            "NATURE" -> {
                // rotating green leaf shapes
                val leaf = Path().apply {
                    moveTo(px - 7f, py)
                    quadraticTo(px, py - 5f, px + 7f, py)
                    quadraticTo(px, py + 5f, px - 7f, py)
                }
                drawPath(leaf, Color(0xFF4CAF50))
            }
            "WATER" -> {
                // marine bubbles with shiny reflections
                drawCircle(Color.White.copy(alpha = 0.4f), radius = 7f, center = Offset(px, py), style = Stroke(width = 2f))
            }
            else -> {
                // standard visual light sparkles
                drawCircle(dna.accentColor.copy(alpha = 0.5f), radius = 4f * sf, center = Offset(px, py))
            }
        }
    }
}
