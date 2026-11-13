package cdev.wargame.ai

import cdev.wargame.core.GameConfig
import cdev.wargame.entities.Base
import kotlin.random.Random

class AiAgent(val ownerId: Int) {

    private var decisionTimer: Float = 0f
    // Random initial delay (0.5s to 2.5s) to desynchronize multiple AI factions
    private var nextDecisionInterval: Float = 2000f + Random.nextFloat() * 1000f

    init {
        decisionTimer = Random.nextFloat() * 1500f
    }

    fun update(dtMs: Float, allBases: List<Base>) {
        decisionTimer += dtMs
        if (decisionTimer < nextDecisionInterval) return

        decisionTimer = 0f
        nextDecisionInterval = 2000f + Random.nextFloat() * 1000f

        // 1. Get owned bases
        val myBases = allBases.filter { it.ownerId == ownerId }
        if (myBases.isEmpty()) return // AI has no bases left

        // 2. Get foreign bases
        val foreignBases = allBases.filter { it.ownerId != ownerId }
        if (foreignBases.isEmpty()) return // Already won everything

        // 3. Find strongest base
        val strongestBase = myBases.maxByOrNull { it.unitCount } ?: return

        // 4. Only attack if strongest base has at least 8 units
        if (strongestBase.unitCount < 8) return

        // 5. Tactical target selection (Vulnerability Scoring)
        val targetBase = foreignBases.minByOrNull { base ->
            val dist = strongestBase.position.distanceTo(base.position)
            base.unitCount * 1.5f + (dist / GameConfig.LOGICAL_HALF_WIDTH) * 20f
        } ?: return

        // 6. Send 50% of garrison to keep defense
        strongestBase.sendUnitsTo(targetBase, ratio = 0.5f)
    }
}
