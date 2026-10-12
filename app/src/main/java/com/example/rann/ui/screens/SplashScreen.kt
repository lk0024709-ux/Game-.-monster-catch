package com.example.rann.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rann.model.Balance
import com.example.rann.vm.AppScreen
import com.example.rann.vm.RannViewModel
import com.example.ui.theme.Rann
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme
import com.example.rann.ui.CoinPill
import com.example.rann.ui.CreatureShowcase
import com.example.rann.ui.GhostButton
import com.example.rann.ui.PrimaryButton
import com.example.rann.ui.RannCard
import com.example.rann.ui.StatChip
import com.example.rann.audio.Sound

@Composable
fun SplashScreen(vm: RannViewModel) {
    val c = RannTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Rann.Cream, Rann.SurfaceAlt, Rann.Cream)))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = "BLADE BRAWL",
                style = RannText.display.copy(letterSpacing = 2.sp),
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "RANNBHOOMI",
                style = RannText.h1.copy(letterSpacing = 6.sp),
                color = Rann.Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "A lone warrior. A pit of wild creatures. Nobody is on your side.",
                style = RannText.body,
                color = c.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFDCC79C), Color(0xFFEDE7DB))))
                    .border(1.dp, c.border, RoundedCornerShape(24.dp)),
            ) {
                CreatureShowcase(
                    modifier = Modifier.fillMaxSize(),
                    creature = com.example.rann.model.CreatureType.GRIFFIN,
                )
            }

            Spacer(Modifier.height(18.dp))
            RannCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatChip("🏆", "${vm.profile.wonCount()} / ${Balance.TOTAL_LEVELS} cleared")
                        Spacer(Modifier.width(8.dp))
                        CoinPill(vm.profile.coins)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatChip("🎖️", vm.profile.equippedTitle)
                        Spacer(Modifier.width(8.dp))
                        StatChip("⚔️", vm.profile.equippedWeapon().displayName)
                        Spacer(Modifier.width(8.dp))
                        StatChip("🛡️", vm.profile.equippedArmorRank().displayName)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "ENTER THE ARENA",
                icon = "🔥",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Sound.levelUp()
                    vm.screen = AppScreen.HOME
                },
            )
            Spacer(Modifier.height(10.dp))
            GhostButton(
                text = "Settings & Progress",
                icon = "⚙️",
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.screen = AppScreen.SETTINGS },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "v1.0 · fully offline · 350 levels · 7 chimeras",
                style = RannText.label,
                color = c.textMuted.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}
