package cdev.wargame.ui

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import cdev.wargame.core.GameEngine
import cdev.wargame.core.GameStatus
import cdev.wargame.core.ScreenState
import cdev.wargame.entities.Base
import kotlin.math.max

class GameInputHandler(
    private val engine: GameEngine,
    private val renderer: CanvasRenderer
) : View.OnTouchListener {

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        synchronized(engine.stateLock) {
            if (engine.screenState != ScreenState.PLAYING || engine.status != GameStatus.RUNNING || engine.isPaused) {
                return false
            }

            val lx = renderer.toLogicalX(event.x)
            val ly = renderer.toLogicalY(event.y)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val touchedBase = findBaseAtLogical(lx, ly)
                    if (touchedBase != null && touchedBase.ownerId == 1) {
                        engine.dragState.active = true
                        engine.dragState.startBases.clear()
                        engine.dragState.startBases.add(touchedBase)
                        engine.dragState.pointerX = lx
                        engine.dragState.pointerY = ly
                        engine.dragState.hoverBase = null
                        engine.soundManager.playDragSelect()
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (engine.dragState.active) {
                        engine.dragState.pointerX = lx
                        engine.dragState.pointerY = ly

                        val hover = findBaseAtLogical(lx, ly)
                        engine.dragState.hoverBase = hover

                        // Multi-drag: if dragging over another friendly base, add it to startBases
                        if (hover != null && hover.ownerId == 1 && !engine.dragState.startBases.contains(hover)) {
                            engine.dragState.startBases.add(hover)
                            engine.soundManager.playDragSelect()
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (engine.dragState.active) {
                        val target = engine.dragState.hoverBase
                        if (target != null) {
                            var dispatchedAny = false
                            for (startBase in engine.dragState.startBases) {
                                if (startBase !== target && startBase.unitCount > 0) {
                                    startBase.sendUnitsTo(target, ratio = 1.0f)
                                    dispatchedAny = true
                                }
                            }
                            if (dispatchedAny) {
                                engine.soundManager.playUnitsDispatched()
                                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            }
                        }
                        engine.dragState.reset()
                    }
                }
            }
            return true
        }
    }

    private fun findBaseAtLogical(lx: Float, ly: Float): Base? {
        val minScreenHitDist = 42f * renderer.s
        val minLogicalHitDist = minScreenHitDist / renderer.scale

        return engine.bases.minByOrNull { base ->
            val dist = Math.hypot((base.position.x - lx).toDouble(), (base.position.y - ly).toDouble()).toFloat()
            val effectiveRadius = max(base.radius, minLogicalHitDist)
            if (dist <= effectiveRadius) dist else Float.MAX_VALUE
        }?.takeIf { base ->
            val dist = Math.hypot((base.position.x - lx).toDouble(), (base.position.y - ly).toDouble()).toFloat()
            val effectiveRadius = max(base.radius, minLogicalHitDist)
            dist <= effectiveRadius
        }
    }
}
