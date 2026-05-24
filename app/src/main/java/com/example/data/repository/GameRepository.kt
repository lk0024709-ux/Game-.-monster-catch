package com.example.data.repository

import com.example.data.db.GameDao
import com.example.data.model.CreatureEntity
import com.example.data.model.PlayerProgress
import com.example.game.CreatureDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class GameRepository(private val gameDao: GameDao) {

    fun getPlayerProgress(slotId: Int): Flow<PlayerProgress?> =
        gameDao.getPlayerProgress(slotId)

    suspend fun getPlayerProgressSync(slotId: Int): PlayerProgress? =
        gameDao.getPlayerProgressSync(slotId)

    suspend fun savePlayerProgress(progress: PlayerProgress) {
        gameDao.insertPlayerProgress(progress)
    }

    fun getCreatures(slotId: Int): Flow<List<CreatureEntity>> =
        gameDao.getCreaturesForSlot(slotId)

    fun getTeam(slotId: Int): Flow<List<CreatureEntity>> =
        gameDao.getTeamForSlot(slotId)

    suspend fun getTeamSync(slotId: Int): List<CreatureEntity> =
        gameDao.getTeamForSlotSync(slotId)

    suspend fun getCreaturesSync(slotId: Int): List<CreatureEntity> =
        gameDao.getCreaturesForSlotSync(slotId)

    suspend fun addCreature(creature: CreatureEntity): Long =
        gameDao.insertCreature(creature)

    suspend fun updateCreature(creature: CreatureEntity) {
        gameDao.updateCreature(creature)
    }

    suspend fun deleteCreature(creature: CreatureEntity) {
        gameDao.deleteCreature(creature)
    }

    suspend fun clearSlot(slotId: Int) {
        gameDao.deleteProgressForSlot(slotId)
        gameDao.deleteCreaturesForSlot(slotId)
    }

    // Automatically initialize a pristine adventure state with balanced starters
    suspend fun initializeNewGame(slotId: Int, heroName: String) {
        // 1. Clear previous slots
        clearSlot(slotId)

        // 2. Insert pristine player progression
        val starterProg = PlayerProgress(
            slotId = slotId,
            playerName = heroName,
            coins = 800, // starting coins
            gems = 30,   // starting gems
            level = 1,
            xp = 0,
            currentQuest = "Defeat Forest Champion Leafy!",
            questProgress = 0,
            currentBiome = "FOREST",
            playerX = 150f,
            playerY = 150f
        )
        gameDao.insertPlayerProgress(starterProg)

        // 3. Insert iconic starters: Pyrofox, Pipfin, Seedling
        val s1 = CreatureDatabase.createCreature("pyrofox", 5, slotId).copy(teamOrder = 0)
        val s2 = CreatureDatabase.createCreature("seedling", 5, slotId).copy(teamOrder = 1)
        val s3 = CreatureDatabase.createCreature("pipfin", 5, slotId).copy(teamOrder = 2)

        gameDao.insertCreature(s1)
        gameDao.insertCreature(s2)
        gameDao.insertCreature(s3)
    }
}
