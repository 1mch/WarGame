package cdev.wargame.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import cdev.wargame.core.GameConfig
import cdev.wargame.core.GameEngine
import cdev.wargame.core.GameStatus
import cdev.wargame.core.ScreenState
import java.util.Locale
import kotlin.math.PI
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

    private val livingBg = LivingBackgroundRenderer()
    private var lastRenderNano: Long = System.nanoTime()

    // Paints
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
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val topHudGradPaint = Paint()
    private val sepGradPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()
    private val tempRect = RectF()
    private val path = Path()

    // In-Game HUD Action Button Rectangles
    val homeBtnRect = RectF()
    val pauseBtnRect = RectF()
    val restartBtnRect = RectF()
    val modalBtnRect = RectF()
    val modalHomeBtnRect = RectF()

    // Main Menu Hit-Test Rectangles
    val menuPlayBtnRect = RectF()
    val menuLevelsBtnRect = RectF()      // Last Sector Pill -> opens Sector Select
    val menuSettingsBtnRect = RectF()    // Top-right Settings Gear button
    val bottomTabRects = Array(3) { RectF() }

    // Settings Modal Hit-Test Rectangles
    val settingsLangBtnRect = RectF()
    val settingsSoundBtnRect = RectF()
    val settingsResetBtnRect = RectF()
    val settingsCloseBtnRect = RectF()

    // Tech & Armory Interactive Hit-Test Rectangles
    val techUpgradeBtnRects = Array(4) { RectF() }
    val armorySkinRects = Array(4) { RectF() }

    // Level Select Rectangles
    val levelCardRects = mutableListOf<RectF>()
    val levelBackBtnRect = RectF()

    fun resize(w: Int, h: Int) {
        width = w
        height = h
        isPortrait = width < height
        scale = if (isPortrait) width / 2.0f else height / 2.0f
        centerX = width / 2.0f
        centerY = height / 2.0f
        // Proportional UI scale factor (s = width / 350.0 in portrait, clamped in landscape)
        s = min(width, height) / 350.0f

        livingBg.resize(w, h)

        val safeTop = 28f * s
        val topHudH = 56f * s + safeTop
        topHudGradPaint.shader = LinearGradient(
            0f, 0f, 0f, topHudH,
            intArrayOf(Color.argb(242, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        // Top HUD Settings button
        val settingsSize = 34f * s
        val sidePad = 16f * s
        val topCenterY = safeTop + 24f * s
        menuSettingsBtnRect.set(
            width - sidePad - settingsSize,
            topCenterY - settingsSize / 2f,
            width - sidePad,
            topCenterY + settingsSize / 2f
        )

        // Bottom Tabs Bar (height 84 * s)
        val bottomBarH = 84f * s
        val bottomBarTop = height - bottomBarH
        val tabW = width / 3f
        for (i in 0 until 3) {
            bottomTabRects[i].set(i * tabW, bottomBarTop, (i + 1) * tabW, height.toFloat())
        }

        // In-Game HUD Buttons bounds
        val btnSize = 34f * s
        val padding = 12f * s
        pauseBtnRect.set(width - btnSize - padding, padding, width - padding, padding + btnSize)
        restartBtnRect.set(pauseBtnRect.left - btnSize - 6f * s, padding, pauseBtnRect.left - 6f * s, padding + btnSize)
        homeBtnRect.set(restartBtnRect.left - btnSize - 6f * s, padding, restartBtnRect.left - 6f * s, padding + btnSize)

        // Level Select Rectangles
        levelCardRects.clear()
        val cardW = min(width - 40f * s, 320f * s)
        val cardLeft = centerX - cardW / 2f
        val cardH = 44f * s
        val startY = 95f * s
        for (i in 0 until 5) {
            val cy = startY + i * (cardH + 10f * s)
            levelCardRects.add(RectF(cardLeft, cy, cardLeft + cardW, cy + cardH))
        }
        levelBackBtnRect.set(cardLeft, height - 110f * s, cardLeft + cardW, height - 66f * s)
    }

    fun toScreenX(x: Float): Float = centerX + x * scale
    fun toScreenY(y: Float): Float = centerY + y * scale
    fun toScreenRadius(r: Float): Float = r * scale
    fun toLogicalX(screenX: Float): Float = (screenX - centerX) / scale
    fun toLogicalY(screenY: Float): Float = (screenY - centerY) / scale

    fun render(canvas: Canvas, engine: GameEngine) {
        val now = System.currentTimeMillis()
        val nowNano = System.nanoTime()
        val dt = ((nowNano - lastRenderNano) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastRenderNano = nowNano

        when (engine.screenState) {
            ScreenState.MAIN_MENU -> {
                livingBg.draw(canvas, width.toFloat(), height.toFloat(), dt)
                renderMainMenu(canvas, engine, now)
            }
            ScreenState.LEVEL_SELECT -> {
                livingBg.draw(canvas, width.toFloat(), height.toFloat(), dt)
                renderLevelSelect(canvas, engine)
            }
            ScreenState.PLAYING -> {
                // 1. Tactical Dark Blue-Black Background
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

                renderGameElements(canvas, engine, now)
                renderHud(canvas, engine)
                if (engine.status != GameStatus.RUNNING) {
                    renderGameOverModal(canvas, engine)
                }
            }
        }
    }

    private fun renderMainMenu(canvas: Canvas, engine: GameEngine, now: Long) {
        val cm = engine.campaignManager
        val isSk = cm?.language == "SK"

        // 1. Top HUD Bar (Fixed at top with Safe Area inset)
        renderTopHud(canvas, engine)

        // 2. Central Content Area based on active Bottom Tab
        when (engine.activeMenuTab) {
            0 -> renderMissionTabContent(canvas, engine, now, isSk)
            1 -> renderTechTabContent(canvas, engine, isSk)
            2 -> renderArmoryTabContent(canvas, engine, isSk)
        }

        // 3. Bottom Navigation Tabs Bar (Fixed at bottom, height = 84 * s)
        renderBottomTabs(canvas, engine, isSk)

        // 4. Settings Modal (if open)
        if (engine.isSettingsModalOpen) {
            renderSettingsModal(canvas, engine, isSk)
        }
    }

    private fun renderTopHud(canvas: Canvas, engine: GameEngine) {
        val cm = engine.campaignManager
        val safeTop = 28f * s
        val topHudH = 56f * s + safeTop

        // Vertical gradient background (rgba(0,0,0,0.95) -> transparent)
        canvas.drawRect(0f, 0f, width.toFloat(), topHudH, topHudGradPaint)

        // Currency Pill on the left
        val sidePad = 16f * s
        val pillH = 32f * s
        val pillW = 168f * s
        val topCenterY = safeTop + 24f * s
        val pillRect = RectF(sidePad, topCenterY - pillH / 2f, sidePad + pillW, topCenterY + pillH / 2f)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#0A0A0A")
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#222222")
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, hudPaint)

        // Credits (🪙 2,450 in #FFC629)
        val creditsStr = cm?.formatNumber(cm.credits) ?: "2,450"
        val gemsStr = cm?.formatNumber(cm.gems) ?: "48"
        val splitX = pillRect.left + pillW * 0.56f

        textPaint.textSize = 11f * s
        textPaint.letterSpacing = 0.02f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#FFC629")
        val textY = pillRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("🪙 $creditsStr", (pillRect.left + splitX) / 2f, textY, textPaint)

        // 1px vertical divider (#333333)
        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#333333")
        canvas.drawLine(splitX, pillRect.top + 7f * s, splitX, pillRect.bottom - 7f * s, hudPaint)

        // Diamonds (💎 48 in #4DF1FF)
        textPaint.color = Color.parseColor("#4DF1FF")
        canvas.drawText("💎 $gemsStr", (splitX + pillRect.right) / 2f, textY, textPaint)
        textPaint.letterSpacing = 0f

        // Settings Circular Button on the right (⚙)
        val cx = menuSettingsBtnRect.centerX()
        val cy = menuSettingsBtnRect.centerY()
        val r = menuSettingsBtnRect.width() / 2f

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#0A0A0A")
        canvas.drawCircle(cx, cy, r, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#222222")
        canvas.drawCircle(cx, cy, r, hudPaint)

        textPaint.textSize = 15f * s
        textPaint.color = Color.parseColor("#CCCCCC")
        textPaint.textAlign = Paint.Align.CENTER
        val gearY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("⚙", cx, gearY, textPaint)
    }

    private fun renderMissionTabContent(canvas: Canvas, engine: GameEngine, now: Long, isSk: Boolean) {
        val cm = engine.campaignManager
        val playerLvl = cm?.playerLevel ?: 12
        val rankName = cm?.getRankName() ?: "COMMANDER"
        val sectorsConquered = cm?.sectorsConquered ?: 12
        val winRate = String.format(Locale.US, "%.1f%%", cm?.getWinRatePercent() ?: 85.7f)
        val streak = cm?.winStreak ?: 4

        val contentW = min(width - 36f * s, 320f * s)
        val contentLeft = centerX - contentW / 2f
        val contentRight = centerX + contentW / 2f

        // Calculate vertical layout within the middle zone
        val safeTop = 28f * s
        val topZoneBottom = safeTop + 52f * s
        val bottomBarTop = height - 84f * s
        val availableH = bottomBarTop - topZoneBottom
        val startY = topZoneBottom + (availableH - 380f * s).coerceAtLeast(8f * s) * 0.42f

        // --- 2. Header Section: Pulsing Rank Badge ---
        val badgeText = if (isSk) "$rankName • ÚROVEŇ $playerLvl" else "$rankName • LVL $playerLvl"
        textPaint.textSize = 9f * s
        textPaint.letterSpacing = 0.08f
        val badgeTextW = textPaint.measureText(badgeText)
        val badgeW = badgeTextW + 30f * s
        val badgeH = 21f * s
        val badgeRect = RectF(centerX - badgeW / 2f, startY, centerX + badgeW / 2f, startY + badgeH)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#11161F")
        canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, hudPaint)

        // Pulsing cyan dot on the left (4.5 * s, alpha 0.6 -> 1.0, scale 1.0 -> 1.2)
        val dotWave = ((sin(now / 260.0) + 1.0) * 0.5).toFloat()
        val dotScale = 1.0f + 0.2f * dotWave
        val dotAlpha = (153 + (102 * dotWave).toInt()).coerceIn(0, 255)
        val dotRadius = (2.25f * s) * dotScale
        val dotX = badgeRect.left + 11f * s
        val dotY = badgeRect.centerY()

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.argb(dotAlpha, 77, 241, 255)
        canvas.drawCircle(dotX, dotY, dotRadius, hudPaint)

        textPaint.color = Color.parseColor("#4DF1FF")
        textPaint.textAlign = Paint.Align.LEFT
        val badgeTy = badgeRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(badgeText, dotX + 7f * s, badgeTy, textPaint)

        // --- Game Logo: WARGAME (White #FFFFFF) + .IO (Cyan #00D2FF) ---
        val logoY = badgeRect.bottom + 38f * s
        textPaint.textSize = 34f * s
        textPaint.letterSpacing = 0.14f
        textPaint.textAlign = Paint.Align.LEFT

        val part1 = "WARGAME"
        val part2 = ".IO"
        val w1 = textPaint.measureText(part1)
        val w2 = textPaint.measureText(part2)
        val totalLogoW = w1 + w2
        val logoStartX = centerX - totalLogoW / 2f

        textPaint.color = Color.WHITE
        canvas.drawText(part1, logoStartX, logoY, textPaint)

        textPaint.color = Color.parseColor("#00D2FF")
        canvas.drawText(part2, logoStartX + w1, logoY, textPaint)

        // --- Subtitle: REAL-TIME TACTICAL SWARM CONQUEST ---
        val subY = logoY + 18f * s
        textPaint.textSize = 8.5f * s
        textPaint.letterSpacing = 0.24f
        textPaint.color = Color.parseColor("#888888")
        textPaint.textAlign = Paint.Align.CENTER
        val subtitle = if (isSk) "TAKTICKÁ DOBÝVACIA STRATÉGIA V REÁLNOM ČASE" else "REAL-TIME TACTICAL SWARM CONQUEST"
        canvas.drawText(subtitle, centerX, subY, textPaint)
        textPaint.letterSpacing = 0f

        // --- Light Gradient Separator (120 * s x 1.5 * s, transparent -> #FFC629 -> transparent) ---
        val sepY = subY + 12f * s
        val sepHalfW = 60f * s
        sepGradPaint.shader = LinearGradient(
            centerX - sepHalfW, sepY, centerX + sepHalfW, sepY,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#FFC629"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(centerX - sepHalfW, sepY, centerX + sepHalfW, sepY + 1.5f * s, sepGradPaint)

        // --- 3. Player Stat Card (#121212 fill, #222222 border, 10 * s radius) ---
        val cardTop = sepY + 16f * s
        val cardH = 128f * s
        val statCardRect = RectF(contentLeft, cardTop, contentRight, cardTop + cardH)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#121212")
        canvas.drawRoundRect(statCardRect, 10f * s, 10f * s, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#222222")
        canvas.drawRoundRect(statCardRect, 10f * s, 10f * s, hudPaint)

        // Top label: PLAYER LEVEL
        textPaint.textSize = 9.5f * s
        textPaint.letterSpacing = 0.12f
        textPaint.color = Color.parseColor("#666666")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isSk) "ÚROVEŇ HRÁČA" else "PLAYER LEVEL", centerX, cardTop + 20f * s, textPaint)

        // Main value: LVL 12 in #FFC629 with dropShadow rgba(255, 198, 41, 0.35)
        textPaint.textSize = 30f * s
        textPaint.letterSpacing = 0.04f
        textPaint.color = Color.parseColor("#FFC629")
        textPaint.setShadowLayer(12f * s, 0f, 2f * s, Color.argb(90, 255, 198, 41))
        canvas.drawText("LVL $playerLvl", centerX, cardTop + 54f * s, textPaint)
        textPaint.clearShadowLayer()

        // Subtitle: 12 SECTORS CONQUERED in #4DF1FF bold 11 * s
        textPaint.textSize = 10.5f * s
        textPaint.letterSpacing = 0.08f
        textPaint.color = Color.parseColor("#4DF1FF")
        val sectorsTxt = if (isSk) "$sectorsConquered DOBYTÝCH SEKTOROV" else "$sectorsConquered SECTORS CONQUERED"
        canvas.drawText(sectorsTxt, centerX, cardTop + 74f * s, textPaint)

        // Divider line #1E1E1E
        val divY = cardTop + 90f * s
        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#1E1E1E")
        canvas.drawLine(statCardRect.left + 14f * s, divY, statCardRect.right - 14f * s, divY, hudPaint)

        // Bottom row: WIN RATE: 85.7% (#4ADE80) | STREAK: 4 WINS (#FFC629)
        val bottomRowY = cardTop + 111f * s
        textPaint.textSize = 9.5f * s
        textPaint.letterSpacing = 0.05f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#4ADE80")
        val winRateLabel = if (isSk) "ÚSPEŠNOSŤ: $winRate" else "WIN RATE: $winRate"
        canvas.drawText(winRateLabel, statCardRect.left + 16f * s, bottomRowY, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.parseColor("#FFC629")
        val streakLabel = if (isSk) "SÉRIA: $streak VÍŤAZSTIEV" else "STREAK: $streak WINS"
        canvas.drawText(streakLabel, statCardRect.right - 16f * s, bottomRowY, textPaint)
        textPaint.letterSpacing = 0f

        // --- 4. Last Sector Summary Pill (#141414 fill, #222222 border) ---
        val pillTop = statCardRect.bottom + 12f * s
        val pillH = 36f * s
        menuLevelsBtnRect.set(contentLeft, pillTop, contentRight, pillTop + pillH)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#141414")
        canvas.drawRoundRect(menuLevelsBtnRect, 8f * s, 8f * s, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#222222")
        canvas.drawRoundRect(menuLevelsBtnRect, 8f * s, 8f * s, hudPaint)

        val lastSector = cm?.lastSector ?: 7
        val lastPts = cm?.formatNumber(cm.lastScore) ?: "4,850"
        val lastCr = cm?.lastCreditsReward ?: 150
        val lastGm = cm?.lastGemsReward ?: 6
        val pillCy = menuLevelsBtnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f

        textPaint.textSize = 9.5f * s
        textPaint.letterSpacing = 0.04f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#94A3B8")
        val lastSecText = if (isSk) "POSL. SEKTOR $lastSector ▾" else "LAST SECTOR $lastSector ▾"
        canvas.drawText(lastSecText, menuLevelsBtnRect.left + 12f * s, pillCy, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.parseColor("#E2E8F0")
        canvas.drawText("$lastPts PTS   +$lastCr 🪙   +$lastGm 💎", menuLevelsBtnRect.right - 12f * s, pillCy, textPaint)
        textPaint.letterSpacing = 0f

        // --- 5. Main Button: "▶ LAUNCH MISSION" with 2.8s Breathing Pulse ---
        val btnTop = menuLevelsBtnRect.bottom + 14f * s
        val btnH = 52f * s
        menuPlayBtnRect.set(contentLeft, btnTop, contentRight, btnTop + btnH)

        // Sinusoidal 2.8s cycle: scale in [1.0, 1.025], shadowBlur in [25 * s, 45 * s]
        val phase = (now % 2800L).toDouble() / 2800.0
        val wave = ((sin(phase * 2.0 * PI) + 1.0) * 0.5).toFloat()
        val pulseScale = 1.0f + 0.025f * wave
        val shadowBlur = (25f + 20f * wave) * s

        canvas.save()
        canvas.scale(pulseScale, pulseScale, menuPlayBtnRect.centerX(), menuPlayBtnRect.centerY())

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#FFC629")
        hudPaint.setShadowLayer(shadowBlur, 0f, 0f, Color.argb(115, 255, 198, 41))
        canvas.drawRoundRect(menuPlayBtnRect, 10f * s, 10f * s, hudPaint)
        hudPaint.clearShadowLayer()

        textPaint.textSize = 14.5f * s
        textPaint.letterSpacing = 0.12f
        textPaint.color = Color.parseColor("#0A0401")
        textPaint.textAlign = Paint.Align.CENTER
        val launchLabel = if (isSk) "▶   SPUSTIŤ MISIU" else "▶   LAUNCH MISSION"
        val playTy = menuPlayBtnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(launchLabel, menuPlayBtnRect.centerX(), playTy, textPaint)
        textPaint.letterSpacing = 0f

        canvas.restore()

        // --- Tactical Instruction below button ---
        textPaint.textSize = 8.5f * s
        textPaint.letterSpacing = 0.10f
        textPaint.color = Color.parseColor("#555555")
        textPaint.textAlign = Paint.Align.CENTER
        val instr1 = if (isSk) "POTIAHNI ZO ZÁKLADNE PRE ÚTOK // ROJOVÁ OFENZÍVA" else "DRAG FROM BASE TO ATTACK // SWARM CONQUEST"
        val instr2 = if (isSk) "SPOJ VIACERO ZÁKLADNÍ JEDNÝM ŤAHOM" else "CONNECT MULTIPLE NODES TO SWARM"
        canvas.drawText(instr1, centerX, menuPlayBtnRect.bottom + 18f * s, textPaint)
        canvas.drawText(instr2, centerX, menuPlayBtnRect.bottom + 30f * s, textPaint)
        textPaint.letterSpacing = 0f
    }

    private fun renderTechTabContent(canvas: Canvas, engine: GameEngine, isSk: Boolean) {
        val cm = engine.campaignManager
        val contentW = min(width - 36f * s, 320f * s)
        val contentLeft = centerX - contentW / 2f
        val contentRight = centerX + contentW / 2f
        val startY = 100f * s

        textPaint.textSize = 20f * s
        textPaint.letterSpacing = 0.10f
        textPaint.color = Color.parseColor("#FFC629")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isSk) "TECHNOLÓGIE ROJA" else "SWARM TECH LAB", centerX, startY, textPaint)

        textPaint.textSize = 9.5f * s
        textPaint.letterSpacing = 0.08f
        textPaint.color = Color.parseColor("#888888")
        canvas.drawText(
            if (isSk) "VYLEPŠI PARAMETRE JEDNOTIEK A ZÁKLADNÍ" else "UPGRADE UNIT & NODE COMBAT PARAMETERS",
            centerX,
            startY + 18f * s,
            textPaint
        )
        textPaint.letterSpacing = 0f

        val names = if (isSk) {
            arrayOf("⚡ RÝCHLOSŤ LETU", "🔄 REGENERÁCIA UZLA", "🛡️ ŠTÍT ZÁKLADNE", "📦 MAX. KAPACITA")
        } else {
            arrayOf("⚡ THRUSTER SPEED", "🔄 NODE REGEN RATE", "🛡️ BASE SHIELDING", "📦 SWARM CAPACITY")
        }
        val levels = arrayOf(
            cm?.techSpeedLevel ?: 2,
            cm?.techRegenLevel ?: 3,
            cm?.techShieldLevel ?: 1,
            cm?.techCapacityLevel ?: 2
        )

        val cardH = 60f * s
        var curY = startY + 36f * s
        for (i in 0 until 4) {
            val cardRect = RectF(contentLeft, curY, contentRight, curY + cardH)
            hudPaint.style = Paint.Style.FILL
            hudPaint.color = Color.parseColor("#121212")
            canvas.drawRoundRect(cardRect, 10f * s, 10f * s, hudPaint)

            hudPaint.style = Paint.Style.STROKE
            hudPaint.strokeWidth = 1f * s
            hudPaint.color = Color.parseColor("#222222")
            canvas.drawRoundRect(cardRect, 10f * s, 10f * s, hudPaint)

            val lvl = levels[i]
            val cost = lvl * 250

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 11.5f * s
            textPaint.color = Color.WHITE
            canvas.drawText(names[i], cardRect.left + 14f * s, cardRect.top + 24f * s, textPaint)

            textPaint.textSize = 9.5f * s
            textPaint.color = Color.parseColor("#4DF1FF")
            canvas.drawText("LVL $lvl / 10", cardRect.left + 14f * s, cardRect.top + 44f * s, textPaint)

            // Upgrade button on right
            val btnW = 92f * s
            val btnH = 34f * s
            val btnRect = techUpgradeBtnRects[i]
            btnRect.set(
                cardRect.right - btnW - 12f * s,
                cardRect.centerY() - btnH / 2f,
                cardRect.right - 12f * s,
                cardRect.centerY() + btnH / 2f
            )

            val canAfford = (cm?.credits ?: 0) >= cost && lvl < 10
            hudPaint.style = Paint.Style.FILL
            hudPaint.color = if (canAfford) Color.parseColor("#FFC629") else Color.parseColor("#1E1E1E")
            canvas.drawRoundRect(btnRect, 6f * s, 6f * s, hudPaint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 10f * s
            textPaint.color = if (canAfford) Color.parseColor("#0A0401") else Color.parseColor("#666666")
            val btnLabel = if (lvl >= 10) "MAX" else "🪙 $cost"
            val bty = btnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(btnLabel, btnRect.centerX(), bty, textPaint)

            curY += cardH + 10f * s
        }
    }

    private fun renderArmoryTabContent(canvas: Canvas, engine: GameEngine, isSk: Boolean) {
        val cm = engine.campaignManager
        val contentW = min(width - 36f * s, 320f * s)
        val contentLeft = centerX - contentW / 2f
        val contentRight = centerX + contentW / 2f
        val startY = 100f * s

        textPaint.textSize = 20f * s
        textPaint.letterSpacing = 0.10f
        textPaint.color = Color.parseColor("#4DF1FF")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isSk) "ZBROJNICA & PROFIL" else "ARMORY & PILOT PROFILE", centerX, startY, textPaint)

        textPaint.textSize = 9.5f * s
        textPaint.letterSpacing = 0.08f
        textPaint.color = Color.parseColor("#888888")
        canvas.drawText(
            if (isSk) "VÝBER VIZUÁLNEJ SCHÉMY FLOTILY" else "SELECT FLEET VISUAL CHROMA SKIN",
            centerX,
            startY + 18f * s,
            textPaint
        )
        textPaint.letterSpacing = 0f

        val skinNames = arrayOf(
            "CYBER CYAN (DEFAULT)",
            "SOLAR GOLD (#FFC629)",
            "EMERALD MATRIX (#10B981)",
            "PLASMA VIOLET (#A855F7)"
        )
        val skinColors = intArrayOf(
            Color.parseColor("#00D2FF"),
            Color.parseColor("#FFC629"),
            Color.parseColor("#10B981"),
            Color.parseColor("#A855F7")
        )
        val selectedSkin = cm?.selectedSkinIndex ?: 0

        val cardH = 56f * s
        var curY = startY + 38f * s
        for (i in 0 until 4) {
            val rect = armorySkinRects[i]
            rect.set(contentLeft, curY, contentRight, curY + cardH)
            val isSel = i == selectedSkin

            hudPaint.style = Paint.Style.FILL
            hudPaint.color = Color.parseColor("#121212")
            canvas.drawRoundRect(rect, 10f * s, 10f * s, hudPaint)

            hudPaint.style = Paint.Style.STROKE
            hudPaint.strokeWidth = if (isSel) 1.8f * s else 1f * s
            hudPaint.color = if (isSel) skinColors[i] else Color.parseColor("#222222")
            canvas.drawRoundRect(rect, 10f * s, 10f * s, hudPaint)

            // Preview node circle
            hudPaint.style = Paint.Style.FILL
            hudPaint.color = skinColors[i]
            canvas.drawCircle(rect.left + 26f * s, rect.centerY(), 11f * s, hudPaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 11f * s
            textPaint.color = Color.WHITE
            val ty = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(skinNames[i], rect.left + 48f * s, ty, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.textSize = 10f * s
            textPaint.color = if (isSel) Color.parseColor("#FFC629") else Color.parseColor("#666666")
            val stateTxt = if (isSel) (if (isSk) "AKTÍVNE" else "EQUIPPED") else (if (isSk) "VYBRAŤ" else "SELECT")
            canvas.drawText(stateTxt, rect.right - 14f * s, ty, textPaint)

            curY += cardH + 10f * s
        }
    }

    private fun renderBottomTabs(canvas: Canvas, engine: GameEngine, isSk: Boolean) {
        val bottomBarH = 84f * s
        val bottomBarTop = height - bottomBarH

        // Background #0B0B0B
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#0B0B0B")
        canvas.drawRect(0f, bottomBarTop, width.toFloat(), height.toFloat(), hudPaint)

        // Top border #222222
        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#222222")
        canvas.drawLine(0f, bottomBarTop, width.toFloat(), bottomBarTop, hudPaint)

        val icons = arrayOf("◎", "⚡", "🛡️")
        val titles = if (isSk) {
            arrayOf("MISIA", "TECH", "PROFIL")
        } else {
            arrayOf("MISSION", "TECH", "ARMORY")
        }

        for (i in 0 until 3) {
            val rect = bottomTabRects[i]
            val isActive = engine.activeMenuTab == i

            if (isActive) {
                // Subtle elevated background highlight
                hudPaint.style = Paint.Style.FILL
                hudPaint.color = Color.argb(18, 255, 198, 41)
                canvas.drawRect(rect, hudPaint)

                // Active top indicator bar in #FFC629
                hudPaint.color = Color.parseColor("#FFC629")
                val indW = 48f * s
                canvas.drawRoundRect(
                    rect.centerX() - indW / 2f,
                    bottomBarTop,
                    rect.centerX() + indW / 2f,
                    bottomBarTop + 3f * s,
                    2f * s,
                    2f * s,
                    hudPaint
                )
            }

            val yOffset = if (isActive) -2f * s else 0f
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 18f * s
            textPaint.color = if (isActive) Color.parseColor("#FFC629") else Color.parseColor("#666666")
            canvas.drawText(icons[i], rect.centerX(), bottomBarTop + 33f * s + yOffset, textPaint)

            textPaint.textSize = 10f * s
            textPaint.letterSpacing = 0.10f
            textPaint.color = if (isActive) Color.parseColor("#FFC629") else Color.parseColor("#666666")
            canvas.drawText(titles[i], rect.centerX(), bottomBarTop + 54f * s + yOffset, textPaint)
            textPaint.letterSpacing = 0f
        }
    }

    private fun renderSettingsModal(canvas: Canvas, engine: GameEngine, isSk: Boolean) {
        canvas.drawColor(Color.argb(210, 0, 0, 0))

        val modalW = min(width - 40f * s, 290f * s)
        val modalH = 235f * s
        val left = centerX - modalW / 2f
        val top = centerY - modalH / 2f
        tempRect.set(left, top, left + modalW, top + modalH)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#121212")
        canvas.drawRoundRect(tempRect, 12f * s, 12f * s, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1.5f * s
        hudPaint.color = Color.parseColor("#333333")
        canvas.drawRoundRect(tempRect, 12f * s, 12f * s, hudPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 15f * s
        textPaint.letterSpacing = 0.10f
        textPaint.color = Color.parseColor("#FFC629")
        canvas.drawText(if (isSk) "NASTAVENIA" else "SETTINGS", centerX, top + 30f * s, textPaint)
        textPaint.letterSpacing = 0f

        val btnW = modalW - 36f * s
        val btnH = 38f * s
        val btnLeft = centerX - btnW / 2f

        // 1. Language EN / SK button
        settingsLangBtnRect.set(btnLeft, top + 50f * s, btnLeft + btnW, top + 50f * s + btnH)
        drawModalOptionButton(
            canvas,
            settingsLangBtnRect,
            if (isSk) "JAZYK / LANGUAGE: SK 🇸🇰" else "LANGUAGE / JAZYK: EN 🇬🇧",
            Color.parseColor("#4DF1FF")
        )

        // 2. Sound SFX button
        settingsSoundBtnRect.set(btnLeft, top + 98f * s, btnLeft + btnW, top + 98f * s + btnH)
        val muted = engine.soundManager.isMuted
        val soundTxt = if (isSk) {
            if (muted) "ZVUKY (SFX): VYPNUTÉ 🔇" else "ZVUKY (SFX): ZAPNUTÉ 🔊"
        } else {
            if (muted) "SOUND SFX: MUTED 🔇" else "SOUND SFX: ACTIVE 🔊"
        }
        drawModalOptionButton(
            canvas,
            settingsSoundBtnRect,
            soundTxt,
            if (muted) Color.parseColor("#EF4444") else Color.parseColor("#4ADE80")
        )

        // 3. Reset Progress button
        settingsResetBtnRect.set(btnLeft, top + 146f * s, btnLeft + btnW, top + 146f * s + btnH)
        drawModalOptionButton(
            canvas,
            settingsResetBtnRect,
            if (isSk) "RESETOVAŤ PROGRES ⚠" else "RESET PROGRESS ⚠",
            Color.parseColor("#FF3366")
        )

        // 4. Close button
        settingsCloseBtnRect.set(btnLeft, top + 192f * s, btnLeft + btnW, top + 192f * s + 32f * s)
        textPaint.textSize = 11f * s
        textPaint.color = Color.parseColor("#94A3B8")
        val cty = settingsCloseBtnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(if (isSk) "ZAVRIEŤ [✕]" else "CLOSE [✕]", centerX, cty, textPaint)
    }

    private fun drawModalOptionButton(canvas: Canvas, rect: RectF, text: String, textColor: Int) {
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#1A1A1A")
        canvas.drawRoundRect(rect, 8f * s, 8f * s, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#2A2A2A")
        canvas.drawRoundRect(rect, 8f * s, 8f * s, hudPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 11f * s
        textPaint.color = textColor
        val ty = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, rect.centerX(), ty, textPaint)
    }

    private fun renderGameElements(canvas: Canvas, engine: GameEngine, now: Long) {
        // Laser Drag Tethers
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

        // Ripples
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

        // Obstacles
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

        // Bases
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
            val textY = by - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText("${base.unitCount}", bx, textY, textPaint)
        }

        // Units
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

        // Particles
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

    private fun renderLevelSelect(canvas: Canvas, engine: GameEngine) {
        val isSk = engine.campaignManager?.language == "SK"

        // Title
        textPaint.textSize = 20f * s
        textPaint.letterSpacing = 0.10f
        textPaint.color = Color.parseColor("#FFC629")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isSk) "VÝBER SEKTORA" else "CAMPAIGN SECTORS", centerX, 62f * s, textPaint)
        textPaint.letterSpacing = 0f

        val unlocked = engine.campaignManager?.unlockedLevel ?: 1
        val levelTitles = listOf(
            "Sector 1: First Clash",
            "Sector 2: Triangulation",
            "Sector 3: Asteroid Chokepoints",
            "Sector 4: Quad Siege",
            "Sector 5: Total Dominion"
        )

        for (i in 0 until 5) {
            val rect = levelCardRects[i]
            val isUnlocked = (i + 1) <= unlocked
            val isCurrent = (i + 1) == engine.currentLevelIndex

            hudPaint.style = Paint.Style.FILL
            hudPaint.color = if (isUnlocked) Color.parseColor("#121212") else Color.parseColor("#0C0C0C")
            canvas.drawRoundRect(rect, 8f * s, 8f * s, hudPaint)

            hudPaint.style = Paint.Style.STROKE
            hudPaint.strokeWidth = if (isCurrent) 1.8f * s else 1f * s
            hudPaint.color = when {
                isCurrent -> Color.parseColor("#FFC629")
                isUnlocked -> Color.parseColor("#2A2A2A")
                else -> Color.parseColor("#1A1A1A")
            }
            canvas.drawRoundRect(rect, 8f * s, 8f * s, hudPaint)

            // Mission title
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 12.5f * s
            textPaint.color = if (isUnlocked) Color.WHITE else Color.parseColor("#555555")
            canvas.drawText(levelTitles[i], rect.left + 14f * s, rect.centerY() + 4f * s, textPaint)

            // Status label
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.textSize = 10.5f * s
            textPaint.color = if (isUnlocked) Color.parseColor("#4ADE80") else Color.parseColor("#666666")
            val statusTxt = if (isUnlocked) (if (isSk) "SPUSTIŤ ▶" else "DEPLOY ▶") else (if (isSk) "ZAMKNUTÉ 🔒" else "LOCKED 🔒")
            canvas.drawText(statusTxt, rect.right - 14f * s, rect.centerY() + 4f * s, textPaint)
        }

        // Back Button
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#141414")
        canvas.drawRoundRect(levelBackBtnRect, 10f * s, 10f * s, hudPaint)
        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#2A2A2A")
        canvas.drawRoundRect(levelBackBtnRect, 10f * s, 10f * s, hudPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 13f * s
        textPaint.color = Color.parseColor("#FFC629")
        val bty = levelBackBtnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(if (isSk) "◀ SPÄŤ DO MENU" else "◀ BACK TO MENU", levelBackBtnRect.centerX(), bty, textPaint)
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
        val barWidth = width - 165f * s
        val barLeft = 14f * s
        val barTop = topPadding + 14f * s

        hudPaint.color = Color.argb(60, 255, 255, 255)
        hudPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, 4f * s, 4f * s, hudPaint)

        val power = engine.getFactionPowerCounts()
        val totalUnits = power.values.sum().coerceAtLeast(1)

        var currentX = barLeft
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

        textPaint.textSize = 10f * s
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(engine.levelName.uppercase(), barLeft, topPadding + 8f * s, textPaint)

        // Action Buttons: Home, Restart, Pause
        drawHudButton(canvas, homeBtnRect, "🏠")
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

        textPaint.textSize = 13f * s
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        val ty = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(symbol, rect.centerX(), ty, textPaint)
    }

    private fun renderGameOverModal(canvas: Canvas, engine: GameEngine) {
        canvas.drawColor(Color.argb(180, 9, 13, 22))

        val isWin = engine.status == GameStatus.VICTORY
        val modalWidth = 280f * s
        val modalHeight = 195f * s
        val left = (width - modalWidth) / 2
        val top = (height - modalHeight) / 2
        val modalRect = RectF(left, top, left + modalWidth, top + modalHeight)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#121212")
        canvas.drawRoundRect(modalRect, 16f * s, 16f * s, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 2.0f * s
        hudPaint.color = if (isWin) Color.parseColor("#FFC629") else Color.parseColor("#FF3366")
        canvas.drawRoundRect(modalRect, 16f * s, 16f * s, hudPaint)

        textPaint.textSize = 24f * s
        textPaint.color = if (isWin) Color.parseColor("#FFC629") else Color.parseColor("#FF3366")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isWin) "VICTORY!" else "DEFEAT", modalRect.centerX(), top + 42f * s, textPaint)

        textPaint.textSize = 11f * s
        textPaint.color = Color.parseColor("#94A3B8")
        val sub = if (isWin) "Sector secured! +150 🪙  +6 💎" else "All command nodes were lost."
        canvas.drawText(sub, modalRect.centerX(), top + 68f * s, textPaint)

        // Action Button 1: Next / Retry
        val btnW = 200f * s
        val btnH = 38f * s
        val btnX = modalRect.centerX() - btnW / 2
        val btnY = top + 92f * s
        modalBtnRect.set(btnX, btnY, btnX + btnW, btnY + btnH)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = if (isWin) Color.parseColor("#FFC629") else Color.parseColor("#E11D48")
        canvas.drawRoundRect(modalBtnRect, 10f * s, 10f * s, hudPaint)

        textPaint.textSize = 13.5f * s
        textPaint.color = if (isWin) Color.parseColor("#0A0401") else Color.WHITE
        val text = if (isWin) "NEXT MISSION ▶" else "RETRY MISSION 🔄"
        val by = modalBtnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, modalBtnRect.centerX(), by, textPaint)

        // Action Button 2: Return to Menu
        val btn2Y = top + 140f * s
        modalHomeBtnRect.set(btnX, btn2Y, btnX + btnW, btn2Y + btnH)

        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.parseColor("#1A1A1A")
        canvas.drawRoundRect(modalHomeBtnRect, 10f * s, 10f * s, hudPaint)
        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 1f * s
        hudPaint.color = Color.parseColor("#333333")
        canvas.drawRoundRect(modalHomeBtnRect, 10f * s, 10f * s, hudPaint)

        textPaint.textSize = 12.5f * s
        textPaint.color = Color.WHITE
        val b2y = modalHomeBtnRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("MAIN MENU 🏠", modalHomeBtnRect.centerX(), b2y, textPaint)
    }
}
