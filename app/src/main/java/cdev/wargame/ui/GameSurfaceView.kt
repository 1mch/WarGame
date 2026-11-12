package cdev.wargame.ui

import android.content.Context
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import cdev.wargame.core.GameEngine
import kotlin.math.min

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    val engine = GameEngine()
    val renderer = CanvasRenderer()

    var onNextMission: (() -> Unit)? = null

    init {
        holder.addCallback(this)
        holder.setFormat(PixelFormat.RGBA_8888)
        setOnTouchListener(GameInputHandler(engine, renderer) {
            onNextMission?.invoke()
        })
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.resize(width, height)
        engine.setupScreenBounds(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    fun startLoop() {
        if (!isRunning) {
            isRunning = true
            gameThread = Thread(this, "GameLoopThread").apply { start() }
        }
    }

    fun stopLoop() {
        isRunning = false
        try {
            gameThread?.join(500)
        } catch (_: InterruptedException) {
        }
        gameThread = null
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (isRunning) {
            val now = System.nanoTime()
            val rawDtMs = (now - lastTime) / 1_000_000f
            lastTime = now

            // Clamp max dtMs to avoid huge teleport steps on pause/resume
            val dtMs = min(rawDtMs, 50f)

            if (!engine.isPaused) {
                engine.update(dtMs)
            }

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    renderer.render(canvas, engine)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            // Cap at ~120 FPS to save battery
            try {
                Thread.sleep(2)
            } catch (_: InterruptedException) {
            }
        }
    }
}
