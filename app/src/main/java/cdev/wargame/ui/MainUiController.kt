package cdev.wargame.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cdev.wargame.R
import cdev.wargame.core.CampaignManager
import cdev.wargame.core.GameConfig
import cdev.wargame.core.GameEngine
import cdev.wargame.core.GameStatus
import cdev.wargame.core.ScreenState
import cdev.wargame.ui.components.BottomNavBar
import cdev.wargame.ui.components.CurrencyPill
import cdev.wargame.ui.components.HudIconButton
import cdev.wargame.ui.components.IconCircleButton
import cdev.wargame.ui.components.PrimaryGlowPillButton
import cdev.wargame.ui.components.PulsingRankBadge
import cdev.wargame.ui.components.SecondaryButton
import cdev.wargame.ui.components.WarGameCard
import cdev.wargame.ui.theme.WarGameColors
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun WarGameRootScreen(
    engine: GameEngine,
    campaignManager: CampaignManager,
    gameSurfaceView: GameSurfaceView,
    uiTickTrigger: Int,
    onNextMission: () -> Unit
) {
    val context = LocalContext.current
    val hostView = LocalView.current

    var refreshCounter by remember { mutableIntStateOf(0) }
    var screenState by remember { mutableStateOf(engine.screenState) }
    var gameStatus by remember { mutableStateOf(engine.status) }
    var isPaused by remember { mutableStateOf(engine.isPaused) }
    var activeTab by remember { mutableIntStateOf(engine.activeMenuTab) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var powerCounts by remember { mutableStateOf(engine.getFactionPowerCounts()) }

    // Periodic sync with GameEngine state
    LaunchedEffect(uiTickTrigger) {
        while (true) {
            screenState = engine.screenState
            gameStatus = engine.status
            isPaused = engine.isPaused
            if (screenState == ScreenState.PLAYING) {
                powerCounts = engine.getFactionPowerCounts()
            }
            delay(80L)
        }
    }

    val res = remember(campaignManager.language, refreshCounter, uiTickTrigger) {
        localizedResources(context, campaignManager.language)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 2D Battlefield SurfaceView (renders bases + base text, units, obstacles, tethers, ripples, particles)
        AndroidView(
            factory = { gameSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        if (screenState != ScreenState.PLAYING) {
            // 2. Living Background View (Cosmic Dust + Counter-Rotating Cyber Radar)
            AndroidView(
                factory = { ctx -> LivingBackgroundView(ctx) },
                modifier = Modifier.fillMaxSize()
            )

            // 3. Main Menu Compose UI
            MainMenuComposeOverlay(
                res = res,
                engine = engine,
                campaignManager = campaignManager,
                screenState = screenState,
                activeTab = activeTab,
                isSettingsOpen = isSettingsOpen,
                onTabChange = { tab ->
                    hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.MAIN_MENU
                    engine.activeMenuTab = tab
                    activeTab = tab
                    screenState = ScreenState.MAIN_MENU
                },
                onOpenSettings = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    isSettingsOpen = true
                },
                onCloseSettings = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    isSettingsOpen = false
                },
                onLaunchMission = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    engine.soundManager.playUnitsDispatched()
                    engine.startCampaign(context)
                    screenState = engine.screenState
                    gameStatus = engine.status
                },
                onOpenLevelSelect = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.LEVEL_SELECT
                    screenState = ScreenState.LEVEL_SELECT
                },
                onBackToMenu = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.screenState = ScreenState.MAIN_MENU
                    screenState = ScreenState.MAIN_MENU
                },
                onSelectLevel = { levelIdx ->
                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    engine.soundManager.playDragSelect()
                    campaignManager.currentLevel = levelIdx
                    engine.loadLevel(levelIdx, context, startPlaying = true)
                    screenState = engine.screenState
                    gameStatus = engine.status
                },
                onStateMutated = { refreshCounter++ }
            )
        } else {
            // 4. In-Game HUD & Victory/Defeat Modal Compose UI
            InGameHudComposeOverlay(
                res = res,
                engine = engine,
                campaignManager = campaignManager,
                gameStatus = gameStatus,
                isPaused = isPaused,
                powerCounts = powerCounts,
                onHome = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.soundManager.playDragSelect()
                    engine.isPaused = false
                    engine.screenState = ScreenState.MAIN_MENU
                    screenState = ScreenState.MAIN_MENU
                },
                onRestart = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.restartLevel(context)
                    gameStatus = engine.status
                    isPaused = engine.isPaused
                },
                onTogglePause = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    engine.isPaused = !engine.isPaused
                    isPaused = engine.isPaused
                },
                onNextMission = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onNextMission()
                    gameStatus = engine.status
                }
            )
        }
    }
}

@Composable
private fun MainMenuComposeOverlay(
    res: Resources,
    engine: GameEngine,
    campaignManager: CampaignManager,
    screenState: ScreenState,
    activeTab: Int,
    isSettingsOpen: Boolean,
    onTabChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onLaunchMission: () -> Unit,
    onOpenLevelSelect: () -> Unit,
    onBackToMenu: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onStateMutated: () -> Unit
) {
    val sysBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val safeTop = maxOf(sysBarsPadding.calculateTopPadding(), 24.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // Middle Content Zone
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safeTop + 54.dp, bottom = 84.dp),
            contentAlignment = Alignment.Center
        ) {
            if (screenState == ScreenState.LEVEL_SELECT) {
                LevelSelectSection(
                    res = res,
                    engine = engine,
                    campaignManager = campaignManager,
                    onSelectLevel = onSelectLevel,
                    onBackToMenu = onBackToMenu
                )
            } else {
                when (activeTab) {
                    0 -> MissionTabSection(
                        res = res,
                        campaignManager = campaignManager,
                        onLaunchMission = onLaunchMission,
                        onOpenLevelSelect = onOpenLevelSelect
                    )
                    1 -> TechTabSection(
                        res = res,
                        engine = engine,
                        campaignManager = campaignManager,
                        onUpgraded = onStateMutated
                    )
                    2 -> ArmoryTabSection(
                        res = res,
                        engine = engine,
                        campaignManager = campaignManager,
                        onSkinSelected = onStateMutated
                    )
                }
            }
        }

        // Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent)
                    )
                )
                .padding(top = safeTop + 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyPill(
                creditsText = res.getString(R.string.currency_credits_format, campaignManager.formatNumber(campaignManager.credits)),
                gemsText = res.getString(R.string.currency_gems_format, campaignManager.formatNumber(campaignManager.gems))
            )
            IconCircleButton(
                iconRes = R.drawable.ic_settings,
                onClick = onOpenSettings
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            activeTab = if (screenState == ScreenState.LEVEL_SELECT) -1 else activeTab,
            titles = listOf(
                res.getString(R.string.tab_mission),
                res.getString(R.string.tab_tech),
                res.getString(R.string.tab_armory)
            ),
            onTabSelected = onTabChange,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Settings Modal
        if (isSettingsOpen) {
            SettingsModalDialog(
                res = res,
                engine = engine,
                campaignManager = campaignManager,
                onClose = onCloseSettings,
                onMutated = onStateMutated
            )
        }
    }
}

@Composable
private fun MissionTabSection(
    res: Resources,
    campaignManager: CampaignManager,
    onLaunchMission: () -> Unit,
    onOpenLevelSelect: () -> Unit
) {
    val lvl = campaignManager.playerLevel
    val rank = res.getString(campaignManager.getRankResId())
    val sectors = campaignManager.sectorsConquered
    val winRate = String.format(Locale.US, "%.1f%%", campaignManager.getWinRatePercent())
    val streak = campaignManager.winStreak

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Pulsing Rank Badge
        PulsingRankBadge(text = res.getString(R.string.rank_badge_format, rank, lvl))

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Game Logo: WARGAME (White) + .IO (Cyan)
        val rawLogo = res.getString(R.string.logo_title)
        val dotIndex = rawLogo.indexOf('.')
        Text(
            text = buildAnnotatedString {
                if (dotIndex > 0) {
                    withStyle(SpanStyle(color = Color.White)) {
                        append(rawLogo.substring(0, dotIndex))
                    }
                    withStyle(SpanStyle(color = WarGameColors.CyanBright)) {
                        append(rawLogo.substring(dotIndex))
                    }
                } else {
                    append(rawLogo)
                }
            },
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Subtitle
        Text(
            text = res.getString(R.string.logo_subtitle),
            color = Color(0xFF888888),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Horizontal Gold Gradient Separator
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, WarGameColors.Gold, Color.Transparent)
                    )
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Player Stat Card
        WarGameCard(
            modifier = Modifier.width(320.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = res.getString(R.string.player_level_label),
                color = WarGameColors.TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = res.getString(R.string.player_level_value, lvl),
                color = WarGameColors.Gold,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    shadow = Shadow(
                        color = WarGameColors.Gold.copy(alpha = 0.38f),
                        offset = Offset(0f, 4f),
                        blurRadius = 24f
                    )
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = res.getString(R.string.sectors_conquered_format, sectors),
                color = WarGameColors.Cyan,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WarGameColors.Divider)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = res.getString(R.string.win_rate_format, winRate),
                    color = WarGameColors.Green,
                    fontSize = 10.5.sp
                )
                Text(
                    text = res.getString(R.string.streak_format, streak),
                    color = WarGameColors.Gold,
                    fontSize = 10.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Last Sector Pill (clickable -> opens Sector Select)
        WarGameCard(
            modifier = Modifier.width(320.dp),
            backgroundColor = WarGameColors.CardAltBg,
            cornerRadius = 8.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            onClick = onOpenLevelSelect
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_crosshair),
                        contentDescription = null,
                        tint = WarGameColors.TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = res.getString(R.string.last_sector_format, campaignManager.lastSector),
                        color = WarGameColors.TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
                Text(
                    text = res.getString(
                        R.string.last_sector_rewards_format,
                        campaignManager.formatNumber(campaignManager.lastScore),
                        campaignManager.lastCreditsReward,
                        campaignManager.lastGemsReward
                    ),
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 7. Main Pill Button with Glow & Breathing Pulse
        PrimaryGlowPillButton(
            text = res.getString(R.string.launch_mission),
            iconRes = R.drawable.ic_play,
            onClick = onLaunchMission,
            modifier = Modifier.width(320.dp),
            height = 58.dp,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 8. Tactical Hints
        Text(
            text = res.getString(R.string.tactical_hint_1),
            color = WarGameColors.TextSubtle,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = res.getString(R.string.tactical_hint_2),
            color = Color(0xFF444444),
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TechTabSection(
    res: Resources,
    engine: GameEngine,
    campaignManager: CampaignManager,
    onUpgraded: () -> Unit
) {
    val hostView = LocalView.current
    val icons = listOf(
        R.drawable.ic_zap,
        R.drawable.ic_rotate_ccw,
        R.drawable.ic_shield,
        R.drawable.ic_box
    )
    val names = listOf(
        res.getString(R.string.tech_speed),
        res.getString(R.string.tech_regen),
        res.getString(R.string.tech_shield),
        res.getString(R.string.tech_capacity)
    )
    val levels = listOf(
        campaignManager.techSpeedLevel,
        campaignManager.techRegenLevel,
        campaignManager.techShieldLevel,
        campaignManager.techCapacityLevel
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = res.getString(R.string.tech_title),
            color = WarGameColors.Gold,
            fontSize = 20.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = res.getString(R.string.tech_subtitle),
            color = Color(0xFF888888),
            fontSize = 9.5.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until 4) {
            val lvl = levels[i]
            val cost = lvl * 250
            val canAfford = campaignManager.credits >= cost && lvl < 10

            WarGameCard(
                modifier = Modifier
                    .width(320.dp)
                    .padding(bottom = 10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(icons[i]),
                        contentDescription = null,
                        tint = WarGameColors.Cyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = names[i], color = Color.White, fontSize = 12.sp)
                        Text(
                            text = res.getString(R.string.tech_level_format, lvl),
                            color = WarGameColors.Cyan,
                            fontSize = 10.sp
                        )
                    }

                    val btnBg = if (canAfford) WarGameColors.Gold else WarGameColors.Divider
                    val btnFg = if (canAfford) WarGameColors.GoldDarkText else WarGameColors.TextMuted

                    Row(
                        modifier = Modifier
                            .width(92.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(btnBg)
                            .clickable {
                                if (lvl < 10 && campaignManager.credits >= cost) {
                                    campaignManager.credits -= cost
                                    when (i) {
                                        0 -> campaignManager.techSpeedLevel = lvl + 1
                                        1 -> campaignManager.techRegenLevel = lvl + 1
                                        2 -> campaignManager.techShieldLevel = lvl + 1
                                        else -> campaignManager.techCapacityLevel = lvl + 1
                                    }
                                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    engine.soundManager.playBaseCaptured()
                                    onUpgraded()
                                } else {
                                    hostView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    engine.soundManager.playBaseHit()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (lvl < 10) {
                            Icon(
                                painter = painterResource(R.drawable.ic_coins),
                                contentDescription = null,
                                tint = btnFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = if (lvl >= 10) res.getString(R.string.tech_max) else res.getString(R.string.tech_cost_format, cost),
                            color = btnFg,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArmoryTabSection(
    res: Resources,
    engine: GameEngine,
    campaignManager: CampaignManager,
    onSkinSelected: () -> Unit
) {
    val hostView = LocalView.current
    val skinNames = listOf(
        res.getString(R.string.skin_cyan),
        res.getString(R.string.skin_gold),
        res.getString(R.string.skin_emerald),
        res.getString(R.string.skin_violet)
    )
    val skinColors = listOf(
        WarGameColors.CyanBright,
        WarGameColors.Gold,
        Color(0xFF10B981),
        WarGameColors.Violet
    )
    val selectedSkin = campaignManager.selectedSkinIndex

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = res.getString(R.string.armory_title),
            color = WarGameColors.Cyan,
            fontSize = 20.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = res.getString(R.string.armory_subtitle),
            color = Color(0xFF888888),
            fontSize = 9.5.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until 4) {
            val isSel = i == selectedSkin
            WarGameCard(
                modifier = Modifier
                    .width(320.dp)
                    .padding(bottom = 10.dp),
                borderColor = if (isSel) skinColors[i] else WarGameColors.CardBorder,
                borderWidth = if (isSel) 1.8.dp else 1.dp,
                contentPadding = PaddingValues(14.dp),
                onClick = {
                    campaignManager.selectedSkinIndex = i
                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    engine.soundManager.playDragSelect()
                    onSkinSelected()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(skinColors[i], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = skinNames[i],
                        color = Color.White,
                        fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = res.getString(if (isSel) R.string.skin_equipped else R.string.skin_select),
                        color = if (isSel) WarGameColors.Gold else WarGameColors.TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelSelectSection(
    res: Resources,
    engine: GameEngine,
    campaignManager: CampaignManager,
    onSelectLevel: (Int) -> Unit,
    onBackToMenu: () -> Unit
) {
    val hostView = LocalView.current
    val unlocked = campaignManager.unlockedLevel
    val titles = listOf(
        res.getString(R.string.sector_1_title),
        res.getString(R.string.sector_2_title),
        res.getString(R.string.sector_3_title),
        res.getString(R.string.sector_4_title),
        res.getString(R.string.sector_5_title)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = res.getString(R.string.sectors_title),
            color = WarGameColors.Gold,
            fontSize = 20.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until 5) {
            val isUnlocked = (i + 1) <= unlocked
            val isCurrent = (i + 1) == engine.currentLevelIndex

            WarGameCard(
                modifier = Modifier
                    .width(320.dp)
                    .padding(bottom = 8.dp),
                backgroundColor = if (isUnlocked) WarGameColors.CardBg else Color(0xFF0C0C0C),
                borderColor = when {
                    isCurrent -> WarGameColors.Gold
                    isUnlocked -> WarGameColors.ButtonSecondaryBorder
                    else -> Color(0xFF1A1A1A)
                },
                borderWidth = if (isCurrent) 1.8.dp else 1.dp,
                cornerRadius = 8.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                onClick = {
                    if (isUnlocked) {
                        onSelectLevel(i + 1)
                    } else {
                        hostView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        engine.soundManager.playBaseHit()
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titles[i],
                        color = if (isUnlocked) Color.White else WarGameColors.TextSubtle,
                        fontSize = 12.5.sp
                    )
                    val statusColor = if (isUnlocked) WarGameColors.Green else WarGameColors.TextMuted
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = res.getString(if (isUnlocked) R.string.sector_deploy else R.string.sector_locked),
                            color = statusColor,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(if (isUnlocked) R.drawable.ic_play else R.drawable.ic_lock),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SecondaryButton(
            text = res.getString(R.string.back_to_menu),
            textColor = WarGameColors.Gold,
            onClick = onBackToMenu,
            modifier = Modifier.width(320.dp)
        )
    }
}

@Composable
private fun SettingsModalDialog(
    res: Resources,
    engine: GameEngine,
    campaignManager: CampaignManager,
    onClose: () -> Unit,
    onMutated: () -> Unit
) {
    val hostView = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            ),
        contentAlignment = Alignment.Center
    ) {
        WarGameCard(
            modifier = Modifier
                .width(290.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            borderColor = Color(0xFF333333),
            borderWidth = 1.5.dp,
            cornerRadius = 12.dp,
            contentPadding = PaddingValues(20.dp)
        ) {
            Text(
                text = res.getString(R.string.settings_title),
                color = WarGameColors.Gold,
                fontSize = 16.sp,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Language Switch
            SecondaryButton(
                text = res.getString(R.string.settings_language),
                iconRes = R.drawable.ic_globe,
                textColor = WarGameColors.Cyan,
                onClick = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    engine.soundManager.playDragSelect()
                    campaignManager.language = if (campaignManager.language.equals("sk", ignoreCase = true)) "en" else "sk"
                    onMutated()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 2. Sound Toggle
            val muted = engine.soundManager.isMuted
            SecondaryButton(
                text = res.getString(if (muted) R.string.settings_sound_muted else R.string.settings_sound_active),
                iconRes = if (muted) R.drawable.ic_volume_off else R.drawable.ic_volume_on,
                textColor = if (muted) Color(0xFFEF4444) else WarGameColors.Green,
                onClick = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    engine.soundManager.isMuted = !engine.soundManager.isMuted
                    if (!engine.soundManager.isMuted) {
                        engine.soundManager.playDragSelect()
                    }
                    onMutated()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 3. Reset Progress
            SecondaryButton(
                text = res.getString(R.string.settings_reset_progress),
                iconRes = R.drawable.ic_trash,
                textColor = WarGameColors.Danger,
                onClick = {
                    hostView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    engine.soundManager.playBaseLost()
                    campaignManager.resetProgress()
                    onMutated()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = res.getString(R.string.settings_close),
                color = WarGameColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun InGameHudComposeOverlay(
    res: Resources,
    engine: GameEngine,
    campaignManager: CampaignManager,
    gameStatus: GameStatus,
    isPaused: Boolean,
    powerCounts: Map<Int, Int>,
    onHome: () -> Unit,
    onRestart: () -> Unit,
    onTogglePause: () -> Unit,
    onNextMission: () -> Unit
) {
    val sysBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val safeTop = maxOf(sysBarsPadding.calculateTopPadding(), 12.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = safeTop + 4.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = engine.levelName.uppercase(Locale.ROOT),
                    color = WarGameColors.TextSecondary,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                FactionPowerBar(
                    powerCounts = powerCounts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }

            HudIconButton(iconRes = R.drawable.ic_home, onClick = onHome)
            Spacer(modifier = Modifier.width(6.dp))
            HudIconButton(iconRes = R.drawable.ic_rotate_ccw, onClick = onRestart)
            Spacer(modifier = Modifier.width(6.dp))
            HudIconButton(
                iconRes = if (isPaused) R.drawable.ic_play else R.drawable.ic_pause,
                onClick = onTogglePause
            )
        }

        // Victory / Defeat Modal
        if (gameStatus != GameStatus.RUNNING) {
            val isWin = gameStatus == GameStatus.VICTORY
            val accent = if (isWin) WarGameColors.Gold else WarGameColors.Danger

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB9090D16))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                WarGameCard(
                    modifier = Modifier.width(290.dp),
                    borderColor = accent,
                    borderWidth = 2.dp,
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(24.dp)
                ) {
                    Text(
                        text = res.getString(if (isWin) R.string.victory_title else R.string.defeat_title),
                        color = accent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isWin) {
                            res.getString(R.string.victory_subtitle_format, campaignManager.lastCreditsReward, campaignManager.lastGemsReward)
                        } else {
                            res.getString(R.string.defeat_subtitle)
                        },
                        color = WarGameColors.TextSecondary,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    if (isWin) {
                        PrimaryGlowPillButton(
                            text = res.getString(R.string.next_mission),
                            iconRes = R.drawable.ic_play,
                            onClick = onNextMission,
                            modifier = Modifier.fillMaxWidth(),
                            height = 46.dp,
                            fontSize = 14.sp
                        )
                    } else {
                        SecondaryButton(
                            text = res.getString(R.string.retry_mission),
                            iconRes = R.drawable.ic_rotate_ccw,
                            textColor = Color.White,
                            backgroundColor = Color(0xFFE11D48),
                            borderColor = WarGameColors.Danger,
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SecondaryButton(
                        text = res.getString(R.string.main_menu_btn),
                        iconRes = R.drawable.ic_home,
                        onClick = onHome,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FactionPowerBar(
    powerCounts: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.clip(CircleShape)
    ) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.24f),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )

        val totalUnits = powerCounts.values.sum().coerceAtLeast(1)
        val sortedFactions = powerCounts.keys.sortedWith { a, b ->
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
            val count = powerCounts[factionId] ?: 0
            if (count > 0) {
                val segW = (count.toFloat() / totalUnits.toFloat()) * size.width
                drawRect(
                    color = Color(GameConfig.getFaction(factionId).primaryColor),
                    topLeft = Offset(currentX, 0f),
                    size = Size(segW, size.height)
                )
                currentX += segW
            }
        }
    }
}

private fun localizedResources(context: Context, language: String): Resources {
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(language.lowercase(Locale.ROOT)))
    return context.createConfigurationContext(config).resources
}
