package com.example.rann.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rann.audio.Sound
import com.example.rann.model.BonusKind
import com.example.rann.model.BattleState
import com.example.rann.model.GradeResult
import com.example.rann.render.BattleCamera
import com.example.rann.render.RenderFlags
import com.example.rann.render.drawBattleScene
import com.example.rann.render.drawMinimap
import com.example.rann.render.drawWindArrow
import com.example.rann.sim.BattleInput
import com.example.rann.sim.BattleSim
import com.example.rann.ui.GradeReveal
import com.example.rann.ui.KeyValueRow
import com.example.rann.ui.ModalScrim
import com.example.rann.ui.PrimaryButton
import com.example.rann.ui.RannCard
import com.example.rann.ui.SecondaryButton
import com.example.rann.ui.StatChip
import com.example.rann.ui.formatCoins
import com.example.rann.vm.AppScreen
import com.example.rann.vm.BattleRequest
import com.example.rann.vm.RannViewModel
import com.example.ui.theme.Rann
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun BattleScreen(vm: RannViewModel) {
    val request = vm.battle ?: return
    BattleHost(vm, request)
}

@Composable
private fun BattleHost(vm: RannViewModel, request: BattleRequest) {
    val c = RannTheme.colors
    val sim = remember(request) { BattleSim(request.setup) }
    val cam = remember(request) { BattleCamera() }
    val input = remember(request) { BattleInput() }

    var time by remember(request) { mutableFloatStateOf(0f) }
    var viewport by remember(request) { mutableStateOf(IntSize(600, 900)) }
    var paused by remember(request) { mutableStateOf(false) }
    var result by remember(request) { mutableStateOf<GradeResult?>(null) }
    var frame by remember(request) { mutableFloatStateOf(0f) }
    var showHint by remember(request) { mutableStateOf(true) }
    // The HUD reads plain (non-snapshot) simulation fields, so nudge the
    // composition ~10x per second while the canvas redraws every frame.
    var hudTick by remember(request) { mutableIntStateOf(0) }
    val hudAccum = remember(request) { floatArrayOf(0f) }

    // ---------------- frame loop ----------------
    LaunchedEffect(sim) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now
            frame += 1f
            if (!paused) {
                time += dt
                sim.update(dt, input)
                for (event in sim.drainSounds()) Sound.play(event)
                cam.follow(
                    sim = sim,
                    viewW = viewport.width.toFloat(),
                    viewH = viewport.height.toFloat(),
                    dt = dt,
                    shakeEnabled = vm.profile.settings.screenShake,
                    frame = frame,
                )
                hudAccum[0] += dt
                if (hudAccum[0] >= 0.1f) {
                    hudAccum[0] = 0f
                    hudTick++
                }
                if (time > 6f) showHint = false
                if (sim.state != BattleState.RUNNING && result == null && sim.outcomeTimer > 0.9f) {
                    result = vm.completeBattle(sim)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(request.setup.map.ground))
            .onSizeChanged { viewport = it },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawBattleScene(
                sim = sim,
                cam = cam,
                time = time,
                flags = RenderFlags(
                    showFloatingText = vm.profile.settings.showFloatingText,
                    screenShake = vm.profile.settings.screenShake,
                ),
            )
        }

        // ---------------- controls ----------------
        ControlsLayer(
            input = input,
            modifier = Modifier.fillMaxSize(),
            onAttack = { if (showHint) showHint = false },
        )

        // ---------------- HUD ----------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            BattleTopBar(sim = sim, mapName = request.mapName, tick = hudTick)
            Spacer(Modifier.height(6.dp))
            BossBar(sim = sim, tick = hudTick)
            StatusStrip(sim)
            Spacer(Modifier.weight(1f))
            BottomStrip(sim, request)
            Spacer(Modifier.height(10.dp))
        }

        // ---------------- minimap ----------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 150.dp)
                .size(112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC1C1917))
                .border(1.dp, c.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        ) {
            Canvas(Modifier.fillMaxSize()) { drawMinimap(sim) }
        }

        // ---------------- wind indicator ----------------
        sim.wind?.let { wind ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 96.dp, end = 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x99262420)),
            ) {
                Canvas(Modifier.fillMaxSize()) { drawWindArrow(wind.angle) }
            }
        }

        // ---------------- banner ----------------
        if (sim.bannerText != null && sim.bannerTimer > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 110.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xCC7C2D12))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(sim.bannerText ?: "", style = RannText.button, color = Color.White)
                }
            }
        }

        // ---------------- hint ----------------
        if (showHint && result == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 190.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xAA1C1917))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Drag left half to move · hold right half to strike",
                    style = RannText.label,
                    color = Color(0xFFF5F5F4),
                )
            }
        }

        // ---------------- pause button ----------------
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 44.dp, end = 20.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x99262420))
                .pointerInput(Unit) { detectTapGestures { paused = true } },
            contentAlignment = Alignment.Center,
        ) {
            Text("⏸", style = RannText.body, color = Color(0xFFF5F5F4))
        }

        // ---------------- pause overlay ----------------
        ModalScrim(visible = paused, onDismiss = { paused = false }) {
            RannCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(request.title, style = RannText.h2, color = c.textPrimary)
                    Text(request.subtitle, style = RannText.bodySmall, color = c.textMuted)
                    Spacer(Modifier.height(10.dp))
                    Text(request.twistText, style = RannText.label, color = c.textMuted)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Resume",
                        icon = "⚔️",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { paused = false },
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = "Abandon battle",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            paused = false
                            vm.clearResult()
                            vm.screen = AppScreen.HOME
                        },
                    )
                }
            }
        }

        // ---------------- result overlay ----------------
        result?.let { res ->
            ResultOverlay(
                vm = vm,
                request = request,
                result = res,
            )
        }
    }
}

// ======================================================
// CONTROLS
// ======================================================

@Composable
private fun ControlsLayer(
    input: BattleInput,
    modifier: Modifier = Modifier,
    onAttack: () -> Unit,
) {
    var origin by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf<Offset?>(null) }
    val radiusPx = 110f

    Box(modifier) {
        // left half: virtual joystick
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            origin = offset
                            knob = offset
                        },
                        onDrag = { change, amount ->
                            val o = origin ?: return@detectDragGestures
                            val k = (knob ?: o) + amount
                            knob = k
                            val dx = k.x - o.x
                            val dy = k.y - o.y
                            val len = hypot(dx, dy)
                            if (len < 1f) {
                                input.moveX = 0f
                                input.moveY = 0f
                            } else {
                                val scale = (len / radiusPx).coerceIn(0f, 1f) / len
                                input.moveX = dx * scale
                                input.moveY = dy * scale
                            }
                        },
                        onDragEnd = {
                            origin = null
                            knob = null
                            input.moveX = 0f
                            input.moveY = 0f
                        },
                        onDragCancel = {
                            origin = null
                            knob = null
                            input.moveX = 0f
                            input.moveY = 0f
                        },
                    )
                },
        )
        // right half: hold to attack
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            input.attack = true
                            onAttack()
                            tryAwaitRelease()
                            input.attack = false
                        },
                    )
                },
        )
        // joystick visual
        val o = origin
        val k = knob
        if (o != null && k != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = Color(0x33FFFFFF), radius = radiusPx * 0.7f, center = o)
                drawCircle(
                    color = Color(0x66F5F1EA),
                    radius = radiusPx * 0.7f,
                    center = o,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                )
                drawCircle(color = Color(0xCCD97706), radius = 34f, center = k)
            }
        }
    }
}

// ======================================================
// HUD PARTS
// ======================================================

@Composable
private fun BattleTopBar(sim: BattleSim, mapName: String, tick: Int) {
    val c = RannTheme.colors
    val hp = sim.player.hpFraction
    val critical = hp < 0.3f
    val pulse = critical && tick % 2 == 0
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xAA1C1917))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${sim.timeText()} · ${sim.livingEnemyCount()} left",
                    style = RannText.buttonSmall,
                    color = Color(0xFFF5F5F4),
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xAA1C1917))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "🪙 ${formatCoins(sim.matchCoins)}",
                    style = RannText.buttonSmall,
                    color = Rann.GoldSoft,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❤️", style = RannText.bodySmall)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xAA1C1917)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            (
                                when {
                                    hp > 0.55f -> Rann.Success
                                    hp > 0.28f -> Rann.Gold
                                    else -> Rann.Danger
                                }
                                ).copy(alpha = if (pulse) 0.55f else 1f),
                        ),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${sim.player.hp.roundToInt()}${if (sim.player.revives > 0) " +${sim.player.revives}❤" else ""}",
                style = RannText.buttonSmall,
                color = Color(0xFFF5F5F4),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${sim.player.weapon.icon} ${sim.player.weapon.displayName} · ${sim.player.armor.icon} ${sim.player.armor.displayName}",
                style = RannText.label,
                color = Color(0xFFE7E1D3),
            )
            Spacer(Modifier.weight(1f))
            Text(mapName, style = RannText.label, color = Color(0xFFE7E1D3))
        }
    }
}

@Composable
private fun BossBar(sim: BattleSim, tick: Int) {
    val boss = sim.npcs.firstOrNull { it.boss != null && it.alive }
    val chimera = sim.chimera
    val show = boss != null || (chimera != null && chimera.alive)
    if (!show) return
    val name = chimera?.spec?.displayName ?: boss?.boss?.name ?: "Champion"
    val frac = chimera?.hpFraction ?: boss?.hpFraction ?: 0f
    val phase = chimera?.phase ?: boss?.bossPhase ?: 1
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xAA1C1917))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = RannText.buttonSmall, color = Color(0xFFF5F5F4))
                Spacer(Modifier.weight(1f))
                Text(
                    text = when {
                        phase > 1 -> "Phase $phase · ${(frac * 100f).toInt()}%"
                        frac < 0.25f && tick % 2 == 0 -> "FINISH IT"
                        else -> "${(frac * 100f).toInt()}%"
                    },
                    style = RannText.label,
                    color = if (phase > 1) Rann.Danger else Rann.GoldSoft,
                )
            }
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x66F5F1EA)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Rann.Accent),
                )
            }
        }
    }
}

@Composable
private fun StatusStrip(sim: BattleSim) {
    val chips = sim.activePowerList()
    val outside = sim.isPlayerOutsideZone()
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip("🌀", sim.zoneStatusText(), tint = if (outside) Rann.Danger else null)
            if (sim.bloodlust) StatChip("🔥", "Bloodlust")
        }
        if (chips.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.take(4).forEach { (icon, label, t) ->
                    StatChip(icon, "$label ${t.roundToInt()}s")
                }
            }
        }
    }
}

@Composable
private fun BottomStrip(sim: BattleSim, request: BattleRequest) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xAA1C1917))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (request.isElite) "ELITE · no boosters" else request.subtitle,
                style = RannText.label,
                color = Color(0xFFE7E1D3),
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x66D97706))
                .border(2.dp, Color(0xCCD97706), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚔️", style = RannText.h2)
        }
    }
}

// ======================================================
// RESULT
// ======================================================

@Composable
private fun ResultOverlay(vm: RannViewModel, request: BattleRequest, result: GradeResult) {
    val c = RannTheme.colors
    ModalScrim(visible = true, onDismiss = null) {
        RannCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = if (result.won) "Victory" else "Defeated",
                    style = RannText.h1,
                    color = if (result.won) c.textPrimary else Rann.Danger,
                )
                Text(
                    text = if (result.won) request.title else result.subScores.firstOrNull { it.grade == result.grade }?.let {
                        "Grade fell to ${result.grade.letter}"
                    } ?: "The pit keeps its secrets",
                    style = RannText.body,
                    color = c.textMuted,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradeReveal(result.grade)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        result.subScores.forEach { sub ->
                            KeyValueRow(
                                label = "${sub.icon} ${sub.label}",
                                value = "${sub.valueText}  ${sub.grade.letter}",
                                valueColor = when (sub.grade) {
                                    com.example.rann.model.LevelGrade.SS, com.example.rann.model.LevelGrade.S -> Rann.PrimaryDark
                                    com.example.rann.model.LevelGrade.D -> c.textMuted
                                    else -> c.textPrimary
                                },
                            )
                        }
                    }
                }
                if (result.bonuses.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    result.bonuses.forEach { bonus ->
                        KeyValueRow(
                            label = "${bonus.kind.icon} ${bonus.label}",
                            value = "+${bonus.amount} 🪙",
                            valueColor = Rann.PrimaryDark,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceAlt)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total", style = RannText.h3, color = c.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "+${formatCoins(result.totalCoins)} 🪙",
                            style = RannText.h2,
                            color = Rann.PrimaryDark,
                        )
                    }
                }
                result.titleUnlocked?.let { title ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "🏅 Title unlocked: $title",
                        style = RannText.body,
                        color = Rann.PrimaryDark,
                    )
                }
                if (result.bonuses.any { it.kind == BonusKind.LOSS }) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Losses still pay a consolation purse — trade it for a better blade.",
                        style = RannText.label,
                        color = c.textMuted,
                    )
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = when {
                        !result.won -> "Try again"
                        result.nextLevelGlobal != null -> "Next battle"
                        request.isElite -> "Next trial"
                        else -> "Home"
                    },
                    icon = if (result.won) "⚔️" else "🔁",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.clearResult()
                        if (!result.won) {
                            vm.retryBattle()
                        } else {
                            vm.nextLevel()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "Retry",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            vm.clearResult()
                            vm.retryBattle()
                        },
                    )
                    SecondaryButton(
                        text = "War Grounds",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            vm.clearResult()
                            vm.screen = if (request.isElite) AppScreen.ELITE else AppScreen.LEVELS
                        },
                    )
                }
            }
        }
    }
}
