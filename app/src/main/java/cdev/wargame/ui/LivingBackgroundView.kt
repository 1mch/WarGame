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
    private var radarAngle = 0f
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

    fun resize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        cachedWidth = w
        cachedHeight = h
        particles.clear()
        val s = w / 350f

        // 2. Centrálny ambientný radiálny gradient v dolnej tretine obrazovky
        val wf = w.toFloat()
        val hf = h.toFloat()
        gradPaint.shader = RadialGradient(
            wf / 2f,
            hf * 0.65f,
            max(wf, hf) * 0.7f,
            intArrayOf(
                Color.argb(12, 255, 198, 41),  // Zlatá žiara rgba(255, 198, 41, 0.045)
                Color.argb(4, 77, 241, 255),   // Jemná cyan rgba(77, 241, 255, 0.015)
                Color.argb(245, 16, 16, 16)    // #101010 na okrajoch
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // 35 autonómnych svetelných častíc (50% zlaté, 30% cyan, 20% biele)
        repeat(35) {
            val roll = Random.nextFloat()
            val color = when {
                roll < 0.50f -> Color.argb((Random.nextFloat() * 100 + 45).toInt(), 255, 198, 41) // Zlatá
                roll < 0.80f -> Color.argb((Random.nextFloat() * 100 + 45).toInt(), 77, 241, 255) // Cyan
                else -> Color.argb((Random.nextFloat() * 80 + 35).toInt(), 255, 255, 255)         // Biela
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

        // 4. Rotujúci taktický vektorový radar (Cyber Radar Geometry)
        // 1 celá otáčka za 180 sekúnd
        radarAngle = (radarAngle + (360f / 180f) * dt) % 360f

        canvas.save()
        val radarCenterX = w / 2f
        val radarCenterY = h * 0.85f // Spodkom zapustený pod okraj (bottom: -15%)
        val radarScale = (380f * s) / 500f

        canvas.translate(radarCenterX, radarCenterY)
        canvas.rotate(radarAngle)
        canvas.scale(radarScale, radarScale)

        // Vonkajší prerušovaný kruh: Polomer 230px, farba #FFC629, dash 8, 12 (opacity ~0.20)
        radarPaint.strokeWidth = 2f
        radarPaint.color = Color.argb(55, 255, 198, 41)
        radarPaint.pathEffect = dashOuter
        canvas.drawCircle(0f, 0f, 230f, radarPaint)

        // Segmentovaný kruh: Polomer 190px, farba #4DF1FF, dash 40, 16, 20, 16
        radarPaint.color = Color.argb(50, 77, 241, 255)
        radarPaint.pathEffect = dashMid
        canvas.drawCircle(0f, 0f, 190f, radarPaint)

        // Šesťuholník radaru: 6 vrcholov s prerušovanou bielou čiarou dash 6, 6
        radarPath.reset()
        val hexRadius = 155f
        for (i in 0 until 6) {
            val angleRad = Math.toRadians((i * 60.0) - 30.0)
            val hx = (cos(angleRad) * hexRadius).toFloat()
            val hy = (sin(angleRad) * hexRadius).toFloat()
            if (i == 0) radarPath.moveTo(hx, hy) else radarPath.lineTo(hx, hy)
        }
        radarPath.close()
        radarPaint.color = Color.argb(42, 255, 255, 255)
        radarPaint.pathEffect = dashHex
        canvas.drawPath(radarPath, radarPaint)

        // Prekrývajúce sa navigačné trojuholníky: Ružový #F45588 a zelený #4ADE80
        radarPaint.pathEffect = null
        radarPaint.strokeWidth = 1.6f

        // Trojuholník 1 (Ružový #F45588, smerom nahor)
        radarPath.reset()
        val triRadius = 130f
        for (i in 0 until 3) {
            val angleRad = Math.toRadians((i * 120.0) - 90.0)
            val tx = (cos(angleRad) * triRadius).toFloat()
            val ty = (sin(angleRad) * triRadius).toFloat()
            if (i == 0) radarPath.moveTo(tx, ty) else radarPath.lineTo(tx, ty)
        }
        radarPath.close()
        radarPaint.color = Color.argb(45, 244, 85, 136)
        canvas.drawPath(radarPath, radarPaint)

        // Trojuholník 2 (Zelený #4ADE80, smerom nadol)
        radarPath.reset()
        for (i in 0 until 3) {
            val angleRad = Math.toRadians((i * 120.0) + 90.0)
            val tx = (cos(angleRad) * triRadius).toFloat()
            val ty = (sin(angleRad) * triRadius).toFloat()
            if (i == 0) radarPath.moveTo(tx, ty) else radarPath.lineTo(tx, ty)
        }
        radarPath.close()
        radarPaint.color = Color.argb(45, 74, 222, 128)
        canvas.drawPath(radarPath, radarPaint)

        // Jadrové krúžky a zameriavací kríž (Crosshairs) pretínajúci stred
        radarPaint.color = Color.argb(40, 255, 255, 255)
        radarPaint.strokeWidth = 1.4f
        canvas.drawCircle(0f, 0f, 70f, radarPaint)
        radarPaint.color = Color.argb(55, 255, 198, 41)
        canvas.drawCircle(0f, 0f, 24f, radarPaint)

        radarPaint.color = Color.argb(38, 255, 255, 255)
        radarPaint.strokeWidth = 1.2f
        canvas.drawLine(-245f, 0f, 245f, 0f, radarPaint)
        canvas.drawLine(0f, -245f, 0f, 245f, radarPaint)

        canvas.restore()
    }
}

