package cdev.wargame.entities

import cdev.wargame.core.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Obstacle(
    override val id: String,
    override var position: Vector2,
    override val radius: Float = 13f,
    val type: String = "rock"
) : Entity(id, position, radius, 0) {

    // Irregular polygon vertices for asteroid / rock rendering
    val relativeVertices: List<Vector2>

    init {
        val count = 8
        val rand = Random(id.hashCode())
        val list = mutableListOf<Vector2>()
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2f * PI.toFloat()
            val variance = 0.75f + rand.nextFloat() * 0.45f // radius variation
            val r = radius * variance
            list.add(Vector2(cos(angle) * r, sin(angle) * r))
        }
        relativeVertices = list
    }
}
