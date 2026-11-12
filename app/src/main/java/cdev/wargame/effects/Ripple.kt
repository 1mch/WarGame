package cdev.wargame.effects

import cdev.wargame.core.Vector2

class Ripple(
    val position: Vector2,
    val color: Int,
    val initialRadius: Float,
    val targetRadius: Float,
    val durationMs: Float = 600f
) {
    var ageMs: Float = 0f
    val isDead: Boolean get() = ageMs >= durationMs
    val progress: Float get() = (ageMs / durationMs).coerceIn(0f, 1f)
    val currentRadius: Float get() = initialRadius + (targetRadius - initialRadius) * progress
    val alphaFraction: Float get() = (1f - progress).coerceIn(0f, 1f)

    fun update(dtMs: Float) {
        ageMs += dtMs
    }
}
