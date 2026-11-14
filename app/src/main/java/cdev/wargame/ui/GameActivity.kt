package cdev.wargame.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cdev.wargame.core.CampaignManager
import cdev.wargame.ui.theme.WarGameTheme

class GameActivity : AppCompatActivity() {

    private lateinit var gameSurfaceView: GameSurfaceView
    private lateinit var campaignManager: CampaignManager
    private var uiTickTrigger by mutableIntStateOf(0)

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
                uiTickTrigger++
            }
        }

        // Start at Main Menu
        gameSurfaceView.engine.loadLevel(campaignManager.currentLevel, this, startPlaying = false)

        setContent {
            WarGameTheme {
                WarGameRootScreen(
                    engine = gameSurfaceView.engine,
                    campaignManager = campaignManager,
                    gameSurfaceView = gameSurfaceView,
                    uiTickTrigger = uiTickTrigger,
                    onNextMission = {
                        val nextLevel = if (campaignManager.currentLevel < campaignManager.maxCampaignLevels) {
                            campaignManager.currentLevel + 1
                        } else {
                            1
                        }
                        campaignManager.currentLevel = nextLevel
                        gameSurfaceView.engine.loadLevel(nextLevel, this, startPlaying = true)
                        uiTickTrigger++
                    }
                )
            }
        }

        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        gameSurfaceView.startLoop()
        uiTickTrigger++
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (_: Exception) {
        }
    }
}
