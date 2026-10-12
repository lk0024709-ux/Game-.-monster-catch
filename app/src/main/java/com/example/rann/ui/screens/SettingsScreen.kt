package com.example.rann.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.example.rann.content.Content
import com.example.rann.model.Balance
import com.example.rann.ui.GhostButton
import com.example.rann.ui.KeyValueRow
import com.example.rann.ui.ModalScrim
import com.example.rann.ui.PrimaryButton
import com.example.rann.ui.RannCard
import com.example.rann.ui.SecondaryButton
import com.example.rann.ui.SectionHeader
import com.example.rann.ui.StatChip
import com.example.rann.ui.ToggleRow
import com.example.rann.ui.TopBar
import com.example.rann.vm.AppScreen
import com.example.rann.vm.RannViewModel
import com.example.ui.theme.Rann
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: RannViewModel) {
    val c = RannTheme.colors
    var confirmReset by remember { mutableStateOf(false) }
    val settings = vm.profile.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        TopBar(
            title = "Settings",
            subtitle = "Comfort, identity, records",
            onBack = { vm.screen = AppScreen.HOME },
            trailing = { StatChip("🪙", "${vm.profile.coins}") },
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item { SectionHeader("Feel", "Everything is local — nothing leaves this device.") }
            item {
                RannCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        ToggleRow(
                            title = "Sound",
                            subtitle = "Swords, storms and paydays",
                            checked = settings.sound,
                            onChange = { vm.setSound(it) },
                        )
                        ToggleRow(
                            title = "Haptics",
                            subtitle = "Short buzz on hits and merges",
                            checked = settings.haptics,
                            onChange = { vm.setHaptics(it) },
                        )
                        ToggleRow(
                            title = "Screen shake",
                            subtitle = "Camera kick on heavy blows",
                            checked = settings.screenShake,
                            onChange = { vm.setScreenShake(it) },
                        )
                        ToggleRow(
                            title = "Floating numbers",
                            subtitle = "Damage and coin text in battle",
                            checked = settings.showFloatingText,
                            onChange = { vm.setFloatingText(it) },
                        )
                    }
                }
            }

            item { SectionHeader("Title", "Earned titles are permanent.") }
            item {
                RannCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Content.TITLES.forEach { title ->
                            val owned = vm.profile.titles.contains(title.label)
                            val equipped = vm.profile.equippedTitle == title.label
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (owned) "🏅" else "🔒",
                                    style = RannText.h3,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = title.label,
                                        style = RannText.body,
                                        color = if (owned) c.textPrimary else c.textMuted,
                                    )
                                    Text(title.requirement, style = RannText.label, color = c.textMuted)
                                }
                                if (equipped) {
                                    StatChip("✓", "worn", tint = Rann.Success)
                                } else if (owned) {
                                    GhostButton("Wear", onClick = { vm.equipTitle(title.label) })
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Achievements", "${vm.profile.achievements.size}/${Content.ACHIEVEMENTS.size} unlocked") }
            items(Content.ACHIEVEMENTS, key = { it.id }) { ach ->
                val unlocked = vm.profile.achievements.contains(ach.id)
                RannCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (unlocked) Rann.Success.copy(alpha = 0.07f) else c.surface,
                    contentPadding = 14.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (unlocked) Rann.Success.copy(alpha = 0.16f) else c.surfaceAlt)
                                .padding(8.dp),
                        ) {
                            Text(if (unlocked) ach.icon else "🔒", style = RannText.h3)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = ach.name,
                                style = RannText.h3,
                                color = if (unlocked) c.textPrimary else c.textMuted,
                            )
                            Text(ach.description, style = RannText.label, color = c.textMuted)
                        }
                        Text(
                            text = "+${ach.rewardCoins}",
                            style = RannText.bodySmall,
                            color = if (unlocked) Rann.Success else c.textMuted,
                        )
                    }
                }
            }

            item { SectionHeader("Career", "Lifetime numbers") }
            item {
                RannCard(modifier = Modifier.fillMaxWidth(), color = c.surfaceAlt) {
                    Column(Modifier.fillMaxWidth()) {
                        KeyValueRow("Matches played", "${vm.profile.stats.matchesPlayed}")
                        KeyValueRow("Wins / Losses", "${vm.profile.stats.wins} / ${vm.profile.stats.losses}")
                        KeyValueRow("Win streak (best)", "${vm.profile.winStreak} (${vm.profile.stats.bestStreak})")
                        KeyValueRow("Total kills", "${vm.profile.stats.totalKills}")
                        KeyValueRow("Damage dealt", "${vm.profile.stats.damageDealt.roundToInt()}")
                        KeyValueRow("Forge merges", "${vm.profile.forge.merges}")
                        KeyValueRow("Elite wins", "${vm.profile.stats.eliteWins}")
                        KeyValueRow("Flawless clears", "${vm.profile.stats.flawlessClears}")
                        KeyValueRow("Coins earned", "${vm.profile.stats.coinsEarned}")
                        KeyValueRow(
                            "Coin multiplier",
                            "×%.2f".format(vm.profile.earnedCoinMultiplier * 1.0f),
                        )
                    }
                }
            }

            item { SectionHeader("Danger zone", "There is no undo.") }
            item {
                GhostButton(
                    text = "Reset all progress",
                    icon = "⚠️",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirmReset = true },
                )
            }

            item {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Blade Brawl: Rannbhoomi · v1.0\n" +
                            "${Balance.TOTAL_LEVELS} campaign levels · 7 rank bosses · 7 elite chimeras · " +
                            "${com.example.rann.model.CreatureType.values().size} creatures · fully offline",
                        style = RannText.label,
                        color = c.textMuted,
                    )
                }
            }
        }
    }

    ModalScrim(visible = confirmReset, onDismiss = { confirmReset = false }) {
        RannCard(modifier = Modifier.fillMaxWidth(), color = RannTheme.colors.surface) {
            Column(Modifier.fillMaxWidth()) {
                Text("Reset everything?", style = RannText.h2, color = RannTheme.colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "All ${Balance.TOTAL_LEVELS} levels, grades, coins, forged weapons and titles " +
                        "will be wiped. The furnace starts cold again.",
                    style = RannText.body,
                    color = RannTheme.colors.textMuted,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "Keep playing",
                        modifier = Modifier.weight(1f),
                        onClick = { confirmReset = false },
                    )
                    PrimaryButton(
                        text = "Wipe save",
                        icon = "🗑️",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            vm.resetProgress()
                            confirmReset = false
                            vm.screen = AppScreen.SPLASH
                        },
                    )
                }
            }
        }
    }
}
