package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.model.CreatureEntity
import com.example.game.ProceduralSystems.serialize
import java.util.Random

// Game elements that control advantages and weaknesses
enum class MythicType(val displayName: String, val color: Color, val resistTypes: List<String>, val weakTypes: List<String>) {
    FIRE("Fire", Color(0xFFFF5722), listOf("NATURE", "ICE", "METAL"), listOf("WATER", "EARTH", "DRAGON")),
    WATER("Water", Color(0xFF00E5FF), listOf("FIRE", "EARTH", "METAL"), listOf("NATURE", "ELECTRIC")),
    NATURE("Nature", Color(0xFF4CAF50), listOf("WATER", "EARTH", "LIGHT"), listOf("FIRE", "ICE")),
    EARTH("Earth", Color(0xFF795548), listOf("FIRE", "ELECTRIC", "METAL"), listOf("NATURE", "WATER")),
    ELECTRIC("Electric", Color(0xFFFFEB3B), listOf("WATER", "WIND"), listOf("EARTH")),
    ICE("Ice", Color(0xFF00BCD4), listOf("NATURE", "WIND"), listOf("FIRE", "METAL")),
    WIND("Wind", Color(0xFF90A4AE), listOf("EARTH", "ELECTRIC"), listOf("ICE")),
    METAL("Metal", Color(0xFF78909C), listOf("NATURE", "EARTH", "WIND"), listOf("FIRE", "ELECTRIC")),
    LIGHT("Light", Color(0xFFFFF176), listOf("DARK", "DRAGON"), listOf("DARK")),
    DARK("Dark", Color(0xFF7E57C2), listOf("LIGHT"), listOf("LIGHT", "DRAGON")),
    DRAGON("Dragon", Color(0xFFFF1744), listOf("FIRE", "WATER", "NATURE", "ELECTRIC"), listOf("ICE", "DRAGON")),
    MYSTIC("Mystic", Color(0xFFE040FB), listOf("LIGHT", "DARK"), listOf("METAL")),
    TOXIC("Toxic", Color(0xFF9C27B0), listOf("NATURE", "WATER"), listOf("FIRE", "ICE")),
    CRYSTAL("Crystal", Color(0xFFE91E63), listOf("ELECTRIC", "WIND"), listOf("METAL", "EARTH")),
    MAGMA("Magma", Color(0xFFFF3D00), listOf("ICE", "NATURE"), listOf("WATER", "EARTH"))
}

data class MoveTemplate(
    val id: String,
    val name: String,
    val type: String,
    val power: Int,
    val energyCost: Int = 10,
    val accuracy: Float = 0.95f,
    val statusEffect: String? = null,
    val effectChance: Float = 0f,
    val description: String = ""
)

data class SpeciesTemplate(
    val id: String,
    val name: String,
    val type: String,
    val description: String,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Int,
    val evolutionLevel: Int = -1,
    val evolvesTo: String? = null,
    val rarity: String = "COMMON",
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val bodyType: String = "ROUND", // ROUND, QUADRUPED, BIRD, REPTILE, ELEMENTAL
    val levelMoves: List<Pair<Int, String>> // Level at which it learns specific move ids
)

object CreatureDatabase {
    val moves = mapOf(
        "tackle" to MoveTemplate("tackle", "Tackle", "EARTH", 35, 5, 0.98f, description = "A simple, physical body charge."),
        "scratch" to MoveTemplate("scratch", "Scratch", "WIND", 40, 5, 0.95f, description = "Sharp claw slash."),
        "embers" to MoveTemplate("embers", "Embers", "FIRE", 50, 10, 0.92f, "BURN", 0.2f, "Spits tiny sparks. May burn."),
        "fire_blast" to MoveTemplate("fire_blast", "Inferno Blast", "FIRE", 85, 20, 0.85f, "BURN", 0.35f, "Massive flame burst. Powerful but costly."),
        "water_spit" to MoveTemplate("water_spit", "Water Spit", "WATER", 48, 10, 0.95f, description = "Shoots high pressure bubble streams."),
        "hydro_pump" to MoveTemplate("hydro_pump", "Hydro Torrent", "WATER", 90, 25, 0.80f, description = "Summons a raging tidal wall."),
        "leaf_cutter" to MoveTemplate("leaf_cutter", "Leaf Cutter", "NATURE", 52, 12, 0.95f, description = "Launches razor sharp leaves."),
        "solar_beam" to MoveTemplate("solar_beam", "Solar Core Beam", "NATURE", 95, 30, 0.85f, "HEAL_SELF", 0.5f, "Recharges light to blast. Recovers user health!"),
        "peck" to MoveTemplate("peck", "Wind Peck", "WIND", 42, 8, 0.95f, description = "Strikes fast with beak."),
        "sand_slap" to MoveTemplate("sand_slap", "Sand Slap", "EARTH", 45, 10, 0.90f, "STUN", 0.15f, "Throws blinding sands. May stun the target."),
        "earthquake" to MoveTemplate("earthquake", "Fissure Rumble", "EARTH", 80, 22, 0.90f, description = "Shakes the ground with violent shockwaves."),
        "spark" to MoveTemplate("spark", "Volt Spark", "ELECTRIC", 45, 10, 0.95f, "STUN", 0.20f, "Discharges lightning. May stun."),
        "thunderbolt" to MoveTemplate("thunderbolt", "Zeus Thunderbolt", "ELECTRIC", 85, 20, 0.90f, "STUN", 0.25f, "Blasts crackling lightning bolt from sky."),
        "frost_breath" to MoveTemplate("frost_breath", "Frost Breath", "ICE", 45, 10, 0.90f, "FREEZE", 0.15f, "Chilled wind. May freeze enemy."),
        "blizzard" to MoveTemplate("blizzard", "Absolute Zero", "ICE", 85, 22, 0.85f, "FREEZE", 0.30f, "Summons a howling snowstorm."),
        "iron_bash" to MoveTemplate("iron_bash", "Iron Headbutt", "METAL", 55, 12, 0.90f, description = "Hard protective headbutt."),
        "shadow_fang" to MoveTemplate("shadow_fang", "Shadow Bite", "DARK", 50, 10, 0.95f, "LEECH", 0.3f, "Bites with phantom armor. Drains lifeforce."),
        "celestial_ray" to MoveTemplate("celestial_ray", "Nova Ray", "LIGHT", 65, 15, 0.92f, "BURN", 0.15f, "Glares radiant solar laser beams."),
        "mystic_blast" to MoveTemplate("mystic_blast", "Zen Blast", "MYSTIC", 70, 18, 0.95f, description = "Mystic telekinetic strike. Heavy impact."),
        "dragon_claw" to MoveTemplate("dragon_claw", "Draco Slice", "DRAGON", 80, 18, 0.95f, description = "Slits target with fierce dragon claws."),
        "toxic_spit" to MoveTemplate("toxic_spit", "Toxic Spit", "TOXIC", 48, 8, 0.95f, "POISON", 0.25f, "Shoots corrosive toxic venom to poison foes."),
        "crystal_shard" to MoveTemplate("crystal_shard", "Crystal Prism", "CRYSTAL", 60, 12, 0.92f, description = "Summons razor sharp crystalline spikes."),
        "magma_eruption" to MoveTemplate("magma_eruption", "Magma Breach", "MAGMA", 88, 24, 0.85f, "BURN", 0.3f, "Erupts high-temp boiling magma from crust segments.")
    )

    val species = listOf(
        // === Legacy Line definitions for 100% backward safety ===
        SpeciesTemplate("pyrofox", "Pyrofox", "FIRE", "A cozy fox pup with flaming ears.", 50, 15, 9, 12, 14, "pyrofang", "COMMON", Color(0xFFFF5722), Color(0xFFFFEB3B), Color(0xFFFF9800), "QUADRUPED", listOf(1 to "scratch", 4 to "embers", 10 to "tackle")),
        SpeciesTemplate("pyrofang", "Pyrofang", "FIRE", "A sleek hunter commanding thermal embers.", 70, 22, 14, 19, 32, "pyrosaur", "RARE", Color(0xFFE64A19), Color(0xFFFFEB3B), Color(0xFFFF9800), "QUADRUPED", listOf(1 to "scratch", 4 to "embers", 20 to "fire_blast")),
        SpeciesTemplate("pyrosaur", "Pyrosaur Supreme", "FIRE", "Volcanic dragon beast.", 100, 32, 20, 26, -1, null, "EPIC", Color(0xFFD84315), Color(0xFF212121), Color(0xFFFF3D00), "DRAGON", listOf(1 to "embers", 20 to "fire_blast", 35 to "dragon_claw")),
        SpeciesTemplate("pipfin", "Pipfin", "WATER", "Round bird seal squirt.", 58, 11, 13, 9, 14, "aquafin", "COMMON", Color(0xFF00BCD4), Color(0xFFE0F7FA), Color(0xFF2196F3), "BIRD", listOf(1 to "tackle", 4 to "water_spit", 10 to "peck")),
        SpeciesTemplate("aquafin", "Aquafin", "WATER", "Rides massive ocean swells.", 78, 17, 19, 13, 32, "leviapod", "RARE", Color(0xFF0097A7), Color(0xFFE0F7FA), Color(0xFF0D47A1), "REPTILE", listOf(1 to "tackle", 4 to "water_spit", 18 to "frost_breath")),
        SpeciesTemplate("leviapod", "Leviapod Titan", "WATER", "Ancient titan deep-sea serpent.", 115, 24, 29, 17, -1, null, "EPIC", Color(0xFF006064), Color(0xFF001F3F), Color(0xFF00E5FF), "REPTILE", listOf(1 to "water_spit", 18 to "frost_breath", 32 to "hydro_pump")),
        SpeciesTemplate("seedling", "Seedling", "NATURE", "Cute sprout dinosaur with flower petal crown.", 52, 12, 11, 11, 14, "leafant", "COMMON", Color(0xFF4CAF50), Color(0xFFDCEDC8), Color(0xFF8BC34A), "QUADRUPED", listOf(1 to "tackle", 4 to "leaf_cutter", 8 to "scratch")),
        SpeciesTemplate("leafant", "Leafant", "NATURE", "Soothes wounds using sunlight.", 72, 18, 17, 16, 32, "terrasaur", "RARE", Color(0xFF388E3C), Color(0xFFDCEDC8), Color(0xFFFFEB3B), "QUADRUPED", listOf(1 to "tackle", 4 to "leaf_cutter", 22 to "solar_beam")),
        SpeciesTemplate("terrasaur", "Terrasaur Rex", "NATURE", "Moss covered leviathan.", 105, 26, 23, 21, -1, null, "EPIC", Color(0xFF1B5E20), Color(0xFF3E2723), Color(0xFF76FF03), "REPTILE", listOf(1 to "leaf_cutter", 22 to "solar_beam", 35 to "earthquake")),
        SpeciesTemplate("sparky", "Sparky", "ELECTRIC", "Hamster discharging static charges.", 45, 14, 8, 16, 18, "voltex", "COMMON", Color(0xFFFFEB3B), Color(0xFFFFF9C4), Color(0xFFFF5722), "ROUND", listOf(1 to "scratch", 5 to "spark")),
        SpeciesTemplate("voltex", "Voltex Beast", "ELECTRIC", "Sleek thunder beast.", 75, 26, 13, 28, -1, null, "RARE", Color(0xFFFBC02D), Color(0xFF212121), Color(0xFFFFFF00), "QUADRUPED", listOf(1 to "scratch", 5 to "spark", 15 to "thunderbolt")),
        SpeciesTemplate("cubfrost", "Cubfrost", "ICE", "Frosty bear with ice chunk shell.", 62, 11, 14, 7, 20, "glaciard", "COMMON", Color(0xFF00BCD4), Color(0xFFE0F7FA), Color(0xFFFFFFFF), "QUADRUPED", listOf(1 to "tackle", 6 to "frost_breath")),
        SpeciesTemplate("glaciard", "Glaciard Guard", "ICE", "Indestructible crystal diamond golem.", 95, 20, 25, 12, -1, null, "RARE", Color(0xFF00838F), Color(0xFFB2EBF2), Color(0xFF00E5FF), "REPTILE", listOf(1 to "tackle", 16 to "blizzard", 25 to "iron_bash")),
        SpeciesTemplate("boulder", "Boulderin", "EARTH", "Self-repairing runic earth segment.", 65, 13, 18, 6, 22, "goliath", "COMMON", Color(0xFF8D6E63), Color(0xFF4E342E), Color(0xFFFF9800), "ELEMENTAL", listOf(1 to "tackle", 6 to "sand_slap")),
        SpeciesTemplate("goliath", "Goliath Peak", "EARTH", "Quartz giant walking shield.", 100, 24, 28, 10, -1, null, "RARE", Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFFFFD600), "ELEMENTAL", listOf(1 to "tackle", 18 to "earthquake", 26 to "iron_bash")),
        SpeciesTemplate("shadowasp", "Shadowasp", "DARK", "Slithers in dark rock fissures.", 48, 17, 9, 15, 24, "phantomrazer", "COMMON", Color(0xFF7E57C2), Color(0xFF311B92), Color(0xFFE040FB), "REPTILE", listOf(1 to "scratch", 6 to "shadow_fang")),
        SpeciesTemplate("phantomrazer", "Phantomrazer", "DARK", "Vampiric shadow bat beast.", 82, 28, 15, 22, -1, null, "RARE", Color(0xFF512DA8), Color(0xFF1A237E), Color(0xFFFF007F), "BIRD", listOf(1 to "scratch", 6 to "shadow_fang", 20 to "mystic_blast")),
        SpeciesTemplate("solargryph", "Solargryph Pride", "LIGHT", "Legends of blazing solar fire.", 120, 35, 26, 30, -1, null, "LEGENDARY", Color(0xFFFFF176), Color(0xFFBF360C), Color(0xFFFFD700), "BIRD", listOf(1 to "scratch", 10 to "peck", 35 to "solar_beam")),
        SpeciesTemplate("voiddragon", "Voiddragon Tyrant", "DRAGON", "Nebula dragon from space portals.", 135, 38, 28, 28, -1, null, "LEGENDARY", Color(0xFF4A148C), Color(0x00000000), Color(0xFFE040FB), "DRAGON", listOf(1 to "scratch", 20 to "dragon_claw", 35 to "fire_blast")),
        SpeciesTemplate("runicgolem", "Runic Golem Lord", "MYSTIC", "Ancient machine inscribed with runes.", 130, 30, 34, 18, -1, null, "LEGENDARY", Color(0xFF3F51B5), Color(0xFF00E676), Color(0xFF00E5FF), "ELEMENTAL", listOf(1 to "tackle", 20 to "mystic_blast", 35 to "earthquake")),

        // === 15 PROCEDURAL NEW BASE SPECIES ===
        SpeciesTemplate("mouse", "Mouse", "ELECTRIC", "A small, lightning-fast modular rodent with extreme survival potential.", 42, 11, 8, 16, -1, null, "COMMON", Color(0xFFFFEB3B), Color(0xFFFFF9C4), Color(0xFFFF5722), "ROUND", listOf(1 to "scratch", 5 to "spark")),
        SpeciesTemplate("cat", "Cat", "WIND", "A sleek wandering predator with agile claws and stealthy steps.", 48, 14, 9, 14, -1, null, "COMMON", Color(0xFF90A4AE), Color(0xFFCFD8DC), Color(0xFF78909C), "ROUND", listOf(1 to "scratch", 8 to "peck")),
        SpeciesTemplate("frog", "Frog", "WATER", "Amphibious mud-skipper that blends into riverbed biomes.", 55, 10, 11, 8, -1, null, "COMMON", Color(0xFF4CAF50), Color(0xFFDCEDC8), Color(0xFF00BCD4), "ROUND", listOf(1 to "tackle", 5 to "water_spit")),
        SpeciesTemplate("bird", "Bird", "WIND", "A high-flying predator bird using wings to glide over the clouds.", 45, 12, 8, 15, -1, null, "COMMON", Color(0xFF00BCD4), Color(0xFFE0F7FA), Color(0xFF2196F3), "BIRD", listOf(1 to "tackle", 4 to "peck")),
        SpeciesTemplate("wolf", "Wolf", "EARTH", "Pack survivor beast with high bite coordination and team defense.", 58, 16, 11, 13, -1, null, "COMMON", Color(0xFF795548), Color(0xFFD7CCC8), Color(0xFF3E2723), "QUADRUPED", listOf(1 to "scratch", 10 to "sand_slap")),
        SpeciesTemplate("snake", "Snake", "TOXIC", "A long coil of modular muscle slithering inside wet cave gaps.", 50, 13, 10, 12, -1, null, "COMMON", Color(0xFF9C27B0), Color(0xFFE040FB), Color(0xFF4CAF50), "REPTILE", listOf(1 to "scratch", 6 to "shadow_fang", 10 to "toxic_spit")),
        SpeciesTemplate("turtle", "Turtle", "EARTH", "Indestructible crawler shield supporting elemental spikes and crystal clusters.", 75, 9, 18, 5, -1, null, "COMMON", Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFF795548), "REPTILE", listOf(1 to "tackle", 10 to "iron_bash")),
        SpeciesTemplate("spider", "Spider", "DARK", "An eight-legged crawler weaving toxic and mystical traps.", 46, 15, 7, 14, -1, null, "COMMON", Color(0xFF212121), Color(0xFF7E57C2), Color(0xFFE040FB), "ROUND", listOf(1 to "scratch", 6 to "shadow_fang")),
        SpeciesTemplate("dragon", "Dragon", "DRAGON", "Ancient flying leviathan capable of elemental fusion attacks.", 75, 18, 13, 11, -1, null, "RARE", Color(0xFFFF1744), Color(0xFF212121), Color(0xFFFFC107), "DRAGON", listOf(1 to "scratch", 10 to "dragon_claw", 22 to "fire_blast")),
        SpeciesTemplate("rabbit", "Rabbit", "WIND", "Alert jumping trickster packing huge speed modifiers.", 44, 12, 7, 17, -1, null, "COMMON", Color(0xFFCFD8DC), Color(0xFFECEFF1), Color(0xFFE0F2F1), "QUADRUPED", listOf(1 to "scratch", 8 to "peck")),
        SpeciesTemplate("owl", "Owl", "MYSTIC", "Upright nocturnal strategist tracking targets visually at nightfall.", 52, 13, 10, 11, -1, null, "COMMON", Color(0xFF7E57C2), Color(0xFFD1C4E9), Color(0xFF00E5FF), "BIRD", listOf(1 to "peck", 12 to "mystic_blast")),
        SpeciesTemplate("fox", "Fox", "FIRE", "A crafty fire predator dodging and blinking through dense bush tags.", 48, 15, 9, 13, -1, null, "COMMON", Color(0xFFFF5722), Color(0xFFFFEB3B), Color(0xFFFF9800), "QUADRUPED", listOf(1 to "scratch", 4 to "embers")),
        SpeciesTemplate("shark", "Shark", "WATER", "Sleek predatory jaw swimmer utilizing tall water currents.", 65, 17, 11, 14, -1, null, "COMMON", Color(0xFF0D47A1), Color(0xFF90CAF9), Color(0xFF00E5FF), "REPTILE", listOf(1 to "tackle", 4 to "water_spit")),
        SpeciesTemplate("lizard", "Lizard", "MAGMA", "Thermal segmented lizard crawling through molten fissures.", 52, 14, 11, 12, -1, null, "COMMON", Color(0xFF720D0D), Color(0xFFFF3D00), Color(0xFFFFC107), "REPTILE", listOf(1 to "scratch", 6 to "embers", 14 to "magma_eruption")),
        SpeciesTemplate("golem", "Golem", "METAL", "Artificial colossus composed of floating heavy rune slabs.", 80, 15, 20, 4, -1, null, "RARE", Color(0xFF78909C), Color(0xFFCFD8DC), Color(0xFF00E676), "ELEMENTAL", listOf(1 to "tackle", 10 to "iron_bash", 20 to "earthquake"))
    )

    private val speciesMap = species.associateBy { it.id }

    fun getSpecies(id: String): SpeciesTemplate? = speciesMap[id]

    fun createCreature(speciesId: String, level: Int, slotId: Int = 1, isShiny: Boolean = false): CreatureEntity {
        val template = getSpecies(speciesId) ?: species[0]
        
        val statMultiplier = 1.0f + (level - 1) * 0.12f
        val calculatedMaxHp = (template.baseHp * statMultiplier).toInt()
        val calculatedAttack = (template.baseAttack * statMultiplier).toInt()
        val calculatedDefense = (template.baseDefense * statMultiplier).toInt()
        val calculatedSpeed = (template.baseSpeed * statMultiplier).toInt()

        val learnedMoves = template.levelMoves
            .filter { it.first <= level }
            .map { it.second }
            .distinct()
            .joinToString(",")

        val finalAttack = if (isShiny) (calculatedAttack * 1.15f).toInt() else calculatedAttack
        val finalMaxHp = if (isShiny) (calculatedMaxHp * 1.15f).toInt() else calculatedMaxHp

        // Construct a fully procedural entity complete with dynamic DNA serialized inside the nickname
        val tempEntity = CreatureEntity(
            slotId = slotId,
            speciesId = template.id,
            name = if (isShiny) "★ ${template.name}" else template.name,
            type = template.type,
            level = level,
            xp = 0,
            maxHp = finalMaxHp,
            hp = finalMaxHp,
            attack = finalAttack,
            defense = calculatedDefense,
            speed = calculatedSpeed,
            moves = learnedMoves,
            teamOrder = -1,
            rarity = template.rarity,
            evolutionStage = if (template.evolvesTo == null) 3 else 1,
            isShiny = isShiny,
            nickname = null
        )

        // Seed-generation DNA
        val dna = ProceduralSystems.getOrCreateDNA(tempEntity)
        return tempEntity.copy(nickname = dna.serialize())
    }

    // Comprehensive list of new 10 biomes procedural pool mapping
    val biomePool = mapOf(
        "FOREST" to listOf("fox", "rabbit", "cat", "wolf", "bird", "owl"),
        "LAVA" to listOf("wolf", "lizard", "golem", "dragon", "snake"),
        "SNOW" to listOf("wolf", "bird", "rabbit", "golem", "cat"),
        "SWAMP" to listOf("frog", "snake", "lizard", "spider", "turtle"),
        "CAVE" to listOf("mouse", "spider", "snake", "golem", "cat"),
        "SKY_ISLANDS" to listOf("bird", "owl", "dragon", "mouse"),
        "CRYSTAL_LANDS" to listOf("lizard", "rabbit", "turtle", "golem", "mouse"),
        "DARK_WASTELAND" to listOf("spider", "wolf", "owl", "snake", "cat"),
        "OCEAN" to listOf("shark", "turtle", "bird", "frog"),
        "RUINS" to listOf("spider", "golem", "owl", "dragon", "mouse")
    )

    fun getRandomSpawn(
        biome: String,
        baseLevel: Int,
        slotId: Int,
        weather: String = "SUNNY",
        isNight: Boolean = false
    ): CreatureEntity {
        val upperBiome = biome.uppercase()
        val pool = biomePool[upperBiome] ?: listOf("mouse", "fox")
        
        // Find candidate species based on night / day/ weather priority filters
        var candidate = pool.random()
        
        // Context specific triggers!
        if (isNight && pool.contains("owl")) {
            candidate = "owl" // nocturnal preference
        } else if (upperBiome == "SNOW" && weather == "BLIZZARD" && pool.contains("wolf")) {
            candidate = "wolf" // blizzard predator priority
        } else if (upperBiome == "SWAMP" && weather == "RAINY" && pool.contains("frog")) {
            candidate = "frog" // rain frog priority
        }

        val lvOffset = (-2..3).random()
        val finalLevel = (baseLevel + lvOffset).coerceAtLeast(1)
        val isShiny = (1..100).random() == 1 // 1% shiny success factor

        // Procedural rarity tier determination
        val roll = (1..100).random()
        val rawRarity = when {
            roll <= 3 -> "MYTHIC"
            roll <= 10 -> "LEGENDARY"
            roll <= 25 -> "EPIC"
            roll <= 55 -> "RARE"
            else -> "COMMON"
        }

        // Generate base entity
        val template = getSpecies(candidate) ?: getSpecies("mouse")!!
        val statMultiplier = 1.0f + (finalLevel - 1) * 0.12f
        
        // Boost statistics based on weather, night, and rarity factor
        val pMultiplier = when (rawRarity) {
            "MYTHIC" -> 1.55f
            "LEGENDARY" -> 1.40f
            "EPIC" -> 1.25f
            "RARE" -> 1.12f
            else -> 1.00f
        }

        val calculatedMaxHp = ((template.baseHp * statMultiplier) * pMultiplier).toInt()
        val calculatedAttack = ((template.baseAttack * statMultiplier) * pMultiplier).toInt()
        val calculatedDefense = ((template.baseDefense * statMultiplier) * pMultiplier).toInt()
        val calculatedSpeed = ((template.baseSpeed * statMultiplier) * pMultiplier).toInt()

        // Elemental mapping overrides (e.g. Ice Wolf in blizzard)
        val finalElement = when {
            upperBiome == "SNOW" -> "ICE"
            upperBiome == "LAVA" -> "MAGMA"
            upperBiome == "SWAMP" -> "TOXIC"
            upperBiome == "CRYSTAL_LANDS" -> "CRYSTAL"
            upperBiome == "DARK_WASTELAND" -> "SHADOW"
            weather == "RAINY" -> "WATER"
            else -> template.type
        }

        val movesList = template.levelMoves
            .filter { it.first <= finalLevel }
            .map { it.second }
            .toMutableList()
        
        // Inject moves based on transformed elements
        if (finalElement == "TOXIC" && !movesList.contains("toxic_spit")) movesList.add("toxic_spit")
        if (finalElement == "CRYSTAL" && !movesList.contains("crystal_shard")) movesList.add("crystal_shard")
        if (finalElement == "MAGMA" && !movesList.contains("magma_eruption")) movesList.add("magma_eruption")

        val generatedName = when (rawRarity) {
            "MYTHIC" -> "🔮 MYTHIC ${template.name.uppercase()}"
            "LEGENDARY" -> "👑 LEGENDARY ${template.name.uppercase()}"
            "EPIC" -> "💎 EPIC ${template.name}"
            "RARE" -> "⭐ ${template.name}"
            else -> template.name
        }

        val finalName = if (isShiny) "★ $generatedName" else generatedName

        val tempEntity = CreatureEntity(
            slotId = slotId,
            speciesId = template.id,
            name = finalName,
            type = finalElement,
            level = finalLevel,
            xp = 0,
            maxHp = calculatedMaxHp,
            hp = calculatedMaxHp,
            attack = calculatedAttack,
            defense = calculatedDefense,
            speed = calculatedSpeed,
            moves = movesList.distinct().joinToString(","),
            teamOrder = -1,
            rarity = rawRarity,
            evolutionStage = if (template.evolvesTo == null) 3 else 1,
            isShiny = isShiny,
            nickname = null
        )

        // Seed details
        val seed = Random().nextLong()
        val baseDna = ProceduralSystems.getOrCreateDNA(tempEntity)
        val finalDna = baseDna.copy(element = finalElement, rarity = rawRarity, seed = seed)

        return tempEntity.copy(nickname = finalDna.serialize())
    }
}
