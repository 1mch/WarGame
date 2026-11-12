package cdev.wargame.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import cdev.wargame.core.GameConfig
import cdev.wargame.core.GameEngine
import cdev.wargame.core.ScreenState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CanvasRenderer {

    var width: Int = 1080
    var height: Int = 1920
    var scale: Float = 540f
    var centerX: Float = 540f
    var centerY: Float = 960f
    var s: Float = 3f
    var isPortrait: Boolean = true

    // Paints for 2D Battlefield Rendering
    private val bgPaint = Paint().apply {
        color = GameConfig.COLOR_BACKGROUND
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        color = GameConfig.COLOR_GRID
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val baseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val baseRingBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val baseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val baseSelectRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val tetherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#38BDF8")
    }
    private val tetherArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#38BDF8")
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val unitTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rockFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
    }
    private val rockStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#475569")
    }

    private val arcRect = RectF()
    private val path = Path()

    fun resize(w: Int, h: Int) {
        width = w
        height = h
        isPortrait = width < height
        scale = if (isPortrait) width / 2.0f else height / 2.0f
        centerX = width / 2.0f
        centerY = height / 2.0f
        s = min(width, height) / 350.0f
    }

    fun toScreenX(x: Float): Float = centerX + x * scale
    fun toScreenY(y: Float): Float = centerY + y * scale
    fun toScreenRadius(r: Float): Float = r * scale
    fun toLogicalX(screenX: Float): Float = (screenX - centerX) / scale
    fun toLogicalY(screenY: Float): Float = (screenY - centerY) / scale

    fun render(canvas: Canvas, engine: GameEngine) {
        val now = System.currentTimeMillis()

        // 1. Tactical Dark Blue-Black Background (#090D16)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Tactical Grid
        val gridSize = 35f * s
        var gx = centerX % gridSize
        while (gx < width) {
            canvas.drawLine(gx, 0f, gx, height.toFloat(), gridPaint)
            gx += gridSize
        }
        var gy = centerY % gridSize
        while (gy < height) {
            canvas.drawLine(0f, gy, width.toFloat(), gy, gridPaint)
            gy += gridSize
        }

        // 3..8. Render 2D Battlefield Elements (Tethers, Ripples, Obstacles, Bases + Garrison Text, Units, Particles)
        synchronized(engine.stateLock) {
            renderGameElements(canvas, engine, now)
        }
    }

    private fun renderGameElements(canvas: Canvas, engine: GameEngine, now: Long) {
        // 3. Laser Drag Tethers
        if (engine.screenState == ScreenState.PLAYING && engine.dragState.active && engine.dragState.startBases.isNotEmpty()) {
            val targetScreenX = if (engine.dragState.hoverBase != null) {
                toScreenX(engine.dragState.hoverBase!!.position.x)
            } else {
                toScreenX(engine.dragState.pointerX)
            }
            val targetScreenY = if (engine.dragState.hoverBase != null) {
                toScreenY(engine.dragState.hoverBase!!.position.y)
            } else {
                toScreenY(engine.dragState.pointerY)
            }

            val phase = -(now / 20f) % (20f * s)
            tetherPaint.strokeWidth = 2.5f * s
            tetherPaint.pathEffect = DashPathEffect(floatArrayOf(12f * s, 8f * s), phase)

            for (startBase in engine.dragState.startBases) {
                val startX = toScreenX(startBase.position.x)
                val startY = toScreenY(startBase.position.y)
                canvas.drawLine(startX, startY, targetScreenX, targetScreenY, tetherPaint)

                val dx = targetScreenX - startX
                val dy = targetScreenY - startY
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist > 25f * s) {
                    val angle = atan2(dy, dx)
                    val arrowDist = dist - (if (engine.dragState.hoverBase != null) toScreenRadius(engine.dragState.hoverBase!!.radius) else 0f)
                    val ax = startX + cos(angle) * arrowDist
                    val ay = startY + sin(angle) * arrowDist
                    drawArrowhead(canvas, ax, ay, angle)
                }
            }
            tetherPaint.pathEffect = null
        }

        // 4. Ripples
        for (ripple in engine.particleSystem.ripples) {
            val rx = toScreenX(ripple.position.x)
            val ry = toScreenY(ripple.position.y)
            val rr = toScreenRadius(ripple.currentRadius)
            val alpha = (ripple.alphaFraction * 255).toInt().coerceIn(0, 255)
            ripplePaint.color = ripple.color
            ripplePaint.alpha = alpha
            ripplePaint.strokeWidth = 3f * s * ripple.alphaFraction
            canvas.drawCircle(rx, ry, rr, ripplePaint)
        }

        // 5. Obstacles
        rockStrokePaint.strokeWidth = 2.0f * s
        for (obs in engine.obstacles) {
            val ox = toScreenX(obs.position.x)
            val oy = toScreenY(obs.position.y)
            path.reset()
            val verts = obs.relativeVertices
            if (verts.isNotEmpty()) {
                path.moveTo(ox + verts[0].x * scale, oy + verts[0].y * scale)
                for (i in 1 until verts.size) {
                    path.lineTo(ox + verts[i].x * scale, oy + verts[i].y * scale)
                }
                path.close()
                canvas.drawPath(path, rockFillPaint)
                canvas.drawPath(path, rockStrokePaint)
            }
        }

        // 6. Bases (Circle + Capacity Ring + Selection Ring + Garrison Number Text)
        baseRingBgPaint.strokeWidth = 2.2f * s
        baseRingBgPaint.color = Color.argb(38, 255, 255, 255)
        baseRingPaint.strokeWidth = 2.4f * s

        for (base in engine.bases) {
            val bx = toScreenX(base.position.x)
            val by = toScreenY(base.position.y)
            val br = toScreenRadius(base.radius)
            val faction = GameConfig.getFaction(base.ownerId)

            baseFillPaint.color = faction.primaryColor
            canvas.drawCircle(bx, by, br, baseFillPaint)

            baseFillPaint.color = faction.darkColor
            canvas.drawCircle(bx, by, br * 0.72f, baseFillPaint)

            val ringRadius = br + 3.5f * s
            canvas.drawCircle(bx, by, ringRadius, baseRingBgPaint)

            val capFraction = (base.unitCount.toFloat() / base.maxUnits.toFloat()).coerceIn(0f, 1f)
            val sweepAngle = capFraction * 360f
            arcRect.set(bx - ringRadius, by - ringRadius, bx + ringRadius, by + ringRadius)
            baseRingPaint.color = if (capFraction >= 1f) GameConfig.COLOR_GOLD else faction.glowColor
            canvas.drawArc(arcRect, -90f, sweepAngle, false, baseRingPaint)

            if (engine.dragState.startBases.contains(base)) {
                val pulse = (sin(now / 120.0) * 2f).toFloat() * s
                baseSelectRingPaint.strokeWidth = 2.2f * s
                canvas.drawCircle(bx, by, br + 7f * s + pulse, baseSelectRingPaint)
            }

            if (engine.dragState.hoverBase == base) {
                val pulse = (sin(now / 80.0) * 3f).toFloat() * s
                baseSelectRingPaint.strokeWidth = 3f * s
                baseSelectRingPaint.color = Color.parseColor("#FFD700")
                canvas.drawCircle(bx, by, ringRadius + 6f * s + pulse, baseSelectRingPaint)
                baseSelectRingPaint.color = Color.WHITE
            }

            textPaint.textSize = 15f * s
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE
            textPaint.setShadowLayer(4f * s, 0f, 1f * s, Color.argb(160, 0, 0, 0))
            val textY = by - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText("${base.unitCount}", bx, textY, textPaint)
            textPaint.clearShadowLayer()
        }

        // 7. Units
        val unitR = toScreenRadius(GameConfig.UNIT_RADIUS)
        for (unit in engine.units) {
            if (unit.isDead) continue
            val ux = toScreenX(unit.position.x)
            val uy = toScreenY(unit.position.y)
            val faction = GameConfig.getFaction(unit.ownerId)
            val angle = atan2(unit.velocity.y, unit.velocity.x)

            unitPaint.color = faction.primaryColor
            canvas.drawCircle(ux, uy, unitR, unitPaint)

            val tipLen = unitR * 1.8f
            val tipX = ux + cos(angle) * tipLen
            val tipY = uy + sin(angle) * tipLen
            val leftAngle = angle + 2.5f
            val rightAngle = angle - 2.5f
            val sideLen = unitR * 0.9f

            path.reset()
            path.moveTo(tipX, tipY)
            path.lineTo(ux + cos(leftAngle) * sideLen, uy + sin(leftAngle) * sideLen)
            path.lineTo(ux + cos(rightAngle) * sideLen, uy + sin(rightAngle) * sideLen)
            path.close()
            canvas.drawPath(path, unitTipPaint)
        }

        // 8. Particles
        for (particle in engine.particleSystem.particles) {
            val px = toScreenX(particle.position.x)
            val py = toScreenY(particle.position.y)
            val pSize = toScreenRadius(particle.size) * particle.alphaFraction
            val alpha = (particle.alphaFraction * 255).toInt().coerceIn(0, 255)
            particlePaint.color = particle.color
            particlePaint.alpha = alpha
            canvas.drawCircle(px, py, pSize, particlePaint)
        }
    }

    private fun drawArrowhead(canvas: Canvas, x: Float, y: Float, angle: Float) {
        val arrowSize = 10f * s
        val backAngle1 = angle + 2.5f
        val backAngle2 = angle - 2.5f
        path.reset()
        path.moveTo(x + cos(angle) * arrowSize, y + sin(angle) * arrowSize)
        path.lineTo(x + cos(backAngle1) * arrowSize, y + sin(backAngle1) * arrowSize)
        path.lineTo(x + cos(backAngle2) * arrowSize, y + sin(backAngle2) * arrowSize)
        path.close()
        canvas.drawPath(path, tetherArrowPaint)
    }
}
