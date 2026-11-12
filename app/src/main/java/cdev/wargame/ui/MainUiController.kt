package cdev.wargame.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cdev.wargame.core.CampaignManager
import cdev.wargame.core.GameConfig
import cdev.wargame.core.GameEngine
import cdev.wargame.core.GameStatus
import cdev.wargame.core.ScreenState
import java.util.Locale

class MainUiController(
    private val context: Context,
    private val engine: GameEngine,
    private val campaignManager: CampaignManager,
    private val gameSurfaceView: GameSurfaceView,
    private val onNextMission: () -> Unit
) {
    val rootLayout = FrameLayout(context)

    // Layer 1: LivingBackgroundView (for Main Menu / Level Select)
    private val livingBackgroundView = LivingBackgroundView(context)

    // Layer 2: Main Menu Native Container
    private val menuOverlay = FrameLayout(context)

    // Layer 3: In-Game HUD Native Container
    private val inGameHudOverlay = FrameLayout(context)

    // Top HUD views
    private lateinit var creditsText: TextView
    private lateinit var gemsText: TextView

    // Mission Tab views
    private lateinit var rankBadgeText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var statCardTitleText: TextView
    private lateinit var statCardLevelText: TextView
    private lateinit var statCardSectorsText: TextView
    private lateinit var statCardWinRateText: TextView
    private lateinit var statCardStreakText: TextView
    private lateinit var lastSectorLeftText: TextView
    private lateinit var lastSectorRightText: TextView
    private lateinit var launchMissionBtn: TextView
    private lateinit var instructionText1: TextView
    private lateinit var instructionText2: TextView

    // Middle tab containers
    private lateinit var missionTabContainer: View
    private lateinit var techTabContainer: LinearLayout
    private lateinit var armoryTabContainer: LinearLayout
    private lateinit var levelSelectContainer: LinearLayout

    // Bottom Navigation Tab views
    private val bottomTabIndicators = arrayOfNulls<View>(3)
    private val bottomTabIcons = arrayOfNulls<TextView>(3)
    private val bottomTabLabels = arrayOfNulls<TextView>(3)
    private val bottomTabContainers = arrayOfNulls<LinearLayout>(3)

    // Settings Modal overlay
    private lateinit var settingsModalOverlay: FrameLayout
    private lateinit var settingsTitleText: TextView
    private lateinit var settingsLangBtn: TextView
    private lateinit var settingsSoundBtn: TextView
    private lateinit var settingsResetBtn: TextView
    private lateinit var settingsCloseBtn: TextView

    // In-Game HUD views
    private lateinit var hudLevelTitleText: TextView
    private lateinit var hudPowerBarView: PowerBarView
    private lateinit var hudPauseBtn: TextView
    private lateinit var gameOverModalOverlay: FrameLayout
    private lateinit var gameOverCard: LinearLayout
    private lateinit var gameOverTitleText: TextView
    private lateinit var gameOverSubText: TextView
    private lateinit var gameOverPrimaryBtn: TextView
    private lateinit var gameOverHomeBtn: TextView

    init {
        buildHierarchy()
        refreshAllUi()
        startHudSyncTicker()
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun dpF(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        )
    }

    private fun roundedBg(fillColor: Int, strokeColor: Int = Color.TRANSPARENT, strokeWidthDp: Float = 0f, radiusDp: Float = 10f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpF(radiusDp)
            setColor(fillColor)
            if (strokeWidthDp > 0f) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun buildHierarchy() {
        // 1. 2D Battlefield SurfaceView (renders bases + base text, units, obstacles, tethers, particles)
        rootLayout.addView(
            gameSurfaceView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        // 2. LivingBackgroundView (shown when in Main Menu / Level Select)
        rootLayout.addView(
            livingBackgroundView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        // 3. Build Native Main Menu Overlay
        buildMenuOverlay()
        rootLayout.addView(
            menuOverlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        // 4. Build Native In-Game HUD & Victory/Defeat Modal Overlay
        buildInGameHudOverlay()
        rootLayout.addView(
            inGameHudOverlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    private fun buildMenuOverlay() {
        // --- A. TOP HUD BAR ---
        val topHudBar = FrameLayout(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(242, 0, 0, 0), Color.TRANSPARENT)
            )
            setPadding(dp(16f), dp(28f), dp(16f), dp(12f))
        }

        ViewCompat.setOnApplyWindowInsetsListener(topHudBar) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val safeTop = maxOf(sysBars.top, dp(24f))
            view.setPadding(dp(16f), safeTop + dp(8f), dp(16f), dp(12f))
            insets
        }

        // Currency Pill (Left)
        val currencyPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBg(Color.parseColor("#0A0A0A"), Color.parseColor("#222222"), 1f, 18f)
            setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
        }

        creditsText = TextView(context).apply {
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val pillDivider = View(context).apply {
            setBackgroundColor(Color.parseColor("#333333"))
        }
        gemsText = TextView(context).apply {
            setTextColor(Color.parseColor("#4DF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }

        currencyPill.addView(creditsText)
        currencyPill.addView(
            pillDivider,
            LinearLayout.LayoutParams(dp(1f), dp(14f)).apply {
                marginStart = dp(10f)
                marginEnd = dp(10f)
            }
        )
        currencyPill.addView(gemsText)

        topHudBar.addView(
            currencyPill,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34f), Gravity.START or Gravity.CENTER_VERTICAL)
        )

        // Settings Circular Button (Right)
        val settingsBtn = TextView(context).apply {
            text = "⚙"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#CCCCCC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0A0A0A"))
                setStroke(dp(1f), Color.parseColor("#222222"))
            }
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.soundManager.playDragSelect()
                settingsModalOverlay.visibility = View.VISIBLE
                refreshAllUi()
            }
        }
        topHudBar.addView(
            settingsBtn,
            FrameLayout.LayoutParams(dp(36f), dp(36f), Gravity.END or Gravity.CENTER_VERTICAL)
        )

        // --- B. MIDDLE ZONE CONTAINER ---
        val middleFrame = FrameLayout(context)
        missionTabContainer = buildMissionTabView()
        techTabContainer = buildTechTabView()
        armoryTabContainer = buildArmoryTabView()
        levelSelectContainer = buildLevelSelectView()

        middleFrame.addView(
            missionTabContainer,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        middleFrame.addView(
            techTabContainer,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        middleFrame.addView(
            armoryTabContainer,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        middleFrame.addView(
            levelSelectContainer,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        menuOverlay.addView(
            middleFrame,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(76f)
                bottomMargin = dp(84f)
            }
        )

        menuOverlay.addView(
            topHudBar,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        )

        // --- C. BOTTOM TABS BAR ---
        val bottomTabsBar = buildBottomTabsBar()
        menuOverlay.addView(
            bottomTabsBar,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(84f), Gravity.BOTTOM)
        )

        // --- D. SETTINGS MODAL ---
        settingsModalOverlay = buildSettingsModal()
        settingsModalOverlay.visibility = View.GONE
        menuOverlay.addView(
            settingsModalOverlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    private fun buildMissionTabView(): View {
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val wrapper = FrameLayout(context).apply {
            setPadding(dp(20f), dp(8f), dp(20f), dp(8f))
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 1. Pulsing Rank Badge
        val rankBadge = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBg(Color.parseColor("#11161F"), Color.parseColor("#1E293B"), 1f, 12f)
            setPadding(dp(12f), dp(4f), dp(12f), dp(4f))
        }

        val pulsingDot = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4DF1FF"))
            }
        }
        // Cyclic pulse animation (alpha 0.6 -> 1.0, scale 1.0 -> 1.2)
        ObjectAnimator.ofPropertyValuesHolder(
            pulsingDot,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 1.0f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.25f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.25f)
        ).apply {
            duration = 900L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        rankBadgeText = TextView(context).apply {
            setTextColor(Color.parseColor("#4DF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }

        rankBadge.addView(pulsingDot, LinearLayout.LayoutParams(dp(6f), dp(6f)).apply { marginEnd = dp(8f) })
        rankBadge.addView(rankBadgeText)
        column.addView(rankBadge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // 2. Game Logo: WARGAME (White #FFFFFF) + .IO (Cyan #00D2FF)
        val logoText = TextView(context).apply {
            val span = SpannableString("WARGAME.IO")
            span.setSpan(ForegroundColorSpan(Color.WHITE), 0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(ForegroundColorSpan(Color.parseColor("#00D2FF")), 7, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = span
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
        }
        column.addView(
            logoText,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10f)
            }
        )

        // 3. Subtitle
        subtitleText = TextView(context).apply {
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.22f
            gravity = Gravity.CENTER
        }
        column.addView(
            subtitleText,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4f)
            }
        )

        // 4. Horizontal Gradient Separator (transparent -> #FFC629 -> transparent)
        val separator = View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#FFC629"), Color.TRANSPARENT)
            )
        }
        column.addView(
            separator,
            LinearLayout.LayoutParams(dp(130f), dp(2f)).apply {
                topMargin = dp(12f)
                bottomMargin = dp(16f)
            }
        )

        // 5. Player Stat Card (#121212 background, #222222 border, 10dp radius)
        val statCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundedBg(Color.parseColor("#121212"), Color.parseColor("#222222"), 1f, 10f)
            setPadding(dp(16f), dp(14f), dp(16f), dp(12f))
        }

        statCardTitleText = TextView(context).apply {
            setTextColor(Color.parseColor("#666666"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
        }
        statCardLevelText = TextView(context).apply {
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(dpF(10f), 0f, dpF(2f), Color.argb(95, 255, 198, 41))
        }
        statCardSectorsText = TextView(context).apply {
            setTextColor(Color.parseColor("#4DF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }
        val cardDivider = View(context).apply {
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }

        val statBottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statCardWinRateText = TextView(context).apply {
            setTextColor(Color.parseColor("#4ADE80"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        }
        statCardStreakText = TextView(context).apply {
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        }
        statBottomRow.addView(statCardWinRateText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        statBottomRow.addView(statCardStreakText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        statCard.addView(statCardTitleText)
        statCard.addView(statCardLevelText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2f) })
        statCard.addView(statCardSectorsText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2f) })
        statCard.addView(cardDivider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f)).apply { topMargin = dp(12f); bottomMargin = dp(10f) })
        statCard.addView(statBottomRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        column.addView(statCard, LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT))

        // 6. Last Sector Pill (#141414 fill, #222222 border) -> Tapping opens Sector Selection
        val lastSectorPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBg(Color.parseColor("#141414"), Color.parseColor("#222222"), 1f, 8f)
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.soundManager.playDragSelect()
                engine.screenState = ScreenState.LEVEL_SELECT
                refreshAllUi()
            }
        }
        lastSectorLeftText = TextView(context).apply {
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.DEFAULT_BOLD
        }
        lastSectorRightText = TextView(context).apply {
            setTextColor(Color.parseColor("#E2E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        }
        lastSectorPill.addView(lastSectorLeftText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        lastSectorPill.addView(lastSectorRightText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        column.addView(
            lastSectorPill,
            LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12f)
            }
        )

        // 7. Main Button: "▶ LAUNCH MISSION" (52dp height, #FFC629 neon-gold, 2.8s Breathing Pulse)
        launchMissionBtn = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0A0401"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.12f
            background = roundedBg(Color.parseColor("#FFC629"), radiusDp = 10f)
            elevation = dpF(8f)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                engine.soundManager.playUnitsDispatched()
                engine.startCampaign(context)
                refreshAllUi()
            }
        }

        // 2.8s Breathing Pulse Animation (scale 1.0 -> 1.025)
        ObjectAnimator.ofPropertyValuesHolder(
            launchMissionBtn,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.025f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.025f)
        ).apply {
            duration = 1400L // 1.4s up + 1.4s down = 2.8s full sinusoidal cycle
            interpolator = AccelerateDecelerateInterpolator()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        column.addView(
            launchMissionBtn,
            LinearLayout.LayoutParams(dp(320f), dp(52f)).apply {
                topMargin = dp(14f)
            }
        )

        // 8. Two-line Tactical Instruction below button
        instructionText1 = TextView(context).apply {
            setTextColor(Color.parseColor("#555555"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        instructionText2 = TextView(context).apply {
            setTextColor(Color.parseColor("#444444"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        column.addView(
            instructionText1,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10f)
            }
        )
        column.addView(
            instructionText2,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2f)
            }
        )

        wrapper.addView(
            column,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        scroll.addView(wrapper)
        return scroll
    }

    private fun buildTechTabView(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20f), dp(12f), dp(20f), dp(12f))
            visibility = View.GONE
        }
    }

    private fun populateTechTab() {
        techTabContainer.removeAllViews()
        val isSk = campaignManager.language == "SK"

        val title = TextView(context).apply {
            text = if (isSk) "TECHNOLÓGIE ROJA" else "SWARM TECH LAB"
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        val sub = TextView(context).apply {
            text = if (isSk) "VYLEPŠI PARAMETRE JEDNOTIEK A ZÁKLADNÍ" else "UPGRADE UNIT & NODE COMBAT PARAMETERS"
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
        }
        techTabContainer.addView(title)
        techTabContainer.addView(sub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(4f)
            bottomMargin = dp(16f)
        })

        val names = if (isSk) {
            arrayOf("⚡ RÝCHLOSŤ LETU", "🔄 REGENERÁCIA UZLA", "🛡️ ŠTÍT ZÁKLADNE", "📦 MAX. KAPACITA")
        } else {
            arrayOf("⚡ THRUSTER SPEED", "🔄 NODE REGEN RATE", "🛡️ BASE SHIELDING", "📦 SWARM CAPACITY")
        }
        val levels = arrayOf(
            campaignManager.techSpeedLevel,
            campaignManager.techRegenLevel,
            campaignManager.techShieldLevel,
            campaignManager.techCapacityLevel
        )

        for (i in 0 until 4) {
            val lvl = levels[i]
            val cost = lvl * 250
            val canAfford = campaignManager.credits >= cost && lvl < 10

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = roundedBg(Color.parseColor("#121212"), Color.parseColor("#222222"), 1f, 10f)
                setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            }

            val leftCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            leftCol.addView(TextView(context).apply {
                text = names[i]
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            })
            leftCol.addView(TextView(context).apply {
                text = "LVL $lvl / 10"
                setTextColor(Color.parseColor("#4DF1FF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
            })

            val upgBtn = TextView(context).apply {
                text = if (lvl >= 10) "MAX" else "🪙 $cost"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (canAfford) Color.parseColor("#0A0401") else Color.parseColor("#666666"))
                background = roundedBg(if (canAfford) Color.parseColor("#FFC629") else Color.parseColor("#1E1E1E"), radiusDp = 6f)
                setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
                setOnClickListener { v ->
                    if (lvl < 10 && campaignManager.credits >= cost) {
                        campaignManager.credits -= cost
                        when (i) {
                            0 -> campaignManager.techSpeedLevel = lvl + 1
                            1 -> campaignManager.techRegenLevel = lvl + 1
                            2 -> campaignManager.techShieldLevel = lvl + 1
                            else -> campaignManager.techCapacityLevel = lvl + 1
                        }
                        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        engine.soundManager.playBaseCaptured()
                        refreshAllUi()
                    } else {
                        v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        engine.soundManager.playBaseHit()
                    }
                }
            }

            card.addView(leftCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(upgBtn, LinearLayout.LayoutParams(dp(92f), dp(36f)))

            techTabContainer.addView(
                card,
                LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10f)
                }
            )
        }
    }

    private fun buildArmoryTabView(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20f), dp(12f), dp(20f), dp(12f))
            visibility = View.GONE
        }
    }

    private fun populateArmoryTab() {
        armoryTabContainer.removeAllViews()
        val isSk = campaignManager.language == "SK"

        val title = TextView(context).apply {
            text = if (isSk) "ZBROJNICA & PROFIL" else "ARMORY & PILOT PROFILE"
            setTextColor(Color.parseColor("#4DF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        val sub = TextView(context).apply {
            text = if (isSk) "VÝBER VIZUÁLNEJ SCHÉMY FLOTILY" else "SELECT FLEET VISUAL CHROMA SKIN"
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
        }
        armoryTabContainer.addView(title)
        armoryTabContainer.addView(sub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(4f)
            bottomMargin = dp(16f)
        })

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
        val selectedSkin = campaignManager.selectedSkinIndex

        for (i in 0 until 4) {
            val isSel = i == selectedSkin
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = roundedBg(
                    Color.parseColor("#121212"),
                    if (isSel) skinColors[i] else Color.parseColor("#222222"),
                    if (isSel) 1.8f else 1f,
                    10f
                )
                setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
                setOnClickListener { v ->
                    campaignManager.selectedSkinIndex = i
                    v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    engine.soundManager.playDragSelect()
                    refreshAllUi()
                }
            }

            val swatch = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(skinColors[i])
                }
            }
            val nameTxt = TextView(context).apply {
                text = skinNames[i]
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                typeface = Typeface.DEFAULT_BOLD
            }
            val statusTxt = TextView(context).apply {
                text = if (isSel) (if (isSk) "AKTÍVNE" else "EQUIPPED") else (if (isSk) "VYBRAŤ" else "SELECT")
                setTextColor(if (isSel) Color.parseColor("#FFC629") else Color.parseColor("#666666"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                typeface = Typeface.DEFAULT_BOLD
            }

            card.addView(swatch, LinearLayout.LayoutParams(dp(22f), dp(22f)).apply { marginEnd = dp(12f) })
            card.addView(nameTxt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(statusTxt)

            armoryTabContainer.addView(
                card,
                LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10f)
                }
            )
        }
    }

    private fun buildLevelSelectView(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20f), dp(12f), dp(20f), dp(12f))
            visibility = View.GONE
        }
    }

    private fun populateLevelSelect() {
        levelSelectContainer.removeAllViews()
        val isSk = campaignManager.language == "SK"
        val unlocked = campaignManager.unlockedLevel

        val title = TextView(context).apply {
            text = if (isSk) "VÝBER SEKTORA" else "CAMPAIGN SECTORS"
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        levelSelectContainer.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(16f)
        })

        val levelTitles = listOf(
            "Sector 1: First Clash",
            "Sector 2: Triangulation",
            "Sector 3: Asteroid Chokepoints",
            "Sector 4: Quad Siege",
            "Sector 5: Total Dominion"
        )

        for (i in 0 until 5) {
            val isUnlocked = (i + 1) <= unlocked
            val isCurrent = (i + 1) == engine.currentLevelIndex

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = roundedBg(
                    if (isUnlocked) Color.parseColor("#121212") else Color.parseColor("#0C0C0C"),
                    when {
                        isCurrent -> Color.parseColor("#FFC629")
                        isUnlocked -> Color.parseColor("#2A2A2A")
                        else -> Color.parseColor("#1A1A1A")
                    },
                    if (isCurrent) 1.8f else 1f,
                    8f
                )
                setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
                setOnClickListener { v ->
                    if (isUnlocked) {
                        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        engine.soundManager.playDragSelect()
                        campaignManager.currentLevel = i + 1
                        engine.loadLevel(i + 1, context, startPlaying = true)
                        refreshAllUi()
                    } else {
                        v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        engine.soundManager.playBaseHit()
                    }
                }
            }

            val nameTxt = TextView(context).apply {
                text = levelTitles[i]
                setTextColor(if (isUnlocked) Color.WHITE else Color.parseColor("#555555"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
            }
            val statusTxt = TextView(context).apply {
                text = if (isUnlocked) (if (isSk) "SPUSTIŤ ▶" else "DEPLOY ▶") else (if (isSk) "ZAMKNUTÉ 🔒" else "LOCKED 🔒")
                setTextColor(if (isUnlocked) Color.parseColor("#4ADE80") else Color.parseColor("#666666"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
            }

            row.addView(nameTxt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(statusTxt)

            levelSelectContainer.addView(
                row,
                LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(8f)
                }
            )
        }

        val backBtn = TextView(context).apply {
            text = if (isSk) "◀ SPÄŤ DO MENU" else "◀ BACK TO MENU"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(Color.parseColor("#141414"), Color.parseColor("#2A2A2A"), 1f, 10f)
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.soundManager.playDragSelect()
                engine.screenState = ScreenState.MAIN_MENU
                refreshAllUi()
            }
        }
        levelSelectContainer.addView(
            backBtn,
            LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8f)
            }
        )
    }

    private fun buildBottomTabsBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0B0B0B"))
        }

        val icons = arrayOf("◎", "⚡", "🛡️")
        for (i in 0 until 3) {
            val tabCell = FrameLayout(context).apply {
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.MAIN_MENU
                    engine.activeMenuTab = i
                    refreshAllUi()
                }
            }

            // Topborder #222222 across the bar
            val topBorder = View(context).apply {
                setBackgroundColor(Color.parseColor("#222222"))
            }
            tabCell.addView(topBorder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f), Gravity.TOP))

            // Active top indicator bar (#FFC629)
            val activeIndicator = View(context).apply {
                background = roundedBg(Color.parseColor("#FFC629"), radiusDp = 2f)
            }
            bottomTabIndicators[i] = activeIndicator
            tabCell.addView(
                activeIndicator,
                FrameLayout.LayoutParams(dp(48f), dp(3f), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            )

            val contentCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            bottomTabContainers[i] = contentCol

            val iconTv = TextView(context).apply {
                text = icons[i]
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                gravity = Gravity.CENTER
            }
            val labelTv = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.10f
                gravity = Gravity.CENTER
            }
            bottomTabIcons[i] = iconTv
            bottomTabLabels[i] = labelTv

            contentCol.addView(iconTv)
            contentCol.addView(labelTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2f)
            })

            tabCell.addView(
                contentCol,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )

            bar.addView(tabCell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }

        return bar
    }

    private fun buildSettingsModal(): FrameLayout {
        val overlay = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(210, 0, 0, 0))
            setOnClickListener {
                visibility = View.GONE
            }
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundedBg(Color.parseColor("#121212"), Color.parseColor("#333333"), 1.5f, 12f)
            setPadding(dp(20f), dp(18f), dp(20f), dp(16f))
            isClickable = true
        }

        settingsTitleText = TextView(context).apply {
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
        }
        card.addView(settingsTitleText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(14f)
        })

        settingsLangBtn = createModalButton { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            engine.soundManager.playDragSelect()
            campaignManager.language = if (campaignManager.language == "SK") "EN" else "SK"
            refreshAllUi()
        }
        settingsSoundBtn = createModalButton { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            engine.soundManager.isMuted = !engine.soundManager.isMuted
            if (!engine.soundManager.isMuted) {
                engine.soundManager.playDragSelect()
            }
            refreshAllUi()
        }
        settingsResetBtn = createModalButton { v ->
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            engine.soundManager.playBaseLost()
            campaignManager.resetProgress()
            overlay.visibility = View.GONE
            refreshAllUi()
        }
        settingsCloseBtn = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12f), dp(10f), dp(12f), dp(6f))
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.soundManager.playDragSelect()
                overlay.visibility = View.GONE
            }
        }

        card.addView(settingsLangBtn, LinearLayout.LayoutParams(dp(250f), dp(40f)).apply { bottomMargin = dp(10f) })
        card.addView(settingsSoundBtn, LinearLayout.LayoutParams(dp(250f), dp(40f)).apply { bottomMargin = dp(10f) })
        card.addView(settingsResetBtn, LinearLayout.LayoutParams(dp(250f), dp(40f)).apply { bottomMargin = dp(8f) })
        card.addView(settingsCloseBtn)

        overlay.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        return overlay
    }

    private fun createModalButton(onClick: (View) -> Unit): TextView {
        return TextView(context).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(Color.parseColor("#1A1A1A"), Color.parseColor("#2A2A2A"), 1f, 8f)
            setOnClickListener(onClick)
        }
    }

    private fun buildInGameHudOverlay() {
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(16f), dp(14f), dp(8f))
        }

        ViewCompat.setOnApplyWindowInsetsListener(topBar) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val safeTop = maxOf(sysBars.top, dp(12f))
            view.setPadding(dp(14f), safeTop + dp(4f), dp(14f), dp(8f))
            insets
        }

        val leftStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        hudLevelTitleText = TextView(context).apply {
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
        }
        hudPowerBarView = PowerBarView(context, engine)

        leftStack.addView(hudLevelTitleText)
        leftStack.addView(
            hudPowerBarView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8f)).apply {
                topMargin = dp(5f)
            }
        )

        topBar.addView(
            leftStack,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(12f)
            }
        )

        // Action Buttons: Home, Restart, Pause
        val homeBtn = createHudIconButton("🏠") { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            engine.soundManager.playDragSelect()
            engine.isPaused = false
            engine.screenState = ScreenState.MAIN_MENU
            refreshAllUi()
        }
        val restartBtn = createHudIconButton("🔄") { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            engine.restartLevel(context)
            refreshAllUi()
        }
        hudPauseBtn = createHudIconButton("⏸") { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            engine.isPaused = !engine.isPaused
            refreshAllUi()
        }

        topBar.addView(homeBtn, LinearLayout.LayoutParams(dp(36f), dp(36f)).apply { marginEnd = dp(6f) })
        topBar.addView(restartBtn, LinearLayout.LayoutParams(dp(36f), dp(36f)).apply { marginEnd = dp(6f) })
        topBar.addView(hudPauseBtn, LinearLayout.LayoutParams(dp(36f), dp(36f)))

        inGameHudOverlay.addView(
            topBar,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        )

        // Victory / Defeat Modal Overlay
        gameOverModalOverlay = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(185, 9, 13, 22))
            visibility = View.GONE
            isClickable = true
        }

        gameOverCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24f), dp(22f), dp(24f), dp(20f))
        }

        gameOverTitleText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        gameOverSubText = TextView(context).apply {
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            gravity = Gravity.CENTER
        }
        gameOverPrimaryBtn = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (engine.status == GameStatus.VICTORY) {
                    onNextMission()
                } else {
                    engine.restartLevel(context)
                }
                refreshAllUi()
            }
        }
        gameOverHomeBtn = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(Color.parseColor("#1A1A1A"), Color.parseColor("#333333"), 1f, 10f)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.soundManager.playDragSelect()
                engine.screenState = ScreenState.MAIN_MENU
                refreshAllUi()
            }
        }

        gameOverCard.addView(gameOverTitleText)
        gameOverCard.addView(gameOverSubText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6f)
            bottomMargin = dp(18f)
        })
        gameOverCard.addView(gameOverPrimaryBtn, LinearLayout.LayoutParams(dp(210f), dp(40f)).apply { bottomMargin = dp(10f) })
        gameOverCard.addView(gameOverHomeBtn, LinearLayout.LayoutParams(dp(210f), dp(40f)))

        gameOverModalOverlay.addView(
            gameOverCard,
            FrameLayout.LayoutParams(dp(280f), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )

        inGameHudOverlay.addView(
            gameOverModalOverlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    private fun createHudIconButton(symbol: String, onClick: (View) -> Unit): TextView {
        return TextView(context).apply {
            text = symbol
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundedBg(Color.argb(140, 30, 41, 59), Color.argb(180, 71, 85, 105), 1.2f, 8f)
            setOnClickListener(onClick)
        }
    }

    fun refreshAllUi() {
        val isSk = campaignManager.language == "SK"
        val isPlaying = engine.screenState == ScreenState.PLAYING

        livingBackgroundView.visibility = if (isPlaying) View.GONE else View.VISIBLE
        menuOverlay.visibility = if (isPlaying) View.GONE else View.VISIBLE
        inGameHudOverlay.visibility = if (isPlaying) View.VISIBLE else View.GONE

        if (!isPlaying) {
            // Top HUD currencies
            creditsText.text = "🪙 ${campaignManager.formatNumber(campaignManager.credits)}"
            gemsText.text = "💎 ${campaignManager.formatNumber(campaignManager.gems)}"

            // Middle content visibility
            val isLevelSelect = engine.screenState == ScreenState.LEVEL_SELECT
            missionTabContainer.visibility = if (!isLevelSelect && engine.activeMenuTab == 0) View.VISIBLE else View.GONE
            techTabContainer.visibility = if (!isLevelSelect && engine.activeMenuTab == 1) View.VISIBLE else View.GONE
            armoryTabContainer.visibility = if (!isLevelSelect && engine.activeMenuTab == 2) View.VISIBLE else View.GONE
            levelSelectContainer.visibility = if (isLevelSelect) View.VISIBLE else View.GONE

            // Mission tab texts
            val lvl = campaignManager.playerLevel
            val rank = campaignManager.getRankName()
            val sectors = campaignManager.sectorsConquered
            val winRate = String.format(Locale.US, "%.1f%%", campaignManager.getWinRatePercent())
            val streak = campaignManager.winStreak

            rankBadgeText.text = if (isSk) "$rank • ÚROVEŇ $lvl" else "$rank • LVL $lvl"
            subtitleText.text = if (isSk) "TAKTICKÁ DOBÝVACIA STRATÉGIA V REÁLNOM ČASE" else "REAL-TIME TACTICAL SWARM CONQUEST"
            statCardTitleText.text = if (isSk) "ÚROVEŇ HRÁČA" else "PLAYER LEVEL"
            statCardLevelText.text = "LVL $lvl"
            statCardSectorsText.text = if (isSk) "$sectors DOBYTÝCH SEKTOROV" else "$sectors SECTORS CONQUERED"
            statCardWinRateText.text = if (isSk) "ÚSPEŠNOSŤ: $winRate" else "WIN RATE: $winRate"
            statCardStreakText.text = if (isSk) "SÉRIA: $streak VÍŤAZSTIEV" else "STREAK: $streak WINS"

            val lastSec = campaignManager.lastSector
            val lastPts = campaignManager.formatNumber(campaignManager.lastScore)
            val lastCr = campaignManager.lastCreditsReward
            val lastGm = campaignManager.lastGemsReward
            lastSectorLeftText.text = if (isSk) "POSL. SEKTOR $lastSec ▾" else "LAST SECTOR $lastSec ▾"
            lastSectorRightText.text = "$lastPts PTS   +$lastCr 🪙   +$lastGm 💎"

            launchMissionBtn.text = if (isSk) "▶   SPUSTIŤ MISIU" else "▶   LAUNCH MISSION"
            instructionText1.text = if (isSk) "POTIAHNI ZO ZÁKLADNE PRE ÚTOK // ROJOVÁ OFENZÍVA" else "DRAG FROM BASE TO ATTACK // SWARM CONQUEST"
            instructionText2.text = if (isSk) "SPOJ VIACERO ZÁKLADNÍ JEDNÝM ŤAHOM" else "CONNECT MULTIPLE NODES TO SWARM"

            if (engine.activeMenuTab == 1) populateTechTab()
            if (engine.activeMenuTab == 2) populateArmoryTab()
            if (isLevelSelect) populateLevelSelect()

            // Bottom Tabs state
            val tabTitles = if (isSk) arrayOf("MISIA", "TECH", "PROFIL") else arrayOf("MISSION", "TECH", "ARMORY")
            for (i in 0 until 3) {
                val active = !isLevelSelect && engine.activeMenuTab == i
                bottomTabIndicators[i]?.visibility = if (active) View.VISIBLE else View.INVISIBLE
                bottomTabIcons[i]?.setTextColor(if (active) Color.parseColor("#FFC629") else Color.parseColor("#666666"))
                bottomTabLabels[i]?.text = tabTitles[i]
                bottomTabLabels[i]?.setTextColor(if (active) Color.parseColor("#FFC629") else Color.parseColor("#666666"))
                bottomTabContainers[i]?.translationY = if (active) -dpF(2f) else 0f
                bottomTabContainers[i]?.setBackgroundColor(if (active) Color.argb(18, 255, 198, 41) else Color.TRANSPARENT)
            }

            // Settings Modal texts
            settingsTitleText.text = if (isSk) "NASTAVENIA" else "SETTINGS"
            settingsLangBtn.text = if (isSk) "JAZYK / LANGUAGE: SK 🇸🇰" else "LANGUAGE / JAZYK: EN 🇬🇧"
            settingsLangBtn.setTextColor(Color.parseColor("#4DF1FF"))

            val muted = engine.soundManager.isMuted
            settingsSoundBtn.text = if (isSk) {
                if (muted) "ZVUKY (SFX): VYPNUTÉ 🔇" else "ZVUKY (SFX): ZAPNUTÉ 🔊"
            } else {
                if (muted) "SOUND SFX: MUTED 🔇" else "SOUND SFX: ACTIVE 🔊"
            }
            settingsSoundBtn.setTextColor(if (muted) Color.parseColor("#EF4444") else Color.parseColor("#4ADE80"))

            settingsResetBtn.text = if (isSk) "RESETOVAŤ PROGRES ⚠" else "RESET PROGRESS ⚠"
            settingsResetBtn.setTextColor(Color.parseColor("#FF3366"))
            settingsCloseBtn.text = if (isSk) "ZAVRIEŤ [✕]" else "CLOSE [✕]"
        } else {
            // In-Game HUD state
            hudLevelTitleText.text = engine.levelName.uppercase()
            hudPauseBtn.text = if (engine.isPaused) "▶" else "⏸"
            hudPowerBarView.invalidate()

            if (engine.status != GameStatus.RUNNING) {
                gameOverModalOverlay.visibility = View.VISIBLE
                val isWin = engine.status == GameStatus.VICTORY
                val accent = if (isWin) Color.parseColor("#FFC629") else Color.parseColor("#FF3366")
                gameOverCard.background = roundedBg(Color.parseColor("#121212"), accent, 2f, 16f)
                gameOverTitleText.text = if (isWin) (if (isSk) "VÍŤAZSTVO!" else "VICTORY!") else (if (isSk) "PREHRA" else "DEFEAT")
                gameOverTitleText.setTextColor(accent)
                gameOverSubText.text = if (isWin) {
                    if (isSk) "Sektor obsadený! +${campaignManager.lastCreditsReward} 🪙  +${campaignManager.lastGemsReward} 💎"
                    else "Sector secured! +${campaignManager.lastCreditsReward} 🪙  +${campaignManager.lastGemsReward} 💎"
                } else {
                    if (isSk) "Všetky veliteľské základne boli stratené." else "All command nodes were lost."
                }
                gameOverPrimaryBtn.text = if (isWin) {
                    if (isSk) "ĎALŠIA MISIA ▶" else "NEXT MISSION ▶"
                } else {
                    if (isSk) "SKÚSIŤ ZNOVA 🔄" else "RETRY MISSION 🔄"
                }
                gameOverPrimaryBtn.setTextColor(if (isWin) Color.parseColor("#0A0401") else Color.WHITE)
                gameOverPrimaryBtn.background = roundedBg(if (isWin) Color.parseColor("#FFC629") else Color.parseColor("#E11D48"), radiusDp = 10f)
                gameOverHomeBtn.text = if (isSk) "HLAVNÉ MENU 🏠" else "MAIN MENU 🏠"
            } else {
                gameOverModalOverlay.visibility = View.GONE
            }
        }
    }

    private fun startHudSyncTicker() {
        val runnable = object : Runnable {
            private var lastStatus = engine.status
            private var lastScreen = engine.screenState

            override fun run() {
                if (engine.screenState == ScreenState.PLAYING) {
                    hudPowerBarView.invalidate()
                }
                if (engine.status != lastStatus || engine.screenState != lastScreen) {
                    lastStatus = engine.status
                    lastScreen = engine.screenState
                    refreshAllUi()
                }
                rootLayout.postDelayed(this, 100L)
            }
        }
        rootLayout.post(runnable)
    }

    private class PowerBarView(
        context: Context,
        private val engine: GameEngine
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            paint.color = Color.argb(60, 255, 255, 255)
            canvas.drawRoundRect(0f, 0f, w, h, h / 2f, h / 2f, paint)

            val power = engine.getFactionPowerCounts()
            val totalUnits = power.values.sum().coerceAtLeast(1)

            val sortedFactions = power.keys.sortedWith { a, b ->
                when {
                    a == 1 -> -1
                    b == 1 -> 1
                    a == 0 -> 1
                    b == 0 -> -1
                    else -> a.compareTo(b)
                }
            }

            var currentX = 0f
            for (factionId in sortedFactions) {
                val count = power[factionId] ?: 0
                if (count > 0) {
                    val segW = (count.toFloat() / totalUnits.toFloat()) * w
                    paint.color = GameConfig.getFaction(factionId).primaryColor
                    canvas.drawRect(currentX, 0f, currentX + segW, h, paint)
                    currentX += segW
                }
            }
        }
    }
}

