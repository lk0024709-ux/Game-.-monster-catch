package com.example.rann.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rann.audio.Sound
import com.example.rann.content.LevelFactory
import com.example.rann.model.Balance
import com.example.rann.model.LevelGrade
import com.example.rann.model.Maps
import com.example.rann.model.Rank
import com.example.rann.ui.CoinPill
import com.example.rann.ui.CreatureShowcase
import com.example.rann.ui.GradeBadge
import com.example.rann.ui.ProgressRing
import com.example.rann.ui.RannCard
import com.example.rann.ui.SectionHeader
import com.example.rann.ui.StatChip
import com.example.rann.ui.TopBar
import com.example.rann.ui.formatCoins
import com.example.rann.vm.AppScreen
import com.example.rann.vm.RannViewModel
import com.example.ui.theme.Rann
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme

@Composable
fun HomeScreen(vm: RannViewModel) {
    val c = RannTheme.colors
    val nextLevel = vm.nextLevelToPlay()
    val spec = LevelFactory.spec(nextLevel)
    val rank = spec.rank
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        TopBar(
            title = "Rannbhoomi",
            subtitle = "${rank.icon} ${vm.profile.equippedTitle} · ${vm.profile.wonCount()}/${Balance.TOTAL_LEVELS} cleared",
            trailing = { CoinPill(vm.profile.coins) },
        )
        Spacer(Modifier.height(14.dp))

        // ---------- HERO: next battle ----------
        RannCard(
            modifier = Modifier.fillMaxWidth(),
            color = Rann.Primary.copy(alpha = 0.06f),
            borderColor = Rann.Primary.copy(alpha = 0.35f),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(spec.map.ground),
                                    Color(spec.map.groundAlt),
                                )
                            )
                        ),
                ) {
                    CreatureShowcase(
                        modifier = Modifier.fillMaxSize(),
                        creature = spec.creatureMix.firstOrNull(),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                    ) {
                        StatChip(spec.map.icon, spec.map.name, tint = Rann.Accent)
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        StatChip(rank.icon, "Difficulty ${"%d%%".format((spec.difficulty * 100).toInt())}")
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (spec.isBoss) "Rank Boss · ${spec.bossSpec?.name ?: ""}" else "Level ${spec.globalLevel}",
                    style = RannText.h2,
                    color = c.textPrimary,
                )
                Text(
                    text = spec.summary(),
                    style = RannText.bodySmall,
                    color = c.textMuted,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = spec.map.twistText,
                    style = RannText.label,
                    color = c.textMuted,
                )
                if (spec.modifiers.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        spec.modifiers.take(4).forEach { m ->
                            StatChip(m.icon, m.label, tint = Rann.Accent)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                com.example.rann.ui.PrimaryButton(
                    text = if (spec.isBoss) "CHALLENGE BOSS" else "FIGHT",
                    icon = "⚔️",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        Sound.levelUp()
                        vm.startLevel(nextLevel)
                    },
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- PROGRESS ----------
        SectionHeader("Campaign Progress", "Every cleared level moves the rank bar.")
        RannCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress = vm.progressPercent(), size = 92.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(vm.progressPercent() * 100).toInt()}%",
                            style = RannText.h3,
                            color = c.textPrimary,
                        )
                        Text("done", style = RannText.label, color = c.textMuted)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    (0..6).forEach { i ->
                        val r = Rank.of(i)
                        val unlocked = vm.profile.isRankUnlocked(r)
                        val cleared = com.example.rann.model.Progression.rankClearedCount(vm.profile.grades, i)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${r.icon} ${r.displayName}",
                                style = RannText.bodySmall,
                                color = if (unlocked) Color(r.color) else c.textMuted.copy(alpha = 0.6f),
                                modifier = Modifier.width(112.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(c.surfaceAlt),
                            ) {
                                if (cleared > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(cleared / 50f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (unlocked) Rann.Primary else c.textMuted),
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (unlocked) "$cleared/50" else "🔒",
                                style = RannText.label,
                                color = c.textMuted,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- TILE GRID ----------
        SectionHeader("War Grounds", "Everything you need between battles.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile(
                modifier = Modifier.weight(1f),
                icon = "🧩",
                title = "Forge",
                body = "${vm.profile.forge.occupiedCount()}/9 slots · ${vm.profile.equippedWeapon().displayName}",
                tint = Rann.Secondary,
                onClick = {
                    Sound.tap()
                    vm.levelsTab = 1
                    vm.screen = AppScreen.LEVELS
                },
            )
            HomeTile(
                modifier = Modifier.weight(1f),
                icon = "🦁",
                title = "Elite Trials",
                body = if (vm.profile.isEliteUnlocked()) {
                    "${vm.profile.eliteCleared.size}/7 chimeras beaten"
                } else {
                    "Locked — clear Bronze L50"
                },
                tint = Rann.Accent,
                onClick = {
                    Sound.tap()
                    vm.screen = AppScreen.ELITE
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile(
                modifier = Modifier.weight(1f),
                icon = "🛒",
                title = "Store",
                body = "${vm.profile.equippedArmorRank().displayName} equipped",
                tint = Rann.PrimaryDark,
                onClick = {
                    Sound.tap()
                    vm.screen = AppScreen.STORE
                },
            )
            HomeTile(
                modifier = Modifier.weight(1f),
                icon = "⚙️",
                title = "Settings",
                body = "${vm.profile.achievements.size}/${com.example.rann.content.Content.ACHIEVEMENTS.size} achievements",
                tint = c.textMuted,
                onClick = {
                    Sound.tap()
                    vm.screen = AppScreen.SETTINGS
                },
            )
        }

        Spacer(Modifier.height(18.dp))

        // ---------- STATS STRIP ----------
        RannCard(modifier = Modifier.fillMaxWidth(), color = c.surfaceAlt) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MiniStat("🏅", vm.profile.stats.wins.toString(), "Wins")
                    MiniStat("💀", vm.profile.stats.losses.toString(), "Losses")
                    MiniStat("⚔️", vm.profile.stats.totalKills.toString(), "Kills")
                    MiniStat("🪙", formatCoins(vm.profile.stats.coinsEarned), "Earned")
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Best streak: ${vm.profile.stats.bestStreak}",
                        style = RannText.bodySmall,
                        color = c.textMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Today's bonus: ${if (vm.profile.dailyFirstWinAvailable(System.currentTimeMillis())) "available ✨" else "claimed"}",
                        style = RannText.bodySmall,
                        color = c.textMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Tip: creatures fight each other too — let them soften up before you dive in.",
            style = RannText.label,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun HomeTile(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    body: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val c = RannTheme.colors
    RannCard(modifier = modifier, onClick = onClick, contentPadding = 14.dp) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(icon, style = RannText.h3.copy(fontSize = 20.sp), color = c.textPrimary)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = RannText.h3, color = c.textPrimary)
            Text(body, style = RannText.label, color = c.textMuted)
        }
    }
}

@Composable
private fun MiniStat(icon: String, value: String, label: String) {
    val c = RannTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$icon $value", style = RannText.h3, color = c.textPrimary)
        Text(label, style = RannText.label, color = c.textMuted)
    }
}

/** Shared preview of a level card used by both Campaign & Elite list screens. */
@Composable
fun LevelPreviewChip(grade: LevelGrade, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GradeBadge(grade)
        Spacer(Modifier.width(10.dp))
        Text(label, style = RannText.body, color = RannTheme.colors.textPrimary)
    }
}

internal fun mapName(index: Int): String = Maps.of(index).name
