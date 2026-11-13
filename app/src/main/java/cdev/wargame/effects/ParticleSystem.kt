package cdev.wargame.effects

import cdev.wargame.core.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleSystem {
    val particles = mutableListOf<Particle>()
    val ripples = mutableListOf<Ripple>()

    fun update(dtMs: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.update(dtMs)
            if (p.isDead) pIter.remove()
        }

        val rIter = ripples.iterator()
        while (rIter.hasNext()) {
            val r = rIter.next()
            r.update(dtMs)
            if (r.isDead) rIter.remove()
        }
    }

    fun spawnClashSparks(pos: Vector2, color1: Int, color2: Int, count: Int = 8) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 35f + Random.nextFloat() * 70f
            val vel = Vector2(cos(angle) * speed, sin(angle) * speed)
            val color = if (i % 2 == 0) color1 else color2
            particles.add(
                Particle(
                    position = pos.copy(),
                    velocity = vel,
                    color = color,
                    maxLifeMs = 250f + Random.nextFloat() * 200f,
                    size = 1.2f + Random.nextFloat() * 0.9f
                )
            )
        }
    }

    fun spawnCaptureRipple(pos: Vector2, color: Int, baseRadius: Float) {
        ripples.add(
            Ripple(
                position = pos.copy(),
                color = color,
                initialRadius = baseRadius * 0.8f,
                targetRadius = baseRadius * 2.2f,
                durationMs = 500f
            )
        )
        // Also add small burst of particles
        for (i in 0 until 14) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 26f + Random.nextFloat() * 61f
            val vel = Vector2(cos(angle) * speed, sin(angle) * speed)
            particles.add(
                Particle(
                    position = pos.copy(),
                    velocity = vel,
                    color = color,
                    maxLifeMs = 350f + Random.nextFloat() * 250f,
                    size = 1.4f
                )
            )
        }
    }

    fun spawnBaseHit(pos: Vector2, color: Int) {
        for (i in 0 until 4) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 18f + Random.nextFloat() * 35f
            val vel = Vector2(cos(angle) * speed, sin(angle) * speed)
            particles.add(
                Particle(
                    position = pos.copy(),
                    velocity = vel,
                    color = color,
                    maxLifeMs = 200f,
                    size = 1.1f
                )
            )
        }
    }

    fun clear() {
        particles.clear()
        ripples.clear()
    }
}
