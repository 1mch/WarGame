package cdev.wargame.entities

import cdev.wargame.core.GameConfig
import cdev.wargame.core.Vector2
import kotlin.random.Random

class Unit(
    override val id: String,
    override var position: Vector2,
    override var ownerId: Int,
    var target: Base,
    var sourceBase: Base? = null
) : Entity(id, position, GameConfig.UNIT_RADIUS, ownerId) {

    val speed: Float = GameConfig.BASE_SPEED * (1f + (Random.nextFloat() * 2f - 1f) * GameConfig.SPEED_VARIATION)
    var velocity: Vector2 = run {
        val dir = target.position.sub(position).normalize()
        if (dir.magSq() == 0f) Vector2(speed, 0f) else dir.mul(speed)
    }

    var isDead: Boolean = false
    var hasLeftSourceBase: Boolean = false

    fun updatePosition(dtSeconds: Float) {
        if (isDead) return
        position = position.add(velocity.mul(dtSeconds))

        if (!hasLeftSourceBase && sourceBase != null) {
            val d = position.distanceTo(sourceBase!!.position)
            if (d > sourceBase!!.radius + radius + 2.5f) {
                hasLeftSourceBase = true
            }
        }
    }
}
