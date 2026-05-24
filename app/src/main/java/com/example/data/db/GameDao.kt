package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CreatureEntity
import com.example.data.model.PlayerProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // Player progress queries
    @Query("SELECT * FROM player_progress WHERE slotId = :slotId")
    fun getPlayerProgress(slotId: Int): Flow<PlayerProgress?>

    @Query("SELECT * FROM player_progress WHERE slotId = :slotId")
    suspend fun getPlayerProgressSync(slotId: Int): PlayerProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProgress(progress: PlayerProgress)

    @Query("DELETE FROM player_progress WHERE slotId = :slotId")
    suspend fun deleteProgressForSlot(slotId: Int)

    // Creature queries
    @Query("SELECT * FROM captured_creatures WHERE slotId = :slotId")
    fun getCreaturesForSlot(slotId: Int): Flow<List<CreatureEntity>>

    @Query("SELECT * FROM captured_creatures WHERE slotId = :slotId")
    suspend fun getCreaturesForSlotSync(slotId: Int): List<CreatureEntity>

    @Query("SELECT * FROM captured_creatures WHERE slotId = :slotId AND teamOrder >= 0 ORDER BY teamOrder ASC")
    fun getTeamForSlot(slotId: Int): Flow<List<CreatureEntity>>

    @Query("SELECT * FROM captured_creatures WHERE slotId = :slotId AND teamOrder >= 0 ORDER BY teamOrder ASC")
    suspend fun getTeamForSlotSync(slotId: Int): List<CreatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreature(creature: CreatureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreatures(creatures: List<CreatureEntity>)

    @Update
    suspend fun updateCreature(creature: CreatureEntity)

    @Delete
    suspend fun deleteCreature(creature: CreatureEntity)

    @Query("DELETE FROM captured_creatures WHERE slotId = :slotId")
    suspend fun deleteCreaturesForSlot(slotId: Int)
}
