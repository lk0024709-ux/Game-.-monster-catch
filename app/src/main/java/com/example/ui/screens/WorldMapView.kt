package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.SoundEffectsManager
import com.example.ui.viewmodel.GameState
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ======================================================
// PROCEDURAL INFINITE CHUNK & TILE MAP SYSTEM (AAA)
// ======================================================

data class MapElement(
    val id: String,
    val x: Float,
    val y: Float,
    val type: String, // "ROCK", "WATER", "GRASS", "TREASURE", "PORTAL", etc.
    val scale: Float = 1.0f,
    var collected: Boolean = false
)

object ProceduralWorld {
    
    // Generates elements deterministically for any 400x400 chunk grid location
    fun getChunkElements(chunkX: Int, chunkY: Int, currentSlot: Int, currentBiome: String): List<MapElement> {
        val seed = currentSlot * 31L + chunkX * 73L + chunkY * 113L + currentBiome.hashCode()
        val random = Random(seed)
        val list = mutableListOf<MapElement>()
        
        val ox = chunkX * 400f
        val oy = chunkY * 400f
        
        // Don't spawn blocking elements right on the spawn circle zone of chunk (0,0) (around x:150, y:150)
        val isSpawnChunk = chunkX == 0 && chunkY == 0
        
        // Spawn 6 to 9 random obstacles/grass clumps
        val count = random.nextInt(6, 10)
        for (i in 0 until count) {
            val tx = random.nextFloat() * 320f + 40f
            val ty = random.nextFloat() * 320f + 40f
            
            if (isSpawnChunk && Math.hypot((tx - 150f).toDouble(), (ty - 150f).toDouble()) < 60.0) {
                continue // skip close to spawn center to avoid trap blocks
            }
            
            val type = when (currentBiome) {
                "LAVA" -> if (random.nextFloat() < 0.45f) "LAVA_FISSURE" else if (random.nextFloat() < 0.75f) "GRASS" else "ROCK"
                "SNOW" -> if (random.nextFloat() < 0.40f) "ICE_BLOCK" else if (random.nextFloat() < 0.70f) "GRASS" else "ROCK"
                "SWAMP" -> if (random.nextFloat() < 0.45f) "WATER" else if (random.nextFloat() < 0.75f) "GRASS" else "ROCK"
                "CAVE" -> if (random.nextFloat() < 0.40f) "ROCK" else if (random.nextFloat() < 0.70f) "GRASS" else "CRYSTAL_WALL"
                "SKY_ISLANDS" -> if (random.nextFloat() < 0.35f) "SKY_VOID" else if (random.nextFloat() < 0.70f) "GRASS" else "ROCK"
                "CRYSTAL_LANDS" -> if (random.nextFloat() < 0.40f) "CRYSTAL_WALL" else if (random.nextFloat() < 0.70f) "GRASS" else "ROCK"
                "DAK_WASTELAND", "DARK_WASTELAND" -> if (random.nextFloat() < 0.45f) "ASH_PIT" else if (random.nextFloat() < 0.75f) "GRASS" else "ROCK"
                "OCEAN" -> if (random.nextFloat() < 0.70f) "WATER" else "GRASS"
                "RUINS" -> if (random.nextFloat() < 0.45f) "RUIN_WALL" else if (random.nextFloat() < 0.75f) "GRASS" else "ROCK"
                else -> if (random.nextFloat() < 0.30f) "ROCK" else if (random.nextFloat() < 0.65f) "GRASS" else "WATER"
            }
            list.add(MapElement("${chunkX}_${chunkY}_obj_$i", ox + tx, oy + ty, type))
        }
        
        // Spawn 1 chest per chunk with 35% probability
        if (random.nextFloat() < 0.35f) {
            list.add(MapElement("${chunkX}_${chunkY}_chest", ox + random.nextFloat() * 300f + 50f, oy + random.nextFloat() * 300f + 50f, "TREASURE"))
        }
        
        // Spawn portals in chunks that are divisible by 2 for easy fast-travel accessibility
        if (Math.abs(chunkX) % 2 == 0 && Math.abs(chunkY) % 2 == 0) {
            list.add(MapElement("${chunkX}_${chunkY}_portal", ox + 200f, oy + 240f, "PORTAL"))
        }
        
        return list
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorldMapView(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.playerProgress.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val isDay by viewModel.isDay.collectAsState()
    val currentSlot by viewModel.currentSlot.collectAsState()

    val scope = rememberCoroutineScope()

    var showShop by remember { mutableStateOf(false) }
    var showBiomeMap by remember { mutableStateOf(false) }
    var showQuestPanel by remember { mutableStateOf(false) }
    var overlayText by remember { mutableStateOf("") }

    // Session cache tracking collected procedural chest identifiers to prevent infinite gold exploits
    val collectedChests = remember { mutableStateMapOf<String, Boolean>() }

    if (progress == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFFD700))
        }
        return
    }

    val player = progress!!
    val currentBiome = player.currentBiome.uppercase()

    // Determine current surrounding 3x3 chunks depending on playerX / playerY
    val pxVal = player.playerX
    val pyVal = player.playerY
    val centerChunkX = (pxVal / 400f).toInt()
    val centerChunkY = (pyVal / 400f).toInt()

    val visibleElements = remember(centerChunkX, centerChunkY, currentSlot, currentBiome) {
        val list = mutableListOf<MapElement>()
        for (cx in (centerChunkX - 1)..(centerChunkX + 1)) {
            for (cy in (centerChunkY - 1)..(centerChunkY + 1)) {
                list.addAll(ProceduralWorld.getChunkElements(cx, cy, currentSlot, currentBiome))
            }
        }
        list
    }

    // Dynamic weather background particle drift timers
    val ticker = remember { Animatable(0f) }
    LaunchedEffect(weather) {
        ticker.snapTo(0f)
        ticker.animateTo(
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 60000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    // Handles stepping, chunk collision check, grass encounters, and portal launches
    fun attemptStep(dx: Float, dy: Float) {
        val targetX = (player.playerX + dx).coerceIn(-10000f, 10000f)
        val targetY = (player.playerY + dy).coerceIn(-10000f, 10000f)

        var collided = false
        var steppedOnGrass = false

        for (el in visibleElements) {
            if (el.type == "TREASURE" && collectedChests.containsKey(el.id)) continue
            
            val dist = Math.hypot((targetX - el.x).toDouble(), (targetY - el.y).toDouble()).toFloat()
            if (dist < 26f) {
                // Obstacles segment blocking
                val isBarrier = el.type == "ROCK" || el.type == "WATER" || el.type == "LAVA_FISSURE" || 
                                el.type == "ICE_BLOCK" || el.type == "CRYSTAL_WALL" || el.type == "SKY_VOID" || 
                                el.type == "ASH_PIT" || el.type == "RUIN_WALL"
                
                if (isBarrier) {
                    collided = true
                    overlayText = "Blocked by ${el.type.replace("_", " ")}!"
                    scope.launch { delay(900); if (overlayText.startsWith("Blocked")) overlayText = "" }
                } else if (el.type == "GRASS") {
                    steppedOnGrass = true
                } else if (el.type == "TREASURE") {
                    collectedChests[el.id] = true
                    viewModel.addCoins(250)
                    SoundEffectsManager.playLevelUp()
                    overlayText = "Discovered Lost Chest! Obtained +250 Gold!"
                    scope.launch { delay(1600); overlayText = "" }
                } else if (el.type == "PORTAL") {
                    showBiomeMap = true
                    SoundEffectsManager.playCured()
                }
            }
        }

        if (!collided) {
            viewModel.updatePlayerLocation(targetX, targetY)

            // Random Wild Battle Spawn: 12% probability on Grass step
            if (steppedOnGrass && Random.nextFloat() <= 0.12f) {
                overlayText = "⚠️ WILD ENCOUNTER!"
                SoundEffectsManager.playCaptureFail()
                scope.launch {
                    delay(1100)
                    overlayText = ""
                    viewModel.startWildBattle()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B1A))
    ) {
        // Core Viewport Camera Scrolling Container
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Render programmatically generated procedural tile layers & biome maps
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                
                val sx = cx - player.playerX
                val sy = cy - player.playerY

                // Render 3x3 Chunks surrounding player
                for (cxIdx in (centerChunkX - 1)..(centerChunkX + 1)) {
                    for (cyIdx in (centerChunkY - 1)..(centerChunkY + 1)) {
                        
                        val chX = cxIdx * 400f
                        val chY = cyIdx * 400f
                        
                        // Draw 10x10 tile block inside each chunk (tile size 40x40)
                        for (tx in 0 until 10) {
                            for (ty in 0 until 10) {
                                val tileX = chX + tx * 40f + sx
                                val tileY = chY + ty * 40f + sy
                                
                                // Offscreen frustum culling to optimize rendering memory on Android
                                if (tileX < -40f || tileX > size.width || tileY < -40f || tileY > size.height) {
                                    continue
                                }

                                drawTilePattern(tileX, tileY, tx, ty, cxIdx, cyIdx, currentBiome)
                            }
                        }
                    }
                }

                // Render dynamic Map elements (Grass, Rocks, Chests, Portals)
                for (el in visibleElements) {
                    val exLocal = el.x + sx
                    val eyLocal = el.y + sy

                    // Frustum cull element
                    if (exLocal < -40f || exLocal > size.width + 40f || eyLocal < -40f || eyLocal > size.height + 40f) {
                        continue
                     }

                    when (el.type) {
                        "GRASS" -> {
                            drawCircle(Color(0xFF388E3C).copy(alpha = 0.5f), radius = 18f, center = Offset(exLocal, eyLocal))
                            drawLine(Color(0xFF4CAF50), Offset(exLocal - 10f, eyLocal + 12f), Offset(exLocal - 2f, eyLocal - 12f), strokeWidth = 5f)
                            drawLine(Color(0xFF8BC34A), Offset(exLocal + 10f, eyLocal + 12f), Offset(exLocal + 2f, eyLocal - 12f), strokeWidth = 5f)
                        }
                        "ROCK" -> {
                            val path = Path().apply {
                                moveTo(exLocal, eyLocal - 18f)
                                lineTo(exLocal + 16f, eyLocal + 14f)
                                lineTo(exLocal - 16f, eyLocal + 14f)
                                close()
                            }
                            drawPath(path, Color(0xFF6D4C41))
                            drawPath(path, Color(0xFF8D6E63), style = Stroke(width = 3f))
                        }
                        "WATER" -> {
                            drawCircle(Color(0xFF0288D1).copy(alpha = 0.7f), radius = 22f, center = Offset(exLocal, eyLocal))
                            drawCircle(Color.White.copy(alpha = 0.2f), radius = 15f, center = Offset(exLocal, eyLocal), style = Stroke(width = 2f))
                        }
                        "LAVA_FISSURE" -> {
                            // Boiling magma hole
                            drawCircle(Color(0xFF260000), radius = 20f, center = Offset(exLocal, eyLocal))
                            drawCircle(Color(0xFFFF3D00).copy(alpha = 0.8f), radius = 14f, center = Offset(exLocal, eyLocal))
                        }
                        "ICE_BLOCK" -> {
                            val path = Path().apply {
                                moveTo(exLocal - 14f, eyLocal - 14f)
                                lineTo(exLocal + 14f, eyLocal - 14f)
                                lineTo(exLocal + 14f, eyLocal + 14f)
                                lineTo(exLocal - 14f, eyLocal + 14f)
                                close()
                            }
                            drawPath(path, Color(0xFF80DEEA))
                            drawPath(path, Color.White, style = Stroke(width = 2.5f))
                        }
                        "CRYSTAL_WALL" -> {
                            val path = Path().apply {
                                moveTo(exLocal, eyLocal - 22f)
                                lineTo(exLocal + 12f, eyLocal)
                                lineTo(exLocal, eyLocal + 22f)
                                lineTo(exLocal - 12f, eyLocal)
                                close()
                            }
                            drawPath(path, Color(0xFFE91E63).copy(alpha = 0.8f))
                        }
                        "SKY_VOID" -> {
                            // Falling pit holes
                            drawCircle(Color(0xFF0F0B1A), radius = 18f, center = Offset(exLocal, eyLocal))
                        }
                        "ASH_PIT" -> {
                            drawCircle(Color(0xFF1E1E1E), radius = 18f, center = Offset(exLocal, eyLocal))
                            drawCircle(Color(0xFFBA68C8).copy(alpha = 0.4f), radius = 14f, center = Offset(exLocal, eyLocal))
                        }
                        "RUIN_WALL" -> {
                            drawRect(Color(0xFF78909C), topLeft = Offset(exLocal - 15f, eyLocal - 15f), size = Size(30f, 30f))
                            drawRect(Color(0xFFFFD700).copy(alpha = 0.3f), topLeft = Offset(exLocal - 10f, eyLocal - 10f), size = Size(20f, 20f))
                        }
                        "TREASURE" -> {
                            if (!collectedChests.containsKey(el.id)) {
                                drawRect(Color(0xFFFFD700), topLeft = Offset(exLocal - 12f, eyLocal - 8f), size = Size(24f, 16f))
                                drawRect(Color(0xFF3E2723), topLeft = Offset(exLocal - 12f, eyLocal - 12f), size = Size(24f, 4f)) // Lid
                                drawCircle(Color.White, radius = 2.5f, center = Offset(exLocal, eyLocal))
                            }
                        }
                        "PORTAL" -> {
                            // Rotating fast travel teleporter portals
                            drawCircle(Color(0xFFD500F9).copy(alpha = 0.5f), radius = 26f, center = Offset(exLocal, eyLocal))
                            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.7f), radius = 16f, center = Offset(exLocal, eyLocal))
                        }
                    }
                }

                // Render dynamic Weather particle overlays (such as falling rain, snow, sandstorm drifts)
                drawActiveWeatherOverlays(weather, ticker.value)

                // Draw centered player sprite
                drawCircle(Color(0xFFFFD700), radius = 14f, center = Offset(cx, cy))
                drawRect(Color(0xFF512DA8), topLeft = Offset(cx - 15f, cy - 18f), size = Size(30f, 6f)) // Adventurer cap
                drawCircle(Color.Black, radius = 3f, center = Offset(cx - 5f, cy - 4f))
                drawCircle(Color.Black, radius = 3f, center = Offset(cx + 5f, cy - 4f))

                // Time-of-day dynamic overlay shading
                if (!isDay) {
                    drawRect(Color(0xFF0C0924).copy(alpha = 0.55f), size = size) // Midnight ambience
                } else {
                    drawRect(Color(0xFFFFEB3B).copy(alpha = 0.05f), size = size) // Warm sun shimmer
                }
            }

            // Top Left Info Banner (Coins, Gems, Biome)
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .statusBarsPadding(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF241445).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$currentBiome REALM",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = "Coins", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${player.coins}", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = "Gem", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${player.gems}", color = Color(0xFF80DEEA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top Right Sidebar buttons (SHOP, QUEST, WORLD)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Exit Save Slot button
                Button(
                    onClick = { viewModel.setGameState(GameState.SPLASH) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Saves", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MENU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FloatingActionButton(
                    onClick = {
                        SoundEffectsManager.playSelected()
                        showShop = true
                    },
                    containerColor = Color(0xFF4527A0),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_shop_fab")
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Shop")
                }

                FloatingActionButton(
                    onClick = {
                        SoundEffectsManager.playSelected()
                        viewModel.openInventory(1)
                    },
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_dex_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Dex")
                }

                FloatingActionButton(
                    onClick = {
                        SoundEffectsManager.playSelected()
                        viewModel.openInventory(0)
                    },
                    containerColor = Color(0xFFE040FB),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_team_btn")
                ) {
                    Icon(Icons.Filled.AccountBox, contentDescription = "Team")
                }

                FloatingActionButton(
                    onClick = {
                        SoundEffectsManager.playSelected()
                        showQuestPanel = true
                    },
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color.Black,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_quests_btn")
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "Boss Raids")
                }
            }

            // Warning Overlay Alert Texts
            AnimatedVisibility(
                visible = overlayText.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF2E63)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = overlayText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            // Interactive Bottom Panel hosting direction controls and active status
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Weather Indicator and Quick Biome Switcher
                    Card(
                        modifier = Modifier
                            .width(135.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1436).copy(alpha = 0.85f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "WEATHER SYSTEM",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "w",
                                    tint = Color(0xFFFFEB3B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = weather,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    SoundEffectsManager.playSelected()
                                    showBiomeMap = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Map Travel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Retro D-PAD Movement Control (Compass)
                    VirtualRetroDPad(
                        onMove = { dx, dy -> attemptStep(dx, dy) }
                    )
                }
            }
        }

        // Shop Overlay dialog
        if (showShop) {
            MapShopOverlay(viewModel = viewModel, onClose = { showShop = false })
        }

        // Biome Portal Map Switcher supporting all 10 custom biomes
        if (showBiomeMap) {
            BiomeTravelDialog(
                unlockedList = player.unlockedBiomes.split(","),
                onSelect = { selectedBiome ->
                    viewModel.changeBiome(selectedBiome)
                    showBiomeMap = false
                },
                onCancel = { showBiomeMap = false }
            )
        }

        if (showQuestPanel) {
            QuestSummonView(viewModel = viewModel, onClose = { showQuestPanel = false })
        }
    }
}

// Draw biome tile backgrounds procedurally with dynamic colors & grids
private fun DrawScope.drawTilePattern(tileX: Float, tileY: Float, tx: Int, ty: Int, cxIdx: Int, cyIdx: Int, biome: String) {
    val seed = cxIdx * 17L + cyIdx * 31L + tx * 101L + ty * 303L
    val random = Random(seed)

    val baseColor = when (biome) {
        "FOREST" -> Color(0xFF2E7D32)
        "LAVA" -> Color(0xFF210000)
        "SNOW" -> Color(0xFFECEFF1)
        "SWAMP" -> Color(0xFF1B2E09)
        "CAVE" -> Color(0xFF1A0933)
        "SKY_ISLANDS" -> Color(0xFF0D47A1)
        "CRYSTAL_LANDS" -> Color(0xFF3F1D72)
        "DAK_WASTELAND", "DARK_WASTELAND" -> Color(0xFF1E1E1E)
        "OCEAN" -> Color(0xFF01579B)
        "RUINS" -> Color(0xFF37474F)
        else -> Color(0xFF2E7D32)
    }

    // Paint solid ground tile
    drawRect(baseColor, topLeft = Offset(tileX, tileY), size = Size(40f, 40f))

    // Subtle aesthetic biome indicators inside square tiles
    when (biome) {
        "LAVA" -> {
            // Molten magma vein cracks
            if (random.nextFloat() < 0.25f) {
                drawLine(
                    color = Color(0xFFFF3D00).copy(alpha = 0.8f),
                    start = Offset(tileX, tileY),
                    end = Offset(tileX + 40f, tileY + 40f),
                    strokeWidth = 2.5f
                )
            }
        }
        "SNOW" -> {
            // Shiny frosted crystal outline
            if (random.nextFloat() < 0.2f) {
                drawRect(
                    color = Color(0xFF80DEEA).copy(alpha = 0.4f),
                    topLeft = Offset(tileX + 5f, tileY + 5f),
                    size = Size(30f, 30f),
                    style = Stroke(width = 1.5f)
                )
            }
        }
        "SWAMP" -> {
            // Slime bubbles circles
            if (random.nextFloat() < 0.15f) {
                drawCircle(
                    color = Color(0xFF7CB342).copy(alpha = 0.4f),
                    radius = random.nextFloat() * 6f + 2f,
                    center = Offset(tileX + 20f, tileY + 20f)
                )
            }
        }
        "CAVE" -> {
            // Shiny crystal deposits
            if (random.nextFloat() < 0.15f) {
                drawCircle(Color(0xFFBA68C8).copy(alpha = 0.6f), radius = 3.5f, center = Offset(tileX + 20f, tileY + 20f))
            }
        }
        "SKY_ISLANDS" -> {
            // Fluffy white drift clouds
            if (random.nextFloat() < 0.2f) {
                drawCircle(Color.White.copy(alpha = 0.25f), radius = 8f, center = Offset(tileX + 20f, tileY + 20f))
            }
        }
        "CRYSTAL_LANDS" -> {
            if (random.nextFloat() < 0.22f) {
                drawCircle(Color(0xFF00FF7F).copy(alpha = 0.5f), radius = 4f, center = Offset(tileX + 20f, tileY + 20f))
            }
        }
        "DAK_WASTELAND", "DARK_WASTELAND" -> {
            // Ashes spark pixels
            if (random.nextFloat() < 0.25f) {
                drawRect(Color(0xFFFF3D00).copy(alpha = 0.6f), topLeft = Offset(tileX + 18f, tileY + 18f), size = Size(4f, 4f))
            }
        }
        "OCEAN" -> {
            // Ripple wave indicators
            if (random.nextFloat() < 0.2f) {
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                    start = Offset(tileX + 8f, tileY + 20f),
                    end = Offset(tileX + 32f, tileY + 20f),
                    strokeWidth = 2f
                )
            }
        }
        "RUINS" -> {
            // Old crumbled brick line separators
            drawLine(Color.Black.copy(alpha = 0.15f), start = Offset(tileX, tileY + 38f), end = Offset(tileX + 40f, tileY + 38f), strokeWidth = 1.5f)
            drawLine(Color.Black.copy(alpha = 0.15f), start = Offset(tileX + 38f, tileY), end = Offset(tileX + 38f, tileY + 40f), strokeWidth = 1.5f)
        }
        else -> {
            // Green grass petals
            if (random.nextFloat() < 0.25f) {
                drawLine(Color(0xFF81C784).copy(alpha = 0.4f), Offset(tileX + 10f, tileY + 25f), Offset(tileX + 15f, tileY + 15f), strokeWidth = 2f)
            }
        }
    }

    // Subtle edge border of tiles
    drawRect(Color.Black.copy(alpha = 0.05f), topLeft = Offset(tileX, tileY), size = Size(40f, 40f), style = Stroke(width = 1f))
}

private fun DrawScope.drawActiveWeatherOverlays(weather: String, ticksProgress: Float) {
    val sizeW = size.width
    val sizeH = size.height
    if (sizeW <= 0f || sizeH <= 0f) return

    when (weather) {
        "RAINY" -> {
            val dripColor = Color(0xFF00E5FF).copy(alpha = 0.35f)
            for (i in 0..25) {
                val seed = i * 401L
                val random = Random(seed)
                val rx = (random.nextFloat() * sizeW + ticksProgress * 10f) % sizeW
                val ry = (random.nextFloat() * sizeH + ticksProgress * 18f) % sizeH
                drawLine(dripColor, Offset(rx, ry), Offset(rx - 6f, ry + 22f), strokeWidth = 2.5f)
            }
        }
        "SNOWY", "BLIZZARD" -> {
            val flakeColor = Color.White.copy(alpha = 0.8f)
            for (i in 0..30) {
                val seed = i * 701L
                val random = Random(seed)
                val rx = (random.nextFloat() * sizeW + sin(ticksProgress * 0.03f) * 45f) % sizeW
                val ry = (random.nextFloat() * sizeH + ticksProgress * 6f) % sizeH
                drawCircle(flakeColor, radius = random.nextFloat() * 3.5f + 1.5f, center = Offset(rx, ry))
            }
        }
        "SANDSTORM" -> {
            val sandColor = Color(0xFFFFD54F).copy(alpha = 0.25f)
            for (i in 0..20) {
                val seed = i * 301L
                val random = Random(seed)
                val rx = (random.nextFloat() * sizeW + ticksProgress * 22f) % sizeW
                val ry = (random.nextFloat() * sizeH + sin(ticksProgress * 0.01f) * 15f) % sizeH
                drawLine(sandColor, Offset(rx, ry), Offset(rx + 50f, ry), strokeWidth = 3f)
            }
        }
        "MISTY" -> {
            val mistColor = Color.White.copy(alpha = 0.12f)
            for (i in 0..6) {
                val seed = i * 905L
                val random = Random(seed)
                val rx = (random.nextFloat() * sizeW + ticksProgress * 1.5f) % sizeW
                val ry = random.nextFloat() * sizeH
                drawCircle(mistColor, radius = 90f + random.nextFloat() * 60f, center = Offset(rx, ry))
            }
        }
        "SUNNY" -> {
            // Mild heatwaves sun flares
            val radial = Brush.radialGradient(
                colors = listOf(Color(0xFFFFEB3B).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(sizeW, 0f),
                radius = 320f
            )
            drawCircle(radial, radius = 320f, center = Offset(sizeW, 0f))
        }
    }
}

@Composable
fun BiomeTravelDialog(
    unlockedList: List<String>,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit
) {
    val allBiomes = listOf("FOREST", "LAVA", "SNOW", "SWAMP", "CAVE", "SKY_ISLANDS", "CRYSTAL_LANDS", "DARK_WASTELAND", "OCEAN", "RUINS")

    AlertDialog(
        onDismissRequest = { onCancel() },
        title = {
            Text(
                "BIOME PORTAL TRAVEL",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 350.dp)
            ) {
                // Wrap in scroll container for high resolution compatibility
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allBiomes.size) { index ->
                            val biome = allBiomes[index]
                            val isLocked = !unlockedList.contains(biome) && biome != "FOREST"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLocked) Color.Black.copy(alpha = 0.4f) else Color(0xFF3D256D))
                                    .clickable(enabled = !isLocked) {
                                        SoundEffectsManager.playSelected()
                                        onSelect(biome)
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LocationOn,
                                    contentDescription = "status",
                                    tint = if (isLocked) Color.Gray else Color(0xFFFFD700)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isLocked) "$biome [LOCKED]" else "$biome SECTOR",
                                    color = if (isLocked) Color.Gray else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCancel() }) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color(0xFF23143E),
        shape = RoundedCornerShape(20.dp)
    )
}

// Retro D-Pad drawing
@Composable
fun VirtualRetroDPad(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .background(Color(0xFF241445).copy(alpha = 0.8f), CircleShape)
            .padding(4.dp)
    ) {
        val speed = 20f

        // North
        IconButton(
            onClick = { onMove(0f, -speed) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(38.dp)
                .background(Color(0xFF512DA8), CircleShape)
                .testTag("dpad_up")
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "N", tint = Color.White)
        }

        // South
        IconButton(
            onClick = { onMove(0f, speed) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(38.dp)
                .background(Color(0xFF512DA8), CircleShape)
                .testTag("dpad_down")
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "S", tint = Color.White)
        }

        // West
        IconButton(
            onClick = { onMove(-speed, 0f) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(38.dp)
                .background(Color(0xFF512DA8), CircleShape)
                .testTag("dpad_left")
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "W", tint = Color.White)
        }

        // East
        IconButton(
            onClick = { onMove(speed, 0f) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(38.dp)
                .background(Color(0xFF512DA8), CircleShape)
                .testTag("dpad_right")
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "E", tint = Color.White)
        }

        // Inner core circle block
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFFFFD700), CircleShape)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun MapShopOverlay(
    viewModel: GameViewModel,
    onClose: () -> Unit
) {
    val prog by viewModel.playerProgress.collectAsState()
    val inv by viewModel.inventory.collectAsState()

    AlertDialog(
        onDismissRequest = { onClose() },
        title = {
            Text(
                "BIOME TRADING MERCHANT",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Spend your collected gold onto essential items for wild taming activities. All offline, zero microtransactions.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                // Item 1: Orbs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFA5114E).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "O", tint = Color(0xFFFF2E63))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mythic Capture Orb", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Owned: ${inv["orb"] ?: 0}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.buyShopItem("orb", 150) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E63)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("150 G", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Item 2: Potion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF388E3C).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "P", tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Health Restoration Potion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Owned: ${inv["potion"] ?: 0}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.buyShopItem("potion", 200) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("200 G", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onClose() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF512DA8))
            ) {
                Text("Close Shop", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF2C1E4E),
        shape = RoundedCornerShape(24.dp)
    )
}
