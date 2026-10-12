package com.example.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.rann.audio.Sound
import com.example.rann.ui.PrimaryButton
import com.example.rann.ui.RannCard
import com.example.rann.ui.SecondaryButton
import com.example.rann.ui.screens.BattleScreen
import com.example.rann.ui.screens.EliteScreen
import com.example.rann.ui.screens.HomeScreen
import com.example.rann.ui.screens.LevelsScreen
import com.example.rann.ui.screens.SettingsScreen
import com.example.rann.ui.screens.SplashScreen
import com.example.rann.ui.screens.StoreScreen
import com.example.rann.vm.AppScreen
import com.example.rann.vm.RannViewModel
import com.example.ui.theme.RannText
import com.example.ui.theme.RannTheme
import com.example.ui.theme.RannbhoomiTheme
import kotlinx.coroutines.delay

/**
 * Blade Brawl: Rannbhoomi — single activity, canvas-driven battle royale RPG.
 * Elite battles run on the exclusive dark palette; everything else is the warm
 * editorial light theme.
 */
class MainActivity : ComponentActivity() {

    private val vm: RannViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Sound.init(this)
        setContent {
            val dark = vm.screen == AppScreen.BATTLE && (vm.battle?.isElite ?: false)
            RannbhoomiTheme(forceDark = dark) {
                Box(Modifier.fillMaxSize()) {
                    RannNavigation(vm)
                    ToastHost(vm)
                    OfflineForgeNotice(vm)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Sound.release()
    }
}

@Composable
fun RannNavigation(vm: RannViewModel) {
    AnimatedContent(
        targetState = vm.screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "rann_navigation",
    ) { screen ->
        when (screen) {
            AppScreen.SPLASH -> SplashScreen(vm)
            AppScreen.HOME -> HomeScreen(vm)
            AppScreen.LEVELS -> LevelsScreen(vm)
            AppScreen.ELITE -> EliteScreen(vm)
            AppScreen.STORE -> StoreScreen(vm)
            AppScreen.SETTINGS -> SettingsScreen(vm)
            AppScreen.BATTLE -> BattleScreen(vm)
        }
    }
}

/** Transient messages queued by the view model. */
@Composable
private fun ToastHost(vm: RannViewModel) {
    val message = vm.toasts.firstOrNull() ?: return
    LaunchedEffect(message, vm.toasts.size) {
        delay(1900)
        vm.consumeToast()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xE61C1917))
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                text = message,
                style = RannText.bodySmall,
                color = Color(0xFFF5F5F4),
            )
        }
    }
}

/** The furnace keeps working while the app is closed — tell the player what it made. */
@Composable
private fun OfflineForgeNotice(vm: RannViewModel) {
    var shown by remember { mutableStateOf(false) }
    val pending = vm.offlineForged
    if (shown || pending <= 0) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        RannCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Column {
                        Text("🔥", style = RannText.display)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "The furnace kept working",
                            style = RannText.h2,
                            color = RannTheme.colors.textPrimary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "$pending dagger${if (pending == 1) "" else "s"} were smelted while you were away. " +
                                "Merge them into something heavier.",
                            style = RannText.body,
                            color = RannTheme.colors.textMuted,
                        )
                }
                Spacer(Modifier.height(18.dp))
                Row {
                    SecondaryButton(
                        text = "Later",
                        modifier = Modifier.weight(1f),
                        onClick = { shown = true },
                    )
                    Spacer(Modifier.width(10.dp))
                    PrimaryButton(
                        text = "Open Forge",
                        icon = "🧩",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            shown = true
                            vm.levelsTab = 1
                            vm.screen = AppScreen.LEVELS
                        },
                    )
                }
            }
        }
    }
}
