package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_progress")
data class PlayerProgress(
    @PrimaryKey val slotId: Int = 1,
    val playerName: String = "Hero",
    val coins: Int = 500,
    val gems: Int = 50,
    val level: Int = 1,
    val xp: Int = 0,
    val currentQuest: String = "Begin your journey in hope forest!",
    val questProgress: Int = 0, // step count or state
    val currentBiome: String = "FOREST",
    val playerX: Float = 100f,
    val playerY: Float = 100f,
    val unlockedBiomes: String = "FOREST", // comma-separated list of unlocked biomes
    val serializedInventory: String = "orb:10,potion:5,revive:2", // key-value serializing items
    val dayCount: Int = 1,
    val lastSaved: Long = System.currentTimeMillis()
)

@Entity(tableName = "captured_creatures")
data class CreatureEntity(
    @PrimaryKey(autoGenerate = true) val instanceId: Long = 0,
    val slotId: Int = 1, // linked to the save slot
    val speciesId: String,
    val name: String,
    val type: String,
    val level: Int = 5,
    val xp: Int = 0,
    val maxHp: Int = 60,
    val hp: Int = 60,
    val attack: Int = 12,
    val defense: Int = 10,
    val speed: Int = 11,
    val moves: String, // comma-separated move ids (e.g. "scratch,embers")
    val teamOrder: Int = -1, // -1 means in box storage, 0-5 represents team slot
    val rarity: String = "COMMON",
    val evolutionStage: Int = 1,
    val isShiny: Boolean = false,
    val nickname: String? = null
)
