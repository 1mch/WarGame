package cdev.wargame.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class LivingBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val renderer = LivingBackgroundRenderer()
    private var lastTime = System.nanoTime()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        renderer.resize(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastTime = now

        renderer.draw(canvas, width.toFloat(), height.toFloat(), dt)

        // Kontinuálne prekresľovanie pre 60 FPS animáciu pozadia
        postInvalidateOnAnimation()
    }
}

class LivingBackgroundRenderer {

    private data class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val radius: Float,
        val color: Int
    )

    private val particles = mutableListOf<Particle>()
    private var elapsedSec = 0f
    private var cachedWidth = 0
    private var cachedHeight = 0

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#101010")
        style = Paint.Style.FILL
    }
    private val gradPaint = Paint()
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val radarPath = Path()

    private val dashOuter = DashPathEffect(floatArrayOf(8f, 12f), 0f)
    private val dashMid = DashPathEffect(floatArrayOf(40f, 16f, 20f, 16f), 0f)
    private val dashHex = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    private val dashInner = DashPathEffect(floatArrayOf(14f, 10f), 0f)

    // Helper functions for strictly Yellow (#FFC629) or White (#FFFFFF) + alpha
    private fun yellowAlpha(alpha: Int): Int = Color.argb(alpha.coerceIn(0, 255), 255, 198, 41)
    private fun whiteAlpha(alpha: Int): Int = Color.argb(alpha.coerceIn(0, 255), 255, 255, 255)

    fun resize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        cachedWidth = w
        cachedHeight = h
        particles.clear()
        val s = w / 350f

        // Centrálny ambientný radiálny gradient (iba žltá + biela + alpha do čiernej)
        val wf = w.toFloat()
        val hf = h.toFloat()
        gradPaint.shader = RadialGradient(
            wf / 2f,
            hf * 0.65f,
            max(wf, hf) * 0.7f,
            intArrayOf(
                yellowAlpha(14),
                whiteAlpha(5),
                Color.argb(245, 16, 16, 16)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // 35 autonómnych svetelných častíc (iba žltá alebo biela + alpha)
        repeat(35) {
            val isYellow = Random.nextFloat() < 0.60f
            val color = if (isYellow) {
                yellowAlpha((Random.nextFloat() * 100 + 45).toInt())
            } else {
                whiteAlpha((Random.nextFloat() * 85 + 35).toInt())
            }
            particles.add(
                Particle(
                    x = Random.nextFloat() * wf,
                    y = Random.nextFloat() * hf,
                    vx = (Random.nextFloat() - 0.5f) * 14f * s,
                    vy = (-Random.nextFloat() * 24f - 9f) * s,
                    radius = (Random.nextFloat() * 1.5f + 0.8f) * s,
                    color = color
                )
            )
        }
    }

    fun draw(canvas: Canvas, w: Float, h: Float, dt: Float) {
        if (w.toInt() != cachedWidth || h.toInt() != cachedHeight || particles.isEmpty()) {
            resize(w.toInt(), h.toInt())
        }

        val s = w / 350f
        elapsedSec += dt

        // 1. Základná hlboká čierna #101010
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Stredový radiálny gradient v dolnej tretine obrazovky
        canvas.drawRect(0f, 0f, w, h, gradPaint)

        // 3. Kozmický kinetický prach (Floating Particles)
        for (p in particles) {
            p.x += p.vx * dt
            p.y += p.vy * dt

            if (p.y < -10f) {
                p.y = h + 10f
                p.x = Random.nextFloat() * w
            }
            if (p.x < -10f) p.x = w + 10f
            if (p.x > w + 10f) p.x = -10f

            particlePaint.color = p.color
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
        }

        // 4. Taktický vektorový radar – každý element rotuje vlastnou rýchlosťou a smerom
        canvas.save()
        val radarCenterX = w / 2f
        val radarCenterY = h * 0.85f
        val radarScale = (380f * s) / 500f

        canvas.translate(radarCenterX, radarCenterY)
        canvas.scale(radarScale, radarScale)

        // 4.1 Vonkajší prerušovaný kruh (r = 230, žltá + alpha, pomalá rotácia doprava +4.5°/s)
        drawRotatedElement(canvas, elapsedSec * 4.5f) {
            radarPaint.strokeWidth = 2f
            radarPaint.color = yellowAlpha(65)
            radarPaint.pathEffect = dashOuter
            canvas.drawCircle(0f, 0f, 230f, radarPaint)
        }

        // 4.2 Segmentovaný kruh (r = 190, biela + alpha, rotácia doľava -7.5°/s)
        drawRotatedElement(canvas, -elapsedSec * 7.5f) {
            radarPaint.strokeWidth = 2f
            radarPaint.color = whiteAlpha(50)
            radarPaint.pathEffect = dashMid
            canvas.drawCircle(0f, 0f, 190f, radarPaint)
        }

        // 4.3 Šesťuholník radaru (r = 155, žltá + alpha, prerušovaná čiara, rotácia doprava +3.2°/s)
        drawRotatedElement(canvas, elapsedSec * 3.2f) {
            radarPath.reset()
            val hexRadius = 155f
            for (i in 0 until 6) {
                val angleRad = Math.toRadians((i * 60.0) - 30.0)
                val hx = (cos(angleRad) * hexRadius).toFloat()
                val hy = (sin(angleRad) * hexRadius).toFloat()
                if (i == 0) radarPath.moveTo(hx, hy) else radarPath.lineTo(hx, hy)
            }
            radarPath.close()
            radarPaint.strokeWidth = 1.8f
            radarPaint.color = yellowAlpha(48)
            radarPaint.pathEffect = dashHex
            canvas.drawPath(radarPath, radarPaint)
        }

        // 4.4 Navigačný trojuholník 1 (r = 130, biela + alpha, rotácia doľava -11.0°/s)
        drawRotatedElement(canvas, -elapsedSec * 11.0f) {
            radarPath.reset()
            val triRadius = 130f
            for (i in 0 until 3) {
                val angleRad = Math.toRadians((i * 120.0) - 90.0)
                val tx = (cos(angleRad) * triRadius).toFloat()
                val ty = (sin(angleRad) * triRadius).toFloat()
                if (i == 0) radarPath.moveTo(tx, ty) else radarPath.lineTo(tx, ty)
            }
            radarPath.close()
            radarPaint.pathEffect = null
            radarPaint.strokeWidth = 1.6f
            radarPaint.color = whiteAlpha(45)
            canvas.drawPath(radarPath, radarPaint)
        }

        // 4.5 Navigačný trojuholník 2 (r = 130, žltá + alpha, protibežná rotácia doprava +8.5°/s)
        drawRotatedElement(canvas, elapsedSec * 8.5f) {
            radarPath.reset()
            val triRadius = 130f
            for (i in 0 until 3) {
                val angleRad = Math.toRadians((i * 120.0) + 90.0)
                val tx = (cos(angleRad) * triRadius).toFloat()
                val ty = (sin(angleRad) * triRadius).toFloat()
                if (i == 0) radarPath.moveTo(tx, ty) else radarPath.lineTo(tx, ty)
            }
            radarPath.close()
            radarPaint.pathEffect = null
            radarPaint.strokeWidth = 1.6f
            radarPaint.color = yellowAlpha(52)
            canvas.drawPath(radarPath, radarPaint)
        }

        // 4.6 Vnútorný prerušovaný prstenec (r = 70, biela + alpha, rotácia doľava -16.0°/s)
        drawRotatedElement(canvas, -elapsedSec * 16.0f) {
            radarPaint.color = whiteAlpha(45)
            radarPaint.strokeWidth = 1.5f
            radarPaint.pathEffect = dashInner
            canvas.drawCircle(0f, 0f, 70f, radarPaint)
        }

        // 4.7 Jadrový prstenec (r = 24, žltá + alpha, rýchlejšia rotácia doprava +24.0°/s)
        drawRotatedElement(canvas, elapsedSec * 24.0f) {
            radarPaint.color = yellowAlpha(65)
            radarPaint.strokeWidth = 1.6f
            radarPaint.pathEffect = dashHex
            canvas.drawCircle(0f, 0f, 24f, radarPaint)
        }

        // 4.8 Zameriavací kríž (Crosshairs, biela + alpha, jemná rotácia doľava -1.8°/s)
        drawRotatedElement(canvas, -elapsedSec * 1.8f) {
            radarPaint.pathEffect = null
            radarPaint.color = whiteAlpha(35)
            radarPaint.strokeWidth = 1.2f
            canvas.drawLine(-245f, 0f, 245f, 0f, radarPaint)
            canvas.drawLine(0f, -245f, 0f, 245f, radarPaint)
        }

        canvas.restore()
    }

    private inline fun drawRotatedElement(canvas: Canvas, angleDegrees: Float, block: () -> Unit) {
        canvas.save()
        canvas.rotate(angleDegrees % 360f)
        block()
        canvas.restore()
    }
}
