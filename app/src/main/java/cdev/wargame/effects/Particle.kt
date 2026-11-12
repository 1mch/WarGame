package cdev.wargame.effects

import cdev.wargame.core.Vector2

class Particle(
    var position: Vector2,
    var velocity: Vector2,
    val color: Int,
    val maxLifeMs: Float = 400f,
    val size: Float = 0.008f
) {
    var ageMs: Float = 0f
    val isDead: Boolean get() = ageMs >= maxLifeMs
    val alphaFraction: Float get() = (1f - (ageMs / maxLifeMs)).coerceIn(0f, 1f)

    fun update(dtMs: Float) {
        ageMs += dtMs
        val dtSec = dtMs / 1000f
        position = position.add(velocity.mul(dtSec))
        // slight deceleration
        velocity = velocity.mul(0.96f)
    }
}
