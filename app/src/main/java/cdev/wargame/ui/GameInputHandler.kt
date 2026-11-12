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
    private val renderer: CanvasRenderer,
    private val onNextLevelRequested: (() -> Unit)? = null
) : View.OnTouchListener {

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        // 1. Handle Main Menu Screen Touches
        if (engine.screenState == ScreenState.MAIN_MENU) {
            if (event.action == MotionEvent.ACTION_UP) {
                val cm = engine.campaignManager

                // Settings Modal Open
                if (engine.isSettingsModalOpen) {
                    if (renderer.settingsLangBtnRect.contains(touchX, touchY)) {
                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        engine.soundManager.playDragSelect()
                        if (cm != null) {
                            cm.language = if (cm.language == "SK") "EN" else "SK"
                        }
                        return true
                    }
                    if (renderer.settingsSoundBtnRect.contains(touchX, touchY)) {
                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        engine.soundManager.isMuted = !engine.soundManager.isMuted
                        if (!engine.soundManager.isMuted) {
                            engine.soundManager.playDragSelect()
                        }
                        return true
                    }
                    if (renderer.settingsResetBtnRect.contains(touchX, touchY)) {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        engine.soundManager.playBaseLost()
                        cm?.resetProgress()
                        engine.isSettingsModalOpen = false
                        return true
                    }
                    if (renderer.settingsCloseBtnRect.contains(touchX, touchY)) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        engine.soundManager.playDragSelect()
                        engine.isSettingsModalOpen = false
                        return true
                    }
                    // Tap outside modal closes it
                    engine.isSettingsModalOpen = false
                    return true
                }

                // Top-Right Settings Gear Button
                if (renderer.menuSettingsBtnRect.contains(touchX, touchY)) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.isSettingsModalOpen = true
                    return true
                }

                // Bottom Navigation Tabs (0 = MISSION, 1 = TECH, 2 = ARMORY)
                for (i in 0 until 3) {
                    if (renderer.bottomTabRects[i].contains(touchX, touchY)) {
                        if (engine.activeMenuTab != i) {
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            engine.soundManager.playDragSelect()
                            engine.activeMenuTab = i
                        }
                        return true
                    }
                }

                // Tab-specific interactions
                when (engine.activeMenuTab) {
                    0 -> {
                        // Launch Mission button
                        if (renderer.menuPlayBtnRect.contains(touchX, touchY)) {
                            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            engine.soundManager.playUnitsDispatched()
                            engine.startCampaign(v.context)
                            return true
                        }
                        // Last Sector Pill -> opens Level Select
                        if (renderer.menuLevelsBtnRect.contains(touchX, touchY)) {
                            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            engine.soundManager.playDragSelect()
                            engine.screenState = ScreenState.LEVEL_SELECT
                            return true
                        }
                    }
                    1 -> {
                        // Tech Lab Upgrades
                        if (cm != null) {
                            for (i in 0 until 4) {
                                if (renderer.techUpgradeBtnRects[i].contains(touchX, touchY)) {
                                    val curLvl = when (i) {
                                        0 -> cm.techSpeedLevel
                                        1 -> cm.techRegenLevel
                                        2 -> cm.techShieldLevel
                                        else -> cm.techCapacityLevel
                                    }
                                    val cost = curLvl * 250
                                    if (curLvl < 10 && cm.credits >= cost) {
                                        cm.credits -= cost
                                        when (i) {
                                            0 -> cm.techSpeedLevel = curLvl + 1
                                            1 -> cm.techRegenLevel = curLvl + 1
                                            2 -> cm.techShieldLevel = curLvl + 1
                                            else -> cm.techCapacityLevel = curLvl + 1
                                        }
                                        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        engine.soundManager.playBaseCaptured()
                                    } else {
                                        v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                        engine.soundManager.playBaseHit()
                                    }
                                    return true
                                }
                            }
                        }
                    }
                    2 -> {
                        // Armory Skin Selection
                        if (cm != null) {
                            for (i in 0 until 4) {
                                if (renderer.armorySkinRects[i].contains(touchX, touchY)) {
                                    cm.selectedSkinIndex = i
                                    v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    engine.soundManager.playDragSelect()
                                    return true
                                }
                            }
                        }
                    }
                }
            }
            return true
        }

        // 2. Handle Level Select Screen Touches
        if (engine.screenState == ScreenState.LEVEL_SELECT) {
            if (event.action == MotionEvent.ACTION_UP) {
                val unlocked = engine.campaignManager?.unlockedLevel ?: 1
                for (i in renderer.levelCardRects.indices) {
                    val rect = renderer.levelCardRects[i]
                    if (rect.contains(touchX, touchY)) {
                        if ((i + 1) <= unlocked) {
                            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            engine.soundManager.playDragSelect()
                            engine.campaignManager?.currentLevel = i + 1
                            engine.loadLevel(i + 1, v.context, startPlaying = true)
                        } else {
                            v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            engine.soundManager.playBaseHit()
                        }
                        return true
                    }
                }
                if (renderer.levelBackBtnRect.contains(touchX, touchY)) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.MAIN_MENU
                    return true
                }
            }
            return true
        }

        // 3. Handle Active Gameplay Modal (Victory / Defeat)
        if (engine.status != GameStatus.RUNNING) {
            if (event.action == MotionEvent.ACTION_UP) {
                if (renderer.modalBtnRect.contains(touchX, touchY)) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    if (engine.status == GameStatus.VICTORY) {
                        onNextLevelRequested?.invoke()
                    } else {
                        engine.restartLevel(v.context)
                    }
                    return true
                }
                if (renderer.modalHomeBtnRect.contains(touchX, touchY)) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.MAIN_MENU
                    return true
                }
            }
            return true
        }

        // 4. Handle In-Game Top HUD Action Buttons
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (renderer.homeBtnRect.contains(touchX, touchY)) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.soundManager.playDragSelect()
                engine.screenState = ScreenState.MAIN_MENU
                return true
            }
            if (renderer.pauseBtnRect.contains(touchX, touchY)) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.isPaused = !engine.isPaused
                return true
            }
            if (renderer.restartBtnRect.contains(touchX, touchY)) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.restartLevel(v.context)
                return true
            }
        }

        // 5. Multi-drag Troop Dispatching on the Battlefield
        val lx = renderer.toLogicalX(touchX)
        val ly = renderer.toLogicalY(touchY)

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
