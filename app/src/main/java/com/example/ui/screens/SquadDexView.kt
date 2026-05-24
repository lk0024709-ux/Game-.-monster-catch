package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.model.CreatureEntity
import com.example.game.CreatureDatabase
import com.example.game.SoundEffectsManager
import com.example.ui.components.CreatureRenderer
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SquadDexView(
    viewModel: GameViewModel,
    initialTab: Int = 0, // 0 = SQUAD, 1 = DEX
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(initialTab) }
    val progress by viewModel.playerProgress.collectAsState()
    val creatures by viewModel.creatures.collectAsState()
    val team by viewModel.team.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C071C), Color(0xFF1E1136), Color(0xFF0C071C))
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
            // Header panel containing close trigger
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
                        .testTag("squad_close_btn")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Dual Tab Switcher
                Row(
                    modifier = Modifier
                        .background(Color(0xFF130A24), RoundedCornerShape(18.dp))
                        .padding(4.dp)
                ) {
                    TabToggleItem(
                        text = "ACTIVE SQUAD",
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 }
                    )
                    TabToggleItem(
                        text = "MYTHIC BOOK",
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Coin balances
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "Gold", tint = Color(0xFFFFCC00), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${progress?.coins ?: 0} G", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Body Area
            Crossfade(targetState = activeTab, label = "tab_fade", modifier = Modifier.weight(1f)) { tab ->
                when (tab) {
                    0 -> ActiveSquadSection(
                        team = team,
                        boxes = creatures.filter { it.teamOrder < 0 },
                        viewModel = viewModel
                    )
                    1 -> MythicBookSection(
                        capturedSpecies = creatures.map { it.speciesId }.toSet()
                    )
                }
            }
        }
    }
}

@Composable
fun TabToggleItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF673AB7) else Color.Transparent)
            .clickable {
                SoundEffectsManager.playSelected()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ActiveSquadSection(
    team: List<CreatureEntity>,
    boxes: List<CreatureEntity>,
    viewModel: GameViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "BATTLE SQUAD (${team.size}/6)",
            color = Color(0xFFFFD700),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Horizontal Grid of Active fighting creatures
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (team.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active crew! Add from boxes below.", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            items(team) { c ->
                SquadMemberCard(
                    creature = c,
                    isActive = true,
                    onSwap = { viewModel.changeTeamStatus(c) },
                    onFeedCandy = { viewModel.feedExpCandy(c) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "STORAGE SECRETS (PC BOXES)",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            if (boxes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PC Box Storage is empty. Visit wild grasses to capture mythics!", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(boxes) { c ->
                    SquadMemberCard(
                        creature = c,
                        isActive = false,
                        onSwap = { viewModel.changeTeamStatus(c) },
                        onFeedCandy = { viewModel.feedExpCandy(c) }
                    )
                }
            }
        }
    }
}

@Composable
fun SquadMemberCard(
    creature: CreatureEntity,
    isActive: Boolean,
    onSwap: () -> Unit,
    onFeedCandy: () -> Unit
) {
    val candyCost = creature.level * 45

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("squad_card_${creature.instanceId}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F123C))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Model Render
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF2D1B50).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                CreatureRenderer(
                    speciesId = creature.speciesId,
                    isShiny = creature.isShiny,
                    scale = 0.9f,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Metadata Core
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = creature.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                when (creature.type) {
                                    "FIRE" -> Color(0xFFE64A19).copy(alpha = 0.2f)
                                    "WATER" -> Color(0xFF1976D2).copy(alpha = 0.2f)
                                    "NATURE" -> Color(0xFF388E3C).copy(alpha = 0.2f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = creature.type,
                            color = when (creature.type) {
                                "FIRE" -> Color(0xFFFF5722)
                                "WATER" -> Color(0xFF2196F3)
                                "NATURE" -> Color(0xFF4CAF50)
                                else -> Color.LightGray
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(text = "LV. ${creature.level}", color = Color(0xFFFFC107), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                // Micro stat logs
                Text(
                    text = "ATK: ${creature.attack} • DEF: ${creature.defense} • SPD: ${creature.speed}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Feed level card Button
                Button(
                    onClick = { onFeedCandy() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CA64C)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "F", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Level Up ($candyCost G)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Push Box/Squad button
                IconButton(
                    onClick = { onSwap() },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF5E35B1), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Swap",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MythicBookSection(
    capturedSpecies: Set<String>
) {
    val registry = CreatureDatabase.species

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "MYTHIC REGISTERED CONCORDANCE (${capturedSpecies.size}/${registry.size})",
            color = Color(0xFFFFD700),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(registry) { spec ->
                val hasCaught = capturedSpecies.contains(spec.id)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasCaught) Color(0xFF2C1C4E) else Color(0xFF1E1430)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(50.dp)) {
                            if (hasCaught) {
                                CreatureRenderer(speciesId = spec.id, isShiny = false, modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = "?",
                                    tint = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (hasCaught) spec.name else "???",
                            color = if (hasCaught) Color.White else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (hasCaught) spec.rarity else "UNKNOWN Rarity",
                            color = when (spec.rarity) {
                                "COMMON" -> Color.LightGray
                                "RARE" -> Color(0xFF2196F3)
                                "EPIC" -> Color(0xFFE040FB)
                                else -> Color(0xFFFFD700)
                            }.copy(alpha = if (hasCaught) 1f else 0.4f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
