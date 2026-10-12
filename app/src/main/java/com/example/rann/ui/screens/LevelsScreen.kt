package com.example.rann.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rann.audio.Sound
import com.example.rann.content.LevelFactory
import com.example.rann.model.Balance
import com.example.rann.model.LevelGrade
import com.example.rann.model.LevelModifier
import com.example.rann.model.Rank
import com.example.rann.model.WeaponTier
import com.example.rann.ui.GhostButton
import com.example.rann.ui.GradeBadge
import com.example.rann.ui.PrimaryButton
import com.example.rann.ui.ProgressRing
import com.example.rann.ui.RannCard
import com.example.rann.ui.SecondaryButton
import com.example.rann.ui.SectionHeader
import com.example.rann.ui.SegmentedTabs
import com.example.rann.ui.StatChip
import com.example.rann.ui.TopBar
import com.example.rann.ui.formatCoins
import com.example.rann.vm.AppScreen
import com.example.rann.vm.RannViewModel
import com.example.ui.theme.Rann
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme
import kotlin.math.roundToInt

@Composable
fun LevelsScreen(vm: RannViewModel) {
    val c = RannTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        TopBar(
            title = "War Grounds",
            subtitle = "Campaign · Forge",
            onBack = { vm.screen = AppScreen.HOME },
            trailing = { StatChip("🪙", formatCoins(vm.profile.coins), tint = Rann.PrimaryDark) },
        )
        Spacer(Modifier.height(12.dp))
        SegmentedTabs(
            tabs = listOf("Campaign", "Forge"),
            selected = vm.levelsTab,
            onSelect = {
                Sound.tap()
                vm.levelsTab = it
            },
        )
        Spacer(Modifier.height(12.dp))
        if (vm.levelsTab == 0) CampaignTab(vm, Modifier.weight(1f)) else ForgeTab(vm, Modifier.weight(1f))
    }
}

// ======================================================
// CAMPAIGN
// ======================================================

@Composable
private fun CampaignTab(vm: RannViewModel, modifier: Modifier = Modifier) {
    val c = RannTheme.colors
    val nextLevel = vm.nextLevelToPlay()
    var selectedRank by remember { mutableStateOf(Rank.of((nextLevel - 1) / Balance.LEVELS_PER_RANK).index) }
    val levels = remember(selectedRank) {
        (selectedRank * Balance.LEVELS_PER_RANK + 1..selectedRank * Balance.LEVELS_PER_RANK + Balance.LEVELS_PER_RANK)
            .map { LevelFactory.spec(it) }
    }
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (0..6).forEach { i ->
                val r = Rank.of(i)
                val unlocked = vm.profile.isRankUnlocked(r)
                val cleared = com.example.rann.model.Progression.rankClearedCount(vm.profile.grades, i)
                val selected = selectedRank == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Color(r.color) else c.surface)
                        .border(
                            1.dp,
                            if (selected) Color(r.color) else c.border,
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (unlocked) "${r.icon} ${r.displayName} $cleared/50" else "${r.icon} 🔒 ${r.unlockWins} wins",
                        style = RannText.buttonSmall,
                        color = if (selected) Color.White else c.textMuted,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                val rank = Rank.of(selectedRank)
                RannCard(color = c.surfaceAlt) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = "${rank.icon} ${rank.displayName} — ${rank.mapNameHint}",
                            style = RannText.h3,
                            color = c.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = com.example.rann.model.Maps.of(selectedRank).twistText,
                            style = RannText.bodySmall,
                            color = c.textMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Coin multiplier ×%.1f".format(rank.coinMult),
                            style = RannText.label,
                            color = Rann.PrimaryDark,
                        )
                    }
                }
            }
            items(levels, key = { it.globalLevel }) { spec ->
                val grade = vm.profile.gradeFor(spec.globalLevel)
                val unlocked = vm.profile.isLevelUnlocked(spec.globalLevel)
                RannCard(
                    onClick = {
                        if (unlocked) {
                            Sound.tap()
                            vm.startLevel(spec.globalLevel)
                        } else {
                            vm.notify("Clear level ${spec.globalLevel - 3} first")
                        }
                    },
                    color = if (spec.globalLevel == nextLevel) Rann.Primary.copy(alpha = 0.07f) else c.surface,
                    borderColor = if (spec.globalLevel == nextLevel) Rann.Primary.copy(alpha = 0.4f) else c.border,
                    contentPadding = 14.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradeBadge(grade, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = buildString {
                                    append(if (spec.isBoss) "👑 " else if (spec.isMiniBoss) "⭐ " else "")
                                    append(spec.displayName())
                                    if (spec.isBoss) spec.bossSpec?.let { append(" — ${it.name}") }
                                },
                                style = RannText.h3,
                                color = if (unlocked) c.textPrimary else c.textMuted,
                            )
                            Text(
                                text = "${spec.npcCount} creatures · ${spec.creatureMix.joinToString(" ") { it.icon }} · ${spec.phaseLabel}",
                                style = RannText.label,
                                color = c.textMuted,
                            )
                            if (spec.modifiers.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    spec.modifiers.take(3).forEach { m: LevelModifier ->
                                        Text(
                                            text = "${m.icon} ${m.label}",
                                            style = RannText.label.copy(fontSize = 10.sp),
                                            color = c.textMuted,
                                        )
                                    }
                                }
                            }
                        }
                        if (!unlocked) {
                            Text("🔒", style = RannText.h3)
                        } else if (grade != LevelGrade.NONE) {
                            Text("×%.1f".format(spec.rank.coinMult), style = RannText.label, color = c.textMuted)
                        }
                    }
                }
            }
        }
    }
}

// ======================================================
// FORGE — 3x3 merge grid with drag & drop
// ======================================================

@Composable
private fun ForgeTab(vm: RannViewModel, modifier: Modifier = Modifier) {
    val c = RannTheme.colors
    @Suppress("UNUSED_EXPRESSION") vm.revision // keep the grid in sync with mutations
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScrollCompat(scroll)
            .padding(bottom = 24.dp),
    ) {
        RannCard(
            modifier = Modifier.fillMaxWidth(),
            color = Rann.Secondary.copy(alpha = 0.07f),
            borderColor = Rann.Secondary.copy(alpha = 0.3f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = vm.forgeProgress(),
                    size = 84.dp,
                    color = Rann.Secondary,
                ) {
                    Text("🔨", style = RannText.h2)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("The Furnace", style = RannText.h2, color = c.textPrimary)
                    Text(
                        text = "One dagger every %.1fs · %d merges".format(
                            vm.profile.forge.intervalSeconds,
                            vm.profile.forge.merges,
                        ),
                        style = RannText.bodySmall,
                        color = c.textMuted,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatChip("${vm.profile.forge.filledCount()}/9", "slots")
                        StatChip("💎", vm.profile.equippedWeapon().displayName)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Drag a weapon onto the same tier to merge. Different tiers swap places.",
            style = RannText.label,
            color = c.textMuted,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        ForgeGrid(vm)

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = if (vm.forgeNowCooldown > 0f) "Wait %.0fs".format(vm.forgeNowCooldown) else "Forge Now",
                icon = "🗡️",
                enabled = vm.forgeNowCooldown <= 0f,
                modifier = Modifier.weight(1f),
                onClick = { vm.forgeNow() },
            )
            SecondaryButton(
                text = "Speed Up ${Balance.SINK_FORGE_SPEEDUP}🪙",
                modifier = Modifier.weight(1f),
                onClick = { vm.forgeSpeedUp() },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(
                text = "Weapon Fragment ${Balance.SINK_WEAPON_FRAGMENT}🪙",
                modifier = Modifier.weight(1f),
                onClick = { vm.buyWeaponFragment() },
            )
            GhostButton(
                text = "Skip Grade ${Balance.SINK_GRADE_SKIP}🪙",
                modifier = Modifier.weight(1f),
                onClick = { vm.buyGradeSkip() },
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader("Arsenal", "Tap a tier you have forged to equip it.")
        val best = vm.profile.bestWeaponTier()
        (1..WeaponTier.maxTier()).forEach { tier ->
            val t = WeaponTier.of(tier)
            val owned = tier <= best
            val equipped = vm.profile.equippedWeaponTier == tier
            RannCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                onClick = {
                    if (owned) {
                        Sound.merge()
                        vm.equipWeaponTier(tier)
                    } else {
                        vm.notify("Merge up to ${t.displayName} first")
                    }
                },
                color = if (equipped) Rann.Primary.copy(alpha = 0.07f) else c.surface,
                borderColor = if (equipped) Rann.Primary.copy(alpha = 0.4f) else c.border,
                contentPadding = 14.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (owned) t.icon else "🔒", style = RannText.h2)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.displayName, style = RannText.h3, color = c.textPrimary)
                        Text(
                            text = "${t.damage.roundToInt()} dmg · ${t.reach.roundToInt()} reach · ${t.cooldown}s · ${t.summary}",
                            style = RannText.label,
                            color = c.textMuted,
                        )
                    }
                    if (equipped) StatChip("✓", "equipped", tint = Rann.Success)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Offline the furnace keeps smelting — up to 9 daggers waiting for you.",
            style = RannText.label,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun Modifier.verticalScrollCompat(state: androidx.compose.foundation.ScrollState): Modifier =
    this.then(androidx.compose.foundation.verticalScroll(state))

@Composable
private fun ForgeGrid(vm: RannViewModel) {
    val c = RannTheme.colors
    val forge = vm.profile.forge
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val cell: Dp = (maxWidth - gap * 2) / 3
        val positions = remember { HashMap<Int, Offset>() }
        val sizes = remember { HashMap<Int, Offset>() }
        var draggingFrom by remember { mutableStateOf<Int?>(null) }
        var pointerAbs by remember { mutableStateOf(Offset.Zero) }
        var gridOrigin by remember { mutableStateOf(Offset.Zero) }

        Box(modifier = Modifier.onGloballyPositioned { gridOrigin = it.positionInRoot() }) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                (0 until 3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        (0 until 3).forEach { col ->
                            val index = row * 3 + col
                            val tierIndex = forge.slots.getOrNull(index)
                            val tier = tierIndex?.let { WeaponTier.of(it) }
                            val isSource = draggingFrom == index
                            val isHover = draggingFrom != null && draggingFrom != index &&
                                hoverTarget(index, positions, sizes, pointerAbs) == index
                            Box(
                                modifier = Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            isHover -> Rann.Primary.copy(alpha = 0.18f)
                                            tier != null -> c.surface
                                            else -> Rann.SlotEmpty.copy(alpha = 0.55f)
                                        }
                                    )
                                    .border(
                                        width = if (isHover) 2.dp else 1.dp,
                                        color = when {
                                            isHover -> Rann.Primary
                                            tier != null -> c.border
                                            else -> c.border.copy(alpha = 0.6f)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                    .onGloballyPositioned {
                                        positions[index] = it.positionInRoot()
                                        sizes[index] = Offset(it.size.width.toFloat(), it.size.height.toFloat())
                                    }
                                    .pointerInput(index, tierIndex, vm.revision) {
                                        if (tierIndex == null) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { draggingFrom = index },
                                            onDragEnd = {
                                                val target = hoverTarget(index, positions, sizes, pointerAbs)
                                                draggingFrom = null
                                                if (target != null && target != index) {
                                                    vm.dropWeapon(index, target)
                                                }
                                            },
                                            onDragCancel = { draggingFrom = null },
                                            onDrag = { change, _ ->
                                                val base = positions[index]
                                                if (base != null) {
                                                    pointerAbs = Offset(
                                                        base.x + change.position.x,
                                                        base.y + change.position.y,
                                                    )
                                                }
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (tier != null && !isSource) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(tier.icon, style = RannText.h1.copy(fontSize = 30.sp))
                                        Text(tier.displayName, style = RannText.label, color = c.textMuted)
                                    }
                                } else if (tier == null) {
                                    Text("+", style = RannText.h2, color = c.textMuted.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
            // floating ghost while dragging
            draggingFrom?.let { from ->
                forge.slots.getOrNull(from)?.let { t ->
                    val weapon = WeaponTier.of(t)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pointerAbs.x - gridOrigin.x - cell.toPx() / 2f).roundToInt(),
                                    (pointerAbs.y - gridOrigin.y - cell.toPx() / 2f).roundToInt(),
                                )
                            }
                            .size(cell)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Rann.Primary.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(weapon.icon, style = RannText.h1.copy(fontSize = 30.sp))
                    }
                }
            }
        }
    }
}

private fun hoverTarget(
    source: Int,
    positions: Map<Int, Offset>,
    sizes: Map<Int, Offset>,
    pointer: Offset,
): Int? {
    val src = positions[source] ?: return null
    val size = sizes[source] ?: return null
    if (pointer.x == 0f && pointer.y == 0f) return null
    var best: Int? = null
    var bestDist = Float.MAX_VALUE
    positions.forEach { (index, pos) ->
        if (index == source) return@forEach
        val cx = pos.x + size.x / 2f
        val cy = pos.y + size.y / 2f
        val d = (cx - pointer.x) * (cx - pointer.x) + (cy - pointer.y) * (cy - pointer.y)
        if (d < bestDist) {
            bestDist = d
            best = index
        }
    }
    if (best == null) return null
    val reachSq = (size.x * 1.05f) * (size.x * 1.05f)
    return if (bestDist <= reachSq) best else null
}

// ======================================================
// ELITE SELECT
// ======================================================

@Composable
fun EliteScreen(vm: RannViewModel) {
    val c = RannTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        TopBar(
            title = "Elite Trials",
            subtitle = "1 v 1 · no creatures · no storm",
            onBack = { vm.screen = AppScreen.HOME },
            trailing = { StatChip("🏅", "${vm.profile.eliteCleared.size}/7") },
        )
        Spacer(Modifier.height(12.dp))
        if (!vm.profile.isEliteUnlocked()) {
            RannCard(modifier = Modifier.fillMaxWidth(), color = c.surfaceAlt) {
                Column(Modifier.fillMaxWidth()) {
                    Text("🔒 Locked", style = RannText.h2, color = c.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Clear Bronze level 50 to unlock the Elite arena. These are hand-tuned duels — " +
                            "the chimera hits hard and adapts to your habits.",
                        style = RannText.body,
                        color = c.textMuted,
                    )
                }
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                RannCard(color = c.surfaceAlt) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Rules of the pit", style = RannText.h3, color = c.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "You fight with 2× HP, 1.5× damage and 2 free revives. " +
                                "Boosters are not allowed. Your best weapon and armor still apply.",
                            style = RannText.bodySmall,
                            color = c.textMuted,
                        )
                    }
                }
            }
            items(com.example.rann.content.ChimeraFactory.ALL, key = { it.index }) { chimera ->
                val unlocked = vm.profile.isChimeraUnlocked(chimera.index)
                val cleared = vm.profile.eliteCleared.contains(chimera.index)
                RannCard(
                    onClick = {
                        if (unlocked) {
                            Sound.levelUp()
                            vm.startElite(chimera.index)
                        } else {
                            vm.notify(
                                if (chimera.index == 0) "Clear Bronze L50 to unlock the trials"
                                else "Beat ${com.example.rann.content.ChimeraFactory.of(chimera.index - 1).displayName} first",
                            )
                        }
                    },
                    color = if (cleared) Rann.Success.copy(alpha = 0.07f) else c.surface,
                    borderColor = if (cleared) Rann.Success.copy(alpha = 0.4f) else c.border,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (unlocked) chimera.emoji else "🔒", style = RannText.h2)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chimera.displayName, style = RannText.h3, color = c.textPrimary)
                            Text(
                                text = "${chimera.arenaName} · ${chimera.type.abilitySummary}",
                                style = RannText.bodySmall,
                                color = c.textMuted,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatChip("❤️", "${chimera.hp.roundToInt()}")
                                StatChip("🧠", "${chimera.abilities.size} atk · ${chimera.defenses.size} def")
                                StatChip("🥊", "Tier ${vm.profile.eliteRecommendedTier(chimera.index)}")
                            }
                        }
                        if (cleared) Text("✓", style = RannText.h2, color = Rann.Success)
                    }
                }
            }
        }
    }
}
