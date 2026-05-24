package com.example.core

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.AssetRepository
import com.example.game.SoundEffectsManager
import com.example.ui.components.CreatureRenderer
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameState
import com.example.ui.viewmodel.GameViewModel

// ======================================================
// MAIN ACTIVITY
// ======================================================

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SoundEffectsManager.initialize(this)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SafeGameContent {
                            GameNavigation(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundEffectsManager.release()
        AssetRepository.clear()
    }
}

// ======================================================
// SAFE COMPOSABLE WRAPPER
// ======================================================

@Composable
fun SafeGameContent(
    content: @Composable () -> Unit
) {
    content()
}

// ======================================================
// NAVIGATION
// ======================================================

@Composable
fun GameNavigation(
    viewModel: GameViewModel
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "game_navigation"
    ) { current ->
        when (current) {
            GameState.SPLASH -> SplashAdventureEntryScreen(
                onEnterSaves = {
                    viewModel.setGameState(GameState.MENU)
                },
                onEnterAssetForge = {
                    viewModel.setGameState(GameState.SETTINGS)
                }
            )

            GameState.MENU -> SaveSlotsView(
                viewModel = viewModel,
                onBack = {
                    viewModel.setGameState(GameState.SPLASH)
                }
            )

            GameState.WORLD -> WorldMapView(
                viewModel = viewModel
            )

            GameState.BATTLE -> BattleArenaView(
                viewModel = viewModel
            )

            GameState.INVENTORY -> SquadDexView(
                viewModel = viewModel,
                initialTab = viewModel.inventoryInitialTab,
                onClose = {
                    viewModel.setGameState(GameState.WORLD)
                }
            )

            GameState.SETTINGS -> AssetForgeView(
                viewModel = viewModel,
                onClose = {
                    viewModel.setGameState(GameState.SPLASH)
                }
            )
        }
    }
}

// ======================================================
// SPLASH INTRO SCREEN
// ======================================================

@Composable
fun SplashAdventureEntryScreen(
    onEnterSaves: () -> Unit,
    onEnterAssetForge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C071C), Color(0xFF241445), Color(0xFF0A0518))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Game branding subtitle
            Text(
                text = "OFFLINE RPG SIMULATOR",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Dynamic Title text styled creatively
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MYTHICMONS",
                    color = Color(0xFFFFD700),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "LEGENDS",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Splash mascot renderer (showing the cute fire mascot Pyrofox!)
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .background(Color(0xFFFF5722).copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                CreatureRenderer(
                    speciesId = "pyrofox",
                    isShiny = false,
                    scale = 1.2f,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Launch Button card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE040FB), Color(0xFF673AB7))
                            )
                        )
                        .clickable {
                            SoundEffectsManager.playLevelUp()
                            onEnterSaves()
                        }
                        .testTag("splash_start_button"),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START CAMPAIGN",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00FF7F), Color(0xFF00E5FF))
                            )
                        )
                        .clickable {
                            SoundEffectsManager.playLevelUp()
                            onEnterAssetForge()
                        }
                        .testTag("splash_asset_forge_button"),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = "Asset Forge", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ASSET FORGE & VFX",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = "Version 1.0.0 • No Internet Required • Made with 100% Original Lore",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
