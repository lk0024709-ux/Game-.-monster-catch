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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rann.audio.Sound
import com.example.rann.content.Content
import com.example.rann.model.ArmorRank
import com.example.rann.model.Balance
import com.example.rann.model.BoosterType
import com.example.rann.ui.AnimatedCoinCounter
import com.example.rann.ui.CoinPill
import com.example.rann.ui.GhostButton
import com.example.rann.ui.PrimaryButton
import com.example.rann.ui.RannCard
import com.example.rann.ui.SecondaryButton
import com.example.rann.ui.SectionHeader
import com.example.rann.ui.StatChip
import com.example.rann.ui.TopBar
import com.example.rann.vm.AppScreen
import com.example.rann.vm.RannViewModel
import com.example.ui.theme.Rann
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme
import kotlin.math.roundToInt

@Composable
fun StoreScreen(vm: RannViewModel) {
    val c = RannTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        TopBar(
            title = "Store",
            subtitle = "Spend it before the pit takes it",
            onBack = { vm.screen = AppScreen.HOME },
            trailing = { CoinPill(vm.profile.coins) },
        )
        Spacer(Modifier.height(10.dp))
        AnimatedCoinCounter(
            target = vm.profile.coins,
            modifier = Modifier.fillMaxWidth(),
            style = RannText.h1,
            color = Rann.PrimaryDark,
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            // ---------------- ARMOR ----------------
            item { SectionHeader("Armour", "One set equipped at a time — the plate you wear decides how you survive.") }
            items(Content.ARMOR_ORDER, key = { it.name }) { armor ->
                val owned = vm.profile.ownsArmor(armor)
                val equipped = vm.profile.equippedArmor == armor.ordinal
                val affordable = vm.profile.coins >= armor.price
                RannCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (equipped) Color(armor.color).copy(alpha = 0.12f) else c.surface,
                    borderColor = if (equipped) Color(armor.color) else c.border,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(armor.color).copy(alpha = 0.22f))
                                .padding(10.dp),
                        ) {
                            Text(armor.icon, style = RannText.h2)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(armor.displayName, style = RannText.h3, color = c.textPrimary)
                            Text(armor.description, style = RannText.bodySmall, color = c.textMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    if (armor.hpBonus > 0) append("+${armor.hpBonus} HP  ")
                                    if (armor.defensePercent > 0f) append("${(armor.defensePercent * 100).roundToInt()}% block  ")
                                    if (armor.speedBonusPercent > 0f) append("+${(armor.speedBonusPercent * 100).roundToInt()}% speed  ")
                                    if (armor.regenPerSec > 0f) append("+${armor.regenPerSec.roundToInt()} HP/s  ")
                                    if (armor.reflectPercent > 0f) append("reflect ${(armor.reflectPercent * 100).roundToInt()}%  ")
                                    if (armor.revives > 0) append("+${armor.revives} revive")
                                }.trim(),
                                style = RannText.label,
                                color = c.textMuted,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    when {
                        equipped -> StatChip("✓", "Equipped", tint = Rann.Success)
                        owned -> SecondaryButton(
                            text = "Equip",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                Sound.tap()
                                vm.buyArmor(armor)
                            },
                        )
                        else -> PrimaryButton(
                            text = "Buy · ${armor.price} 🪙",
                            enabled = affordable,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { vm.buyArmor(armor) },
                        )
                    }
                }
            }

            // ---------------- BOOSTERS ----------------
            item { SectionHeader("Boosters", "Arm one before a fight. Consumed when the battle starts.") }
            items(Content.BOOSTER_ORDER, key = { it.name }) { booster: BoosterType ->
                val armed = vm.profile.armedBooster() == booster
                RannCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (armed) Rann.Primary.copy(alpha = 0.1f) else c.surface,
                    borderColor = if (armed) Rann.Primary else c.border,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(booster.icon, style = RannText.h1.copy(fontSize = 30.sp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(booster.displayName, style = RannText.h3, color = c.textPrimary)
                            Text(booster.description, style = RannText.bodySmall, color = c.textMuted)
                        }
                        Text(
                            text = if (armed) "ARMED" else "${booster.price} 🪙",
                            style = RannText.buttonSmall,
                            color = if (armed) Rann.PrimaryDark else c.textMuted,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (armed) {
                        GhostButton(
                            text = "Discard booster",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { vm.clearBooster() },
                        )
                    } else {
                        SecondaryButton(
                            text = "Arm for next battle",
                            enabled = vm.profile.coins >= booster.price,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { vm.buyBooster(booster) },
                        )
                    }
                }
            }

            // ---------------- SINKS ----------------
            item { SectionHeader("Workshop", "Coins into tools. Tools into victories.") }
            items(Content.SINKS, key = { it.id }) { sink ->
                RannCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sink.icon, style = RannText.h2)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sink.name, style = RannText.h3, color = c.textPrimary)
                            Text(sink.description, style = RannText.bodySmall, color = c.textMuted)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = "Use · ${sink.price} 🪙",
                        enabled = vm.profile.coins >= sink.price,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            when (sink.id) {
                                "speedup" -> vm.forgeSpeedUp()
                                "fragment" -> vm.buyWeaponFragment()
                                "gradeskip" -> vm.buyGradeSkip()
                            }
                        },
                    )
                }
            }

            item {
                RannCard(modifier = Modifier.fillMaxWidth(), color = c.surfaceAlt) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Earning coins", style = RannText.h3, color = c.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Base reward 40 + 8 per level in rank, multiplied by your grade " +
                                "(D×1, C×1.2, B×1.5, A×2, S×3, SS×5) and the rank bonus " +
                                "(Bronze×1 → Grand Masters×5). First clear pays 1.5× extra.",
                            style = RannText.bodySmall,
                            color = c.textMuted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Rannslayer title: +50% coins on everything, forever.",
                            style = RannText.label,
                            color = Rann.PrimaryDark,
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Maximum forge interval %.1fs · merge reward %d 🪙".format(
                        com.example.rann.forge.ForgeRules.MIN_INTERVAL,
                        Balance.MERGE_COINS,
                    ),
                    style = RannText.label,
                    color = c.textMuted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
