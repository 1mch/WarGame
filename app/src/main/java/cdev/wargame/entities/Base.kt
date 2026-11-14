package cdev.wargame.entities

import cdev.wargame.core.GameConfig
import cdev.wargame.core.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class BaseType(val maxUnits: Int, val radius: Float, val regenRateMs: Long) {
    SMALL(25, 14f, 1200L),
    MEDIUM(50, 18f, 800L),
    LARGE(100, 23f, 500L);

    companion object {
        fun fromString(str: String?): BaseType {
            return when (str?.lowercase()) {
                "small" -> SMALL
                "large" -> LARGE
                else -> MEDIUM
            }
        }
    }
}

class Base(
    override val id: String,
    override var position: Vector2,
    override var ownerId: Int,
    var unitCount: Int,
    val type: BaseType = BaseType.MEDIUM
) : Entity(id, position, type.radius, ownerId) {

    val maxUnits: Int = type.maxUnits

    private var regenTimer: Float = 0f
    private var spawnTimer: Float = 0f

    var targetBase: Base? = null
    var sendingUnitsCount: Int = 0

    // Visual pulse effect when selected or attacked
    var pulseTimer: Float = 0f

    fun update(dtMs: Float, onSpawnUnit: (Unit) -> kotlin.Unit) {
        pulseTimer += dtMs

        // Unit regeneration (owned bases only)
        if (ownerId != 0) {
            regenTimer += dtMs
            if (regenTimer >= type.regenRateMs) {
                regenTimer = 0f
                if (unitCount < maxUnits) {
                    unitCount++
                }
            }
        }

        // Unit dispatching logic
        targetBase?.let { target ->
            if (sendingUnitsCount > 0) {
                spawnTimer += dtMs
                if (spawnTimer >= GameConfig.SPAWN_INTERVAL_MS) {
                    spawnTimer = 0f
                    if (unitCount > 0) {
                        unitCount--
                        sendingUnitsCount--

                        // Angular spread +- 30 degrees (PI / 6 radians, total PI / 3)
                        val toTarget = target.position.sub(position).normalize()
                        val angle = (Random.nextFloat() - 0.5f) * (Math.PI.toFloat() / 3f)
                        val cosA = cos(angle)
                        val sinA = sin(angle)
                        val spreadDir = Vector2(
                            toTarget.x * cosA - toTarget.y * sinA,
                            toTarget.x * sinA + toTarget.y * cosA
                        )
                        val spawnPos = position.add(spreadDir.mul(radius * 0.7f))

                        val newUnit = Unit(
                            id = "u_${System.currentTimeMillis()}_${Random.nextInt(10000)}",
                            position = spawnPos,
                            ownerId = ownerId,
                            target = target,
                            sourceBase = this
                        )
                        onSpawnUnit(newUnit)
                    }
                    if (sendingUnitsCount <= 0 || unitCount <= 0) {
                        sendingUnitsCount = 0
                        targetBase = null
                    }
                }
            } else {
                targetBase = null
            }
        }
    }

    fun resetRegenTimer() {
        regenTimer = 0f
    }

    fun sendUnitsTo(target: Base, ratio: Float = 1.0f) {
        if (unitCount <= 0 || target === this) return
        val amount = (unitCount * ratio).toInt().coerceAtLeast(1)
        targetBase = target
        sendingUnitsCount += amount
        spawnTimer = GameConfig.SPAWN_INTERVAL_MS.toFloat() // First unit leaves immediately
    }
}
