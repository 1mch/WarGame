package cdev.wargame.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cdev.wargame.R
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
    private lateinit var launchMissionBtn: LinearLayout
    private lateinit var launchMissionBtnText: TextView
    private lateinit var instructionText1: TextView
    private lateinit var instructionText2: TextView

    // Middle tab containers
    private lateinit var missionTabContainer: View
    private lateinit var techTabContainer: LinearLayout
    private lateinit var armoryTabContainer: LinearLayout
    private lateinit var levelSelectContainer: LinearLayout

    // Bottom Navigation Tab views
    private val bottomTabIndicators = arrayOfNulls<View>(3)
    private val bottomTabIcons = arrayOfNulls<ImageView>(3)
    private val bottomTabLabels = arrayOfNulls<TextView>(3)
    private val bottomTabContainers = arrayOfNulls<LinearLayout>(3)

    // Settings Modal overlay
    private lateinit var settingsModalOverlay: FrameLayout
    private lateinit var settingsTitleText: TextView
    private lateinit var settingsLangBtnText: TextView
    private lateinit var settingsSoundBtnIcon: ImageView
    private lateinit var settingsSoundBtnText: TextView
    private lateinit var settingsResetBtnText: TextView
    private lateinit var settingsCloseBtn: TextView

    // In-Game HUD views
    private lateinit var hudLevelTitleText: TextView
    private lateinit var hudPowerBarView: PowerBarView
    private lateinit var hudPauseIcon: ImageView
    private lateinit var gameOverModalOverlay: FrameLayout
    private lateinit var gameOverCard: LinearLayout
    private lateinit var gameOverTitleText: TextView
    private lateinit var gameOverSubText: TextView
    private lateinit var gameOverPrimaryBtn: LinearLayout
    private lateinit var gameOverPrimaryIcon: ImageView
    private lateinit var gameOverPrimaryText: TextView
    private lateinit var gameOverHomeText: TextView

    private val fontBold: Typeface = AppFonts.bold(context)
    private val fontBlack: Typeface = AppFonts.black(context)

    init {
        buildHierarchy()
        applyInterFontRecursively(rootLayout)
        refreshAllUi()
        startHudSyncTicker()
    }

    private fun applyInterFontRecursively(view: View) {
        when (view) {
            is TextView -> {
                if (view.typeface != fontBlack) {
                    view.typeface = fontBold
                }
            }
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    applyInterFontRecursively(view.getChildAt(i))
                }
            }
        }
    }

    private fun localizedResources(): Resources {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(campaignManager.language.lowercase(Locale.ROOT)))
        return context.createConfigurationContext(config).resources
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

    private fun createLucideIcon(@DrawableRes resId: Int, sizeDp: Float, tintColor: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(resId)
            setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        }
    }

    private fun roundedBg(
        fillColor: Int,
        strokeColor: Int = Color.TRANSPARENT,
        strokeWidthDp: Float = 0f,
        radiusDp: Float = 10f
    ): GradientDrawable {
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

        // Currency Pill (Left) with Monochrome Lucide Icons (ic_coins & ic_gem)
        val currencyPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBg(Color.parseColor("#0A0A0A"), Color.parseColor("#222222"), 1f, 18f)
            setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
        }

        val coinsIcon = createLucideIcon(R.drawable.ic_coins, 14f, Color.parseColor("#FFC629")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(6f)
        }
        creditsText = TextView(context).apply {
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val pillDivider = View(context).apply {
            setBackgroundColor(Color.parseColor("#333333"))
        }
        val gemIcon = createLucideIcon(R.drawable.ic_gem, 14f, Color.parseColor("#4DF1FF")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(6f)
        }
        gemsText = TextView(context).apply {
            setTextColor(Color.parseColor("#4DF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }

        currencyPill.addView(coinsIcon)
        currencyPill.addView(creditsText)
        currencyPill.addView(
            pillDivider,
            LinearLayout.LayoutParams(dp(1f), dp(14f)).apply {
                marginStart = dp(10f)
                marginEnd = dp(10f)
            }
        )
        currencyPill.addView(gemIcon)
        currencyPill.addView(gemsText)

        topHudBar.addView(
            currencyPill,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34f), Gravity.START or Gravity.CENTER_VERTICAL)
        )

        // Settings Circular Button (Right) with Lucide ic_settings
        val settingsBtn = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0A0A0A"))
                setStroke(dp(1f), Color.parseColor("#222222"))
            }
            addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.ic_settings)
                    setColorFilter(Color.parseColor("#CCCCCC"), PorterDuff.Mode.SRC_IN)
                },
                FrameLayout.LayoutParams(dp(18f), dp(18f), Gravity.CENTER)
            )
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
            val rawLogo = context.getString(R.string.logo_title)
            val span = SpannableString(rawLogo)
            val dotIdx = rawLogo.indexOf('.')
            if (dotIdx > 0) {
                span.setSpan(ForegroundColorSpan(Color.WHITE), 0, dotIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.setSpan(ForegroundColorSpan(Color.parseColor("#00D2FF")), dotIdx, rawLogo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            text = span
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = fontBlack
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
            typeface = fontBlack
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
        val radarMiniIcon = createLucideIcon(R.drawable.ic_crosshair, 13f, Color.parseColor("#94A3B8")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(6f)
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
        lastSectorPill.addView(radarMiniIcon)
        lastSectorPill.addView(lastSectorLeftText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        lastSectorPill.addView(lastSectorRightText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        column.addView(
            lastSectorPill,
            LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12f)
            }
        )

        // 7. Main Button: "LAUNCH MISSION" with Lucide ic_play and 2.8s Breathing Pulse
        launchMissionBtn = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#FFC629"), radiusDp = 10f)
            elevation = dpF(8f)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                engine.soundManager.playUnitsDispatched()
                engine.startCampaign(context)
                refreshAllUi()
            }
        }
        val playIcon = createLucideIcon(R.drawable.ic_play, 16f, Color.parseColor("#0A0401")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(10f)
        }
        launchMissionBtnText = TextView(context).apply {
            setTextColor(Color.parseColor("#0A0401"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = fontBlack
            letterSpacing = 0.12f
        }
        launchMissionBtn.addView(playIcon)
        launchMissionBtn.addView(launchMissionBtnText)

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

    private fun populateTechTab(res: Resources) {
        techTabContainer.removeAllViews()

        val title = TextView(context).apply {
            text = res.getString(R.string.tech_title)
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        val sub = TextView(context).apply {
            text = res.getString(R.string.tech_subtitle)
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

        val iconResIds = intArrayOf(
            R.drawable.ic_zap,
            R.drawable.ic_rotate_ccw,
            R.drawable.ic_shield,
            R.drawable.ic_box
        )
        val nameResIds = intArrayOf(
            R.string.tech_speed,
            R.string.tech_regen,
            R.string.tech_shield,
            R.string.tech_capacity
        )
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

            val iconView = createLucideIcon(iconResIds[i], 20f, Color.parseColor("#4DF1FF")).apply {
                (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(12f)
            }

            val leftCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            leftCol.addView(TextView(context).apply {
                text = res.getString(nameResIds[i])
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
            })
            leftCol.addView(TextView(context).apply {
                text = res.getString(R.string.tech_level_format, lvl)
                setTextColor(Color.parseColor("#4DF1FF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
            })

            val upgBtn = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = roundedBg(if (canAfford) Color.parseColor("#FFC629") else Color.parseColor("#1E1E1E"), radiusDp = 6f)
                setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
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
            val fgColor = if (canAfford) Color.parseColor("#0A0401") else Color.parseColor("#666666")
            if (lvl < 10) {
                upgBtn.addView(createLucideIcon(R.drawable.ic_coins, 12f, fgColor).apply {
                    (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(5f)
                })
            }
            upgBtn.addView(TextView(context).apply {
                text = if (lvl >= 10) res.getString(R.string.tech_max) else res.getString(R.string.tech_cost_format, cost)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(fgColor)
            })

            card.addView(iconView)
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

    private fun populateArmoryTab(res: Resources) {
        armoryTabContainer.removeAllViews()

        val title = TextView(context).apply {
            text = res.getString(R.string.armory_title)
            setTextColor(Color.parseColor("#4DF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        val sub = TextView(context).apply {
            text = res.getString(R.string.armory_subtitle)
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

        val skinNameResIds = intArrayOf(
            R.string.skin_cyan,
            R.string.skin_gold,
            R.string.skin_emerald,
            R.string.skin_violet
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
                text = res.getString(skinNameResIds[i])
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                typeface = Typeface.DEFAULT_BOLD
            }
            val statusTxt = TextView(context).apply {
                text = res.getString(if (isSel) R.string.skin_equipped else R.string.skin_select)
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

    private fun populateLevelSelect(res: Resources) {
        levelSelectContainer.removeAllViews()
        val unlocked = campaignManager.unlockedLevel

        val title = TextView(context).apply {
            text = res.getString(R.string.sectors_title)
            setTextColor(Color.parseColor("#FFC629"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.10f
            gravity = Gravity.CENTER
        }
        levelSelectContainer.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(16f)
        })

        val levelTitleResIds = intArrayOf(
            R.string.sector_1_title,
            R.string.sector_2_title,
            R.string.sector_3_title,
            R.string.sector_4_title,
            R.string.sector_5_title
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
                text = res.getString(levelTitleResIds[i])
                setTextColor(if (isUnlocked) Color.WHITE else Color.parseColor("#555555"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
            }

            val statusRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val statusColor = if (isUnlocked) Color.parseColor("#4ADE80") else Color.parseColor("#666666")
            val statusTxt = TextView(context).apply {
                text = res.getString(if (isUnlocked) R.string.sector_deploy else R.string.sector_locked)
                setTextColor(statusColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
            }
            val statusIcon = createLucideIcon(
                if (isUnlocked) R.drawable.ic_play else R.drawable.ic_lock,
                12f,
                statusColor
            ).apply {
                (layoutParams as LinearLayout.LayoutParams).marginStart = dp(6f)
            }
            statusRow.addView(statusTxt)
            statusRow.addView(statusIcon)

            row.addView(nameTxt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(statusRow)

            levelSelectContainer.addView(
                row,
                LinearLayout.LayoutParams(dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(8f)
                }
            )
        }

        val backBtn = TextView(context).apply {
            text = res.getString(R.string.back_to_menu)
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

        // Monochrome Lucide Icons for Bottom Tabs: Crosshair (Mission), Zap (Tech), Shield (Armory)
        val iconResIds = intArrayOf(
            R.drawable.ic_crosshair,
            R.drawable.ic_zap,
            R.drawable.ic_shield
        )
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

            // Top border #222222 across the bar
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

            val iconIv = ImageView(context).apply {
                setImageResource(iconResIds[i])
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val labelTv = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.10f
                gravity = Gravity.CENTER
            }
            bottomTabIcons[i] = iconIv
            bottomTabLabels[i] = labelTv

            contentCol.addView(iconIv, LinearLayout.LayoutParams(dp(20f), dp(20f)))
            contentCol.addView(labelTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(5f)
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

        // 1. Language Row
        val langRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#1A1A1A"), Color.parseColor("#2A2A2A"), 1f, 8f)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                engine.soundManager.playDragSelect()
                campaignManager.language = if (campaignManager.language.equals("sk", ignoreCase = true)) "en" else "sk"
                refreshAllUi()
            }
        }
        val langIcon = createLucideIcon(R.drawable.ic_globe, 15f, Color.parseColor("#4DF1FF")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8f)
        }
        settingsLangBtnText = TextView(context).apply {
            setTextColor(Color.parseColor("#4DF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.DEFAULT_BOLD
        }
        langRow.addView(langIcon)
        langRow.addView(settingsLangBtnText)

        // 2. Sound Row
        val soundRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#1A1A1A"), Color.parseColor("#2A2A2A"), 1f, 8f)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                engine.soundManager.isMuted = !engine.soundManager.isMuted
                if (!engine.soundManager.isMuted) {
                    engine.soundManager.playDragSelect()
                }
                refreshAllUi()
            }
        }
        settingsSoundBtnIcon = createLucideIcon(R.drawable.ic_volume_on, 15f, Color.parseColor("#4ADE80")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8f)
        }
        settingsSoundBtnText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.DEFAULT_BOLD
        }
        soundRow.addView(settingsSoundBtnIcon)
        soundRow.addView(settingsSoundBtnText)

        // 3. Reset Progress Row
        val resetRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#1A1A1A"), Color.parseColor("#2A2A2A"), 1f, 8f)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                engine.soundManager.playBaseLost()
                campaignManager.resetProgress()
                overlay.visibility = View.GONE
                refreshAllUi()
            }
        }
        val resetIcon = createLucideIcon(R.drawable.ic_trash, 15f, Color.parseColor("#FF3366")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8f)
        }
        settingsResetBtnText = TextView(context).apply {
            setTextColor(Color.parseColor("#FF3366"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.DEFAULT_BOLD
        }
        resetRow.addView(resetIcon)
        resetRow.addView(settingsResetBtnText)

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

        card.addView(langRow, LinearLayout.LayoutParams(dp(250f), dp(40f)).apply { bottomMargin = dp(10f) })
        card.addView(soundRow, LinearLayout.LayoutParams(dp(250f), dp(40f)).apply { bottomMargin = dp(10f) })
        card.addView(resetRow, LinearLayout.LayoutParams(dp(250f), dp(40f)).apply { bottomMargin = dp(8f) })
        card.addView(settingsCloseBtn)

        overlay.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        return overlay
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

        // Action Buttons with Lucide Icons: Home, Restart, Pause
        val homeBtn = createHudLucideButton(R.drawable.ic_home) { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            engine.soundManager.playDragSelect()
            engine.isPaused = false
            engine.screenState = ScreenState.MAIN_MENU
            refreshAllUi()
        }
        val restartBtn = createHudLucideButton(R.drawable.ic_rotate_ccw) { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            engine.restartLevel(context)
            refreshAllUi()
        }
        hudPauseIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_pause)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        val pauseBtn = FrameLayout(context).apply {
            background = roundedBg(Color.argb(140, 30, 41, 59), Color.argb(180, 71, 85, 105), 1.2f, 8f)
            addView(hudPauseIcon, FrameLayout.LayoutParams(dp(16f), dp(16f), Gravity.CENTER))
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.isPaused = !engine.isPaused
                refreshAllUi()
            }
        }

        topBar.addView(homeBtn, LinearLayout.LayoutParams(dp(36f), dp(36f)).apply { marginEnd = dp(6f) })
        topBar.addView(restartBtn, LinearLayout.LayoutParams(dp(36f), dp(36f)).apply { marginEnd = dp(6f) })
        topBar.addView(pauseBtn, LinearLayout.LayoutParams(dp(36f), dp(36f)))

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
        gameOverPrimaryBtn = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
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
        gameOverPrimaryIcon = createLucideIcon(R.drawable.ic_play, 14f, Color.parseColor("#0A0401")).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8f)
        }
        gameOverPrimaryText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.DEFAULT_BOLD
        }
        gameOverPrimaryBtn.addView(gameOverPrimaryIcon)
        gameOverPrimaryBtn.addView(gameOverPrimaryText)

        val homeModalBtn = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#1A1A1A"), Color.parseColor("#333333"), 1f, 10f)
            setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                engine.soundManager.playDragSelect()
                engine.screenState = ScreenState.MAIN_MENU
                refreshAllUi()
            }
        }
        val homeModalIcon = createLucideIcon(R.drawable.ic_home, 14f, Color.WHITE).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8f)
        }
        gameOverHomeText = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.DEFAULT_BOLD
        }
        homeModalBtn.addView(homeModalIcon)
        homeModalBtn.addView(gameOverHomeText)

        gameOverCard.addView(gameOverTitleText)
        gameOverCard.addView(gameOverSubText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6f)
            bottomMargin = dp(18f)
        })
        gameOverCard.addView(gameOverPrimaryBtn, LinearLayout.LayoutParams(dp(210f), dp(40f)).apply { bottomMargin = dp(10f) })
        gameOverCard.addView(homeModalBtn, LinearLayout.LayoutParams(dp(210f), dp(40f)))

        gameOverModalOverlay.addView(
            gameOverCard,
            FrameLayout.LayoutParams(dp(280f), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )

        inGameHudOverlay.addView(
            gameOverModalOverlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    private fun createHudLucideButton(@DrawableRes iconRes: Int, onClick: (View) -> Unit): FrameLayout {
        return FrameLayout(context).apply {
            background = roundedBg(Color.argb(140, 30, 41, 59), Color.argb(180, 71, 85, 105), 1.2f, 8f)
            addView(
                ImageView(context).apply {
                    setImageResource(iconRes)
                    setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                },
                FrameLayout.LayoutParams(dp(16f), dp(16f), Gravity.CENTER)
            )
            setOnClickListener(onClick)
        }
    }

    fun refreshAllUi() {
        val res = localizedResources()
        val isPlaying = engine.screenState == ScreenState.PLAYING

        livingBackgroundView.visibility = if (isPlaying) View.GONE else View.VISIBLE
        menuOverlay.visibility = if (isPlaying) View.GONE else View.VISIBLE
        inGameHudOverlay.visibility = if (isPlaying) View.VISIBLE else View.GONE

        if (!isPlaying) {
            // Top HUD currencies
            creditsText.text = res.getString(R.string.currency_credits_format, campaignManager.formatNumber(campaignManager.credits))
            gemsText.text = res.getString(R.string.currency_gems_format, campaignManager.formatNumber(campaignManager.gems))

            // Middle content visibility
            val isLevelSelect = engine.screenState == ScreenState.LEVEL_SELECT
            missionTabContainer.visibility = if (!isLevelSelect && engine.activeMenuTab == 0) View.VISIBLE else View.GONE
            techTabContainer.visibility = if (!isLevelSelect && engine.activeMenuTab == 1) View.VISIBLE else View.GONE
            armoryTabContainer.visibility = if (!isLevelSelect && engine.activeMenuTab == 2) View.VISIBLE else View.GONE
            levelSelectContainer.visibility = if (isLevelSelect) View.VISIBLE else View.GONE

            // Mission tab texts from strings.xml / values-sk/strings.xml
            val lvl = campaignManager.playerLevel
            val rank = res.getString(campaignManager.getRankResId())
            val sectors = campaignManager.sectorsConquered
            val winRate = String.format(Locale.US, "%.1f%%", campaignManager.getWinRatePercent())
            val streak = campaignManager.winStreak

            rankBadgeText.text = res.getString(R.string.rank_badge_format, rank, lvl)
            subtitleText.text = res.getString(R.string.logo_subtitle)
            statCardTitleText.text = res.getString(R.string.player_level_label)
            statCardLevelText.text = res.getString(R.string.player_level_value, lvl)
            statCardSectorsText.text = res.getString(R.string.sectors_conquered_format, sectors)
            statCardWinRateText.text = res.getString(R.string.win_rate_format, winRate)
            statCardStreakText.text = res.getString(R.string.streak_format, streak)

            val lastSec = campaignManager.lastSector
            val lastPts = campaignManager.formatNumber(campaignManager.lastScore)
            val lastCr = campaignManager.lastCreditsReward
            val lastGm = campaignManager.lastGemsReward
            lastSectorLeftText.text = res.getString(R.string.last_sector_format, lastSec)
            lastSectorRightText.text = res.getString(R.string.last_sector_rewards_format, lastPts, lastCr, lastGm)

            launchMissionBtnText.text = res.getString(R.string.launch_mission)
            instructionText1.text = res.getString(R.string.tactical_hint_1)
            instructionText2.text = res.getString(R.string.tactical_hint_2)

            if (engine.activeMenuTab == 1) {
                populateTechTab(res)
                applyInterFontRecursively(techTabContainer)
            }
            if (engine.activeMenuTab == 2) {
                populateArmoryTab(res)
                applyInterFontRecursively(armoryTabContainer)
            }
            if (isLevelSelect) {
                populateLevelSelect(res)
                applyInterFontRecursively(levelSelectContainer)
            }

            // Bottom Tabs state (Monochrome Lucide icons tinted #FFC629 when active, #666666 when inactive)
            val tabTitleResIds = intArrayOf(R.string.tab_mission, R.string.tab_tech, R.string.tab_armory)
            for (i in 0 until 3) {
                val active = !isLevelSelect && engine.activeMenuTab == i
                val tintColor = if (active) Color.parseColor("#FFC629") else Color.parseColor("#666666")
                bottomTabIndicators[i]?.visibility = if (active) View.VISIBLE else View.INVISIBLE
                bottomTabIcons[i]?.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                bottomTabLabels[i]?.text = res.getString(tabTitleResIds[i])
                bottomTabLabels[i]?.setTextColor(tintColor)
                bottomTabContainers[i]?.translationY = if (active) -dpF(2f) else 0f
                bottomTabContainers[i]?.setBackgroundColor(if (active) Color.argb(18, 255, 198, 41) else Color.TRANSPARENT)
            }

            // Settings Modal texts & monochrome Lucide icons
            settingsTitleText.text = res.getString(R.string.settings_title)
            settingsLangBtnText.text = res.getString(R.string.settings_language)

            val muted = engine.soundManager.isMuted
            val soundColor = if (muted) Color.parseColor("#EF4444") else Color.parseColor("#4ADE80")
            settingsSoundBtnIcon.setImageResource(if (muted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
            settingsSoundBtnIcon.setColorFilter(soundColor, PorterDuff.Mode.SRC_IN)
            settingsSoundBtnText.text = res.getString(if (muted) R.string.settings_sound_muted else R.string.settings_sound_active)
            settingsSoundBtnText.setTextColor(soundColor)

            settingsResetBtnText.text = res.getString(R.string.settings_reset_progress)
            settingsCloseBtn.text = res.getString(R.string.settings_close)
        } else {
            // In-Game HUD state
            hudLevelTitleText.text = engine.levelName.uppercase(Locale.ROOT)
            hudPauseIcon.setImageResource(if (engine.isPaused) R.drawable.ic_play else R.drawable.ic_pause)
            hudPowerBarView.invalidate()

            if (engine.status != GameStatus.RUNNING) {
                gameOverModalOverlay.visibility = View.VISIBLE
                val isWin = engine.status == GameStatus.VICTORY
                val accent = if (isWin) Color.parseColor("#FFC629") else Color.parseColor("#FF3366")
                val btnTextColor = if (isWin) Color.parseColor("#0A0401") else Color.WHITE

                gameOverCard.background = roundedBg(Color.parseColor("#121212"), accent, 2f, 16f)
                gameOverTitleText.text = res.getString(if (isWin) R.string.victory_title else R.string.defeat_title)
                gameOverTitleText.setTextColor(accent)
                gameOverSubText.text = if (isWin) {
                    res.getString(R.string.victory_subtitle_format, campaignManager.lastCreditsReward, campaignManager.lastGemsReward)
                } else {
                    res.getString(R.string.defeat_subtitle)
                }

                gameOverPrimaryIcon.setImageResource(if (isWin) R.drawable.ic_play else R.drawable.ic_rotate_ccw)
                gameOverPrimaryIcon.setColorFilter(btnTextColor, PorterDuff.Mode.SRC_IN)
                gameOverPrimaryText.text = res.getString(if (isWin) R.string.next_mission else R.string.retry_mission)
                gameOverPrimaryText.setTextColor(btnTextColor)
                gameOverPrimaryBtn.background = roundedBg(if (isWin) Color.parseColor("#FFC629") else Color.parseColor("#E11D48"), radiusDp = 10f)
                gameOverHomeText.text = res.getString(R.string.main_menu_btn)
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
