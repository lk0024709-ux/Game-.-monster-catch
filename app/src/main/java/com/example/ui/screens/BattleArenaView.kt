package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CreatureDatabase
import com.example.game.MoveTemplate
import com.example.game.SoundEffectsManager
import com.example.ui.components.CreatureRenderer
import com.example.ui.viewmodel.BattleState
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BattleArenaView(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val battleState by viewModel.battleState.collectAsState()
    val inv by viewModel.inventory.collectAsState()

    val scope = rememberCoroutineScope()
    val logListState = rememberLazyListState()

    if (battleState == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFFD700))
        }
        return
    }

    val b = battleState!!
    val player = b.playerCreature!!
    val enemy = b.enemyCreature!!

    // Dynamic rotation for capture rolls
    val infiniteTransition = rememberInfiniteTransition(label = "capture_ball")
    val rollAngle by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ball_rotation"
    )

    // Scroll to latest log item automatically
    LaunchedEffect(b.battleLogs.size) {
        if (b.battleLogs.isNotEmpty()) {
            scope.launch {
                logListState.animateScrollToItem(b.battleLogs.size - 1)
            }
        }
    }

    // Camera Shake translation offsets
    val shakeOffset = if (b.cameraShake) {
        (-15..15).random().toFloat()
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(translationX = shakeOffset, translationY = shakeOffset)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0720), Color(0xFF1C0D32), Color(0xFF0B0515))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Stats HUD Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (b.isBoss) "🔥 BOSS RAID ARENA" else "🏆 BATTLE MODE",
                    color = Color(0xFFFFD700),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Orbs: ${inv["orb"] ?: 0}",
                        color = Color(0xFF80DEEA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Potions: ${inv["potion"] ?: 0}",
                        color = Color(0xFF81C784),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upper Split Area: Facing visual renders of creatures
            Row(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Area: Player Monster HUD Card
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LEVEL ${player.level}",
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(105.dp)) {
                        CreatureRenderer(
                            speciesId = player.speciesId,
                            isShiny = player.isShiny,
                            scale = 1.0f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Progress Health Bar
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "HP ${player.hp}/${player.maxHp}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { player.hp.toFloat() / player.maxHp.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (player.hp.toFloat() / player.maxHp.toFloat() > 0.4f) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }

                // Middle split spacer
                Text(
                    text = "VS",
                    color = ColorxFFFFD700(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Right Area: Enemy / Savage target HUD Card
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LEVEL ${enemy.level}",
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = enemy.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(if (b.isBoss) 125.dp else 105.dp)) {
                        CreatureRenderer(
                            speciesId = enemy.speciesId,
                            isShiny = enemy.isShiny,
                            scale = if (b.isBoss) 1.25f else 1.0f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Progress Health Bar
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "HP ${enemy.hp}/${enemy.maxHp}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { enemy.hp.toFloat() / enemy.maxHp.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (enemy.hp.toFloat() / enemy.maxHp.toFloat() > 0.4f) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Area: Narrative logs screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.3f))
            ) {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(b.battleLogs) { log ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "▶", color = Color(0xFFFFD700), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp, end = 6.dp))
                            Text(
                                text = log,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capture Cinematic rolling sphere overlay
            if (b.isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color(0xFF2C1C4E), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Capture ball",
                            tint = Color(0xFFFF2E63),
                            modifier = Modifier
                                .size(44.dp)
                                .rotate(if (b.captureRollState in 1..3) rollAngle else 0f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (b.captureRollState) {
                                1 -> "Mythic Orb shakes once... 💫"
                                2 -> "Mythic Orb shakes twice... 🌟"
                                3 -> "Mythic Orb shakes three times... ✨"
                                4 -> "SUCCESSFULLY TAMED!"
                                else -> "Attempting calibration..."
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else if (b.isFinished) {
                // Game Finished options
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2C1D43)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "THE MATCH HAS CONCLUDED",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // Action Choice panel
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val learnedMoves = player.moves.split(",")
                    
                    // Attack Skill Buttons Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 0..1) {
                            val mvId = learnedMoves.getOrNull(i) ?: "tackle"
                            val moveDetails = CreatureDatabase.moves[mvId] ?: CreatureDatabase.moves["tackle"]!!
                            
                            AttackMoveButton(
                                move = moveDetails,
                                enabled = b.isPlayerTurn && !b.isFinished,
                                onClick = {
                                    viewModel.executePlayerAttack(mvId)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("attack_move_btn_$mvId")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 2..3) {
                            val mvId = learnedMoves.getOrNull(i)
                            val moveDetails = if (mvId != null) CreatureDatabase.moves[mvId] else null

                            if (moveDetails != null) {
                                AttackMoveButton(
                                    move = moveDetails,
                                    enabled = b.isPlayerTurn && !b.isFinished,
                                    onClick = {
                                        viewModel.executePlayerAttack(mvId!!)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("attack_move_btn_$mvId")
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f)) // spacer
                            }
                        }
                    }

                    // Auxiliary options (Potion, Orb, Flee)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Restore health potion
                        Button(
                            onClick = { viewModel.usePotionInBattle() },
                            enabled = b.isPlayerTurn && !b.isFinished,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("battle_potion_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "P", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Potion", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Catch Orb taming
                        Button(
                            onClick = { viewModel.attemptCaptureInBattle() },
                            enabled = b.isPlayerTurn && !b.isFinished && !b.isBoss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5114E)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("battle_orb_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "O", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (b.isBoss) "No Capture" else "Catch Orb", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Exit combat fleeing
                        Button(
                            onClick = { viewModel.triggerBattleFlee() },
                            enabled = b.isPlayerTurn && !b.isFinished,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("battle_flee_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "R", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flee", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttackMoveButton(
    move: MoveTemplate,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            SoundEffectsManager.playSelected()
            onClick()
        },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (move.type) {
                "FIRE" -> Color(0xFFE64A19)
                "WATER" -> Color(0xFF1976D2)
                "NATURE" -> Color(0xFF388E3C)
                "ELECTRIC" -> Color(0xFFFBC02D)
                "ICE" -> Color(0xFF0097A7)
                "DARK" -> Color(0xFF512DA8)
                "WIND" -> Color(0xFF78909C)
                else -> Color(0xFF424242)
            },
            contentColor = if (move.type == "ELECTRIC") Color.Black else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = move.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Dmg: ${move.power}", fontSize = 9.sp, color = if (move.type == "ELECTRIC") Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f))
                Text(text = move.type, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun ColorxFFFFD700(): Color = Color(0xFFFFD700)
