package cdev.wargame.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import cdev.wargame.core.CampaignManager
import cdev.wargame.core.GameEngine
import cdev.wargame.core.ScreenState
import cdev.wargame.ui.screens.GameHudScreen
import cdev.wargame.ui.screens.MainMenuScreen
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun WarGameRootScreen(
    engine: GameEngine,
    campaignManager: CampaignManager,
    gameSurfaceView: GameSurfaceView,
    uiTickTrigger: Int,
    onNextMission: () -> Unit
) {
    val context = LocalContext.current
    val hostView = LocalView.current

    var refreshCounter by remember { mutableIntStateOf(0) }
    var screenState by remember { mutableStateOf(engine.screenState) }
    var gameStatus by remember { mutableStateOf(engine.status) }
    var isPaused by remember { mutableStateOf(engine.isPaused) }
    var activeTab by remember { mutableIntStateOf(engine.activeMenuTab) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var powerCounts by remember { mutableStateOf(engine.getFactionPowerCounts()) }

    // Periodic sync with GameEngine state
    LaunchedEffect(uiTickTrigger) {
        while (true) {
            screenState = engine.screenState
            gameStatus = engine.status
            isPaused = engine.isPaused
            if (screenState == ScreenState.PLAYING) {
                powerCounts = engine.getFactionPowerCounts()
            }
            delay(80L)
        }
    }

    val res = remember(campaignManager.language, refreshCounter, uiTickTrigger) {
        localizedResources(context, campaignManager.language)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 2D Battlefield SurfaceView (renders bases + base text, units, obstacles, tethers, ripples, particles)
        AndroidView(
            factory = { gameSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        if (screenState != ScreenState.PLAYING) {
            // 2. Living Background View (Cosmic Dust + Counter-Rotating Cyber Radar)
            AndroidView(
                factory = { ctx -> LivingBackgroundView(ctx) },
                modifier = Modifier.fillMaxSize()
            )

            // 3. Main Menu Compose Screen
            MainMenuScreen(
                res = res,
                engine = engine,
                campaignManager = campaignManager,
                screenState = screenState,
                activeTab = activeTab,
                isSettingsOpen = isSettingsOpen,
                onTabChange = { tab ->
                    hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.MAIN_MENU
                    engine.activeMenuTab = tab
                    activeTab = tab
                    screenState = ScreenState.MAIN_MENU
                },
                onOpenSettings = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    isSettingsOpen = true
                },
                onCloseSettings = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    isSettingsOpen = false
                },
                onLaunchMission = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    engine.soundManager.playUnitsDispatched()
                    engine.startCampaign(context)
                    screenState = engine.screenState
                    gameStatus = engine.status
                },
                onOpenLevelSelect = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.LEVEL_SELECT
                    screenState = ScreenState.LEVEL_SELECT
                },
                onBackToMenu = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.MAIN_MENU
                    screenState = ScreenState.MAIN_MENU
                },
                onSelectLevel = { levelIdx ->
                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    engine.soundManager.playDragSelect()
                    campaignManager.currentLevel = levelIdx
                    engine.loadLevel(levelIdx, context, startPlaying = true)
                    screenState = engine.screenState
                    gameStatus = engine.status
                },
                onStateMutated = { refreshCounter++ }
            )
        } else {
            // 4. In-Game HUD & Victory/Defeat Modal Compose Screen
            GameHudScreen(
                res = res,
                engine = engine,
                campaignManager = campaignManager,
                gameStatus = gameStatus,
                isPaused = isPaused,
                powerCounts = powerCounts,
                onHome = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.isPaused = false
                    engine.screenState = ScreenState.MAIN_MENU
                    screenState = ScreenState.MAIN_MENU
                },
                onRestart = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.restartLevel(context)
                    gameStatus = engine.status
                    isPaused = engine.isPaused
                },
                onTogglePause = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.isPaused = !engine.isPaused
                    isPaused = engine.isPaused
                },
                onNextMission = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onNextMission()
                    gameStatus = engine.status
                }
            )
        }
    }
}

private fun localizedResources(context: Context, language: String): Resources {
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(language.lowercase(Locale.ROOT)))
    return context.createConfigurationContext(config).resources
}
