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
import cdev.wargame.core.GameStatus
import cdev.wargame.core.Vector2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CanvasRenderer {

    var width: Int = 1080
    var height: Int = 1920
    var scale: Float = 540f
    var centerX: Float = 540f
    var centerY: Float = 960f
    var s: Float = 3f
    var isPortrait: Boolean = true

    // Reusable Paints to avoid GC allocation during rendering
    private val bgPaint = Paint().apply {
        color = GameConfig.COLOR_BACKGROUND
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        color = GameConfig.COLOR_GRID
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val baseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val baseRingBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

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

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val unitTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val rockFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
    }

    private val rockStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#475569")
    }

    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()
    private val path = Path()

    // HUD Button Rectangles for touch testing
    val pauseBtnRect = RectF()
    val restartBtnRect = RectF()
    val modalBtnRect = RectF()

    fun resize(w: Int, h: Int) {
        width = w
        height = h
        isPortrait = width < height
        scale = if (isPortrait) width / 2.0f else height / 2.0f
        centerX = width / 2.0f
        centerY = height / 2.0f
        s = width / 350.0f

        // Setup HUD buttons bounds
        val btnSize = 36f * s
        val padding = 12f * s
        pauseBtnRect.set(width - btnSize - padding, padding, width - padding, padding + btnSize)
        restartBtnRect.set(pauseBtnRect.left - btnSize - 8f * s, padding, pauseBtnRect.left - 8f * s, padding + btnSize)
    }

    fun toScreenX(x: Float): Float = centerX + x * scale
    fun toScreenY(y: Float): Float = centerY + y * scale
    fun toScreenRadius(r: Float): Float = r * scale

    fun toLogicalX(screenX: Float): Float = (screenX - centerX) / scale
    fun toLogicalY(screenY: Float): Float = (screenY - centerY) / scale

    fun render(canvas: Canvas, engine: GameEngine) {
        val now = System.currentTimeMillis()

        // 1. Background
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

        // 3. Laser Drag Tethers
        if (engine.dragState.active && engine.dragState.startBases.isNotEmpty()) {
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
            tetherPaint.pathEffect = DashPathEffect(floatArrayOf(10f * s, 6f * s), phase)

            for (startBase in engine.dragState.startBases) {
                val startX = toScreenX(startBase.position.x)
                val startY = toScreenY(startBase.position.y)

                canvas.drawLine(startX, startY, targetScreenX, targetScreenY, tetherPaint)

                // Directional arrowhead near the target
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
            tetherPaint.pathEffect = null // reset
        }

        // 4. Ripples (shockwaves)
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

        // 5. Obstacles (Asteroids / rocks)
        rockStrokePaint.strokeWidth = 2.0f * s
        for (obs in engine.obstacles) {
            val ox = toScreenX(obs.position.x)
            val oy = toScreenY(obs.position.y)
            path.reset()
            val verts = obs.relativeVertices
            if (verts.isNotEmpty()) {
                val firstX = ox + verts[0].x * scale
                val firstY = oy + verts[0].y * scale
                path.moveTo(firstX, firstY)
                for (i in 1 until verts.size) {
                    path.lineTo(ox + verts[i].x * scale, oy + verts[i].y * scale)
                }
                path.close()
                canvas.drawPath(path, rockFillPaint)
                canvas.drawPath(path, rockStrokePaint)
            }
        }

        // 6. Bases
        baseRingBgPaint.strokeWidth = 2.4f * s
        baseRingBgPaint.color = Color.argb(40, 255, 255, 255)
        baseRingPaint.strokeWidth = 2.6f * s

        for (base in engine.bases) {
            val bx = toScreenX(base.position.x)
            val by = toScreenY(base.position.y)
            val br = toScreenRadius(base.radius)
            val faction = GameConfig.getFaction(base.ownerId)

            // Base body
            baseFillPaint.color = faction.primaryColor
            canvas.drawCircle(bx, by, br, baseFillPaint)

            // Inner dark accent
            baseFillPaint.color = faction.darkColor
            canvas.drawCircle(bx, by, br * 0.72f, baseFillPaint)

            // Capacity Ring
            val ringRadius = br + 3.5f * s
            canvas.drawCircle(bx, by, ringRadius, baseRingBgPaint)

            val capFraction = (base.unitCount.toFloat() / base.maxUnits.toFloat()).coerceIn(0f, 1f)
            val sweepAngle = capFraction * 360f
            arcRect.set(bx - ringRadius, by - ringRadius, bx + ringRadius, by + ringRadius)
            baseRingPaint.color = if (capFraction >= 1f) GameConfig.COLOR_GOLD else faction.glowColor
            canvas.drawArc(arcRect, -90f, sweepAngle, false, baseRingPaint)

            // Drag selection highlight ring
            if (engine.dragState.startBases.contains(base)) {
                val pulse = (sin(now / 120.0) * 2f).toFloat() * s
                baseSelectRingPaint.strokeWidth = 2.2f * s
                canvas.drawCircle(bx, by, ringRadius + 4f * s + pulse, baseSelectRingPaint)
            }

            // Target hover reticle
            if (engine.dragState.hoverBase == base) {
                val pulse = (sin(now / 80.0) * 3f).toFloat() * s
                baseSelectRingPaint.strokeWidth = 3f * s
                baseSelectRingPaint.color = Color.parseColor("#FFD700")
                canvas.drawCircle(bx, by, ringRadius + 6f * s + pulse, baseSelectRingPaint)
                baseSelectRingPaint.color = Color.WHITE
            }

            // Crew count text in center
            textPaint.textSize = 15f * s
            textPaint.setShadowLayer(4f * s, 0f, 1f * s, Color.BLACK)
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

            // Unit circular body
            unitPaint.color = faction.primaryColor
            canvas.drawCircle(ux, uy, unitR, unitPaint)

            // Directional kinetic arrow tip
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

        // 8. Kinetic Clash Particles
        for (particle in engine.particleSystem.particles) {
            val px = toScreenX(particle.position.x)
            val py = toScreenY(particle.position.y)
            val pSize = toScreenRadius(particle.size) * particle.alphaFraction
            val alpha = (particle.alphaFraction * 255).toInt().coerceIn(0, 255)
            particlePaint.color = particle.color
            particlePaint.alpha = alpha
            canvas.drawCircle(px, py, pSize, particlePaint)
        }

        // 9. Top HUD: Power Bar, Level Name, Action Buttons
        renderHud(canvas, engine)

        // 10. Victory / Defeat Modal
        if (engine.status != GameStatus.RUNNING) {
            renderGameOverModal(canvas, engine)
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

    private fun renderHud(canvas: Canvas, engine: GameEngine) {
        val topPadding = 12f * s
        val barHeight = 8f * s
        val barWidth = width - 130f * s
        val barLeft = 14f * s
        val barTop = topPadding + 14f * s

        // Power bar background
        hudPaint.color = Color.argb(60, 255, 255, 255)
        hudPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, 4f * s, 4f * s, hudPaint)

        // Calculate power bar proportions
        val power = engine.getFactionPowerCounts()
        val totalUnits = power.values.sum().coerceAtLeast(1)

        var currentX = barLeft
        // Render segments (Player first, then enemy factions, then neutral)
        val sortedFactions = power.keys.sortedWith(Comparator { a, b ->
            when {
                a == 1 -> -1
                b == 1 -> 1
                a == 0 -> 1
                b == 0 -> -1
                else -> a.compareTo(b)
            }
        })

        for (factionId in sortedFactions) {
            val count = power[factionId] ?: 0
            if (count > 0) {
                val segWidth = (count.toFloat() / totalUnits.toFloat()) * barWidth
                hudPaint.color = GameConfig.getFaction(factionId).primaryColor
                canvas.drawRect(currentX, barTop, currentX + segWidth, barTop + barHeight, hudPaint)
                currentX += segWidth
            }
        }

        // Mission Title Text
        textPaint.textSize = 10f * s
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(engine.levelName.uppercase(), barLeft, topPadding + 8f * s, textPaint)

        // Action Buttons: Restart and Pause
        drawHudButton(canvas, restartBtnRect, "🔄")
        val pauseIcon = if (engine.isPaused) "▶" else "⏸"
        drawHudButton(canvas, pauseBtnRect, pauseIcon)
    }

    private fun drawHudButton(canvas: Canvas, rect: RectF, symbol: String) {
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.argb(120, 30, 41, 59)
        canvas.drawRoundRect(rect, 8f * s, 8f * s, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1.2f * s
        hudPaint.color = Color.argb(180, 71, 85, 105)
        canvas.drawRoundRect(rect, 8f * s, 8f * s, hudPaint)

        textPaint.textSize = 14f * s
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        val ty = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(symbol, rect.centerX(), ty, textPaint)
    }

    private fun renderGameOverModal(canvas: Canvas, engine: GameEngine) {
        // Dim overlay
        canvas.drawColor(Color.argb(180, 9, 13, 22))

        val isWin = engine.status == GameStatus.VICTORY
        val modalWidth = 280f * s
        val modalHeight = 180f * s
        val left = (width - modalWidth) / 2
        val top = (height - modalHeight) / 2
        val modalRect = RectF(left, top, left + modalWidth, top + modalHeight)

        // Modal Card
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(modalRect, 16f * s, 16f * s, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 2.5f * s
        hudPaint.color = if (isWin) Color.parseColor("#00D2FF") else Color.parseColor("#FF3366")
        canvas.drawRoundRect(modalRect, 16f * s, 16f * s, hudPaint)

        // Title
        textPaint.textSize = 24f * s
        textPaint.color = if (isWin) Color.parseColor("#00D2FF") else Color.parseColor("#FF3366")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isWin) "VICTORY!" else "DEFEAT", modalRect.centerX(), top + 45f * s, textPaint)

        // Subtitle
        textPaint.textSize = 11f * s
        textPaint.color = Color.parseColor("#94A3B8")
        val sub = if (isWin) "Sector fully secured, Commander!" else "All command nodes were lost."
        canvas.drawText(sub, modalRect.centerX(), top + 75f * s, textPaint)

        // Action Button
        val btnW = 180f * s
        val btnH = 40f * s
        val btnX = modalRect.centerX() - btnW / 2
        val btnY = top + 105f * s
        modalBtnRect.set(btnX, btnY, btnX + btnW, btnY + btnH)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = if (isWin) Color.parseColor("#00D2FF") else Color.parseColor("#E11D48")
        canvas.drawRoundRect(modalBtnRect, 10f * s, 10f * s, hudPaint)

        textPaint.textSize = 14f * s
        textPaint.color = if (isWin) Color.parseColor("#090D16") else Color.WHITE
        val text = if (isWin) "NEXT MISSION ▶" else "RETRY 🔄"
        val by = modalBtnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, modalBtnRect.centerX(), by, textPaint)
    }
}
