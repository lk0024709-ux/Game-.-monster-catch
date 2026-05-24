package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.game.CreatureDatabase
import com.example.game.MythicType
import com.example.game.SoundEffectsManager
import com.example.ui.components.CreatureRenderer
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Structure to persist tap-driven particles for VFX live testing
data class VisualParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val maxAge: Int,
    var age: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetForgeView(
    viewModel: GameViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Creature Codex, 1: Magic VFX, 2: Map Tilesets, 3: UI & Loot
    val tabs = listOf("Monster-Dex", "VFX Sandbox", "Map Tilesets", "UI & Loot")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0824), Color(0xFF1B0F3F), Color(0xFF070412))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Dashboard Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎨 MYTHICMONS GRAPHIC ASSET FORGE",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Procedural Vector Render Engine v1.1 • AAA RPG Assets",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                IconButton(
                    onClick = {
                        SoundEffectsManager.playSelected()
                        onClose()
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Tab bar switcher
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Black.copy(alpha = 0.25f),
                contentColor = Color(0xFFFFD700),
                edgePadding = 12.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFFFFE082)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = {
                            SoundEffectsManager.playSelected()
                            activeTab = index
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> CreatureCodexSubPane()
                    1 -> MagicVfxSubPane()
                    2 -> MapTilesetSubPane()
                    3 -> UiAndLootSubPane()
                }
            }
        }
    }
}

@Composable
fun CreatureCodexSubPane() {
    val speciesList = CreatureDatabase.species
    var selectedId by remember { mutableStateOf("pyrofox") }
    val currentSpec = CreatureDatabase.getSpecies(selectedId) ?: speciesList[0]

    var isShiny by remember { mutableStateOf(false) }
    var levelSlider by remember { mutableFloatStateOf(10f) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column list list of species
        Column(
            modifier = Modifier
                .width(135.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.15f))
        ) {
            Text(
                text = "SPECIES (20+)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(12.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(speciesList.size) { index ->
                    val spec = speciesList[index]
                    val isSelected = spec.id == selectedId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                SoundEffectsManager.playSelected()
                                selectedId = spec.id
                            }
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(listOf(Color(0xFF311B92), Color(0xFF1E105C)))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = spec.name,
                                color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = spec.type,
                                fontSize = 9.sp,
                                color = getThemeColorForType(spec.type),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.08f))
        )

        // Right Content pane presenting the chosen Creature with options
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Live render container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Core visual display
                    CreatureRenderer(
                        speciesId = currentSpec.id,
                        isShiny = isShiny,
                        scale = 1.5f,
                        modifier = Modifier.fillMaxSize(0.85f)
                    )

                    // Floating Type badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(
                                getThemeColorForType(currentSpec.type).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, getThemeColorForType(currentSpec.type).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentSpec.type,
                            color = getThemeColorForType(currentSpec.type),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Floating Shiny label
                    if (isShiny) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "★ SHINY EDITION",
                                color = Color(0xFFFFD700),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shiny Toggle button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isShiny = !isShiny
                            SoundEffectsManager.playSelected()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isShiny) Color(0xFFFFD700).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
                    ),
                    border = BorderStroke(1.dp, if (isShiny) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Shiny",
                            tint = if (isShiny) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shiny Spark",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Random Evolution simulated button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            SoundEffectsManager.playLevelUp()
                            if (currentSpec.evolvesTo != null) {
                                selectedId = currentSpec.evolvesTo
                            } else {
                                SoundEffectsManager.playCaptureFail()
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Evolve",
                            tint = if (currentSpec.evolvesTo != null) Color(0xFF00FF7F) else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentSpec.evolvesTo != null) "Trigger Evolution" else "Fully Evolved",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Stats section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CONCEPT DESIGN METADATA",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = currentSpec.description,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    StatBar(name = "HP Max Value", value = currentSpec.baseHp, color = Color(0xFF4CAF50), max = 150)
                    StatBar(name = "Physical Attack", value = currentSpec.baseAttack, color = Color(0xFFFF5252), max = 50)
                    StatBar(name = "Defense Armor", value = currentSpec.baseDefense, color = Color(0xFF2196F3), max = 50)
                    StatBar(name = "Speed Initiative", value = currentSpec.baseSpeed, color = Color(0xFFFFEB3B), max = 50)
                }
            }
        }
    }
}

@Composable
fun StatBar(name: String, value: Int, color: Color, max: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(text = value.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value.toFloat() / max.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color.White.copy(alpha = 0.08f)
        )
    }
}

@Composable
fun MagicVfxSubPane() {
    var activeVfxType by remember { mutableStateOf(0) } // 0: Fire, 1: Electric, 2: Water Splash, 3: Dark Vortex, 4: Summon Circle
    val types = listOf("Fire Burst", "Lightning Storm", "Aqua Splash", "Shadow Void", "Summon Shield")

    val scope = rememberCoroutineScope()
    val particles = remember { mutableStateListOf<VisualParticle>() }
    var backgroundFlash by remember { mutableStateOf(false) }

    // Constant particle integration loop update (thread-safe transaction base)
    LaunchedEffect(key1 = activeVfxType) {
        val frameDuration = 33L // approx 30 FPS
        while (true) {
            delay(frameDuration)
            if (particles.isNotEmpty()) {
                // Create a temporary mapped & filtered update list to avoid concurrent mutations during rendering
                val updatedList = particles.map { p ->
                    p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        vy = if (activeVfxType == 0) p.vy - 0.15f else p.vy, // Gravity rising for fire
                        age = p.age + 1
                    )
                }.filter { it.age < it.maxAge }

                // Repopulate in a single Atomic operation to keep UI reactive & stable
                particles.clear()
                particles.addAll(updatedList)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Switchers row
        ScrollableTabRow(
            selectedTabIndex = activeVfxType,
            containerColor = Color.White.copy(alpha = 0.03f),
            contentColor = Color(0xFFFFD700),
            edgePadding = 0.dp,
            divider = {}
        ) {
            types.forEachIndexed { index, name ->
                Tab(
                    selected = activeVfxType == index,
                    onClick = {
                        SoundEffectsManager.playSelected()
                        activeVfxType = index
                        particles.clear()
                    },
                    text = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Live interactive Canvas test-tube screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = if (backgroundFlash) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.6f)),
            border = BorderStroke(2.dp, getThemeColorForVfx(activeVfxType).copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(key1 = activeVfxType) {
                        detectTapGestures(
                            onTap = { offset ->
                                SoundEffectsManager.playHit()
                                scope.launch {
                                    if (activeVfxType == 1) {
                                        backgroundFlash = true
                                        delay(80)
                                        backgroundFlash = false
                                    }
                                }

                                when (activeVfxType) {
                                    0 -> { // Fire burst
                                        repeat(25) {
                                            particles.add(
                                                VisualParticle(
                                                    x = offset.x,
                                                    y = offset.y,
                                                    vx = Random.nextFloat() * 10f - 5f,
                                                    vy = Random.nextFloat() * 10f - 5f,
                                                    size = Random.nextFloat() * 20f + 10f,
                                                    color = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFEB3B)).random(),
                                                    maxAge = (15..30).random()
                                                )
                                            )
                                        }
                                    }

                                    1 -> { // Lightning Spark Strike
                                        repeat(12) {
                                            particles.add(
                                                VisualParticle(
                                                    x = offset.x,
                                                    y = offset.y,
                                                    vx = Random.nextFloat() * 16f - 8f,
                                                    vy = Random.nextFloat() * 16f - 8f,
                                                    size = Random.nextFloat() * 15f + 5f,
                                                    color = Color(0xFFFFFF00),
                                                    maxAge = (10..20).random()
                                                )
                                            )
                                        }
                                        // Spawn a lightning branch vertical
                                        repeat(15) { idx ->
                                            particles.add(
                                                VisualParticle(
                                                    x = offset.x + (Random.nextFloat() * 10f - 5f),
                                                    y = offset.y - (idx * 28f),
                                                    vx = 0f,
                                                    vy = 0f,
                                                    size = 12f,
                                                    color = Color.White,
                                                    maxAge = 12
                                                )
                                            )
                                        }
                                    }

                                    2 -> { // Water splash concentric rings
                                        repeat(3) { r ->
                                            particles.add(
                                                VisualParticle(
                                                    x = offset.x,
                                                    y = offset.y,
                                                    vx = 0f,
                                                    vy = 0f,
                                                    size = (r + 1) * 35f,
                                                    color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                                                    maxAge = 18
                                                )
                                            )
                                        }
                                        repeat(15) {
                                            particles.add(
                                                VisualParticle(
                                                    x = offset.x,
                                                    y = offset.y,
                                                    vx = Random.nextFloat() * 6f - 3f,
                                                    vy = Random.nextFloat() * 4f - 8f, // float up
                                                    size = Random.nextFloat() * 12f + 4f,
                                                    color = Color(0xFFE0F7FA),
                                                    maxAge = (20..40).random()
                                                )
                                            )
                                        }
                                    }

                                    3 -> { // Dark Void Vortex
                                        repeat(30) {
                                            val radDist = Random.nextFloat() * 90f + 30f
                                            val ang = Random.nextFloat() * (Math.PI * 2)
                                            val px = offset.x + (radDist * cos(ang)).toFloat()
                                            val py = offset.y + (radDist * sin(ang)).toFloat()

                                            particles.add(
                                                VisualParticle(
                                                    x = px,
                                                    y = py,
                                                    vx = (-cos(ang) * 3f).toFloat(), // pull into center
                                                    vy = (-sin(ang) * 3f).toFloat(),
                                                    size = Random.nextFloat() * 10f + 6f,
                                                    color = listOf(Color(0xFF7E57C2), Color(0xFF4A148C), Color(0xFFE040FB)).random(),
                                                    maxAge = 25
                                                )
                                            )
                                        }
                                    }

                                    4 -> { // Summon Circle concentric rotating runic
                                        particles.add(
                                            VisualParticle(
                                                x = offset.x,
                                                y = offset.y,
                                                vx = 0f,
                                                vy = 0f,
                                                size = 110f,
                                                color = Color(0xFF00E676),
                                                maxAge = 35
                                            )
                                        )
                                        repeat(36) { step ->
                                            val ang = Math.toRadians((step * 10).toDouble())
                                            val rx = offset.x + (65f * cos(ang)).toFloat()
                                            val ry = offset.y + (65f * sin(ang)).toFloat()
                                            particles.add(
                                                VisualParticle(
                                                    x = rx,
                                                    y = ry,
                                                    vx = 0f,
                                                    vy = 0f,
                                                    size = 6f,
                                                    color = Color(0xFFFFF176),
                                                    maxAge = 30
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Central watermark
                    if (particles.isEmpty()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.03f),
                            radius = size.width / 4,
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }

                    // Render alive particle lists
                    particles.forEach { p ->
                        when (activeVfxType) {
                            2 -> { // Concentric water rings
                                if (p.vx == 0f) {
                                    val growthFactor = p.age.toFloat() / p.maxAge.toFloat()
                                    drawCircle(
                                        color = p.color.copy(alpha = 1f - growthFactor),
                                        radius = p.size * growthFactor,
                                        center = Offset(p.x, p.y),
                                        style = Stroke(width = 4f)
                                    )
                                } else {
                                    drawCircle(
                                        color = p.color.copy(alpha = 0.45f),
                                        radius = p.size / 2,
                                        center = Offset(p.x, p.y)
                                    )
                                }
                            }

                            4 -> { // Summon runes
                                if (p.size > 100f) {
                                    val factor = p.age.toFloat() / p.maxAge.toFloat()
                                    drawCircle(
                                        color = p.color.copy(alpha = 0.5f * (1f - factor)),
                                        radius = p.size,
                                        center = Offset(p.x, p.y),
                                        style = Stroke(width = 6f)
                                    )
                                } else {
                                    drawCircle(
                                        color = p.color,
                                        radius = p.size / 2,
                                        center = Offset(p.x, p.y)
                                    )
                                }
                            }

                            else -> { // Default particles (spark/ember)
                                val alphaFactor = 1f - (p.age.toFloat() / p.maxAge.toFloat())
                                drawCircle(
                                    color = p.color.copy(alpha = alphaFactor),
                                    radius = (p.size * alphaFactor).coerceAtLeast(1f),
                                    center = Offset(p.x, p.y)
                                )
                            }
                        }
                    }
                }

                // Instructors overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "👉 TAP ANYWHERE INSIDE BOX TO DEPLOY COMBAT SKILL SPELL 👈",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MapTilesetSubPane() {
    var selectedTileset by remember { mutableStateOf(0) } // 0: Grass, 1: Lava, 2: Tundra Ice, 3: Desert Sand, 4: Stone Ruins
    val tilesets = listOf("Meadow Grass", "Volcanic Lava", "Frozen Ice", "Desert Dune", "Stone Ruins")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTileset,
            containerColor = Color.White.copy(alpha = 0.03f),
            divider = {}
        ) {
            tilesets.forEachIndexed { index, name ->
                Tab(
                    selected = selectedTileset == index,
                    onClick = {
                        SoundEffectsManager.playSelected()
                        selectedTileset = index
                    },
                    text = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Live tileset grid mockup representation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive grid canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tileColumns = 6
                        val tileRows = 5
                        val tileW = size.width / tileColumns
                        val tileH = size.height / tileRows

                        val brushScheme = when (selectedTileset) {
                            0 -> Brush.linearGradient(listOf(Color(0xFF388E3C), Color(0xFF2E7D32))) // Meadow Grass
                            1 -> Brush.linearGradient(listOf(Color(0xFFFF3D00).copy(alpha = 0.85f), Color(0xFFE64A19))) // Volcanic Lava
                            2 -> Brush.linearGradient(listOf(Color(0xFF80DEEA), Color(0xFF4DD0E1))) // Frozen Ice
                            3 -> Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFCA28))) // Desert Sand
                            else -> Brush.linearGradient(listOf(Color(0xFF5D4037), Color(0xFF4E342E))) // Stone Ruins
                        }

                        // Drawing seamless tiles grid
                        for (c in 0 until tileColumns) {
                            for (r in 0 until tileRows) {
                                val tx = c * tileW
                                val ty = r * tileH

                                drawRect(
                                    brush = brushScheme,
                                    topLeft = Offset(tx + 2f, ty + 2f),
                                    size = Size(tileW - 4f, tileH - 4f)
                                )

                                // Procedural map textures details (glowing cracks, snow particles)
                                when (selectedTileset) {
                                    0 -> { // grass grass tufts
                                        drawLine(Color(0xFF81C784), Offset(tx + tileW/2, ty + tileH/2), Offset(tx + tileW/2 - 5f, ty + tileH/2 - 12f), strokeWidth = 3f)
                                        drawLine(Color(0xFF81C784), Offset(tx + tileW/2, ty + tileH/2), Offset(tx + tileW/2 + 5f, ty + tileH/2 - 12f), strokeWidth = 3f)
                                    }
                                    1 -> { // lava ripples
                                        drawCircle(Color(0xFFFFEB3B), radius = 6f, center = Offset(tx + tileW/3 + (r*3 % 8), ty + tileH/2 + (c*2 % 12)))
                                    }
                                    2 -> { // Ice diamond sparkle
                                        drawCircle(Color.White.copy(alpha = 0.4f), radius = 4f, center = Offset(tx + tileW/2, ty + tileH/2))
                                    }
                                    3 -> { // Sand wind waves
                                        drawLine(Color(0xFFFFB300), Offset(tx + 10f, ty + tileH/2), Offset(tx + tileW - 10f, ty + tileH/2 + 5f), strokeWidth = 2f)
                                    }
                                    4 -> { // ruins brick boundaries
                                        drawLine(Color(0xFF3E2723), Offset(tx, ty), Offset(tx + tileW, ty), strokeWidth = 3f)
                                        drawLine(Color(0xFF3E2723), Offset(tx, ty), Offset(tx, ty + tileH), strokeWidth = 3f)
                                    }
                                }
                            }
                        }

                        // Overlay environment props on top (magical portal, epic chest, trees)
                        drawMapPropsOverlay(selectedTileset, size.width, size.height)
                    }
                }

                // Grid stats layout details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tile Unit: 64x64px • Seamless Atlases ready",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Format: PNG-24",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// Procedurally draws beautiful game environment obstacles onto tilesets
fun DrawScope.drawMapPropsOverlay(tilesetIndex: Int, w: Float, h: Float) {
    when (tilesetIndex) {
        0 -> { // Meadow Green: Magical Ancient Portal & glowing tree
            // High contrast portal gate
            val cx = w * 0.75f
            val cy = h * 0.45f
            drawCircle(Color(0xFF1E105C), radius = 35f, center = Offset(cx, cy))
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.7f), radius = 35f, center = Offset(cx, cy), style = Stroke(width = 6f))

            // Magical Tree trunk & leaves
            val tx = w * 0.25f
            val ty = h * 0.5f
            drawRect(Color(0xFF5D4037), topLeft = Offset(tx - 6f, ty), size = Size(12f, 35f))
            drawCircle(Color(0xFF4CAF50), radius = 30f, center = Offset(tx, ty - 10f))
            drawCircle(Color(0xFF81C784), radius = 18f, center = Offset(tx + 12f, ty - 15f))
        }

        1 -> { // Lava Volcanic Rocks
            val rx = w * 0.5f
            val ry = h * 0.5f
            val rPath = Path().apply {
                moveTo(rx, ry - 30f)
                lineTo(rx + 35f, ry + 25f)
                lineTo(rx - 30f, ry + 20f)
                close()
            }
            drawPath(rPath, Color(0xFF212121))
            drawPath(rPath, Color(0xFFFF5722), style = Stroke(width = 3f))
        }

        2 -> { // Ice Crystals
            val cx = w * 0.5f
            val cy = h * 0.5f
            val xPath = Path().apply {
                moveTo(cx, cy - 40f)
                lineTo(cx + 18f, cy + 10f)
                lineTo(cx, cy + 30f)
                lineTo(cx - 18f, cy + 10f)
                close()
            }
            drawPath(xPath, Color(0xFFE0F7FA))
            drawPath(xPath, Color(0xFF00E5FF), style = Stroke(width = 4f))
        }

        3 -> { // Desert Sand Ruins Obelisk
            val ox = w * 0.35f
            val oy = h * 0.4f
            drawRect(Color(0xFFD7CCC8), topLeft = Offset(ox, oy), size = Size(20f, 65f))
            val tip = Path().apply {
                moveTo(ox, oy)
                lineTo(ox + 10f, oy - 15f)
                lineTo(ox + 20f, oy)
                close()
            }
            drawPath(tip, Color(0xFFD7CCC8))
            drawCircle(Color(0xFF00FF7F), radius = 5f, center = Offset(ox + 10f, oy + 25f)) // Glowing obelisk core
        }

        4 -> { // Stone Ruins Legendary Chest Loot
            val cx = w * 0.5f
            val cy = h * 0.5f
            // Outer golden shine
            drawCircle(Color(0xFFFFD700).copy(alpha = 0.25f), radius = 38f, center = Offset(cx, cy))
            // Chest body
            drawRect(Color(0xFF3E2723), topLeft = Offset(cx - 24f, cy - 8f), size = Size(48f, 26f))
            drawArc(
                color = Color(0xFFEF6C00),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 24f, cy - 25f),
                size = Size(48f, 32f)
            )
            // Gold metal trimmings
            drawRect(Color(0xFFFFD700), topLeft = Offset(cx - 24f, cy - 8f), size = Size(6f, 26f))
            drawRect(Color(0xFFFFD700), topLeft = Offset(cx + 18f, cy - 8f), size = Size(6f, 26f))
            drawCircle(Color(0xFFFFD700), radius = 5f, center = Offset(cx, cy))
        }
    }
}

@Composable
fun UiAndLootSubPane() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Elemental types list row
        Text(
            text = "ELEMENTAL BADGE GRAPHICS",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val elementTypes = MythicType.values()
            elementTypes.forEach { myth ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = myth.color.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, myth.color.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(myth.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = myth.displayName.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Collectible items row
        Text(
            text = "COLLECTIBLE ITEM VECTOR ASSETS",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Health potion asset box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drawer canvas representing potion
                    Canvas(modifier = Modifier.size(60.dp)) {
                        // Bottle Neck
                        drawRect(Color(0xFFE0F7FA), topLeft = Offset(24f, 5f), size = Size(12f, 15f))
                        // Cork
                        drawRect(Color(0xFF8D6E63), topLeft = Offset(22f, 0f), size = Size(16f, 6f))
                        // Bottle Body
                        drawCircle(Color(0xFFFF1744), radius = 18f, center = Offset(30f, 38f))
                        drawCircle(Color(0xFFE0F7FA).copy(alpha = 0.4f), radius = 20f, center = Offset(30f, 38f), style = Stroke(width = 2f))
                        // Glowing core highlight
                        drawCircle(Color.White, radius = 5f, center = Offset(24f, 28f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Health Potion", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("HP Restorer", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                }
            }

            // Taming Orb asset box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Canvas(modifier = Modifier.size(60.dp)) {
                        // Outer neon aura
                        drawCircle(Color(0xFF00FF7F).copy(alpha = 0.15f), radius = 26f, center = Offset(30f, 30f))
                        // Orb body representation
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0xFF00FF7F), Color(0xFF006000)), center = Offset(24f, 24f)),
                            radius = 20f,
                            center = Offset(30f, 30f)
                        )
                        // Metal safety band line
                        drawLine(Color(0xFFD7CCC8), Offset(10f, 30f), Offset(50f, 30f), strokeWidth = 3f)
                        drawCircle(Color.White, radius = 4f, center = Offset(30f, 30f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Capturing Orb", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Tame Beasts", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                }
            }

            // Legendary Sword Weapon
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Canvas(modifier = Modifier.size(60.dp)) {
                        // Rotate to show epic angle diagonal
                        withTransform({
                            rotate(45f, pivot = Offset(30f, 30f))
                        }) {
                            // Blade body
                            drawRect(Color(0xFFECEFF1), topLeft = Offset(26f, 0f), size = Size(8f, 40f))
                            // Glowing line core
                            drawLine(Color(0xFF33B5E5), Offset(30f, 0f), Offset(30f, 40f), strokeWidth = 1.5f)
                            // Hilt crossbar guard
                            drawRect(Color(0xFFFFD700), topLeft = Offset(18f, 40f), size = Size(24f, 4f))
                            // Handle
                            drawRect(Color(0xFF5D4037), topLeft = Offset(27f, 44f), size = Size(6f, 15f))
                            // Core diamond pommel
                            drawCircle(Color(0xFFFF3030), radius = 3.5f, center = Offset(30f, 57f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mythic Sword", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("RAID Slasher", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                }
            }
        }

        // Custom Collectible Card Framer HUD
        Text(
            text = "COLLECTIBLE CREATURE CARD INTERFACE",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Card visual display box
                Box(
                    modifier = Modifier
                        .size(100.dp, 130.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A237E), Color(0xFF0D47A1))
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title header Rarity badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("★", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("LEGENDARY", color = Color(0xFFFFD700), fontSize = 6.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        // Creature renderer inside
                        Box(modifier = Modifier.size(55.dp)) {
                            CreatureRenderer(speciesId = "solargryph")
                        }

                        // Footer statistics summary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "ATK: 35 | SPD: 30",
                                color = Color.White,
                                fontSize = 7.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // Metadata description detailing components layout HUD
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PREMIUM CARD LAYOUT GRID",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Crafted with dynamic gold-foil frame boundaries, real-time responsive scaling, custom status bars overlays, and elemental attribute integration tags.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

// Helper methods to return distinct colors for visual categories
fun getThemeColorForType(type: String): Color {
    return try {
        MythicType.valueOf(type).color
    } catch (e: Exception) {
        Color(0xFF78909C)
    }
}

fun getThemeColorForVfx(tabIndex: Int): Color {
    return when (tabIndex) {
        0 -> Color(0xFFFF3D00) // Fire
        1 -> Color(0xFFFFFF00) // Electric
        2 -> Color(0xFF00E5FF) // Water
        3 -> Color(0xFF7E57C2) // Dark
        else -> Color(0xFF00E676) // Nature
    }
}
