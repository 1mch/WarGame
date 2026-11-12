package cdev.wargame.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cdev.wargame.core.CampaignManager

class GameActivity : AppCompatActivity() {

    private lateinit var gameSurfaceView: GameSurfaceView
    private lateinit var campaignManager: CampaignManager
    private lateinit var uiController: MainUiController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while playing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        campaignManager = CampaignManager(this)
        gameSurfaceView = GameSurfaceView(this)

        // Link campaign manager to engine
        gameSurfaceView.engine.campaignManager = campaignManager

        gameSurfaceView.engine.onLevelCompleted = { levelIndex ->
            campaignManager.completeLevel(levelIndex)
            runOnUiThread {
                if (::uiController.isInitialized) {
                    uiController.refreshAllUi()
                }
            }
        }

        uiController = MainUiController(
            context = this,
            engine = gameSurfaceView.engine,
            campaignManager = campaignManager,
            gameSurfaceView = gameSurfaceView,
            onNextMission = {
                val nextLevel = if (campaignManager.currentLevel < campaignManager.maxCampaignLevels) {
                    campaignManager.currentLevel + 1
                } else {
                    1
                }
                campaignManager.currentLevel = nextLevel
                gameSurfaceView.engine.loadLevel(nextLevel, this, startPlaying = true)
                uiController.refreshAllUi()
            }
        )

        setContentView(uiController.rootLayout)

        // Hide system bars AFTER setContentView so decorView is initialized
        hideSystemBars()

        // Start at Main Menu
        gameSurfaceView.engine.loadLevel(campaignManager.currentLevel, this, startPlaying = false)
        uiController.refreshAllUi()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        gameSurfaceView.startLoop()
        if (::uiController.isInitialized) {
            uiController.refreshAllUi()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onPause() {
        super.onPause()
        gameSurfaceView.engine.isPaused = true
        gameSurfaceView.stopLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameSurfaceView.engine.soundManager.release()
    }

    private fun hideSystemBars() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (_: Exception) {
        }
    }
}
