package cdev.wargame.ui.screens

import android.content.res.Resources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdev.wargame.R
import cdev.wargame.core.CampaignManager
import cdev.wargame.core.GameConfig
import cdev.wargame.core.GameEngine
import cdev.wargame.core.GameStatus
import cdev.wargame.ui.components.HudIconButton
import cdev.wargame.ui.components.PrimaryGlowPillButton
import cdev.wargame.ui.components.SecondaryButton
import cdev.wargame.ui.components.WarGameCard
import cdev.wargame.ui.components.rememberSafeNotchInsets
import cdev.wargame.ui.theme.WarGameColors
import java.util.Locale

@Composable
fun GameHudScreen(
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
    val notchInsets = rememberSafeNotchInsets(minTop = 14.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // Top HUD Bar (respects notch / displayCutout + hidden statusBars height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = notchInsets.top + 6.dp,
                    start = notchInsets.start + 14.dp,
                    end = notchInsets.end + 14.dp
                )
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
                            fontSize = 14.sp,
                            iconSize = 14.dp,
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

