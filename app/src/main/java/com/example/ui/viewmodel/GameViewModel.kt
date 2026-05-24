package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.GameDatabase
import com.example.data.model.CreatureEntity
import com.example.data.model.PlayerProgress
import com.example.data.repository.GameRepository
import com.example.game.CreatureDatabase
import com.example.game.MythicType
import com.example.game.SoundEffectsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameState {
    SPLASH,
    MENU,
    WORLD,
    BATTLE,
    INVENTORY,
    SETTINGS
}

data class BattleState(
    val playerCreature: CreatureEntity? = null,
    val enemyCreature: CreatureEntity? = null,
    val isPlayerTurn: Boolean = true,
    val battleLogs: List<String> = emptyList(),
    val isFinished: Boolean = false,
    val resultMessage: String = "",
    val cameraShake: Boolean = false,
    val isCapturing: Boolean = false,
    val captureRollState: Int = 0, // 0-idle, 1,2,3 - ball shakes, 4 - caught, 5 - failed
    val isBoss: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GameDatabase.getDatabase(application)
    private val repository = GameRepository(database.gameDao())

    private var activeCollectJob: kotlinx.coroutines.Job? = null
    private var locationSaveJob: kotlinx.coroutines.Job? = null

    // UI Navigation Game State
    private val _gameState = MutableStateFlow(GameState.SPLASH)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Current Active Save Slot
    private val _currentSlot = MutableStateFlow(1)
    val currentSlot: StateFlow<Int> = _currentSlot.asStateFlow()

    // Player Save Data Reactives
    private val _playerProgress = MutableStateFlow<PlayerProgress?>(null)
    val playerProgress: StateFlow<PlayerProgress?> = _playerProgress.asStateFlow()

    private val _creatures = MutableStateFlow<List<CreatureEntity>>(emptyList())
    val creatures: StateFlow<List<CreatureEntity>> = _creatures.asStateFlow()

    private val _team = MutableStateFlow<List<CreatureEntity>>(emptyList())
    val team: StateFlow<List<CreatureEntity>> = _team.asStateFlow()

    // Weather & Atmospheric Settings
    private val _weather = MutableStateFlow("SUNNY")
    val weather: StateFlow<String> = _weather.asStateFlow()

    private val _isDay = MutableStateFlow(true)
    val isDay: StateFlow<Boolean> = _isDay.asStateFlow()

    // Active Battle State
    private val _battleState = MutableStateFlow<BattleState?>(null)
    val battleState: StateFlow<BattleState?> = _battleState.asStateFlow()

    // Items map (Potion, Orb, Revive) parsed from DB
    private val _inventory = MutableStateFlow<Map<String, Int>>(emptyMap())
    val inventory: StateFlow<Map<String, Int>> = _inventory.asStateFlow()

    private var soundEnabled = true
    var inventoryInitialTab = 0

    fun openInventory(tab: Int) {
        inventoryInitialTab = tab
        setGameState(GameState.INVENTORY)
    }

    init {
        // Preload memory assets upon startup
        viewModelScope.launch(Dispatchers.IO) {
            com.example.game.AssetRepository.preloadAssets(application)
        }

        // Run a constant slowly rotating atmospheric tick
        viewModelScope.launch {
            while (true) {
                delay(40000) // update atmospheric properties every 40 sec
                _isDay.value = !_isDay.value
                val randWeather = listOf("SUNNY", "RAINY", "SNOWY", "SANDSTORM", "MISTY").random()
                _weather.value = randWeather
            }
        }
    }

    fun setGameState(state: GameState) {
        _gameState.value = state
        SoundEffectsManager.playSelected()
        if (state == GameState.SPLASH || state == GameState.MENU) {
            // Cancel any active observation of database slots when not actively exploring/battling
            activeCollectJob?.cancel()
            activeCollectJob = null
            locationSaveJob?.cancel()
            locationSaveJob = null
            _playerProgress.value = null
            _creatures.value = emptyList()
            _team.value = emptyList()
        }
    }

    // Load slot state
    fun selectSlot(slotId: Int) {
        _currentSlot.value = slotId
        SoundEffectsManager.playSelected()
        
        // Ensure we cancel any existing slot flows/collators before loading the new slot
        activeCollectJob?.cancel()
        locationSaveJob?.cancel()
        
        activeCollectJob = viewModelScope.launch(Dispatchers.IO) {
            // Load and initialize
            var progress = repository.getPlayerProgressSync(slotId)
            if (progress == null) {
                repository.initializeNewGame(slotId, "Hero ${Random.nextInt(100, 999)}")
                progress = repository.getPlayerProgressSync(slotId)
            }
            
            _playerProgress.value = progress
            parseInventory(progress?.serializedInventory ?: "")

            // Gather reactive databases for this slot safely in a structured child launch
            launch(Dispatchers.Main) {
                repository.getCreatures(slotId).collect {
                    _creatures.value = it
                    _team.value = it.filter { c -> c.teamOrder >= 0 }.sortedBy { c -> c.teamOrder }
                }
            }
            _gameState.value = GameState.WORLD
        }
    }

    fun getPlayerProgress(slotId: Int): kotlinx.coroutines.flow.Flow<PlayerProgress?> {
        return repository.getPlayerProgress(slotId)
    }

    fun deleteSlot(slotId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSlot(slotId)
            if (_currentSlot.value == slotId) {
                _playerProgress.value = null
                _creatures.value = emptyList()
                _team.value = emptyList()
            }
        }
        SoundEffectsManager.playCaptureFail()
    }

    private fun parseInventory(str: String) {
        val map = str.split(",").associate {
            val tokens = it.split(":")
            if (tokens.size == 2) {
                tokens[0].trim() to (tokens[1].trim().toIntOrNull() ?: 0)
            } else {
                "" to 0
            }
        }.filterKeys { it.isNotEmpty() }
        _inventory.value = map
    }

    private fun serializeInventory(map: Map<String, Int>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    fun addCoins(amount: Int) {
        val current = _playerProgress.value ?: return
        val updated = current.copy(coins = current.coins + amount)
        saveProgress(updated)
    }

    // Persistent save triggered in background threads
    private fun saveProgress(prog: PlayerProgress) {
        _playerProgress.value = prog
        // Cancel outstanding location saves since we are committing an explicit progressive change
        locationSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePlayerProgress(prog)
        }
    }

    fun updatePlayerLocation(x: Float, y: Float) {
        val curr = _playerProgress.value ?: return
        val updated = curr.copy(playerX = x, playerY = y)
        _playerProgress.value = updated
        
        // Performance-focused debounce: Cancel any pending location saves,
        // and commit coordinates only after 400 milliseconds of walk inactivity.
        locationSaveJob?.cancel()
        locationSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400)
            repository.savePlayerProgress(updated)
        }
    }

    fun changeBiome(biome: String) {
        val curr = _playerProgress.value ?: return
        val currentUnlocked = curr.unlockedBiomes.split(",")
        val updatedUnlocked = if (!currentUnlocked.contains(biome)) {
            curr.unlockedBiomes + ",$biome"
        } else {
            curr.unlockedBiomes
        }
        val updated = curr.copy(
            currentBiome = biome,
            unlockedBiomes = updatedUnlocked,
            playerX = 150f,
            playerY = 150f
        )
        saveProgress(updated)
        SoundEffectsManager.playCured()
    }

    // ==========================================
    // BATTLE SUB-ENGINE IMPLEMENTATION
    // ==========================================

    fun startWildBattle() {
        val biom = _playerProgress.value?.currentBiome ?: "FOREST"
        val playerLevel = _team.value.firstOrNull()?.level ?: 5
        viewModelScope.launch(Dispatchers.Default) {
            val wild = CreatureDatabase.getRandomSpawn(biom, playerLevel, _currentSlot.value)
            val pActive = _team.value.firstOrNull { it.hp > 0 } ?: _team.value.first()
            _battleState.value = BattleState(
                playerCreature = pActive,
                enemyCreature = wild,
                isPlayerTurn = pActive.speed >= wild.speed,
                battleLogs = listOf("A wild ${wild.name} (Lv. ${wild.level}) emerged from tall grass!"),
                isFinished = false,
                isBoss = wild.rarity == "LEGENDARY"
            )
            _gameState.value = GameState.BATTLE
        }
    }

    fun startBossRaid(speciesId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val boss = CreatureDatabase.createCreature(speciesId, 35, _currentSlot.value).copy(
                maxHp = 300, hp = 300, attack = 35, defense = 22, name = "👿 RAID BOSS: ${speciesId.uppercase()}"
            )
            val pActive = _team.value.firstOrNull { it.hp > 0 } ?: _team.value.first()
            _battleState.value = BattleState(
                playerCreature = pActive,
                enemyCreature = boss,
                isPlayerTurn = pActive.speed >= boss.speed,
                battleLogs = listOf("Warning! Titan Raid Boss ${boss.name} has disrupted the biome!"),
                isFinished = false,
                isBoss = true
            )
            _gameState.value = GameState.BATTLE
        }
    }

    fun triggerBattleFlee() {
        val b = _battleState.value ?: return
        _battleState.value = b.copy(
            battleLogs = b.battleLogs + "You successfully fled the encounter!",
            isFinished = true,
            resultMessage = "FLEED"
        )
        SoundEffectsManager.playSelected()
        viewModelScope.launch {
            delay(1500)
            _gameState.value = GameState.WORLD
            _battleState.value = null
        }
    }

    fun executePlayerAttack(moveId: String) {
        val b = _battleState.value ?: return
        if (!b.isPlayerTurn || b.isFinished || b.isCapturing) return

        val attacker = b.playerCreature ?: return
        val defender = b.enemyCreature ?: return
        val move = CreatureDatabase.moves[moveId] ?: CreatureDatabase.moves["tackle"]!!

        viewModelScope.launch {
            // 1. Calculate Damage containing elemental multipliers
            val rawDmg = (attacker.attack.toFloat() / defender.defense.toFloat() * move.power * 0.4f) + 2f
            val isWeak = isWeakTo(move.type, defender.type)
            val isStrong = isStrongAgainst(move.type, defender.type)
            val mult = if (isStrong) 1.5f else if (isWeak) 0.6f else 1.0f

            val crits = if (Random.nextFloat() < 0.12f) 1.5f else 1.0f // 12% critical hits option
            val finalDmg = (rawDmg * mult * crits).toInt().coerceAtLeast(1)

            val updatedDhp = (defender.hp - finalDmg).coerceAtLeast(0)
            val updatedEnemy = defender.copy(hp = updatedDhp)

            // Dynamic battle logs
            val elementLog = if (isStrong) " It's super effective! 🔥" else if (isWeak) " It was not very effective... 💧" else ""
            val critLog = if (crits > 1f) " Critical Strike! ⚡" else ""
            val moveLog = "${attacker.name} utilized ${move.name}! Dealt $finalDmg damage!$elementLog$critLog"

            SoundEffectsManager.playHit()
            _battleState.value = b.copy(
                enemyCreature = updatedEnemy,
                cameraShake = true,
                battleLogs = b.battleLogs + moveLog,
                isPlayerTurn = false
            )

            // Toggle camera shake off soon
            delay(300)
            val bCurrent = _battleState.value ?: return@launch
            _battleState.value = bCurrent.copy(cameraShake = false)

            // Check if enemy fainted
            if (updatedDhp <= 0) {
                handleBattleVictory(updatedEnemy)
            } else {
                delay(1000)
                executeEnemyAITurn()
            }
        }
    }

    private fun executeEnemyAITurn() {
        val b = _battleState.value ?: return
        if (b.isFinished) return

        val attacker = b.enemyCreature ?: return
        val defender = b.playerCreature ?: return

        viewModelScope.launch {
            // Tactical Select: AI queries available moves and picks the strongest versus player type
            val moveIds = attacker.moves.split(",")
            val bestMoveId = moveIds.maxByOrNull { mId ->
                val move = CreatureDatabase.moves[mId] ?: return@maxByOrNull 0
                if (isStrongAgainst(move.type, defender.type)) 10 else if (isWeakTo(move.type, defender.type)) 1 else 5
            } ?: "tackle"

            val move = CreatureDatabase.moves[bestMoveId] ?: CreatureDatabase.moves["tackle"]!!

            val rawDmg = (attacker.attack.toFloat() / defender.defense.toFloat() * move.power * 0.4f) + 2f
            val isWeak = isWeakTo(move.type, defender.type)
            val isStrong = isStrongAgainst(move.type, defender.type)
            val mult = if (isStrong) 1.5f else if (isWeak) 0.6f else 1.0f
            val finalDmg = (rawDmg * mult).toInt().coerceAtLeast(1)

            val updatedPhp = (defender.hp - finalDmg).coerceAtLeast(0)
            val updatedPlayer = defender.copy(hp = updatedPhp)

            val elementLog = if (isStrong) " Extremely dangerous! 💥" else ""
            val moveLog = "Opponent ${attacker.name} countered with ${move.name}! Dealt $finalDmg damage!$elementLog"

            // Update in SQLite directly to persist squad health updates
            repository.updateCreature(updatedPlayer)
            SoundEffectsManager.playHit()

            _battleState.value = b.copy(
                playerCreature = updatedPlayer,
                cameraShake = true,
                battleLogs = b.battleLogs + moveLog,
                isPlayerTurn = true
            )

            delay(300)
            val bCurrent = _battleState.value ?: return@launch
            _battleState.value = bCurrent.copy(cameraShake = false)

            if (updatedPhp <= 0) {
                // Try to find next alive squad member
                val nextAlive = _team.value.firstOrNull { it.hp > 0 && it.instanceId != updatedPlayer.instanceId }
                if (nextAlive != null) {
                    delay(800)
                    _battleState.value = _battleState.value?.copy(
                        playerCreature = nextAlive,
                        battleLogs = _battleState.value!!.battleLogs + "${updatedPlayer.name} fainted! Sent in ${nextAlive.name}!"
                    )
                } else {
                    handleBattleLoss()
                }
            }
        }
    }

    private fun handleBattleVictory(defeatedEnemy: CreatureEntity) {
        val b = _battleState.value ?: return
        val active = b.playerCreature ?: return

        viewModelScope.launch(Dispatchers.IO) {
            // Reward: Gold & XP
            val xpGain = defeatedEnemy.level * 15 + (if (b.isBoss) 300 else 0)
            val coinsReward = defeatedEnemy.level * 25 + (if (b.isBoss) 400 else 0)

            val xpNeeded = active.level * 100
            var newXp = active.xp + xpGain
            var newLevel = active.level
            var leveledUp = false

            if (newXp >= xpNeeded) {
                newLevel++
                newXp -= xpNeeded
                leveledUp = true
            }

            // Stat upgrades upon level up
            val baseSpec = CreatureDatabase.getSpecies(active.speciesId)!!
            val mult = 1.0f + (newLevel - 1) * 0.12f
            val finalHp = (baseSpec.baseHp * mult).toInt()
            val finalAtk = (baseSpec.baseAttack * mult).toInt()
            val finalDef = (baseSpec.baseDefense * mult).toInt()
            val finalSpd = (baseSpec.baseSpeed * mult).toInt()

            var updated = active.copy(
                level = newLevel,
                xp = newXp,
                maxHp = finalHp,
                hp = finalHp, // Heal to full on level up
                attack = finalAtk,
                defense = finalDef,
                speed = finalSpd
            )

            // Auto-Evolve Check
            if (baseSpec.evolutionLevel in 1..newLevel && baseSpec.evolvesTo != null) {
                val nextSpec = CreatureDatabase.getSpecies(baseSpec.evolvesTo)!!
                updated = updated.copy(
                    speciesId = nextSpec.id,
                    name = if (updated.isShiny) "★ ${nextSpec.name}" else nextSpec.name,
                    type = nextSpec.type,
                    evolutionStage = updated.evolutionStage + 1
                )
                SoundEffectsManager.playLevelUp()
            }

            repository.updateCreature(updated)

            // Save player progress coins
            val currProg = _playerProgress.value
            if (currProg != null) {
                val updatedProg = currProg.copy(
                    coins = currProg.coins + coinsReward,
                    gems = currProg.gems + (if (b.isBoss) 5 else 1)
                )
                repository.savePlayerProgress(updatedProg)
                _playerProgress.value = updatedProg
            }

            val winLogs = mutableListOf<String>()
            winLogs.add("Victory! Opponent ${defeatedEnemy.name} fainted!")
            winLogs.add("Collected +$coinsReward Coins! Captured +$xpGain XP!")
            if (leveledUp) {
                SoundEffectsManager.playLevelUp()
                winLogs.add("🎉 SPECTACULAR! Your ${updated.name} reached Level $newLevel!")
            }

            launch(Dispatchers.Main) {
                _battleState.value = b.copy(
                    battleLogs = b.battleLogs + winLogs,
                    isFinished = true,
                    resultMessage = "WIN"
                )
                delay(2500)
                _gameState.value = GameState.WORLD
                _battleState.value = null
            }
        }
    }

    private fun handleBattleLoss() {
        val b = _battleState.value ?: return
        SoundEffectsManager.playCaptureFail()
        _battleState.value = b.copy(
            battleLogs = b.battleLogs + "All your creatures have fainted! Escaped back to Forest Haven...",
            isFinished = true,
            resultMessage = "LOSE"
        )
        viewModelScope.launch(Dispatchers.IO) {
            // Fully heal whole team back in hospital so players don't get softlocked
            val list = repository.getCreaturesSync(_currentSlot.value)
            list.forEach {
                repository.updateCreature(it.copy(hp = it.maxHp))
            }

            // Deduct some gold as safety penalty
            val progress = _playerProgress.value
            if (progress != null) {
                val penal = (progress.coins * 0.15f).toInt()
                repository.savePlayerProgress(progress.copy(
                    coins = (progress.coins - penal).coerceAtLeast(0),
                    currentBiome = "FOREST" // Return safely to home haven
                ))
            }

            delay(3000)
            launch(Dispatchers.Main) {
                _gameState.value = GameState.WORLD
                _battleState.value = null
            }
        }
    }

    fun usePotionInBattle() {
        val b = _battleState.value ?: return
        if (b.isFinished || b.isCapturing) return

        val qty = _inventory.value["potion"] ?: 0
        if (qty <= 0) {
            _battleState.value = b.copy(battleLogs = b.battleLogs + "No health potions remaining!")
            return
        }

        val active = b.playerCreature ?: return
        val healHp = (active.hp + 50).coerceAtMost(active.maxHp)
        val updatedActive = active.copy(hp = healHp)

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCreature(updatedActive)

            // Consume potion
            val mutableInv = _inventory.value.toMutableMap()
            mutableInv["potion"] = qty - 1
            _inventory.value = mutableInv

            val progress = _playerProgress.value
            if (progress != null) {
                repository.savePlayerProgress(progress.copy(serializedInventory = serializeInventory(mutableInv)))
                _playerProgress.value = progress.copy(serializedInventory = serializeInventory(mutableInv))
            }

            SoundEffectsManager.playCured()

            launch(Dispatchers.Main) {
                _battleState.value = b.copy(
                    playerCreature = updatedActive,
                    battleLogs = b.battleLogs + "Utilized Health Potion! Restored +50 HP to ${active.name}!",
                    isPlayerTurn = false
                )
                delay(1200)
                executeEnemyAITurn()
            }
        }
    }

    fun attemptCaptureInBattle() {
        val b = _battleState.value ?: return
        if (b.isFinished || b.isCapturing) return

        val qty = _inventory.value["orb"] ?: 0
        if (qty <= 0) {
            _battleState.value = b.copy(battleLogs = b.battleLogs + "Out of Capture Orbs!")
            return
        }

        val enemy = b.enemyCreature ?: return
        if (b.isBoss) {
            _battleState.value = b.copy(battleLogs = b.battleLogs + "Raid bosses represent ultimate legends and CANNOT be captured!")
            return
        }

        viewModelScope.launch {
            // Consume Orb
            val mutableInv = _inventory.value.toMutableMap()
            mutableInv["orb"] = qty - 1
            _inventory.value = mutableInv

            val progress = _playerProgress.value
            if (progress != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.savePlayerProgress(progress.copy(serializedInventory = serializeInventory(mutableInv)))
                }
                _playerProgress.value = progress.copy(serializedInventory = serializeInventory(mutableInv))
            }

            // Start rolling visuals
            _battleState.value = b.copy(
                isCapturing = true,
                captureRollState = 1,
                battleLogs = b.battleLogs + "Launched Mythic Orb... Please wait!"
            )

            // Capture Formula Math: lower HP, higher rates
            val hpRatio = enemy.hp.toFloat() / enemy.maxHp.toFloat()
            val baseRate = when (enemy.rarity) {
                "COMMON" -> 0.70f
                "RARE" -> 0.40f
                "EPIC" -> 0.22f
                else -> 0.10f
            }
            // Capture boost on low HP
            val finalCaptureChance = baseRate + (1f - hpRatio) * 0.35f
            val captureSuccess = Random.nextFloat() <= finalCaptureChance

            // Roll 3 times
            for (roll in 1..3) {
                delay(800)
                SoundEffectsManager.playSelected()
                _battleState.value = _battleState.value?.copy(captureRollState = roll)
            }

            delay(600)

            if (captureSuccess) {
                SoundEffectsManager.playCaptureSuccess()
                _battleState.value = _battleState.value?.copy(
                    captureRollState = 4,
                    battleLogs = _battleState.value!!.battleLogs + "CLICK! Successfully captured ${enemy.name}! Added to box!"
                )

                // Save captured creature as BOX Storage
                val capturedSave = enemy.copy(
                    slotId = _currentSlot.value,
                    teamOrder = -1 // Add directly to boxes
                )
                viewModelScope.launch(Dispatchers.IO) {
                    repository.addCreature(capturedSave)
                }

                delay(2000)
                _gameState.value = GameState.WORLD
                _battleState.value = null
            } else {
                SoundEffectsManager.playCaptureFail()
                _battleState.value = _battleState.value?.copy(
                    isCapturing = false,
                    captureRollState = 5,
                    battleLogs = _battleState.value!!.battleLogs + "Oh no! ${enemy.name} broke free!",
                    isPlayerTurn = false
                )
                delay(1200)
                executeEnemyAITurn()
            }
        }
    }

    // Purchase items from items shop using collected coins
    fun buyShopItem(item: String, cost: Int) {
        val prog = _playerProgress.value ?: return
        if (prog.coins < cost) {
            SoundEffectsManager.playCaptureFail()
            return
        }

        val mutableInv = _inventory.value.toMutableMap()
        mutableInv[item] = (mutableInv[item] ?: 0) + 1
        _inventory.value = mutableInv

        val updatedProg = prog.copy(
            coins = prog.coins - cost,
            serializedInventory = serializeInventory(mutableInv)
        )
        saveProgress(updatedProg)
        SoundEffectsManager.playCured()
    }

    // Creature Management Systems: Level up and manage active squads
    fun feedExpCandy(c: CreatureEntity) {
        val prog = _playerProgress.value ?: return
        // Level up costs coins
        val cost = c.level * 45
        if (prog.coins < cost) {
            SoundEffectsManager.playCaptureFail()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val baseSpec = CreatureDatabase.getSpecies(c.speciesId)!!
            val nextLv = c.level + 1
            val mult = 1.0f + (nextLv - 1) * 0.12f

            var updated = c.copy(
                level = nextLv,
                xp = 0,
                maxHp = (baseSpec.baseHp * mult).toInt(),
                hp = (baseSpec.baseHp * mult).toInt(),
                attack = (baseSpec.baseAttack * mult).toInt(),
                defense = (baseSpec.baseDefense * mult).toInt(),
                speed = (baseSpec.baseSpeed * mult).toInt()
            )

            // Check dynamic evolution
            if (baseSpec.evolutionLevel in 1..nextLv && baseSpec.evolvesTo != null) {
                val nextSpec = CreatureDatabase.getSpecies(baseSpec.evolvesTo)!!
                updated = updated.copy(
                    speciesId = nextSpec.id,
                    name = if (updated.isShiny) "★ ${nextSpec.name}" else nextSpec.name,
                    type = nextSpec.type,
                    evolutionStage = updated.evolutionStage + 1
                )
                SoundEffectsManager.playLevelUp()
            }

            repository.updateCreature(updated)

            val updatedProg = prog.copy(coins = prog.coins - cost)
            repository.savePlayerProgress(updatedProg)
            _playerProgress.value = updatedProg

            SoundEffectsManager.playLevelUp()
        }
    }

    // Swap creature from box to team or team to box
    fun changeTeamStatus(c: CreatureEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val isCurrentlyInTeam = c.teamOrder >= 0
            val currentTeam = repository.getTeamSync(_currentSlot.value)

            if (isCurrentlyInTeam) {
                // Remove from team, make teamOrder -1
                val updated = c.copy(teamOrder = -1)
                repository.updateCreature(updated)

                // Re-sort remaining team orders safely
                val remainingSorted = currentTeam.filter { it.instanceId != c.instanceId }
                remainingSorted.forEachIndexed { index, creatureEntity ->
                    repository.updateCreature(creatureEntity.copy(teamOrder = index))
                }
            } else {
                // Add to team if limit (6) is not exceeded
                if (currentTeam.size >= 6) {
                    SoundEffectsManager.playCaptureFail()
                    return@launch
                }
                val updated = c.copy(teamOrder = currentTeam.size)
                repository.updateCreature(updated)
            }
            SoundEffectsManager.playCured()
        }
    }

    // Dynamic type advantages matrix
    private fun isStrongAgainst(atk: String, def: String): Boolean {
        return try {
            val element = MythicType.valueOf(atk)
            element.resistTypes.contains(def) // wait, resistTypes is actually what it's strong against!
        } catch (e: Exception) {
            false
        }
    }

    private fun isWeakTo(atk: String, def: String): Boolean {
        return try {
            val element = MythicType.valueOf(atk)
            element.weakTypes.contains(def)
        } catch (e: Exception) {
            false
        }
    }
}
