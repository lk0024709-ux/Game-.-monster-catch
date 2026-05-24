package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.game.SoundEffectsManager
import com.example.ui.viewmodel.GameViewModel

data class GameQuest(
    val id: String,
    val title: String,
    val description: String,
    val coinsReward: Int,
    val completed: Boolean
)

data class LegendaryBoss(
    val id: String,
    val name: String,
    val type: String,
    val subSpecies: String,
    val description: String
)

@Composable
fun QuestSummonView(
    viewModel: GameViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.playerProgress.collectAsState()

    val quests = remember {
        listOf(
            GameQuest("q1", "Forest Initiation", "Exhaust wild grass encounters in the Forest Kingdom to tarnish 3 wild mythics.", 300, true),
            GameQuest("q2", "Volcanic Retribution", "Defeat Fire Boss Ignis in the Lava Mountains.", 600, false),
            GameQuest("q3", "Frozen Diamond Search", "Travel to Snowy Tundra to find the ancient Frost Diamond.", 1200, false),
            GameQuest("q4", "Grand Arena Achievement", "Level up any captured creature to Level 20.", 1500, false)
        )
    }

    val bosses = remember {
        listOf(
            LegendaryBoss("solargryph", "SOLARGRYPH PRIDE", "LIGHT", "BIRD", "Protector of the high clouds. Radiates blinding celestial solar rays."),
            LegendaryBoss("voiddragon", "VOIDDRAGON TYRANT", "DRAGON", "DRAGON", "The master of black hole gravity cores. Commands cosmic rifts."),
            LegendaryBoss("runicgolem", "RUNIC GOLEM LORD", "MYSTIC", "ELEMENTAL", "An ancient robotic titan built by historians. Immune to basic status afflictions.")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF090518), Color(0xFF1B0E32), Color(0xFF0C071F))
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        SoundEffectsManager.playSelected()
                        onClose()
                    },
                    modifier = Modifier
                        .background(Color(0xFF2E194D).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("quests_close_btn")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "JOURNAL & RAID HUBS",
                    color = Color(0xFFFFD700),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Body content
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Text(
                        text = "JOURNAL STORY MILESTONES",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                items(quests) { q ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E143A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (q.completed) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                contentDescription = "status",
                                tint = if (q.completed) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = q.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = q.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Reward: +${q.coinsReward} Coins", color = Color(0xFFFFCC00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "LEGENDARY RAID SUMMONING PORTAL",
                        color = ColorxFFFF3D00(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Challenge massive level-35 boss editions of mythical guardians to earn gems. Highly lethal AI tactics.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                items(bosses) { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF281335)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = "D", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = b.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF3D00).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = b.type, color = Color(0xFFFF3D00), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = b.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    SoundEffectsManager.playHit()
                                    viewModel.startBossRaid(b.id)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("summon_boss_${b.id}")
                            ) {
                                Text("SUMMON BOSS ALIGNMENT (LV. 35)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ColorxFFFF3D00(): Color = Color(0xFFFF3D00)
