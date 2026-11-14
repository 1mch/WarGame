package cdev.wargame.core

import android.content.Context
import cdev.wargame.ai.AiAgent
import cdev.wargame.audio.SoundManager
import cdev.wargame.effects.ParticleSystem
import cdev.wargame.entities.Base
import cdev.wargame.entities.Obstacle
import cdev.wargame.entities.Unit
import cdev.wargame.levels.LevelData
import cdev.wargame.levels.LevelLoader
import cdev.wargame.physics.CollisionSystem

enum class GameStatus {
    RUNNING,
    VICTORY,
    DEFEAT
}

enum class ScreenState {
    MAIN_MENU,
    LEVEL_SELECT,
    PLAYING
}

class DragState {
    var active: Boolean = false
    val startBases: MutableList<Base> = mutableListOf()
    var hoverBase: Base? = null
    var pointerX: Float = 0f
    var pointerY: Float = 0f

    fun reset() {
        active = false
        startBases.clear()
        hoverBase = null
    }
}

class GameEngine(
    val soundManager: SoundManager = SoundManager(),
    val particleSystem: ParticleSystem = ParticleSystem()
) {
    val stateLock = Any()
    val collisionSystem = CollisionSystem(particleSystem, soundManager)

    @Volatile var screenState: ScreenState = ScreenState.MAIN_MENU
    var activeMenuTab: Int = 0 // 0 = MISSION, 1 = TECH, 2 = ARMORY/PROFILE
    var isSettingsModalOpen: Boolean = false
    var campaignManager: CampaignManager? = null

    val bases = mutableListOf<Base>()
    val units = mutableListOf<Unit>()
    val obstacles = mutableListOf<Obstacle>()
    val aiAgents = mutableListOf<AiAgent>()

    val dragState = DragState()

    @Volatile var status: GameStatus = GameStatus.RUNNING
    @Volatile var isPaused: Boolean = false
    @Volatile var currentLevelIndex: Int = 1
    @Volatile var levelName: String = "Operation"

    var screenWidth: Int = 1080
    var screenHeight: Int = 1920
    var aspect: Float = 9f / 16f
    var isPortrait: Boolean = true

    var onLevelCompleted: ((Int) -> kotlin.Unit)? = null

    fun setupScreenBounds(width: Int, height: Int) {
        synchronized(stateLock) {
            screenWidth = width
            screenHeight = height
            aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
            isPortrait = width < height
        }
    }

    fun loadLevel(levelIndex: Int, context: Context, startPlaying: Boolean = true) {
        val levelData: LevelData = LevelLoader.loadLevel(context, levelIndex)

        synchronized(stateLock) {
            currentLevelIndex = levelIndex
            levelName = levelData.name

            bases.clear()
            units.clear()
            obstacles.clear()
            aiAgents.clear()
            particleSystem.clear()
            dragState.reset()
            status = GameStatus.RUNNING
            isPaused = false
            isSettingsModalOpen = false

            if (startPlaying) {
                screenState = ScreenState.PLAYING
            }

            // Instantiate bases
            for (b in levelData.bases) {
                val type = cdev.wargame.entities.BaseType.fromString(b.type)
                bases.add(
                    Base(
                        id = b.id,
                        position = Vector2(b.x, b.y),
                        ownerId = b.ownerId,
                        unitCount = b.initialUnits,
                        type = type
                    )
                )
            }

            // Instantiate obstacles
            for (o in levelData.obstacles) {
                obstacles.add(
                    Obstacle(
                        id = o.id,
                        position = Vector2(o.x, o.y),
                        radius = o.radius,
                        type = o.type
                    )
                )
            }

            // Instantiate AI agents for all enemy factions present
            val enemyOwnerIds = bases.map { it.ownerId }.filter { it > 1 }.distinct()
            for (ownerId in enemyOwnerIds) {
                aiAgents.add(AiAgent(ownerId))
            }
        }
    }

    fun restartLevel(context: Context) {
        loadLevel(currentLevelIndex, context, startPlaying = true)
    }

    fun startCampaign(context: Context) {
        val targetLvl = campaignManager?.currentLevel ?: 1
        loadLevel(targetLvl, context, startPlaying = true)
    }

    fun update(dtMs: Float) {
        var completedLvl: Int? = null

        synchronized(stateLock) {
            if (isPaused) return

            val dtSec = dtMs / 1000f

            // When in Main Menu or Level Select, background is handled by LivingBackgroundView
            if (screenState == ScreenState.MAIN_MENU || screenState == ScreenState.LEVEL_SELECT) {
                for (base in bases) {
                    base.pulseTimer += dtMs
                }
                particleSystem.update(dtMs)
                return
            }

            // 1. Update bases (regeneration & unit dispatching)
            val spawnedUnits = mutableListOf<Unit>()
            for (base in bases) {
                base.update(dtMs) { newUnit ->
                    spawnedUnits += newUnit
                }
            }
            if (spawnedUnits.isNotEmpty()) {
                units.addAll(spawnedUnits)
            }

            // 2. Update AI agents
            if (status == GameStatus.RUNNING) {
                for (ai in aiAgents) {
                    ai.update(dtMs, bases)
                }
            }

            // 3. Update units steering and positions
            for (unit in units) {
                if (!unit.isDead) {
                    SteeringBehavior.applySteering(unit, units, bases, obstacles, dtSec)
                    unit.updatePosition(dtSec)
                }
            }

            // 4. Handle collisions & arrivals
            collisionSystem.handleCollisions(units, bases)

            // 5. Update particle effects & ripples
            particleSystem.update(dtMs)

            // 6. Check Win/Loss conditions
            if (status == GameStatus.RUNNING) {
                val playerBases = bases.count { it.ownerId == 1 }
                val playerUnits = units.count { it.ownerId == 1 }
                val enemyBases = bases.count { it.ownerId > 1 }
                val enemyUnits = units.count { it.ownerId > 1 }

                if (playerBases == 0 && playerUnits == 0) {
                    status = GameStatus.DEFEAT
                    soundManager.playDefeat()
                    campaignManager?.recordDefeat(currentLevelIndex)
                } else if (enemyBases == 0 && enemyUnits == 0 && playerBases > 0) {
                    status = GameStatus.VICTORY
                    soundManager.playVictory()
                    completedLvl = currentLevelIndex
                }
            }
        }

        completedLvl?.let { lvl ->
            onLevelCompleted?.invoke(lvl)
        }
    }

    // Get distribution of power (units in bases + units in air) by faction
    fun getFactionPowerCounts(): Map<Int, Int> {
        synchronized(stateLock) {
            val counts = mutableMapOf<Int, Int>()
            for (b in bases) {
                counts[b.ownerId] = (counts[b.ownerId] ?: 0) + b.unitCount
            }
            for (u in units) {
                if (!u.isDead) {
                    counts[u.ownerId] = (counts[u.ownerId] ?: 0) + 1
                }
            }
            return counts
        }
    }
}
