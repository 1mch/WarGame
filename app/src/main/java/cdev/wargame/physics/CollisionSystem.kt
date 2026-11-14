package cdev.wargame.physics

import cdev.wargame.audio.SoundManager
import cdev.wargame.core.GameConfig
import cdev.wargame.effects.ParticleSystem
import cdev.wargame.entities.Base
import cdev.wargame.entities.Unit
import kotlin.random.Random

class CollisionSystem(
    private val particleSystem: ParticleSystem,
    private val soundManager: SoundManager
) {
    fun handleCollisions(
        units: MutableList<Unit>,
        bases: List<Base>
    ) {
        // 1. Unit vs Unit collisions (clash between hostile units)
        val unitCount = units.size
        val clashRadius = GameConfig.UNIT_RADIUS * 2f

        for (i in 0 until unitCount) {
            val u1 = units[i]
            if (u1.isDead) continue

            for (j in i + 1 until unitCount) {
                val u2 = units[j]
                if (u2.isDead) continue

                // Only clash if different owners
                if (u1.ownerId != u2.ownerId) {
                    val dist = u1.position.distanceTo(u2.position)
                    if (dist <= clashRadius) {
                        val midPoint = u1.position.add(u2.position).mul(0.5f)
                        val f1 = GameConfig.getFaction(u1.ownerId)
                        val f2 = GameConfig.getFaction(u2.ownerId)

                        particleSystem.spawnClashSparks(midPoint, f1.primaryColor, f2.primaryColor)
                        soundManager.playUnitClash()

                        // 90% chance both die, else one survives randomly
                        if (Random.nextFloat() < GameConfig.CLASH_DESTROY_CHANCE) {
                            u1.isDead = true
                            u2.isDead = true
                        } else {
                            if (Random.nextBoolean()) {
                                u1.isDead = true
                            } else {
                                u2.isDead = true
                            }
                        }

                        if (u1.isDead) break
                    }
                }
            }
        }

        // 2. Unit vs Target Base collisions
        for (unit in units) {
            if (unit.isDead) continue
            val target = unit.target

            val dist = unit.position.distanceTo(target.position)
            if (dist <= target.radius) {
                unit.isDead = true

                if (target.ownerId == unit.ownerId) {
                    // Reinforcing friendly base
                    target.unitCount++
                    soundManager.playBaseReinforce()
                    particleSystem.spawnBaseHit(unit.position, GameConfig.getFaction(target.ownerId).glowColor)
                } else {
                    // Attacking foreign / neutral base:
                    // Garrison goes all the way down to 0 first;
                    // the base is captured only when another attacking unit enters it at 0.
                    target.unitCount--
                    if (target.unitCount < 0) {
                        val prevOwner = target.ownerId
                        target.ownerId = unit.ownerId
                        target.unitCount = 1
                        target.targetBase = null
                        target.sendingUnitsCount = 0
                        target.resetRegenTimer()

                        val newFaction = GameConfig.getFaction(target.ownerId)
                        particleSystem.spawnCaptureRipple(target.position, newFaction.primaryColor, target.radius)

                        if (unit.ownerId == 1) {
                            soundManager.playBaseCaptured()
                        } else if (prevOwner == 1) {
                            soundManager.playBaseLost()
                        } else {
                            soundManager.playBaseCaptured()
                        }
                    } else {
                        val hitFaction = GameConfig.getFaction(target.ownerId)
                        particleSystem.spawnBaseHit(unit.position, hitFaction.primaryColor)
                        soundManager.playBaseHit()
                    }
                }
            }
        }

        // Remove dead units
        units.removeAll { it.isDead }
    }
}
