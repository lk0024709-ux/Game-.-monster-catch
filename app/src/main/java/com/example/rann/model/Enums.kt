package com.example.rann.model

// ======================================================
// BLADE BRAWL: RANNBHOOMI — CORE ENUMS / DATA MODEL
// Pure Kotlin: no Android or Compose imports in this package.
// ======================================================

// ------------------------------------------------------
// WILD CREATURES (NPC factions)
// ------------------------------------------------------
enum class CreatureType(
    val label: String,
    val icon: String,
    val baseHp: Float,
    val baseSpeed: Float,
    val baseDamage: Float,
    val reach: Float,
    val attackInterval: Float,
    val size: Float,
    val color: Long,
    val specialty: String,
) {
    GRIFFIN(
        label = "Griffin", icon = "🦅",
        baseHp = 70f, baseSpeed = 160f, baseDamage = 14f, reach = 65f,
        attackInterval = 0.7f, size = 20f, color = 0xFFC2410C,
        specialty = "Wing-flap dash every 4s (2x speed)",
    ),
    DRAKE(
        label = "Drake", icon = "🐉",
        baseHp = 55f, baseSpeed = 130f, baseDamage = 16f, reach = 220f,
        attackInterval = 1.4f, size = 22f, color = 0xFF166534,
        specialty = "Ranged fire breath, 400 u/s",
    ),
    SKYHAWK(
        label = "Skyhawk", icon = "🐦",
        baseHp = 45f, baseSpeed = 220f, baseDamage = 12f, reach = 55f,
        attackInterval = 0.5f, size = 16f, color = 0xFF92400E,
        specialty = "Circles at 150, dives every 3s",
    ),
    IRONBOAR(
        label = "Ironboar", icon = "🐗",
        baseHp = 120f, baseSpeed = 110f, baseDamage = 20f, reach = 60f,
        attackInterval = 1.1f, size = 26f, color = 0xFF44403C,
        specialty = "Telegraphed charge every 5s",
    ),
    VENOMTAIL(
        label = "Venomtail", icon = "🐍",
        baseHp = 60f, baseSpeed = 150f, baseDamage = 10f, reach = 70f,
        attackInterval = 0.8f, size = 18f, color = 0xFF4D7C0F,
        specialty = "Venom stacks, max 5 (2 dmg/s each)",
    );

    val ranged: Boolean get() = this == DRAKE
}

// ------------------------------------------------------
// WEAPON TIERS (Forge merge chain)
// maxTargets = Trident dual hit / Mythic reach; aoeDamage = Storm burst.
// ------------------------------------------------------
enum class WeaponTier(
    val tier: Int,
    val icon: String,
    val displayName: String,
    val damage: Float,
    val reach: Float,
    val cooldown: Float,
    val arcMult: Float = 1f,
    val maxTargets: Int = 1,
    val aoeDamage: Float = 0f,
    val doubleHitChance: Float = 0f,
) {
    DAGGER(1, "🗡️", "Dagger", 15f, 55f, 0.35f),
    SWORD(2, "⚔️", "Sword", 22f, 70f, 0.45f),
    AXE(3, "🪓", "Axe", 35f, 75f, 0.55f, arcMult = 1.294f),
    TRIDENT(4, "🔱", "Trident", 50f, 95f, 0.60f, maxTargets = 2),
    STORM(5, "⚡", "Storm Blade", 75f, 110f, 0.70f, aoeDamage = 15f),
    MYTHIC(
        6, "💎", "Mythic Blade", 120f, 130f, 0.80f,
        arcMult = 1.294f, maxTargets = 2, aoeDamage = 15f, doubleHitChance = 0.20f,
    );

    val summary: String
        get() = when (this) {
            DAGGER -> "Fast, short reach"
            SWORD -> "Balanced starter blade"
            AXE -> "Wider swing arc"
            TRIDENT -> "Hits up to 2 foes"
            STORM -> "Lightning burst AoE 60"
            MYTHIC -> "AoE + 20% double strike"
        }

    companion object {
        fun of(tier: Int): WeaponTier = values().firstOrNull { it.tier == tier } ?: DAGGER
        fun maxTier(): Int = MYTHIC.tier
    }
}

// ------------------------------------------------------
// ARMOR RANKS (store)
// ------------------------------------------------------
enum class ArmorRank(
    val displayName: String,
    val icon: String,
    val color: Long,
    val price: Int,
    val hpBonus: Int,
    val defensePercent: Float,
    val speedBonusPercent: Float,
    val description: String,
    val regenPerSec: Float = 0f,
    val reflectPercent: Float = 0f,
    val revives: Int = 0,
    val starter: Boolean = false,
) {
    CLOTH(
        "Cloth Tunic", "🧵", 0xFFA8A29E, 0, 0, 0f, 0f,
        "Starter tunic — basic protection", starter = true,
    ),
    NORMAL(
        "Normal Plate", "🥉", 0xFF9E9E9E, 100, 10, 0.05f, 0f,
        "Basic protection",
    ),
    ADVANCED(
        "Advanced Mail", "🥈", 0xFF66BB6A, 350, 25, 0.12f, 0.03f,
        "+5 HP regen / sec", regenPerSec = 5f,
    ),
    LEGEND(
        "Legend Aegis", "🥇", 0xFFAB47BC, 900, 50, 0.22f, 0.05f,
        "Reflect 10% damage", reflectPercent = 0.10f,
    ),
    EPIC(
        "Epic Carapace", "💎", 0xFFCF8B22, 2200, 100, 0.35f, 0.08f,
        "Revive once per battle", revives = 1,
    );

    companion object {
        val purchasable: List<ArmorRank> get() = values().filter { !it.starter }
    }
}

// ------------------------------------------------------
// BOOSTERS (single-use, one armed per battle)
// ------------------------------------------------------
enum class BoosterType(
    val displayName: String,
    val icon: String,
    val price: Int,
    val shortDesc: String,
    val description: String,
) {
    REVIVE_POTION("Revive Potion", "❤️", 200, "+1 auto-revive", "+1 auto-revive next battle"),
    DAMAGE_ELIXIR("Damage Elixir", "⚔️", 150, "+25% damage", "+25% damage next battle"),
    CHEST_MAGNET("Chest Magnet", "🧲", 100, "2x chests", "2x chest spawn rate next battle"),
    IRON_SKIN("Iron Skin", "🛡️", 180, "+20% defense", "+20% defense next battle"),
}

// ------------------------------------------------------
// RANKED TIERS (7 ranks × 50 levels)
// ------------------------------------------------------
enum class Rank(
    val displayName: String,
    val icon: String,
    val index: Int,
    val color: Long,
    val unlockWins: Int,
    val coinMult: Float,
    val mapNameHint: String,
) {
    BRONZE("Bronze", "🥉", 0, 0xFFA16207, 0, 1.0f, "The Dust Pit"),
    SILVER("Silver", "🥈", 1, 0xFF94A3B8, 3, 1.2f, "Frost Hollow"),
    GOLD("Gold", "🥇", 2, 0xFFD97706, 8, 1.5f, "Ember Forge"),
    PLATINUM("Platinum", "💠", 3, 0xFF0891B2, 15, 2.0f, "Jade Temple"),
    DIAMOND("Diamond", "💎", 4, 0xFF6366F1, 25, 2.5f, "Crystal Cavern"),
    MASTERS("Masters", "🏆", 5, 0xFF7C2D12, 40, 3.0f, "Storm Citadel"),
    GRAND_MASTERS("Grand Masters", "👑", 6, 0xFF1C1917, 60, 5.0f, "Throne of Rann");

    companion object {
        fun of(index: Int): Rank = values()[index.coerceIn(0, values().size - 1)]
    }
}

// ------------------------------------------------------
// LEVEL GRADES
// ------------------------------------------------------
enum class LevelGrade(val letter: String, val baseReward: Int, val label: String, val coinMult: Float) {
    NONE("—", 0, "Not Cleared", 0f),
    D("D", 40, "Cleared", 1.0f),
    C("C", 80, "Fair", 1.2f),
    B("B", 150, "Good", 1.5f),
    A("A", 250, "Excellent", 2.0f),
    S("S", 450, "Master", 3.0f),
    SS("SS", 800, "Flawless", 5.0f);

    val strength: Int get() = ordinal

    companion object {
        /** Final grade = LOWEST of all sub-grades. */
        fun lowest(vararg grades: LevelGrade): LevelGrade = grades.minByOrNull { it.strength } ?: NONE
        fun fromLetter(letter: String?): LevelGrade = values().firstOrNull { it.letter == letter } ?: NONE
    }
}

// ------------------------------------------------------
// LEVEL MODIFIERS (20 total, stack 1-4)
// ------------------------------------------------------
enum class ModCategory {
    NPC, PLAYER, ENVIRONMENT, SPECIAL;

    val label: String
        get() = when (this) {
            NPC -> "Creature boost"
            PLAYER -> "Player handicap"
            ENVIRONMENT -> "Environment"
            SPECIAL -> "Special rule"
        }
}

enum class LevelModifier(
    val id: String,
    val icon: String,
    val description: String,
    val category: ModCategory,
) {
    NPC_TOUGH("npcTough", "🛡️", "+25% NPC health", ModCategory.NPC),
    NPC_FIERCE("npcFierce", "⚔️", "+25% NPC damage", ModCategory.NPC),
    NPC_SWIFT("npcSwift", "💨", "+20% NPC speed", ModCategory.NPC),
    NPC_LEGION("npcLegion", "👥", "+5 creatures", ModCategory.NPC),
    NPC_ELITE("npcElite", "⭐", "Elite tier creatures", ModCategory.NPC),

    NO_HEAL("noHeal", "🚫", "Healing disabled", ModCategory.PLAYER),
    NO_SHIELD("noShield", "🚫", "Shield power-ups disabled", ModCategory.PLAYER),
    IRON_SKINLESS("ironSkinless", "🥄", "Armor effect -50%", ModCategory.PLAYER),
    SLOW_FORGE("slowForge", "🐌", "Weapon cooldown +30%", ModCategory.PLAYER),

    FAST_STORM("fastStorm", "⏱️", "Storm shrinks 30% faster", ModCategory.ENVIRONMENT),
    LOW_GRAVITY("lowGravity", "🎈", "Knockback +50%", ModCategory.ENVIRONMENT),
    SLIPPERY("slippery", "🧊", "Slippery ground", ModCategory.ENVIRONMENT),
    DARKNESS("darkness", "🌑", "Vision -30%", ModCategory.ENVIRONMENT),
    RISING_LAVA("risingLava", "🌋", "Lava keeps rising", ModCategory.ENVIRONMENT),
    VENOM_HAZE("venomHaze", "☠️", "Toxic haze: -1 HP/s in the open", ModCategory.ENVIRONMENT),

    BOSS_RUSH("bossRush", "👑", "Extra mini-boss joins", ModCategory.SPECIAL),
    IRONMAN("ironman", "💀", "1 HP — one hit kills", ModCategory.SPECIAL),
    SPEEDRUN("speedrun", "⏳", "Win within 60s", ModCategory.SPECIAL),
    NO_POWERS("noPowers", "🚫", "No power-ups spawn", ModCategory.SPECIAL),
    MIRROR_MATCH("mirrorMatch", "🪞", "Foes mirror your weapon tier", ModCategory.SPECIAL);

    companion object {
        fun fromId(id: String): LevelModifier? = values().firstOrNull { it.id == id }
    }
}

// ------------------------------------------------------
// POWER-UPS
// ------------------------------------------------------
enum class PowerUpType(val icon: String, val displayName: String, val color: Long, val duration: Float, val description: String) {
    HEAL("❤️", "Healing Draught", 0xFF15803D, 0f, "Restores 35% HP"),
    SHIELD("🛡️", "Ward", 0xFF0891B2, 10f, "-40% damage taken"),
    RAGE("😡", "Bloodrage", 0xFFB91C1C, 10f, "+50% damage dealt"),
    SWIFT("💨", "Windstep", 0xFF3F6212, 10f, "+30% movement speed"),
    VAMP("🩸", "Leech", 0xFF7C2D12, 12f, "+3 HP per landed hit"),
    COIN("🪙", "Coin Cache", 0xFFD97706, 0f, "Instant coins"),
}

// ------------------------------------------------------
// MAP TWIST FEATURES
// ------------------------------------------------------
enum class MapFeature {
    SAND_FOG,
    ICE_PATCHES,
    HEAT_ZONES,
    LAVA_POOLS,
    FIRE_GEYSERS,
    MOVING_WALLS,
    TREASURE_SHRINES,
    MIRROR_CLONES,
    DESTRUCTIBLE_CRYSTALS,
    LIGHTNING_STORM,
    WIND_PUSH,
    TWIST_CYCLE,
    ARENA_BOSS,
}

// ------------------------------------------------------
// THE 7 RANKED MAPS
// ------------------------------------------------------
data class MapTheme(
    val index: Int,
    val name: String,
    val icon: String,
    val tagline: String,
    val twistText: String,
    val coinMult: Float,
    val shrinkMult: Float,
    val sky: Long,
    val ground: Long,
    val groundAlt: Long,
    val accent: Long,
    val npcPool: List<CreatureType>,
    val features: List<MapFeature>,
)

object Maps {
    val ALL: List<MapTheme> = listOf(
        MapTheme(
            index = 0, name = "The Dust Pit", icon = "🥉",
            tagline = "Sandy beige, dead shrubs, bleached bones.",
            twistText = "Sandfog every 12s — 5s of -15% speed and murky vision.",
            coinMult = 1.0f, shrinkMult = 1.2f,
            sky = 0xFFE4D6B8, ground = 0xFFDCC79C, groundAlt = 0xFFCDB686, accent = 0xFF8A6A3B,
            npcPool = listOf(CreatureType.GRIFFIN, CreatureType.SKYHAWK, CreatureType.IRONBOAR),
            features = listOf(MapFeature.SAND_FOG),
        ),
        MapTheme(
            index = 1, name = "Frost Hollow", icon = "🥈",
            tagline = "Icy blues, pines, frozen ponds.",
            twistText = "Slippery ice patches + chill: -2 HP/s outside heat zones.",
            coinMult = 1.2f, shrinkMult = 1.0f,
            sky = 0xFFDCE7F0, ground = 0xFFE8EFF5, groundAlt = 0xFFC9D9E6, accent = 0xFF4E6E8E,
            npcPool = listOf(CreatureType.SKYHAWK, CreatureType.DRAKE, CreatureType.VENOMTAIL),
            features = listOf(MapFeature.ICE_PATCHES, MapFeature.HEAT_ZONES),
        ),
        MapTheme(
            index = 2, name = "Ember Forge", icon = "🥇",
            tagline = "Dark volcanic rock, lava pools, drifting embers.",
            twistText = "Lava pools (5 dmg/s) + fire geysers every 8s.",
            coinMult = 1.5f, shrinkMult = 1.0f,
            sky = 0xFF3A2B24, ground = 0xFF4A3A31, groundAlt = 0xFF5C463A, accent = 0xFFC2410C,
            npcPool = listOf(CreatureType.IRONBOAR, CreatureType.DRAKE, CreatureType.GRIFFIN),
            features = listOf(MapFeature.LAVA_POOLS, MapFeature.FIRE_GEYSERS),
        ),
        MapTheme(
            index = 3, name = "Jade Temple", icon = "💠",
            tagline = "Jade stone, broken columns, gold veins.",
            twistText = "4 moving walls on tracks + 2 treasure shrines.",
            coinMult = 2.0f, shrinkMult = 0.9f,
            sky = 0xFFDCE8DA, ground = 0xFFCFE0CB, groundAlt = 0xFFB6CCB2, accent = 0xFF8A6A1F,
            npcPool = listOf(CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR, CreatureType.VENOMTAIL),
            features = listOf(MapFeature.MOVING_WALLS, MapFeature.TREASURE_SHRINES),
        ),
        MapTheme(
            index = 4, name = "Crystal Cavern", icon = "💎",
            tagline = "Deep purple cave walls, humming crystal veins.",
            twistText = "Mirror clones every 20s — smash 3 crystals to stall them 15s.",
            coinMult = 2.5f, shrinkMult = 0.9f,
            sky = 0xFF2A2338, ground = 0xFF352D46, groundAlt = 0xFF443A58, accent = 0xFF6366F1,
            npcPool = listOf(CreatureType.SKYHAWK, CreatureType.VENOMTAIL, CreatureType.DRAKE, CreatureType.GRIFFIN),
            features = listOf(MapFeature.MIRROR_CLONES, MapFeature.DESTRUCTIBLE_CRYSTALS),
        ),
        MapTheme(
            index = 5, name = "Storm Citadel", icon = "🏆",
            tagline = "Dark grey clouds, floating stone platforms.",
            twistText = "Lightning every 6s + rotating wind push (watch the HUD arrow).",
            coinMult = 3.0f, shrinkMult = 0.85f,
            sky = 0xFF2F3338, ground = 0xFF3E444A, groundAlt = 0xFF4E555C, accent = 0xFF94A3B8,
            npcPool = listOf(CreatureType.SKYHAWK, CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR),
            features = listOf(MapFeature.LIGHTNING_STORM, MapFeature.WIND_PUSH),
        ),
        MapTheme(
            index = 6, name = "Throne of Rann", icon = "👑",
            tagline = "Cosmic void, black marble, gold constellations.",
            twistText = "All twists cycle every 30s + Rann's Avatar guards the throne.",
            coinMult = 5.0f, shrinkMult = 0.8f,
            sky = 0xFF12100F, ground = 0xFF1D1B19, groundAlt = 0xFF272422, accent = 0xFFD97706,
            npcPool = listOf(CreatureType.GRIFFIN, CreatureType.DRAKE, CreatureType.IRONBOAR, CreatureType.VENOMTAIL, CreatureType.SKYHAWK),
            features = listOf(MapFeature.TWIST_CYCLE, MapFeature.ARENA_BOSS),
        ),
    )

    fun of(index: Int): MapTheme = ALL[index.coerceIn(0, ALL.size - 1)]

    /**
     * Elite Mode arenas: themed stages with no twists and no storm,
     * so the duel is purely chimera vs warrior.
     */
    fun eliteArena(chimeraIndex: Int): MapTheme {
        val t = ChimeraType.values()[chimeraIndex.coerceIn(0, ChimeraType.values().size - 1)]
        return MapTheme(
            index = chimeraIndex,
            name = t.arenaName,
            icon = t.emoji,
            tagline = "Elite trial arena",
            twistText = "Pure 1v1 — no twists, no storm, no helpers.",
            coinMult = 1f,
            shrinkMult = 1f,
            sky = t.arenaSky,
            ground = t.arenaGround,
            groundAlt = t.body,
            accent = t.accent,
            npcPool = emptyList(),
            features = emptyList(),
        )
    }
}

// ------------------------------------------------------
// ELITE MODE — THE CHIMERA TRIALS
// ------------------------------------------------------
enum class ChimeraType(
    val displayName: String,
    val emoji: String,
    val baseHp: Float,
    val sizeMult: Float,
    val damageMult: Float,
    val abilitySummary: String,
    val body: Long,
    val accent: Long,
    val arenaName: String,
    val arenaGround: Long,
    val arenaSky: Long,
) {
    GRYPHON_PRIME(
        "Gryphon Prime", "🦁🦅", 800f, 3.5f, 1.0f, "Dive + Shockwave",
        0xFFC2410C, 0xFFF3D9A4, "Skyfall Peak", 0xFF6B6B66, 0xFFB9BDC2,
    ),
    BASILISK(
        "Basilisk", "🐉🐍", 900f, 4.0f, 1.05f, "Poison Breath + Petrify",
        0xFF166534, 0xFF65A30D, "Sunken Serpent Hall", 0xFF3F4A3C, 0xFF8FA88C,
    ),
    RAVAGER(
        "Ravager", "🐗🦁", 1200f, 4.5f, 1.05f, "Charge + Roar",
        0xFF44403C, 0xFFB45309, "The Blasted Plain", 0xFF6A5A48, 0xFFD8C7A4,
    ),
    WYVERN(
        "Wyvern", "🦅🐉", 700f, 4.0f, 1.1f, "Flight + Fireballs",
        0xFF7C2D12, 0xFFD97706, "Thunder Cliff", 0xFF4E555C, 0xFFA6AFB8,
    ),
    DEATHROLL(
        "Deathroll", "🐍🐗", 1000f, 4.5f, 1.05f, "Venom Aura + Roll",
        0xFF4D7C0F, 0xFF3F6212, "Marsh of Fangs", 0xFF4A5240, 0xFF9DAE86,
    ),
    RANNS_CHIMERA(
        "Rann's Chimera", "🦁🐗🦅", 1800f, 6.0f, 1.25f, "Full Arsenal",
        0xFF7C2D12, 0xFFD97706, "Colosseum of Rann", 0xFF3A342E, 0xFFC8B79A,
    ),
    OMEGA_CHIMERA(
        "Omega Chimera", "🐲", 3000f, 8.0f, 1.35f, "Reality Shift",
        0xFF1C1917, 0xFF6366F1, "The Void Rift", 0xFF18161A, 0xFF3B3550,
    );

    companion object {
        val hpMultiplier = 3.2f // tuned so fights last 60-140s at tier 3-4 weapons
    }
}

enum class ChimeraAbility(
    val displayName: String,
    val baseDamage: Float,
    val cooldown: Float,
    val telegraph: Float,
    val radius: Float,
) {
    DIVE_STRIKE("Dive Strike", 45f, 7f, 0.9f, 95f),
    SHOCKWAVE("Shockwave", 40f, 8f, 1.0f, 250f),
    POISON_BREATH("Poison Breath", 25f, 6f, 0.8f, 220f),
    FIREBALLS("Fireball Volley", 20f, 5f, 0.6f, 140f),
    PETRIFY("Petrify Gaze", 8f, 9f, 0.8f, 320f),
    CHARGE("Ravager Charge", 60f, 7f, 0.7f, 70f),
    ROAR("Terrifying Roar", 12f, 10f, 0.6f, 300f),
    ROLL_ATTACK("Death Roll", 50f, 6f, 0.7f, 80f),
    TAIL_SWIPE("Tail Swipe", 35f, 3f, 0.4f, 110f),
    LIGHTNING_STRIKE("Rift Lightning", 30f, 5f, 1.0f, 90f),
}

enum class ChimeraDefense(val displayName: String, val description: String) {
    IRON_HIDE("Iron Hide", "Takes 40% less damage"),
    REGEN("Monstrous Regen", "Heals 8 HP/s below 45% HP"),
    ENERGY_SHIELD("Energy Shield", "200 point shield, regenerates every 15s"),
    DODGE_INSTINCT("Dodge Instinct", "30% chance to evade"),
    DAMAGE_REFLECT("Spiked Hide", "Chance to reflect a slice of damage"),
    PHASE_SHIFT("Phase Shift", "1s invulnerability at 66% and 33%"),
    SUMMON_MINIONS("Summon Minions", "Two 150 HP minions every 20s"),
    MIRROR_IMAGE("Mirror Image", "Spawns a decoy clone for 10s"),
}

// ------------------------------------------------------
// BATTLE PLUMBING
// ------------------------------------------------------
enum class BattleKind { CAMPAIGN, ELITE }

enum class BattleState { INTRO, RUNNING, WON, LOST }

/** Where a battle was launched from, used to route results back. */
enum class BattleOrigin {
    CAMPAIGN,
    ELITE,
}
