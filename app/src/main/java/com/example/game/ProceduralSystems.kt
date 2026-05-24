package com.example.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.CreatureEntity
import java.util.Random

// ======================================================
// PROCEDURAL CREATURE SYSTEMS DEFINITION (AAA GRADE)
// ======================================================

enum class SurvivalMovementStyle {
    HOPPING, FLYING, CRAWLING, SLITHERING, WALKING, GLIDING, STEALTH
}

data class BaseSpeciesDef(
    val id: String,
    val name: String,
    val movement: SurvivalMovementStyle,
    val baseHpHint: Int,
    val baseAttackHint: Int,
    val baseDefenseHint: Int,
    val baseSpeedHint: Int,
    val description: String
)

data class CreatureDNA(
    val speciesA: String,          // Primary base species
    val speciesB: String?,         // Hybrid partner (visual hybrid blending)
    val element: String,           // FIRE, ICE, ELECTRIC, TOXIC, SHADOW, CRYSTAL, NATURE, WATER, WIND, MAGMA
    val rarity: String,            // COMMON, RARE, EPIC, LEGENDARY, MYTHIC
    val mutations: List<String>,   // List of active mutations: spikes, crystal_growth, cyber_implants, scales, poison_veins, lava_cracks, glowing_runes, shadow_smoke, energy_lines
    val scale: Float,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val headType: String,          // ROUND, FLUFFY, SCALED, WINGED, HORNED, DIGITAL
    val eyeStyle: String,          // ANIME_SHINY, GLOWING_EMBERS, SHADOW_RING, CAT_SLIT, MONOCLE, CROSS
    val tailStyle: String,         // FLAME_BRUSH, SPIKED_CLUB, FEATHER_COIL, FIN_SPLIT, LIGHTNING_BOLT, CRYSTAL_SPIN, NONE
    val hornStyle: String,         // RAM_SPIRAL, UNIDORN, DRAGON_SPIKES, CRYSTAL_PRISM, ANTENNA, NONE
    val accessoryStyle: String,    // RUNE_AMULET, CYBER_VISOR, FLOWER_CROWN, SPARK_COLLAR, NONE
    val seed: Long
)

object ProceduralSystems {

    val speciesMap = mapOf(
        "mouse" to BaseSpeciesDef("mouse", "Squeakster", SurvivalMovementStyle.HOPPING, 42, 11, 8, 16, "A tiny, extremely nimble rodent packed with potential."),
        "cat" to BaseSpeciesDef("cat", "Felink", SurvivalMovementStyle.STEALTH, 48, 14, 9, 14, "A graceful feline that stalks in silences, flashing sharp senses."),
        "frog" to BaseSpeciesDef("frog", "Croaker", SurvivalMovementStyle.HOPPING, 55, 10, 11, 8, "An amphibious hopper capable of leaping high into tree branches."),
        "bird" to BaseSpeciesDef("bird", "Avipen", SurvivalMovementStyle.FLYING, 45, 12, 8, 15, "A beautiful avian flyer with razor-sharp gaze and flapping wingbeats."),
        "wolf" to BaseSpeciesDef("wolf", "Lupis", SurvivalMovementStyle.WALKING, 58, 16, 11, 13, "A fierce canine predator that relies on coordination and swift bites."),
        "snake" to BaseSpeciesDef("snake", "Serpen", SurvivalMovementStyle.SLITHERING, 50, 13, 10, 12, "A coiled, slithering reptile that can strike with venomous speed."),
        "turtle" to BaseSpeciesDef("turtle", "Shelldon", SurvivalMovementStyle.CRAWLING, 75, 9, 18, 5, "An ancient crawl-walker protected by an ultra-durable heavy shell."),
        "spider" to BaseSpeciesDef("spider", "Arachnid", SurvivalMovementStyle.CRAWLING, 46, 15, 7, 14, "An eight-legged creeper that weaves glowing visual elemental webs."),
        "dragon" to BaseSpeciesDef("dragon", "Draconis", SurvivalMovementStyle.GLIDING, 75, 18, 13, 11, "A magnificent flying lizard with hardened scales and horns."),
        "rabbit" to BaseSpeciesDef("rabbit", "Hoppit", SurvivalMovementStyle.HOPPING, 44, 12, 7, 17, "A fluffy, lightning-fast jumper with oversized scanning ears."),
        "owl" to BaseSpeciesDef("owl", "Strix", SurvivalMovementStyle.FLYING, 52, 13, 10, 11, "A silent nocturnal wisdom-bird that tracks targets in absolute darkness."),
        "fox" to BaseSpeciesDef("fox", "Vulpix", SurvivalMovementStyle.STEALTH, 48, 15, 9, 13, "A highly intelligent, bushy-tailed visual trickster."),
        "shark" to BaseSpeciesDef("shark", "Sharq", SurvivalMovementStyle.GLIDING, 65, 17, 11, 14, "A fierce hydrodynamic predator with sleek lines and a tall dorsal fin."),
        "lizard" to BaseSpeciesDef("lizard", "Sauria", SurvivalMovementStyle.CRAWLING, 52, 14, 11, 12, "An elongated, agile climber decorated with spikes and armored plating."),
        "golem" to BaseSpeciesDef("golem", "Colossus", SurvivalMovementStyle.WALKING, 80, 15, 20, 4, "A heavy artificial guardian composed of floating stone runes.")
    )

    val elements = listOf("FIRE", "ICE", "ELECTRIC", "TOXIC", "SHADOW", "CRYSTAL", "NATURE", "WATER", "WIND", "MAGMA")

    val mutations = listOf("spikes", "crystal_growth", "cyber_implants", "scales", "poison_veins", "lava_cracks", "glowing_runes", "shadow_smoke", "energy_lines")

    val rarities = listOf("COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")

    fun CreatureDNA.serialize(): String {
        val b = speciesB ?: "none"
        val muts = if (mutations.isEmpty()) "none" else mutations.joinToString(",")
        return "DNA:$speciesA|$b|$element|$rarity|$muts|$scale|${primaryColor.toArgb()}|${secondaryColor.toArgb()}|${accentColor.toArgb()}|$headType|$eyeStyle|$tailStyle|$hornStyle|$accessoryStyle|$seed"
    }

    fun parseDNA(dnaStr: String): CreatureDNA? {
        if (!dnaStr.startsWith("DNA:")) return null
        return try {
            val parts = dnaStr.substring(4).split("|")
            val speciesA = parts[0]
            val speciesB = if (parts[1] == "none") null else parts[1]
            val element = parts[2]
            val rarity = parts[3]
            val mutations = if (parts[4] == "none" || parts[4].isEmpty()) emptyList() else parts[4].split(",")
            val scale = parts[5].toFloatOrNull() ?: 1.0f
            val primaryColor = Color(parts[6].toLongOrNull()?.toInt() ?: 0xFF9E9E9E.toInt())
            val secondaryColor = Color(parts[7].toLongOrNull()?.toInt() ?: 0xFFE0E0E0.toInt())
            val accentColor = Color(parts[8].toLongOrNull()?.toInt() ?: 0xFF212121.toInt())
            val headType = parts[9]
            val eyeStyle = parts[10]
            val tailStyle = parts[11]
            val hornStyle = parts[12]
            val accessoryStyle = parts[13]
            val seed = parts[14].toLongOrNull() ?: 0L
            CreatureDNA(speciesA, speciesB, element, rarity, mutations, scale, primaryColor, secondaryColor, accentColor, headType, eyeStyle, tailStyle, hornStyle, accessoryStyle, seed)
        } catch (e: Exception) {
            null
        }
    }

    fun getOrCreateDNA(entity: CreatureEntity): CreatureDNA {
        // If nickname is set and starts with DNS marker, parse it!
        if (entity.nickname != null && entity.nickname.startsWith("DNA:")) {
            val parsed = parseDNA(entity.nickname)
            if (parsed != null) return parsed
        }

        // Generate deterministically from values to prevent migration state drops
        val seed = (entity.instanceId + 1) * 31L + entity.speciesId.hashCode() + entity.maxHp * 13L + entity.attack * 7L + (if (entity.isShiny) 777L else 0L)
        val rand = Random(seed)

        // Find primary species (remap legacy species to the new base list)
        val remapPrimary = when (entity.speciesId) {
            "pyrofox", "pyrofang", "pyrosaur" -> "fox"
            "pipfin", "aquafin", "leviapod" -> "bird"
            "seedling", "leafant", "terrasaur" -> "frog"
            "sparky", "voltex" -> "mouse"
            "cubfrost", "glaciard" -> "wolf"
            "boulder", "goliath" -> "golem"
            "shadowasp", "phantomrazer" -> "snake"
            "solargryph" -> "dragon"
            "voiddragon" -> "dragon"
            "runicgolem" -> "golem"
            else -> if (speciesMap.containsKey(entity.speciesId)) entity.speciesId else "mouse"
        }

        val primary = remapPrimary
        // Support dynamic Hybrid matching - 25% chance of hybridization for level > 8
        val speciesB = if (entity.level > 8 && rand.nextFloat() < 0.3f) {
            val pool = speciesMap.keys.filter { it != primary }
            if (pool.isNotEmpty()) pool[rand.nextInt(pool.size)] else null
        } else {
            null
        }

        val el = entity.type
        val rarity = entity.rarity

        // Mutations calculation: up to 3 depending on level and rarity
        val activeMuts = mutableListOf<String>()
        val pMuts = mutations.shuffled(rand)
        val limit = when (rarity) {
            "COMMON" -> if (rand.nextFloat() < 0.12f) 1 else 0
            "RARE" -> 1
            "EPIC" -> 2
            "LEGENDARY" -> 3
            "MYTHIC" -> 4
            else -> 0
        }
        for (i in 0 until limit.coerceAtMost(pMuts.size)) {
            activeMuts.add(pMuts[i])
        }

        // Procedural coloration
        val (pc, sc, ac) = getProceduralColors(el, entity.isShiny, rand)

        val headTypes = listOf("ROUND", "FLUFFY", "SCALED", "WINGED", "HORNED", "DIGITAL")
        val eyeStyles = if (rarity == "MYTHIC") listOf("GLOWING_EMBERS", "SHADOW_RING") else listOf("ANIME_SHINY", "CAT_SLIT", "CROSS", "GLOWING_EMBERS")
        val tailStyles = listOf("FLAME_BRUSH", "SPIKED_CLUB", "FEATHER_COIL", "FIN_SPLIT", "LIGHTNING_BOLT", "CRYSTAL_SPIN", "NONE")
        val hornStyles = listOf("RAM_SPIRAL", "UNIDORN", "DRAGON_SPIKES", "CRYSTAL_PRISM", "ANTENNA", "NONE")
        val accessoryStyles = listOf("RUNE_AMULET", "CYBER_VISOR", "FLOWER_CROWN", "SPARK_COLLAR", "NONE")

        return CreatureDNA(
            speciesA = primary,
            speciesB = speciesB,
            element = el,
            rarity = rarity,
            mutations = activeMuts,
            scale = 0.85f + (rand.nextFloat() * 0.30f),
            primaryColor = pc,
            secondaryColor = sc,
            accentColor = ac,
            headType = headTypes[rand.nextInt(headTypes.size)],
            eyeStyle = eyeStyles[rand.nextInt(eyeStyles.size)],
            tailStyle = tailStyles[rand.nextInt(tailStyles.size)],
            hornStyle = hornStyles[rand.nextInt(hornStyles.size)],
            accessoryStyle = accessoryStyles[rand.nextInt(accessoryStyles.size)],
            seed = seed
        )
    }

    private fun getProceduralColors(element: String, isShiny: Boolean, rand: Random): Triple<Color, Color, Color> {
        if (isShiny) {
            return Triple(
                Color(0xFFFFD700), // Glowing gold
                Color(0xFFFFF176), // Shiny light yellow
                Color(0xFFFF3D00)  // Sparkle intense hot orange
            )
        }

        return when (element) {
            "FIRE" -> Triple(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFEB3B))
            "ICE" -> Triple(Color(0xFF80DEEA), Color(0xFF00BCD4), Color(0xFFE0F7FA))
            "ELECTRIC" -> Triple(Color(0xFFFFEB3B), Color(0xFFFFEB3B), Color(0xFFE040FB))
            "TOXIC" -> Triple(Color(0xFF8E24AA), Color(0xFF00E676), Color(0xFF76FF03))
            "SHADOW" -> Triple(Color(0xFF1F1235), Color(0xFF3F1D72), Color(0xFFE040FB))
            "CRYSTAL" -> Triple(Color(0xFFE91E63), Color(0xFF00E5FF), Color(0xFFF48FB1))
            "NATURE" -> Triple(Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFD7CCC8))
            "WATER" -> Triple(Color(0xFF2196F3), Color(0xFF90CAF9), Color(0xFF0D47A1))
            "WIND" -> Triple(Color(0xFF90A4AE), Color(0xFFCFD8DC), Color(0xFF00ACC1))
            "MAGMA" -> Triple(Color(0xFF720D0D), Color(0xFFFF3D00), Color(0xFFFFC107))
            else -> Triple(Color(0xFF607D8B), Color(0xFFB0BEC5), Color(0xFF212121))
        }
    }
}
