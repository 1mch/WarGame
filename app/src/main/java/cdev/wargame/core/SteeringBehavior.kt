package cdev.wargame.core

import cdev.wargame.entities.Base
import cdev.wargame.entities.Obstacle
import cdev.wargame.entities.Unit
import kotlin.math.max
import kotlin.math.min

object SteeringBehavior {
    fun calculateSteering(
        unit: Unit,
        allUnits: List<Unit>,
        bases: List<Base>,
        obstacles: List<Obstacle>,
        dtSeconds: Float
    ): Vector2 {
        val toTarget = unit.target.position.sub(unit.position)
        if (toTarget.magSq() == 0f) return Vector2(0f, 0f)
        val targetDir = toTarget.normalize()

        // 1. Separation from friendly units
        var separation = Vector2(0f, 0f)
        var sepCount = 0
        for (other in allUnits) {
            if (other === unit || other.isDead || other.ownerId != unit.ownerId) continue
            val d = unit.position.distanceTo(other.position)
            if (d in 0.0001f..GameConfig.SEPARATION_RADIUS) {
                val diff = unit.position.sub(other.position).normalize()
                val factor = 1f - (d / GameConfig.SEPARATION_RADIUS)
                separation = separation.add(diff.mul(factor))
                sepCount++
            }
        }
        if (sepCount > 0) {
            separation = separation.div(sepCount.toFloat()).mul(GameConfig.SEPARATION_WEIGHT)
        }

        // 2. Repulsion and tangential flow around non-target bases
        var baseRepel = Vector2(0f, 0f)
        for (base in bases) {
            if (base === unit.target) continue
            if (base === unit.sourceBase && !unit.hasLeftSourceBase) continue

            val toBase = base.position.sub(unit.position)
            val d = toBase.mag()
            val minDist = base.radius + unit.radius
            val avoidDist = minDist + GameConfig.BASE_AVOIDANCE_RADIUS

            if (d >= 0.0001f && d < avoidDist) {
                val fromBase = unit.position.sub(base.position).normalize()
                val ratio = max(0f, (avoidDist - d) / (avoidDist - minDist))
                val intensity = ratio * ratio

                val radial = fromBase.mul(intensity * GameConfig.BASE_REPEL_WEIGHT)
                val cross = unit.velocity.x * toBase.y - unit.velocity.y * toBase.x
                val tangent = if (cross >= 0f) Vector2(-fromBase.y, fromBase.x) else Vector2(fromBase.y, -fromBase.x)
                val tangentForce = tangent.mul(intensity * GameConfig.BASE_TANGENT_WEIGHT)

                baseRepel = baseRepel.add(radial).add(tangentForce)
            }
        }

        // 3. Repulsion and tangential flow around obstacles
        var obstacleRepel = Vector2(0f, 0f)
        for (obs in obstacles) {
            val toObs = obs.position.sub(unit.position)
            val d = toObs.mag()
            val minDist = obs.radius + unit.radius
            val avoidDist = minDist + GameConfig.OBSTACLE_AVOIDANCE_RADIUS

            if (d >= 0.0001f && d < avoidDist) {
                val fromObs = unit.position.sub(obs.position).normalize()
                val ratio = max(0f, (avoidDist - d) / (avoidDist - minDist))
                val intensity = ratio * ratio

                val radial = fromObs.mul(intensity * GameConfig.OBSTACLE_REPEL_WEIGHT)
                val cross = unit.velocity.x * toObs.y - unit.velocity.y * toObs.x
                val tangent = if (cross >= 0f) Vector2(-fromObs.y, fromObs.x) else Vector2(fromObs.y, -fromObs.x)
                val tangentForce = tangent.mul(intensity * GameConfig.OBSTACLE_TANGENT_WEIGHT)

                obstacleRepel = obstacleRepel.add(radial).add(tangentForce)
            }
        }

        // 4. Desired steering vector
        var desiredDir = targetDir.mul(GameConfig.SEEK_WEIGHT)
            .add(separation)
            .add(baseRepel)
            .add(obstacleRepel)

        if (desiredDir.magSq() == 0f) desiredDir = targetDir

        val desiredVelocity = desiredDir.normalize().mul(unit.speed)
        val steer = desiredVelocity.sub(unit.velocity)
        return steer.mul(min(1f, dtSeconds * 16f))
    }

    fun applySteering(
        unit: Unit,
        allUnits: List<Unit>,
        bases: List<Base>,
        obstacles: List<Obstacle>,
        dtSeconds: Float
    ) {
        val steer = calculateSteering(unit, allUnits, bases, obstacles, dtSeconds)
        unit.velocity = unit.velocity.add(steer).normalize().mul(unit.speed)

        // Hard boundary collision correction for non-target bases
        for (base in bases) {
            if (base === unit.target) continue
            if (base === unit.sourceBase && !unit.hasLeftSourceBase) continue

            val d = unit.position.distanceTo(base.position)
            val minDist = base.radius + unit.radius
            if (d < minDist && d > 0.0001f) {
                val pushDir = unit.position.sub(base.position).normalize()
                unit.position = base.position.add(pushDir.mul(minDist))
            }
        }

        // Hard boundary collision correction for obstacles
        for (obs in obstacles) {
            val d = unit.position.distanceTo(obs.position)
            val minDist = obs.radius + unit.radius
            if (d < minDist && d > 0.0001f) {
                val pushDir = unit.position.sub(obs.position).normalize()
                unit.position = obs.position.add(pushDir.mul(minDist))
            }
        }
    }
}
