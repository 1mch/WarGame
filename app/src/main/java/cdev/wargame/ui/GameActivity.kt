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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while playing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        campaignManager = CampaignManager(this)

        gameSurfaceView = GameSurfaceView(this)
        setContentView(gameSurfaceView)

        // Hide system bars AFTER setContentView so decorView is initialized
        hideSystemBars()

        gameSurfaceView.engine.onLevelCompleted = { levelIndex ->
            campaignManager.completeLevel(levelIndex)
        }

        gameSurfaceView.onNextMission = {
            val nextLevel = if (campaignManager.currentLevel < campaignManager.maxCampaignLevels) {
                campaignManager.currentLevel + 1
            } else {
                1
            }
            campaignManager.currentLevel = nextLevel
            gameSurfaceView.engine.loadLevel(nextLevel, this)
        }

        // Start initial campaign level
        gameSurfaceView.engine.loadLevel(campaignManager.currentLevel, this)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        gameSurfaceView.startLoop()
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
