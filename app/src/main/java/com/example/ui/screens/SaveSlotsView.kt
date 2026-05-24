package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerProgress
import com.example.game.SoundEffectsManager
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSlotsView(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C1B), Color(0xFF24153C), Color(0xFF0F0C1B))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .background(Color(0xFF2C194D).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("slots_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "SAVE REGISTRY",
                    color = Color(0xFFFFD700),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1.2f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select an existing offline save compartment or initialize a clean legendary campaign.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 3 Legendary Slots List
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                for (slot in 1..3) {
                    SaveSlotCard(
                        slotId = slot,
                        viewModel = viewModel,
                        onSelect = {
                            viewModel.selectSlot(slot)
                        },
                        onWipe = {
                            viewModel.deleteSlot(slot)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mythicmons Legends securely saves progress locally using SQLite frameworks. Fully offline compliant.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun SaveSlotCard(
    slotId: Int,
    viewModel: GameViewModel,
    onSelect: () -> Unit,
    onWipe: () -> Unit
) {
    var currentProgress by remember { mutableStateOf<PlayerProgress?>(null) }
    var loaded by remember { mutableStateOf(false) }

    // Fetch progress safely when card enters screen
    LaunchedEffect(key1 = slotId) {
        viewModel.getPlayerProgress(slotId).collect {
            currentProgress = it
            loaded = true
        }
    }

    val isNew = currentProgress == null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isNew) {
                        listOf(Color(0xFF1E1735), Color(0xFF130E26))
                    } else {
                        listOf(Color(0xFF2C1E4E), Color(0xFF1F1235))
                    },
                    start = Offset(0f, 0f),
                    end = Offset(500f, 500f)
                )
            )
            .clickable {
                SoundEffectsManager.playSelected()
                onSelect()
            }
            .padding(16.dp)
            .testTag("save_slot_$slotId")
    ) {
        if (!loaded) {
            CircularProgressIndicator(
                color = Color(0xFFFFD700),
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (isNew) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = "New Game",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "EMPTY SLOT $slotId",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Tap to begin high-fidelity adventure",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        } else {
            val progress = currentProgress!!
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crest Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF4527A0).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Crest",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Metadata Core
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = progress.playerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "g", tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${progress.coins}", color = Color(0xFFFFD54F), fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "gems", tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${progress.gems}", color = Color(0xFF80DEEA), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zone: ${progress.currentBiome} • Lv. ${progress.level}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                // Delete wipe button
                IconButton(
                    onClick = {
                        SoundEffectsManager.playCaptureFail()
                        onWipe()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .testTag("wipe_slot_$slotId")
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252).copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
